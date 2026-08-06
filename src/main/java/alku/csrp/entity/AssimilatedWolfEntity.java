package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Legacy assimilated Wolf with parasitic mutations. */
public final class AssimilatedWolfEntity extends Monster implements GeoEntity, Parasite {
    // 状态数据
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            AssimilatedWolfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STILL_ANI = SynchedEntityData.defineId(
            AssimilatedWolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> T_HEIGHT = SynchedEntityData.defineId(
            AssimilatedWolfEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SHRIMP_FED = SynchedEntityData.defineId(
            AssimilatedWolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TAMED_TEXTURE = SynchedEntityData.defineId(
            AssimilatedWolfEntity.class, EntityDataSerializers.BOOLEAN);

    // 融化相关常量
    private static final int MELT_SOUND_INTERVAL = 20;
    private static final float SIZE_DECREASE_RATE = 0.005F;
    private static final float HEIGHT_DECREASE_RATE = 0.01F;
    private static final float MELT_TRANSFORM_THRESHOLD = 0.7F;

    // 动画定义 - 状态0和1（正常行走/攻击）
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    // 动画定义 - 状态2（特殊移动模式）
    private final RawAnimation SPECIAL_IDLE = ParasiteAnimations.loop(this, "idle.special_mode");
    private final RawAnimation SPECIAL_WALK = ParasiteAnimations.loop(this, "walk.special_mode");

    // 动画定义 - 状态6（融化动画）
    private final RawAnimation MELTING = ParasiteAnimations.loop(this, "melting");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int meltTicks;
    private float currentSize = 1.0F;

    public AssimilatedWolfEntity(EntityType<? extends AssimilatedWolfEntity> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, 0);
        builder.define(STILL_ANI, false);
        builder.define(T_HEIGHT, 0.0F);
        builder.define(SHRIMP_FED, false);
        builder.define(TAMED_TEXTURE, false);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType spawnType,
            net.minecraft.world.entity.SpawnGroupData spawnGroupData) {
        net.minecraft.world.entity.SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        // 1% 概率生成驯服狼材质变体
        if (random.nextFloat() < 0.01F) {
            entityData.set(TAMED_TEXTURE, true);
        }

        // 随机初始状态（0, 1, 或 2）
        int initialStatus = random.nextInt(3);
        entityData.set(PARASITE_STATUS, initialStatus);

        return data;
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    public boolean getStillAni() {
        return entityData.get(STILL_ANI);
    }

    public void setStillAni(boolean still) {
        entityData.set(STILL_ANI, still);
    }

    public float getTHeight() {
        return entityData.get(T_HEIGHT);
    }

    public void setTHeight(float height) {
        entityData.set(T_HEIGHT, height);
    }

    public boolean isShrimpFed() {
        return entityData.get(SHRIMP_FED);
    }

    private void setShrimpFed(boolean fed) {
        entityData.set(SHRIMP_FED, fed);
    }

    public boolean hasTamedTexture() {
        return entityData.get(TAMED_TEXTURE);
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
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.SHRIMP.get()) || isShrimpFed()) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide) {
            setShrimpFed(true);
            playSound(ModSounds.get("shrimp.eat"), 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            spawnMeltParticles();
            return;
        }

        // 处理融化状态
        int status = getParasiteStatus();
        if (status == 6) {
            handleMelting();
        }
    }

    private void handleMelting() {
        meltTicks++;

        // 每20tick播放融化音效
        if (meltTicks % MELT_SOUND_INTERVAL == 0) {
            playSound(ModSounds.get("parasite.melt"), 1.0F, 1.0F);
        }

        // 逐渐降低高度
        float currentHeight = getTHeight();
        if (currentHeight == 0.0F) {
            setTHeight(0.85F);
            currentHeight = 0.85F;
        }
        setTHeight(currentHeight - HEIGHT_DECREASE_RATE);

        // 缩小尺寸
        currentSize = Math.max(0.0F, currentSize - SIZE_DECREASE_RATE);

        // 转换为Lesh实体
        if (currentHeight <= MELT_TRANSFORM_THRESHOLD && level() instanceof ServerLevel serverLevel) {
            transformToLesh(serverLevel);
        }
    }

    private void transformToLesh(ServerLevel serverLevel) {
        // 转换为MovingFlesh实体
        Entity flesh = ModEntities.MOVINGFLESH.get().create(serverLevel);
        if (flesh instanceof LivingEntity living) {
            living.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            serverLevel.addFreshEntity(living);
        }
        discard();
    }

    private void spawnMeltParticles() {
        int status = getParasiteStatus();
        if (status == 6) {
            // 红色和黄色云粒子
            for (int i = 0; i < 2; i++) {
                double offsetX = (random.nextDouble() - 0.5D) * getBbWidth();
                double offsetY = random.nextDouble() * getBbHeight();
                double offsetZ = (random.nextDouble() - 0.5D) * getBbWidth();

                if (random.nextBoolean()) {
                    level().addParticle(ParticleTypes.CRIMSON_SPORE,
                            getX() + offsetX, getY() + offsetY, getZ() + offsetZ,
                            0.0D, 0.01D, 0.0D);
                } else {
                    level().addParticle(ParticleTypes.FLAME,
                            getX() + offsetX, getY() + offsetY, getZ() + offsetZ,
                            0.0D, 0.01D, 0.0D);
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        LivingEntity livingTarget = entity instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(entity);
        if (hit) {
            triggerAnim("attack_controller", "attack");
        }
        if (hit && livingTarget != null) {
            ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
            InfectionMechanics.applyCoth(livingTarget, this);
        }
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasite_status", getParasiteStatus());
        tag.putBoolean("still_ani", getStillAni());
        tag.putFloat("t_height", getTHeight());
        tag.putBoolean("shrimp_fed", isShrimpFed());
        tag.putBoolean("tamed_texture", hasTamedTexture());
        tag.putInt("melt_ticks", meltTicks);
        tag.putFloat("current_size", currentSize);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setParasiteStatus(tag.getInt("parasite_status"));
        setStillAni(tag.getBoolean("still_ani"));
        setTHeight(tag.getFloat("t_height"));
        setShrimpFed(tag.getBoolean("shrimp_fed"));
        entityData.set(TAMED_TEXTURE, tag.getBoolean("tamed_texture"));
        meltTicks = tag.getInt("melt_ticks");
        currentSize = tag.getFloat("current_size");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主移动控制器
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            int status = getParasiteStatus();
            boolean moving = !getStillAni() && ParasiteAnimations.isMoving(this, state.isMoving());

            // 状态6：融化动画
            if (status == 6) {
                return state.setAndContinue(MELTING);
            }

            // 状态2：特殊移动模式
            if (status == 2) {
                return state.setAndContinue(moving ? SPECIAL_WALK : SPECIAL_IDLE);
            }

            // 状态0和1：正常行走/攻击
            return state.setAndContinue(moving ? WALK : IDLE);
        }));

        // 攻击控制器
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    /**
     * 触发融化效果
     */
    public void startMelting() {
        setParasiteStatus(6);
        setTHeight(0.85F);
        meltTicks = 0;
    }
}
