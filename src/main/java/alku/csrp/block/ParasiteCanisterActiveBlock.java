package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 活体寄生囊肿（原版 canisteractive）：寄生体超距消失时落下的资源囊肿。
 *
 * <p>1.20.1 target 里只负责形状与外观；原版的消化方块实体
 * （{@code ParasiteCanisterBlockEntity}）与超距消失处理器尚未移植，
 * 因此这里注册的是它落地的固定形态。</p>
 */
public final class ParasiteCanisterActiveBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 11.0D, 15.0D);

    public ParasiteCanisterActiveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
