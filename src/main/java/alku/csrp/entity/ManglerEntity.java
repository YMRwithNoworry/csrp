package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class ManglerEntity extends PrimitiveParasiteEntity {
    @Override
    protected int maxDamageAdaptationHits() {
        return 8;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return 0.125F;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return 12;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return 0.95F;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return 0.30F;
    }

    @Override
    protected float damageAdaptationEffectiveness() {
        return 0.95F;
    }
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation AGE_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation LIMB_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation LIMB_STATUS_2 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");
    private static final int NORMAL = 0;
    private static final int VIRAL = 1;
    private static final int BLEEDING = 2;

    private int variant;

    public ManglerEntity(EntityType<? extends ManglerEntity> type, Level level) {
        super(type, level);
        xpReward = 10;
        if (!level.isClientSide && random.nextFloat() < 0.25F) {
            variant = random.nextBoolean() ? VIRAL : BLEEDING;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 17.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.37)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new EvasiveDashGoal());
        goalSelector.addGoal(2, createAnimatedLeapGoal(0.8F, 20));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.3, false));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3), this);
            if (variant == VIRAL) {
                living.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 100, 0), this);
            } else if (variant == BLEEDING) {
                living.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 100, 0), this);
            }
            if (!living.hasEffect(ModMobEffects.COTH) && random.nextFloat() < 0.15F) {
                living.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0), this);
            }
        }
        return hurt;
    }

    @Override
    public void push(Entity entity) {
        if (!level().isClientSide && variant == VIRAL && entity instanceof LivingEntity living
                && isValidParasiteTarget(living)) {
            living.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 100, 0), this);
        }
        super.push(entity);
    }

    @Override
    public boolean onClimbable() {
        return horizontalCollision || super.onClimbable();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return distance >= 200.0F && super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.RUPTER_STEP.get(), 0.3F, getVoicePitch());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("variant", variant);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        variant = tag.getInt("variant");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> {
                    if (isSpecialLeapAnimating()) {
                        return state.setAndContinue(LEAP);
                    }
                    boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
                    return switch (variant) {
                        case VIRAL -> state.setAndContinue(moving ? LIMB_STATUS_1 : AGE_STATUS_1);
                        case BLEEDING -> state.setAndContinue(moving ? LIMB_STATUS_2 : AGE_IN_TICKS);
                        default -> state.setAndContinue(moving ? LIMB_SWING : AGE_IN_TICKS);
                    };
                }));
    }

    private final class EvasiveDashGoal extends Goal {
        private static final int DASH_COOLDOWN_TICKS = 10;
        private static final double MIN_DASH_DISTANCE_SQR = 1.0D;
        private static final double MAX_DASH_DISTANCE_SQR = 225.0D;
        private int cooldown;

        private EvasiveDashGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !onGround()) {
                cooldown = 0;
                return false;
            }
            double distance = distanceToSqr(target);
            if (distance <= MIN_DASH_DISTANCE_SQR || distance >= MAX_DASH_DISTANCE_SQR
                    || !hasLineOfSight(target)) {
                return false;
            }
            return ++cooldown >= DASH_COOLDOWN_TICKS;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            cooldown = 0;
            if (target == null) {
                return;
            }
            Vec3 direction = new Vec3(target.getX() - getX(), 0.0D, target.getZ() - getZ());
            if (direction.lengthSqr() <= 0.001D) {
                return;
            }
            direction = direction.normalize();
            Vec3 movement = getDeltaMovement();
            double axisX = random.nextBoolean() ? 1.0D : 0.0D;
            double axisZ = axisX == 0.0D ? 1.0D : 0.0D;
            setDeltaMovement(movement.x * 1.2D + direction.x * 0.8D + axisX,
                    movement.y, movement.z * 1.2D + direction.z * 0.8D + axisZ);
            navigation.stop();
        }
    }
}
