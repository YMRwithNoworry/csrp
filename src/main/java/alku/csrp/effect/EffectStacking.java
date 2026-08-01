package alku.csrp.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Legacy SRP potion stacking rules used by contamination and effect-neg. */
public final class EffectStacking {
    private EffectStacking() {
    }

    public static void apply(LivingEntity entity, Holder<MobEffect> effect, int duration, int amplifier) {
        if (entity.level().isClientSide || amplifier < -255 || amplifier > 254) {
            return;
        }
        MobEffectInstance current = entity.getEffect(effect);
        if (current == null) {
            entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
            return;
        }
        int newDuration = current.getDuration() + 40 <= duration
                ? duration : current.getDuration() + 10;
        int newAmplifier;
        if (current.getAmplifier() < amplifier) {
            newAmplifier = amplifier;
        } else {
            newAmplifier = current.getAmplifier() + 1;
        }
        entity.addEffect(new MobEffectInstance(effect, newDuration, newAmplifier, false, false));
    }
}
