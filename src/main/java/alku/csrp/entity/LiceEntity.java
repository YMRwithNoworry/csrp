package alku.csrp.entity;

import alku.csrp.effect.EffectStacking;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

/** SRP 1.10.7 EntityViin: the short-lived flying vermin dropped by adapted Vermin. */
public final class LiceEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Byte> FLIGHT_FLAGS =
            SynchedEntityData.defineId(LiceEntity.class, EntityDataSerializers.BYTE);
    private static final int MAX_LIFESPAN_TICKS = 1_200;
    private static final float HIJACK_HEALTH_FRACTION = 0.5F;
    private static final int VIRAL_DURATION_TICKS = 120;
    private static final int VIRAL_AMPLIFIER = 2;

    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");

    private int lifespan;
    private boolean consumed;

    public LiceEntity(EntityType<? extends LiceEntity> type, Level level) {
        super(type, level);
        moveControl = new LiceMoveControl();
        setNoGravity(true);
        xpReward = 0;
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
    public boolean supportsDamageAdaptation() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLIGHT_FLAGS, (byte) 0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(3, new FlightAttackGoal());
        goalSelector.addGoal(4, new ChargeAttackGoal());
        goalSelector.addGoal(6, new RandomFlyGoal());
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 0,
                true, false, this::isValidParasiteTarget));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 0,
                true, false, this::isValidBaseMobTarget));
    }

    private boolean isValidBaseMobTarget(LivingEntity target) {
        return !(target instanceof WaterAnimal) && isValidParasiteTarget(target);
    }

    private boolean isValidFlightMobTarget(LivingEntity target) {
        return target instanceof Mob && isValidParasiteTarget(target)
                && !(target instanceof Animal) && !(target instanceof Creeper)
                && !(target instanceof WaterAnimal) && distanceToSqr(target) < 1024.0D
                && hasLineOfSight(target) && canAttack(target);
    }

    private boolean isValidPlayerTarget(Player player) {
        return isValidParasiteTarget(player) && !player.getAbilities().instabuild
                && !player.isSpectator() && canAttack(player);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide || consumed) {
            return;
        }
        if (++lifespan > MAX_LIFESPAN_TICKS) {
            expireAndDiscard();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public void push(Entity entity) {
        super.push(entity);
        if (entity == getTarget()) {
            performContactAttack(entity);
        }
    }

    private boolean performContactAttack(Entity entity) {
        if (level().isClientSide || consumed || entity != getTarget()
                || !(entity instanceof LivingEntity target)
                || target instanceof Parasite || !target.isAlive()) {
            return false;
        }
        boolean converted = false;
        if (target.getHealth() <= target.getMaxHealth() * HIJACK_HEALTH_FRACTION) {
            converted = InfectionMechanics.convertFeralEndermanHost(target)
                    || InfectionMechanics.convertGnatHost(target);
        }
        boolean damaged = false;
        if (!converted) {
            damaged = target.hurt(damageSources().mobAttack(this),
                    (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        contactAndDiscard(target, converted);
        return converted || damaged;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !consumed && level() instanceof ServerLevel serverLevel) {
            consumed = true;
            VerminParticles.sendType10Burst(serverLevel, this);
            discard();
            return;
        }
        super.tickDeath();
    }

    private void expireAndDiscard() {
        if (consumed || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        consumed = true;
        VerminParticles.sendType11Burst(serverLevel, this);
        discard();
    }

    private void contactAndDiscard(LivingEntity target, boolean converted) {
        if (consumed || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        consumed = true;
        VerminParticles.sendContactBursts(serverLevel, this, converted);
        EffectStacking.apply(target, ModMobEffects.VIRAL, VIRAL_DURATION_TICKS, VIRAL_AMPLIFIER);
        playSound(ModSounds.get("buthol.boom"), 0.4F,
                (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        discard();
    }

    private boolean isCharging() {
        return (entityData.get(FLIGHT_FLAGS) & 1) != 0;
    }

    private void setCharging(boolean charging) {
        byte flags = entityData.get(FLIGHT_FLAGS);
        entityData.set(FLIGHT_FLAGS, charging ? (byte) (flags | 1) : (byte) (flags & ~1));
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(0.8F);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0D, dimensions.height() * 0.5D, 0.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.get("mob.silence");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("mob.silence");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get("mob.silence");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(AGE_IN_TICKS)));
    }

    /** EntityAIFlightAttack, active during the original ten-tick AI work window. */
    private final class FlightAttackGoal extends Goal {
        private int lostTargetTicks;

        @Override
        public boolean canUse() {
            int cycleTick = tickCount % 21;
            return cycleTick > 0 && cycleTick <= 10;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                validateCurrentTarget(target);
                return;
            }

            lostTargetTicks = 0;
            double followRange = getAttributeValue(Attributes.FOLLOW_RANGE);
            AABB searchArea = new AABB(blockPosition()).inflate(followRange);
            for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class, searchArea)) {
                if (candidate instanceof Player player) {
                    if (isValidPlayerTarget(player)) {
                        setTarget(player);
                        return;
                    }
                } else if (isValidFlightMobTarget(candidate)) {
                    setTarget(candidate);
                    return;
                }
            }
        }

        private void validateCurrentTarget(LivingEntity target) {
            if (!isValidParasiteTarget(target)
                    || target instanceof Player player && (player.getAbilities().instabuild || player.isSpectator())) {
                clearTarget();
                return;
            }
            double followRange = getAttributeValue(Attributes.FOLLOW_RANGE);
            if (!hasLineOfSight(target) || distanceToSqr(target) >= followRange * followRange) {
                lostTargetTicks++;
            } else {
                lostTargetTicks = 0;
            }
            if (lostTargetTicks >= 6) {
                clearTarget();
            }
        }

        private void clearTarget() {
            setTarget(null);
            lostTargetTicks = 0;
            moveControl.setWantedPosition(getX(), getY(), getZ(), 1.0D);
        }
    }

    private final class ChargeAttackGoal extends Goal {
        private ChargeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && !getMoveControl().hasWanted()
                    && random.nextInt(7) == 0 && distanceToSqr(target) > 1.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return getMoveControl().hasWanted() && isCharging() && target != null && target.isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                Vec3 eye = target.getEyePosition();
                getMoveControl().setWantedPosition(eye.x, eye.y, eye.z, 2.0D);
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
            } else if (distanceToSqr(target) < 9.0D) {
                Vec3 eye = target.getEyePosition();
                getMoveControl().setWantedPosition(eye.x, eye.y, eye.z, 2.0D);
            }
        }
    }

    private final class RandomFlyGoal extends Goal {
        private RandomFlyGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !getMoveControl().hasWanted() && random.nextInt(7) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos origin = blockPosition();
            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos destination = origin.offset(random.nextInt(15) - 7,
                        random.nextInt(11) - 5, random.nextInt(15) - 7);
                if (level().isEmptyBlock(destination)) {
                    getMoveControl().setWantedPosition(destination.getX() + 0.5D,
                            destination.getY() + 0.5D, destination.getZ() + 0.5D, 0.25D);
                    if (getTarget() == null) {
                        getLookControl().setLookAt(destination.getX() + 0.5D,
                                destination.getY() + 0.5D, destination.getZ() + 0.5D, 180.0F, 20.0F);
                    }
                    break;
                }
            }
        }
    }

    private final class LiceMoveControl extends MoveControl {
        private LiceMoveControl() {
            super(LiceEntity.this);
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
            double arrivalRadius = (getBbWidth() * 2.0D + getBbHeight()) / 3.0D;
            if (distance < arrivalRadius) {
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
