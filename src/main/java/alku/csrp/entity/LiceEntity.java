package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class LiceEntity extends PrimitiveParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    public static final int MAX_LIFESPAN_TICKS = 1200;
    public static final int VIRAL_DURATION_TICKS = 120;
    public static final int VIRAL_AMPLIFIER = 2;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");

    private int lifespan;
    private boolean charging;

    public LiceEntity(EntityType<? extends LiceEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.ARMOR, 5.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34559)
                .add(Attributes.FLYING_SPEED, 0.34559)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ChargeAttackGoal());
        goalSelector.addGoal(6, new RandomFlyGoal());
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide) {
            return;
        }
        if (++lifespan > MAX_LIFESPAN_TICKS) {
            discard();
            return;
        }

        LivingEntity target = getTarget();
        if (charging && target != null && target.isAlive()
                && getBoundingBox().inflate(0.15).intersects(target.getBoundingBox())) {
            target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, VIRAL_DURATION_TICKS, VIRAL_AMPLIFIER), this);
            discard();
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("lifespan", lifespan);
        tag.putBoolean("charging", charging);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifespan = tag.getInt("lifespan");
        charging = tag.getBoolean("charging");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(IDLE)));
    }

    private final class ChargeAttackGoal extends Goal {
        private int chargeTicks;

        private ChargeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return !charging && target != null && target.isAlive() && distanceToSqr(target) > 1.0
                    && random.nextInt(7) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return charging && chargeTicks < 50 && target != null && target.isAlive();
        }

        @Override
        public void start() {
            chargeTicks = 0;
            charging = true;
        }

        @Override
        public void stop() {
            charging = false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            chargeTicks++;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            Vec3 eye = target.getEyePosition();
            getMoveControl().setWantedPosition(eye.x, eye.y, eye.z, 2.0);
        }
    }

    private final class RandomFlyGoal extends Goal {
        private int cooldown;

        @Override
        public boolean canUse() {
            return getTarget() == null && --cooldown <= 0;
        }

        @Override
        public void start() {
            cooldown = 20 + random.nextInt(40);
            Vec3 destination = position().add(
                    random.nextInt(15) - 7,
                    random.nextInt(11) - 5,
                    random.nextInt(15) - 7);
            getMoveControl().setWantedPosition(destination.x, destination.y, destination.z, 0.8);
        }
    }
}
