package alku.csrp.world;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Reverse mapping for blocks produced by SRP infestation. */
public final class BlockPurification {
    private BlockPurification() {
    }

    public static boolean purify(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        Block replacement = replacement(current);
        if (replacement == null) {
            return false;
        }
        level.setBlock(pos, replacement.defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    private static Block replacement(BlockState state) {
        if (state.is(ModBlocks.INFESTED_STAIN)) return Blocks.DIRT;
        if (state.is(ModBlocks.INFESTED_RUBBLE)) return Blocks.STONE;
        if (state.is(ModBlocks.INFESTED_SAND)) return Blocks.SAND;
        if (state.is(ModBlocks.INFESTED_COBBLESTONE)) return Blocks.COBBLESTONE;
        if (state.is(ModBlocks.INFESTED_TRUNK)) return Blocks.OAK_LOG;
        if (state.is(ModBlocks.INFESTED_PLANKS)) return Blocks.OAK_PLANKS;
        if (state.is(ModBlocks.INFESTED_STONE_BRICKS)) return Blocks.STONE_BRICKS;
        if (state.is(ModBlocks.INFESTED_TERRACOTTA)) return Blocks.TERRACOTTA;
        if (state.is(ModBlocks.POLISHED_INFESTED_STONE)) return Blocks.SMOOTH_STONE;
        if (state.is(ModBlocks.RESIDUE_BRICKS)) return Blocks.STONE_BRICKS;
        if (state.is(ModBlocks.INFESTED_COLUMN)) return Blocks.QUARTZ_PILLAR;
        if (state.is(ModBlocks.INFESTED_SANDSTONE)) return Blocks.SANDSTONE;
        if (state.is(ModBlocks.CHISELED_INFESTED_SANDSTONE)) return Blocks.CHISELED_SANDSTONE;
        if (state.is(ModBlocks.CUT_INFESTED_SANDSTONE)) return Blocks.CUT_SANDSTONE;
        if (state.is(ModBlocks.INFESTED_COBBLESTONE_SLAB)) return Blocks.COBBLESTONE_SLAB;
        if (state.is(ModBlocks.INFESTED_STONE_SLAB)) return Blocks.STONE_SLAB;
        if (state.is(ModBlocks.INFESTED_DIRT_SLAB)) return Blocks.DIRT;
        if (state.is(ModBlocks.INFESTED_STONE_BRICK_SLAB)) return Blocks.STONE_BRICK_SLAB;
        if (state.is(ModBlocks.INFESTED_TERRACOTTA_SLAB)) return Blocks.TERRACOTTA;
        if (state.is(ModBlocks.POLISHED_INFESTED_STONE_SLAB)) return Blocks.SMOOTH_STONE_SLAB;
        if (state.is(ModBlocks.RESIDUE_BRICK_SLAB)) return Blocks.STONE_BRICK_SLAB;
        if (state.is(ModBlocks.INFESTED_SANDSTONE_SLAB)) return Blocks.SANDSTONE_SLAB;
        if (state.is(ModBlocks.INFESTED_PLANK_SLAB)) return Blocks.OAK_SLAB;
        if (state.is(ModBlocks.INFESTED_SANDSTONE_STAIRS)) return Blocks.SANDSTONE_STAIRS;
        if (state.is(ModBlocks.RESIDUE_STAIRS)) return Blocks.STONE_BRICK_STAIRS;
        if (state.is(ModBlocks.INFESTED_PLANKS_STAIRS)) return Blocks.OAK_STAIRS;
        if (state.is(ModBlocks.INFESTED_STONE_BRICKS_STAIRS)) return Blocks.STONE_BRICK_STAIRS;
        if (state.is(ModBlocks.INFESTED_POLISHED_STONE_BRICKS_STAIRS)) return Blocks.SMOOTH_QUARTZ_STAIRS;
        if (state.is(ModBlocks.INFESTED_STONE_STAIRS)) return Blocks.STONE_STAIRS;
        if (state.is(ModBlocks.RESIDUE_WALL)) return Blocks.STONE_BRICK_WALL;
        if (state.is(ModBlocks.INFESTED_PLANK_WALL)) return Blocks.OAK_FENCE;
        if (state.is(ModBlocks.POLISHED_INFESTED_STONE_WALL)) return Blocks.COBBLESTONE_WALL;
        if (state.is(ModBlocks.INFESTED_STONE_BRICK_WALL)) return Blocks.STONE_BRICK_WALL;
        if (state.is(ModBlocks.INFESTED_SANDSTONE_WALL)) return Blocks.SANDSTONE_WALL;
        if (state.is(ModBlocks.INFESTED_RUBBLE_WALL)) return Blocks.COBBLESTONE_WALL;
        if (state.is(ModBlocks.INFESTED_STAIN_WALL)) return Blocks.COBBLESTONE_WALL;
        if (state.is(ModBlocks.INFESTED_REMAINS)) return Blocks.AIR;
        return null;
    }
}
