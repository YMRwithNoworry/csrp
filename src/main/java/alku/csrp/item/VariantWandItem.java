package alku.csrp.item;
import net.minecraft.world.level.Level;

import alku.csrp.entity.ManualVariantProvider;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Creative tool that advances parasite variants in the same order as SRP 1.10.8. */
public final class VariantWandItem extends Item {
    public VariantWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
            InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(target instanceof ManualVariantProvider variants)) {
            return InteractionResult.PASS;
        }
        if (!target.level().isClientSide) {
            variants.cycleManualVariant();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp.itemvariant",
                Component.translatable("tooltip.csrp.itemvariant.action").withStyle(ChatFormatting.RED)));
    }
}
