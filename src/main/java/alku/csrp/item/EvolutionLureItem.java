package alku.csrp.item;

import alku.csrp.block.EvolutionLureBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class EvolutionLureItem extends BlockItem {
    private final EvolutionLureBlock.Tier tier;

    public EvolutionLureItem(Block block, EvolutionLureBlock.Tier tier, Properties properties) {
        super(block, properties);
        this.tier = tier;
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(EvolutionLureBlock.TIER, tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.csrp.evolutionlure", tier.cooldownSeconds())
                .withStyle(ChatFormatting.GRAY));
    }
}
