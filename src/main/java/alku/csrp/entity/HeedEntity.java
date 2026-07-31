package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public final class HeedEntity extends CrudeParasiteEntity {
    private static final int SIGNAL_COOLDOWN_TICKS = 1000;
    private static final int RAGE_COOLDOWN_TICKS = 200;
    private static final int RAGE_DURATION_TICKS = 1200;
    private final RawAnimation ANIMATION = ParasiteAnimations.loop(this, "idle");

    private int signalCooldown = SIGNAL_COOLDOWN_TICKS;
    private int rageCooldown = RAGE_COOLDOWN_TICKS;

    public HeedEntity(EntityType<? extends HeedEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ARMOR, 9.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.7F));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;

        if (--signalCooldown <= 0) {
            alertNearbyParasites(target);
            signalCooldown = SIGNAL_COOLDOWN_TICKS;
        }
        if (--rageCooldown <= 0) {
            for (PrimitiveParasiteEntity ally : level().getEntitiesOfClass(PrimitiveParasiteEntity.class,
                    getBoundingBox().inflate(1.5), ally -> ally != this && ally.isAlive())) {
                ally.addEffect(new MobEffectInstance(ModMobEffects.RAGE, RAGE_DURATION_TICKS, 0), this);
            }
            rageCooldown = RAGE_COOLDOWN_TICKS;
        }
    }

    private void alertNearbyParasites(LivingEntity target) {
        for (PrimitiveParasiteEntity ally : level().getEntitiesOfClass(PrimitiveParasiteEntity.class,
                getBoundingBox().inflate(32.0), ally -> ally != this && ally.isAlive())) {
            if (ally.getTarget() == null && ally.isValidParasiteTarget(target)) ally.setTarget(target);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(ANIMATION)));
    }
}
