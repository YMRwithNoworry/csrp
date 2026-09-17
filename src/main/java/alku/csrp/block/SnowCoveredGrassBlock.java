package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of SRParasites 1.10.9 {@code BlockSnowCoveredGrass}: the "snowy" surface block that snow
 * grass sits on.  It is permanently {@code snowy = true} and falls back to a vanilla grass block as
 * soon as no snow grass remains above it.
 *
 * <p>The original overrode {@code getPickBlock} to hand back a grass block.  The modern equivalent
 * is {@link #getCloneItemStack}, which makes the pick-block preview and middle-click result read as
 * grass, while the loot table drops dirt to match the original {@code getItemDropped}.</p>
 */
public class SnowCoveredGrassBlock extends GrassBlock {
    public SnowCoveredGrassBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SNOWY, true));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BlockState updated = super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos,
                neighbourState, random);
        // The original forced snowy = true unconditionally from getActualState.
        return updated.is(this) ? updated.setValue(SNOWY, true) : updated;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Blocks.GRASS_BLOCK);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!hasSnowGrassAbove(level, pos)) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            return;
        }
        super.randomTick(state, level, pos, random);
    }

    static boolean hasSnowGrassAbove(LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(ModBlocks.SNOW_TALL_GRASS.get()) || above.is(ModBlocks.SNOW_SHORT_GRASS.get());
    }
}
