package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;

/** Legacy Ancient Dreadnaut ground tendril (EntityOroncoTen). */
public final class DreadnautTentacleEntity extends PrimitiveParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private int groundTicks;
    private int spawnedMobs;

    public DreadnautTentacleEntity(EntityType<? extends DreadnautTentacleEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ARMOR, 3.75D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!onGround()) {
            return;
        }
        groundTicks++;
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        if (groundTicks <= 200 || groundTicks % 20 != 0 || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.ASH, getX(), getY() + 0.4D, getZ(),
                12, 0.5D, 0.25D, 0.5D, 0.01D);
        if (spawnedMobs < 10 && nearbyNonParasitesHaveAdvantage()) {
            spawnBuglin(serverLevel);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("tendril_ground_ticks", groundTicks);
        tag.putInt("tendril_spawned_mobs", spawnedMobs);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        groundTicks = tag.getInt("tendril_ground_ticks");
        spawnedMobs = tag.getInt("tendril_spawned_mobs");
    }

    private boolean nearbyNonParasitesHaveAdvantage() {
        int balance = 0;
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(16.0D), LivingEntity::isAlive)) {
            if (living == this) {
                continue;
            }
            balance += living instanceof Parasite ? -1 : 1;
        }
        return balance > 0;
    }

    private void spawnBuglin(ServerLevel level) {
        BuglinEntity buglin = ModEntities.BUGLIN.get().create(level);
        if (buglin == null) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 1.0D + random.nextDouble() * 2.0D;
        buglin.moveTo(getX() + Math.cos(angle) * distance, getY(), getZ() + Math.sin(angle) * distance,
                random.nextFloat() * 360.0F, 0.0F);
        buglin.finalizeSpawn(level, level.getCurrentDifficultyAt(buglin.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        buglin.setTarget(getTarget());
        if (level.addFreshEntity(buglin)) {
            spawnedMobs++;
        }
    }
}
