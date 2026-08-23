package alku.csrp.block;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Infested stairs; SRP 1.10.8 explicitly disables spread from stair variants. */
public final class InfestedStairBlock extends StairBlock {

    private final BlockState baseState;

    public InfestedStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
        this.baseState = baseState;
    }

}
