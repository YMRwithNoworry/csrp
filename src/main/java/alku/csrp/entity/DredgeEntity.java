package alku.csrp.entity;

import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.UUID;

public final class DredgeEntity extends CrudeParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return true;
    }
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_COMBAT = 1;
    private static final int STATUS_SPRINT = 2;
    private static final int STATUS_PULLING = 3;
    private static final int STILL_ANIMATION_DELAY_TICKS = 25;
    private static final int MAX_PULL_TICKS = 200;
    private static final double PULL_STRENGTH = 0.13;
    private static final EntityDataAccessor<Boolean> PULLING = SynchedEntityData.defineId(
            DredgeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            DredgeEntity.class, EntityDataSerializers.INT);
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation STILL_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_still_ani_1");
    private final RawAnimation COMBAT_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation COMBAT_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation COMBAT_STILL_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation SPRINT_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_2");
    private final RawAnimation SPRINT_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation SPRINT_STILL_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1");

    private UUID pullTargetId;
    private int pullTicks;
    private int pullCooldown;
    private int stillAnimationTicks;

    public DredgeEntity(EntityType<? extends DredgeEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ARMOR, 9.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0).add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PULLING, false);
        builder.define(PARASITE_STATUS, STATUS_IDLE);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit) {
        }
        if (hit && entity instanceof LivingEntity target && pullTargetId == null && pullCooldown == 0) {
            pullTargetId = target.getUUID();
            pullTicks = 0;
            entityData.set(PULLING, true);
            setParasiteStatus(STATUS_PULLING);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 3), this);
        }
        return hit;
    }

    @Override
    public void tick() {
        if (getX() == xo && getZ() == zo) {
            stillAnimationTicks++;
        } else {
            stillAnimationTicks = 0;
        }
        super.tick();
        if (level().isClientSide) return;
        if (pullCooldown > 0) pullCooldown--;

        LivingEntity pullTarget = pullTarget();
        if (pullTarget == null) {
            pullTargetId = null;
            pullTicks = 0;
            entityData.set(PULLING, false);
            updateCombatStatus();
            return;
        }
        if (!hasLineOfSight(pullTarget) || distanceToSqr(pullTarget) > 9.0 || ++pullTicks > MAX_PULL_TICKS) {
            releasePullTarget();
            return;
        }

        pullTarget.stopRiding();
        setParasiteStatus(STATUS_PULLING);
        pullTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false), this);
        pullTarget.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 1, false, false), this);
        var direction = position().subtract(pullTarget.position());
        if (direction.lengthSqr() > 0.001) {
            pullTarget.push(direction.x / direction.length() * PULL_STRENGTH,
                    direction.y / direction.length() * PULL_STRENGTH,
                    direction.z / direction.length() * PULL_STRENGTH);
        }
    }

    private LivingEntity pullTarget() {
        if (pullTargetId == null || !(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(pullTargetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void releasePullTarget() {
        pullTargetId = null;
        pullTicks = 0;
        pullCooldown = MAX_PULL_TICKS;
        entityData.set(PULLING, false);
        updateCombatStatus();
    }

    private int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    private void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    private boolean isStillAnimation() {
        return stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS;
    }

    private void updateCombatStatus() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setParasiteStatus(STATUS_IDLE);
            return;
        }
        setParasiteStatus(getDeltaMovement().horizontalDistanceSqr() > 0.0004D
                ? STATUS_SPRINT : STATUS_COMBAT);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return pullTargetId == null ? ModSounds.DREDGE_LIVING.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.DREDGE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DREDGE_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (pullTargetId != null) tag.putUUID("pull_target", pullTargetId);
        tag.putInt("pull_ticks", pullTicks);
        tag.putInt("pull_cooldown", pullCooldown);
        tag.putInt("parasite_status", getParasiteStatus());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pullTargetId = tag.hasUUID("pull_target") ? tag.getUUID("pull_target") : null;
        pullTicks = tag.getInt("pull_ticks");
        pullCooldown = tag.getInt("pull_cooldown");
        entityData.set(PULLING, pullTargetId != null);
        setParasiteStatus(tag.getInt("parasite_status"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
            return switch (getParasiteStatus()) {
                case STATUS_COMBAT -> state.setAndContinue(isStillAnimation()
                        ? COMBAT_STILL_IDLE : moving ? COMBAT_WALK : COMBAT_IDLE);
                case STATUS_SPRINT -> state.setAndContinue(isStillAnimation()
                        ? SPRINT_STILL_IDLE : moving ? SPRINT_WALK : SPRINT_IDLE);
                case STATUS_PULLING -> state.setAndContinue(IDLE);
                default -> state.setAndContinue(isStillAnimation() ? STILL_IDLE : moving ? WALK : IDLE);
            };
        }));
    }
}
