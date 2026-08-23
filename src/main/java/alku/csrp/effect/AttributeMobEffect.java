package alku.csrp.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.core.Holder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Marker effect with a legacy multiplicative attribute modifier. */
public final class AttributeMobEffect extends MarkerMobEffect {
    public AttributeMobEffect(boolean harmful, int color, Attribute attribute,
                              ResourceLocation id, double amount) {
        super(harmful, color);
        String modifierId = UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8)).toString();
        addAttributeModifier(attribute, modifierId, amount, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
