package alku.csrp.item;

import alku.csrp.world.SrpWorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class EvolutionClockItem extends Item {
    public EvolutionClockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            SrpWorldData data = SrpWorldData.get(serverLevel);
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putInt("evolution_phase", data.evolutionPhase()));
            player.sendSystemMessage(Component.translatable("message.csrp.evolution_clock",
                    data.evolutionPhase(), data.evolutionPoints(), data.cooldown(serverLevel)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
