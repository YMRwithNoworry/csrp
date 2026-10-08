package alku.csrp.block;

import alku.csrp.particle.SrpColorParticleOptions;
import alku.csrp.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Shared spawners for the original {@code ParticleSpawner#spawnParticle} calls used by the parasite
 * blocks ({@code SRPEnumParticle.SPORE / FOG / BLOOD / DOT}).
 *
 * <p>The 1.12.2 {@code ParticleSpawner} packet carried the particle id plus the {@code (r, g, b)}
 * extras; 26.3's {@code Level#addParticle} takes a {@code ParticleOptions} instance instead, so the
 * coloured ones wrap their values in {@link SrpColorParticleOptions}.</p>
 */
public final class SrpBlockParticles {
    private SrpBlockParticles() {
    }

    /** Original {@code SRPEnumParticle.SPORE} at {@code y + 2.5}. */
    public static void spawnSpore(Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ModParticles.SPORE.get(),
                pos.getX() + random.nextDouble(),
                pos.getY() + 2.5D,
                pos.getZ() + random.nextDouble(),
                0.0D, 0.0D, 0.0D);
    }

    /** Original {@code SRPEnumParticle.FOG} at {@code y + 1.5} with the green tint {@code (0, g, 0)}. */
    public static void spawnFog(Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ModParticles.FOG.get(),
                pos.getX() + random.nextDouble(),
                pos.getY() + 1.5D,
                pos.getZ() + random.nextDouble(),
                0.0D, 1.0E-4D, 0.0D);
    }

    /** Original {@code SRPEnumParticle.BLOOD} variant 0. */
    public static void spawnBlood(Level level, BlockPos pos, RandomSource random) {
        level.addParticle(new SrpColorParticleOptions(ModParticles.BLOOD.get(), 0, 0, 0, 0),
                pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                pos.getY() + 3.05D + random.nextDouble() * 0.9D,
                pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                0.0D, 0.0D, 0.0D);
    }

    /** Original {@code SRPEnumParticle.DOT} with an explicit colour. */
    public static void spawnDot(Level level, BlockPos pos, RandomSource random, int red, int green, int blue) {
        level.addParticle(new SrpColorParticleOptions(ModParticles.DOT.get(), red, green, blue, 0),
                pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                pos.getY() + 0.05D + random.nextDouble() * 0.9D,
                pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                0.0D, 0.0D, 0.0D);
    }

    /** Original {@code SRPEnumParticle.RAGE}. */
    public static void spawnRage(Level level, double x, double y, double z) {
        level.addParticle(ModParticles.RAGE.get(), x, y, z, 0.0D, 0.0D, 0.0D);
    }

    /** Original {@code SRPEnumParticle.RHAPPY}. */
    public static void spawnRHappy(Level level, double x, double y, double z) {
        level.addParticle(ModParticles.RHAPPY.get(), x, y, z, 0.0D, 0.0D, 0.0D);
    }

    /** Original {@code SRPEnumParticle.EEN}. */
    public static void spawnEen(Level level, double x, double y, double z,
                                double velocityX, double velocityY, double velocityZ) {
        level.addParticle(ModParticles.EEN.get(), x, y, z, velocityX, velocityY, velocityZ);
    }

    /** Original {@code SRPEnumParticle.WIND}. */
    public static void spawnWind(Level level, double x, double y, double z,
                                 double velocityX, double velocityY, double velocityZ) {
        level.addParticle(ModParticles.WIND.get(), x, y, z, velocityX, velocityY, velocityZ);
    }
}
