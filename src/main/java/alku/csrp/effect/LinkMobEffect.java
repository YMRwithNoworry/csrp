package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Legacy SRP Link marker; its level controls global-adaptation sharing chance. */
public final class LinkMobEffect extends MobEffect {
    public LinkMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF6BDB);
    }
}
