package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

/** Legacy flying colony architect (EntityTenn). */
public final class ArchitectEntity extends PrimitiveParasiteEntity {
    @Override
    protected int maxDamageAdaptationHits() {
        return 5;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return 0.20F;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return 20;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return 1.0F;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return 0.30F;
    }

    @Override
    protected float damageAdaptationEffectiveness() {
        return 0.95F;
    }
    private final RawAnimation idleAnimation = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation flyAnimation = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation attackAnimation = ParasiteAnimations.play(this, "attack");
    private int attackAnimationTicks;

    public ArchitectEntity(EntityType<? extends ArchitectEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 18, true);
        setNoGravity(true);
        xpReward = 75;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 22.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.40D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new FlightAttackGoal());
        goalSelector.addGoal(6, new RandomFlightGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (attackAnimationTicks > 0) {
            attackAnimationTicks--;
        }
        if (level().isClientSide) {
            return;
        }
        if (onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.5D);
        }
        if (tickCount % 10 == 0 && random.nextInt(10) == 0 && level() instanceof ServerLevel serverLevel) {
            spawnColonyWorker(serverLevel);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    private PlayState movementAnimation(AnimationState<ArchitectEntity> state) {
        if (attackAnimationTicks > 0) {
            return state.setAndContinue(attackAnimation);
        }
        return state.setAndContinue(state.isMoving() ? flyAnimation : idleAnimation);
    }

    private void spawnColonyWorker(ServerLevel level) {
        if (level.getEntitiesOfClass(WorkerEntity.class, getBoundingBox().inflate(16.0D)).size() > 3) {
            return;
        }
        SrpWorldData.ColonyEntry colony = SrpWorldData.get(level).nearestColonyInEffectRange(blockPosition());
        if (colony == null) {
            return;
        }
        WorkerEntity worker = ModEntities.WORKER.get().create(level);
        if (worker == null) {
            return;
        }
        worker.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        worker.setColonyTask(colony.pos(), WorkerEntity.colonyRadius(colony));
        worker.finalizeSpawn(level, level.getCurrentDifficultyAt(worker.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(worker);
    }

    private final class FlightAttackGoal extends Goal {
        private int attackCooldown;

        private FlightAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = distanceToSqr(target);
            double height = distance > 400.0D ? 1.0D : distance < 100.0D ? 5.0D : 3.0D;
            getMoveControl().setWantedPosition(target.getX(), target.getY() + height, target.getZ(), 1.0D);
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            if (distance <= 9.0D && attackCooldown <= 0 && doHurtTarget(target)) {
                attackCooldown = 20;
                attackAnimationTicks = 10;
            }
        }
    }

    private final class RandomFlightGoal extends Goal {
        private RandomFlightGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && !getMoveControl().hasWanted();
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos destination = blockPosition().offset(
                    random.nextInt(33) - 16, random.nextInt(17) - 8, random.nextInt(33) - 16);
            if (level().getBlockState(destination).isAir()) {
                Vec3 center = Vec3.atCenterOf(destination);
                getMoveControl().setWantedPosition(center.x, center.y, center.z, 0.5D);
            }
        }
    }
}
