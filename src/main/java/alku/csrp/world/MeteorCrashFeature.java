package alku.csrp.world;

import alku.csrp.world.gen.WorldGenParasiteMeteorCrash;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * Stable entry point for the meteor impact, kept for the projectile entities
 * ({@link alku.csrp.entity.MeteorEntity}, {@link alku.csrp.entity.ParasiteProjectileEntity}).
 *
 * <p>The implementation is the line-by-line port of SRParasites 1.10.9
 * {@code WorldGenParasiteMeteorCrash} in {@link WorldGenParasiteMeteorCrash}; this facade only keeps
 * the {@code generate(level, random, pos, type)} signature the entities already call
 * ({@code type == 5} is the root meteor, anything else the fragment meteor).</p>
 */
public final class MeteorCrashFeature {
    private MeteorCrashFeature() {
    }

    public static void generate(ServerLevel level, RandomSource random, BlockPos pos, int type) {
        new WorldGenParasiteMeteorCrash(type).generate(level, random, pos);
    }
}
