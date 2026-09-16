package alku.csrp.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Venkrol boots identify the wearer as anchored against Beckon vortex pull. */
public final class VenkrolBootsItem extends Item {
    public VenkrolBootsItem(ArmorMaterial material, ArmorType type, Item.Properties properties) {
        super(properties.humanoidArmor(material, type));
    }

    public VenkrolBootsItem(Holder<ArmorMaterial> material, ArmorType type, Item.Properties properties) {
        this(material.value(), type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.csrp.venkrol_boots")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
