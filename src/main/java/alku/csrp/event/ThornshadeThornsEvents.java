package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

/** Legacy Thornshade Thorns use limit, reflection, self-destruction, and propagation. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ThornshadeThornsEvents {
    private static final String ROOT_TAG = "srp_thornshade_thorns";
    private static final String USES_TAG = "Uses";
    private static final String COOLDOWN_TAG = "CooldownUntil";
    private static final String EXPLODE_DELAY_TAG = "ExplodeDelay";
    private static final String EXPLODED_TAG = "HasExplodedOnce";
    private static final float MAX_ALLOWED_HEALTH = 120.0F;
    private static final ResourceLocation SELF_DESTRUCT_ADVANCEMENT =
            new ResourceLocation(Csrp.MODID, "thornshade_self_destruct");
    private static final String SELF_DESTRUCT_CRITERION = "exploded";

    private ThornshadeThornsEvents() {
    }

    @SubscribeEvent
    public static void checkApplication(MobEffectEvent.Applicable event) {
        MobEffectInstance incoming = event.getEffectInstance();
        if (incoming.getEffect() != ModMobEffects.THORNSHADE_THORNS.get()) {
            return;
        }
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide) {
            return;
        }
        if (living instanceof Parasite || living.getMaxHealth() > MAX_ALLOWED_HEALTH
                || living.hasEffect(ModMobEffects.THORNSHADE_THORNS.get()) || isInfinite(incoming)) {
            event.setResult(MobEffectEvent.Applicable.Result.DENY);
            return;
        }

        CompoundTag data = thornData(living);
        int uses = data.getInt(USES_TAG);
        if (uses >= 2) {
            event.setResult(MobEffectEvent.Applicable.Result.DENY);
            if (!data.contains(EXPLODE_DELAY_TAG)) {
                scheduleExplosion(living, data);
            }
            saveThornData(living, data);
            return;
        }

        long now = living.level().getGameTime();
        if (data.getLong(COOLDOWN_TAG) > now) {
            event.setResult(MobEffectEvent.Applicable.Result.DENY);
            return;
        }
        data.putInt(USES_TAG, uses + 1);
        data.putLong(COOLDOWN_TAG, now + incoming.getDuration() / 2L);
        saveThornData(living, data);
    }

    @SubscribeEvent
    public static void reflectDamage(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        MobEffectInstance thorns = target.getEffect(ModMobEffects.THORNSHADE_THORNS.get());
        Entity source = event.getSource().getEntity();
        if (thorns == null || isInfinite(thorns) || !(source instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker || event.getAmount() <= 0.0F) {
            return;
        }
        int uses = thornData(target).getInt(USES_TAG);
        float reflected = event.getAmount() * (uses <= 1 ? 0.25F : 0.5F);
        attacker.hurt(target.damageSources().thorns(target), reflected);
    }

    @SubscribeEvent
    public static void tickExplosion(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide || !living.isAlive()) {
            return;
        }
        CompoundTag data = thornData(living);
        if (!data.contains(EXPLODE_DELAY_TAG)) {
            return;
        }
        int delay = data.getInt(EXPLODE_DELAY_TAG);
        if (delay > 0) {
            data.putInt(EXPLODE_DELAY_TAG, delay - 1);
            saveThornData(living, data);
            spawnBloodParticles((ServerLevel) living.level(), living, 15);
            return;
        }
        data.remove(EXPLODE_DELAY_TAG);
        saveThornData(living, data);
        explode(living);
    }

    private static void scheduleExplosion(LivingEntity living, CompoundTag data) {
        if (data.getBoolean(EXPLODED_TAG)) {
            return;
        }
        data.putInt(EXPLODE_DELAY_TAG, 20);
        living.level().playSound(null, living.blockPosition(),
                net.minecraft.sounds.SoundEvents.SCULK_SHRIEKER_SHRIEK,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F,
                0.8F + living.getRandom().nextFloat() * 0.4F);
    }

    private static void explode(LivingEntity center) {
        if (!(center.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag centerData = thornData(center);
        centerData.putBoolean(EXPLODED_TAG, true);
        saveThornData(center, centerData);
        awardSelfDestruction(center);

        double x = center.getX();
        double y = center.getY();
        double z = center.getZ();
        level.explode(null, x, y, z, 3.0F, Level.ExplosionInteraction.NONE);
        spawnRing(level, x, y + center.getBbHeight() * 0.5D, z, 3.0D, 50);
        spawnRing(level, x, y + center.getBbHeight() * 0.5D, z, 10.0D, 120);

        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(3.0D), ThornshadeThornsEvents::canReceiveThorns)) {
            if (other == center || !other.hasEffect(ModMobEffects.THORNSHADE_THORNS.get())) {
                continue;
            }
            CompoundTag data = thornData(other);
            if (!data.getBoolean(EXPLODED_TAG) && !data.contains(EXPLODE_DELAY_TAG)) {
                data.putInt(USES_TAG, Math.max(2, data.getInt(USES_TAG)));
                scheduleExplosion(other, data);
                saveThornData(other, data);
            }
        }

        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(10.0D), ThornshadeThornsEvents::canReceiveThorns)) {
            double distance = other.distanceToSqr(x, y, z);
            if (other == center || distance <= 9.0D || distance > 100.0D
                    || other.hasEffect(ModMobEffects.THORNSHADE_THORNS.get())) {
                continue;
            }
            other.addEffect(new MobEffectInstance(ModMobEffects.THORNSHADE_THORNS.get(),
                    600, 0, false, true), center);
        }
        center.invulnerableTime = 0;
        center.hurt(level.damageSources().magic(), Float.MAX_VALUE);
    }

    private static void awardSelfDestruction(LivingEntity center) {
        if (!(center instanceof ServerPlayer player)) {
            return;
        }
        Advancement advancement = player.server.getAdvancements().getAdvancement(SELF_DESTRUCT_ADVANCEMENT);
        if (advancement != null) {
            player.getAdvancements().award(advancement, SELF_DESTRUCT_CRITERION);
        }
    }

    private static boolean canReceiveThorns(LivingEntity entity) {
        return entity.isAlive() && !(entity instanceof Parasite)
                && entity.getMaxHealth() <= MAX_ALLOWED_HEALTH;
    }

    private static void spawnBloodParticles(ServerLevel level, LivingEntity entity, int count) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), count,
                0.3D, 0.4D, 0.3D, 0.15D);
    }

    private static void spawnRing(ServerLevel level, double x, double y, double z, double radius, int count) {
        for (int index = 0; index < count; index++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = radius * (0.7D + level.random.nextDouble() * 0.3D);
            double px = x + Math.cos(angle) * distance;
            double pz = z + Math.sin(angle) * distance;
            level.sendParticles(ParticleTypes.WITCH, px, y, pz, 1, 0.0D, 0.1D, 0.0D, 0.05D);
        }
    }

    private static CompoundTag thornData(LivingEntity entity) {
        return entity.getPersistentData().getCompound(ROOT_TAG);
    }

    private static void saveThornData(LivingEntity entity, CompoundTag data) {
        entity.getPersistentData().put(ROOT_TAG, data);
    }

    private static boolean isInfinite(MobEffectInstance effect) {
        return effect.isInfiniteDuration() || effect.getDuration() >= 72_000
                || effect.getDuration() == Integer.MAX_VALUE;
    }
}
