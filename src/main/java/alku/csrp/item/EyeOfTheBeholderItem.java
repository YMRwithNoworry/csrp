package alku.csrp.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Eye of the Beholder: glows near Enderman variants and is lost when its
 * wielder is slain by one of them.
 */
public final class EyeOfTheBeholderItem extends Item {
    public EyeOfTheBeholderItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp.pearl.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.csrp.pearl.assimilated")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.csrp.pearl.feral")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.csrp.pearl.assimara")
                .withStyle(ChatFormatting.BLUE));
    }
}
