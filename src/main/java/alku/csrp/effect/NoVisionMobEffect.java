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
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10,
                    amplifier, false, false, false));
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
