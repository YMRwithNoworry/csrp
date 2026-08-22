package alku.csrp.entity;

import net.minecraftforge.common.ForgeMod;
import alku.csrp.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import software.bernie.geckolib.core.animation.AnimatableManager;

/** Legacy Worm Carrier (EntityQuac), including its four linked body segments. */
public final class CarrierWormEntity extends BurrowingVariantEntity {
    private static final int BODY_SEGMENTS = 4;

    public CarrierWormEntity(EntityType<? extends CarrierWormEntity> type, Level level) {
        super(type, level);
        setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 77.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(ForgeMod.STEP_HEIGHT_ADDITION.get(), 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, createBurrowMovementGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3D, false));
    }

    @Override
    protected boolean supportsBurrowing() {
        return true;
    }

    @Override
    protected int burrowSkillCooldownTicks() {
        return 120;
    }

    @Override
    protected int bodySegmentCount() {
        return BODY_SEGMENTS;
    }

    @Override
    protected double bodyFollowDistance() {
        return 1.9D;
    }

    @Override
    protected SoundEvent burrowSound() {
        return ModSounds.get("quac.dig");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // SRP 1.10.7 registers EntityQuac without a renderer.
    }
}
