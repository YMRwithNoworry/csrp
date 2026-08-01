package alku.csrp.infection;

import alku.csrp.Config;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Server-side COTH and Viral infection progression, spread, and host conversion. */
public final class InfectionMechanics {
    public static final int COTH_BASE_DURATION_TICKS = 3_600;
    public static final int COTH_REFRESH_THRESHOLD_TICKS = 200;
    public static final int COTH_MAX_AMPLIFIER = 2;
    public static final double COTH_SPREAD_RADIUS = 4.0D;
    public static final float COTH_CONVERSION_HEALTH_FRACTION = 0.35F;
    private static final int VIRAL_MIN_DURATION_TICKS = 100;
    private static final double VIRAL_SPREAD_RADIUS = 4.0D;

    private InfectionMechanics() {
    }

    public static void applyCoth(LivingEntity target, Entity source) {
        applyCoth(target, source, COTH_BASE_DURATION_TICKS);
    }

    public static void applyCoth(LivingEntity target, Entity source, int minimumDurationTicks) {
        if (!isInfectable(target)) {
            return;
        }
        int durationFloor = Math.max(COTH_BASE_DURATION_TICKS, minimumDurationTicks);
        MobEffectInstance existing = target.getEffect(ModMobEffects.COTH);
        int amplifier = existing == null ? 0 : existing.getAmplifier();
        int duration = existing == null ? durationFloor : Math.max(existing.getDuration(), durationFloor);
        target.addEffect(new MobEffectInstance(ModMobEffects.COTH, duration, amplifier, false, false), source);
    }

    public static void tickCoth(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !isInfectable(entity)) {
            return;
        }
        MobEffectInstance coth = entity.getEffect(ModMobEffects.COTH);
        if (coth == null) {
            return;
        }

        int effectiveAmplifier = Math.min(COTH_MAX_AMPLIFIER, amplifier);
        if (coth.getDuration() > 0 && coth.getDuration() <= COTH_REFRESH_THRESHOLD_TICKS) {
            effectiveAmplifier = Math.min(COTH_MAX_AMPLIFIER, effectiveAmplifier + 1);
            entity.addEffect(new MobEffectInstance(ModMobEffects.COTH, COTH_BASE_DURATION_TICKS,
                    effectiveAmplifier, false, false));
        }
        if (effectiveAmplifier >= 1) {
            spreadCoth(entity);
        }
        if (effectiveAmplifier >= COTH_MAX_AMPLIFIER
                && entity.getHealth() <= entity.getMaxHealth() * COTH_CONVERSION_HEALTH_FRACTION) {
            convertInfectedHost(entity);
        }
    }

    public static void tickViral(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !isInfectable(entity)) {
            return;
        }
        entity.hurt(entity.damageSources().magic(), 1.0F + amplifier);
        if (entity.isAlive()) {
            spreadViral(entity, amplifier);
        }
    }

    public static boolean convertInfectedHost(LivingEntity host) {
        if (host.level().isClientSide || !isInfectable(host) || host instanceof Player
                || !(host.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Mob converted = createAssimilatedHost(host, serverLevel);
        if (converted == null) {
            return false;
        }

        float healthFraction = host.getMaxHealth() <= 0.0F ? 1.0F : host.getHealth() / host.getMaxHealth();
        converted.moveTo(host.getX(), host.getY(), host.getZ(), host.getYRot(), host.getXRot());
        converted.setHealth(Math.max(1.0F, converted.getMaxHealth() * Math.max(0.1F, healthFraction)));
        converted.setCustomName(host.getCustomName());
        converted.setCustomNameVisible(host.isCustomNameVisible());
        converted.setPersistenceRequired();
        serverLevel.addFreshEntity(converted);
        EvolutionSystem.addPoints(serverLevel, EvolutionSystem.VALUE_COTH, EvolutionSystem.PointSource.COTH);
        host.discard();
        return true;
    }

    /** Applies the original SRP COTH-on-kill conversion chance. */
    public static boolean convertKilledHost(LivingEntity host, Entity attacker) {
        if (!(attacker instanceof Parasite) || host.level().isClientSide || host instanceof Parasite
                || host instanceof Player || !host.isAlive() || host.getEffect(ModMobEffects.COTH) == null
                || host.hasEffect(ModMobEffects.REPEL)
                || host.getRandom().nextDouble() >= Config.cothConvert()) {
            return false;
        }
        return convertInfectedHost(host);
    }

    private static void spreadCoth(LivingEntity source) {
        for (LivingEntity target : source.level().getEntitiesOfClass(LivingEntity.class,
                source.getBoundingBox().inflate(COTH_SPREAD_RADIUS), InfectionMechanics::isInfectable)) {
            if (target != source) {
                applyCoth(target, source);
            }
        }
    }

    private static void spreadViral(LivingEntity source, int amplifier) {
        MobEffectInstance viral = source.getEffect(ModMobEffects.VIRAL);
        int duration = viral == null ? VIRAL_MIN_DURATION_TICKS
                : Math.max(VIRAL_MIN_DURATION_TICKS, viral.getDuration() / 2);
        for (LivingEntity target : source.level().getEntitiesOfClass(LivingEntity.class,
                source.getBoundingBox().inflate(VIRAL_SPREAD_RADIUS), InfectionMechanics::isInfectable)) {
            if (target != source) {
                target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, duration, amplifier, false, false), source);
            }
        }
    }

    private static Mob createAssimilatedHost(LivingEntity host, ServerLevel level) {
        ResourceLocation hostId = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType());
        for (String mapping : Config.cothVictimParasites()) {
            String[] parts = mapping.split(";", -1);
            if (parts.length != 2 || !parts[0].trim().equals(hostId.toString())) {
                continue;
            }
            ResourceLocation targetId = ResourceLocation.tryParse(parts[1].trim());
            if (targetId == null) {
                continue;
            }
            Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(targetId)
                    .map(type -> type.create(level)).orElse(null);
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }

    private static boolean isInfectable(LivingEntity entity) {
        return entity.isAlive() && !(entity instanceof Parasite) && !entity.hasEffect(ModMobEffects.REPEL)
                && !(entity instanceof Player player && player.getAbilities().invulnerable);
    }
}
