package alku.csrp.effect;

import net.minecraft.world.entity.LivingEntity;

/** Sets the affected entity on fire every second. */
public final class OverheatingMobEffect extends MarkerMobEffect {
    public OverheatingMobEffect() {
        super(true, 16746246);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity.tickCount % 20 == 0) {
            entity.setSecondsOnFire(1);;
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
