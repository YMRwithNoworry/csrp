package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class VerminEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Byte> FLIGHT_FLAGS = SynchedEntityData.defineId(
            VerminEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> COMBAT_STATUS = SynchedEntityData.defineId(
            VerminEntity.class, EntityDataSerializers.INT);
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");

    public VerminEntity(EntityType<? extends VerminEntity> type, Level level) {
        super(type, level);
        moveControl = new VerminMoveControl();
        setNoGravity(true);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 45.0).add(Attributes.ARMOR, 15.0)
                .add(Attributes.ATTACK_DAMAGE, 30.0).add(Attributes.FLYING_SPEED, 0.25)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.KNOCKBACK_RESISTANCE, 0.65)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLIGHT_FLAGS, (byte) 0);
        builder.define(COMBAT_STATUS, 0);
    }

    @Override protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        if (MobsConfig.yelloweyeMaxFlightHeight() != 256) {
            goalSelector.addGoal(3, new FlightHeightLimitGoal(MobsConfig.yelloweyeMaxFlightHeight()));
        }
        goalSelector.addGoal(4, new ChargeAttackGoal());
        goalSelector.addGoal(5, new DropPayloadGoal());
        goalSelector.addGoal(7, new RandomFlightGoal());
    }

    @Override public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide) {
            if (random.nextInt(25) == 0) {
                for (int index = 0; index < 4; index++) {
                    level().addParticle(ModParticles.ASSIMILATION_SPLASH.get(),
                            getRandomX(0.8D), getRandomY(), getRandomZ(0.8D),
                            0.0D, -0.1D, 0.0D);
                }
            }
            return;
        }
        LivingEntity target = getTarget();
        entityData.set(COMBAT_STATUS, target != null && target.isAlive() ? 1 : 0);
        if (tickCount % 21 == 10) {
            if (onGround()) {
                moveControl.setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.5D);
            }
            if (target != null && (!level().getBlockState(blockPosition().below()).isAir()
                    || !level().getBlockState(blockPosition().below(2)).isAir())) {
                Vec3 movement = getDeltaMovement();
                setDeltaMovement(movement.x, 0.5D, movement.z);
            }
        }
    }

    @Override
    protected boolean usesDefaultMovementGoals() {
        return false;
    }

    @Override
    protected boolean isValidParasiteTarget(LivingEntity target) {
        return super.isValidParasiteTarget(target) && !(target instanceof Animal)
                && !(target instanceof WaterAnimal) && !(target instanceof Creeper);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return entityData.get(COMBAT_STATUS) == 0 ? super.getAmbientSound() : null;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    private void dropGnatBomb() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int gnatCount = 0;
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity instanceof GnatEntity) {
                gnatCount++;
            }
        }
        if (gnatCount < Config.worldGnatCap()) {
            GnatEntity gnat = ModEntities.GNAT.get().create(serverLevel, null, blockPosition(),
                    MobSpawnType.MOB_SUMMONED, false, false);
            if (gnat != null) {
                gnat.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                serverLevel.addFreshEntity(gnat);
                spawnPayloadParticles(serverLevel);
            }
            return;
        }
        if (getTarget() == null || getTarget().getY() > getY()) {
            return;
        }
        BombEntity bomb = ModEntities.BOMB.get().create(serverLevel);
        if (bomb != null) {
            bomb.configure(this, 60, 0.0F, (float) getAttributeValue(Attributes.ATTACK_DAMAGE),
                    2, 1, false);
            bomb.moveTo(getX(), getY(), getZ(), getYRot(), getXRot() + 20.0F);
            serverLevel.addFreshEntity(bomb);
            spawnPayloadParticles(serverLevel);
        }
    }

    private void spawnPayloadParticles(ServerLevel level) {
        level.sendParticles(ModParticles.ASSIMILATION_SPLASH.get(), getX(), getY(), getZ(),
                8, 0.5D, 0.5D, 0.5D, 0.0D);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 0,
                state -> state.setAndContinue(AGE_IN_TICKS)));
    }

    public boolean isCharging() {
        return (entityData.get(FLIGHT_FLAGS) & 1) != 0;
    }

    private void setCharging(boolean charging) {
        byte flags = entityData.get(FLIGHT_FLAGS);
        entityData.set(FLIGHT_FLAGS, charging ? (byte) (flags | 1) : (byte) (flags & ~1));
    }

    private final class DropPayloadGoal extends Goal {
        private int checkTicks;

        @Override
        public boolean canUse() {
            checkTicks++;
            if (checkTicks < 20) {
                return false;
            }
            checkTicks = 0;
            LivingEntity target = getTarget();
            if (target == null) {
                return false;
            }
            if (!target.onGround()) {
                checkTicks = 10;
                return false;
            }
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            return dx * dx + dz * dz < 256.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            dropGnatBomb();
        }
    }

    private final class ChargeAttackGoal extends Goal {
        private ChargeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && random.nextInt(5) == 0
                    && distanceToSqr(target) > 4.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return moveControl.hasWanted() && isCharging() && target != null && target.isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                moveControl.setWantedPosition(target.getX(), target.getY() + 7.0D, target.getZ(), 0.2D);
                setCharging(true);
            }
        }

        @Override
        public void stop() {
            setCharging(false);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }
            if (getBoundingBox().intersects(target.getBoundingBox())) {
                doHurtTarget(target);
                setCharging(false);
                return;
            }
            double speed = distanceToSqr(target) < 9.0D ? 1.0D : 1.1D;
            moveControl.setWantedPosition(target.getX(), target.getY() + 7.0D, target.getZ(), speed);
        }
    }

    private final class RandomFlightGoal extends Goal {
        private RandomFlightGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !moveControl.hasWanted() && random.nextInt(7) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos center = blockPosition();
            int mode = 1;
            double speed = 0.5D;
            LivingEntity target = getTarget();
            if (target != null) {
                double distance = distanceToSqr(target);
                if (distance > 100.0D) {
                    center = target.blockPosition();
                    mode = 2;
                    speed += 0.25D;
                } else if (distance < 36.0D) {
                    center = target.blockPosition();
                    mode = 3;
                    speed += 0.25D;
                }
            }

            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos candidate = switch (mode) {
                    case 2 -> center.offset(random.nextInt(6) - 2, random.nextInt(7) - 2,
                            random.nextInt(6) - 2);
                    case 3 -> center.offset(random.nextInt(4) + 3, random.nextInt(5) + 4,
                            random.nextInt(4) + 3);
                    default -> center.offset(random.nextInt(15) - 7, random.nextInt(11) - 5,
                            random.nextInt(15) - 7);
                };
                if (!level().isEmptyBlock(candidate)) {
                    continue;
                }
                moveControl.setWantedPosition(candidate.getX() + 0.5D, candidate.getY() + 0.5D,
                        candidate.getZ() + 0.5D, speed);
                if (target == null) {
                    getLookControl().setLookAt(candidate.getX() + 0.5D, candidate.getY() + 0.5D,
                            candidate.getZ() + 0.5D, 180.0F, 20.0F);
                }
                break;
            }
        }
    }

    private final class FlightHeightLimitGoal extends Goal {
        private final int limit;

        private FlightHeightLimitGoal(int limit) {
            this.limit = limit;
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null ? target.getY() + limit > getY() : hasExceededGroundDistance()) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
            }
        }

        private boolean hasExceededGroundDistance() {
            BlockPos pos = blockPosition().below();
            for (int count = 1; count <= limit; count++, pos = pos.below()) {
                if (pos.getY() < level().getMinBuildHeight() || !level().getBlockState(pos).isAir()) {
                    return false;
                }
            }
            return true;
        }
    }

    private final class VerminMoveControl extends MoveControl {
        private VerminMoveControl() {
            super(VerminEntity.this);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            double dx = wantedX - getX();
            double dy = wantedY - getY();
            double dz = wantedZ - getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < getBbWidth()) {
                operation = Operation.WAIT;
                setDeltaMovement(getDeltaMovement().scale(0.5D));
                return;
            }
            setDeltaMovement(getDeltaMovement().add(dx / distance * 0.05D * speedModifier,
                    dy / distance * 0.05D * speedModifier,
                    dz / distance * 0.05D * speedModifier));
            LivingEntity target = getTarget();
            double faceX = target == null ? getDeltaMovement().x : target.getX() - getX();
            double faceZ = target == null ? getDeltaMovement().z : target.getZ() - getZ();
            float yaw = -((float) Mth.atan2(faceX, faceZ)) * Mth.RAD_TO_DEG;
            setYRot(yaw);
            yBodyRot = yaw;
        }
    }
}
