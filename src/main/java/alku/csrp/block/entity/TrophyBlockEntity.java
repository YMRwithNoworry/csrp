package alku.csrp.block.entity;

import alku.csrp.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TrophyBlockEntity extends BlockEntity {
    public TrophyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TROPHY.get(), pos, state);
    }
}
