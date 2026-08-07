package alku.csrp.block;

import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Original three-stage parasite fog produced around Dispatcher nests.
 */
public final class FogBlock extends HalfTransparentBlock {
    public static final IntegerProperty AIR = IntegerProperty.create("air", 0, 2);

    public FogBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AIR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AIR);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(AIR);
        if (stage == 0) {
            level.setBlock(pos, state.setValue(AIR, 1), 3);
            return;
        }
        if (stage != 2) {
            return;
        }

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos target = pos.offset(x, y, z);
                    BlockState targetState = level.getBlockState(target);
                    if (targetState.is(this) && targetState.getValue(AIR) != 2) {
                        level.setBlock(target, targetState.setValue(AIR, 2), 3);
                    }
                }
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.1F) {
            double x = pos.getX() + 0.5D + random.nextDouble() * 0.9D;
            double y = pos.getY() + 0.05D + random.nextDouble() * 0.9D;
            double z = pos.getZ() + 0.5D + random.nextDouble() * 0.9D;
            level.addParticle(ModParticles.FOG.get(), x, y, z, 0.0D, 0.0D, 0.0D);
        }
        if (random.nextFloat() < 0.005F) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    ModSounds.get("block.fog"), SoundSource.AMBIENT, 1.3F, 1.0F, false);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
