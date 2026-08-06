package alku.csrp.entity;

import alku.csrp.Csrp;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Legacy assimilated sheep with texture variants, melting mechanism, and COTH aura. */
public final class AssimilatedSheepEntity extends Monster implements GeoEntity, Parasite {
    public static final int FERAL_KILL_THRESHOLD = 60;
    private static final int COTH_DURATION_TICKS = 4_800;
    private static final int COTH_AURA_RADIUS = 8;
    private static final EntityDataAccessor<Integer> SHEEP_TEXTURE_VARIANT =
            SynchedEntityData.defineId(AssimilatedSheepEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MELTING =
            SynchedEntityData.defineId(AssimilatedSheepEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> MELT_HEIGHT =
            SynchedEntityData.defineId(AssimilatedSheepEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(AssimilatedSheepEntity.class, EntityDataSerializers.INT);

    // 动画定义 - 基于原模组的动画状态
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation MELTING_ANIM = ParasiteAnimations.loop(this, "melting");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int parasiteKills;
    private int meltTicks;

    // 羊属性常量
    private static final double BASE_HEIGHT = 1.3D;
    private static final float MELT_START_HEIGHT = 1.3F;
    private static final int MELT_DURATION = 63;

    public AssimilatedSheepEntity(EntityType<? extends AssimilatedSheepEntity> type, Level level) {
        super(type, level);
        xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 13.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 1.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHEEP_TEXTURE_VARIANT, 0);
        builder.define(MELTING, false);
        builder.define(MELT_HEIGHT, 0.0F);
        builder.define(PARASITE_STATUS, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
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

        // 根据运动状态更新动画状态
        updateParasiteStatus();

        if (tickCount % 20 == 0) {
            infectNearby();
        }

    }

    /**
     * 根据实体状态更新寄生体状态值，用于驱动动画
     */
    private void updateParasiteStatus() {
        if (isMelting()) {
            entityData.set(PARASITE_STATUS, 6); // 融化状态
            return;
        }

        LivingEntity target = getTarget();
        double speed = getDeltaMovement().horizontalDistanceSqr();

        if (target != null && distanceToSqr(target) < 16.0D) {
            // 冲刺状态
            entityData.set(PARASITE_STATUS, 2);
        } else if (target != null) {
            // 攻击/激活状态
            entityData.set(PARASITE_STATUS, 1);
        } else {
            // 正常行走状态
            entityData.set(PARASITE_STATUS, 0);
        }
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        LivingEntity livingTarget = entity instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(entity);
        if (hit && livingTarget != null) {
            triggerAnim("attack_controller", "attack");
            ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
            InfectionMechanics.applyCoth(livingTarget, this);
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
        if (parasiteKills >= FERAL_KILL_THRESHOLD) {
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
        if (level().isClientSide || random.nextFloat() >= 0.5F || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AssimilatedHeadEntity head = ModEntities.SIM_SHEEP_HEAD.get().create(serverLevel);
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
        FeralParasiteEntity feral = ModEntities.FER_SHEEP.get().create(level);
        if (feral == null) {
            return;
        }

        feral.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        feral.setTarget(getTarget());
        feral.setCustomName(getCustomName());
        feral.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            feral.setPersistenceRequired();
        }
        level.addFreshEntity(feral);
        discard();
    }

    public boolean canMelt() {
        return !isMelting();
    }

    public void melt() {
        if (!canMelt()) {
            return;
        }
        entityData.set(MELTING, true);
        entityData.set(MELT_HEIGHT, MELT_START_HEIGHT);
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
        return dimensions.scale(1.0F, getMeltHeight() / (float) BASE_HEIGHT);
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
            rollSheepTextureVariant();
        }
        return data;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
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
        tag.putBoolean("melting", isMelting());
        tag.putFloat("melt_height", getMeltHeight());
        tag.putInt("melt_ticks", meltTicks);
        tag.putInt("parasite_status", getParasiteStatus());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        parasiteKills = tag.getInt("parasite_kills");
        setSheepTextureVariant(tag.getInt("sheep_texture_variant"));
        entityData.set(MELTING, tag.getBoolean("melting"));
        entityData.set(MELT_HEIGHT, tag.getFloat("melt_height"));
        meltTicks = tag.getInt("melt_ticks");
        entityData.set(PARASITE_STATUS, tag.getInt("parasite_status"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主要运动控制器 - 根据状态选择不同的动画
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            int status = getParasiteStatus();
            boolean moving = state.isMoving();

            // 状态 6: 融化状态
            if (status == 6 || isMelting()) {
                return state.setAndContinue(MELTING_ANIM);
            }

            // 状态 2: 冲刺状态 (快速移动)
            if (status == 2) {
                return state.setAndContinue(moving ? RUN : IDLE);
            }

            // 状态 1: 攻击/激活状态
            if (status == 1) {
                return state.setAndContinue(moving ? WALK : IDLE);
            }

            // 状态 0: 正常行走状态
            return state.setAndContinue(moving ? WALK : IDLE);
        }));

        // 攻击控制器 - 可触发的攻击动画
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
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

    public ResourceLocation getTextureResource() {
        String texture = switch (getSheepTextureVariant()) {
            case 1 -> "sim_sheep_grey";
            case 2 -> "sim_sheep_black";
            default -> "sim_sheep";
        };
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "textures/entity/" + texture + ".png");
    }

    public float getMeltHeight() {
        return Math.max(0.7F, entityData.get(MELT_HEIGHT));
    }

    public float getBaseHeight() {
        return (float) BASE_HEIGHT;
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
        if (getMeltHeight() > 0.7F && meltTicks < MELT_DURATION) {
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
}
