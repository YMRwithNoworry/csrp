package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fog Nullifier: clears fog blocks in straight lines from itself. It can be
 * used three times before breaking.
 */
public final class FogNullifierBlock extends Block {
    public static final IntegerProperty USES = IntegerProperty.create("uses", 0, 3);

    public FogNullifierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(USES, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(USES);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        int cleared = 0;
        for (Direction direction : Direction.values()) {
            cleared += clearLine(serverLevel, pos, direction);
        }
        serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                24, 0.4D, 0.4D, 0.4D, 0.02D);
        if (cleared == 0) {
            return InteractionResult.SUCCESS;
        }
        int uses = state.getValue(USES) + 1;
        if (uses >= 3) {
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else {
            serverLevel.setBlock(pos, state.setValue(USES, uses), 3);
        }
        return InteractionResult.SUCCESS;
    }

    private int clearLine(ServerLevel level, BlockPos origin, Direction direction) {
        int cleared = 0;
        BlockPos current = origin.relative(direction);
        while (level.getBlockState(current).getBlock() instanceof FogBlock) {
            level.setBlock(current, Blocks.AIR.defaultBlockState(), 3);
            cleared++;
            current = current.relative(direction);
        }
        return cleared;
    }
}
