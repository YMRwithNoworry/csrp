package alku.csrp.item;
import net.minecraft.world.level.Level;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Venkrol boots identify the wearer as anchored against Beckon vortex pull. */
public final class VenkrolBootsItem extends ArmorItem {
    public VenkrolBootsItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp.venkrol_boots")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
