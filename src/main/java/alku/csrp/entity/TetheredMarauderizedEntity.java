package alku.csrp.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Shared capture state for the legacy Marauderized bear and enderman pull attacks. */
public abstract class TetheredMarauderizedEntity extends MarauderizedParasiteEntity implements PullingBallOwner {
    private static final EntityDataAccessor<Integer> PULL_TARGET = SynchedEntityData.defineId(
            TetheredMarauderizedEntity.class, EntityDataSerializers.INT);

    private UUID pullTargetId;
    private int pullTicks;
    private int pullCooldown;

    protected TetheredMarauderizedEntity(EntityType<? extends TetheredMarauderizedEntity> type, Level level,
                                         int experience) {
        super(type, level, experience);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PULL_TARGET, 0);
    }

    @Override
    public boolean captureTarget(LivingEntity target) {
        if (level().isClientSide || target == null || hasPullTarget() || pullCooldown > 0
                || !isValidPullTarget(target)) {
            return false;
        }

        pullTargetId = target.getUUID();
        pullTicks = 0;
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, initialWeaknessAmplifier(), false, false), this);
        syncPullTarget();
        return true;
    }

    @Override
    public boolean isValidPullTarget(LivingEntity target) {
        return isValidParasiteTarget(target);
    }

    protected void tickTether() {
        if (level().isClientSide) {
            return;
        }
        if (pullCooldown > 0) {
            pullCooldown--;
        }
        if (!hasPullTarget()) {
            return;
        }

        LivingEntity target = resolvePullTarget();
        if (target == null || !hasLineOfSight(target) || distanceToSqr(target) > maxPullDistanceSqr()) {
            releasePullTarget(0);
            return;
        }
        if (++pullTicks > pullDurationTicks()) {
            releasePullTarget(pullCooldownTicks());
            return;
        }

        target.stopRiding();
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false), this);
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 1, false, false), this);
        Vec3 direction = position().subtract(target.position());
        if (direction.lengthSqr() > 0.001D) {
            Vec3 pull = direction.normalize().scale(pullStrength());
            target.push(pull.x, pull.y, pull.z);
        }
        target.hurt(damageSources().mobAttack(this), tetherDamage());
        syncPullTarget();
    }

    public LivingEntity getPullTargetForRendering() {
        int id = entityData.get(PULL_TARGET);
        Entity entity = id == 0 ? null : level().getEntity(id);
        return entity instanceof LivingEntity target && target.isAlive() ? target : null;
    }

    protected boolean hasPullTarget() {
        return pullTargetId != null;
    }

    protected int pullCooldown() {
        return pullCooldown;
    }

    protected double pullStrength() {
        return 0.1D;
    }

    protected int initialWeaknessAmplifier() {
        return 1;
    }

    protected float tetherDamage() {
        return 0.01F;
    }

    protected double maxPullDistanceSqr() {
        return 64.0D * 64.0D;
    }

    protected int pullDurationTicks() {
        return 200;
    }

    protected int pullCooldownTicks() {
        return 60;
    }

    private LivingEntity resolvePullTarget() {
        if (pullTargetId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(pullTargetId);
        return entity instanceof LivingEntity target && target.isAlive() ? target : null;
    }

    private void releasePullTarget(int cooldown) {
        pullTargetId = null;
        pullTicks = 0;
        pullCooldown = Math.max(pullCooldown, cooldown);
        syncPullTarget();
    }

    private void syncPullTarget() {
        if (!level().isClientSide) {
            LivingEntity target = resolvePullTarget();
            entityData.set(PULL_TARGET, target == null ? 0 : target.getId());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (pullTargetId != null) {
            tag.putUUID("pull_target", pullTargetId);
        }
        tag.putInt("pull_ticks", pullTicks);
        tag.putInt("pull_cooldown", pullCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pullTargetId = tag.hasUUID("pull_target") ? tag.getUUID("pull_target") : null;
        pullTicks = tag.getInt("pull_ticks");
        pullCooldown = tag.getInt("pull_cooldown");
        syncPullTarget();
    }
}
