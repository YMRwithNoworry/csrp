package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.config.MobsConfig;
import alku.csrp.effect.EffectStacking;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.List;

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
    private static final EntityDataAccessor<Integer> REEKER_SKIN = SynchedEntityData.defineId(
            PrimitiveVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> REEKER_RICARDO_BALD = SynchedEntityData.defineId(
            PrimitiveVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DEVOURER_SKIN = SynchedEntityData.defineId(
            PrimitiveVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> YELLOWEYE_SKIN = SynchedEntityData.defineId(
            PrimitiveVariantEntity.class, EntityDataSerializers.INT);
    private static final int REEKER_CHARGE_NONE = 0;
    private static final int REEKER_CHARGE_WINDUP = 1;
    private static final int REEKER_CHARGING = 2;
    private static final int REEKER_SKIN_NORMAL = 0;
    private static final int REEKER_SKIN_FRAGILE = 1;
    private static final int REEKER_SKIN_VIRULENT = 5;
    private static final int REEKER_SKIN_BERSERKER = 6;
    private static final int REEKER_SKIN_HEAVY = 7;
    private static final int DEVOURER_SKIN_NORMAL = 0;
    private static final int DEVOURER_SKIN_HEAVY = 7;
    private static final int YELLOWEYE_SKIN_NORMAL = 0;
    private static final int YELLOWEYE_SKIN_HEAVY = 7;
    private static final int REEKER_SKILL_PREP_TICKS = 40;
    private static final int REEKER_WINDUP_TICKS = 20;
    private static final int REEKER_CHARGE_TICKS = 40;
    private static final int REEKER_ATTACK_INTERVAL = 10;
    private static final int REEKER_EVADE_COOLDOWN = 55;
    private static final int REEKER_EVADE_DURATION = 10;
    private static final double REEKER_EVADE_MIN_DISTANCE_SQR = 16.0D;
    private static final double REEKER_EVADE_MAX_DISTANCE_SQR = 225.0D;
    private static final int REEKER_DIVE_COOLDOWN_TICKS = 1200;
    private static final double REEKER_DIVE_SPEED = 3.8D;
    private static final float REEKER_DIVE_EXPLOSION = 3.0F;
    private static final int YELLOWEYE_WARNING_TICK = 70;
    private static final int YELLOWEYE_FIRE_TICK = 100;
    private static final double YELLOWEYE_MAX_ATTACK_DISTANCE_SQR = 4225.0D;
    private static final byte RICARDO_BURST_EVENT = 77;
    private static final double RICARDO_MAX_HEALTH = 3763.0D;
    private static final double RICARDO_NORMAL_ARMOR = 32.0D;
    private static final double RICARDO_ENRAGED_ARMOR = 40.0D;
    private static final double RICARDO_BERSERK_ARMOR = 44.0D;
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
    private int yelloweyeAttackTimer;
    private boolean yelloweyeShotFired;
    private int manducaterCamouflageTimer;
    private int manducaterPullTicks;
    private LivingEntity manducaterTarget;
    private int reekerChargePreparationTicks;

    public PrimitiveVariantEntity(EntityType<? extends PrimitiveVariantEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind == Kind.YELLOWEYE ? 30 : 18;
        if (kind == Kind.DEVOURER) {
            xpReward = 1 + random.nextInt(3);
        }
        if (kind == Kind.YELLOWEYE) {
            moveControl = new YelloweyeMoveControl(this);
            setNoGravity(true);
        } else if (kind == Kind.DEVOURER) {
            moveControl = new DevourerMoveControl(this);
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
    protected boolean usesDefaultMovementGoals() {
        return activeKind() != Kind.DEVOURER;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REEKER_CHARGE_STATE, REEKER_CHARGE_NONE);
        builder.define(SPECIAL_ANIMATION_TICKS, 0);
        builder.define(MANDUCATER_CAMOUFLAGED, false);
        builder.define(MANDUCATER_TARGET_ENTITY, 0);
        builder.define(MANDUCATER_STATUS, 0);
        builder.define(REEKER_SKIN, REEKER_SKIN_NORMAL);
        builder.define(REEKER_RICARDO_BALD, false);
        builder.define(DEVOURER_SKIN, DEVOURER_SKIN_NORMAL);
        builder.define(YELLOWEYE_SKIN, YELLOWEYE_SKIN_NORMAL);
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
                health = MobsConfig.devourerHealth();
                armor = MobsConfig.devourerArmor();
                damage = MobsConfig.devourerDamage();
                speed = 0.0D;
                knockbackResistance = MobsConfig.devourerKnockbackResistance();
                followRange = 24.0D;
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
                speed = 0.31234D;
                knockbackResistance = 0.60D;
                followRange = 24.0D;
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
                health = MobsConfig.yelloweyeHealth();
                armor = MobsConfig.yelloweyeArmor();
                damage = MobsConfig.yelloweyeNadeDamage();
                speed = 0.25D;
                knockbackResistance = MobsConfig.yelloweyeKnockbackResistance();
                followRange = 24.0D;
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
                goalSelector.addGoal(2, new DevourerAttackGoal());
                goalSelector.addGoal(6, new DevourerRandomSwimGoal());
            }
            case MANDUCATER -> {
                goalSelector.addGoal(2, new ManducaterWaterLeapGoal());
                goalSelector.addGoal(2, new ManducaterEvadeGoal());
                goalSelector.addGoal(3, new ManducaterMeleeGoal());
            }
            case REEKER -> {
                goalSelector.addGoal(1, new RicardoDiveBombGoal());
                goalSelector.addGoal(2, new ReekerWaterLeapGoal());
                goalSelector.addGoal(2, new ReekerEvadeGoal());
                goalSelector.addGoal(2, new ChargeGoal());
                goalSelector.addGoal(3, new ReekerMeleeGoal());
                goalSelector.addGoal(6, new ReekerRecruitFollowersGoal());
            }
            case TOZOON -> {
                goalSelector.addGoal(1, createBurrowMovementGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.00D, false));
            }
            case YELLOWEYE -> {
                goalSelector.addGoal(1, new YelloweyeRangedGoal());
                goalSelector.addGoal(6, new YelloweyeRandomFlightGoal());
            }
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (!level.isClientSide() && activeKind() == Kind.REEKER) {
            if (random.nextDouble() < Config.variantSpawnChance()
                    || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase()) {
                setReekerSkin(switch (random.nextInt(4)) {
                    case 0 -> REEKER_SKIN_VIRULENT;
                    case 1 -> REEKER_SKIN_BERSERKER;
                    case 2 -> REEKER_SKIN_FRAGILE;
                    default -> REEKER_SKIN_HEAVY;
                });
            }
            applyReekerAttributes(true);
            setHealth(getMaxHealth());
        }
        if (!level.isClientSide() && activeKind() == Kind.DEVOURER
                && (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase())) {
            setDevourerSkin(DEVOURER_SKIN_HEAVY);
        }
        if (!level.isClientSide() && activeKind() == Kind.YELLOWEYE
                && (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase())) {
            setYelloweyeSkin(YELLOWEYE_SKIN_HEAVY);
        }
        return data;
    }

    @Override
    public void tick() {
        super.tick();
        Kind activeKind = activeKind();
        if (activeKind == Kind.DEVOURER) {
            boolean inWater = isInWaterOrBubble();
            setNoGravity(inWater);
            if (!level().isClientSide) {
                if (inWater) {
                    setAirSupply(getMaxAirSupply());
                } else if (getAirSupply() <= -20) {
                    setAirSupply(0);
                    hurt(damageSources().drown(), 2.0F);
                }
            }
        }
        if (activeKind == Kind.YELLOWEYE) {
            setNoGravity(true);
            if (!level().isClientSide) {
                tickYelloweyeFlightLimits();
            }
        }

        if (level().isClientSide) {
            if (activeKind == Kind.MANDUCATER) {
                applyManducaterPullMotion();
            }
            if (activeKind == Kind.REEKER && isRicardoVariant() && (tickCount & 3) == 0) {
                spawnRicardoParticles();
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
        }
        if (activeKind == Kind.MANDUCATER) {
            tickManducater();
        }
        if (activeKind == Kind.REEKER && tickCount % 20 == 0) {
            applyReekerAttributes(true);
            if (isRicardoBald()) {
                addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 0, false, true), this);
            }
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
        if (activeKind() == Kind.REEKER
                && entityData.get(REEKER_CHARGE_STATE) != REEKER_CHARGE_NONE) {
            return null;
        }
        return super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (activeKind() == Kind.REEKER && getAdaptationHitStatus() > 0 && random.nextBoolean()) {
            return null;
        }
        return super.getHurtSound(source);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (activeKind() == Kind.REEKER) {
            playSound(ModSounds.get("monster.step"), 0.15F, 1.0F);
            return;
        }
        super.playStepSound(pos, state);
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
            case DEVOURER -> target.setDeltaMovement(target.getDeltaMovement().add(0.0D, -0.5645D, 0.0D));
            case MANDUCATER -> {
                if (random.nextFloat() < 0.20F) {
                    target.addEffect(new MobEffectInstance(ModMobEffects.COTH, 300, 0), this);
                }
            }
            case REEKER -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), this);
                if (getReekerSkin() == REEKER_SKIN_VIRULENT) {
                    EffectStacking.apply(target, ModMobEffects.VIRAL, 40, 0);
                } else if (getReekerSkin() == REEKER_SKIN_BERSERKER) {
                    EffectStacking.apply(target, ModMobEffects.BLEED, 40, 0);
                }
                launchSlime(target);
                if (isRicardoVariant() && level() instanceof ServerLevel serverLevel) {
                    serverLevel.explode(this, target.getX(), target.getY(), target.getZ(),
                            1.8F, Level.ExplosionInteraction.NONE);
                }
            }
            case TOZOON -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
            default -> {
            }
        }
        return true;
    }

    @Override
    public void push(Entity entity) {
        super.push(entity);
        if (!level().isClientSide && activeKind() == Kind.REEKER
                && getReekerSkin() == REEKER_SKIN_VIRULENT
                && entity instanceof LivingEntity target && !(target instanceof Parasite)) {
            EffectStacking.apply(target, ModMobEffects.VIRAL, 40, 0);
        }
    }

    @Override
    public boolean applyScaryOrbEffect(LivingEntity target, int nearbyEntities) {
        boolean applied = super.applyScaryOrbEffect(target, nearbyEntities);
        if (applied && activeKind() == Kind.REEKER) {
            applyReekerOrbEffects(target, nearbyEntities);
        }
        return applied;
    }

    private void applyReekerOrbEffects(LivingEntity target, int nearbyEntities) {
        List<? extends String> effects = MobsConfig.reekerOrbEffects();
        for (String raw : effects) {
            String[] parts = raw.split(";", -1);
            if (parts.length != 6) {
                continue;
            }
            try {
                int self = Integer.parseInt(parts[0].trim());
                int duration = Math.max(0, Integer.parseInt(parts[1].trim())) * 20;
                int amplifier = Integer.parseInt(parts[2].trim());
                int amplifierStep = Integer.parseInt(parts[4].trim());
                int durationStep = Integer.parseInt(parts[5].trim());
                ResourceLocation effectId = ResourceLocation.tryParse(parts[3].trim());
                if (effectId == null) {
                    continue;
                }
                int scaledAmplifier = amplifierStep == 0 ? amplifier
                        : amplifier + nearbyEntities / amplifierStep;
                int scaledDuration = durationStep == 0 ? duration
                        : duration + nearbyEntities / durationStep * 20;
                BuiltInRegistries.MOB_EFFECT.getOptional(effectId).ifPresent(effect -> {
                    if (self == 1) {
                        EffectStacking.apply(PrimitiveVariantEntity.this,
                                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), scaledDuration, scaledAmplifier);
                    } else if (self == 2) {
                        if (target instanceof Parasite) {
                            EffectStacking.apply(target, BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                                    scaledDuration, scaledAmplifier);
                        }
                    } else if (!(target instanceof Parasite)) {
                        EffectStacking.apply(target, BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                                scaledDuration, scaledAmplifier);
                    }
                });
            } catch (NumberFormatException ignored) {
                // Invalid entries are rejected by the config validator; retain runtime tolerance for old files.
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (activeKind() != Kind.REEKER || !isRicardoVariant() || isRicardoBald()
                || !stack.is(Items.SHEARS)) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide) {
            setRicardoBald(true);
            addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 0, false, true), this);
            if (player instanceof ServerPlayer serverPlayer) {
                awardRicardoShearing(serverPlayer);
            }
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(1, player,
                        hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            level().playSound(null, getX(), getY(), getZ(), ParasiteSoundProfiles.hurt(this),
                    SoundSource.HOSTILE, 2.0F, 0.65F);
            level().playSound(null, getX(), getY(), getZ(), ParasiteSoundProfiles.ambient(this),
                    SoundSource.HOSTILE, 2.5F, 0.75F);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
                                1.0F, 0.25F, 0.75F),
                        getX(), getY() + 1.2D, getZ(), 40, 0.6D, 0.8D, 0.6D, 0.05D);
                serverLevel.broadcastEntityEvent(this, RICARDO_BURST_EVENT);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        boolean wasRicardo = isRicardoVariant();
        super.setCustomName(name);
        if (level().isClientSide || activeKind() != Kind.REEKER) {
            return;
        }
        boolean isRicardo = isRicardoVariant();
        applyReekerAttributes(true);
        if (isRicardo && !wasRicardo) {
            setHealth(getMaxHealth());
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.HOSTILE, 8.0F, 1.0F);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.HOSTILE, 4.0F, 1.0F);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        getX(), getY() + 1.2D, getZ(), 40, 0.6D, 0.8D, 0.6D, 0.1D);
                serverLevel.broadcastEntityEvent(this, RICARDO_BURST_EVENT);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == RICARDO_BURST_EVENT) {
            for (int i = 0; i < 40; i++) {
                level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
                                1.0F, 0.25F, 0.75F),
                        getRandomX(1.2D), getRandomY(), getRandomZ(1.2D),
                        random.nextGaussian() * 0.08D, random.nextGaussian() * 0.08D,
                        random.nextGaussian() * 0.08D);
            }
            return;
        }
        super.handleEntityEvent(id);
    }

    public boolean isPrimitiveReeker() {
        return activeKind() == Kind.REEKER;
    }

    public boolean isPrimitiveYelloweye() {
        return activeKind() == Kind.YELLOWEYE;
    }

    public boolean isPrimitiveDevourer() {
        return activeKind() == Kind.DEVOURER;
    }

    public int getDevourerSkin() {
        return entityData.get(DEVOURER_SKIN);
    }

    private void setDevourerSkin(int skin) {
        entityData.set(DEVOURER_SKIN, skin == DEVOURER_SKIN_HEAVY
                ? DEVOURER_SKIN_HEAVY : DEVOURER_SKIN_NORMAL);
    }

    public int getYelloweyeSkin() {
        return entityData.get(YELLOWEYE_SKIN);
    }

    private void setYelloweyeSkin(int skin) {
        entityData.set(YELLOWEYE_SKIN, skin == YELLOWEYE_SKIN_HEAVY
                ? YELLOWEYE_SKIN_HEAVY : YELLOWEYE_SKIN_NORMAL);
    }

    public int getReekerSkin() {
        return entityData.get(REEKER_SKIN);
    }

    public boolean isRicardoVariant() {
        return activeKind() == Kind.REEKER && MobsConfig.reekerRicardoVariantEnabled()
                && getCustomName() != null
                && "ricardo".equals(getCustomName().getString().trim().toLowerCase(java.util.Locale.ROOT));
    }

    public boolean isRicardoBald() {
        return isRicardoVariant() && entityData.get(REEKER_RICARDO_BALD);
    }

    private void setReekerSkin(int skin) {
        entityData.set(REEKER_SKIN, skin);
    }

    private void setRicardoBald(boolean bald) {
        entityData.set(REEKER_RICARDO_BALD, bald);
    }

    private void applyReekerAttributes(boolean preserveHealth) {
        if (activeKind() != Kind.REEKER) {
            return;
        }
        float currentHealth = getHealth();
        boolean ricardo = isRicardoVariant();
        double health = ricardo ? RICARDO_MAX_HEALTH : MobsConfig.reekerHealth();
        double damage = MobsConfig.reekerDamage();
        if (!ricardo && getReekerSkin() == REEKER_SKIN_FRAGILE) {
            health *= 0.5D;
            damage *= 1.5D;
        }
        double healthRatio = getMaxHealth() <= 0.0F ? 1.0D : getHealth() / getMaxHealth();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(MobsConfig.reekerKnockbackResistance());

        double armor = ricardo ? RICARDO_NORMAL_ARMOR : MobsConfig.reekerArmor();
        double speed = ricardo ? 0.45D : 0.3D;
        if (ricardo) {
            double ratio = getMaxHealth() <= 0.0F ? 1.0D : getHealth() / getMaxHealth();
            if (ratio <= 0.10D) {
                armor = RICARDO_BERSERK_ARMOR;
                speed = 0.62D;
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, false, true), this);
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2, false, true), this);
            } else if (ratio <= 0.25D) {
                armor = RICARDO_ENRAGED_ARMOR;
                speed = 0.58D;
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, true), this);
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, true), this);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                            getX(), getY() + 1.2D, getZ(), 12, 0.35D, 0.6D, 0.35D, 0.02D);
                }
            }
        }
        getAttribute(Attributes.ARMOR).setBaseValue(armor);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        if (preserveHealth) {
            float scaledHealth = ricardo ? currentHealth
                    : (float) Math.min(getMaxHealth(), Math.max(currentHealth, getMaxHealth() * healthRatio));
            setHealth(Mth.clamp(scaledHealth, 0.01F, getMaxHealth()));
        }
    }

    private void launchSlime(LivingEntity target) {
        if (!(target instanceof Slime) || level().isClientSide || random.nextFloat() >= 0.10F) {
            return;
        }
        double deltaX = target.getX() - getX();
        double deltaZ = target.getZ() - getZ();
        double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (length < 1.0E-4D) {
            deltaX = random.nextDouble() - 0.5D;
            deltaZ = random.nextDouble() - 0.5D;
            length = Math.max(1.0E-4D, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ));
        }
        target.push(deltaX / length * 4.75D, 1.2D, deltaZ / length * 4.75D);
        target.hurtMarked = true;
    }

    private void spawnRicardoParticles() {
        ColorParticleOption color = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
                1.0F, 0.25F, 0.75F);
        for (int i = 0; i < 3; i++) {
            level().addParticle(color, getRandomX(1.6D),
                    getY() + random.nextDouble() * getBbHeight() * 0.9D, getRandomZ(1.6D),
                    0.0D, 0.0D, 0.0D);
        }
    }

    private void awardRicardoShearing(ServerPlayer player) {
        AdvancementHolder advancement = player.server.getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "tricked_me"));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "sheared_ricardo");
        }
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
        if (activeKind() == Kind.REEKER) {
            tag.putInt("reeker_skin", getReekerSkin());
            tag.putBoolean("RicardoBald", entityData.get(REEKER_RICARDO_BALD));
            tag.putInt("reeker_charge_preparation", reekerChargePreparationTicks);
        }
        if (activeKind() == Kind.DEVOURER) {
            tag.putInt("devourer_skin", getDevourerSkin());
        }
        if (activeKind() == Kind.YELLOWEYE) {
            tag.putInt("yelloweye_skin", getYelloweyeSkin());
            tag.putInt("yelloweye_shots", rangedShots);
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
        if (activeKind() == Kind.REEKER) {
            setReekerSkin(tag.getInt("reeker_skin"));
            setRicardoBald(tag.getBoolean("RicardoBald"));
            reekerChargePreparationTicks = Math.max(0, tag.getInt("reeker_charge_preparation"));
            entityData.set(REEKER_CHARGE_STATE, REEKER_CHARGE_NONE);
            if (!level().isClientSide) {
                applyReekerAttributes(true);
            }
        }
        if (activeKind() == Kind.DEVOURER) {
            setDevourerSkin(tag.getInt("devourer_skin"));
        }
        if (activeKind() == Kind.YELLOWEYE) {
            setYelloweyeSkin(tag.getInt("yelloweye_skin"));
            rangedShots = Mth.clamp(tag.getInt("yelloweye_shots"), 0, 3);
            resetYelloweyeAttack();
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
    protected int increaseAirSupply(int airSupply) {
        return activeKind() == Kind.DEVOURER ? airSupply - 1 : super.increaseAirSupply(airSupply);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return activeKind() != Kind.YELLOWEYE && super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    protected float adjustBlockBreakHardness(float baseHardness) {
        boolean heavy = activeKind() == Kind.YELLOWEYE && getYelloweyeSkin() == YELLOWEYE_SKIN_HEAVY
                || activeKind() == Kind.REEKER && getReekerSkin() == REEKER_SKIN_HEAVY
                || activeKind() == Kind.DEVOURER && getDevourerSkin() == DEVOURER_SKIN_HEAVY;
        return heavy ? baseHardness * 2.0F : baseHardness;
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
                    || hardness < 0.0F || hardness > adjustBlockBreakHardness(1.0F)) {
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
        Vec3 view = getViewVector(1.0F);
        Vec3 start = new Vec3(getX() + view.x, getY() + getEyeHeight() - 0.2D, getZ() + view.z);
        Vec3 targetCenter = new Vec3(target.getX(), target.getBoundingBox().minY + target.getBbHeight() * 0.5D,
                target.getZ());
        projectile.configureLegacyFireball(this,
                acid ? ParasiteProjectileEntity.Mode.YELLOWEYE_NADE
                        : ParasiteProjectileEntity.Mode.YELLOWEYE_SPINE,
                start, targetCenter.subtract(start),
                acid ? 0.0F : MobsConfig.yelloweyeRangedDamage(), 0.15D, Integer.MAX_VALUE);
        level().addFreshEntity(projectile);
        playSound(ModSounds.get("emana.shooting"), 2.0F, acid ? 2.0F : 1.0F);
    }

    private void tickYelloweyeFlightLimits() {
        if (tickCount % 21 == 10) {
            if (onGround()) {
                getMoveControl().setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.5D);
            }
            LivingEntity target = getTarget();
            if (target != null && (!level().isEmptyBlock(blockPosition().below())
                    || !level().isEmptyBlock(blockPosition().below(2)))) {
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x, Math.max(motion.y, 0.5D), motion.z);
            }
        }

        int limit = MobsConfig.yelloweyeMaxFlightHeight();
        if (limit == 256) {
            return;
        }
        LivingEntity target = getTarget();
        double maximumY = target == null
                ? level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        blockPosition().getX(), blockPosition().getZ()) + limit
                : target.getY() + limit;
        if (getY() > maximumY) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }
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

    private final class DevourerAttackGoal extends Goal {
        private static final double ATTACK_DISTANCE_SQR = 16.0D;
        private static final double TRACKING_FACTOR = 0.85D;
        private static final double SWIM_ACCELERATION = 0.08D;
        private int attackCooldown;
        private int movementCycle;

        private DevourerAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                setTarget(null);
                return;
            }
            if (!isInWaterOrBubble()) {
                return;
            }
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            movementCycle++;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double followRange = getAttributeValue(Attributes.FOLLOW_RANGE);
            if (distanceToSqr(target) * TRACKING_FACTOR >= followRange * followRange
                    || !hasLineOfSight(target)) {
                return;
            }

            double deltaX = target.getX() - getX();
            double deltaZ = target.getZ() - getZ();
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (horizontalDistance > 1.0E-5D) {
                Vec3 motion = getDeltaMovement();
                double verticalMotion = target.getY() >= getY() + 3.0D ? 0.52D : -0.2D;
                if (target.getY() >= getY() + 1.0D) {
                    verticalMotion -= 0.2D;
                }
                setDeltaMovement(motion.x * 1.08D + deltaX / horizontalDistance * SWIM_ACCELERATION,
                        verticalMotion,
                        motion.z * 1.08D + deltaZ / horizontalDistance * SWIM_ACCELERATION);
            }

            double attackDistance = distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            if (attackDistance <= ATTACK_DISTANCE_SQR && attackCooldown <= 0) {
                attackCooldown = 20;
                doHurtTarget(target);
            }
            if (movementCycle > 140) {
                movementCycle = 0;
            }
        }
    }

    private final class DevourerRandomSwimGoal extends Goal {
        private DevourerRandomSwimGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return random.nextInt(7) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            BlockPos origin = blockPosition();
            int pattern = 1;
            if (target != null) {
                double distanceSqr = distanceToSqr(target);
                if (distanceSqr > 100.0D) {
                    origin = target.blockPosition();
                    pattern = 2;
                } else if (distanceSqr < 36.0D) {
                    origin = target.blockPosition();
                    pattern = 3;
                }
            }

            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos destination = switch (pattern) {
                    case 2 -> origin.offset(random.nextInt(6) - 2, random.nextInt(7) - 2,
                            random.nextInt(6) - 2);
                    case 3 -> origin.offset(random.nextInt(4) + 3, random.nextInt(5) + 4,
                            random.nextInt(4) + 3);
                    default -> origin.offset(random.nextInt(15) - 7, random.nextInt(11) - 5,
                            random.nextInt(15) - 7);
                };
                if (!level().getFluidState(destination).is(FluidTags.WATER)) {
                    continue;
                }
                getMoveControl().setWantedPosition(destination.getX() + 0.5D,
                        destination.getY() + 0.5D, destination.getZ() + 0.5D, 0.19D);
                if (target == null) {
                    getLookControl().setLookAt(destination.getX() + 0.5D,
                            destination.getY() + 0.5D, destination.getZ() + 0.5D, 180.0F, 20.0F);
                }
                return;
            }
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

    private final class ReekerMeleeGoal extends MeleeAttackGoal {
        private int attackCooldown;

        private ReekerMeleeGoal() {
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
            return REEKER_ATTACK_INTERVAL;
        }
    }

    private final class ReekerWaterLeapGoal extends Goal {
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
            if (target != null && target.isAlive()
                    && entityData.get(REEKER_CHARGE_STATE) == REEKER_CHARGE_NONE) {
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
            }
        }
    }

    private final class ReekerEvadeGoal extends Goal {
        private int cooldown = REEKER_EVADE_COOLDOWN + 1;
        private int duration;
        private boolean evading;

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return evading || target != null && target.isAlive() && onGround()
                    && entityData.get(REEKER_CHARGE_STATE) == REEKER_CHARGE_NONE
                    && !hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                    && distanceToSqr(target) > REEKER_EVADE_MIN_DISTANCE_SQR
                    && distanceToSqr(target) < REEKER_EVADE_MAX_DISTANCE_SQR
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
                if (++duration >= REEKER_EVADE_DURATION) {
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
            if (distanceSqr <= REEKER_EVADE_MIN_DISTANCE_SQR
                    || distanceSqr >= REEKER_EVADE_MAX_DISTANCE_SQR || !hasLineOfSight(target)) {
                return;
            }
            if (++cooldown < REEKER_EVADE_COOLDOWN) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            setXxa(random.nextBoolean() ? 1.0F : -1.0F);
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

    private final class ReekerRecruitFollowersGoal extends Goal {
        @Override
        public boolean canUse() {
            return tickCount % 20 == 0 && getTarget() == null
                    && ParasiteFollowGoal.getLeader(PrimitiveVariantEntity.this) == null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            for (Mob follower : level().getEntitiesOfClass(Mob.class,
                    getBoundingBox().inflate(16.0D, 2.0D, 16.0D),
                    candidate -> candidate != PrimitiveVariantEntity.this
                            && candidate instanceof Parasite && candidate.isAlive()
                            && ParasiteFollowGoal.commandRank(candidate) < 41)) {
                if (!hasLineOfSight(follower)) {
                    continue;
                }
                Mob currentLeader = ParasiteFollowGoal.getLeader(follower);
                if (currentLeader == null || ParasiteFollowGoal.commandRank(currentLeader) <= 30) {
                    ParasiteFollowGoal.setLeader(follower, PrimitiveVariantEntity.this);
                    break;
                }
            }
        }
    }

    private final class ChargeGoal extends Goal {
        private int chargeTicks;
        private double targetX;
        private double targetY;
        private double targetZ;
        private boolean finished;

        private ChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || !onGround() || isInWaterOrBubble()
                    || !hasLineOfSight(target)) {
                return false;
            }
            double distance = distanceToSqr(target);
            if (distance < 64.0D || distance >= 1024.0D) {
                return false;
            }
            reekerChargePreparationTicks += hasEffect(ModMobEffects.RAGE) ? 2 : 1;
            return reekerChargePreparationTicks >= REEKER_SKILL_PREP_TICKS;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return !finished && target != null && target.isAlive();
        }

        @Override
        public void start() {
            reekerChargePreparationTicks = 0;
            chargeTicks = 0;
            finished = false;
            entityData.set(REEKER_CHARGE_STATE, REEKER_CHARGE_WINDUP);
            getNavigation().stop();
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        }

        @Override
        public void stop() {
            entityData.set(REEKER_CHARGE_STATE, REEKER_CHARGE_NONE);
            getNavigation().stop();
            finished = false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                finished = true;
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (chargeTicks < REEKER_WINDUP_TICKS) {
                if (!onGround() || isInWaterOrBubble()
                        || target.getY() > getY() && target.onGround()) {
                    finished = true;
                    return;
                }
                getNavigation().stop();
                setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
                if (chargeTicks == 1) {
                    float pitch = (random.nextFloat() - random.nextFloat()) * 0.4F + 2.0F;
                    playSound(ParasiteSoundProfiles.hurt(PrimitiveVariantEntity.this), 4.0F, pitch);
                }
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, getRandomX(0.8D),
                            getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.8D),
                            2, 0.05D, 0.05D, 0.05D, 0.01D);
                }
                double distance = Math.max(0.001D, distanceTo(target));
                targetX = getX() + 15.0D * (target.getX() - getX()) / distance;
                targetY = getY() + 15.0D * (target.getY() - getY()) / distance;
                targetZ = getZ() + 15.0D * (target.getZ() - getZ()) / distance;
                if (++chargeTicks == REEKER_WINDUP_TICKS) {
                    entityData.set(REEKER_CHARGE_STATE, REEKER_CHARGING);
                    getNavigation().moveTo(targetX, targetY, targetZ, 2.5D);
                }
                return;
            }

            for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(2.0D, 0.0D, 2.0D),
                    PrimitiveVariantEntity.this::isValidParasiteTarget)) {
                double deltaX = getX() - victim.getX();
                double deltaZ = getZ() - victim.getZ();
                double length = Math.max(0.001D, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ));
                victim.push(-deltaX / length * 0.5D, 0.4D, -deltaZ / length * 0.5D);
                doHurtTarget(victim);
            }
            if (!onGround()) {
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x * 0.7D, motion.y, motion.z * 0.7D);
            }
            chargeTicks++;
            if (chargeTicks >= REEKER_WINDUP_TICKS + REEKER_CHARGE_TICKS
                    && (getNavigation().isDone()
                    || Math.abs(getX() - xo) < 1.0E-6D && Math.abs(getZ() - zo) < 1.0E-6D)) {
                finished = true;
            }
        }
    }

    private final class RicardoDiveBombGoal extends Goal {
        private DivePhase phase = DivePhase.IDLE;
        private long nextAllowedTick;
        private int hoverTicks;
        private int diveTicks;
        private int ascendTicks;
        private double ascendTargetY;
        private double hoverY;
        private Vec3 lockedTarget = Vec3.ZERO;

        private RicardoDiveBombGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return isRicardoVariant() && level().getGameTime() >= nextAllowedTick
                    && target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return isRicardoVariant() && phase != DivePhase.IDLE;
        }

        @Override
        public void start() {
            phase = DivePhase.ASCEND;
            hoverTicks = 0;
            diveTicks = 0;
            ascendTicks = 0;
            ascendTargetY = getY() + 20.0D;
            getNavigation().stop();
            fallDistance = 0.0F;
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void stop() {
            finish(true);
        }

        @Override
        public void tick() {
            switch (phase) {
                case ASCEND -> tickAscend();
                case HOVER -> tickHover();
                case DIVE -> tickDive();
                default -> {
                }
            }
        }

        private void tickAscend() {
            setNoGravity(true);
            ascendTicks++;
            double remaining = ascendTargetY - getY();
            if (remaining <= 0.05D || ascendTicks >= 120) {
                enterHover();
                return;
            }
            double step = Math.min(0.6D, remaining);
            Vec3 start = position();
            Vec3 end = start.add(0.0D, step, 0.0D);
            HitResult hit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, PrimitiveVariantEntity.this));
            if (hit.getType() == HitResult.Type.BLOCK) {
                setPos(getX(), hit.getLocation().y - 0.05D, getZ());
                enterHover();
                return;
            }
            setPos(getX(), getY() + step, getZ());
            setDeltaMovement(Vec3.ZERO);
            fallDistance = 0.0F;
            faceDiveTarget(getTarget());
        }

        private void tickHover() {
            setNoGravity(true);
            double deltaY = hoverY - getY();
            if (Math.abs(deltaY) > 0.05D) {
                setPos(getX(), getY() + Math.copySign(Math.min(0.2D, Math.abs(deltaY)), deltaY), getZ());
            }
            setDeltaMovement(Vec3.ZERO);
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                finish(false);
                return;
            }
            faceDiveTarget(target);
            if (++hoverTicks >= 20) {
                lockedTarget = target.getEyePosition().subtract(0.0D, target.getEyeHeight() * 0.5D, 0.0D);
                phase = DivePhase.DIVE;
                diveTicks = 0;
                applyDiveVector(lockedTarget, 1.6D);
            }
        }

        private void tickDive() {
            getNavigation().stop();
            setNoGravity(true);
            fallDistance = 0.0F;
            diveTicks++;
            Vec3 start = position();
            Vec3 end = start.add(getDeltaMovement());
            if (level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, PrimitiveVariantEntity.this)).getType() == HitResult.Type.BLOCK) {
                explodeAndFinish();
                return;
            }
            LivingEntity target = getTarget();
            Vec3 aim = lockedTarget;
            if (target != null && target.isAlive()) {
                if (getBoundingBox().intersects(target.getBoundingBox()) || distanceToSqr(target) < 2.25D) {
                    explodeAndFinish();
                    return;
                }
                aim = target.getEyePosition().subtract(0.0D, target.getEyeHeight() * 0.5D, 0.0D);
            }
            applyDiveVector(aim, Math.min(REEKER_DIVE_SPEED + 0.35D * diveTicks, 4.5D));
            if (diveTicks > 80) {
                explodeAndFinish();
            }
        }

        private void enterHover() {
            phase = DivePhase.HOVER;
            hoverTicks = 0;
            hoverY = getY();
            setDeltaMovement(Vec3.ZERO);
            hasImpulse = true;
        }

        private void applyDiveVector(Vec3 target, double speed) {
            Vec3 direction = target.subtract(position());
            if (direction.lengthSqr() > 1.0E-4D) {
                setDeltaMovement(direction.normalize().scale(speed));
                hasImpulse = true;
                getLookControl().setLookAt(target.x, target.y, target.z, 30.0F, 30.0F);
            }
        }

        private void explodeAndFinish() {
            level().explode(PrimitiveVariantEntity.this, getX(), getY(), getZ(),
                    REEKER_DIVE_EXPLOSION, Level.ExplosionInteraction.NONE);
            finish(true);
        }

        private void finish(boolean setCooldown) {
            setNoGravity(false);
            setDeltaMovement(Vec3.ZERO);
            hasImpulse = true;
            fallDistance = 0.0F;
            phase = DivePhase.IDLE;
            if (setCooldown) {
                nextAllowedTick = level().getGameTime() + REEKER_DIVE_COOLDOWN_TICKS;
            }
        }

        private void faceDiveTarget(@Nullable LivingEntity target) {
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }
    }

    private enum DivePhase {
        IDLE,
        ASCEND,
        HOVER,
        DIVE
    }

    private final class YelloweyeRangedGoal extends Goal {
        private YelloweyeRangedGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
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
                resetYelloweyeAttack();
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (distanceToSqr(target) >= YELLOWEYE_MAX_ATTACK_DISTANCE_SQR || !hasLineOfSight(target)) {
                yelloweyeAttackTimer = Math.max(0, yelloweyeAttackTimer - 1);
                return;
            }

            yelloweyeAttackTimer += hasEffect(ModMobEffects.RAGE) ? 2 : 1;
            if (yelloweyeAttackTimer == YELLOWEYE_WARNING_TICK) {
                rangedShots++;
                if (rangedShots == 4) {
                    if (level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY() + getEyeHeight(), getZ(),
                                2, 0.15D, 0.15D, 0.15D, 0.01D);
                    }
                    playSound(ModSounds.get("emana.hurt"), 4.0F,
                            (random.nextFloat() - random.nextFloat()) * 0.4F + 2.0F);
                } else {
                    playSound(ModSounds.get("emana.shootingpost"), 2.0F, 1.0F);
                }
            }
            if (yelloweyeAttackTimer >= YELLOWEYE_FIRE_TICK && !yelloweyeShotFired) {
                boolean acid = rangedShots >= 4;
                fireYelloweyeProjectile(target, acid);
                if (acid) {
                    rangedShots = 0;
                }
                yelloweyeShotFired = true;
                return;
            }
            if (yelloweyeShotFired) {
                resetYelloweyeAttack();
            }
        }

        @Override
        public void stop() {
            resetYelloweyeAttack();
        }
    }

    private void resetYelloweyeAttack() {
        yelloweyeAttackTimer = 0;
        yelloweyeShotFired = false;
    }

    private final class YelloweyeRandomFlightGoal extends Goal {
        private YelloweyeRandomFlightGoal() {
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
            int mode = 1;
            double speed = 0.5D;
            LivingEntity target = getTarget();
            if (target != null) {
                double distance = distanceToSqr(target);
                if (distance > 100.0D) {
                    origin = target.blockPosition();
                    mode = 2;
                    speed = 0.75D;
                } else if (distance < 36.0D) {
                    origin = target.blockPosition();
                    mode = 3;
                    speed = 0.75D;
                }
            }

            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos destination = switch (mode) {
                    case 2 -> origin.offset(random.nextInt(6) - 2, random.nextInt(7) - 2,
                            random.nextInt(6) - 2);
                    case 3 -> origin.offset(random.nextInt(4) + 3, random.nextInt(5) + 4,
                            random.nextInt(4) + 3);
                    default -> origin.offset(random.nextInt(15) - 7, random.nextInt(11) - 5,
                            random.nextInt(15) - 7);
                };
                if (!level().isEmptyBlock(destination)) {
                    continue;
                }
                getMoveControl().setWantedPosition(destination.getX() + 0.5D,
                        destination.getY() + 0.5D, destination.getZ() + 0.5D, speed);
                if (target == null) {
                    getLookControl().setLookAt(destination.getX() + 0.5D,
                            destination.getY() + 0.5D, destination.getZ() + 0.5D, 180.0F, 20.0F);
                }
                return;
            }
        }
    }

    private static final class DevourerMoveControl extends MoveControl {
        private DevourerMoveControl(PrimitiveVariantEntity devourer) {
            super(devourer);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            PrimitiveVariantEntity devourer = (PrimitiveVariantEntity) mob;
            double x = wantedX - devourer.getX();
            double y = wantedY - devourer.getY();
            double z = wantedZ - devourer.getZ();
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance < devourer.getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                devourer.setDeltaMovement(devourer.getDeltaMovement().scale(0.5D));
                return;
            }
            devourer.setDeltaMovement(devourer.getDeltaMovement().add(
                    x / distance * 0.05D * speedModifier,
                    y / distance * 0.05D * speedModifier,
                    z / distance * 0.05D * speedModifier));
            LivingEntity target = devourer.getTarget();
            double lookX = target == null ? devourer.getDeltaMovement().x : target.getX() - devourer.getX();
            double lookZ = target == null ? devourer.getDeltaMovement().z : target.getZ() - devourer.getZ();
            devourer.setYRot(-((float) Mth.atan2(lookX, lookZ)) * Mth.RAD_TO_DEG);
            devourer.yBodyRot = devourer.getYRot();
        }
    }

    private static final class YelloweyeMoveControl extends MoveControl {
        private YelloweyeMoveControl(PrimitiveVariantEntity yelloweye) {
            super(yelloweye);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            PrimitiveVariantEntity yelloweye = (PrimitiveVariantEntity) mob;
            double x = wantedX - yelloweye.getX();
            double y = wantedY - yelloweye.getY();
            double z = wantedZ - yelloweye.getZ();
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance < yelloweye.getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                yelloweye.setDeltaMovement(yelloweye.getDeltaMovement().scale(0.5D));
                return;
            }
            yelloweye.setDeltaMovement(yelloweye.getDeltaMovement().add(
                    x / distance * 0.05D * speedModifier,
                    y / distance * 0.05D * speedModifier,
                    z / distance * 0.05D * speedModifier));
            LivingEntity target = yelloweye.getTarget();
            double lookX = target == null ? yelloweye.getDeltaMovement().x : target.getX() - yelloweye.getX();
            double lookZ = target == null ? yelloweye.getDeltaMovement().z : target.getZ() - yelloweye.getZ();
            yelloweye.setYRot(-((float) Mth.atan2(lookX, lookZ)) * Mth.RAD_TO_DEG);
            yelloweye.yBodyRot = yelloweye.getYRot();
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
