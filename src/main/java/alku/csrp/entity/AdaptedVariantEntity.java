package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
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

/** Shared implementation for the legacy adapted parasite tier. */
public final class AdaptedVariantEntity extends BurrowingVariantEntity {
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation FLY = ParasiteAnimations.loop(this, "fly");
    private final RawAnimation DIG = ParasiteAnimations.loop(this, "func_78087_a.getDigging");

    private final Kind kind;
    private int abilityCooldown;
    private int supportCooldown;
    private int secondaryCooldown;
    private int blockBreakCooldown;
    private int rangedShots;
    private int cloakTicks;
    private boolean cloaked;

    public AdaptedVariantEntity(EntityType<? extends AdaptedVariantEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = 55;
        if (isFlying(kind)) {
            moveControl = new FlyingMoveControl(this, 20, true);
            setNoGravity(true);
        } else if (kind == Kind.DEVOURER) {
            moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 0.2F, true);
        }
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return 10;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return 0.10F;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return 8;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return 0.80F;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return 0.50F;
    }

    @Override
    protected float damageAdaptationEffectiveness() {
        Kind activeKind = activeKind();
        return activeKind == Kind.MANDUCATER || activeKind == Kind.YELLOWEYE ? 0.95F : 1.0F;
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
                health = 95.0D;
                armor = 12.0D;
                damage = 16.0D;
                speed = 0.31D;
                knockbackResistance = 0.50D;
                followRange = 40.0D;
            }
            case BOLSTER -> {
                health = 180.0D;
                armor = 20.0D;
                damage = 28.0D;
                speed = 0.17D;
                knockbackResistance = 0.90D;
                followRange = 40.0D;
            }
            case BURROWER -> {
                health = 110.0D;
                armor = 14.0D;
                damage = 20.0D;
                speed = 0.34D;
                knockbackResistance = 0.55D;
                followRange = 40.0D;
            }
            case DEVOURER -> {
                health = 100.0D;
                armor = 13.0D;
                damage = 23.0D;
                speed = 0.32D;
                knockbackResistance = 0.40D;
                followRange = 40.0D;
            }
            case LONGARMS -> {
                health = 150.0D;
                armor = 16.0D;
                damage = 26.0D;
                speed = 0.31D;
                knockbackResistance = 0.70D;
                followRange = 48.0D;
            }
            case MANDUCATER -> {
                health = 135.0D;
                armor = 17.0D;
                damage = 35.0D;
                speed = 0.27D;
                knockbackResistance = 0.75D;
                followRange = 40.0D;
            }
            case REEKER -> {
                health = 115.0D;
                armor = 12.0D;
                damage = 25.0D;
                speed = 0.36D;
                knockbackResistance = 0.55D;
                followRange = 48.0D;
            }
            case SUMMONER -> {
                health = 120.0D;
                armor = 14.0D;
                damage = 18.0D;
                speed = 0.28D;
                knockbackResistance = 0.60D;
                followRange = 40.0D;
            }
            case TOZOON -> {
                health = 130.0D;
                armor = 15.0D;
                damage = 23.0D;
                speed = 0.31D;
                knockbackResistance = 0.65D;
                followRange = 40.0D;
            }
            case VERMIN -> {
                health = 100.0D;
                armor = 10.0D;
                damage = 12.0D;
                speed = 0.25D;
                knockbackResistance = 0.35D;
                followRange = 40.0D;
            }
            case VISCERA -> {
                health = 130.0D;
                armor = 16.0D;
                damage = 24.0D;
                speed = 0.31D;
                knockbackResistance = 0.65D;
                followRange = 40.0D;
            }
            case YELLOWEYE -> {
                health = 90.0D;
                armor = 12.0D;
                damage = 14.0D;
                speed = 0.30D;
                knockbackResistance = 0.35D;
                followRange = 48.0D;
            }
            default -> throw new IllegalStateException("Unexpected adapted kind: " + kind);
        }

        AttributeSupplier.Builder attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, followRange);
        if (isFlying(kind)) {
            attributes.add(Attributes.FLYING_SPEED, 0.35D);
        }
        return attributes;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (isDevourerType(getType())) {
            return new WaterBoundPathNavigation(this, level);
        }
        if (!isFlyingType(getType())) {
            return super.createNavigation(level);
        }
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        switch (activeKind()) {
            case ARACHNIDA -> {
                goalSelector.addGoal(1, new WebPullGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.20D, false));
            }
            case BOLSTER -> {
                goalSelector.addGoal(1, new BolsterSupportGoal());
                goalSelector.addGoal(2, new BarrageGoal());
                goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.95D, false));
            }
            case BURROWER -> {
                goalSelector.addGoal(1, createBurrowMovementGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.30D, false));
            }
            case TOZOON -> {
                goalSelector.addGoal(1, createBurrowMovementGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.30D, false));
            }
            case DEVOURER -> {
                goalSelector.addGoal(1, new TryFindWaterGoal(this));
                goalSelector.addGoal(2, new DevourerMeleeGoal());
                goalSelector.addGoal(6, new RandomSwimmingGoal(this, 1.0D, 20));
            }
            case LONGARMS -> {
                goalSelector.addGoal(1, new ShockwaveGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, false));
            }
            case MANDUCATER -> {
                goalSelector.addGoal(1, new CloakGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.20D, false));
            }
            case REEKER -> {
                goalSelector.addGoal(1, new ChargeGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, false));
            }
            case SUMMONER -> {
                goalSelector.addGoal(1, new SummonGoal());
                goalSelector.addGoal(2, new VomitGoal());
                goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.05D, false));
            }
            case VERMIN -> goalSelector.addGoal(1, new VerminFlightGoal());
            case VISCERA -> {
                goalSelector.addGoal(1, new SideLeapGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, false));
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
        if (isFlying(activeKind)) {
            setNoGravity(true);
        }
        if (level().isClientSide) {
            return;
        }
        if (abilityCooldown > 0) abilityCooldown--;
        if (supportCooldown > 0) supportCooldown--;
        if (secondaryCooldown > 0) secondaryCooldown--;
        if (blockBreakCooldown > 0) blockBreakCooldown--;
        updateCloak();

        LivingEntity target = getTarget();
        if (target != null && breaksSoftBlocks(activeKind)) {
            breakSoftBlockTowards(target);
        }
        if (activeKind == Kind.DEVOURER) {
            if (!isInWaterOrBubble() && tickCount % 40 == 0) {
                hurt(damageSources().drown(), 3.0F);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (activeKind() == Kind.MANDUCATER && cloaked) {
            endCloak();
            abilityCooldown = 140;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 4.0F;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected int decreaseAirSupply(int airSupply) {
        return activeKind() == Kind.DEVOURER ? getMaxAirSupply() : super.decreaseAirSupply(airSupply);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        Kind activeKind = activeKind();
        if (activeKind == Kind.DEVOURER && !isInWaterOrBubble()) {
            return false;
        }
        if (!(entity instanceof LivingEntity target)) {
            return super.doHurtTarget(entity);
        }

        boolean hit;
        if (activeKind == Kind.MANDUCATER && cloaked) {
            hit = target.hurt(damageSources().mobAttack(this), meleeDamage() * 4.0F);
            if (hit) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4), this);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), this);
            }
            endCloak();
            abilityCooldown = 140;
        } else if (activeKind == Kind.LONGARMS) {
            hit = target.hurt(damageSources().mobAttack(this), meleeDamage());
        } else {
            hit = super.doHurtTarget(entity);
        }
        if (!hit) {
            return false;
        }

        switch (activeKind) {
            case BOLSTER -> hurtNearby(this, 2.75D, meleeDamage() * 0.75F, true);
            case LONGARMS -> hurtNearby(this, 3.25D, meleeDamage() * 0.80F, true);
            case REEKER -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1), this);
            case VISCERA -> target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 100, 0), this);
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean onClimbable() {
        return (activeKind() == Kind.ARACHNIDA || activeKind() == Kind.LONGARMS) && horizontalCollision
                || super.onClimbable();
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return !isFlying(activeKind()) && super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    private PlayState movementAnimation(AnimationState<AdaptedVariantEntity> state) {
        Kind kind = activeKind();
        if (supportsBurrowing() && isBurrowing()) {
            return state.setAndContinue(DIG);
        }
        if (kind == Kind.VERMIN) {
            return state.setAndContinue(IDLE);
        }
        if (isFlying(kind)) {
            return state.setAndContinue(FLY);
        }
        if (!state.isMoving()) {
            return state.setAndContinue(IDLE);
        }
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
    }

    private float meleeDamage() {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (activeKind() == Kind.LONGARMS) {
            damage *= 1.0F + (1.0F - getHealth() / getMaxHealth());
        }
        return damage;
    }

    private void updateCloak() {
        if (!cloaked || --cloakTicks > 0) {
            return;
        }
        endCloak();
        abilityCooldown = 140;
    }

    private void endCloak() {
        cloaked = false;
        cloakTicks = 0;
        setInvisible(false);
    }

    private void breakSoftBlockTowards(LivingEntity target) {
        if (blockBreakCooldown > 0 || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.01D) {
            return;
        }
        horizontal = horizontal.normalize();
        double reach = activeKind() == Kind.BOLSTER || activeKind() == Kind.MANDUCATER
                || activeKind() == Kind.VISCERA || activeKind() == Kind.YELLOWEYE ? 1.7D : 1.0D;
        BlockPos origin = BlockPos.containing(getX() + horizontal.x * reach,
                getY() + getBbHeight() * 0.5D, getZ() + horizontal.z * reach);
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                    || hardness < 0.0F || hardness > blockBreakHardness()) {
                continue;
            }
            if (level().destroyBlock(candidate, true, this)) {
                blockBreakCooldown = 20;
            }
            return;
        }
    }

    private float blockBreakHardness() {
        return activeKind() == Kind.BOLSTER ? 3.5F : 3.0F;
    }

    private static boolean breaksSoftBlocks(Kind kind) {
        return kind != Kind.BURROWER && kind != Kind.DEVOURER && kind != Kind.TOZOON && !isFlying(kind);
    }

    private void pullTargets(double radius, double strength) {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius), this::isValidParasiteTarget)) {
            Vec3 pull = position().subtract(target.position());
            if (pull.lengthSqr() > 0.001D) {
                pull = pull.normalize().scale(strength);
                target.push(pull.x, 0.08D, pull.z);
            }
        }
    }

    private void fireProjectile(LivingEntity target, ParasiteProjectileEntity.Mode mode, double speed,
            float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.55D));
        projectile.configure(this, mode, start, target.getEyePosition(), speed, damage, radius, lifetime);
        level().addFreshEntity(projectile);
    }

    private void summonPrimitiveMinions() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int index = 0; index < 3; index++) {
            Mob minion = switch (random.nextInt(4)) {
                case 0 -> ModEntities.PRI_ARACHNIDA.get().create(serverLevel);
                case 1 -> ModEntities.PRI_REEKER.get().create(serverLevel);
                case 2 -> ModEntities.PRI_VISCERA.get().create(serverLevel);
                default -> ModEntities.PRI_LONGARMS.get().create(serverLevel);
            };
            if (minion == null) {
                continue;
            }
            double angle = Math.PI * 2.0D * index / 3.0D;
            minion.moveTo(getX() + Math.cos(angle) * 2.5D, getY(), getZ() + Math.sin(angle) * 2.5D,
                    getYRot(), 0.0F);
            minion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(minion.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            minion.setTarget(getTarget());
            minion.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 600, 0), this);
            serverLevel.addFreshEntity(minion);
        }
    }

    private void spawnLice() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LiceEntity lice = ModEntities.LICE.get().create(serverLevel);
        if (lice == null) {
            return;
        }
        lice.moveTo(getX(), getY() - 0.5D, getZ(), getYRot(), 0.0F);
        lice.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(lice.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        lice.setTarget(getTarget());
        serverLevel.addFreshEntity(lice);
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        EntityType<?> type = getType();
        if (type == ModEntities.ADA_BOLSTER.get()) return Kind.BOLSTER;
        if (type == ModEntities.ADA_BURROWER.get()) return Kind.BURROWER;
        if (type == ModEntities.ADA_DEVOURER.get()) return Kind.DEVOURER;
        if (type == ModEntities.ADA_LONGARMS.get()) return Kind.LONGARMS;
        if (type == ModEntities.ADA_MANDUCATER.get()) return Kind.MANDUCATER;
        if (type == ModEntities.ADA_REEKER.get()) return Kind.REEKER;
        if (type == ModEntities.ADA_SUMMONER.get()) return Kind.SUMMONER;
        if (type == ModEntities.ADA_TOZOON.get()) return Kind.TOZOON;
        if (type == ModEntities.ADA_VERMIN.get()) return Kind.VERMIN;
        if (type == ModEntities.ADA_VISCERA.get()) return Kind.VISCERA;
        if (type == ModEntities.ADA_YELLOWEYE.get()) return Kind.YELLOWEYE;
        return Kind.ARACHNIDA;
    }

    @Override
    protected boolean supportsBurrowing() {
        Kind activeKind = activeKind();
        return activeKind == Kind.BURROWER || activeKind == Kind.TOZOON;
    }

    @Override
    protected int burrowSkillCooldownTicks() {
        return activeKind() == Kind.BURROWER ? 80 : 140;
    }

    @Override
    protected SoundEvent burrowSound() {
        return activeKind() == Kind.BURROWER
                ? ModSounds.ADAPTED_BURROWER_DIG.get()
                : ModSounds.ADAPTED_TOZOON_DIG.get();
    }

    private static boolean isFlying(Kind kind) {
        return kind == Kind.VERMIN || kind == Kind.YELLOWEYE;
    }

    private static boolean isFlyingType(EntityType<?> type) {
        return type == ModEntities.ADA_VERMIN.get() || type == ModEntities.ADA_YELLOWEYE.get();
    }

    private static boolean isDevourerType(EntityType<?> type) {
        return type == ModEntities.ADA_DEVOURER.get();
    }

    private final class DevourerMeleeGoal extends MeleeAttackGoal {
        private DevourerMeleeGoal() {
            super(AdaptedVariantEntity.this, 1.30D, false);
        }

        @Override
        public boolean canUse() {
            return isInWaterOrBubble() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return isInWaterOrBubble() && super.canContinueToUse();
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
                    && distanceToSqr(target) >= 9.0D && distanceToSqr(target) <= 400.0D;
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
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), AdaptedVariantEntity.this);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), AdaptedVariantEntity.this);
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2), AdaptedVariantEntity.this);
            Vec3 pull = position().subtract(target.position());
            if (pull.lengthSqr() > 0.001D) {
                pull = pull.normalize().scale(0.65D);
                target.push(pull.x, 0.12D, pull.z);
            }
            abilityCooldown = 70;
        }
    }

    private final class BolsterSupportGoal extends Goal {
        private BolsterSupportGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return supportCooldown <= 0 && getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(24.0D), entity -> entity instanceof Parasite && entity.isAlive())) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 3), AdaptedVariantEntity.this);
                ally.clearFire();
            }
            supportCooldown = 1200;
        }
    }

    private final class BarrageGoal extends Goal {
        private int barrageTicks;

        private BarrageGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && onGround() && target != null && distanceToSqr(target) <= 196.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return barrageTicks < 60 && target != null && target.isAlive();
        }

        @Override
        public void start() {
            barrageTicks = 0;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            barrageTicks++;
            if (barrageTicks % 10 == 0) {
                hurtNearby(AdaptedVariantEntity.this, 8.0D, meleeDamage() * 0.55F, true);
                pullTargets(8.0D, 0.22D);
            }
        }

        @Override
        public void stop() {
            abilityCooldown = 300;
        }
    }

    private final class ShockwaveGoal extends Goal {
        private int chargeTicks;

        private ShockwaveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && onGround() && target != null && distanceToSqr(target) <= 256.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return chargeTicks < 40 && getTarget() != null;
        }

        @Override
        public void start() {
            chargeTicks = 0;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (++chargeTicks == 30) {
                hurtNearby(AdaptedVariantEntity.this, 10.0D, meleeDamage() * 1.20F, true);
            }
        }

        @Override
        public void stop() {
            abilityCooldown = 180;
        }
    }

    private final class CloakGoal extends Goal {
        private CloakGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && !cloaked && target != null && target.isAlive()
                    && getHealth() >= getMaxHealth() * 0.40F && distanceToSqr(target) >= 16.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return cloaked && cloakTicks > 0;
        }

        @Override
        public void start() {
            cloaked = true;
            cloakTicks = 80;
            setInvisible(true);
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
            return abilityCooldown <= 0 && onGround() && target != null
                    && distanceToSqr(target) >= 25.0D && distanceToSqr(target) <= 484.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return chargeTicks < 24 && target != null && target.isAlive();
        }

        @Override
        public void start() {
            chargeTicks = 0;
            abilityCooldown = 160;
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
                setDeltaMovement(direction.x * 0.88D, getDeltaMovement().y, direction.z * 0.88D);
            }
            if (distanceToSqr(target) <= 9.0D) {
                doHurtTarget(target);
                hurtNearby(AdaptedVariantEntity.this, 3.5D, meleeDamage() * 1.35F, true);
                chargeTicks = 24;
                return;
            }
            chargeTicks++;
        }
    }

    private final class SummonGoal extends Goal {
        private int castTicks;

        private SummonGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return abilityCooldown <= 0 && getTarget() != null && distanceToSqr(getTarget()) <= 400.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return castTicks < 40 && getTarget() != null;
        }

        @Override
        public void start() {
            castTicks = 0;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            if (++castTicks == 30) {
                summonPrimitiveMinions();
            }
        }

        @Override
        public void stop() {
            abilityCooldown = 360;
        }
    }

    private final class VomitGoal extends Goal {
        private VomitGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return secondaryCooldown <= 0 && target != null && hasLineOfSight(target)
                    && distanceToSqr(target) >= 16.0D && distanceToSqr(target) <= 576.0D;
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
            fireProjectile(target, ParasiteProjectileEntity.Mode.VOMIT, 0.75D, 9.0F, 2.4D, 90);
            secondaryCooldown = 100;
        }
    }

    private final class VerminFlightGoal extends Goal {
        private VerminFlightGoal() {
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
            getMoveControl().setWantedPosition(target.getX(), target.getY() + 4.0D, target.getZ(), 1.1D);
            if (abilityCooldown > 0 || distanceToSqr(target) > 576.0D) {
                return;
            }
            int liceCount = level().getEntitiesOfClass(LiceEntity.class, getBoundingBox().inflate(32.0D),
                    lice -> lice.getTarget() == target).size();
            if (liceCount >= 8) {
                fireProjectile(target, ParasiteProjectileEntity.Mode.BOMB, 0.75D, 12.0F, 2.5D, 80);
                abilityCooldown = 80;
            } else {
                spawnLice();
                spawnLice();
                abilityCooldown = 40;
            }
        }
    }

    private final class SideLeapGoal extends Goal {
        private SideLeapGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return abilityCooldown <= 0 && onGround() && target != null
                    && distanceToSqr(target) >= 9.0D && distanceToSqr(target) <= 144.0D;
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
                setDeltaMovement(-direction.z * 0.70D, 0.40D, direction.x * 0.70D);
            }
            abilityCooldown = 70;
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
            boolean acid = ++rangedShots % 5 == 0;
            if (acid) {
                fireProjectile(target, ParasiteProjectileEntity.Mode.ACID, 0.70D, 14.0F, 2.25D, 100);
                abilityCooldown = 90;
            } else {
                fireProjectile(target, ParasiteProjectileEntity.Mode.SPINE, 1.15D, 7.0F, 0.85D, 70);
                fireProjectile(target, ParasiteProjectileEntity.Mode.SPINE, 1.05D, 7.0F, 0.85D, 70);
                abilityCooldown = 36;
            }
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
            if (secondaryCooldown <= 0 && distanceToSqr(target) <= 4.0D) {
                doHurtTarget(target);
                secondaryCooldown = 20;
            }
        }
    }

    public enum Kind {
        ARACHNIDA,
        BOLSTER,
        BURROWER,
        DEVOURER,
        LONGARMS,
        MANDUCATER,
        REEKER,
        SUMMONER,
        TOZOON,
        VERMIN,
        VISCERA,
        YELLOWEYE
    }
}
