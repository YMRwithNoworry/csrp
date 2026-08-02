package alku.csrp.celestial;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class CelestialWorldData extends SavedData {
    private static final String DATA_NAME = "csrp_celestial_events";
    private static final Factory<CelestialWorldData> FACTORY =
            new Factory<>(CelestialWorldData::new, CelestialWorldData::load);

    private long nightIndex = Long.MIN_VALUE;
    private final Set<String> active = new LinkedHashSet<>();
    private final Set<String> forced = new LinkedHashSet<>();
    private long darkDaysStartTime = -1;
    private long darkDaysEndTime = -1;
    private boolean darkDaysEndingSoundPlayed;
    private long darkDaysLastRollDay = Long.MIN_VALUE;
    private long lastEffectNightIndex = Long.MIN_VALUE;

    public static CelestialWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static CelestialWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        CelestialWorldData data = new CelestialWorldData();
        data.nightIndex = tag.contains("night_index") ? tag.getLong("night_index") : Long.MIN_VALUE;
        readSet(tag, "active", data.active);
        readSet(tag, "forced", data.forced);
        data.darkDaysStartTime = tag.contains("dark_days_start_time")
                ? tag.getLong("dark_days_start_time") : -1;
        data.darkDaysEndTime = tag.contains("dark_days_end_time")
                ? tag.getLong("dark_days_end_time") : -1;
        data.darkDaysEndingSoundPlayed = tag.getBoolean("dark_days_ending_sound_played");
        data.darkDaysLastRollDay = tag.contains("dark_days_last_roll_day")
                ? tag.getLong("dark_days_last_roll_day") : Long.MIN_VALUE;
        data.lastEffectNightIndex = tag.contains("last_effect_night_index")
                ? tag.getLong("last_effect_night_index") : Long.MIN_VALUE;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("night_index", nightIndex);
        writeSet(tag, "active", active);
        writeSet(tag, "forced", forced);
        tag.putLong("dark_days_start_time", darkDaysStartTime);
        tag.putLong("dark_days_end_time", darkDaysEndTime);
        tag.putBoolean("dark_days_ending_sound_played", darkDaysEndingSoundPlayed);
        tag.putLong("dark_days_last_roll_day", darkDaysLastRollDay);
        tag.putLong("last_effect_night_index", lastEffectNightIndex);
        return tag;
    }

    public long nightIndex() { return nightIndex; }
    public void nightIndex(long value) { nightIndex = value; setDirty(); }
    public Set<String> active() { return Collections.unmodifiableSet(active); }
    public Set<String> forced() { return Collections.unmodifiableSet(forced); }
    Set<String> mutableActive() { return active; }
    Set<String> mutableForced() { return forced; }
    public long darkDaysStartTime() { return darkDaysStartTime; }
    public void darkDaysStartTime(long value) { darkDaysStartTime = value; setDirty(); }
    public long darkDaysEndTime() { return darkDaysEndTime; }
    public void darkDaysEndTime(long value) { darkDaysEndTime = value; setDirty(); }
    public boolean darkDaysEndingSoundPlayed() { return darkDaysEndingSoundPlayed; }
    public void darkDaysEndingSoundPlayed(boolean value) { darkDaysEndingSoundPlayed = value; setDirty(); }
    public long darkDaysLastRollDay() { return darkDaysLastRollDay; }
    public void darkDaysLastRollDay(long value) { darkDaysLastRollDay = value; setDirty(); }
    public long lastEffectNightIndex() { return lastEffectNightIndex; }
    public void lastEffectNightIndex(long value) { lastEffectNightIndex = value; setDirty(); }
    public void changed() { setDirty(); }

    private static void readSet(CompoundTag tag, String key, Set<String> target) {
        ListTag values = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < values.size(); i++) target.add(values.getString(i));
    }

    private static void writeSet(CompoundTag tag, String key, Set<String> values) {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        tag.put(key, list);
    }
}
