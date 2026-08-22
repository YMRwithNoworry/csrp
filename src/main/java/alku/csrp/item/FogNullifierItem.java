package alku.csrp.item;

import alku.csrp.block.FogNullifierBlock;
import alku.csrp.block.entity.FogNullifierBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

public final class FogNullifierItem extends BlockItem {
    public FogNullifierItem(FogNullifierBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag data = BlockItem.getBlockEntityData(stack);
        int uses = data != null && data.contains(FogNullifierBlockEntity.USES_TAG)
                ? data.getInt(FogNullifierBlockEntity.USES_TAG) : FogNullifierBlock.MAX_USES;
        tooltip.add(Component.translatable("tooltip.csrp.fog_nullifier.uses", uses, FogNullifierBlock.MAX_USES)
                .withStyle(ChatFormatting.GRAY));
    }
}
