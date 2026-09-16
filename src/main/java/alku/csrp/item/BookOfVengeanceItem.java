package alku.csrp.item;

import alku.csrp.event.BookOfVengeanceEvents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Executes the two-stage Ricardo vengeance slam and its alternate pulse attack. */
public final class BookOfVengeanceItem extends Item {
    public BookOfVengeanceItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
            LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return pulse(player, stack);
        }
        if (player.getCooldowns().isOnCooldown(stack) || !target.isAlive()
                || target == player || player.isAlliedTo(target)
                || target instanceof Player other && !player.canHarmPlayer(other)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && player.level() instanceof ServerLevel serverLevel) {
            BookOfVengeanceEvents.beginSlamChain(serverLevel, serverPlayer, target);
            player.getCooldowns().addCooldown(stack, BookOfVengeanceEvents.SLAM_COOLDOWN_TICKS);
        }
        return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        pulse(player, stack);
        return level.isClientSide()
                ? InteractionResult.SUCCESS.heldItemTransformedTo(stack)
                : InteractionResult.CONSUME.heldItemTransformedTo(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()
                || player.getCooldowns().isOnCooldown(context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        return pulse(player, context.getItemInHand());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.csrp.book_of_vengeance.line1")
                .withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("tooltip.csrp.book_of_vengeance.line2")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.accept(Component.translatable("tooltip.csrp.book_of_vengeance.line3")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.csrp.book_of_vengeance.line4")
                .withStyle(ChatFormatting.GRAY));
    }

    private InteractionResult pulse(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer
                && player.level() instanceof ServerLevel serverLevel) {
            BookOfVengeanceEvents.pulse(serverLevel, serverPlayer);
            player.getCooldowns().addCooldown(stack, BookOfVengeanceEvents.PULSE_COOLDOWN_TICKS);
        }
        return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
