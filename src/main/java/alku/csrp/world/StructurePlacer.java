package alku.csrp.world;

import alku.csrp.Csrp;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Port of the 1.10.8 {@code WorldGenStructure} template placement helper. */
public final class StructurePlacer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Csrp.MODID + "/structures");

    /** Templates already reported as unusable, so a broken file cannot spam the log. */
    private static final java.util.Set<Identifier> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private StructurePlacer() {
    }

    public static boolean place(ServerLevel level, Identifier id, BlockPos pos) {
        return place(level, id, pos, level.getRandom());
    }

    public static boolean place(ServerLevel level, Identifier id, BlockPos pos, RandomSource random) {
        return place(level, id, pos, random, null, Rotation.NONE);
    }

    /**
     * Places a template at an arbitrary rotation while keeping the block that maps to
     * {@code anchor} at {@code placementPos}.  The 1.10.9 deadhead trees are authored around a
     * sapling anchor and the original used {@code Template#transformedBlockPos} to line that anchor
     * up; this is the modern equivalent, expressed without touching
     * {@code StructureTemplate#placeInWorld}.
     *
     * @param anchor   the template-local position that should end up on {@code placementPos};
     *                 {@code null} places the template origin at {@code placementPos}
     * @param rotation template rotation applied before the anchor is aligned
     */
    public static boolean place(ServerLevel level, Identifier id, BlockPos placementPos, RandomSource random,
            BlockPos anchor, Rotation rotation) {
        Optional<StructureTemplate> optional = level.getStructureTemplateManager().get(id);
        if (optional.isEmpty()) {
            reportOnce(id, "template not found; expected it at data/" + id.getNamespace()
                    + "/structure/" + id.getPath() + ".nbt");
            return false;
        }
        StructureTemplate template = optional.get();
        if (template.getSize().getX() <= 0 || template.getSize().getY() <= 0 || template.getSize().getZ() <= 0) {
            // A 0x0x0 template means the NBT did not use a LIST<INT> for "size"; the file
            // loads but places nothing, which used to fail silently.
            reportOnce(id, "template has an empty size " + template.getSize()
                    + "; its NBT \"size\" must be a LIST<INT>");
            return false;
        }
        BlockPos origin = placementPos;
        if (anchor != null) {
            origin = placementPos.subtract(rotateAnchor(anchor, rotation));
        }
        BlockState state = level.getBlockState(origin);
        level.sendBlockUpdated(origin, state, state, 3);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setIgnoreEntities(false);
        boolean placed = template.placeInWorld(level, origin, origin, settings, random, 2);
        if (!placed) {
            reportOnce(id, "placement was rejected at " + origin.toShortString());
        }
        return placed;
    }

    /**
     * Mirrors vanilla's internal position transform for the four horizontal rotations (the only
     * ones the deadhead trees use).  Kept local so the behaviour stays verifiable without
     * depending on a package-private vanilla helper.
     */
    private static BlockPos rotateAnchor(BlockPos anchor, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(-anchor.getZ(), anchor.getY(), anchor.getX());
            case CLOCKWISE_180 -> new BlockPos(-anchor.getX(), anchor.getY(), -anchor.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(anchor.getZ(), anchor.getY(), -anchor.getX());
            default -> anchor;
        };
    }

    private static void reportOnce(Identifier id, String reason) {
        if (REPORTED.add(id)) {
            LOGGER.warn("Could not place structure {}: {}", id, reason);
        }
    }
}
