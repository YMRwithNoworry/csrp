package alku.csrp.infection;

import alku.csrp.block.InfestedBlock;
import alku.csrp.config.BlockConversionsConfig;
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
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !InfestationSpreadLimiter.canSpread(level, InfestationSpreadLimiter.Type.BIOME)) {
            return 0;
        }
        Direction first = Direction.getRandom(random);
        int converted = convertUnchecked(level, origin.relative(first), stage) ? 1 : 0;
        if (converted == 0 && random.nextInt(3) == 0) {
            converted = convertUnchecked(level, origin.relative(Direction.getRandom(random)), stage) ? 1 : 0;
        }
        if (converted > 0) {
            InfestationSpreadLimiter.record(level, InfestationSpreadLimiter.Type.BIOME, converted);
            EvolutionSystem.addPoints(level, EvolutionSystem.VALUE_BLOCK * converted,
                    EvolutionSystem.PointSource.BLOCK_CONVERSION);
        }
        return converted;
    }

    public static int infestAround(ServerLevel level, BlockPos origin, int stage) {
        return infestAround(level, origin, stage, InfestationSpreadLimiter.Type.BIOME);
    }

    public static int infestAround(ServerLevel level, BlockPos origin, int stage,
            InfestationSpreadLimiter.Type type) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !InfestationSpreadLimiter.canSpread(level, type)) {
            return 0;
        }
        int converted = 0;
        for (Direction direction : Direction.values()) {
            if (convertUnchecked(level, origin.relative(direction), stage)) {
                converted++;
            }
        }
        if (converted > 0) {
            InfestationSpreadLimiter.record(level, type, converted);
            EvolutionSystem.addPoints(level, EvolutionSystem.VALUE_BLOCK * converted,
                    EvolutionSystem.PointSource.BLOCK_CONVERSION);
        }
        return converted;
    }

    public static boolean convert(ServerLevel level, BlockPos pos, int stage) {
        if (!InfestationSpreadLimiter.canSpread(level, InfestationSpreadLimiter.Type.BIOME)) {
            return false;
        }
        boolean converted = convertUnchecked(level, pos, stage);
        if (converted) {
            InfestationSpreadLimiter.record(level, InfestationSpreadLimiter.Type.BIOME, 1);
        }
        return converted;
    }

    private static boolean convertUnchecked(ServerLevel level, BlockPos pos, int stage) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() instanceof InfestedBlock) {
            int currentStage = current.getValue(InfestedBlock.STAGE);
            if (currentStage < stage && level.setBlock(pos,
                    current.setValue(InfestedBlock.STAGE, Math.min(3, stage)), Block.UPDATE_ALL)) {
                InfestationFlora.tryGrow(level, pos, level.random);
                return true;
            }
            return false;
        }
        Block target = BlockConversionsConfig.customTarget(current.getBlock());
        if (target == null && BlockConversionsConfig.useDefaultConversions()) {
            target = convertedBlock(current);
        }
        if (target == null || current.hasBlockEntity() || current.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (level.setBlock(pos, target.defaultBlockState().setValue(InfestedBlock.STAGE,
                Math.max(0, Math.min(3, stage))), Block.UPDATE_ALL)) {
            InfestationFlora.tryGrow(level, pos, level.random);
            return true;
        }
        return false;
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
