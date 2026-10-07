package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.block.ParasiteRubbleBlock;
import alku.csrp.block.ParasiteStainBlock;
import alku.csrp.fluid.DeadBloodFluid;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Port of {@code SRPEventHandlerBus#onNeighborNotify} and {@code #tryConvertSponge}
 * (out109 lines 465-605): when a block changes next to Dead Blood, the fluid converts the
 * surrounding water, lava and sponges.
 *
 * <p>Original behaviour, reproduced verbatim:</p>
 * <ul>
 *   <li>changed block is water and touches Dead Blood -&gt; the water becomes {@code parasitestain}
 *       variant 1 ({@code MUD});</li>
 *   <li>changed block is lava and touches Dead Blood -&gt; the lava becomes {@code parasiterubble}
 *       variant 7 ({@code OBSIDIAN});</li>
 *   <li>changed block is a sponge touching Dead Blood -&gt; it becomes a Diseased Sponge;</li>
 *   <li>changed block is Dead Blood -&gt; every adjacent water/lava/sponge is converted the same way.</li>
 * </ul>
 *
 * <p>The original guarded the whole handler with a static {@code srpSoakGuard} flag so the recursive
 * block updates it triggers cannot re-enter; the same guard is kept here.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DeadBloodConversionEvents {
    /** Original {@code SRPEventHandlerBus.srpSoakGuard}. */
    private static boolean soakGuard;

    private DeadBloodConversionEvents() {
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || soakGuard) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState changedState = event.getState();
        boolean sponge = changedState.is(Blocks.SPONGE);
        boolean water = changedState.getFluidState().is(FluidTags.WATER);
        boolean lava = changedState.getFluidState().is(FluidTags.LAVA);
        boolean deadBlood = isDeadBlood(level, pos);
        if (!sponge && !deadBlood && !water && !lava) {
            return;
        }

        soakGuard = true;
        try {
            if (!deadBlood) {
                if (!water && !lava) {
                    // Sponge: absorb an adjacent Dead Blood block.
                    if (sponge && touchesDeadBlood(level, pos)) {
                        convertSponge(level, pos, changedState);
                    }
                    return;
                }
                if (!touchesDeadBlood(level, pos)) {
                    return;
                }
                level.setBlock(pos, water ? stainState() : rubbleState(), 2);
                return;
            }

            // Dead Blood itself changed: convert every neighbour.
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = pos.relative(direction);
                BlockState state = level.getBlockState(neighbour);
                if (state.is(Blocks.SPONGE)) {
                    convertSponge(level, neighbour, state);
                } else if (state.getFluidState().is(FluidTags.WATER)) {
                    level.setBlock(neighbour, stainState(), 2);
                } else if (state.getFluidState().is(FluidTags.LAVA)) {
                    level.setBlock(neighbour, rubbleState(), 2);
                }
            }
        } finally {
            soakGuard = false;
        }
    }

    /** Original {@code tryConvertSponge}. */
    private static void convertSponge(ServerLevel level, BlockPos spongePos, BlockState spongeState) {
        if (soakGuard || !spongeState.is(Blocks.SPONGE) || !touchesDeadBlood(level, spongePos)) {
            return;
        }
        soakGuard = true;
        try {
            level.setBlock(spongePos, ModBlocks.DISEASED_SPONGE.get().defaultBlockState(), 2);
        } finally {
            soakGuard = false;
        }
    }

    private static boolean touchesDeadBlood(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (isDeadBlood(level, pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    /** The original compared against the Dead Blood {@code Block}; in 26.3 it is a fluid. */
    private static boolean isDeadBlood(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).getType() instanceof DeadBloodFluid;
    }

    /** Original {@code getParasiteStainState}: {@code parasitestain} with metadata 1 ({@code MUD}). */
    private static BlockState stainState() {
        BlockState state = ModBlocks.legacyBlock("parasitestain").get().defaultBlockState();
        return state.hasProperty(ParasiteStainBlock.VARIANT)
                ? state.setValue(ParasiteStainBlock.VARIANT, ParasiteStainBlock.Variant.MUD) : state;
    }

    /** Original {@code getParasiteRubbleState}: {@code parasiterubble} with metadata 7 ({@code OBSIDIAN}). */
    private static BlockState rubbleState() {
        BlockState state = ModBlocks.legacyBlock("parasiterubble").get().defaultBlockState();
        return state.hasProperty(ParasiteRubbleBlock.VARIANT)
                ? state.setValue(ParasiteRubbleBlock.VARIANT, ParasiteRubbleBlock.Variant.OBSIDIAN) : state;
    }
}
