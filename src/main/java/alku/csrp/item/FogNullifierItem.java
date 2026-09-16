package alku.csrp.item;

import alku.csrp.block.FogNullifierBlock;
import alku.csrp.block.entity.FogNullifierBlockEntity;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class FogNullifierItem extends BlockItem {
    public FogNullifierItem(FogNullifierBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        TypedEntityData<BlockEntityType<?>> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        int uses = data != null && data.copyTagWithoutId().contains(FogNullifierBlockEntity.USES_TAG)
                ? data.copyTagWithoutId().getIntOr(FogNullifierBlockEntity.USES_TAG, 0) : FogNullifierBlock.MAX_USES;
        tooltip.accept(Component.translatable("tooltip.csrp.fog_nullifier.uses", uses, FogNullifierBlock.MAX_USES)
                .withStyle(ChatFormatting.GRAY));
    }
}
