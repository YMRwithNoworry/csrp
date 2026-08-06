package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public final class HostIIEntity extends AbstractHostEntity {
    // 动画状态数据访问器
    private static final EntityDataAccessor<Float> BURIED_TIMER =
            SynchedEntityData.defineId(HostIIEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> MOUTH_OPEN =
            SynchedEntityData.defineId(HostIIEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ATTACK_TIMER =
            SynchedEntityData.defineId(HostIIEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ATTACK_UP =
            SynchedEntityData.defineId(HostIIEntity.class, EntityDataSerializers.BOOLEAN);

    // 动画常量
    private static final float MAX_BURIED_TIMER = 8.3F;
    private static final float BURIED_INCREMENT = 0.1F;
    private static final float ATTACK_RISE_RATE = 0.2F;
    private static final float ATTACK_FALL_RATE = 0.1F;
    private static final float MAX_ATTACK_TIMER = 1.0F;

    @Override
    public boolean supportsDamageAdaptation() {
        return true;
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
        return 0.95F;
    }
    public static final int BURROW_DURATION_TICKS = 120;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation BURROW = ParasiteAnimations.loop(this, "get_burrow_timer.get_burrowed_1");
    private final RawAnimation BURROWED = ParasiteAnimations.loop(this, "idle.get_burrowed_1");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation BURROWED_ATTACK =
            ParasiteAnimations.play(this, "idle.get_burrowed_1.get_open_1");

    public HostIIEntity(EntityType<? extends HostIIEntity> type, Level level) {
        super(type, level, 0.12, 5.0, 5.0, BURROW_DURATION_TICKS, 20, 20);
        xpReward = 35;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURIED_TIMER, MAX_BURIED_TIMER);
        builder.define(MOUTH_OPEN, false);
        builder.define(ATTACK_TIMER, 0.0F);
        builder.define(ATTACK_UP, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createHostAttributes(140.0, 12.0, 18.0, 0.12, 32.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            updateBuriedTimer();
            updateAttackTimer();
            updateMouthState();
        }
    }

    /**
     * 更新潜地计时器
     * 潜地状态时: buried 从 8.3F 递减到 0.0F
     * 展开状态时: buried 从 0.0F 递增到 8.3F
     */
    private void updateBuriedTimer() {
        float currentBuried = entityData.get(BURIED_TIMER);
        if (isBurrowed()) {
            // 潜地时减少计时器
            if (currentBuried > 0.0F) {
                entityData.set(BURIED_TIMER, Math.max(0.0F, currentBuried - BURIED_INCREMENT));
            }
        } else {
            // 展开时增加计时器
            if (currentBuried < MAX_BURIED_TIMER) {
                entityData.set(BURIED_TIMER, Math.min(MAX_BURIED_TIMER, currentBuried + BURIED_INCREMENT));
            }
        }
    }

    /**
     * 更新攻击计时器
     * 上升阶段: 每tick +0.2F，直到 > 1.0F
     * 下降阶段: 每tick -0.1F，直到归零
     */
    private void updateAttackTimer() {
        float currentTimer = entityData.get(ATTACK_TIMER);
        boolean up = entityData.get(ATTACK_UP);

        if (up) {
            currentTimer += ATTACK_RISE_RATE;
            if (currentTimer >= MAX_ATTACK_TIMER) {
                entityData.set(ATTACK_UP, false);
            }
        } else if (currentTimer > 0.0F) {
            currentTimer -= ATTACK_FALL_RATE;
        }

        entityData.set(ATTACK_TIMER, Math.max(0.0F, currentTimer));
    }

    /**
     * 更新嘴部状态
     * 只有在完全潜地时(buried <= 0)且潜地状态为true时才张开嘴
     */
    private void updateMouthState() {
        boolean shouldOpen = isBurrowed() && entityData.get(BURIED_TIMER) <= 0.0F;
        entityData.set(MOUTH_OPEN, shouldOpen);
    }

    @Override
    protected void performRangedAttack(LivingEntity target) {
        double distance = distanceToSqr(target);
        if (distance > 25.0 && distance < 225.0 && random.nextInt(4) == 0) {
            performBombAttack(target);
        } else {
            performSpineBallAttack(target);
        }
    }

    private void performBombAttack(LivingEntity target) {
        spawnProjectile(ParasiteProjectileEntity.Mode.BOMB, target, 0.8, 20.0F, 5.0, 40);
    }

    private void performSpineBallAttack(LivingEntity target) {
        spawnProjectile(ParasiteProjectileEntity.Mode.SPINE, target, 1.1, 11.0F, 1.5, 60);
    }

    @Override
    protected void summonMinions() {
        summonManglers();
    }

    @Override
    protected void triggerAttackAnimation() {
        // 触发攻击动画并重置攻击计时器
        entityData.set(ATTACK_UP, true);
        entityData.set(ATTACK_TIMER, 0.0F);
        triggerAnim("attack_controller", isBurrowed() ? "burrowed_attack" : "attack");
    }

    // 客户端动画数据访问器
    public float getBuriedTimer() {
        return entityData.get(BURIED_TIMER);
    }

    public boolean isMouthOpen() {
        return entityData.get(MOUTH_OPEN);
    }

    public float getAttackTimer() {
        return entityData.get(ATTACK_TIMER);
    }

    public boolean isAttackUp() {
        return entityData.get(ATTACK_UP);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("buried_timer", getBuriedTimer());
        tag.putBoolean("mouth_open", isMouthOpen());
        tag.putFloat("attack_timer", getAttackTimer());
        tag.putBoolean("attack_up", isAttackUp());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(BURIED_TIMER, tag.getFloat("buried_timer"));
        entityData.set(MOUTH_OPEN, tag.getBoolean("mouth_open"));
        entityData.set(ATTACK_TIMER, tag.getFloat("attack_timer"));
        entityData.set(ATTACK_UP, tag.getBoolean("attack_up"));
    }

    private void summonManglers() {
        spawnMinions(ModEntities.MANGLER, ManglerEntity.class, 4);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 移动控制器 - 处理idle、walk、burrow、burrowed状态
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> {
                    // 优先级1: 潜地过渡动画
                    if (getBurrowAnimationTicks() > 0) {
                        return state.setAndContinue(BURROW);
                    }
                    // 优先级2: 完全潜地状态
                    if (isBurrowed()) {
                        return state.setAndContinue(BURROWED);
                    }
                    // 优先级3: 行走或待机
                    return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? WALK : IDLE);
                }));

        // 攻击控制器 - 处理attack和burrowed_attack状态
        controllers.add(new AnimationController<>(this, "attack_controller", 0,
                state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("burrowed_attack", BURROWED_ATTACK));
    }
}
