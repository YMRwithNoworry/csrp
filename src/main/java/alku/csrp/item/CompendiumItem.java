package alku.csrp.item;

import alku.csrp.compendium.client.CompendiumClient;
import alku.csrp.compendium.network.CompendiumRequestPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class CompendiumItem extends Item {
    public CompendiumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                CompendiumClient.toggleSounds();
            } else {
                ClientPacketDistributor.sendToServer(new CompendiumRequestPayload());
            }
        }
        return level.isClientSide()
                ? InteractionResult.SUCCESS.heldItemTransformedTo(stack)
                : InteractionResult.CONSUME.heldItemTransformedTo(stack);
    }
}
