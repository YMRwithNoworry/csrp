package alku.csrp.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Spawn-validity light checks ported from the original EntityParasiteBase.
 *
 * <p>Both original checks are randomised on purpose (they are not plain threshold comparisons), so
 * this port keeps the exact random shapes:
 *
 * <pre>
 * isValidLightLevelTwo(): int light = blockLight(pos);
 *                         return light &lt;= random.nextInt(1000) &amp;&amp; light &lt;= 7
 *                                 ? random.nextInt(8) == 0 : false;
 * </pre>
 *
 * <p>{@code isValidLightLevelOne} additionally short-circuits to the two-check inside parasite
 * biomes, and otherwise probes sky light against {@code random.nextInt(32)}; its remaining tail has
 * not been transcribed yet, so only the two-check is exposed here.
 */
public final class SpawnLightChecks {
    private SpawnLightChecks() {
    }

    /** The original {@code isValidLightLevelTwo}: block light gated by two random tests. */
    public static boolean isValidLightLevelTwo(Level level, Entity entity) {
        return isValidLightLevelTwo(level, entityBlockPos(entity), level.getRandom());
    }

    /** Same check against an explicit position and random source (used by spawn predicates). */
    public static boolean isValidLightLevelTwo(BlockAndTintGetter level, BlockPos pos, RandomSource random) {
        int light = level.getBrightness(LightLayer.BLOCK, pos);
        return light <= random.nextInt(1000) && light <= 7 ? random.nextInt(8) == 0 : false;
    }

    /**
     * The original {@code isValidLightLevelOne}: the strict tier of the spawn-validity light check.
     *
     * <p>Two deliberate deviations from the original are recorded in
     * {@code docs/gap/RESTORATION_PROGRESS.md} (batch 94): the original short-circuits to the
     * two-check inside a parasite biome, but this port expresses parasite regions through terrain
     * rework rather than a biome type, so the caller passes {@code parasiteRegion}; and the original
     * temporarily lowers sky light while thundering, which this port does not emulate because it has
     * no writable sky-darken entry. Both deviations make the check stricter, never looser.
     */
    public static boolean isValidLightLevelOne(Level level, PathfinderMob mob, boolean parasiteRegion) {
        if (parasiteRegion) {
            return isValidLightLevelTwo(level, mob);
        }
        RandomSource random = level.getRandom();
        BlockPos pos = entityBlockPos(mob);
        if (level.getBrightness(LightLayer.SKY, pos) > random.nextInt(32)) {
            return false;
        }
        int light = mob.level().getMaxLocalRawBrightness(pos);
        return light <= random.nextInt(8) && mob.getWalkTargetValue(pos) >= 0.0F;
    }

    private static BlockPos entityBlockPos(Entity entity) {
        return new BlockPos((int) Math.floor(entity.getX()), (int) Math.floor(entity.getY()),
                (int) Math.floor(entity.getZ()));
    }
}
