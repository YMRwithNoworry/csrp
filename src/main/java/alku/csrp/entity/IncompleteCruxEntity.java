package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/** Legacy CruxB growth form that matures into a full Crux after a random 20-60 second interval. */
public final class IncompleteCruxEntity extends CrudeParasiteEntity {
    private static final int MIN_GROW_TICKS = 20 * 20;
    private static final int MAX_GROW_TICKS = 60 * 20;
    private static final int BURST_TICKS = 70;

    // 动画定义
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation MELEE_ATTACK = ParasiteAnimations.play(this, "melee_attack");
    private final RawAnimation RANGED_ATTACK = ParasiteAnimations.play(this, "ranged_attack");
    private final RawAnimation BURST = ParasiteAnimations.play(this, "burst");

    // 数据同步器
    private static final EntityDataAccessor<Boolean> CAN_BACK =
        SynchedEntityData.defineId(IncompleteCruxEntity.class, EntityDataSerializers.BOOLEAN);

    // 动画计时器
    private float attackTimerM = 0.0F;  // 近战攻击动画计时器
    private boolean upM = false;         // 近战攻击动画上升标志
    private float attackTimerR = 0.0F;  // 远程攻击动画计时器
    private boolean upR = false;         // 远程攻击动画上升标志

    private int growthDuration;
    private int growthTicks;
    private int burstTicks = -1;

    public IncompleteCruxEntity(EntityType<? extends IncompleteCruxEntity> type, Level level) {
        super(type, level);
        growthDuration = MIN_GROW_TICKS + random.nextInt(MAX_GROW_TICKS - MIN_GROW_TICKS + 1);
        xpReward = 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CAN_BACK, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 21.0).add(Attributes.ARMOR, 3.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3).add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
    }

    @Override
    public void tick() {
        super.tick();

        // 更新动画计时器（客户端和服务端都需要）
        updateAnimationTimers();

        if (level().isClientSide || !isAlive()) {
            return;
        }

        if (burstTicks >= 0) {
            getNavigation().stop();
            if (++burstTicks >= BURST_TICKS) {
                transformIntoCrux();
            }
            return;
        }

        growthTicks++;
        if (growthTicks > growthDuration) {
            burstTicks = 0;
            triggerAnim("burst_controller", "burst");
            return;
        }
        if (getHealth() < getMaxHealth()) {
            setHealth(Math.min(getMaxHealth(), getHealth() + 0.007F));
        }
    }

    /**
     * 更新动画计时器
     * 近战动画: 上升速度 0.2/tick, 下降速度 0.1/tick, 峰值 0.9
     * 远程动画: 上升速度 0.4/tick, 下降速度 0.2/tick, 峰值 2.2
     */
    private void updateAnimationTimers() {
        // 近战攻击动画更新
        if (upM) {
            attackTimerM += 0.2F;
            if (attackTimerM > 0.9F) {
                upM = false;
            }
        } else if (attackTimerM > 0) {
            attackTimerM -= 0.1F;
            if (attackTimerM < 0) {
                attackTimerM = 0;
            }
        }

        // 远程攻击动画更新
        if (upR) {
            attackTimerR += 0.4F;
            if (attackTimerR > 2.2F) {
                upR = false;
            }
        } else if (attackTimerR > 0) {
            attackTimerR -= 0.2F;
            if (attackTimerR < 0) {
                attackTimerR = 0;
            }
        }
    }

    public float getGrowthProgress() {
        return Math.min(1.0F, growthTicks / (float) growthDuration);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            // 触发近战攻击动画
            triggerMeleeAttack();
        }
        return hit;
    }

    /**
     * 触发近战攻击动画
     * 对应原版的 handleStatusUpdate(id=22)
     */
    public void triggerMeleeAttack() {
        upM = true;
        attackTimerM = 0.0F;
        triggerAnim("melee_attack_controller", "melee_attack");
    }

    /**
     * 触发远程攻击动画
     * 对应原版的 handleStatusUpdate(id=23)
     */
    public void triggerRangedAttack() {
        upR = true;
        attackTimerR = 0.0F;
        triggerAnim("ranged_attack_controller", "ranged_attack");
    }

    /**
     * 设置后坐力状态
     * 对应原版的 handleStatusUpdate(id=24/25)
     */
    public void setCanBack(boolean canBack) {
        entityData.set(CAN_BACK, canBack);
    }

    /**
     * 获取后坐力状态
     */
    public boolean getCanBack() {
        return entityData.get(CAN_BACK);
    }

    /**
     * 获取近战攻击动画计时器（客户端使用）
     */
    public float getAttackTimerM() {
        return attackTimerM;
    }

    /**
     * 获取远程攻击动画计时器（客户端使用）
     */
    public float getAttackTimerR() {
        return attackTimerR;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("crux_growth_duration", growthDuration);
        tag.putInt("crux_growth_ticks", growthTicks);
        tag.putInt("crux_burst_ticks", burstTicks);
        tag.putFloat("crux_attack_timer_m", attackTimerM);
        tag.putBoolean("crux_up_m", upM);
        tag.putFloat("crux_attack_timer_r", attackTimerR);
        tag.putBoolean("crux_up_r", upR);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("crux_growth_duration", Tag.TAG_INT)) {
            growthDuration = Math.max(1, tag.getInt("crux_growth_duration"));
        }
        if (tag.contains("crux_growth_ticks", Tag.TAG_INT)) {
            growthTicks = Math.max(0, tag.getInt("crux_growth_ticks"));
        }
        if (tag.contains("crux_burst_ticks", Tag.TAG_INT)) {
            burstTicks = tag.getInt("crux_burst_ticks");
        }
        if (tag.contains("crux_attack_timer_m", Tag.TAG_FLOAT)) {
            attackTimerM = tag.getFloat("crux_attack_timer_m");
        }
        if (tag.contains("crux_up_m", Tag.TAG_BYTE)) {
            upM = tag.getBoolean("crux_up_m");
        }
        if (tag.contains("crux_attack_timer_r", Tag.TAG_FLOAT)) {
            attackTimerR = tag.getFloat("crux_attack_timer_r");
        }
        if (tag.contains("crux_up_r", Tag.TAG_BYTE)) {
            upR = tag.getBoolean("crux_up_r");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 移动动画控制器
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(ParasiteAnimations.isMoving(this, state.isMoving()) ? WALK : IDLE)));

        // 近战攻击动画控制器
        controllers.add(new AnimationController<>(this, "melee_attack_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP)
                .triggerableAnim("melee_attack", MELEE_ATTACK));

        // 远程攻击动画控制器
        controllers.add(new AnimationController<>(this, "ranged_attack_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP)
                .triggerableAnim("ranged_attack", RANGED_ATTACK));

        // 爆发/成长动画控制器
        controllers.add(new AnimationController<>(this, "burst_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP)
                .triggerableAnim("burst", BURST));
    }

    private void transformIntoCrux() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        CruxEntity adult = ModEntities.CRUX.get().create(serverLevel);
        if (adult != null) {
            adult.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                adult.setTarget(target);
            }
            serverLevel.addFreshEntity(adult);
        }
        discard();
    }
}
