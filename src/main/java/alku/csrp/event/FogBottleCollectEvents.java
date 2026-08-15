package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Collects the fog block with a vanilla glass bottle, matching SRP 1.10.8. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class FogBottleCollectEvents {
    private static final double DEFAULT_REACH = 5.0D;

    private FogBottleCollectEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (tryCollect(event.getLevel(), event.getEntity(), event.getHand(), event.getPos())) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (tryCollect(event.getLevel(), event.getEntity(), event.getHand(), findFogInLook(event.getEntity()))) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
        }
    }

    private static boolean tryCollect(Level level, Player player, InteractionHand hand, BlockPos hintedPos) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.GLASS_BOTTLE)) {
            return false;
        }
        BlockPos fogPos = hintedPos;
        if (fogPos == null || !level.getBlockState(fogPos).is(ModBlocks.FOG.get())) {
            fogPos = findFogInLook(player);
        }
        if (fogPos == null) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        ItemStack filled = new ItemStack(ModItems.FOG_BOTTLE.get());
        if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
        level.playSound(null, fogPos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(fogPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        return true;
    }

    private static BlockPos findFogInLook(Player player) {
        Level level = player.level();
        Vec3 eyes = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        BlockPos previous = null;
        for (double distance = 0.0D; distance <= DEFAULT_REACH; distance += 0.15D) {
            BlockPos pos = BlockPos.containing(eyes.add(look.scale(distance)));
            if (pos.equals(previous)) {
                continue;
            }
            previous = pos;
            if (level.getBlockState(pos).is(ModBlocks.FOG.get())) {
                return pos;
            }
        }
        return null;
    }
}
