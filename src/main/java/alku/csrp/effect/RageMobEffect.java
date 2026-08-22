package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import alku.csrp.Csrp;

public final class RageMobEffect extends MobEffect {
    public RageMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xB51F1F);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                new ResourceLocation(Csrp.MODID, "rage_speed"), 0.1D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                new ResourceLocation(Csrp.MODID, "rage_damage"), 0.1D,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
