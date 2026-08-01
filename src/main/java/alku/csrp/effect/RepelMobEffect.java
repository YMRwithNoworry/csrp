package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Legacy SRP repel marker; protected hosts reject parasite infection effects. */
public final class RepelMobEffect extends MobEffect {
    public RepelMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x43AA80);
    }
}
