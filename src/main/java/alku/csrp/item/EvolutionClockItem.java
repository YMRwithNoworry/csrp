package alku.csrp.item;

import alku.csrp.util.NbtData;
import alku.csrp.world.SrpWorldData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class EvolutionClockItem extends Item {
    public static final String PHASE_TAG = "evolution_phase";
    private static final String POINTS_TAG = "evolution_points";
    private static final String COOLDOWN_TAG = "evolution_cooldown";

    public EvolutionClockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            SrpWorldData data = SrpWorldData.get(serverLevel);
            updateClock(stack, serverLevel, data);
            player.sendSystemMessage(Component.translatable("message.csrp.evolution_clock",
                    data.evolutionPhase(), data.generation(), data.evolutionPoints(), data.cooldown(serverLevel)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level instanceof ServerLevel serverLevel && entity.tickCount % 20 == 0) {
            updateClock(stack, serverLevel, SrpWorldData.get(serverLevel));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context,
            List<Component> tooltip, TooltipFlag flag) {
        var tag = NbtData.copyTag(stack);
        tooltip.add(Component.translatable("tooltip.csrp.evolution_clock.phase",
                tag.getInt(PHASE_TAG), tag.getInt(POINTS_TAG)).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.csrp.evolution_clock.cooldown",
                tag.getInt(COOLDOWN_TAG)).withStyle(ChatFormatting.GRAY));
    }

    private static void updateClock(ItemStack stack, ServerLevel level, SrpWorldData data) {
        int phase = data.evolutionPhase();
        int points = data.evolutionPoints();
        int cooldown = data.cooldown(level);
        var current = NbtData.copyTag(stack);
        if (current.getInt(PHASE_TAG) == phase && current.getInt(POINTS_TAG) == points
                && current.getInt(COOLDOWN_TAG) == cooldown) {
            return;
        }
        NbtData.update(stack, tag -> {
            tag.putInt(PHASE_TAG, phase);
            tag.putInt(POINTS_TAG, points);
            tag.putInt(COOLDOWN_TAG, cooldown);
        });
    }
}
