package alku.csrp.item;

import alku.csrp.world.SrpWorldData;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class EvolutionDeviceItem extends Item {
    public EvolutionDeviceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level instanceof ServerLevel serverLevel && user instanceof Player player) {
            SrpWorldData data = SrpWorldData.get(serverLevel);
            if (data.nodes().isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.csrp.overlast.evo_device.empty"));
            } else if ((data.evolutionPhase() == 7 && level.random.nextBoolean())
                    || (data.evolutionPhase() >= 8 && level.random.nextInt(10) >= 2)) {
                player.sendSystemMessage(Component.translatable("message.csrp.overlast.evo_device.jammed"));
            } else {
                String nodes = data.nodes().stream()
                        .map(node -> "[" + node.pos().getX() + ", " + node.pos().getY() + ", "
                                + node.pos().getZ() + ", " + node.age() + "]")
                        .collect(Collectors.joining(" "));
                player.sendSystemMessage(Component.translatable("message.csrp.overlast.evo_device.found", nodes));
            }
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 20;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
