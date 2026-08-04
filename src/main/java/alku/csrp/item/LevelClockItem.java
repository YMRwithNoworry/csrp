package alku.csrp.item;

import alku.csrp.world.EvolutionSystem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Displays the server-wide Ubiquitous Development level from zero to four. */
public final class LevelClockItem extends Item {
    public static final String DEVELOPMENT_TAG = "ubiquitous_development";

    public LevelClockItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level instanceof ServerLevel serverLevel && entity.tickCount % 20 == 0) {
            updateLevel(stack, serverLevel);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            int development = updateLevel(stack, serverLevel);
            player.sendSystemMessage(Component.translatable("message.csrp.level_clock", development));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        int development = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(DEVELOPMENT_TAG);
        tooltip.add(Component.translatable("tooltip.csrp.level_clock", development)
                .withStyle(ChatFormatting.AQUA));
    }

    private static int updateLevel(ItemStack stack, ServerLevel level) {
        int development = EvolutionSystem.ubiquitousDevelopment(level.getServer());
        int current = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(DEVELOPMENT_TAG);
        if (current != development) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putInt(DEVELOPMENT_TAG, development));
        }
        return development;
    }
}
