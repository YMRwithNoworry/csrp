package alku.csrp.entity;

import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public class IncompleteFormSmallEntity extends CrudeParasiteEntity {
    private final RawAnimation AGE_ANIMATION = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");

    public IncompleteFormSmallEntity(EntityType<? extends IncompleteFormSmallEntity> type, Level level) {
        super(type, level);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 9.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isLegacyCrudeTarget));
    }

    protected boolean isLegacyCrudeTarget(LivingEntity target) {
        return isValidParasiteTarget(target) && !(target instanceof WaterAnimal)
                && !(target instanceof Animal) && !(target instanceof Villager);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.INCOMPLETE_SMALL_LIVING.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.INCOMPLETE_SMALL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.INCOMPLETE_SMALL_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.LITE_FLESH_SLIDE.get(), 0.3F, 1.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 4,
                state -> state.setAndContinue(AGE_ANIMATION)));
    }
}
