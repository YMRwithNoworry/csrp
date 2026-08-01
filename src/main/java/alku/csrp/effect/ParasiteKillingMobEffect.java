package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Stacking marker used by the original version-specific parasite damage rules. */
public final class ParasiteKillingMobEffect extends MobEffect {
    public ParasiteKillingMobEffect(int color) {
        super(MobEffectCategory.NEUTRAL, color);
    }
}
