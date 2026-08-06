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
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this, "idle.get_parasite_status_10");
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
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, false));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            triggerAnim("attack_controller", "attack");
        }
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
                state -> state.setAndContinue(isSpecialLeapAnimating() ? LEAP : ParasiteAnimations.isMoving(this, state.isMoving()) ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    private final class EvasiveDashGoal extends Goal {
        private EvasiveDashGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && onGround() && distanceToSqr(target) < 100.0 && random.nextInt(30) == 0;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            Vec3 away = position().subtract(target.position());
            Vec3 side = new Vec3(-away.z, 0.0, away.x);
            if (side.lengthSqr() > 0.001) {
                side = side.normalize().scale(random.nextBoolean() ? 1.0 : -1.0);
                setDeltaMovement(getDeltaMovement().add(side.x, 0.35, side.z));
            }
        }
    }
}
