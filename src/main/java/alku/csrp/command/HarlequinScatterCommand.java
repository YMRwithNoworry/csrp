package alku.csrp.command;

import alku.csrp.Csrp;
import alku.csrp.world.ParasiteBiomeDecorator;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Port of SRP 1.10.9 {@code network/CommandHarlequinScatter} (out109, 225 lines) —
 * {@code /harlequin_scatter <rock|bush|tree|ruin|both|all> [tries=32] [radius=32]} at permission
 * level 2.
 *
 * <p>Scatters Harlequin structure templates around the caller: {@code tries} random columns inside
 * {@code radius} blocks, each accepted only when the column is in the Harlequin biome, its ground is
 * sand / sandstone / red sandstone / grass / dirt (or {@code harlequinn_grass} /
 * {@code harleskinn_block}), its top is not water, lava or leaves, and the neighbourhood is flat
 * enough for the template being placed.  {@code rock} and {@code bush} need a 5-block radius within a
 * 3-block height delta, {@code tree} 6/3, {@code ruin} 10/4; {@code both} and {@code all} combine the
 * families with the same offsets and cluster counts as the original.</p>
 *
 * <p>The 1.12.2 ground ids are compared by registry name because the original compared
 * {@code srparasites:harlequinn_grass} / {@code srparasites:harleskinn_block}; this port registers
 * under {@code csrp}, so both namespaces are accepted — the same rule
 * {@code HarlequinRockBushGen} uses.</p>
 *
 * <p>26.3 note: the original's {@code PlacementSettings#setKeepLiquids(true)} has no counterpart —
 * {@link StructurePlaceSettings} only exposes {@code LiquidSettings}, and the 1.12.2 default was
 * already {@code true} — so the placement keeps 26.3's default {@code APPLY_WATERLOGGING}.</p>
 */
public final class HarlequinScatterCommand {
    public static final String NAME = "harlequin_scatter";

    private static final String[] ROCKS = {
            "harlequin_rock_01", "harlequin_rock_02", "harlequin_rock_03", "harlequin_rock_04",
            "harlequin_rock_05", "harlequin_rock_06", "harlequin_rock_07"
    };
    private static final String[] BUSHES = {"harlequin_bush_00", "harlequin_bush_01", "harlequin_bush_02"};
    private static final String[] TREES = {
            "harlequin_tree_01", "harlequin_tree_02", "harlequin_tree_03", "harlequin_tree_04",
            "harlequin_tree_05", "harlequin_tree_06", "harlequin_tree_07"
    };
    private static final String[] RUINS = {"harlequin_ruin_01", "harlequin_ruin_02", "harlequin_ruin_03"};

    /** {@code CommandHarlequinScatter}: the six accepted type words. */
    private static final String[] TYPES = {"rock", "bush", "tree", "ruin", "both", "all"};
    private static final int DEFAULT_TRIES = 32;
    private static final int DEFAULT_RADIUS = 32;

    /** {@code StructureTemplate#placeInWorld} flags: {@code addBlocksToWorld} used flag 2. */
    private static final int UPDATE_CLIENTS = 2;

    private HarlequinScatterCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (String type : TYPES) {
            LiteralArgumentBuilder<CommandSourceStack> typeNode = Commands.literal(type)
                    .executes(context -> scatter(context.getSource(), type, DEFAULT_TRIES, DEFAULT_RADIUS));
            RequiredArgumentBuilder<CommandSourceStack, Integer> triesNode = Commands.argument("tries",
                    IntegerArgumentType.integer(1, 4096));
            triesNode.executes(context -> scatter(context.getSource(), type,
                    IntegerArgumentType.getInteger(context, "tries"), DEFAULT_RADIUS));
            triesNode.then(Commands.argument("radius", IntegerArgumentType.integer(8, 512))
                    .executes(context -> scatter(context.getSource(), type,
                            IntegerArgumentType.getInteger(context, "tries"),
                            IntegerArgumentType.getInteger(context, "radius"))));
            typeNode.then(triesNode);
            root.then(typeNode);
        }
        return root;
    }

    private static int scatter(CommandSourceStack source, String type, int tries, int radius)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        RandomSource random = level.getRandom();
        BlockPos origin = player.blockPosition();
        int placed = 0;

        for (int i = 0; i < tries; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            BlockPos xz = new BlockPos(origin.getX() + dx, 0, origin.getZ() + dz);
            BlockPos surf = surfaceAt(level, xz);
            if (!isHarlequin(level, surf) || !isGoodGround(level, surf.below()) || isBadTop(level, surf)) {
                continue;
            }
            switch (type) {
                case "rock" -> {
                    if (flatEnough(level, surf, 5, 3)
                            && placeTemplate(level, surf.below(random.nextInt(2)), pick(ROCKS, random), random)) {
                        placed++;
                    }
                }
                case "bush" -> {
                    int count = 1 + random.nextInt(3);
                    for (int k = 0; k < count; k++) {
                        BlockPos around = surf.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
                        BlockPos s2 = surfaceAt(level, around);
                        if (isHarlequin(level, s2) && isGoodGround(level, s2.below()) && !isBadTop(level, s2)
                                && flatEnough(level, s2, 3, 3)
                                && placeTemplate(level, s2, pick(BUSHES, random), random)) {
                            placed++;
                        }
                    }
                }
                case "tree" -> {
                    if (flatEnough(level, surf, 6, 3)
                            && placeTemplate(level, surf, pick(TREES, random), random)) {
                        placed++;
                    }
                }
                case "ruin" -> {
                    if (flatEnough(level, surf, 10, 4)
                            && placeTemplate(level, surf.below(random.nextBoolean() ? 1 : 0),
                                    pick(RUINS, random), random)) {
                        placed++;
                    }
                }
                case "both" -> {
                    if (flatEnough(level, surf, 5, 3)) {
                        if (placeTemplate(level, surf.below(random.nextInt(2)), pick(ROCKS, random), random)) {
                            placed++;
                        }
                        int count = 1 + random.nextInt(2);
                        for (int k = 0; k < count; k++) {
                            BlockPos around = surf.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
                            BlockPos s3 = surfaceAt(level, around);
                            if (isHarlequin(level, s3) && isGoodGround(level, s3.below()) && !isBadTop(level, s3)
                                    && flatEnough(level, s3, 3, 3)
                                    && placeTemplate(level, s3, pick(BUSHES, random), random)) {
                                placed++;
                            }
                        }
                    }
                }
                case "all" -> {
                    if (flatEnough(level, surf, 5, 3)
                            && placeTemplate(level, surf.below(random.nextInt(2)), pick(ROCKS, random), random)) {
                        placed++;
                    }
                    BlockPos treePos = surfaceAt(level, surf.offset(random.nextInt(9) - 4, 0,
                            random.nextInt(9) - 4));
                    if (isHarlequin(level, treePos) && isGoodGround(level, treePos.below())
                            && !isBadTop(level, treePos) && flatEnough(level, treePos, 6, 3)
                            && placeTemplate(level, treePos, pick(TREES, random), random)) {
                        placed++;
                    }
                    BlockPos ruinPos = surfaceAt(level, surf.offset(random.nextInt(17) - 8, 0,
                            random.nextInt(17) - 8));
                    if (isHarlequin(level, ruinPos) && isGoodGround(level, ruinPos.below())
                            && !isBadTop(level, ruinPos) && flatEnough(level, ruinPos, 10, 4)
                            && placeTemplate(level, ruinPos.below(random.nextBoolean() ? 1 : 0),
                                    pick(RUINS, random), random)) {
                        placed++;
                    }
                    int count = 2 + random.nextInt(3);
                    for (int k = 0; k < count; k++) {
                        BlockPos bushPos = surfaceAt(level, surf.offset(random.nextInt(9) - 4, 0,
                                random.nextInt(9) - 4));
                        if (isHarlequin(level, bushPos) && isGoodGround(level, bushPos.below())
                                && !isBadTop(level, bushPos) && flatEnough(level, bushPos, 3, 3)
                                && placeTemplate(level, bushPos, pick(BUSHES, random), random)) {
                            placed++;
                        }
                    }
                }
                default -> throw new IllegalStateException("unreachable type " + type);
            }
        }

        int total = placed;
        source.sendSuccess(() -> Component.literal("Harlequin scatter: placed ~" + total
                + " (type=" + type + ", tries=" + tries + ", radius=" + radius + ")"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static boolean isHarlequin(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(ParasiteBiomeDecorator.HARLEQUIN_BIOME);
    }

    /** {@code CommandHarlequinScatter#isGoodGround} — {@code SAND}, {@code SANDSTONE}, {@code RED_SANDSTONE}, {@code GRASS}, {@code DIRT}, plus the two SRP ground ids. */
    public static boolean isGoodGround(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
            return true;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) {
            return false;
        }
        boolean srpNamespace = Csrp.MODID.equals(id.getNamespace()) || "srparasites".equals(id.getNamespace());
        return srpNamespace && ("harlequinn_grass".equals(id.getPath()) || "harleskinn_block".equals(id.getPath()));
    }

    /** {@code CommandHarlequinScatter#isBadTop} — water, lava or leaves. */
    public static boolean isBadTop(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.WATER || block == Blocks.LAVA) {
            return true;
        }
        return block instanceof LeavesBlock;
    }

    /** {@code CommandHarlequinScatter#flatEnough} — the four radius corners within {@code maxDelta}. */
    static boolean flatEnough(ServerLevel level, BlockPos center, int radius, int maxDelta) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dx = -radius; dx <= radius; dx += radius) {
            for (int dz = -radius; dz <= radius; dz += radius) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, center.getX() + dx, center.getZ() + dz);
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        return max - min <= maxDelta;
    }

    /** {@code CommandHarlequinScatter#surfaceAt} — {@code world.getHeight(xz)} (MOTION_BLOCKING). */
    static BlockPos surfaceAt(ServerLevel level, BlockPos xz) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, xz.getX(), xz.getZ());
        return new BlockPos(xz.getX(), y, xz.getZ());
    }

    /** {@code CommandHarlequinScatter#placeTemplate} — random rotation, random mirror, no entities. */
    public static boolean placeTemplate(ServerLevel level, BlockPos at, String name, RandomSource random) {
        Optional<StructureTemplate> template = level.getStructureTemplateManager()
                .get(Identifier.fromNamespaceAndPath(Csrp.MODID, name));
        if (template.isEmpty()) {
            return false;
        }
        Rotation[] rotations = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180,
                Rotation.COUNTERCLOCKWISE_90};
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotations[random.nextInt(rotations.length)])
                .setMirror(random.nextBoolean() ? Mirror.NONE : Mirror.FRONT_BACK)
                .setIgnoreEntities(true);
        template.get().placeInWorld(level, at, at, settings, random, UPDATE_CLIENTS);
        return true;
    }

    private static String pick(String[] values, RandomSource random) {
        return values[random.nextInt(values.length)];
    }
}
