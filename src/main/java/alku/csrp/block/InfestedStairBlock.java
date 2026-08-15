package alku.csrp.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Infested stairs; SRP 1.10.8 explicitly disables spread from stair variants. */
public final class InfestedStairBlock extends StairBlock {
    public static final MapCodec<InfestedStairBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("base_state").forGetter(block -> block.baseState),
            propertiesCodec()).apply(instance, InfestedStairBlock::new));

    private final BlockState baseState;

    public InfestedStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
        this.baseState = baseState;
    }

    @Override
    public MapCodec<? extends StairBlock> codec() {
        return CODEC;
    }

}
