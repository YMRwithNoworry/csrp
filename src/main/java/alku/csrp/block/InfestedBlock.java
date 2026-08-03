package alku.csrp.block;

import alku.csrp.infection.BlockInfestation;
import alku.csrp.world.ReinforcementSystem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Staged SRP infestation block that propagates through random ticks. */
public final class InfestedBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final MapCodec<InfestedBlock> CODEC = simpleCodec(InfestedBlock::new);

    public InfestedBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockInfestation.spread(level, pos, state.getValue(STAGE), random);
        ReinforcementSystem.tryFromInfestedBlock(level, pos, random);
    }
}
