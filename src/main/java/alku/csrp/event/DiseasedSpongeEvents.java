package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.fluid.DeadBloodFluid;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.level.BlockEvent;

/**
 * Placing a sponge in Dead Blood absorbs the liquid and turns it into a
 * Diseased Sponge.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DiseasedSpongeEvents {
    private DiseasedSpongeEvents() {
    }

    @SubscribeEvent
    public static void onSpongePlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !event.getPlacedBlock().is(Blocks.SPONGE)) {
            return;
        }
        BlockPos pos = event.getPos();
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (level.getBlockState(adjacent).getFluidState().getType()
                    instanceof DeadBloodFluid) {
                level.setBlock(adjacent, Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(pos, ModBlocks.DISEASED_SPONGE.get().defaultBlockState(), 3);
                return;
            }
        }
    }

}
