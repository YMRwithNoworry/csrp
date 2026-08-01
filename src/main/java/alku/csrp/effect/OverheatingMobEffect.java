package alku.csrp.effect;

import net.minecraft.world.entity.LivingEntity;

/** Sets the affected entity on fire every second. */
public final class OverheatingMobEffect extends MarkerMobEffect {
    public OverheatingMobEffect() {
        super(true, 16746246);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity.tickCount % 20 == 0) {
            entity.igniteForSeconds(2.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
