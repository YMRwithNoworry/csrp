package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.effect.EffectStacking;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.EnumSet;

/** SRP 1.10.7 EntityAta: the short-lived ground vermin dropped by primitive Vermin. */
public final class GnatEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Byte> CLIMBING =
            SynchedEntityData.defineId(GnatEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> COMBAT_STATUS =
            SynchedEntityData.defineId(GnatEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SKILL_LEAPING =
            SynchedEntityData.defineId(GnatEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_LIFETIME_TICKS = 1_200;
    private static final float HIJACK_HEALTH_FRACTION = 0.5F;
    private static final int VIRAL_DURATION_TICKS = 120;
    private static final int VIRAL_AMPLIFIER = 2;

    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");
    private final RawAnimation COMBAT_AGE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation COMBAT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation SPRINT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");

    private boolean consumed;

    public GnatEntity(EntityType<? extends GnatEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ARMOR, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.MOVEMENT_SPEED, 0.34559)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new SkillLeapGoal());
        goalSelector.addGoal(0, new SwimmingDivingGoal());
        goalSelector.addGoal(3, new FastMeleeAttackGoal());
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CLIMBING, (byte) 0);
        entityData.define(COMBAT_STATUS, false);
        entityData.define(SKILL_LEAPING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || consumed) {
            return;
        }

        LivingEntity target = getTarget();
        entityData.set(COMBAT_STATUS, target != null && target.isAlive());
        if (tickCount > MAX_LIFETIME_TICKS) {
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
        return distance >= 60.0F && super.causeFallDamage(distance, damageMultiplier, source);
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
        playSound(ModSounds.get("buthol.boom"), 0.3F,
                (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        EffectStacking.apply(target, ModMobEffects.VIRAL.get(), VIRAL_DURATION_TICKS, VIRAL_AMPLIFIER);
        discard();
    }

    @Override
    public boolean onClimbable() {
        return (entityData.get(CLIMBING) & 1) != 0;
    }

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose pose) {
        return 0.8F;
    }

    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0D, dimensions.height * 0.5D, 0.0D);
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.get("small.step"), getSoundVolume(), getVoicePitch());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 3,
                state -> {
                    if (entityData.get(SKILL_LEAPING)) {
                        return state.setAndContinue(LEAP);
                    }
                    boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
                    if (!moving) {
                        return state.setAndContinue(entityData.get(COMBAT_STATUS) ? COMBAT_AGE : AGE_IN_TICKS);
                    }
                    if (getDeltaMovement().horizontalDistanceSqr() > 0.02D) {
                        return state.setAndContinue(SPRINT_LIMB);
                    }
                    return state.setAndContinue(entityData.get(COMBAT_STATUS) ? COMBAT_LIMB : LIMB_SWING);
                }));
    }

    private final class FastMeleeAttackGoal extends MeleeAttackGoal {
        private FastMeleeAttackGoal() {
            super(GnatEntity.this, 1.3D, false);
        }

        @Override
        protected int getTicksUntilNextAttack() {
            return 6;
        }
    }

    /** EntityAISkill(type 14) plus EntityParasiteBase.skillLeap. */
    private final class SkillLeapGoal extends Goal {
        private static final int CHARGE_TICKS = 20;
        private static final double MIN_DISTANCE_SQR = 25.0D;
        private static final double MAX_DISTANCE_SQR = 10_000.0D;
        private int chargeTicks;
        private boolean armed;
        private boolean launched;
        private boolean sawAirborne;

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return armed || launched || target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            if (launched) {
                if (!onGround()) {
                    sawAirborne = true;
                } else if (sawAirborne) {
                    launched = false;
                    sawAirborne = false;
                    armed = false;
                    chargeTicks = 0;
                    entityData.set(SKILL_LEAPING, false);
                }
                return;
            }

            LivingEntity target = getTarget();
            if (armed) {
                if (target != null && target.isAlive() && onGround()
                        && !hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                    launchAt(target);
                }
                return;
            }
            if (target == null || !target.isAlive()) {
                return;
            }
            double distance = distanceToSqr(target);
            if (distance < MAX_DISTANCE_SQR && distance >= MIN_DISTANCE_SQR && hasLineOfSight(target)
                    && ++chargeTicks >= CHARGE_TICKS) {
                armed = true;
            }
        }

        private void launchAt(LivingEntity target) {
            double dx = target.getX() - getX();
            double dz = target.getZ() - getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            Vec3 movement = getDeltaMovement();
            double launchX = movement.x * 1.3D;
            double launchZ = movement.z * 1.3D;
            if (horizontalDistance > 1.0E-7D) {
                launchX += dx / horizontalDistance * 0.9D;
                launchZ += dz / horizontalDistance * 0.9D;
            }
            getNavigation().stop();
            setDeltaMovement(launchX, 0.4D, launchZ);
            launched = true;
            sawAirborne = false;
            entityData.set(SKILL_LEAPING, true);
        }
    }

    private final class SwimmingDivingGoal extends Goal {
        private SwimmingDivingGoal() {
            setFlags(EnumSet.of(Flag.JUMP));
            getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            if (!isInWaterOrBubble() && !isInLava()) {
                return false;
            }
            LivingEntity target = getTarget();
            if (target != null && (target.isInWaterOrBubble() || target.isInLava())
                    && distanceToSqr(getX(), target.getY(), getZ()) < 25.0D
                    && target.getY() - getY() < -1.0D) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.12D, 0.0D));
                return false;
            }
            return true;
        }

        @Override
        public void tick() {
            if (random.nextFloat() < 0.8F) {
                getJumpControl().jump();
            }
        }
    }
}
