package alku.csrp.block;

import alku.csrp.Config;
import alku.csrp.infection.BlockInfestation;
import alku.csrp.registry.ModBlocks;
import alku.csrp.world.ReinforcementSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Staged SRP infestation block that propagates through random ticks. */
public final class InfestedBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    public InfestedBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    /**
     * Original {@code BlockInfestedStain/BlockInfestedRubble/BlockFallingInfestedStain#randomDisplayTick}:
     * a stage-2 block puffs a spore, a stage-3 block puffs fog and a spore, both only while the block
     * is open to the sky.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockState above = level.getBlockState(pos.above());
        if (!above.isAir() && !above.is(ModBlocks.legacyBlock("parasitebush").get())) {
            return;
        }
        int stage = state.getValue(STAGE);
        if (stage == 2) {
            if (random.nextDouble() <= Config.blockParticleSporeChance()) {
                SrpBlockParticles.spawnSpore(level, pos, random);
            }
        } else if (stage == 3) {
            if (random.nextDouble() <= Config.blockParticleFogChance()) {
                SrpBlockParticles.spawnFog(level, pos, random);
            }
            if (random.nextDouble() <= Config.blockParticleSporeChance()) {
                SrpBlockParticles.spawnSpore(level, pos, random);
            }
        }
    }
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockInfestation.spread(level, pos, state.getValue(STAGE), random);
        ReinforcementSystem.tryFromInfestedBlock(level, pos, random);
    }
}
