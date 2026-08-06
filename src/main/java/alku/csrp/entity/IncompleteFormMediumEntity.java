package alku.csrp.entity;

import alku.csrp.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public final class IncompleteFormMediumEntity extends IncompleteFormSmallEntity {
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    public IncompleteFormMediumEntity(EntityType<? extends IncompleteFormMediumEntity> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.MOVEMENT_SPEED, 0.15)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.INCOMPLETE_MEDIUM_LIVING.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.INCOMPLETE_MEDIUM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.INCOMPLETE_MEDIUM_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主要移动动画控制器
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(ParasiteAnimations.isMoving(this, state.isMoving()) ? WALK : IDLE)));

        // 攻击动画控制器
        controllers.add(new AnimationController<>(this, "attack_controller", 0,
                state -> PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    // 注意：原模组的程序化关节动画（animateJoints）依赖于不存在的GeckoLib API
    // (setBoneAnimationProcessor 和 DataTickets.ANIMATION_PROCESSOR)
    // 这些复杂的正弦波动画需要在模型文件的动画中实现，或等待GeckoLib提供相应的API
}
