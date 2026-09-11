package alku.csrp.world;

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

/** Port of the 1.10.8 {@code WorldGenStructure} template placement helper. */
public final class StructurePlacer {
    private StructurePlacer() {
    }

    public static boolean place(ServerLevel level, ResourceLocation id, BlockPos pos) {
        return place(level, id, pos, level.getRandom());
    }

    public static boolean place(ServerLevel level, ResourceLocation id, BlockPos pos, RandomSource random) {
        Optional<StructureTemplate> optional = level.getStructureManager().get(id);
        if (optional.isEmpty()) {
            return false;
        }
        StructureTemplate template = optional.get();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false);
        return template.placeInWorld(level, pos, pos, settings, random, 2);
    }
}
