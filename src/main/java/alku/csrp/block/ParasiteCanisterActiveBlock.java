package alku.csrp.block;

import alku.csrp.block.entity.ParasiteCanisterBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import alku.csrp.registry.ModBlockEntities;

/**
 * 活体寄生囊肿（原版 ParasiteCanisterActive）：寄生体超距消失时落下的资源囊肿，
 * 会随时间消化内部物品并给予进化点，消化完毕后自行消失。
 */
public final class ParasiteCanisterActiveBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 11.0D, 15.0D);

    public ParasiteCanisterActiveBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ParasiteCanisterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide ? null
                : (level1, pos, state1, blockEntity) ->
                        ParasiteCanisterBlockEntity.serverTick(level1, pos, state1, blockEntity);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
