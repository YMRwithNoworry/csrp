package alku.csrp.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Vigile (原模组 EntityAnged) - 具有触手的纯种寄生体
 * 根据状态切换不同的动画和速度
 */
public final class VigileEntity extends PrimitiveParasiteEntity {
    // 动画定义
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation RANGED_ATTACK = ParasiteAnimations.play(this, "ranged_attack");
    private final RawAnimation DEATH = ParasiteAnimations.play(this, "death");

    public VigileEntity(EntityType<? extends VigileEntity> type, Level level) {
        super(type, level);
        xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 65.0D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主要移动动画控制器，包含状态切换逻辑
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));

        // 攻击动画控制器（触发式）
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("ranged_attack", RANGED_ATTACK));

        // 死亡动画控制器
        controllers.add(new AnimationController<>(this, "death_controller", 0, this::deathAnimation));
    }

    /**
     * 移动动画逻辑
     * 根据实体的移动状态选择合适的动画
     */
    private PlayState movementAnimation(AnimationState<VigileEntity> state) {
        // 死亡时不播放移动动画
        if (!isAlive()) {
            return PlayState.STOP;
        }

        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(IDLE);
        }

        // Actual movement is confirmed above; velocity is only used to choose walk or run.
        double horizontalSpeed = getDeltaMovement().horizontalDistanceSqr();

        // 快速移动 - run 动画（状态0：正常移动）
        if (horizontalSpeed > 0.02D) {
            return state.setAndContinue(RUN);
        }

        // 慢速移动 - walk 动画（状态1/2：攻击/远程攻击）
        return state.setAndContinue(WALK);
    }

    /**
     * 死亡动画逻辑
     * 播放死亡动画（状态25）
     */
    private PlayState deathAnimation(AnimationState<VigileEntity> state) {
        if (!isAlive()) {
            return state.setAndContinue(DEATH);
        }
        return PlayState.STOP;
    }

    /**
     * 获取寄生体状态
     * 用于在动画JSON中通过 query.get_parasite_status 查询
     *
     * @return 0=正常移动, 1=攻击, 2=远程攻击, 25=死亡
     */
    public int getParasiteStatus() {
        if (!isAlive()) {
            return 25; // 死亡状态
        }

        // 可以根据实际行为扩展状态判断
        // 例如：检测是否正在攻击、是否有目标等

        return 0; // 默认正常移动状态
    }

    /**
     * 触发攻击动画
     */
    public void triggerAttackAnimation() {
        triggerAnim("attack_controller", "attack");
    }

    /**
     * 触发远程攻击动画
     */
    public void triggerRangedAttackAnimation() {
        triggerAnim("attack_controller", "ranged_attack");
    }
}
