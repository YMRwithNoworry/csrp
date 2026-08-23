package alku.csrp.effect;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Rooter-linked defense marker with the original attack-speed benefit. */
public final class PivotMobEffect extends MarkerMobEffect {
    public PivotMobEffect() {
        super(false, 16757187);
        addAttributeModifier(Attributes.ATTACK_SPEED,
                "8e8a8b5f-0d5e-4f44-a27e-12a6717a1d03",
                0.1D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
