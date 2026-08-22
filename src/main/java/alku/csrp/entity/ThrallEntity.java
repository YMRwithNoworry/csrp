package alku.csrp.entity;

import alku.csrp.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

public final class ThrallEntity extends CrudeParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return true;
    }
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");

    public ThrallEntity(EntityType<? extends ThrallEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ARMOR, 5.0)
                .add(Attributes.ATTACK_DAMAGE, 13.0).add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit) {
        }
        if (hit && entity instanceof Player target && hasCustomName()
                && target.getGameProfile().getName().equals(getCustomName().getString())) {
            target.hurt(damageSources().mobAttack(this),
                    (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.THRALL_LIVING.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.THRALL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.THRALL_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(ParasiteAnimations.isMoving(this, state.isMoving())
                        ? LIMB_SWING : AGE_IN_TICKS)));
    }
}
