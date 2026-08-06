package alku.csrp.entity;

import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.EnumSet;

/** Shared detonation, residue, and toxic-cloud behavior of the original carrier parasites. */
public abstract class CarrierEntity extends PrimitiveParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private static final float LOW_HEALTH_FUSE_THRESHOLD = 0.05F;
    private static final String FUSE_TICKS_TAG = "carrier_fuse_ticks";
    private final RawAnimation walkAnimation = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation attackAnimation = ParasiteAnimations.play(this, "attack");

    private final int fuseTime;
    private final int residueRadius;
    private final double viralRadius;
    private final int viralAmplifier;
    private final int cloudDuration;
    private int fuseTicks = -1;
    private boolean detonated;

    protected CarrierEntity(EntityType<? extends CarrierEntity> type, Level level, int fuseTime, int residueRadius,
            double viralRadius, int viralAmplifier, int cloudDuration) {
        super(type, level);
        this.fuseTime = fuseTime;
        this.residueRadius = residueRadius;
        this.viralRadius = viralRadius;
        this.viralAmplifier = viralAmplifier;
        this.cloudDuration = cloudDuration;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new SwellGoal());
        if (usesMeleeAttack()) {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        }
    }

    protected boolean usesMeleeAttack() {
        return true;
    }

    protected abstract RawAnimation idleAnimation();

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 运动控制器
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.0001 ? walkAnimation : idleAnimation())));

        // 攻击控制器
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state ->
                PlayState.STOP)
                .triggerableAnim("attack", attackAnimation));

        // 翅膀扇动控制器 - 持续性动画
        controllers.add(new AnimationController<>(this, "wing_controller", 0, this::wingAnimationPredicate));

        // 触须/树状结构控制器 - 持续性波动
        controllers.add(new AnimationController<>(this, "tentacle_controller", 0, this::tentacleAnimationPredicate));

        // 身体节段控制器 - 持续性波动
        controllers.add(new AnimationController<>(this, "body_controller", 0, this::bodyAnimationPredicate));

        // 爆炸膨胀控制器 - 爆炸前的缩放效果
        controllers.add(new AnimationController<>(this, "swell_controller", 0, this::swellAnimationPredicate));
    }

    /**
     * 翅膀扇动动画谓词
     * 基于原模组: wing.rotateAngleZ = 0.6F * sin(ageInTicks * 0.3F + 2.0F)
     * 频率: 0.3, 振幅: 0.6, 相位: +2.0
     */
    private <E extends CarrierEntity> PlayState wingAnimationPredicate(AnimationState<E> state) {
        AnimationProcessor processor = state.getController().getAnimationProcessor();
        if (processor == null) return PlayState.CONTINUE;

        float ageInTicks = (float) state.getAnimatable().tickCount + state.getPartialTick();
        float waveValue = 0.6F * Mth.sin(ageInTicks * 0.3F + 2.0F);

        // 翅膀扇动 - 对称动画
        GeoBone wing1 = processor.getBone("wing1");
        GeoBone wing2 = processor.getBone("wing2");
        GeoBone wing3 = processor.getBone("wing3");

        if (wing1 != null) {
            wing1.setRotZ(waveValue);
        }
        if (wing2 != null) {
            wing2.setRotZ(-1.0F * waveValue);  // 反向
        }
        if (wing3 != null) {
            wing3.setRotZ(-1.0F * waveValue);  // 反向
        }

        return PlayState.CONTINUE;
    }

    /**
     * 触须/树状结构动画谓词
     * 基于原模组: tree.offsetY = 0.2F * sin(ageInTicks * 0.3F + phase) / 4.0F
     * 相位: tree1=+2.0, tree2=+4.0, tree3=+6.0
     */
    private <E extends CarrierEntity> PlayState tentacleAnimationPredicate(AnimationState<E> state) {
        AnimationProcessor processor = state.getController().getAnimationProcessor();
        if (processor == null) return PlayState.CONTINUE;

        float ageInTicks = (float) state.getAnimatable().tickCount + state.getPartialTick();

        GeoBone tree1 = processor.getBone("tree1");
        GeoBone tree2 = processor.getBone("tree2");
        GeoBone tree3 = processor.getBone("tree3");

        if (tree1 != null) {
            float offset1 = 0.2F * Mth.sin(ageInTicks * 0.3F + 2.0F) / 4.0F;
            tree1.setPosY(tree1.getPosY() + offset1);
        }
        if (tree2 != null) {
            float offset2 = 0.2F * Mth.sin(ageInTicks * 0.3F + 4.0F) / 4.0F;
            tree2.setPosY(tree2.getPosY() + offset2);
        }
        if (tree3 != null) {
            float offset3 = 0.2F * Mth.sin(ageInTicks * 0.3F + 6.0F) / 4.0F;
            tree3.setPosY(tree3.getPosY() + offset3);
        }

        return PlayState.CONTINUE;
    }

    /**
     * 身体节段动画谓词
     * 基于原模组: body.offsetY = 0.2F * sin(ageInTicks * 0.3F + phase) / 4.0F
     * 相位映射: body1↔tree3(+6.0), body2↔tree2(+4.0), body3↔tree1(+2.0)
     */
    private <E extends CarrierEntity> PlayState bodyAnimationPredicate(AnimationState<E> state) {
        AnimationProcessor processor = state.getController().getAnimationProcessor();
        if (processor == null) return PlayState.CONTINUE;

        float ageInTicks = (float) state.getAnimatable().tickCount + state.getPartialTick();

        GeoBone body1 = processor.getBone("body1");
        GeoBone body2 = processor.getBone("body2");
        GeoBone body3 = processor.getBone("body3");

        if (body1 != null) {
            float offset1 = 0.2F * Mth.sin(ageInTicks * 0.3F + 6.0F) / 4.0F;
            body1.setPosY(body1.getPosY() + offset1);
        }
        if (body2 != null) {
            float offset2 = 0.2F * Mth.sin(ageInTicks * 0.3F + 4.0F) / 4.0F;
            body2.setPosY(body2.getPosY() + offset2);
        }
        if (body3 != null) {
            float offset3 = 0.2F * Mth.sin(ageInTicks * 0.3F + 2.0F) / 4.0F;
            body3.setPosY(body3.getPosY() + offset3);
        }

        return PlayState.CONTINUE;
    }

    /**
     * 爆炸膨胀动画谓词
     * 基于原模组 RenderButhol.preRenderCallback:
     * - 使用 getCreeperFlashIntensity(partialTick) 获取爆炸进度 (0.0-1.0)
     * - 应用四次方曲线加速
     * - XZ缩放 +40%, Y缩放 +10%
     * - 叠加正弦波抖动效果
     */
    private <E extends CarrierEntity> PlayState swellAnimationPredicate(AnimationState<E> state) {
        if (!isDetonating()) {
            return PlayState.CONTINUE;
        }

        AnimationProcessor processor = state.getController().getAnimationProcessor();
        if (processor == null) return PlayState.CONTINUE;

        float swellProgress = getSwellProgress(state.getPartialTick());

        // 四次方曲线
        float f = Mth.clamp(swellProgress, 0.0F, 1.0F);
        f = f * f * f * f;

        // 正弦波抖动
        float f1 = 1.0F + Mth.sin(swellProgress * 100.0F) * swellProgress * 0.01F;

        // XZ缩放 +40%, Y缩放 +10%
        float scaleXZ = (1.0F + f * 0.4F) * f1;
        float scaleY = (1.0F + f * 0.1F) / f1;

        // 应用到根骨骼
        GeoBone root = processor.getBone("root");
        if (root != null) {
            root.setScaleX(scaleXZ);
            root.setScaleY(scaleY);
            root.setScaleZ(scaleXZ);
        }

        return PlayState.CONTINUE;
    }

    /**
     * 获取爆炸膨胀进度 (模拟原版苦力怕的 getCreeperFlashIntensity)
     * @param partialTick 部分刻
     * @return 0.0-1.0 的进度值
     */
    public float getSwellProgress(float partialTick) {
        if (fuseTicks < 0 || fuseTime <= 0) {
            return 0.0F;
        }
        return Mth.clamp((fuseTicks + partialTick) / (float) fuseTime, 0.0F, 1.0F);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("attack_controller", "attack");
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || detonated || !isAlive()) {
            return;
        }

        if (getHealth() < getMaxHealth() * LOW_HEALTH_FUSE_THRESHOLD) {
            startFuse();
        }
        if (fuseTicks >= 0 && ++fuseTicks >= fuseTime) {
            detonate();
        }
    }

    protected final void startFuse() {
        if (fuseTicks < 0) {
            fuseTicks = 0;
            getNavigation().stop();
            triggerAnim("attack_controller", "attack");
        }
    }

    public final boolean isDetonating() {
        return fuseTicks >= 0 && !detonated;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(FUSE_TICKS_TAG, fuseTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(FUSE_TICKS_TAG, Tag.TAG_INT)) {
            fuseTicks = tag.getInt(FUSE_TICKS_TAG);
        }
    }

    private void detonate() {
        if (!(level() instanceof ServerLevel) || detonated) {
            return;
        }

        detonated = true;
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(4.0D));
        Level.ExplosionInteraction interaction = level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), 4.0F, interaction);
        applyExplosionEffects();
        spreadResidue();
        spawnLingeringCloud();
        spawnGnats();
        super.die(damageSources().mobAttack(this));
        discard();
    }

    protected int gnatSpawnCount() {
        return 0;
    }

    private void spawnGnats() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int index = 0; index < gnatSpawnCount(); index++) {
            GnatEntity gnat = ModEntities.GNAT.get().create(serverLevel, null, blockPosition(),
                    MobSpawnType.MOB_SUMMONED, false, false);
            if (gnat == null) {
                continue;
            }
            gnat.moveTo(getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + 0.2D, getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    random.nextFloat() * 360.0F, 0.0F);
            gnat.setDeltaMovement((random.nextDouble() - 0.5D) * 0.35D,
                    0.2D + random.nextDouble() * 0.2D,
                    (random.nextDouble() - 0.5D) * 0.35D);
            gnat.setTarget(getTarget());
            serverLevel.addFreshEntity(gnat);
        }
    }

    private void applyExplosionEffects() {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(viralRadius), this::isValidParasiteTarget)) {
            target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 400, viralAmplifier), this);
        }
    }

    private void spreadResidue() {
        if (residueRadius <= 0) {
            return;
        }

        BlockPos origin = blockPosition();
        for (int x = -residueRadius; x <= residueRadius; x++) {
            for (int z = -residueRadius; z <= residueRadius; z++) {
                if (random.nextBoolean()) {
                    placeResidueAtFloor(origin.offset(x, 3, z));
                }
            }
        }
    }

    private void placeResidueAtFloor(BlockPos start) {
        for (int y = 0; y <= 6; y++) {
            BlockPos candidate = start.below(y);
            if (level().isEmptyBlock(candidate) && !level().isEmptyBlock(candidate.below())) {
                level().setBlock(candidate, ModBlocks.INFESTED_REMAINS.get().defaultBlockState(), 3);
                return;
            }
        }
    }

    private void spawnLingeringCloud() {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        float radius = getBbWidth() * 3.5F;
        cloud.setOwner(this);
        cloud.setRadius(radius);
        cloud.setWaitTime(10);
        cloud.setDuration(cloudDuration);
        cloud.setRadiusPerTick(-radius / cloudDuration);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 0));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0, false, false));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 3600, 0, false, false));
        level().addFreshEntity(cloud);
    }

    private final class SwellGoal extends Goal {
        private SwellGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && distanceToSqr(target) < 9.0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && distanceToSqr(target) < 49.0 && !detonated;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            startFuse();
        }
    }
}
