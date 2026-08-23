package alku.csrp.block;

import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Solid residue grows residue bloomings on exposed faces. */
public final class ResidueBlock extends Block {
    public ResidueBlock(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) {
            return;
        }
        Direction[] directions = Direction.values();
        for (int attempt = 0; attempt < 3; attempt++) {
            Direction direction = directions[random.nextInt(directions.length)];
            BlockPos target = pos.relative(direction);
            BlockState blooming = ModBlocks.RESIDUE_PLANTS.get().defaultBlockState()
                    .setValue(ResidueBloomingBlock.FACING, direction);
            if (level.getBlockState(target).isAir() && blooming.canSurvive(level, target)) {
                level.setBlock(target, blooming, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
public void entityInside(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5D, 1.0D, 0.5D));
    }
}
