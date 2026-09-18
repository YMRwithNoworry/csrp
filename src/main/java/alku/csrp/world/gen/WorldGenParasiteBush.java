package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of 1.10.9 {@code WorldGenParasiteBush}.
 *
 * <p>{@code type} selects one of the four original behaviour branches (the count is fixed per type,
 * exactly as in 1.10.9; the decorator only ever used types 1, 2 and 4):</p>
 * <ul>
 *   <li>{@code 1} — 128 scattered single bushes around the position (after sinking to the floor),</li>
 *   <li>{@code 2} — 4 scattered single bushes,</li>
 *   <li>{@code 3} — 64 flowers with a {@code y < 255} ceiling,</li>
 *   <li>{@code 4} — 128 vine runs (plus one at the position), each stacking 1..5 bushes upwards.</li>
 * </ul>
 */
public final class WorldGenParasiteBush {
    private final BlockState bushState;
    /** TENDRIL and BINE need ceiling support; POP, EYE and TOOH only need a floor. */
    private final boolean ceilingVariant;
    private final int type;

    public WorldGenParasiteBush(BlockState bushState, boolean ceilingVariant, int type) {
        this.bushState = bushState;
        this.ceilingVariant = ceilingVariant;
        this.type = type;
    }

    /** Convenience factory using the legacy {@code parasitebush} variant name (tendril, eye, ...). */
    public static WorldGenParasiteBush of(String variant, int type) {
        boolean tendril = "tendril".equalsIgnoreCase(variant) || "bine".equalsIgnoreCase(variant);
        return new WorldGenParasiteBush(
                ParasiteGenContext.variantState(ParasiteGenContext.PARASITE_BUSH, variant), tendril, type);
    }

    public boolean generate(ServerLevel level, RandomSource random, BlockPos position) {
        if (type == 3) {
            flower(level, random, position);
            return true;
        }
        if (type == 4) {
            for (int i = 0; i < 128; i++) {
                BlockPos pos = scatter(position, random);
                if (isValid(level, pos)) {
                    vine(level, random, pos);
                }
            }
            vine(level, random, position);
            return true;
        }

        BlockPos floor = position;
        int guard = 0;
        while ((ParasiteGenContext.isAir(level, floor)
                || ParasiteGenContext.isLeaves(level, floor))
                && floor.getY() > level.getMinY() && guard++ < 512) {
            floor = floor.below();
        }

        if (type == 1) {
            for (int i = 0; i < 128; i++) {
                BlockPos pos = scatter(floor, random);
                if (isValid(level, pos)) {
                    ParasiteGenContext.setBlock(level, pos, bushState);
                }
            }
        } else if (type == 2) {
            for (int i = 0; i < 4; i++) {
                BlockPos pos = scatter(floor, random);
                if (isValid(level, pos)) {
                    ParasiteGenContext.setBlock(level, pos, bushState);
                }
            }
        }
        return true;
    }

    private boolean isValid(ServerLevel level, BlockPos pos) {
        if (!ParasiteGenContext.isAir(level, pos)) {
            return false;
        }
        BlockPos below = pos.below();
        if (ceilingVariant) {
            return ParasiteGenContext.isSolidGround(level, pos.above())
                    || ParasiteGenContext.isFullFloor(level, below);
        }
        return ParasiteGenContext.isFullFloor(level, below);
    }

    private void flower(ServerLevel level, RandomSource random, BlockPos position) {
        for (int i = 0; i < 64; i++) {
            BlockPos pos = scatter(position, random);
            // The original capped the decorative flowers at y < 255.
            if (pos.getY() < 255 && isValid(level, pos)) {
                ParasiteGenContext.setBlock(level, pos, bushState);
            }
        }
    }

    private void vine(ServerLevel level, RandomSource random, BlockPos position) {
        for (int i = 0; i < 10; i++) {
            BlockPos pos = scatter(position, random);
            if (!ParasiteGenContext.isAir(level, pos)) {
                continue;
            }
            int height = 1 + random.nextInt(random.nextInt(3) + 3);
            for (int k = 0; k < height; k++) {
                BlockPos up = pos.offset(0, k, 0);
                if (ParasiteGenContext.isAir(level, up) && isValid(level, up)) {
                    ParasiteGenContext.setBlock(level, up, bushState);
                }
            }
        }
    }

    /** {@code pos.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), ...)}. */
    private static BlockPos scatter(BlockPos pos, RandomSource random) {
        return pos.offset(random.nextInt(8) - random.nextInt(8),
                random.nextInt(4) - random.nextInt(4),
                random.nextInt(8) - random.nextInt(8));
    }
}
