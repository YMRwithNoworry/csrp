package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
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
    private static final int SUPPORT_SUMMON_INTERVAL = 80;
    private static final int MAX_SUMMONED_SUCCORS = 3;
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
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
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
        goalSelector.addGoal(6, new RandomFlightGoal());
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide) {
            return;
        }
        if (onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.5D);
        }
        if (tickCount % 10 == 0 && random.nextInt(10) == 0 && level() instanceof ServerLevel serverLevel) {
            spawnColonyWorker(serverLevel);
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && tickCount % SUPPORT_SUMMON_INTERVAL == SUPPORT_SUMMON_INTERVAL / 2
                && level() instanceof ServerLevel serverLevel) {
            spawnSuccor(serverLevel, target);
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

    private void spawnSuccor(ServerLevel level, LivingEntity target) {
        int existing = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof FlamEntity flam && flam.isAlive() && flam.isSummonedBy(this)) {
                existing++;
            }
        }
        if (existing >= MAX_SUMMONED_SUCCORS) {
            return;
        }
        FlamEntity succor = ModEntities.SUCCOR.get().create(level);
        if (succor == null) {
            return;
        }
        succor.moveTo(getX(), getY() + getEyeHeight(), getZ(), getYRot(), 0.0F);
        succor.configure(this, target, random.nextInt(3) + 1);
        level.addFreshEntity(succor);
    }

    private final class RandomFlightGoal extends Goal {
        private RandomFlightGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !getMoveControl().hasWanted();
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
