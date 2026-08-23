package alku.csrp.effect;

import java.util.ArrayList;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Penalizes each beneficial effect with periodic magic damage. */
public final class EffectPosMobEffect extends MarkerMobEffect {
    public EffectPosMobEffect() {
        super(true, 12095688);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity.tickCount % 20 == 0) {
            for (MobEffectInstance active : new ArrayList<>(entity.getActiveEffects())) {
                if (active.getEffect().getCategory() != MobEffectCategory.HARMFUL) {
                    entity.hurt(entity.damageSources().magic(), 0.5F * (active.getAmplifier() + 1)
                            * (amplifier + 1));
                }
            }
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
