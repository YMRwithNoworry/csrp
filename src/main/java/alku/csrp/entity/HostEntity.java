package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public final class HostEntity extends AbstractHostEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return true;
    }

    @Override
    protected float damageAdaptationEffectiveness() {
        return 0.95F;
    }
    public static final int BURROW_DURATION_TICKS = 80;
    private static final int HOST_TO_HOSTII_KILLS = 40;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");

    public HostEntity(EntityType<? extends HostEntity> type, Level level) {
        super(type, level, 0.12, 3.0, 4.0, BURROW_DURATION_TICKS, 50, 40);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createHostAttributes(50.0, 7.0, 10.0, 0.12, 24.0);
    }

    @Override
    protected void performRangedAttack(LivingEntity target) {
        performBombAttack(target);
    }

    private void performBombAttack(LivingEntity target) {
        spawnProjectile(ParasiteProjectileEntity.Mode.BOMB, target, 0.75, 12.0F, 4.0, 80);
    }

    @Override
    protected void performShockwave() {
        super.performShockwave();
    }

    @Override
    protected void summonMinions() {
        summonRupters();
    }

    private void summonRupters() {
        spawnMinions(ModEntities.RUPTER, RupterEntity.class, 4);
    }

    @Override
    protected void onParasiteKill(ServerLevel level, LivingEntity victim, int kills) {
        if (kills <= HOST_TO_HOSTII_KILLS) {
            return;
        }
        HostIIEntity hostII = ModEntities.HOSTII.get().create(level);
        if (hostII == null) {
            return;
        }
        hostII.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        level.addFreshEntity(hostII);
        discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }
}
