package alku.csrp.item;

import alku.csrp.event.BookOfVengeanceEvents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
        if (player.getCooldowns().isOnCooldown(this) || !target.isAlive()
                || target == player || player.isAlliedTo(target)
                || target instanceof Player other && !player.canHarmPlayer(other)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && player.level() instanceof ServerLevel serverLevel) {
            BookOfVengeanceEvents.beginSlamChain(serverLevel, serverPlayer, target);
            player.getCooldowns().addCooldown(this, BookOfVengeanceEvents.SLAM_COOLDOWN_TICKS);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }
        pulse(player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.PASS;
        }
        return pulse(player, context.getItemInHand());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp.book_of_vengeance.line1")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.csrp.book_of_vengeance.line2")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.csrp.book_of_vengeance.line3")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.csrp.book_of_vengeance.line4")
                .withStyle(ChatFormatting.GRAY));
    }

    private InteractionResult pulse(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer
                && player.level() instanceof ServerLevel serverLevel) {
            BookOfVengeanceEvents.pulse(serverLevel, serverPlayer);
            player.getCooldowns().addCooldown(this, BookOfVengeanceEvents.PULSE_COOLDOWN_TICKS);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
