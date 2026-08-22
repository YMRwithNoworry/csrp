package alku.csrp.item;

import alku.csrp.network.CsrpNetwork;
import alku.csrp.compendium.client.CompendiumClient;
import alku.csrp.compendium.network.CompendiumRequestPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CompendiumItem extends Item {
    public CompendiumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            if (player.isShiftKeyDown()) {
                CompendiumClient.toggleSounds();
            } else {
                CsrpNetwork.sendToServer(new CompendiumRequestPayload());
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
