package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;
import net.minecraft.util.Mth;

import java.util.EnumSet;

/** Legacy flying colony architect (EntityTenn). */
public final class ArchitectEntity extends PrimitiveParasiteEntity {
    private static final int SUPPORT_SUMMON_INTERVAL = 80;
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
        moveControl = new ArchitectMoveControl();
        setNoGravity(true);
        noPhysics = true;
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
                .add(Attributes.FOLLOW_RANGE, 32.0D);
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
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 0,
                false, false, this::isValidPlayerTarget));
        if (Config.mobAttackingEnabled()) {
            targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 0,
                    !Config.collectiveConsciousnessEnabled(), false, this::isValidMobTarget));
        }
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        noPhysics = true;
        if (level().isClientSide) {
            return;
        }
        if (onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.5D);
        }
        applyFlightLimit(getTarget());
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

    public void applyConfiguredAttributes() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(MobsConfig.overseerHealth());
        getAttribute(Attributes.ARMOR).setBaseValue(MobsConfig.overseerArmor());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(MobsConfig.overseerMeleeDamage());
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(MobsConfig.overseerKnockbackResistance());
        if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose pose) {
        // Legacy EntityDimensions.withEyeHeight(1.6F).
        return 1.6F;
    }

    /** EntityTenn is a colony-only spawn when a colony already exists. */
    public boolean onlySpawnInside() {
        return true;
    }

    private boolean isValidPlayerTarget(LivingEntity target) {
        return target instanceof Player player && !player.getAbilities().instabuild
                && !player.isSpectator() && isValidParasiteTarget(target) && canAttack(player);
    }

    private boolean isValidMobTarget(LivingEntity target) {
        if (!(target instanceof Mob) || target instanceof Animal || target instanceof WaterAnimal
                || target instanceof Villager || target instanceof Creeper
                || !isValidParasiteTarget(target) || distanceToSqr(target) >= 1024.0D
                || !hasLineOfSight(target) || !canAttack(target)) {
            return false;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        boolean listed = Config.mobAttackingBlacklist().stream().anyMatch(entry ->
                entry.indexOf(':') >= 0 ? entry.equals(id) : entry.equals(
                        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace()));
        return Config.mobAttackingBlacklistInverted() ? listed : !listed;
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
        if (existing >= MobsConfig.overseerTotalActiveMobs()) {
            return;
        }
        FlamEntity succor = ModEntities.SUCCOR.get().create(level);
        if (succor == null) {
            return;
        }
        float heading = getYRot() * Mth.DEG_TO_RAD - yBodyRot * 0.01F;
        float spawnDistance = 4.0F * Mth.cos((float) Math.PI / 18.0F);
        Vec3 spawn = position().add(-Mth.sin(heading) * spawnDistance, getEyeHeight(),
                Mth.cos(heading) * spawnDistance);
        succor.moveTo(spawn.x, spawn.y, spawn.z, getYRot(), 0.0F);
        int actionType = random.nextInt(3) + 1;
        boolean teleportReserved = false;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof FlamEntity flam && flam.isAlive() && flam.isSummonedBy(this)) {
                teleportReserved |= flam.reservesTeleportAction();
            }
        }
        if (actionType == FlamEntity.ACTION_TELEPORT && (distanceToSqr(target) < 100.0D
                || !target.onGround() || teleportReserved)) {
            actionType = random.nextInt(2) + 1;
        }
        succor.configure(this, target, actionType);
        if (level.addFreshEntity(succor)) {
            for (int index = 0; index < 4; index++) {
                level.broadcastEntityEvent(succor, (byte) 8);
            }
        }
    }

    private void applyFlightLimit(LivingEntity target) {
        int limit = MobsConfig.overseerMaxY();
        if (limit == 256) {
            return;
        }
        if (target != null) {
            if (target.getY() + limit > getY()) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
            }
        } else if (!hasGroundWithin(limit)) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }
    }

    private boolean hasGroundWithin(int distance) {
        BlockPos cursor = blockPosition().below();
        for (int offset = 1; offset <= distance && cursor.getY() >= level().getMinBuildHeight(); offset++) {
            if (!level().getBlockState(cursor).isAir()) {
                return true;
            }
            cursor = cursor.below();
        }
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return random.nextBoolean() && getAdaptationHitStatus() > 0
                ? ModSounds.get("mob.silence") : super.getHurtSound(source);
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    private final class RandomFlightGoal extends Goal {
        private RandomFlightGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!getMoveControl().hasWanted()) {
                return true;
            }
            double x = getMoveControl().getWantedX() - getX();
            double y = getMoveControl().getWantedY() - getY();
            double z = getMoveControl().getWantedZ() - getZ();
            double distance = x * x + y * y + z * z;
            return distance < 1.0D || distance > 3600.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                double x = getX() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                double y = getY() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                double z = getZ() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                getMoveControl().setWantedPosition(x, y, z, 0.5D);
                return;
            }
            BlockPos origin = blockPosition();
            int mode = 1;
            double speed = 0.11D;
            double distance = distanceToSqr(target);
            if (distance > 400.0D) {
                origin = target.blockPosition();
                mode = 2;
                speed += 0.11D;
            } else if (distance < 100.0D) {
                origin = target.blockPosition();
                mode = 3;
                speed += 0.11D;
            }
            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos destination = switch (mode) {
                    case 2 -> origin.offset(random.nextInt(6) - 2, random.nextInt(7) - 2,
                            random.nextInt(6) - 2);
                    case 3 -> origin.offset(random.nextInt(4) + 3, random.nextInt(5) + 4,
                            random.nextInt(4) + 3);
                    default -> origin.offset(random.nextInt(15) - 7, random.nextInt(9) - 5,
                            random.nextInt(15) - 7);
                };
                if (level().isEmptyBlock(destination)) {
                    getMoveControl().setWantedPosition(destination.getX() + 0.5D,
                            destination.getY() + 0.5D, destination.getZ() + 0.5D, speed);
                    return;
                }
            }
        }
    }

    private final class ArchitectMoveControl extends MoveControl {
        private ArchitectMoveControl() {
            super(ArchitectEntity.this);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            double x = wantedX - getX();
            double y = wantedY - getY();
            double z = wantedZ - getZ();
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance < getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                setDeltaMovement(getDeltaMovement().scale(0.5D));
                return;
            }
            setDeltaMovement(getDeltaMovement().add(
                    x / distance * 0.05D * speedModifier,
                    y / distance * 0.05D * speedModifier,
                    z / distance * 0.05D * speedModifier));
            LivingEntity target = getTarget();
            double lookX = target == null ? getDeltaMovement().x : target.getX() - getX();
            double lookZ = target == null ? getDeltaMovement().z : target.getZ() - getZ();
            setYRot(-((float) Mth.atan2(lookX, lookZ)) * Mth.RAD_TO_DEG);
            yBodyRot = getYRot();
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
