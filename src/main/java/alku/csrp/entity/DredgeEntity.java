package alku.csrp.entity;

import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
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
    private static final int MAX_PULL_TICKS = 200;
    private static final double PULL_STRENGTH = 0.13;
    private static final RawAnimation ANIMATION = RawAnimation.begin().thenLoop("animation");

    private UUID pullTargetId;
    private int pullTicks;
    private int pullCooldown;

    public DredgeEntity(EntityType<? extends DredgeEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ARMOR, 9.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0).add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit && entity instanceof LivingEntity target && pullTargetId == null && pullCooldown == 0) {
            pullTargetId = target.getUUID();
            pullTicks = 0;
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 3), this);
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (pullCooldown > 0) pullCooldown--;

        LivingEntity pullTarget = pullTarget();
        if (pullTarget == null) {
            pullTargetId = null;
            pullTicks = 0;
            return;
        }
        if (!hasLineOfSight(pullTarget) || distanceToSqr(pullTarget) > 9.0 || ++pullTicks > MAX_PULL_TICKS) {
            releasePullTarget();
            return;
        }

        pullTarget.stopRiding();
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pullTargetId = tag.hasUUID("pull_target") ? tag.getUUID("pull_target") : null;
        pullTicks = tag.getInt("pull_ticks");
        pullCooldown = tag.getInt("pull_cooldown");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(ANIMATION)));
    }
}
