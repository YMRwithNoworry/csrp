package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class KirinEntity extends DerivedParasiteEntity {
    public static final int BLINK_CHARGE_TICKS = 60;
    public static final int BLINK_COOLDOWN_TICKS = 200;
    public static final double BLINK_LIFE_STEAL_RADIUS = 5.0;
    public static final double BLINK_HEALTH_DRAIN_FRACTION = 0.5;
    private static final int VOID_ORB_INTERVAL_TICKS = 240;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private static final EntityDataAccessor<BlockPos> BLINK_POS = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> BLINK_TICKS = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.INT);

    private int blinkCooldown;
    private int blinkCharge;
    private int voidOrbCooldown = 80;
    private BlockPos blinkDestination = BlockPos.ZERO;

    public KirinEntity(EntityType<? extends KirinEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 350;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 410.0)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 155.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FLYING_SPEED, 0.24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new KirinBlinkGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BLINK_POS, BlockPos.ZERO);
        builder.define(BLINK_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide) {
            spawnAmbientPortalParticles();
            spawnBlinkWarningParticles();
            return;
        }
        if (blinkCooldown > 0) blinkCooldown--;
        if (voidOrbCooldown > 0) voidOrbCooldown--;

        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            if (blinkCharge <= 0 && distanceToSqr(target) > 36.0) {
                getMoveControl().setWantedPosition(target.getX(), target.getY() + 1.5, target.getZ(), 0.8);
            }
            if (voidOrbCooldown <= 0 && hasLineOfSight(target)) {
                summonVoidOrb(target);
                voidOrbCooldown = VOID_ORB_INTERVAL_TICKS;
            }
        }
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().add(0.0, 0.08, 0.0));
        }
    }

    private void spawnAmbientPortalParticles() {
        for (int i = 0; i < 4; i++) {
            level().addParticle(ParticleTypes.PORTAL,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 3.0D,
                    getY() + random.nextDouble() * getBbHeight() - 0.25D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 3.0D,
                    (random.nextDouble() - 0.5D) * 0.4D, -random.nextDouble() * 0.2D,
                    (random.nextDouble() - 0.5D) * 0.4D);
        }
    }

    private void spawnBlinkWarningParticles() {
        int remainingTicks = entityData.get(BLINK_TICKS);
        BlockPos destination = entityData.get(BLINK_POS);
        if (remainingTicks <= 0 || destination.equals(BlockPos.ZERO)) {
            return;
        }

        float progress = 1.0F - remainingTicks / (float) BLINK_CHARGE_TICKS;
        double x = destination.getX() + 0.5D;
        double y = destination.getY() + 1.5D + progress;
        double z = destination.getZ() + 0.5D;
        double rotationSpeed = 0.35D + progress * 1.25D;
        float clockwise = (float) ((tickCount * rotationSpeed) % (Math.PI * 2.0D));
        float counterClockwise = (float) ((-tickCount * rotationSpeed) % (Math.PI * 2.0D));

        level().addParticle(ModParticles.KIRIN_WARNING.get(), x, y, z, 5.5D, clockwise, 1.0D);
        level().addParticle(ModParticles.KIRIN_WARNING.get(), x, y, z, 6.0D, counterClockwise, 1.0D);
    }

    private void summonVoidOrb(LivingEntity target) {
        ScaryOrbEntity orb = new ScaryOrbEntity(ModEntities.SCARY_ORB.get(), level(), this);
        orb.setAnchor(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
        level().addFreshEntity(orb);
        playSound(ModSounds.KIRIN_BLACK_HOLE.get(), 2.0F, 1.0F);
    }

    private void setBlinkCharge(BlockPos destination, int ticks) {
        blinkDestination = destination == null ? BlockPos.ZERO : destination.immutable();
        blinkCharge = Math.max(0, ticks);
        entityData.set(BLINK_POS, blinkDestination);
        entityData.set(BLINK_TICKS, blinkCharge);
    }

    private void clearBlinkCharge() {
        blinkCharge = 0;
        entityData.set(BLINK_POS, BlockPos.ZERO);
        entityData.set(BLINK_TICKS, 0);
    }

    private void performBlink(LivingEntity target) {
        Vec3 destination = Vec3.atBottomCenterOf(blinkDestination);
        teleportTo(destination.x, destination.y, destination.z);
        float totalStolen = 0.0F;
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(BLINK_LIFE_STEAL_RADIUS), this::isValidParasiteTarget)) {
            if (victim instanceof Player player && player.getAbilities().invulnerable) {
                continue;
            }
            float stolen = Math.max(1.0F, victim.getHealth() * (float) BLINK_HEALTH_DRAIN_FRACTION);
            if (victim.hurt(damageSources().indirectMagic(this, this), stolen)) {
                totalStolen += Math.min(stolen, victim.getHealth() + stolen);
            }
        }
        heal(totalStolen);
        target.hurt(damageSources().mobAttack(this),
                (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.25F);
    }

    private BlockPos findBlinkDestination(LivingEntity target) {
        for (int i = 0; i < 64; i++) {
            int x = target.blockPosition().getX() + random.nextInt(49) - 24;
            int z = target.blockPosition().getZ() + random.nextInt(49) - 24;
            int y = target.blockPosition().getY() + random.nextInt(9) - 4;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
            for (int scan = 0; scan < 12; scan++) {
                if (level().getBlockState(pos).isAir() && level().getBlockState(pos.above()).isAir()
                        && level().getBlockState(pos.below()).isSolid()) {
                    return pos.immutable();
                }
                pos.move(0, -1, 0);
            }
        }
        return target.blockPosition();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.KIRIN_LIVING.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.KIRIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KIRIN_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("blink_cooldown", blinkCooldown);
        tag.putInt("blink_charge", blinkCharge);
        tag.putInt("void_orb_cooldown", voidOrbCooldown);
        tag.putInt("blink_x", blinkDestination.getX());
        tag.putInt("blink_y", blinkDestination.getY());
        tag.putInt("blink_z", blinkDestination.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        blinkCooldown = tag.getInt("blink_cooldown");
        blinkCharge = tag.getInt("blink_charge");
        voidOrbCooldown = tag.getInt("void_orb_cooldown");
        blinkDestination = new BlockPos(tag.getInt("blink_x"), tag.getInt("blink_y"), tag.getInt("blink_z"));
        if (!level().isClientSide) {
            entityData.set(BLINK_POS, blinkDestination);
            entityData.set(BLINK_TICKS, blinkCharge);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(IDLE)));
    }

    private final class KirinBlinkGoal extends Goal {
        private KirinBlinkGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return blinkCooldown <= 0 && blinkCharge <= 0 && target != null && target.isAlive()
                    && distanceToSqr(target) >= 256.0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return blinkCharge > 0 && target != null && target.isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            setBlinkCharge(findBlinkDestination(target), BLINK_CHARGE_TICKS);
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getNavigation().stop();
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (level() instanceof ServerLevel serverLevel) {
                Vec3 destination = Vec3.atCenterOf(blinkDestination);
                serverLevel.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y, destination.z,
                        8, 0.8, 1.2, 0.8, 0.05);
            }
            if (--blinkCharge <= 0) {
                clearBlinkCharge();
                performBlink(target);
                blinkCooldown = BLINK_COOLDOWN_TICKS;
            } else {
                entityData.set(BLINK_TICKS, blinkCharge);
            }
        }

        @Override
        public void stop() {
            if (blinkCharge > 0) {
                clearBlinkCharge();
                blinkCooldown = 40;
            }
        }
    }
}
