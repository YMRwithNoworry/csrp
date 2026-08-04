package alku.csrp.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Applies VHS-like visual interference to players and burdens every affected host with mining fatigue. */
public final class NoVisionMobEffect extends MarkerMobEffect {
    public NoVisionMobEffect() {
        super(true, 1582649);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10,
                    amplifier, false, false, false));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
