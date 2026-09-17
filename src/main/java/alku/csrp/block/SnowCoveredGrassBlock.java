package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * SRParasites 1.10.9 {@code BlockSnowCoveredGrass} — a grass block that is permanently snow-covered
 * and reverts to vanilla grass once the snow grass above it is gone.
 *
 * <p>Reference: {@code _srp-orig/decomp-1.10.9/.../block/BlockSnowCoveredGrass.java} —
 * {@code extends BlockGrass}, hardness {@code 0.6F}, {@code getActualState} forces
 * {@code SNOWY = true}, drops dirt, pick-block gives vanilla grass, reverts on neighbour change and
 * during random ticks when no snow grass sits above.
 *
 * <p>1.20.1 mapping notes (verified, see PLAN.md §4.3 / §10.1):
 * <ul>
 *   <li>{@code GrassBlock extends SpreadingSnowyDirtBlock extends SnowyDirtBlock}; the inherited
 *       {@code SnowyDirtBlock.SNOWY} ({@code BooleanProperty}) is usable directly and the
 *       {@code snowy} state is already part of {@code GrassBlock}'s state definition — this class
 *       must <b>not</b> re-add it (PLAN.md T25).</li>
 *   <li>1.10.9 {@code updateTick} maps to {@code Properties.randomTicks()} (set by
 *       {@code ModBlocks}) + {@code randomTick(BlockState, ServerLevel, BlockPos, RandomSource)}.</li>
 * </ul>
 */
public class SnowCoveredGrassBlock extends GrassBlock {
    public SnowCoveredGrassBlock(Properties properties) {
        super(properties);
    }

    /** 1.10.9 {@code getActualState} equivalent: the block is always snowy. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.SNOWY, true);
    }

    /** 1.10.9 {@code updateTick}: revert to vanilla grass when no snow grass is above. */
    @Override
    public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
            net.minecraft.util.RandomSource random) {
        if (!hasSnowGrassAbove(level, pos)) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            return;
        }
        super.randomTick(state, level, pos, random);
    }

    /** 1.10.9 {@code neighborChanged}: same revert-on-loss rule. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        if (!level.isClientSide && !hasSnowGrassAbove(level, pos)) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }
    }

    /** 1.10.9 {@code hasSnowGrassAbove}. */
    private static boolean hasSnowGrassAbove(BlockGetter level, BlockPos pos) {
        Block above = level.getBlockState(pos.above()).getBlock();
        return above == ModBlocks.SNOW_TALL_GRASS.get() || above == ModBlocks.SNOW_SHORT_GRASS.get();
    }

    /**
     * 1.10.9 {@code getItemDropped} returned {@code Item.getItemFromBlock(Blocks.DIRT)}
     * ({@code getDamageValue} = 0) and {@code canSilkHarvest} = true, i.e. silk touch makes no
     * difference — dirt is always the drop.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Items.DIRT));
    }

    /** 1.10.9 {@code getPickBlock} → vanilla grass block. */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.GRASS_BLOCK);
    }
}
