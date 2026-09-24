package alku.csrp.world.gen;

import alku.csrp.block.ParasiteLootBlock;
import alku.csrp.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of SRParasites 1.10.9 {@code WorldGenParasiteColonyBase}.
 *
 * <p>The original was a {@code WorldGenerator} subclass that gave the colony family (B1..B4,
 * BS1..BS4, the colony core, the three nexus protections and the meteor crash) a shared vocabulary
 * of shell primitives: {@code generateCircle} / {@code generateSphere} / {@code placeColumn} /
 * {@code directionToGrow} and the DNA-helix and entrance helpers.  This port keeps the same
 * primitives, the same call order and the same magic numbers so every subclass can be transliterated
 * one statement at a time.</p>
 *
 * <p>Deviations from the original, all forced by the platform rather than chosen:</p>
 * <ul>
 *   <li>{@code World} becomes {@link ServerLevel} and {@code Random} becomes {@link RandomSource};
 *       writes go through {@link ParasiteGenContext#setBlock} (flag 2, exactly the original
 *       {@code World#setBlockState(pos, state, 2)}), with build-height and chunk-load guards that
 *       1.12.2 did not need because decoration always ran on a loaded chunk.</li>
 *   <li>The original mixed the chunk random it was handed with {@code world.rand}; both are the same
 *       generator stream in this port, so only the number of draws is preserved, not which of the
 *       two objects supplied them.</li>
 *   <li>The {@code y &gt; 2 &amp;&amp; y &lt; 240} gates stay literal: they are the world-gen keep-out
 *       band of the original, not a build-height probe.</li>
 *   <li>{@code instanceof BlockBase} (every block of the mod) becomes
 *       {@link ParasiteGenContext#isModBlock}.</li>
 * </ul>
 */
public abstract class WorldGenParasiteColonyBase {
    /** {@code SRPBlocks.ParasiteLoot} tiers — the original read the three config id lists. */
    protected static final ParasiteLootBlock.Tier LOOT_COMMON = ParasiteLootBlock.Tier.COMMON;
    protected static final ParasiteLootBlock.Tier LOOT_UNCOMMON = ParasiteLootBlock.Tier.UNCOMMON;
    protected static final ParasiteLootBlock.Tier LOOT_RARE = ParasiteLootBlock.Tier.RARE;

    /** The original {@code stage} constructor argument. */
    protected int type;

    protected BlockState floor = ParasiteGenContext.STAIN_DIRT;
    protected BlockState tacle = ParasiteGenContext.STAIN_FEELER;
    protected BlockState wall = ParasiteGenContext.DENSE_WALL;
    protected BlockState floorColony = ParasiteGenContext.RUBBLE_FLESH;

    protected WorldGenParasiteColonyBase(int stage) {
        this.type = stage;
    }

    /** {@code func_180709_b(World, Random, BlockPos)} — the generation entry point. */
    public abstract boolean generate(ServerLevel level, RandomSource random, BlockPos pos);

    // ---------------------------------------------------------------------------------------------
    // Small write helpers
    // ---------------------------------------------------------------------------------------------

    protected void placeBlock(ServerLevel level, BlockPos pos, BlockState state) {
        ParasiteGenContext.setBlock(level, pos, state);
    }

    /** {@code placeVine}: the bine variant of the parasite bush. */
    protected void placeVine(ServerLevel level, BlockPos pos) {
        ParasiteGenContext.setBlock(level, pos, ParasiteGenContext.BUSH_BINE);
    }

    protected void placeReplacement(ServerLevel level, BlockPos pos, BlockState state) {
        ParasiteGenContext.setBlock(level, pos, state);
    }

    // ---------------------------------------------------------------------------------------------
    // Directions — the original SRG-faithful mapping (0 = north, 1 = south, 2 = west, 3 = east)
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code getDirectionRoot}: the {@code BlockPos#func_177964_d/177965_g/177970_e/177985_f}
     * family, i.e. 0 = north, 1 = south, 2 = west, 3 = east.  This is deliberately <em>not</em>
     * {@link ParasiteGenContext#horizontal}, which uses the {@code directionToGrow} ordering.
     */
    protected BlockPos getDirectionRoot(BlockPos center, int direction, int times) {
        return switch (direction) {
            case 0 -> center.north(times);
            case 1 -> center.south(times);
            case 2 -> center.west(times);
            default -> center.east(times);
        };
    }

    /** {@code directionToGrow}: 0 = north, 1 = east, 2 = south, 3 = west; doubles on {@code sideCurse}. */
    protected BlockPos directionToGrow(BlockPos atm, int choice, boolean sideCurse) {
        if (sideCurse) {
            return ParasiteGenContext.sideCurse(atm, choice);
        }
        return switch (choice) {
            case 0 -> atm.north();
            case 1 -> atm.east();
            case 3 -> atm.west();
            default -> atm.south();
        };
    }

    /**
     * {@code placeColumn}: walks {@code in} blocks up (when {@code extraChance == 1.0}) or down and
     * places {@code state} at every step plus one past the end, returning the final position.
     */
    protected BlockPos placeColumn(ServerLevel level, BlockPos pos, int in, RandomSource random,
            double extraChance, BlockState state) {
        int current = pos.getY();
        int atm = current;
        BlockPos newPos = pos;

        while (current < atm + in) {
            placeBlock(level, newPos, state);
            if (extraChance == 1.0D) {
                newPos = newPos.above();
            } else {
                newPos = newPos.below();
            }
            current++;
        }

        placeBlock(level, newPos, state);
        return newPos;
    }

    // ---------------------------------------------------------------------------------------------
    // Vines
    // ---------------------------------------------------------------------------------------------

    /** {@code addVines}: hangs bines downward while the two blocks below stay air. */
    protected void addVines(ServerLevel level, BlockPos position, RandomSource random, int longer) {
        if (!ParasiteGenContext.get(level, position).isAir()) {
            return;
        }
        placeVine(level, position);

        for (int chance = longer; chance > 0; chance--) {
            if (!ParasiteGenContext.get(level, position.below()).isAir()
                    || !ParasiteGenContext.get(level, position.below(2)).isAir()) {
                return;
            }
            if (random.nextInt(chance) == 0) {
                return;
            }
            position = position.below();
            placeVine(level, position);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Floors
    // ---------------------------------------------------------------------------------------------

    /** {@code replaceLayer}: exact-state replacement inside a square. */
    protected void replaceLayer(ServerLevel level, BlockPos position, int range, BlockState toReplace,
            BlockState in) {
        int xx = position.getX();
        int zz = position.getZ();
        int yy = position.getY();

        for (int x = xx - range; x <= xx + range; x++) {
            for (int z = zz - range; z <= zz + range; z++) {
                BlockPos target = new BlockPos(x, yy, z);
                if (ParasiteGenContext.get(level, target) == toReplace) {
                    placeReplacement(level, target, in);
                }
            }
        }
    }

    /** {@code addFloor}: lays the colony floor until it meets mod material, optionally filling. */
    protected void addFloor(ServerLevel level, BlockPos position, int range, boolean fill) {
        if (fill) {
            placeReplacement(level, position, floorColony);
        }

        int offsetN = 2;

        for (BlockPos atm = position.north();
                !ParasiteGenContext.isModBlock(ParasiteGenContext.get(level, atm));
                offsetN += 2) {
            placeReplacement(level, atm, floorColony);
            genFloorFloor(level, atm.below(), 15, fill);
            placeReplacement(level, atm.west(offsetN), floorColony);
            genFloorFloor(level, atm.west(offsetN).below(), 15, fill);
            atm = atm.north();
        }

        for (BlockPos here = position.south(1);
                !ParasiteGenContext.isModBlock(ParasiteGenContext.get(level, here));
                here = here.east()) {
            int offsetNx = 2;

            for (BlockPos atm = here.north();
                    !ParasiteGenContext.isModBlock(ParasiteGenContext.get(level, atm));
                    offsetNx += 2) {
                placeReplacement(level, atm, floorColony);
                genFloorFloor(level, atm.below(), 15, fill);
                placeReplacement(level, atm.west(offsetNx), floorColony);
                genFloorFloor(level, atm.west(offsetNx).below(), 15, fill);
                atm = atm.north();
            }

            placeReplacement(level, here, floorColony);
            genFloorFloor(level, here.below(), 15, fill);
        }

        for (BlockPos atm = position.east(1);
                !ParasiteGenContext.isModBlock(ParasiteGenContext.get(level, atm));
                atm = atm.west()) {
            int offsetNx = 2;

            for (BlockPos inner = atm.north();
                    !ParasiteGenContext.isModBlock(ParasiteGenContext.get(level, inner));
                    offsetNx += 2) {
                placeReplacement(level, inner, floorColony);
                genFloorFloor(level, inner.below(), 15, fill);
                placeReplacement(level, inner.west(offsetNx), floorColony);
                genFloorFloor(level, inner.west(offsetNx).below(), 15, fill);
                inner = inner.north();
            }

            placeReplacement(level, atm, floorColony);
            genFloorFloor(level, atm.below(), 15, fill);
        }

        if (!fill) {
            addFloorSpace(level, position);
        }
    }

    /** {@code genFloorFloor}: pushes the stain floor down through air, bushes and leaves. */
    protected void genFloorFloor(ServerLevel level, BlockPos position, int range, boolean fill) {
        if (!fill) {
            return;
        }
        BlockPos filler = position;

        while (range > 0 && isFloorReplaceable(ParasiteGenContext.get(level, filler))) {
            placeReplacement(level, filler, floor);
            filler = filler.below();
            range--;
        }
    }

    private static boolean isFloorReplaceable(BlockState state) {
        return state.isAir()
                || state.getBlock() == ParasiteGenContext.PARASITE_BUSH.getBlock()
                || state.getBlock() instanceof LeavesBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.BushBlock;
    }

    /** {@code addFloorSpace}: punches the 3x3 hole the colony floor leaves at the entrance. */
    protected void addFloorSpace(ServerLevel level, BlockPos position) {
        int xx = position.getX();
        int zz = position.getZ();
        int yy = position.getY();
        int range = 1;

        for (int x = xx - range; x <= xx + range; x++) {
            for (int z = zz - range; z <= zz + range; z++) {
                BlockPos target = new BlockPos(x, yy, z);
                if (ParasiteGenContext.get(level, target) == floorColony) {
                    placeReplacement(level, target, ParasiteGenContext.AIR);
                }
            }
        }
    }

    /** The original hook was empty; kept so subclasses can be transliterated verbatim. */
    protected void addMobSpawner(ServerLevel level, BlockPos position, int type, double chance) {
    }

    // ---------------------------------------------------------------------------------------------
    // Shells
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code generateSphere}: stacks a tapered cylinder (inner), a short crown (outer) and a tip
     * section, each layer drawn by {@link #generateCircle}.
     */
    protected void generateSphere(ServerLevel level, BlockPos posss, int innerHeight, int outerHeight,
            RandomSource rand, boolean invertedTip, int valueStarting, boolean random, int heightBelow,
            int heightAbove, int tip, BlockState state1, BlockState state2, BlockState state3,
            int incomplete) {
        int xx = valueStarting;
        int zz = valueStarting;
        int test = innerHeight;
        int ticc = 2;

        while (test > 0) {
            test--;
            int heig = heightBelow;

            while (heig > 0) {
                heig--;
                generateCircle(state1, state2, level, rand, posss, xx, zz, 1, incomplete, 6);
                generateCircle(state3, state3, level, rand, posss, xx - ticc, zz - ticc, 1, 50000, 6);
                posss = posss.above();
            }

            if (rand.nextBoolean() && random) {
                xx += 2;
            } else {
                xx++;
            }

            if (rand.nextBoolean() && random) {
                zz += 2;
            } else {
                zz++;
            }
        }

        for (int remaining = outerHeight; remaining > 0; posss = posss.above()) {
            remaining--;
            generateCircle(state1, state2, level, rand, posss, xx, zz, 1, incomplete, 0);
            generateCircle(state3, state3, level, rand, posss, xx - 2, zz - 2, 1, 50000, 0);
        }

        test = valueStarting + tip;
        if (invertedTip) {
            heightAbove = (int) (heightAbove * 0.5D);
        }

        while (test > 0) {
            test--;

            for (int heig = heightAbove; heig > 0; posss = posss.above()) {
                heig--;
                generateCircle(state1, state2, level, rand, posss, xx, zz, 1, incomplete, invertedTip ? 9 : 0);
                generateCircle(state3, state3, level, rand, posss, xx - ticc, zz - ticc, 1, 50000,
                        invertedTip ? 9 : 0);
            }

            if (invertedTip) {
                if (rand.nextBoolean() && random) {
                    xx += 2;
                } else {
                    xx++;
                }
                if (rand.nextBoolean() && random) {
                    zz += 2;
                } else {
                    zz++;
                }
            } else {
                if (rand.nextBoolean() && random) {
                    xx -= 2;
                } else {
                    xx--;
                }
                if (rand.nextBoolean() && random) {
                    zz -= 2;
                } else {
                    zz--;
                }
            }
        }
    }

    /** {@code generateCircle}: one elliptical disc, with the 1/60 loot-or-blood rim roll. */
    protected boolean generateCircle(BlockState state, BlockState state2, ServerLevel level,
            RandomSource rand, BlockPos pos, int radiusX, int radiusZ, int height, int incomplete,
            int veins) {
        if (pos.getY() <= 2 || pos.getY() >= 240) {
            return false;
        }
        if (radiusX <= 0 || radiusZ <= 0) {
            return false;
        }

        for (int y = 0; y < height; y++) {
            BlockPos layerPos = pos.above(y);

            for (int x = -radiusX; x <= radiusX; x++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    double normalizedX = (double) x / radiusX;
                    double normalizedZ = (double) z / radiusZ;
                    if (normalizedX * normalizedX + normalizedZ * normalizedZ > 1.0D) {
                        continue;
                    }
                    BlockPos target = layerPos.offset(x, 0, z);
                    BlockState existing = ParasiteGenContext.get(level, target);
                    boolean flagAir = state.isAir() && state2.isAir();
                    if (!(existing.isAir() || ParasiteGenContext.isModBlock(existing) || flagAir)
                            || rand.nextInt(incomplete) == 0) {
                        continue;
                    }

                    boolean rim = x == radiusX || z == radiusZ || x == -radiusX || z == -radiusZ
                            || x + 1 == radiusX || z + 1 == radiusZ
                            || x - 1 == -radiusX || z - 1 == -radiusZ;
                    if (rim && rand.nextInt(60) == 0) {
                        if (rand.nextInt(4) == 0) {
                            if (!flagAir) {
                                placeReplacement(level, target, ParasiteGenContext.DEAD_BLOOD);
                                if (veins > 0) {
                                    addVines(level, target.below(), rand, veins);
                                }
                            }
                        } else if (!flagAir) {
                            if (rand.nextInt(10) == 0) {
                                ParasiteGenContext.placeLoot(level, target, LOOT_RARE, rand);
                            } else if (rand.nextInt(4) == 0) {
                                ParasiteGenContext.placeLoot(level, target, LOOT_UNCOMMON, rand);
                            } else {
                                ParasiteGenContext.placeLoot(level, target, LOOT_COMMON, rand);
                            }

                            if (veins > 0) {
                                addVines(level, target.below(), rand, veins);
                            }
                        }
                    } else if (rand.nextBoolean()) {
                        placeReplacement(level, target, state);
                        if (veins > 0 && !flagAir) {
                            addVines(level, target.below(), rand, veins);
                        }
                    } else {
                        placeReplacement(level, target, state2);
                        if (veins > 0 && !flagAir) {
                            addVines(level, target.below(), rand, veins);
                        }
                    }
                }
            }
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // DNA helix and circles
    // ---------------------------------------------------------------------------------------------

    /** {@code generateDNAHelix}: two interleaved strands of pitch {@code pitch} over {@code numTurns}. */
    protected boolean generateDNAHelix(BlockState state, ServerLevel level, RandomSource rand,
            BlockPos pos, double radius, int numTurns, double pitch) {
        if (pos.getY() <= 2 || pos.getY() >= 240) {
            return false;
        }
        double tStep = 0.1D;

        for (double t = 0.0D; t <= numTurns * 2 * Math.PI; t += tStep) {
            double yOffset = pitch / (Math.PI * 2) * t;
            int y = pos.getY() + (int) Math.round(yOffset);
            int x1 = pos.getX() + (int) Math.round(radius * Math.cos(t));
            int z1 = pos.getZ() + (int) Math.round(radius * Math.sin(t));
            int x2 = pos.getX() + (int) Math.round(radius * Math.cos(t + Math.PI));
            int z2 = pos.getZ() + (int) Math.round(radius * Math.sin(t + Math.PI));
            placeReplacement(level, new BlockPos(x1, y, z1), state);
            placeReplacement(level, new BlockPos(x2, y, z2), state);
        }

        return true;
    }

    public BlockPos getCirclePoint(BlockPos center, int radius, double theta) {
        int x = center.getX() + (int) Math.round(Math.cos(theta) * radius);
        int z = center.getZ() + (int) Math.round(Math.sin(theta) * radius);
        return new BlockPos(x, center.getY(), z);
    }

    public List<BlockPos> getCirclePoints(BlockPos center, int radius, int steps) {
        List<BlockPos> points = new ArrayList<>();
        double cy = center.getY();

        for (int i = 0; i < steps; i++) {
            double theta = (Math.PI * 2) * i / steps;
            int x = center.getX() + (int) Math.round(Math.cos(theta) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(theta) * radius);
            points.add(new BlockPos(x, (int) cy, z));
        }

        return points;
    }

    /** {@code generatePillar}: a column of randomly alternating shell and inner material. */
    public void generatePillar(ServerLevel level, BlockPos basePos, int height, BlockState blockState,
            BlockState blockState2) {
        for (int dy = 0; dy < height; dy++) {
            BlockPos pos = basePos.above(dy);
            if (level.getRandom().nextBoolean()) {
                placeReplacement(level, pos, blockState);
            } else {
                placeReplacement(level, pos, blockState2);
            }
        }
    }

    /** {@code replaceCircleGround}: overwrites a horizontal disc, sparing mod blocks and air. */
    public void replaceCircleGround(ServerLevel level, BlockPos center, int radius, BlockState target) {
        int cx = center.getX();
        int cz = center.getZ();
        int cy = center.getY();
        if (cy <= 2 || cy >= 240) {
            return;
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos pos = new BlockPos(cx + dx, cy, cz + dz);
                BlockState current = ParasiteGenContext.get(level, pos);
                if (!ParasiteGenContext.isModBlock(current) && !current.isAir()) {
                    placeReplacement(level, pos, target);
                }
            }
        }
    }

    /** {@code generateVerticalCircle}: a stone ring in the X/Y or Z/Y plane. */
    public void generateVerticalCircle(ServerLevel level, BlockPos center, int radius, boolean useXZPlane) {
        double step = Math.PI / (radius * 4);

        for (double theta = 0.0D; theta < Math.PI * 2; theta += step) {
            int dy = (int) Math.round(radius * Math.sin(theta));
            int dPrimary = (int) Math.round(radius * Math.cos(theta));
            BlockPos target = useXZPlane
                    ? center.offset(dPrimary, dy, 0)
                    : center.offset(0, dy, dPrimary);
            placeReplacement(level, target, ParasiteGenContext.STONE);
        }
    }

    /** {@code generateFilledVerticalDisk}: carves a filled disc, sparing blood and bushes. */
    public void generateFilledVerticalDisk(ServerLevel level, BlockPos center, int radius, boolean useXZPlane) {
        int rSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx * dx + dy * dy > rSq) {
                    continue;
                }
                BlockPos target = useXZPlane
                        ? center.offset(dx, dy, 0)
                        : center.offset(0, dy, dx);
                BlockState existing = ParasiteGenContext.get(level, target);
                if (existing.getBlock() != ModBlocks.DEAD_BLOOD.get()
                        && existing.getBlock() != ParasiteGenContext.PARASITE_BUSH.getBlock()) {
                    placeReplacement(level, target, ParasiteGenContext.AIR);
                }
            }
        }
    }

    /** {@code addEntrance}: walks out three blocks and carves a disk every further step. */
    public void addEntrance(ServerLevel level, RandomSource rand, BlockPos position, int entrance) {
        int direction = rand.nextInt(4);
        int offset = 3;

        while (entrance > 0) {
            if (offset > 0) {
                position = directionToGrow(position, direction, false);
                offset--;
            } else {
                entrance--;
                position = directionToGrow(position, direction, false);
                generateFilledVerticalDisk(level, position.above(), 3, direction != 1 && direction != 3);
            }
        }
    }
}
