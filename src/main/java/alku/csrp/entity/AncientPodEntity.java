package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/** Legacy Ancient Drop Pod (EntityDropPod). */
public final class AncientPodEntity extends PrimitiveParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private static final int DEFAULT_FUSE = 80;

    private final RawAnimation idleAnimation = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation airborneAnimation = ParasiteAnimations.loop(
            this, "idle.get_parasite_status_1");
    private byte owner = 62;
    private int fuseTicks = DEFAULT_FUSE;
    private boolean fuseStarted;
    private boolean exploded;

    public AncientPodEntity(EntityType<? extends AncientPodEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!fuseStarted && onGround()) {
            fuseStarted = true;
        }
        if (!fuseStarted || exploded) {
            return;
        }
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0D, getZ(),
                    4, 0.35D, 0.5D, 0.35D, 0.02D);
            if (--fuseTicks <= 0) {
                explodePod(serverLevel);
            }
        }
    }

    public void setOwner(byte owner) {
        this.owner = owner;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 2, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "hair_controller", 0, this::hairAnimation));
        controllers.add(new AnimationController<>(this, "tentacle_controller", 0, this::tentacleAnimation));
        controllers.add(new AnimationController<>(this, "body_controller", 0, this::bodyAnimation));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("pod_owner", owner);
        tag.putInt("pod_fuse", fuseTicks);
        tag.putBoolean("pod_fuse_started", fuseStarted);
        tag.putBoolean("pod_exploded", exploded);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.contains("pod_owner") ? tag.getByte("pod_owner") : 62;
        fuseTicks = tag.contains("pod_fuse") ? tag.getInt("pod_fuse") : DEFAULT_FUSE;
        fuseStarted = tag.getBoolean("pod_fuse_started");
        exploded = tag.getBoolean("pod_exploded");
    }

    private PlayState movementAnimation(AnimationState<AncientPodEntity> state) {
        // The legacy pod uses status 1 while falling and returns to its normal pose on landing.
        return state.setAndContinue(onGround() ? idleAnimation : airborneAnimation);
    }

    /**
     * 触须/毛发动画控制器 - 周期性摆动
     * 使用正弦波函数控制不同部位的触须
     * - 后部触须: 频率 0.1133, 幅度 0.091
     * - 中部触须: 频率 0.1, 幅度 -0.07 (反向)
     * - 前部触须: 频率 0.13, 幅度 0.08
     */
    private PlayState hairAnimation(AnimationState<AncientPodEntity> state) {
        if (exploded) {
            return PlayState.STOP;
        }

        long ticks = state.getAnimatable().tickCount;
        float ageInTicks = ticks + state.getPartialTick();

        // 计算三组触须的摆动角度
        float backHairAngle = (float) Math.sin(ageInTicks * 0.1133F) * 0.091F;
        float middleHairAngle = -1.0F * (float) Math.sin(ageInTicks * 0.1F) * 0.07F;
        float frontHairAngle = (float) Math.sin(ageInTicks * 0.13F) * 0.08F;

        // 应用动画数据 (实际的骨骼旋转会在模型/渲染层处理)
        // 这里仅作为动画状态的驱动

        return PlayState.CONTINUE;
    }

    /**
     * 触手动画控制器 - 多轴旋转动画
     * 使用余弦波函数控制触手关节的旋转
     * - Y轴旋转: 频率 0.19, 基础幅度 0.0633
     * - Z轴旋转: 频率 0.21, 基础幅度 0.114
     * - X轴旋转: 频率 0.2, 基础幅度 0.18
     */
    private PlayState tentacleAnimation(AnimationState<AncientPodEntity> state) {
        if (exploded) {
            return PlayState.STOP;
        }

        long ticks = state.getAnimatable().tickCount;
        float ageInTicks = ticks + state.getPartialTick();

        // 计算触手关节的旋转角度
        float tentacleYRotation = 0.3F * (float) Math.cos(ageInTicks * 0.19F) * 0.211F;
        float tentacleZRotation = 0.6F * (float) Math.cos(ageInTicks * 0.21F) * 0.19F;
        float tentacleXRotation = 0.9F * (float) Math.cos(ageInTicks * 0.2F) * 0.2F;

        return PlayState.CONTINUE;
    }

    /**
     * 主体动画控制器 - 根据状态切换
     * 状态 1 (空中): 高频振动模拟不稳定
     * 状态 0 (触地): 停止偏移动画，准备爆炸
     */
    private PlayState bodyAnimation(AnimationState<AncientPodEntity> state) {
        if (exploded) {
            return PlayState.STOP;
        }

        long ticks = state.getAnimatable().tickCount;
        float ageInTicks = ticks + state.getPartialTick();

        // 空中状态 - 快速振动
        if (!onGround()) {
            float bodyOffsetY = (float) Math.cos(ageInTicks * 1.29F) * 0.0333F;
            float bodyOffsetZ = (float) Math.cos(ageInTicks * 1.31F) * 0.054F;
            // 偏移动画由渲染层应用
        } else {
            // 触地状态 - 停止主体偏移
            // 准备爆炸效果
        }

        return PlayState.CONTINUE;
    }

    private void explodePod(ServerLevel level) {
        exploded = true;
        DragonEggAssimilationEntity.assimilateDragonEggs(level, getBoundingBox().inflate(4.0D));
        Level.ExplosionInteraction interaction = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level.explode(this, getX(), getY(), getZ(), 4.0F, interaction);
        spawnLingeringCloud(level);
        spawnContents(level);
        discard();
    }

    private void spawnLingeringCloud(ServerLevel level) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(getBbWidth() * 2.0F);
        cloud.setWaitTime(5);
        cloud.setDuration(600);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 0, false, false));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0, false, false));
        level.addFreshEntity(cloud);
    }

    private void spawnContents(ServerLevel level) {
        int count = owner == 62 ? 5 : owner == 63 ? 1 : 0;
        for (int index = 0; index < count; index++) {
            Mob mob = random.nextBoolean() ? ModEntities.BUGLIN.get().create(level) : ModEntities.RUPTER.get().create(level);
            if (mob == null) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2.0D;
            mob.moveTo(getX() + Math.cos(angle) * 1.5D, getY(), getZ() + Math.sin(angle) * 1.5D,
                    random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            mob.setTarget(getTarget());
            level.addFreshEntity(mob);
        }
    }
}
