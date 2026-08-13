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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.EnumSet;

/** Legacy flying colony architect (EntityTenn). */
public final class ArchitectEntity extends PrimitiveParasiteEntity {
    private static final int SUPPORT_SUMMON_INTERVAL = 80;
    private static final int MAX_SUMMONED_SUCCORS = 3;
    private static final int MELEE_COOLDOWN = 20;
    private static final int COLONY_WORKER_CYCLE_TICKS = 21;
    private static final int COLONY_WORKER_CYCLE_OFFSET = 10;
    private static final double COLONY_WORKER_SEARCH_RANGE = 16.0D;
    private static final int MAX_NEARBY_COLONY_WORKERS = 4;
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
        if (level().isClientSide) {
            return;
        }
        if (onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.5D);
        }
        if (tickCount % COLONY_WORKER_CYCLE_TICKS == COLONY_WORKER_CYCLE_OFFSET
                && random.nextInt(10) == 0 && level() instanceof ServerLevel serverLevel) {
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
    }

    private void spawnColonyWorker(ServerLevel level) {
        AABB searchArea = new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D,
                getZ() + 1.0D).inflate(COLONY_WORKER_SEARCH_RANGE);
        if (level.getEntitiesOfClass(WorkerEntity.class, searchArea).size()
                >= MAX_NEARBY_COLONY_WORKERS) {
            return;
        }
        SrpWorldData.ColonyEntry colony = SrpWorldData.get(level).nearestColonyInConstructionRange(blockPosition());
        if (colony == null) {
            return;
        }
        WorkerEntity worker = ModEntities.WORKER.get().create(level);
        if (worker == null) {
            return;
        }
        worker.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        worker.setColonyTask(colony.pos(), WorkerEntity.colonyRadius(colony));
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
            getMoveControl().setWantedPosition(target.getX(), target.getY() + target.getEyeHeight() * 0.5D,
                    target.getZ(), distanceToSqr(target) > 400.0D ? 1.1D : 0.8D);
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            double reach = getBbWidth() * 2.0D + target.getBbWidth();
            if (attackCooldown <= 0 && distanceToSqr(target) <= reach * reach && hasLineOfSight(target)) {
                doHurtTarget(target);
                attackCooldown = MELEE_COOLDOWN;
            }
        }
    }
}
