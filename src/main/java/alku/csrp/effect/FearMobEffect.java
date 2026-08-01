package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker used by feral parasites to expose the legacy fear status. */
public final class FearMobEffect extends MobEffect {
    public FearMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x111114);
    }
}
