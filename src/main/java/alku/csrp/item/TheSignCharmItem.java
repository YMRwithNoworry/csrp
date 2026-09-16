package alku.csrp.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class TheSignCharmItem extends Item {
    public TheSignCharmItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.csrp.the_sign_charm.red").withStyle(ChatFormatting.RED));
        builder.accept(Component.translatable("tooltip.csrp.the_sign_charm.white").withStyle(ChatFormatting.WHITE));
        builder.accept(Component.translatable("tooltip.csrp.the_sign_charm.gray")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
