package alku.csrp.item;

import alku.csrp.event.BookOfVengeanceEvents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Port of SRP 1.10.9 {@code item/ItemBookOfVengeance}.
 *
 * <p>Two abilities, both from {@code onItemRightClick}:</p>
 * <ul>
 *   <li>plain use — grapple whatever the player is looking at within {@code RANGE = 32} blocks, with the
 *       vanilla item cooldown set to {@code COOLDOWN_TICKS = 60};</li>
 *   <li>sneak use — the knockback burst, on its own {@code KB_COOLDOWN_TICKS = 80} cooldown which the
 *       original kept in the item's {@code srp_kb_next} tag rather than the vanilla cooldown.</li>
 * </ul>
 *
 * <p>26.3 splits right-click into {@code use} (air or block) and {@code interactLivingEntity} (an entity
 * under the crosshair), so both paths lead to the same two abilities; {@code useOn} is left at its
 * default {@code PASS} so a click on a block falls through to {@code use}, exactly as the original's
 * single right-click handler did.</p>
 */
public final class BookOfVengeanceItem extends Item {
    /** {@code ItemBookOfVengeance.RANGE}. */
    private static final double RANGE = 32.0D;
    /** {@code ItemBookOfVengeance.NBT_KB_NEXT}. */
    private static final String KB_NEXT_KEY = "srp_kb_next";

    public BookOfVengeanceItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
            LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return knockbackBurst(player, stack);
        }
        return grapple(player, stack, target);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return knockbackBurst(player, stack);
        }
        LivingEntity target = findLookTarget(player, RANGE);
        if (target == null) {
            return InteractionResult.PASS;
        }
        return grapple(player, stack, target);
    }

    /** The grapple half: vanilla cooldown, then start the chain on the server. */
    private InteractionResult grapple(Player player, ItemStack stack, LivingEntity target) {
        if (player.getCooldowns().isOnCooldown(stack) || !target.isAlive() || target == player
                || player.isAlliedTo(target)
                || target instanceof Player other && !player.canHarmPlayer(other)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer
                && player.level() instanceof ServerLevel serverLevel) {
            BookOfVengeanceEvents.beginGrapple(serverLevel, serverPlayer, target);
            player.getCooldowns().addCooldown(stack, BookOfVengeanceEvents.GRAPPLE_COOLDOWN_TICKS);
        }
        return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    /** The knockback half: {@code doKnockbackBurst}, gated by the item's own {@code srp_kb_next} tag. */
    private InteractionResult knockbackBurst(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        long now = serverLevel.getGameTime();
        if (isKnockbackOnCooldown(stack, now)) {
            return InteractionResult.FAIL;
        }
        BookOfVengeanceEvents.knockbackBurst(serverLevel, serverPlayer);
        setKnockbackCooldown(stack, now + BookOfVengeanceEvents.KNOCKBACK_COOLDOWN_TICKS);
        return InteractionResult.CONSUME;
    }

    private static boolean isKnockbackOnCooldown(ItemStack stack, long gameTime) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getLongOr(KB_NEXT_KEY, 0L) > gameTime;
    }

    private static void setKnockbackCooldown(ItemStack stack, long nextUseTick) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(KB_NEXT_KEY, nextUseTick));
    }

    /**
     * {@code ItemBookOfVengeance.findLookTargetLiving}: cast the look vector up to {@code range}, stop it
     * at the first block, then pick the closest living entity whose hitbox (grown by 0.3) that ray
     * crosses.  The target must also be visible to the player.
     */
    private static LivingEntity findLookTarget(Player player, double range) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));

        BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            end = blockHit.getLocation();
        }

        AABB searchBox = player.getBoundingBox()
                .expandTowards(look.scale(range))
                .inflate(1.0D);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(player, searchBox,
                candidate -> candidate instanceof LivingEntity && candidate.isPickable())) {
            if (!(entity instanceof LivingEntity living) || living == player || !living.isAlive()) {
                continue;
            }
            var intercept = living.getBoundingBox().inflate(0.3D).clip(eye, end);
            if (intercept.isEmpty()) {
                continue;
            }
            double distance = eye.distanceTo(intercept.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }
        return best != null && !player.hasLineOfSight(best) ? null : best;
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
}
