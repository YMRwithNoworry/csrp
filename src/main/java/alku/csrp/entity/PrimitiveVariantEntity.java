package alku.csrp.entity;

import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
    private static final EntityDataAccessor<Integer> SPECIAL_ANIMATION_TICKS = SynchedEntityData.defineId(
            PrimitiveVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MANDUCATER_CAMOUFLAGED = SynchedEntityData.defineId(
            PrimitiveVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MANDUCATER_TARGET_ENTITY = SynchedEntityData.defineId(
            PrimitiveVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MANDUCATER_STATUS = SynchedEntityData.defineId(
            PrimitiveVariantEntity.class, EntityDataSerializers.INT);
    private static final int REEKER_CHARGE_NONE = 0;
    private static final int REEKER_CHARGE_WINDUP = 1;
    private static final int REEKER_CHARGING = 2;
    private static final int REEKER_WINDUP_TICKS = 20;
    private static final int REEKER_CHARGE_TICKS = 40;
    private static final int MANDUCATER_CAMOUFLAGE_CHECK_PERIOD = 21;
    private static final int MANDUCATER_PULL_MAX_TICKS = 200;
    private static final double MANDUCATER_PULL_MAX_DISTANCE_SQR = 9.0D;
    private static final double MANDUCATER_PULL_STRENGTH = 0.13D;
    private static final float MANDUCATER_MINIMUM_DAMAGE = 0.02F;

    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");
    private final RawAnimation AGE_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation LIMB_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation LIMB_STATUS_2 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation LIMB_STATUS_3 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_3");
    private final RawAnimation AGE_STATUS_3_STILL = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1");
    private final RawAnimation AGE_STATUS_3 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3");
    private final RawAnimation AGE_BODY_05 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_body_number_0_5");
    private final RawAnimation AGE_BODY_1 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_body_number_1");
    private final RawAnimation AGE_DEVOURER_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation DIG = ParasiteAnimations.loop(this,
            "get_dig_model.get_digging_1");
    private final RawAnimation DIG_BODY_05 = ParasiteAnimations.loop(this,
            "get_dig_model.get_body_number_0_5.get_digging_1");
    private final RawAnimation DIG_BODY_NEG_03 = ParasiteAnimations.loop(this,
            "get_dig_model.get_body_number_neg_0_3.get_digging_1");
    private final RawAnimation ATTACK_BODY_NEG_03 = ParasiteAnimations.play(this,
            "get_attack_timer.get_body_number_neg_0_3");
    private final RawAnimation ATTACK_BODY_1 = ParasiteAnimations.play(this,
            "get_attack_timer.get_body_number_1");
    private final RawAnimation TOZOON_ATTACK = ParasiteAnimations.play(this,
            "get_attack_timer");
    private final RawAnimation TOZOON_DIG = ParasiteAnimations.loop(this,
            "get_dig_model");
    private final RawAnimation DIG_BODY_1 = ParasiteAnimations.loop(this,
            "get_dig_model.get_body_number_1.get_digging_1");
    private final RawAnimation REEKER_WINDUP = AGE_STATUS_3_STILL;
    private final RawAnimation REEKER_CHARGE = LIMB_STATUS_3;
    private final RawAnimation[] BODY_ATTACK = {
            ATTACK_BODY_NEG_03,
            ATTACK_BODY_1,
            ATTACK_BODY_NEG_03
    };

    private final Kind kind;
    private int abilityCooldown;
    private int rangedShots;
    private int manducaterCamouflageTimer;
    private int manducaterPullTicks;
    private LivingEntity manducaterTarget;

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
        builder.define(SPECIAL_ANIMATION_TICKS, 0);
        builder.define(MANDUCATER_CAMOUFLAGED, false);
        builder.define(MANDUCATER_TARGET_ENTITY, 0);
        builder.define(MANDUCATER_STATUS, 0);
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
                speed = 0.35D;
                knockbackResistance = 0.50D;
                followRange = 24.0D;
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
                goalSelector.addGoal(2, new ManducaterWaterLeapGoal());
                goalSelector.addGoal(2, new ManducaterEvadeGoal());
                goalSelector.addGoal(3, new ManducaterMeleeGoal());
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
            if (activeKind == Kind.MANDUCATER) {
                applyManducaterPullMotion();
            }
            return;
        }
        if (abilityCooldown > 0) {
            abilityCooldown--;
        }
        int specialTicks = entityData.get(SPECIAL_ANIMATION_TICKS);
        if (specialTicks > 0) {
            entityData.set(SPECIAL_ANIMATION_TICKS, specialTicks - 1);
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
        if (activeKind == Kind.MANDUCATER) {
            tickManducater();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (activeKind() == Kind.MANDUCATER) {
            setManducaterCamouflaged(false);
            manducaterCamouflageTimer = 0;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 4.0F;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (activeKind() == Kind.MANDUCATER
                && (entityData.get(MANDUCATER_STATUS) != 0 || hasEffect(MobEffects.INVISIBILITY))) {
            return null;
        }
        return super.getAmbientSound();
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        Kind activeKind = activeKind();
        if (activeKind == Kind.DEVOURER && !isInWaterOrBubble()) {
            return false;
        }
        boolean stealthAttack = activeKind == Kind.MANDUCATER && isManducaterCamouflaged();
        boolean hit = super.doHurtTarget(entity);
        if (!hit || !(entity instanceof LivingEntity target)) {
            return hit;
        }
        if (activeKind == Kind.TOZOON) {
            triggerAnim("attack_controller", "get_attack_timer");
        }

        if (stealthAttack) {
            applyManducaterStealthDamage(target);
            setManducaterCamouflaged(false);
            manducaterCamouflageTimer = 0;
        }
        if (activeKind == Kind.MANDUCATER) {
            entityData.set(MANDUCATER_STATUS, 1);
            if (entityData.get(MANDUCATER_TARGET_ENTITY) == 0) {
                setManducaterTarget(target);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 3, false, false), this);
            }
        }

        switch (activeKind) {
            case ARACHNIDA -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), this);
            case DEVOURER -> target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 100, 0), this);
            case MANDUCATER -> {
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

    private void tickManducater() {
        tickManducaterCamouflage();

        LivingEntity target = getManducaterTarget();
        if (target == null || !target.isAlive() || getTarget() != target || !hasLineOfSight(target)
                || distanceToSqr(target) <= 0.0D) {
            clearManducaterTarget();
            updateManducaterStatus();
            return;
        }

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false), this);
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 1, false, false), this);
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        applyManducaterMinimumDamage(target);
        entityData.set(MANDUCATER_STATUS, 3);
        manducaterPullTicks++;

        if (manducaterPullTicks > MANDUCATER_PULL_MAX_TICKS
                || distanceToSqr(target) > MANDUCATER_PULL_MAX_DISTANCE_SQR) {
            clearManducaterTarget();
            updateManducaterStatus();
            return;
        }
        applyManducaterPullMotion();
    }

    private void tickManducaterCamouflage() {
        if (tickCount % MANDUCATER_CAMOUFLAGE_CHECK_PERIOD != 10) {
            return;
        }

        double healthRatio = getMaxHealth() <= 0.0F ? 0.0D : getHealth() / getMaxHealth();
        if (isManducaterCamouflaged()) {
            addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 25, 0, false, false));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2, false, false));
            if (tickCount % 2 == 0) {
                playSound(ModSounds.get("hull.c"), 0.2F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
            }
            if (healthRatio < MobsConfig.manducaterNeededHealth()) {
                setManducaterCamouflaged(false);
            }
            return;
        }

        if (healthRatio >= MobsConfig.manducaterNeededHealth()) {
            manducaterCamouflageTimer++;
            if (manducaterCamouflageTimer > MobsConfig.manducaterNeededTime()) {
                setManducaterCamouflaged(true);
                addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 25, 0, false, false));
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2, false, false));
                spawnManducaterCamouflageParticles();
                manducaterCamouflageTimer = 0;
            }
        }
    }

    private void spawnManducaterCamouflageParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ColorParticleOption cloud = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
                127.0F / 255.0F, 0.0F, 0.0F);
        serverLevel.sendParticles(cloud, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                11, getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.08D);
    }

    private void applyManducaterStealthDamage(LivingEntity target) {
        DamageSource source = damageSources().mobAttack(this);
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (float) MobsConfig.manducaterStealthDamageMultiplier();
        if (level() instanceof ServerLevel serverLevel) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, getWeaponItem(), target, source, damage);
        }
        target.hurt(source, damage);
    }

    private void applyManducaterMinimumDamage(LivingEntity target) {
        float damage = MANDUCATER_MINIMUM_DAMAGE;
        float absorption = target.getAbsorptionAmount();
        if (absorption > 0.0F) {
            target.setHealth(target.getHealth() - damage * 0.5F);
            target.setAbsorptionAmount(Math.max(0.0F, absorption - damage * 0.5F));
        } else {
            target.setHealth(target.getHealth() - damage);
        }
        if (target.isDeadOrDying()) {
            target.die(damageSources().mobAttack(this));
        }
    }

    private void applyManducaterPullMotion() {
        LivingEntity target = getManducaterTarget();
        if (target == null) {
            return;
        }
        target.stopRiding();
        Vec3 pull = position().subtract(target.position());
        if (pull.lengthSqr() <= 0.0D) {
            return;
        }
        pull = pull.normalize().scale(MANDUCATER_PULL_STRENGTH);
        target.push(pull.x, pull.y, pull.z);
    }

    private LivingEntity getManducaterTarget() {
        int entityId = entityData.get(MANDUCATER_TARGET_ENTITY);
        if (entityId == 0) {
            manducaterTarget = null;
            return null;
        }
        if (manducaterTarget != null && manducaterTarget.getId() == entityId) {
            return manducaterTarget;
        }
        Entity entity = level().getEntity(entityId);
        manducaterTarget = entity instanceof LivingEntity living ? living : null;
        return manducaterTarget;
    }

    private void setManducaterTarget(LivingEntity target) {
        manducaterTarget = target;
        manducaterPullTicks = 0;
        entityData.set(MANDUCATER_TARGET_ENTITY, target.getId());
    }

    private void clearManducaterTarget() {
        manducaterTarget = null;
        manducaterPullTicks = 0;
        entityData.set(MANDUCATER_TARGET_ENTITY, 0);
    }

    private boolean isManducaterCamouflaged() {
        return entityData.get(MANDUCATER_CAMOUFLAGED);
    }

    private void setManducaterCamouflaged(boolean camouflaged) {
        entityData.set(MANDUCATER_CAMOUFLAGED, camouflaged);
    }

    private void updateManducaterStatus() {
        int status = entityData.get(MANDUCATER_STATUS);
        if (status == 10 || entityData.get(MANDUCATER_TARGET_ENTITY) != 0) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            entityData.set(MANDUCATER_STATUS, 0);
            return;
        }
        boolean moving = getDeltaMovement().horizontalDistanceSqr() > 0.02D;
        entityData.set(MANDUCATER_STATUS, moving && distanceToSqr(target) > 64.0D ? 2 : 1);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == MANDUCATER_TARGET_ENTITY) {
            manducaterTarget = null;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (activeKind() == Kind.MANDUCATER) {
            tag.putBoolean("manducater_camouflaged", isManducaterCamouflaged());
            tag.putInt("manducater_camouflage_timer", manducaterCamouflageTimer);
            tag.putInt("manducater_pull_ticks", manducaterPullTicks);
            tag.putInt("manducater_status", entityData.get(MANDUCATER_STATUS));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (activeKind() == Kind.MANDUCATER) {
            setManducaterCamouflaged(tag.getBoolean("manducater_camouflaged"));
            manducaterCamouflageTimer = tag.getInt("manducater_camouflage_timer");
            manducaterPullTicks = tag.getInt("manducater_pull_ticks");
            entityData.set(MANDUCATER_STATUS, tag.getInt("manducater_status"));
            entityData.set(MANDUCATER_TARGET_ENTITY, 0);
            manducaterTarget = null;
        }
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
        if (activeKind() == Kind.TOZOON) {
            controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                    .triggerableAnim("get_attack_timer", TOZOON_ATTACK));
        }
    }

    private PlayState movementAnimation(AnimationState<PrimitiveVariantEntity> state) {
        if (getBodyNumber() > 0) {
            int body = Math.min(getBodyNumber(), BODY_ATTACK.length - 1);
            Kind activeKind = activeKind();
            if (activeKind == Kind.TOZOON && isBodyAttackAnimating()) {
                return state.setAndContinue(BODY_ATTACK[body]);
            }
            if (activeKind == Kind.BURROWER) {
                return state.setAndContinue(isBurrowing() ? DIG_BODY_05 : AGE_BODY_05);
            }
            return state.setAndContinue(isBurrowing()
                    ? body == 1 ? DIG_BODY_1 : DIG_BODY_NEG_03
                    : body == 1 ? AGE_BODY_1 : AGE_IN_TICKS);
        }
        if (supportsBurrowing() && isBurrowing()) {
            return state.setAndContinue(activeKind() == Kind.TOZOON ? TOZOON_DIG : DIG);
        }
        if (activeKind() == Kind.YELLOWEYE) {
            return state.setAndContinue(AGE_IN_TICKS);
        }
        if (activeKind() == Kind.DEVOURER) {
            LivingEntity target = getTarget();
            return state.setAndContinue(target != null && target.isAlive()
                    ? AGE_DEVOURER_STATUS_1 : AGE_IN_TICKS);
        }
        boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
        if (activeKind() == Kind.MANDUCATER) {
            return switch (entityData.get(MANDUCATER_STATUS)) {
                case 3, 10 -> PlayState.STOP;
                case 2 -> state.setAndContinue(LIMB_STATUS_2);
                case 1 -> state.setAndContinue(moving ? LIMB_STATUS_1 : AGE_STATUS_1);
                default -> state.setAndContinue(moving ? LIMB_SWING : AGE_IN_TICKS);
            };
        }
        if (activeKind() == Kind.ARACHNIDA && entityData.get(SPECIAL_ANIMATION_TICKS) > 0) {
            return state.setAndContinue(AGE_STATUS_3);
        }
        if (activeKind() == Kind.REEKER) {
            return switch (entityData.get(REEKER_CHARGE_STATE)) {
                case REEKER_CHARGE_WINDUP -> state.setAndContinue(REEKER_WINDUP);
                case REEKER_CHARGING -> state.setAndContinue(REEKER_CHARGE);
                default -> state.setAndContinue(selectGroundAnimation(moving));
            };
        }
        if (activeKind() == Kind.BOLSTER) {
            return state.setAndContinue(moving ? LIMB_SWING : AGE_IN_TICKS);
        }
        return state.setAndContinue(selectGroundAnimation(moving));
    }

    private RawAnimation selectGroundAnimation(boolean moving) {
        LivingEntity target = getTarget();
        boolean combat = target != null && target.isAlive();
        if (moving && getDeltaMovement().horizontalDistanceSqr() > 0.02D) {
            return LIMB_STATUS_2;
        }
        if (combat) {
            return moving ? LIMB_STATUS_1 : AGE_STATUS_1;
        }
        return moving ? LIMB_SWING : AGE_IN_TICKS;
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
            entityData.set(SPECIAL_ANIMATION_TICKS, 20);
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

    private final class ManducaterMeleeGoal extends MeleeAttackGoal {
        private int attackCooldown;

        private ManducaterMeleeGoal() {
            super(PrimitiveVariantEntity.this, 1.30D, false);
        }

        @Override
        public void start() {
            super.start();
            attackCooldown = 0;
        }

        @Override
        public void tick() {
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (attackCooldown <= 0 && mob.isWithinMeleeAttackRange(target)
                    && mob.getSensing().hasLineOfSight(target)) {
                attackCooldown = getAttackInterval();
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(target);
            }
        }

        @Override
        protected int getAttackInterval() {
            return 6;
        }
    }

    private final class ManducaterWaterLeapGoal extends Goal {
        private int attackTimer;
        private int attacking;
        private double targetX;
        private double targetY;
        private double targetZ;

        @Override
        public boolean canUse() {
            return isInWaterOrBubble() || attacking >= 1;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && entityData.get(MANDUCATER_STATUS) <= 2) {
                attackTimer++;
                if (attackTimer >= 20 && attacking == 0) {
                    attacking = 1;
                    targetX = target.getX();
                    targetZ = target.getZ();
                    targetY = Math.max(0.0D, (target.getY() - getY()) * 0.07D);
                }
            } else if (attackTimer > 0) {
                attackTimer--;
            }

            if (attacking < 1) {
                return;
            }
            attacking++;
            if (attacking == 2 && onGround()) {
                entityData.set(MANDUCATER_STATUS, 10);
                getNavigation().stop();
                double deltaX = targetX - getX();
                double deltaZ = targetZ - getZ();
                double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                Vec3 motion = getDeltaMovement();
                if (distance > 0.0D) {
                    setDeltaMovement(motion.x + deltaX / distance * 1.5D * 0.9D + motion.x * 0.3D,
                            0.7D + targetY,
                            motion.z + deltaZ / distance * 1.5D * 0.9D + motion.z * 0.3D);
                } else {
                    setDeltaMovement(motion.x, 0.7D + targetY, motion.z);
                }
            }

            if (attacking >= 3 && onGround()) {
                attacking = 0;
                attackTimer = 0;
                entityData.set(MANDUCATER_STATUS, 2);
            }
        }
    }

    private final class ManducaterEvadeGoal extends Goal {
        private int cooldown = 41;
        private int duration;
        private boolean evading;

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return evading || target != null && target.isAlive() && onGround()
                    && !hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                    && distanceToSqr(target) > 64.0D && distanceToSqr(target) < 225.0D
                    && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            cooldown = 0;
            duration = 0;
            evading = false;
            setXxa(0.0F);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (evading) {
                duration++;
                if (duration >= 5) {
                    setXxa(0.0F);
                    duration = 0;
                    cooldown = 0;
                    evading = false;
                }
                return;
            }
            if (target == null || hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                return;
            }

            double distanceSqr = distanceToSqr(target);
            if (distanceSqr <= 64.0D || distanceSqr >= 225.0D || !hasLineOfSight(target)) {
                return;
            }
            cooldown++;
            if (cooldown < 40) {
                return;
            }

            getLookControl().setLookAt(target, 30.0F, 30.0F);
            int strafe = random.nextBoolean() ? 1 : -1;
            setXxa(strafe);
            evading = true;
            Vec3 motion = getDeltaMovement();
            double deltaX = target.getX() - getX();
            double deltaZ = target.getZ() - getZ();
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (distance > 0.0D) {
                setDeltaMovement(motion.x + deltaX / distance * 1.77D * 0.8D + motion.x * 0.2D,
                        0.2D + getBbHeight() * 0.1D,
                        motion.z + deltaZ / distance * 1.77D * 0.8D + motion.z * 0.2D);
            }
            getNavigation().stop();
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
