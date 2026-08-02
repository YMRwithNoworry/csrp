package alku.csrp.block;

import alku.csrp.registry.ModEntities;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Residue periodically attempts to create the phase-dependent reinforcement nexus. */
public final class ResidueBlock extends Block {
    private static final int[] REINFORCEMENT_INTERVAL = {
            0, 0, 0, 5_500, 4_000, 1_000, 500, 400, 300, 250, 150
    };

    public ResidueBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int phase = SrpWorldData.get(level).evolutionPhase();
        if (phase >= 3 && random.nextInt(REINFORCEMENT_INTERVAL[Math.min(10, phase)]) == 0) {
            var reinforcement = ModEntities.BECKON_SI.get().create(level);
            BlockPos spawnPos = pos.above();
            if (reinforcement != null && level.getBlockState(spawnPos).isAir()) {
                reinforcement.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        random.nextFloat() * 360.0F, 0.0F);
                reinforcement.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.MOB_SUMMONED, null);
                if (!level.noCollision(reinforcement)) {
                    reinforcement.discard();
                } else {
                    level.addFreshEntity(reinforcement);
                }
            }
        }
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, 1);
    }
}
