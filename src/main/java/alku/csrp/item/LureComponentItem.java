package alku.csrp.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Legacy SRP lure component.  The 1.10 item was intentionally lightweight:
 * using it poked the global lure/update counter and its main user-facing
 * behaviour was the coloured version tooltip.
 */
public final class LureComponentItem extends Item {
    private final int version;

    public LureComponentItem(int version, Properties properties) {
        super(properties.stacksTo(16));
        this.version = version;
    }

    public int version() {
        return version;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        // The original ItemLure did not consume the component and had no
        // visible world-side effect beyond incrementing SRP's update number.
        // Keep the item non-consuming while preserving the interaction hook.
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tootip.srparasites.lurecomp." + version)
                .withStyle(ChatFormatting.AQUA));
    }
}
