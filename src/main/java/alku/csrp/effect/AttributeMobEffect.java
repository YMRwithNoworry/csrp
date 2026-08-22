package alku.csrp.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.core.Holder;

/** Marker effect with a legacy multiplicative attribute modifier. */
public final class AttributeMobEffect extends MarkerMobEffect {
    public AttributeMobEffect(boolean harmful, int color, Holder<Attribute> attribute,
                              ResourceLocation id, double amount) {
        super(harmful, color);
        addAttributeModifier(attribute, id, amount, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
