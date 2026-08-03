package alku.csrp.block;

import alku.csrp.infection.BlockInfestation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy infested stairs periodically seed stage-zero infestation. */
public final class InfestedStairBlock extends StairBlock {
    public static final MapCodec<InfestedStairBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("base_state").forGetter(block -> block.baseState),
            propertiesCodec()).apply(instance, InfestedStairBlock::new));

    private final BlockState baseState;

    public InfestedStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties.randomTicks());
        this.baseState = baseState;
    }

    @Override
    public MapCodec<? extends StairBlock> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockInfestation.spread(level, pos, 0, random);
    }
}
