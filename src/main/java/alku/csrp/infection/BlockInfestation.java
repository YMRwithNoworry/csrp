package alku.csrp.infection;

import alku.csrp.block.InfestedBlock;
import alku.csrp.registry.ModBlocks;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Material-aware conversion used by spreading blocks and Beckon nexuses. */
public final class BlockInfestation {
    private BlockInfestation() {
    }

    public static int spread(ServerLevel level, BlockPos origin, int stage, RandomSource random) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return 0;
        }
        Direction first = Direction.getRandom(random);
        int converted = convert(level, origin.relative(first), stage) ? 1 : 0;
        if (converted == 0 && random.nextInt(3) == 0) {
            converted = convert(level, origin.relative(Direction.getRandom(random)), stage) ? 1 : 0;
        }
        if (converted > 0) {
            EvolutionSystem.addPoints(level, EvolutionSystem.VALUE_BLOCK * converted,
                    EvolutionSystem.PointSource.BLOCK_CONVERSION);
        }
        return converted;
    }

    public static int infestAround(ServerLevel level, BlockPos origin, int stage) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return 0;
        }
        int converted = 0;
        for (Direction direction : Direction.values()) {
            if (convert(level, origin.relative(direction), stage)) {
                converted++;
            }
        }
        if (converted > 0) {
            EvolutionSystem.addPoints(level, EvolutionSystem.VALUE_BLOCK * converted,
                    EvolutionSystem.PointSource.BLOCK_CONVERSION);
        }
        return converted;
    }

    public static boolean convert(ServerLevel level, BlockPos pos, int stage) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() instanceof InfestedBlock) {
            int currentStage = current.getValue(InfestedBlock.STAGE);
            return currentStage < stage && level.setBlock(pos,
                    current.setValue(InfestedBlock.STAGE, Math.min(3, stage)), Block.UPDATE_ALL);
        }
        Block target = convertedBlock(current);
        if (target == null || current.hasBlockEntity() || current.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        return level.setBlock(pos, target.defaultBlockState().setValue(InfestedBlock.STAGE,
                Math.max(0, Math.min(3, stage))), Block.UPDATE_ALL);
    }

    private static Block convertedBlock(BlockState state) {
        if (state.getBlock() instanceof InfestedBlock || state.isAir() || state.liquid()) {
            return null;
        }
        if (state.is(BlockTags.LOGS)) return ModBlocks.INFESTED_TRUNK.get();
        if (state.is(BlockTags.PLANKS)) return ModBlocks.INFESTED_PLANKS.get();
        if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.MOSSY_COBBLESTONE)) {
            return ModBlocks.INFESTED_COBBLESTONE.get();
        }
        if (state.is(BlockTags.SAND)) return ModBlocks.INFESTED_SAND.get();
        if (state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)) {
            return ModBlocks.INFESTED_STAIN.get();
        }
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.STONE_BRICKS)
                || state.is(BlockTags.TERRACOTTA)) {
            return ModBlocks.INFESTED_RUBBLE.get();
        }
        return null;
    }
}
