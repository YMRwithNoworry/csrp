package alku.csrp.command;

import alku.csrp.registry.ModBlocks;
import alku.csrp.world.gen.HarlequinBlockConverter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Port of SRP 1.10.9 {@code network/CommandHarlequinConvert} (out109, 176 lines) —
 * {@code /harlequin_convert [radius] [sampleChance] [depth] [blotchChance] [blotchMinDiam]
 * [blotchMaxDiam]} at permission level 2.
 *
 * <p>Two passes over the {@code (2r+1)x(2r+1)} column square around the caller:</p>
 * <ol>
 *   <li><em>blotch seeding</em>: a column is seeded with probability {@code 1/blotchChance} (always
 *       when {@code blotchChance <= 1}); the seed rolls a diameter in {@code [min,max]} and marks
 *       every column inside that circle whose top block is grass or would itself turn into harlequin
 *       grass (sunlit sand / sandstone / stone).</li>
 *   <li><em>conversion</em>: for every column, from the heightmap top down to {@code max(5, top-depth)},
 *       a block is handed to {@link HarlequinBlockConverter} when its column was marked, when it is
 *       leaves, or when a 1-in-{@code sampleChance} roll fires.  A converted block gets a
 *       {@code lipoma_mass} under it with 30 % probability (never under {@code alveoli}), and a marked
 *       grass column that became {@code harleskinn_block} is counted as "cell converted".</li>
 * </ol>
 *
 * <p>26.3 notes: the original's client-side guard is dropped — a command source is always
 * server-side here — and {@code World#getHeight(pos)} (MOTION_BLOCKING) is spelled out as
 * {@link ServerLevel#getHeight} with {@link Heightmap.Types#MOTION_BLOCKING}.</p>
 */
public final class HarlequinConvertCommand {
    public static final String NAME = "harlequin_convert";

    /** {@code CommandHarlequinConvert}: positional arguments, defaults and {@code parseInt} bounds. */
    private static final String[] ARGUMENTS = {
            "radius", "sampleChance", "depth", "blotchChance", "blotchMinDiam", "blotchMaxDiam"
    };
    private static final int[] DEFAULTS = {16, 1, 32, 18, 3, 5};
    private static final int[] MINIMUMS = {1, 1, 1, 1, 1, 1};
    private static final int[] MAXIMUMS = {256, 128, 256, 256, 64, 64};

    /** {@code world.setBlockState(pos, state, 2)} for the lipoma under a converted block. */
    private static final int UPDATE_CLIENTS = 2;

    private HarlequinConvertCommand() {
    }

    /** The seven-block palette {@code CommandHarlequinConvert} built from {@code SRPBlocks}. */
    public static HarlequinBlockConverter.Config config() {
        return new HarlequinBlockConverter.Config(
                ModBlocks.HARLESKINN_BLOCK.get(),
                ModBlocks.legacyBlock("harlequinn_grass").get(),
                ModBlocks.ALVEOLI.get(),
                ModBlocks.ALVEOLI_GROWTH.get(),
                ModBlocks.legacyBlock("lipoma_mass").get(),
                ModBlocks.legacyBlock("tresses_hair").get(),
                ModBlocks.legacyBlock("hirsute_hair").get());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        root.executes(context -> convert(context.getSource(), DEFAULTS.clone()));

        ArgumentBuilder<CommandSourceStack, ?> node = root;
        for (int index = 0; index < ARGUMENTS.length; index++) {
            int lastIndex = index;
            RequiredArgumentBuilder<CommandSourceStack, Integer> next = Commands.argument(ARGUMENTS[index],
                    IntegerArgumentType.integer(MINIMUMS[index], MAXIMUMS[index]));
            next.executes(context -> convert(context.getSource(), values(context, lastIndex)));
            node.then(next);
            node = next;
        }
        return root;
    }

    private static int[] values(CommandContext<CommandSourceStack> context, int lastIndex) {
        int[] values = DEFAULTS.clone();
        for (int index = 0; index <= lastIndex; index++) {
            values[index] = IntegerArgumentType.getInteger(context, ARGUMENTS[index]);
        }
        return values;
    }

    private static int convert(CommandSourceStack source, int[] args) {
        int radius = args[0];
        int sampleChance = args[1];
        int depth = args[2];
        int blotchChance = args[3];
        int blotchMinDiam = args[4];
        int blotchMaxDiam = args[5];

        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        RandomSource random = level.getRandom();
        HarlequinBlockConverter.Config config = config();

        int changed = 0;
        Set<Long> blotchXZ = new HashSet<>();
        int blotchesSeeded = 0;
        int blotchCellsMarked = 0;
        int blotchCellsConverted = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                boolean seedThisColumn = blotchChance <= 1 || random.nextInt(Math.max(2, blotchChance)) == 0;
                if (!seedThisColumn) {
                    continue;
                }
                int minDiameter = Math.min(blotchMinDiam, blotchMaxDiam);
                int maxDiameter = Math.max(blotchMinDiam, blotchMaxDiam);
                int diameter = minDiameter + random.nextInt(maxDiameter - minDiameter + 1);
                int r = diameter / 2;
                boolean anyMarked = false;

                for (int ox = -r; ox <= r; ox++) {
                    for (int oz = -r; oz <= r; oz++) {
                        if (ox * ox + oz * oz > r * r) {
                            continue;
                        }
                        int sx = x + ox;
                        int sz = z + oz;
                        int ySurf = level.getHeight(Heightmap.Types.MOTION_BLOCKING, sx, sz) - 1;
                        if (ySurf < 0) {
                            continue;
                        }
                        BlockPos top = new BlockPos(sx, ySurf, sz);
                        BlockState topState = level.getBlockState(top);
                        boolean wouldBecomeHarlequinn = level.canSeeSky(top.above())
                                && (HarlequinBlockConverter.isAnySand(topState)
                                        || HarlequinBlockConverter.isAnySandstone(topState)
                                        || topState.is(Blocks.STONE));
                        if ((topState.is(Blocks.GRASS_BLOCK) || wouldBecomeHarlequinn)
                                && blotchXZ.add(keyXZ(sx, sz))) {
                            blotchCellsMarked++;
                            anyMarked = true;
                        }
                    }
                }

                if (anyMarked) {
                    blotchesSeeded++;
                }
            }
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int yTop = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                if (yTop < 0) {
                    continue;
                }
                for (int y = yTop; y >= Math.max(5, yTop - depth); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    boolean blotchHere = blotchXZ.contains(keyXZ(x, z));
                    if (!blotchHere && !(state.getBlock() instanceof LeavesBlock)
                            && random.nextInt(Math.max(1, sampleChance)) != 0) {
                        continue;
                    }
                    if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
                        continue;
                    }
                    Block replacedWith = HarlequinBlockConverter.convert(level, pos, state, blotchHere,
                            random, config);
                    if (replacedWith == null) {
                        continue;
                    }
                    if (replacedWith != config.alveoli() && random.nextInt(100) < 30) {
                        BlockPos below = pos.below();
                        if (level.isEmptyBlock(below)) {
                            level.setBlock(below, config.lipoma().defaultBlockState(), UPDATE_CLIENTS);
                        }
                    }
                    changed++;
                    if (blotchHere && state.is(Blocks.GRASS_BLOCK) && replacedWith == config.harleskinn()) {
                        blotchCellsConverted++;
                    }
                }
            }
        }

        int reported = changed;
        int seeded = blotchesSeeded;
        int marked = blotchCellsMarked;
        int converted = blotchCellsConverted;
        int minDiameter = Math.min(blotchMinDiam, blotchMaxDiam);
        int maxDiameter = Math.max(blotchMinDiam, blotchMaxDiam);
        source.sendSuccess(() -> Component.literal("Harlequin convert: changed ~" + reported
                + " | blotches: " + seeded
                + " | cells marked: " + marked
                + " | cells converted: " + converted
                + " | radius " + radius
                + " | 1-in-" + sampleChance
                + " | depth " + depth
                + " | blotchChance 1-in-" + Math.max(1, blotchChance)
                + " | blotchDiam " + minDiameter + ".." + maxDiameter), false);
        return Command.SINGLE_SUCCESS;
    }

    /** {@code CommandHarlequinConvert#keyXZ} — {@code (long) x << 32 ^ z & 0xFFFFFFFFL}. */
    static long keyXZ(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
