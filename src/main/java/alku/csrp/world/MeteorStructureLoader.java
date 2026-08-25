package alku.csrp.world;

import alku.csrp.Csrp;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/** Loads the original 1.12 meteor structures while translating their legacy registry ids. */
final class MeteorStructureLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LEGACY_NAMESPACE = "srparasites:";
    private static final Map<String, String> BLOCK_RENAMES = Map.ofEntries(
            Map.entry("dermoid_cyst", "gluttonous_cyst"),
            Map.entry("infestedbush", "residue_plants"),
            Map.entry("lipoma_mass", "biomass_block"),
            Map.entry("parasiterubble", "infestedrubble"),
            Map.entry("parasiterubbledense", "residue_bricks"),
            Map.entry("parasiterubbledense_colony_wall", "residue_wall"),
            Map.entry("parasiterubbledense_wallstairs", "residue_stairs"),
            Map.entry("parasiterubble_metal_wall", "residue_wall"),
            Map.entry("parasiterubble_stonestairs", "infested_stone_stairs"),
            Map.entry("parasiterubbleslabhalf", "infested_stone_slab"),
            Map.entry("parasitestain", "infestedstain"),
            Map.entry("parasitestain_flesh_wall", "infestedstain_wall"),
            Map.entry("parasitestain_fleshstairs", "cooked_flesh_stairs"),
            Map.entry("parasitestain_mudstairs", "residue_stairs"),
            Map.entry("parasitestainslabdouble", "infestedstain"),
            Map.entry("parasitestainslabhalf", "infested_dirt_slab"),
            Map.entry("parasitic_compressed_colony_stone_slab", "residue_brick_slab")
    );
    private MeteorStructureLoader() {
    }

    public static boolean place(ServerLevel level, String name, BlockPos origin, RandomSource random) {
        StructureTemplate template = load(level, name);
        if (template == null) {
            return false;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(false)
                .setKeepLiquids(false)
                .setRandom(random);
        return template.placeInWorld(level, origin, origin, settings, random, Block.UPDATE_CLIENTS);
    }

    private static StructureTemplate load(ServerLevel level, String name) {
        ResourceLocation location = new ResourceLocation(Csrp.MODID, "structures/" + name + ".nbt");
        Optional<Resource> resource = level.getServer().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            LOGGER.error("Missing meteor structure {}", location);
            return null;
        }
        try (InputStream input = resource.get().open()) {
            CompoundTag root = NbtIo.readCompressed(input);
            rewriteLegacyIds(root);
            StructureTemplate template = new StructureTemplate();
            template.load(level.holderLookup(Registries.BLOCK), root);
            return template;
        } catch (IOException exception) {
            LOGGER.error("Unable to load meteor structure {}", location, exception);
            return null;
        }
    }

    private static void rewriteLegacyIds(CompoundTag compound) {
        for (String key : compound.getAllKeys().toArray(String[]::new)) {
            Tag child = compound.get(key);
            if (child instanceof StringTag stringTag) {
                String value = stringTag.getAsString();
                if (key.equals("id") && value.equals(LEGACY_NAMESPACE + "dermoid_cyst")) {
                    compound.putString(key, Csrp.MODID + ":parasitic_cyst");
                } else {
                    compound.putString(key, rewriteId(value));
                }
            } else if (child instanceof CompoundTag nested) {
                rewriteLegacyIds(nested);
            } else if (child instanceof ListTag list) {
                rewriteLegacyIds(list);
            }
        }
    }

    private static void rewriteLegacyIds(ListTag list) {
        for (int index = 0; index < list.size(); index++) {
            Tag child = list.get(index);
            if (child instanceof StringTag stringTag) {
                list.set(index, StringTag.valueOf(rewriteId(stringTag.getAsString())));
            } else if (child instanceof CompoundTag nested) {
                rewriteLegacyIds(nested);
            } else if (child instanceof ListTag nested) {
                rewriteLegacyIds(nested);
            }
        }
    }

    private static String rewriteId(String value) {
        if (!value.startsWith(LEGACY_NAMESPACE)) {
            return value;
        }
        String path = value.substring(LEGACY_NAMESPACE.length());
        return Csrp.MODID + ":" + BLOCK_RENAMES.getOrDefault(path, path);
    }
}
