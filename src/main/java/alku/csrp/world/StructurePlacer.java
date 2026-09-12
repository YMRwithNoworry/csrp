package alku.csrp.world;

import alku.csrp.Csrp;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
    private static final java.util.Set<ResourceLocation> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private StructurePlacer() {
    }

    public static boolean place(ServerLevel level, ResourceLocation id, BlockPos pos) {
        return place(level, id, pos, level.getRandom());
    }

    public static boolean place(ServerLevel level, ResourceLocation id, BlockPos pos, RandomSource random) {
        Optional<StructureTemplate> optional = level.getStructureManager().get(id);
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
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false);
        boolean placed = template.placeInWorld(level, pos, pos, settings, random, 2);
        if (!placed) {
            reportOnce(id, "placement was rejected at " + pos.toShortString());
        }
        return placed;
    }

    private static void reportOnce(ResourceLocation id, String reason) {
        if (REPORTED.add(id)) {
            LOGGER.warn("Could not place structure {}: {}", id, reason);
        }
    }
}
