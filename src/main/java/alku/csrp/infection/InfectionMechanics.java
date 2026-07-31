package alku.csrp.infection;

import alku.csrp.entity.AssimilatedParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Wolf;
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
        if (host.level().isClientSide || host instanceof Parasite || host instanceof Player
                || !(host.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        AssimilatedParasiteEntity converted = createAssimilatedHost(host, serverLevel);
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

    private static AssimilatedParasiteEntity createAssimilatedHost(LivingEntity host, ServerLevel level) {
        if (host instanceof Cow) {
            return ModEntities.SIM_COW.get().create(level);
        }
        if (host instanceof Pig) {
            return ModEntities.SIM_PIG.get().create(level);
        }
        if (host instanceof Sheep) {
            return ModEntities.SIM_SHEEP.get().create(level);
        }
        if (host instanceof Wolf) {
            return ModEntities.SIM_WOLF.get().create(level);
        }
        if (host instanceof Squid) {
            return ModEntities.SIM_SQUID.get().create(level);
        }
        if (host instanceof PolarBear) {
            return ModEntities.SIM_BEAR.get().create(level);
        }
        return null;
    }

    private static boolean isInfectable(LivingEntity entity) {
        return entity.isAlive() && !(entity instanceof Parasite)
                && !(entity instanceof Player player && player.getAbilities().invulnerable);
    }
}
