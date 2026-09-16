package alku.csrp.compendium;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class CompendiumSavedData extends SavedData {
    private static final String DATA_NAME = "csrp_compendium";
    private static final Codec<CompendiumSavedData> CODEC =
            CompoundTag.CODEC.xmap(CompendiumSavedData::load, CompendiumSavedData::save);
    private static final SavedDataType<CompendiumSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("csrp", DATA_NAME), CompendiumSavedData::new, CODEC);
    private final Map<UUID, CompendiumProgress> players = new LinkedHashMap<>();

    public static CompendiumSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private static CompendiumSavedData load(CompoundTag tag) {
        CompendiumSavedData data = new CompendiumSavedData();
        CompoundTag playersTag = tag.getCompoundOrEmpty("players");
        for (String key : playersTag.keySet()) {
            try {
                data.players.put(UUID.fromString(key), CompendiumProgress.load(playersTag.getCompoundOrEmpty(key)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy entries instead of invalidating the whole world save.
            }
        }
        return data;
    }

    private CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag playersTag = new CompoundTag();
        players.forEach((uuid, progress) -> playersTag.put(uuid.toString(), progress.save()));
        tag.put("players", playersTag);
        return tag;
    }

    public CompendiumProgress progress(UUID player) {
        return players.computeIfAbsent(player, ignored -> new CompendiumProgress());
    }

    public void changed() {
        setDirty();
    }
}
