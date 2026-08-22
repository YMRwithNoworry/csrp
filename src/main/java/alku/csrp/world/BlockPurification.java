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
        if (state.is(ModBlocks.INFESTED_STAIN.get())) return Blocks.DIRT;
        if (state.is(ModBlocks.INFESTED_RUBBLE.get())) return Blocks.STONE;
        if (state.is(ModBlocks.INFESTED_SAND.get())) return Blocks.SAND;
        if (state.is(ModBlocks.INFESTED_COBBLESTONE.get())) return Blocks.COBBLESTONE;
        if (state.is(ModBlocks.INFESTED_TRUNK.get())) return Blocks.OAK_LOG;
        if (state.is(ModBlocks.INFESTED_PLANKS.get())) return Blocks.OAK_PLANKS;
        if (state.is(ModBlocks.INFESTED_STONE_BRICKS.get())) return Blocks.STONE_BRICKS;
        if (state.is(ModBlocks.INFESTED_TERRACOTTA.get())) return Blocks.TERRACOTTA;
        if (state.is(ModBlocks.POLISHED_INFESTED_STONE.get())) return Blocks.SMOOTH_STONE;
        if (state.is(ModBlocks.RESIDUE_BRICKS.get())) return Blocks.STONE_BRICKS;
        if (state.is(ModBlocks.INFESTED_COLUMN.get())) return Blocks.QUARTZ_PILLAR;
        if (state.is(ModBlocks.INFESTED_SANDSTONE.get())) return Blocks.SANDSTONE;
        if (state.is(ModBlocks.CHISELED_INFESTED_SANDSTONE.get())) return Blocks.CHISELED_SANDSTONE;
        if (state.is(ModBlocks.CUT_INFESTED_SANDSTONE.get())) return Blocks.CUT_SANDSTONE;
        if (state.is(ModBlocks.INFESTED_COBBLESTONE_SLAB.get())) return Blocks.COBBLESTONE_SLAB;
        if (state.is(ModBlocks.INFESTED_STONE_SLAB.get())) return Blocks.STONE_SLAB;
        if (state.is(ModBlocks.INFESTED_DIRT_SLAB.get())) return Blocks.DIRT;
        if (state.is(ModBlocks.INFESTED_STONE_BRICK_SLAB.get())) return Blocks.STONE_BRICK_SLAB;
        if (state.is(ModBlocks.INFESTED_TERRACOTTA_SLAB.get())) return Blocks.TERRACOTTA;
        if (state.is(ModBlocks.POLISHED_INFESTED_STONE_SLAB.get())) return Blocks.SMOOTH_STONE_SLAB;
        if (state.is(ModBlocks.RESIDUE_BRICK_SLAB.get())) return Blocks.STONE_BRICK_SLAB;
        if (state.is(ModBlocks.INFESTED_SANDSTONE_SLAB.get())) return Blocks.SANDSTONE_SLAB;
        if (state.is(ModBlocks.INFESTED_PLANK_SLAB.get())) return Blocks.OAK_SLAB;
        if (state.is(ModBlocks.INFESTED_SANDSTONE_STAIRS.get())) return Blocks.SANDSTONE_STAIRS;
        if (state.is(ModBlocks.RESIDUE_STAIRS.get())) return Blocks.STONE_BRICK_STAIRS;
        if (state.is(ModBlocks.INFESTED_PLANKS_STAIRS.get())) return Blocks.OAK_STAIRS;
        if (state.is(ModBlocks.INFESTED_STONE_BRICKS_STAIRS.get())) return Blocks.STONE_BRICK_STAIRS;
        if (state.is(ModBlocks.INFESTED_POLISHED_STONE_BRICKS_STAIRS.get())) return Blocks.SMOOTH_QUARTZ_STAIRS;
        if (state.is(ModBlocks.INFESTED_STONE_STAIRS.get())) return Blocks.STONE_STAIRS;
        if (state.is(ModBlocks.RESIDUE_WALL.get())) return Blocks.STONE_BRICK_WALL;
        if (state.is(ModBlocks.INFESTED_PLANK_WALL.get())) return Blocks.OAK_FENCE;
        if (state.is(ModBlocks.POLISHED_INFESTED_STONE_WALL.get())) return Blocks.COBBLESTONE_WALL;
        if (state.is(ModBlocks.INFESTED_STONE_BRICK_WALL.get())) return Blocks.STONE_BRICK_WALL;
        if (state.is(ModBlocks.INFESTED_SANDSTONE_WALL.get())) return Blocks.SANDSTONE_WALL;
        if (state.is(ModBlocks.INFESTED_RUBBLE_WALL.get())) return Blocks.COBBLESTONE_WALL;
        if (state.is(ModBlocks.INFESTED_STAIN_WALL.get())) return Blocks.COBBLESTONE_WALL;
        if (state.is(ModBlocks.INFESTED_REMAINS.get())) return Blocks.AIR;
        return null;
    }
}
