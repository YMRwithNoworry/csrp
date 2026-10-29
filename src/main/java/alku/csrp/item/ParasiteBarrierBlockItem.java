package alku.csrp.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public final class ParasiteBarrierBlockItem extends BlockItem {
    public ParasiteBarrierBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.csrp.parasite_barrier.creative_only")
                .withStyle(ChatFormatting.RED));
        tooltip.accept(Component.translatable("tooltip.csrp.parasite_barrier.desc1")
                .withStyle(ChatFormatting.WHITE));
        tooltip.accept(Component.translatable("tooltip.csrp.parasite_barrier.desc2")
                .withStyle(ChatFormatting.WHITE));
    }
}
