package alku.csrp.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/** Shared hostile state for legacy hijacked mobs. */
public abstract class HijackedParasiteEntity extends PrimitiveParasiteEntity {
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    protected HijackedParasiteEntity(EntityType<? extends HijackedParasiteEntity> type, Level level, int experience) {
        super(type, level);
        xpReward = experience;
    }

    protected static AttributeSupplier.Builder createAttributes(double health, double armor, double damage,
                                                                 double knockbackResistance, double movementSpeed,
                                                                 double followRange) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance)
                .add(Attributes.MOVEMENT_SPEED, movementSpeed)
                .add(Attributes.FOLLOW_RANGE, followRange);
    }

    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            triggerAttackAnimation();
        }
        return hurt;
    }

    protected final void triggerAttackAnimation() {
        triggerAnim("attack_controller", "attack");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.0001 ? RUN : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0,
                state -> PlayState.STOP).triggerableAnim("attack", ATTACK));
    }
}
