package alku.csrp.effect;

import net.minecraft.world.entity.LivingEntity;

/** The legacy distortion effect temporarily disables gravity. */
public final class DistortedEnlightenmentMobEffect extends MarkerMobEffect {
    public DistortedEnlightenmentMobEffect() {
        super(true, 8970751);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.setNoGravity(true);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
