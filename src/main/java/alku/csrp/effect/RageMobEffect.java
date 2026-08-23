package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class RageMobEffect extends MobEffect {
    public RageMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xB51F1F);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                "8e8a8b5f-0d5e-4f44-a27e-12a6717a1d01", 0.1D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                "8e8a8b5f-0d5e-4f44-a27e-12a6717a1d02", 0.1D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
