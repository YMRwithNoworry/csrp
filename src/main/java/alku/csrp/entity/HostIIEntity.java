package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public final class HostIIEntity extends AbstractHostEntity {
    public static final int BURROW_DURATION_TICKS = 120;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");

    public HostIIEntity(EntityType<? extends HostIIEntity> type, Level level) {
        super(type, level, 0.12, 5.0, 5.0, BURROW_DURATION_TICKS, 20, 20);
        xpReward = 35;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createHostAttributes(140.0, 12.0, 18.0, 0.12, 32.0);
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

    private void summonManglers() {
        spawnMinions(ModEntities.MANGLER, ManglerEntity.class, 4);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }
}
