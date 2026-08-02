package alku.csrp.compendium;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class CompendiumSavedData extends SavedData {
    private static final String DATA_NAME = "csrp_compendium";
    private static final Factory<CompendiumSavedData> FACTORY =
            new Factory<>(CompendiumSavedData::new, CompendiumSavedData::load);
    private final Map<UUID, CompendiumProgress> players = new LinkedHashMap<>();

    public static CompendiumSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static CompendiumSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompendiumSavedData data = new CompendiumSavedData();
        CompoundTag playersTag = tag.getCompound("players");
        for (String key : playersTag.getAllKeys()) {
            try {
                data.players.put(UUID.fromString(key), CompendiumProgress.load(playersTag.getCompound(key)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy entries instead of invalidating the whole world save.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
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
