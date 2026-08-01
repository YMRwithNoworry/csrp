package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Legacy stacked feral resistance marker carried by an attacker. */
public final class FeralMobEffect extends MobEffect {
    public FeralMobEffect() {
        super(MobEffectCategory.NEUTRAL, 0x993030);
    }
}
