package alku.csrp.world.gen;

import alku.csrp.world.ParasiteBiomeDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
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
 * Port of the 1.10.9 {@code HarlequinRockBushGen} world generator.
 *
 * <p>For every newly generated Harlequin chunk it places one of seven {@code harlequin_rock_*}
 * templates with a 1-in-10 chance and a 3..6 piece {@code harlequin_bush_*} cluster with a 1-in-8
 * chance.  Every candidate is validated against the original rules: the ground must be sand / red
 * sand / gravel / grass / dirt (or the SRP harlequin ground ids), the top block must not be
 * water/lava/ice/leaves, and the 7-block neighbourhood must stay within a 3-block height delta.</p>
 *
 * <p>The 1.12.2 {@code IWorldGenerator} hook does not exist in 26.3, so this is driven from
 * {@link ParasiteBiomeDecorator} while a Harlequin chunk is being decorated.</p>
 */
public final class HarlequinRockBushGen {
    private static final String[] ROCKS = {
            "harlequin_rock_01", "harlequin_rock_02", "harlequin_rock_03", "harlequin_rock_04",
            "harlequin_rock_05", "harlequin_rock_06", "harlequin_rock_07"
    };
    private static final String[] BUSHES = {"harlequin_bush_00", "harlequin_bush_01", "harlequin_bush_02"};

    private static final int ROCK_CHANCE_PER_CHUNK = 10;
    private static final int BUSH_CLUSTER_CHANCE = 8;
    private static final int BUSH_CLUSTER_MIN = 3;
    private static final int BUSH_CLUSTER_MAX = 6;
    private static final int BUSH_CLUSTER_RADIUS = 7;
    private static final int MAX_SLOPE = 3;

    private HarlequinRockBushGen() {
    }

    /** The 1.12.2 {@code generate(Random, chunkX, chunkZ, World, ...)} body. */
    public static void generate(ServerLevel level, RandomSource random, ChunkPos chunkPos) {
        int baseX = (chunkPos.x() << 4) + 8 + random.nextInt(6) - 3;
        int baseZ = (chunkPos.z() << 4) + 8 + random.nextInt(6) - 3;
        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX, baseZ);
        BlockPos base = new BlockPos(baseX, baseY, baseZ);

        if (!isHarlequin(level, base)) {
            return;
        }

        if (random.nextInt(ROCK_CHANCE_PER_CHUNK) == 0) {
            tryPlaceTemplateSurface(level, base, ROCKS[random.nextInt(ROCKS.length)], random);
        }

        if (random.nextInt(BUSH_CLUSTER_CHANCE) == 0) {
            int count = BUSH_CLUSTER_MIN + random.nextInt(BUSH_CLUSTER_MAX - BUSH_CLUSTER_MIN + 1);
            BlockPos anchor = findSurface(level, base);
            if (anchor != null) {
                for (int i = 0; i < count; i++) {
                    BlockPos around = anchor.offset(random.nextInt(15) - BUSH_CLUSTER_RADIUS, 0,
                            random.nextInt(15) - BUSH_CLUSTER_RADIUS);
                    tryPlaceTemplateSurface(level, around, BUSHES[random.nextInt(BUSHES.length)], random);
                }
            }
        }
    }

    private static boolean isHarlequin(ServerLevel level, BlockPos pos) {
        return ParasiteBiomeDecorator.parasiteBiomeAt(level.getChunkAt(pos), pos)
                == ParasiteBiomeDecorator.HARLEQUIN_BIOME;
    }

    private static void tryPlaceTemplateSurface(ServerLevel level, BlockPos near, String name,
            RandomSource random) {
        BlockPos surface = findSurface(level, near);
        if (surface == null || !flatEnough(level, surface, BUSH_CLUSTER_RADIUS, MAX_SLOPE)) {
            return;
        }
        int sink = name.startsWith("harlequin_rock_") ? random.nextInt(2) : 0;
        BlockPos placeAt = surface.below(sink);
        // There are exactly four horizontal rotations; the enum survived the 26.3 port.
        Rotation[] rotations = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180,
                Rotation.COUNTERCLOCKWISE_90};
        Rotation rotation = rotations[random.nextInt(rotations.length)];
        Mirror mirror = random.nextBoolean() ? Mirror.NONE : Mirror.FRONT_BACK;
        Identifier id = Identifier.fromNamespaceAndPath("csrp", name);
        StructureTemplate template = level.getStructureTemplateManager().get(id).orElse(null);
        if (template == null) {
            return;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(mirror)
                .setIgnoreEntities(true);
        template.placeInWorld(level, placeAt, placeAt, settings, random, 2);
    }

    private static BlockPos findSurface(ServerLevel level, BlockPos xz) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz.getX(), xz.getZ());
        BlockPos pos = new BlockPos(xz.getX(), y, xz.getZ());
        if (!isGoodGround(level, pos.below())) {
            return null;
        }
        return isBadTop(level, pos) ? null : pos;
    }

    private static boolean isGoodGround(ServerLevel level, BlockPos pos) {
        Block block = ParasiteGenContext.get(level, pos).getBlock();
        if (block == Blocks.SAND || block == Blocks.RED_SAND || block == Blocks.GRAVEL
                || block == Blocks.GRASS_BLOCK || block == Blocks.DIRT) {
            return true;
        }
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        String path = id.getPath();
        return ("csrp".equals(id.getNamespace()) || "srparasites".equals(id.getNamespace()))
                && ("harlequinn_grass".equals(path) || "harleskinn_block".equals(path));
    }

    private static boolean isBadTop(ServerLevel level, BlockPos pos) {
        BlockState state = ParasiteGenContext.get(level, pos);
        Block block = state.getBlock();
        if (block == Blocks.WATER || block == Blocks.LAVA || block == Blocks.ICE
                || block == Blocks.FROSTED_ICE) {
            return true;
        }
        return block instanceof LeavesBlock;
    }

    /** Samples the four radius corners, exactly like the 1.10.9 {@code flatEnough}. */
    private static boolean flatEnough(ServerLevel level, BlockPos center, int radius, int maxDelta) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dx = -radius; dx <= radius; dx += radius) {
            for (int dz = -radius; dz <= radius; dz += radius) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        center.getX() + dx, center.getZ() + dz);
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        return max - min <= maxDelta;
    }
}
