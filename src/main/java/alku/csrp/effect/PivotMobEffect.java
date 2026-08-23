package alku.csrp.effect;

import alku.csrp.Csrp;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Rooter-linked defense marker with the original attack-speed benefit. */
public final class PivotMobEffect extends MarkerMobEffect {
    public PivotMobEffect() {
        super(false, 16757187);
        addAttributeModifier(Attributes.ATTACK_SPEED,
                "pivot_attack_speed",
                0.1D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
