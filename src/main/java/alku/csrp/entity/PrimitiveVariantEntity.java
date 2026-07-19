package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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
public final class PrimitiveVariantEntity extends PrimitiveParasiteEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");

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
        }
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
                health = 38.0D;
                armor = 4.0D;
                damage = 9.0D;
                speed = 0.30D;
                knockbackResistance = 0.20D;
                followRange = 36.0D;
            }
            case BOLSTER -> {
                health = 64.0D;
                armor = 12.0D;
                damage = 12.0D;
                speed = 0.19D;
                knockbackResistance = 0.80D;
                followRange = 32.0D;
            }
            case BURROWER -> {
                health = 44.0D;
                armor = 7.0D;
                damage = 11.0D;
                speed = 0.27D;
                knockbackResistance = 0.45D;
                followRange = 32.0D;
            }
            case DEVOURER -> {
                health = 36.0D;
                armor = 5.0D;
                damage = 10.0D;
                speed = 0.32D;
                knockbackResistance = 0.30D;
                followRange = 36.0D;
            }
            case MANDUCATER -> {
                health = 50.0D;
                armor = 10.0D;
                damage = 14.0D;
                speed = 0.25D;
                knockbackResistance = 0.65D;
                followRange = 40.0D;
            }
            case REEKER -> {
                health = 32.0D;
                armor = 3.0D;
                damage = 10.0D;
                speed = 0.34D;
                knockbackResistance = 0.15D;
                followRange = 40.0D;
            }
            case TOZOON -> {
                health = 56.0D;
                armor = 9.0D;
                damage = 13.0D;
                speed = 0.22D;
                knockbackResistance = 0.75D;
                followRange = 36.0D;
            }
            case YELLOWEYE -> {
                health = 30.0D;
                armor = 3.0D;
                damage = 4.0D;
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
                goalSelector.addGoal(1, new BurrowAmbushGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.20D, false));
            }
            case DEVOURER -> goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, false));
            case MANDUCATER -> {
                goalSelector.addGoal(1, new AmbushLeapGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
            }
            case REEKER -> {
                goalSelector.addGoal(1, new ChargeGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.30D, false));
            }
            case TOZOON -> {
                goalSelector.addGoal(1, new BurrowAmbushGoal());
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
    }

    private PlayState movementAnimation(AnimationState<PrimitiveVariantEntity> state) {
        if (activeKind() == Kind.YELLOWEYE) {
            return state.setAndContinue(FLY);
        }
        if (!state.isMoving()) {
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
            if (level().destroyBlock(candidate, true, this)) {
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
            abilityCooldown = 90;
        }
    }

    private final class ChargeGoal extends Goal {
        private int chargeTicks;

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
            return chargeTicks < 18 && target != null && target.isAlive();
        }

        @Override
        public void start() {
            chargeTicks = 0;
            abilityCooldown = 100;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            Vec3 direction = target.position().subtract(position());
            if (direction.lengthSqr() > 0.001D) {
                direction = direction.normalize();
                setDeltaMovement(direction.x * 0.72D, getDeltaMovement().y, direction.z * 0.72D);
            }
            if (distanceToSqr(target) <= 6.25D) {
                doHurtTarget(target);
                hurtNearby(PrimitiveVariantEntity.this, 2.5D,
                        (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25F, true);
                chargeTicks = 18;
                return;
            }
            chargeTicks++;
        }
    }

    private final class BurrowAmbushGoal extends Goal {
        private BurrowAmbushGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && onGround() && target != null && target.isAlive()
                    && distanceToSqr(target) >= 16.0D && distanceToSqr(target) <= 256.0D
                    && hasBurrowableGround(blockPosition());
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
            for (int attempt = 0; attempt < 6; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = 1.5D + random.nextDouble() * 2.0D;
                double x = target.getX() + Math.cos(angle) * distance;
                double z = target.getZ() + Math.sin(angle) * distance;
                BlockPos destination = BlockPos.containing(x, target.getY(), z);
                if (!hasBurrowableGround(destination) || !level().getBlockState(destination).isAir()
                        || !level().getBlockState(destination.above()).isAir()) {
                    continue;
                }
                teleportTo(x, target.getY(), z);
                setDeltaMovement(Vec3.ZERO);
                abilityCooldown = 100;
                return;
            }
            abilityCooldown = 40;
        }
    }

    private boolean hasBurrowableGround(BlockPos position) {
        float totalHardness = 0.0F;
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos below = position.below(depth);
            BlockState state = level().getBlockState(below);
            float hardness = state.getDestroySpeed(level(), below);
            if (state.isAir() || !state.isSolidRender(level(), below) || hardness < 0.0F) {
                return false;
            }
            totalHardness += hardness;
        }
        return totalHardness < 10.0F;
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
