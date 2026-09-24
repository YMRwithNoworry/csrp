package alku.csrp.world;

import alku.csrp.Csrp;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in diagnostic that validates every shipped structure template against the live
 * registry. Structures fail silently in the game -- a template whose NBT uses the wrong
 * tag types loads as 0x0x0, and a palette entry naming a block that no longer exists
 * turns into air -- so run the server with
 *
 * <pre>-Dcsrp.structureCheck=true</pre>
 *
 * to have each template's size and any unresolvable palette block reported in the log.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class StructureTemplateDiagnostics {
    private static final String ENABLE_PROPERTY = "csrp.structureCheck";
    private static final Logger LOGGER = LoggerFactory.getLogger(Csrp.MODID + "/structures");

    private StructureTemplateDiagnostics() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) {
            return;
        }
        MinecraftServer server = event.getServer();
        var level = server.overworld();
        int unusable = 0;
        int checked = 0;
        List<String> unknownBlocks = new ArrayList<>();

        for (Identifier id : level.getStructureManager().listTemplates().toList()) {
            if (!id.getNamespace().equals(Csrp.MODID)) {
                continue;
            }
            checked++;
            StructureTemplate template = level.getStructureManager().get(id).orElse(null);
            if (template == null) {
                LOGGER.error("CSRPCHECK {} -> template could not be loaded", id);
                unusable++;
            } else {
                var size = template.getSize();
                if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
                    LOGGER.error("CSRPCHECK {} -> empty size {}; the NBT \"size\" must be a LIST<INT>",
                            id, size);
                    unusable++;
                } else {
                    LOGGER.info("CSRPCHECK {} -> size {}x{}x{}", id,
                            size.getX(), size.getY(), size.getZ());
                }
            }
            unknownBlocks.addAll(unresolvedPaletteBlocks(id));
        }

        LOGGER.info("CSRPCHECK palette blocks missing from the registry: {}",
                unknownBlocks.isEmpty() ? "NONE" : unknownBlocks);
        LOGGER.info("CSRPCHECK finished: {} unusable templates out of {}", unusable, checked);
    }

    private static List<String> unresolvedPaletteBlocks(Identifier id) {
        List<String> unknown = new ArrayList<>();
        String path = "data/" + id.getNamespace() + "/structure/" + id.getPath() + ".nbt";
        try (InputStream stream = StructureTemplateDiagnostics.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                unknown.add(id + " (resource " + path + " missing)");
                return unknown;
            }
            CompoundTag tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            ListTag palette = tag.getListOrEmpty("palette");
            for (int i = 0; i < palette.size(); i++) {
                String blockName = palette.getCompoundOrEmpty(i).getStringOr("Name", "");
                Identifier blockId = Identifier.tryParse(blockName);
                if (blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId)) {
                    unknown.add(id.getPath() + " -> " + blockName);
                }
            }
        } catch (Exception error) {
            unknown.add(id + " (NBT read failed: " + error + ")");
        }
        return unknown;
    }
}
