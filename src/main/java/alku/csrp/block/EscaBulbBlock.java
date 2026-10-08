package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Esca Bulb: a large light-emitting block that can be dyed in 16 colors.
 */
public final class EscaBulbBlock extends Block {
    public EscaBulbBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.5F) {
            // Original BlockEscaBulb#randomDisplayTick used SRPEnumParticle.BLOOD, not a vanilla particle.
            SrpBlockParticles.spawnBlood(level, pos, random);
        }
    }
}
