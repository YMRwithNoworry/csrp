package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
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
    private final RawAnimation BURROW = ParasiteAnimations.loop(this, "get_burrow_timer");
    private final RawAnimation BURROWED = ParasiteAnimations.loop(this, "idle.get_burrowed_1");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "get_attack_timer");
    private final RawAnimation BURROWED_ATTACK =
            ParasiteAnimations.play(this, "get_attack_timer.get_burrowed_1");

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

    @Override
    protected void triggerAttackAnimation() {
        triggerAnim("attack_controller", isBurrowed() ? "burrowed_attack" : "attack");
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
                state -> {
                    if (getBurrowAnimationTicks() > 0) {
                        return state.setAndContinue(BURROW);
                    }
                    if (isBurrowed()) {
                        return state.setAndContinue(BURROWED);
                    }
                    return state.setAndContinue(state.isMoving() ? WALK : IDLE);
                }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0,
                state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("burrowed_attack", BURROWED_ATTACK));
    }
}
