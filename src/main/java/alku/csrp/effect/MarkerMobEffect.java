package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** A legacy SRP status marker whose behavior is consumed by entities or events. */
public class MarkerMobEffect extends MobEffect {
    public MarkerMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    public MarkerMobEffect(boolean harmful, int color) {
        this(harmful ? MobEffectCategory.HARMFUL : MobEffectCategory.BENEFICIAL, color);
    }
}
