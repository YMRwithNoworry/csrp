package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

/**
 * Shared implementation for the remaining legacy primitive parasites.
 *
 * <p>Each registered entity keeps its own type, attributes, model, loot, and
 * combat branch while sharing the common primitive adaptation state.</p>
 */
public final class PrimitiveVariantEntity extends BurrowingVariantEntity {
    private static final EntityDataAccessor<Integer> REEKER_CHARGE_STATE = SynchedEntityData.defineId(
            PrimitiveVariantEntity.class, EntityDataSerializers.INT);
    private static final int REEKER_CHARGE_NONE = 0;
    private static final int REEKER_CHARGE_WINDUP = 1;
    private static final int REEKER_CHARGING = 2;
    private static final int REEKER_WINDUP_TICKS = 20;
    private static final int REEKER_CHARGE_TICKS = 40;

    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation FLY = ParasiteAnimations.loop(this, "fly");
    private final RawAnimation DIG = ParasiteAnimations.loop(this, "func_78087_a.getDigging");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation DEVOURER_IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation DEVOURER_MOVEMENT = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation DEVOURER_ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation REEKER_WINDUP = ParasiteAnimations.loop(
            this, "idle.get_parasite_status_3.get_still_ani_1");
    private final RawAnimation REEKER_CHARGE = ParasiteAnimations.loop(this, "walk.get_parasite_status_3");
    private final RawAnimation[] BODY_IDLE = {
            ParasiteAnimations.loop(this, "idle"),
            ParasiteAnimations.loop(this, "idle.get_body_number_1"),
            ParasiteAnimations.loop(this, "idle.get_body_number_2")
    };
    private final RawAnimation[] BODY_DIG = {
            DIG,
            ParasiteAnimations.loop(this, "get_dig_model.get_body_number_1.get_digging_1"),
            ParasiteAnimations.loop(this, "get_dig_model.get_body_number_2.get_digging_1")
    };
    private final RawAnimation[] BODY_ATTACK = {
            ATTACK,
            ParasiteAnimations.loop(this, "get_attack_timer.get_body_number_1"),
            ParasiteAnimations.loop(this, "get_attack_timer.get_body_number_2")
    };

    private final Kind kind;
    private int abilityCooldown;
    private int rangedShots;

    public PrimitiveVariantEntity(EntityType<? extends PrimitiveVariantEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = 18;
        if (kind == Kind.YELLOWEYE) {
            moveControl = new FlyingMoveControl(this, 20, true);
            setNoGravity(true);
        } else if (kind == Kind.DEVOURER) {
            moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 0.2F, true);
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (getType() == ModEntities.PRI_DEVOURER.get()) {
            return new WaterBoundPathNavigation(this, level);
        }
        return super.createNavigation(level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REEKER_CHARGE_STATE, REEKER_CHARGE_NONE);
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        double health;
        double armor;
        double damage;
        double speed;
        double knockbackResistance;
        double followRange;

        switch (kind) {
            case ARACHNIDA -> {
                health = 35.0D;
                armor = 4.0D;
                damage = 15.0D;
                speed = 0.30D;
                knockbackResistance = 0.20D;
                followRange = 36.0D;
            }
            case BOLSTER -> {
                health = 35.0D;
                armor = 4.0D;
                damage = 6.0D;
                speed = 0.19D;
                knockbackResistance = 0.80D;
                followRange = 32.0D;
            }
            case BURROWER -> {
                health = 45.0D;
                armor = 9.0D;
                damage = 15.0D;
                speed = 0.27D;
                knockbackResistance = 0.45D;
                followRange = 32.0D;
            }
            case DEVOURER -> {
                health = 60.0D;
                armor = 4.0D;
                damage = 20.0D;
                speed = 0.32D;
                knockbackResistance = 0.30D;
                followRange = 36.0D;
            }
            case MANDUCATER -> {
                health = 30.0D;
                armor = 4.0D;
                damage = 12.0D;
                speed = 0.25D;
                knockbackResistance = 0.65D;
                followRange = 40.0D;
            }
            case REEKER -> {
                health = 40.0D;
                armor = 12.0D;
                damage = 12.0D;
                speed = 0.34D;
                knockbackResistance = 0.15D;
                followRange = 40.0D;
            }
            case TOZOON -> {
                health = 45.0D;
                armor = 9.0D;
                damage = 15.0D;
                speed = 0.22D;
                knockbackResistance = 0.75D;
                followRange = 36.0D;
            }
            case YELLOWEYE -> {
                health = 30.0D;
                armor = 3.5D;
                damage = 5.0D;
                speed = 0.25D;
                knockbackResistance = 0.15D;
                followRange = 48.0D;
            }
            default -> throw new IllegalStateException("Unexpected primitive kind: " + kind);
        }

        AttributeSupplier.Builder attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, followRange);
        if (kind == Kind.YELLOWEYE) {
            attributes.add(Attributes.FLYING_SPEED, 0.30D);
        }
        return attributes;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        switch (activeKind()) {
            case ARACHNIDA -> {
                goalSelector.addGoal(1, new WebPullGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
            }
            case BOLSTER -> {
                goalSelector.addGoal(1, new BolsterSupportGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.95D, false));
            }
            case BURROWER -> {
                goalSelector.addGoal(1, createBurrowMovementGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.20D, false));
            }
            case DEVOURER -> {
                goalSelector.addGoal(1, new TryFindWaterGoal(this));
                goalSelector.addGoal(2, new DevourerMeleeGoal());
                goalSelector.addGoal(6, new RandomSwimmingGoal(this, 1.0D, 20));
            }
            case MANDUCATER -> {
                goalSelector.addGoal(1, new AmbushLeapGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
            }
            case REEKER -> {
                goalSelector.addGoal(1, new ChargeGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.30D, false));
            }
            case TOZOON -> {
                goalSelector.addGoal(1, createBurrowMovementGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.00D, false));
            }
            case YELLOWEYE -> {
                goalSelector.addGoal(1, new YelloweyeRangedGoal());
                goalSelector.addGoal(2, new YelloweyeFlightGoal());
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        Kind activeKind = activeKind();
        if (activeKind == Kind.YELLOWEYE) {
            setNoGravity(true);
            if (!level().isClientSide && onGround()) {
                getMoveControl().setWantedPosition(getX(), getY() + 4.0D, getZ(), 0.6D);
            }
        }

        if (level().isClientSide) {
            return;
        }
        if (abilityCooldown > 0) {
            abilityCooldown--;
        }

        LivingEntity target = getTarget();
        if (target != null && breaksSoftBlocks(activeKind)) {
            breakSoftBlockTowards(target);
        } else if (activeKind == Kind.DEVOURER && target != null && isInWaterOrBubble()) {
            Vec3 direction = target.getEyePosition().subtract(getEyePosition());
            if (direction.lengthSqr() > 0.001D) {
                setDeltaMovement(getDeltaMovement().add(direction.normalize().scale(0.045D)));
            }
        }
        if (activeKind == Kind.DEVOURER && !isInWaterOrBubble() && tickCount % 40 == 0) {
            hurt(damageSources().drown(), 2.0F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 4.0F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (activeKind() == Kind.DEVOURER && !isInWaterOrBubble()) {
            return false;
        }
        boolean hit = super.doHurtTarget(entity);
        if (!hit || !(entity instanceof LivingEntity target)) {
            return hit;
        }
        triggerAnim("attack_controller", "attack");

        switch (activeKind()) {
            case ARACHNIDA -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), this);
            case DEVOURER -> target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 100, 0), this);
            case MANDUCATER -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
                if (random.nextFloat() < 0.20F) {
                    target.addEffect(new MobEffectInstance(ModMobEffects.COTH, 300, 0), this);
                }
            }
            case REEKER -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), this);
            case TOZOON -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean onClimbable() {
        return activeKind() == Kind.ARACHNIDA && horizontalCollision || super.onClimbable();
    }

    @Override
    protected int decreaseAirSupply(int airSupply) {
        return activeKind() == Kind.DEVOURER ? getMaxAirSupply() : super.decreaseAirSupply(airSupply);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return activeKind() != Kind.YELLOWEYE && super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        if (activeKind() == Kind.DEVOURER) {
            controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                    .triggerableAnim("attack", DEVOURER_ATTACK));
        } else {
            controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                    .triggerableAnim("attack", ATTACK));
        }
    }

    private PlayState movementAnimation(AnimationState<PrimitiveVariantEntity> state) {
        if (getBodyNumber() > 0) {
            int body = Math.min(getBodyNumber(), BODY_IDLE.length - 1);
            if (activeKind() == Kind.TOZOON && isBodyAttackAnimating()) {
                return state.setAndContinue(BODY_ATTACK[body]);
            }
            return state.setAndContinue(isBurrowing() ? BODY_DIG[body] : BODY_IDLE[body]);
        }
        if (supportsBurrowing() && isBurrowing()) {
            return state.setAndContinue(DIG);
        }
        if (activeKind() == Kind.YELLOWEYE) {
            return state.setAndContinue(FLY);
        }
        if (activeKind() == Kind.DEVOURER) {
            if (ParasiteAnimations.isMoving(this, state.isMoving())) {
                return state.setAndContinue(DEVOURER_MOVEMENT);
            }
            return state.setAndContinue(DEVOURER_IDLE);
        }
        if (activeKind() == Kind.REEKER) {
            return switch (entityData.get(REEKER_CHARGE_STATE)) {
                case REEKER_CHARGE_WINDUP -> state.setAndContinue(REEKER_WINDUP);
                case REEKER_CHARGING -> state.setAndContinue(REEKER_CHARGE);
                default -> state.setAndContinue(ParasiteAnimations.isMoving(this, state.isMoving()) ? WALK : IDLE);
            };
        }
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(IDLE);
        }
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
    }

    private void breakSoftBlockTowards(LivingEntity target) {
        if (abilityCooldown > 0 || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.01D) {
            return;
        }
        horizontal = horizontal.normalize();
        BlockPos origin = BlockPos.containing(getX() + horizontal.x * 0.8D,
                getY() + getBbHeight() * 0.5D, getZ() + horizontal.z * 0.8D);
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                    || hardness < 0.0F || hardness > 1.0F) {
                continue;
            }
            if (ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this)) {
                abilityCooldown = 60;
            }
            return;
        }
    }

    private static boolean breaksSoftBlocks(Kind kind) {
        return switch (kind) {
            case ARACHNIDA, BOLSTER, MANDUCATER, REEKER, YELLOWEYE -> true;
            default -> false;
        };
    }

    private void fireYelloweyeProjectile(LivingEntity target, boolean acid) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.45D));
        projectile.configure(this, acid ? ParasiteProjectileEntity.Mode.ACID : ParasiteProjectileEntity.Mode.SPINE,
                start, target.getEyePosition(), acid ? 0.65D : 1.0D, acid ? 9.0F : 5.0F,
                acid ? 1.8D : 0.75D, acid ? 90 : 60);
        level().addFreshEntity(projectile);
    }

    private void fireWebProjectile(LivingEntity target, int webKind) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.45D));
        projectile.configure(this, ParasiteProjectileEntity.Mode.WEB, start,
                target.getEyePosition(), 0.95D, 4.0F, 0.75D, 70, target);
        projectile.setWebKind(webKind);
        level().addFreshEntity(projectile);
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        EntityType<?> type = getType();
        if (type == ModEntities.PRI_BOLSTER.get()) return Kind.BOLSTER;
        if (type == ModEntities.PRI_BURROWER.get()) return Kind.BURROWER;
        if (type == ModEntities.PRI_DEVOURER.get()) return Kind.DEVOURER;
        if (type == ModEntities.PRI_MANDUCATER.get()) return Kind.MANDUCATER;
        if (type == ModEntities.PRI_REEKER.get()) return Kind.REEKER;
        if (type == ModEntities.PRI_TOZOON.get()) return Kind.TOZOON;
        if (type == ModEntities.PRI_YELLOWEYE.get()) return Kind.YELLOWEYE;
        return Kind.ARACHNIDA;
    }

    @Override
    protected boolean supportsBurrowing() {
        Kind activeKind = activeKind();
        return activeKind == Kind.BURROWER || activeKind == Kind.TOZOON;
    }

    @Override
    protected int burrowSkillCooldownTicks() {
        return activeKind() == Kind.BURROWER ? 140 : 200;
    }

    @Override
    protected int bodySegmentCount() {
        Kind kind = activeKind();
        return kind == Kind.BURROWER || kind == Kind.TOZOON ? 2 : 0;
    }

    @Override
    protected SoundEvent burrowSound() {
        return activeKind() == Kind.BURROWER
                ? ModSounds.PRIMITIVE_BURROWER_DIG.get()
                : ModSounds.PRIMITIVE_TOZOON_DIG.get();
    }

    private final class DevourerMeleeGoal extends MeleeAttackGoal {
        private DevourerMeleeGoal() {
            super(PrimitiveVariantEntity.this, 1.35D, false);
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return isInWaterOrBubble() && target != null && target.isInWaterOrBubble() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return isInWaterOrBubble() && target != null && target.isInWaterOrBubble()
                    && super.canContinueToUse();
        }
    }

    private final class WebPullGoal extends Goal {
        private WebPullGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && target != null && hasLineOfSight(target)
                    && distanceToSqr(target) >= 9.0D && distanceToSqr(target) <= 256.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), PrimitiveVariantEntity.this);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), PrimitiveVariantEntity.this);
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1), PrimitiveVariantEntity.this);
            Vec3 pull = position().subtract(target.position());
            if (pull.lengthSqr() > 0.001D) {
                pull = pull.normalize().scale(0.45D);
                target.push(pull.x, 0.10D, pull.z);
            }
            fireWebProjectile(target, 0);
            triggerAnim("attack_controller", "attack");
            abilityCooldown = 80;
        }
    }

    private final class BolsterSupportGoal extends Goal {
        private BolsterSupportGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return abilityCooldown <= 0 && getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(16.0D), entity -> entity instanceof Parasite && entity.isAlive())) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1), PrimitiveVariantEntity.this);
                ally.clearFire();
            }
            triggerAnim("attack_controller", "attack");
            abilityCooldown = 600;
        }
    }

    private final class AmbushLeapGoal extends Goal {
        private AmbushLeapGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && target != null && onGround()
                    && distanceToSqr(target) >= 16.0D && distanceToSqr(target) <= 196.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            Vec3 direction = target.position().subtract(position());
            if (direction.lengthSqr() > 0.001D) {
                direction = direction.normalize();
                setDeltaMovement(direction.x * 0.65D, 0.45D, direction.z * 0.65D);
            }
            triggerAnim("attack_controller", "attack");
            abilityCooldown = 90;
        }
    }

    private final class ChargeGoal extends Goal {
        private int chargeTicks;
        private Vec3 chargeDirection = Vec3.ZERO;

        private ChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && target != null && onGround()
                    && distanceToSqr(target) >= 25.0D && distanceToSqr(target) <= 400.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return chargeTicks < REEKER_WINDUP_TICKS + REEKER_CHARGE_TICKS
                    && target != null && target.isAlive();
        }

        @Override
        public void start() {
            chargeTicks = 0;
            abilityCooldown = 100;
            chargeDirection = Vec3.ZERO;
            entityData.set(REEKER_CHARGE_STATE, REEKER_CHARGE_WINDUP);
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        }

        @Override
        public void stop() {
            entityData.set(REEKER_CHARGE_STATE, REEKER_CHARGE_NONE);
            chargeDirection = Vec3.ZERO;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (chargeTicks < REEKER_WINDUP_TICKS) {
                getNavigation().stop();
                setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
                if (chargeTicks == 1) {
                    playSound(ParasiteSoundProfiles.ambient(PrimitiveVariantEntity.this), 4.0F, 2.0F);
                }
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, getRandomX(0.8D),
                            getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.8D),
                            2, 0.05D, 0.05D, 0.05D, 0.01D);
                }
                chargeTicks++;
                return;
            }
            if (chargeTicks == REEKER_WINDUP_TICKS) {
                chargeDirection = target.position().subtract(position());
                chargeDirection = new Vec3(chargeDirection.x, 0.0D, chargeDirection.z);
                if (chargeDirection.lengthSqr() > 0.001D) {
                    chargeDirection = chargeDirection.normalize();
                }
                entityData.set(REEKER_CHARGE_STATE, REEKER_CHARGING);
            }
            if (chargeDirection.lengthSqr() > 0.001D) {
                setDeltaMovement(chargeDirection.x * 0.72D, getDeltaMovement().y,
                        chargeDirection.z * 0.72D);
            }
            if (distanceToSqr(target) <= 6.25D) {
                doHurtTarget(target);
                hurtNearby(PrimitiveVariantEntity.this, 2.5D,
                        (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25F, true);
                chargeTicks = REEKER_WINDUP_TICKS + REEKER_CHARGE_TICKS;
                return;
            }
            chargeTicks++;
        }
    }

    private final class YelloweyeRangedGoal extends Goal {
        private YelloweyeRangedGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && target != null && target.isAlive() && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            boolean acid = ++rangedShots % 4 == 0;
            fireYelloweyeProjectile(target, acid);
            triggerAnim("attack_controller", "attack");
            abilityCooldown = acid ? 80 : 30;
        }
    }

    private final class YelloweyeFlightGoal extends Goal {
        private YelloweyeFlightGoal() {
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
            getMoveControl().setWantedPosition(target.getX(), target.getY() + 2.5D, target.getZ(), 1.0D);
        }
    }

    public enum Kind {
        ARACHNIDA,
        BOLSTER,
        BURROWER,
        DEVOURER,
        MANDUCATER,
        REEKER,
        TOZOON,
        YELLOWEYE
    }
}
