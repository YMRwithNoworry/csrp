package alku.csrp.entity;

import alku.csrp.Csrp;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * Shared legacy behaviour for assimilated animals. They retain their own
 * no-adaptation, fire-vulnerable progression path rather than inheriting the
 * primitive-parasite damage-adaptation state.
 */
public final class AssimilatedParasiteEntity extends Monster implements GeoEntity, Parasite {
    public static final int FERAL_KILL_THRESHOLD = 60;
    private static final int COTH_DURATION_TICKS = 4_800;
    private static final int COTH_AURA_RADIUS = 8;
    private static final EntityDataAccessor<Integer> SHEEP_TEXTURE_VARIANT =
            SynchedEntityData.defineId(AssimilatedParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED_WOLF_TEXTURE =
            SynchedEntityData.defineId(AssimilatedParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MELTING =
            SynchedEntityData.defineId(AssimilatedParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> MELT_HEIGHT =
            SynchedEntityData.defineId(AssimilatedParasiteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COW_CHARGE_STATE =
            SynchedEntityData.defineId(AssimilatedParasiteEntity.class, EntityDataSerializers.INT);
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation SWIM = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation COW_CHARGE_WINDUP = ParasiteAnimations.loop(
            this, "idle.get_parasite_status_3.get_still_ani_1");
    private final RawAnimation COW_CHARGING = ParasiteAnimations.loop(this, "walk.get_parasite_status_3");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Kind kind;
    private int parasiteKills;
    private int chargeCooldown;
    private int meltTicks;

    // 固化机制相关字段
    private static final String SURVIVAL_TIME_TAG = "survival_time";
    private static final int PETRIFICATION_TIME_TICKS = 24000; // 20分钟
    private static final int PETRIFICATION_KILL_THRESHOLD = 30; // 低于转变阈值60
    private static final int PETRIFICATION_CHECK_INTERVAL = 100; // 每5秒检查一次
    private int survivalTicks;

    public AssimilatedParasiteEntity(EntityType<? extends AssimilatedParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.experience;
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind.knockbackResistance)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.FOLLOW_RANGE, kind.followRange);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (kind == Kind.COW) {
            goalSelector.addGoal(1, new CowChargeGoal());
        }
        goalSelector.addGoal(2, new MeleeAttackGoal(this, kind == Kind.WOLF ? 1.35D : 1.15D, false));
        if (kind == Kind.SQUID) {
            goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0D, 30));
        } else {
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        }
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return kind == Kind.SQUID ? new WaterBoundPathNavigation(this, level) : super.createNavigation(level);
    }

    @Override
    public void tick() {
        if (isMelting()) {
            freezeMelting();
        }
        super.tick();
        if (level().isClientSide) {
            return;
        }

        if (isMelting()) {
            freezeMelting();
            tickMelting();
            return;
        }

        if (chargeCooldown > 0) {
            chargeCooldown--;
        }
        if (tickCount % 20 == 0) {
            infectNearby();
        }
        if (kind == Kind.SQUID && !isInWaterOrBubble()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.02D, 0.0D));
        }

        // 固化机制检查
        if (tickCount % PETRIFICATION_CHECK_INTERVAL == 0) {
            survivalTicks += PETRIFICATION_CHECK_INTERVAL;
            checkPetrification();
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        LivingEntity livingTarget = entity instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(entity);
        if (hit && livingTarget != null) {
            triggerAnim("attack_controller", "attack");
            ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
        }
        if (hit && kind == Kind.SQUID) {
            float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(1.75D));
            for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(1.75D), this::isValidParasiteTarget)) {
                if (nearby != entity) {
                    nearby.hurt(damageSources().mobAttack(this), damage);
                }
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        parasiteKills++;
        if (parasiteKills > FERAL_KILL_THRESHOLD) {
            transformToFeral(level);
        }
        return super.killedEntity(level, victim);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ParasiteSoundProfiles.ambient(this);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ParasiteSoundProfiles.hurt(this);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ParasiteSoundProfiles.death(this);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide || kind == Kind.BEAR || kind == Kind.SQUID || random.nextFloat() >= 0.5F
                || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AssimilatedHeadEntity head = switch (kind) {
            case COW -> ModEntities.SIM_COW_HEAD.get().create(serverLevel);
            case PIG -> ModEntities.SIM_PIG_HEAD.get().create(serverLevel);
            case SHEEP -> ModEntities.SIM_SHEEP_HEAD.get().create(serverLevel);
            case WOLF -> ModEntities.SIM_WOLF_HEAD.get().create(serverLevel);
            case BEAR, SQUID -> null;
        };
        if (head == null) {
            return;
        }
        head.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        head.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        head.setCustomName(getCustomName());
        head.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            head.setPersistenceRequired();
        }
        serverLevel.addFreshEntity(head);
    }

    private void transformToFeral(ServerLevel level) {
        FeralParasiteEntity feral = switch (kind) {
            case BEAR -> ModEntities.FER_BEAR.get().create(level);
            case COW -> ModEntities.FER_COW.get().create(level);
            case PIG -> ModEntities.FER_PIG.get().create(level);
            case SHEEP -> ModEntities.FER_SHEEP.get().create(level);
            case WOLF -> ModEntities.FER_WOLF.get().create(level);
            case SQUID -> null;
        };
        if (feral == null) {
            return;
        }

        feral.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        feral.setTarget(getTarget());
        feral.setCustomName(getCustomName());
        feral.setCustomNameVisible(isCustomNameVisible());
        level.addFreshEntity(feral);
        discard();
    }

    /**
     * 检查固化条件并执行固化
     */
    private void checkPetrification() {
        if (survivalTicks >= PETRIFICATION_TIME_TICKS
            && parasiteKills < PETRIFICATION_KILL_THRESHOLD
            && level() instanceof ServerLevel serverLevel) {
            petrify(serverLevel);
        }
    }

    /**
     * 执行固化：生成 AssimilatedHeadEntity 遗骸
     */
    private void petrify(ServerLevel level) {
        AssimilatedHeadEntity head = switch (kind) {
            case COW -> ModEntities.SIM_COW_HEAD.get().create(level);
            case PIG -> ModEntities.SIM_PIG_HEAD.get().create(level);
            case SHEEP -> ModEntities.SIM_SHEEP_HEAD.get().create(level);
            case WOLF -> ModEntities.SIM_WOLF_HEAD.get().create(level);
            case BEAR, SQUID -> null;
        };

        if (head == null) {
            return;
        }

        // 设置遗骸位置和姿态
        head.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        head.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        head.setCustomName(getCustomName());
        head.setCustomNameVisible(isCustomNameVisible());

        if (isPersistenceRequired()) {
            head.setPersistenceRequired();
        }

        if (level.addFreshEntity(head)) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHEEP_TEXTURE_VARIANT, 0);
        builder.define(TAMED_WOLF_TEXTURE, false);
        builder.define(MELTING, false);
        builder.define(MELT_HEIGHT, 0.0F);
        builder.define(COW_CHARGE_STATE, 0);
    }

    public boolean canMelt() {
        return kind != Kind.SQUID && !isMelting();
    }

    public void melt() {
        if (!canMelt()) {
            return;
        }
        entityData.set(MELTING, true);
        entityData.set(MELT_HEIGHT, kind.meltStartHeight);
        meltTicks = 0;
        freezeMelting();
    }

    public boolean isMelting() {
        return entityData.get(MELTING);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        EntityDimensions dimensions = super.getDefaultDimensions(pose);
        if (!isMelting()) {
            return dimensions;
        }
        return dimensions.scale(1.0F, getMeltHeight() / (float) kind.baseHeight);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == MELTING || accessor == MELT_HEIGHT) {
            refreshDimensions();
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (!level.isClientSide()) {
            if (kind == Kind.SHEEP) {
                rollSheepTextureVariant();
            } else if (kind == Kind.WOLF) {
                setTamedWolfTexture(random.nextInt(100) == 0);
            }
        }
        return data;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (kind != Kind.SHEEP) {
            return super.mobInteract(player, hand);
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof DyeItem dyeItem)) {
            return super.mobInteract(player, hand);
        }

        int variant = sheepVariantForDye(dyeItem.getDyeColor());
        if (variant < 0) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide && variant != getSheepTextureVariant()) {
            setSheepTextureVariant(variant);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasite_kills", parasiteKills);
        tag.putInt("sheep_texture_variant", getSheepTextureVariant());
        tag.putBoolean("tamed_wolf_texture", hasTamedWolfTexture());
        tag.putBoolean("melting", isMelting());
        tag.putFloat("melt_height", getMeltHeight());
        tag.putInt("melt_ticks", meltTicks);
        tag.putInt(SURVIVAL_TIME_TAG, survivalTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        parasiteKills = tag.getInt("parasite_kills");
        setSheepTextureVariant(tag.getInt("sheep_texture_variant"));
        setTamedWolfTexture(tag.getBoolean("tamed_wolf_texture"));
        entityData.set(MELTING, tag.getBoolean("melting"));
        entityData.set(MELT_HEIGHT, tag.getFloat("melt_height"));
        meltTicks = tag.getInt("melt_ticks");
        survivalTicks = tag.getInt(SURVIVAL_TIME_TAG);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (kind == Kind.COW) {
                int chargeState = entityData.get(COW_CHARGE_STATE);
                if (chargeState == 1) {
                    return state.setAndContinue(COW_CHARGE_WINDUP);
                }
                if (chargeState == 2) {
                    return state.setAndContinue(COW_CHARGING);
                }
            }
            if (kind == Kind.SQUID) {
                return state.setAndContinue(SWIM);
            }
            return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? RUN : IDLE);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public Kind getKind() {
        return kind;
    }

    public int getParasiteKills() {
        return parasiteKills;
    }

    public int getSheepTextureVariant() {
        return entityData.get(SHEEP_TEXTURE_VARIANT);
    }

    public void setSheepTextureVariant(int variant) {
        entityData.set(SHEEP_TEXTURE_VARIANT, Math.clamp(variant, 0, 2));
    }

    public boolean hasTamedWolfTexture() {
        return entityData.get(TAMED_WOLF_TEXTURE);
    }

    public void setTamedWolfTexture(boolean tamed) {
        entityData.set(TAMED_WOLF_TEXTURE, tamed);
    }

    public ResourceLocation getTextureResource() {
        String texture = switch (kind) {
            case SHEEP -> switch (getSheepTextureVariant()) {
                case 1 -> "sim_sheep_grey";
                case 2 -> "sim_sheep_black";
                default -> "sim_sheep";
            };
            case WOLF -> hasTamedWolfTexture() ? "sim_wolf_tamed" : "sim_wolf";
            default -> kind.id;
        };
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/" + texture + ".png");
    }

    public float getMeltHeight() {
        return Math.max(0.7F, entityData.get(MELT_HEIGHT));
    }

    public float getBaseHeight() {
        return (float) kind.baseHeight;
    }

    private void freezeMelting() {
        getNavigation().stop();
        setTarget(null);
        setDeltaMovement(Vec3.ZERO);
    }

    private void tickMelting() {
        meltTicks++;
        if (meltTicks % 20 == 0) {
            playSound(ModSounds.SIM_ADVENTURER_MELT.get(), 1.0F, 1.0F);
        }
        float height = getMeltHeight();
        if (height > 0.7F) {
            entityData.set(MELT_HEIGHT, Math.max(0.7F, height - 0.01F));
        }
        if (getMeltHeight() > 0.7F && meltTicks < kind.meltDuration) {
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        MovingFleshEntity flesh = ModEntities.MOVINGFLESH.get().create(serverLevel);
        if (flesh == null) {
            return;
        }
        flesh.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        flesh.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        flesh.setCustomName(getCustomName());
        flesh.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            flesh.setPersistenceRequired();
        }
        serverLevel.addFreshEntity(flesh);
        discard();
    }

    public boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    private void infectNearby() {
        for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(COTH_AURA_RADIUS), this::isValidParasiteTarget)) {
            InfectionMechanics.applyCoth(nearby, this, COTH_DURATION_TICKS);
        }
    }

    private void rollSheepTextureVariant() {
        float roll = random.nextFloat();
        setSheepTextureVariant(roll < 0.1F ? 2 : roll < 0.4F ? 1 : 0);
    }

    private static int sheepVariantForDye(DyeColor dyeColor) {
        return switch (dyeColor) {
            case WHITE -> 0;
            case GRAY, LIGHT_GRAY -> 1;
            case BLACK -> 2;
            default -> -1;
        };
    }

    public enum Kind {
        BEAR("sim_bear", 40.0D, 13.0D, 5.0D, 0.1D, 0.25D, 32.0D, 8, 1.4D, 1.6F, 73),
        COW("sim_cow", 18.0D, 7.0D, 5.0D, 0.4D, 0.25D, 32.0D, 6, 1.4D, 1.4F, 73),
        PIG("sim_pig", 9.0D, 3.5D, 0.1D, 0.1D, 0.30D, 24.0D, 3, 0.9D, 0.9F, 25),
        SHEEP("sim_sheep", 13.0D, 6.0D, 1.3D, 0.3D, 0.28D, 24.0D, 4, 1.3D, 1.3F, 63),
        WOLF("sim_wolf", 10.0D, 10.5D, 0.5D, 0.2D, 0.34D, 32.0D, 5, 0.85D, 0.85F, 19),
        SQUID("sim_squid", 15.0D, 11.0D, 5.0D, 0.1D, 0.26D, 24.0D, 5, 0.9D, 0.0F, 0);

        private final String id;
        private final double maxHealth;
        private final double attackDamage;
        private final double armor;
        private final double knockbackResistance;
        private final double movementSpeed;
        private final double followRange;
        private final int experience;
        private final double baseHeight;
        private final float meltStartHeight;
        private final int meltDuration;

        Kind(String id, double maxHealth, double attackDamage, double armor, double knockbackResistance,
             double movementSpeed, double followRange, int experience, double baseHeight,
             float meltStartHeight, int meltDuration) {
            this.id = id;
            this.maxHealth = maxHealth;
            this.attackDamage = attackDamage;
            this.armor = armor;
            this.knockbackResistance = knockbackResistance;
            this.movementSpeed = movementSpeed;
            this.followRange = followRange;
            this.experience = experience;
            this.baseHeight = baseHeight;
            this.meltStartHeight = meltStartHeight;
            this.meltDuration = meltDuration;
        }
    }

    private final class CowChargeGoal extends Goal {
        private static final int PREPARE_TICKS = 40;
        private static final int MAX_CHARGE_TICKS = 80;
        private int ticks;
        private Vec3 chargeDestination;

        private CowChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return chargeCooldown == 0 && target != null && target.isAlive() && onGround()
                    && !isInWaterOrBubble() && distanceToSqr(target) >= 16.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < MAX_CHARGE_TICKS && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            ticks = 0;
            chargeDestination = null;
            navigation.stop();
            entityData.set(COW_CHARGE_STATE, 1);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            ticks++;
            getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (ticks < PREPARE_TICKS) {
                return;
            }
            if (ticks == PREPARE_TICKS) {
                Vec3 direction = target.position().subtract(position()).normalize();
                chargeDestination = position().add(direction.scale(15.0D));
                navigation.moveTo(chargeDestination.x, chargeDestination.y, chargeDestination.z, 2.0D);
                entityData.set(COW_CHARGE_STATE, 2);
            }

            DragonEggAssimilationEntity.assimilateDragonEggs(level(),
                    getBoundingBox().inflate(1.0D, 0.0D, 1.0D));
            for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(1.0D, 0.0D, 1.0D), AssimilatedParasiteEntity.this::isValidParasiteTarget)) {
                if (nearby.hurt(damageSources().mobAttack(AssimilatedParasiteEntity.this),
                        (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                    Vec3 push = nearby.position().subtract(position());
                    if (push.lengthSqr() > 0.001D) {
                        push = push.normalize().scale(0.5D);
                        nearby.push(push.x, 0.25D, push.z);
                    }
                }
            }
        }

        @Override
        public void stop() {
            navigation.stop();
            chargeCooldown = 100;
            ticks = 0;
            chargeDestination = null;
            entityData.set(COW_CHARGE_STATE, 0);
        }
    }
}
