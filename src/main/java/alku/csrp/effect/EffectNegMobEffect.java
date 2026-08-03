package alku.csrp.effect;

import java.util.ArrayList;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Stacks the amplifier onto every harmful effect once per second. */
public final class EffectNegMobEffect extends MarkerMobEffect {
    public EffectNegMobEffect() {
        super(true, 7318708);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity.tickCount % 20 == 0) {
            for (MobEffectInstance active : new ArrayList<>(entity.getActiveEffects())) {
                if (active.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    if (active.getEffect().is(alku.csrp.registry.ModMobEffects.EFFECTNEG)) {
                        continue;
                    }
                    EffectStacking.apply(entity, active.getEffect(), 20, amplifier);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
