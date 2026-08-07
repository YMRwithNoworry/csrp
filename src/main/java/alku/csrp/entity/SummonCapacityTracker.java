package alku.csrp.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class SummonCapacityTracker {
    private final Map<UUID, Integer> trackedSummons = new LinkedHashMap<>();

    int usedCapacity() {
        return trackedSummons.values().stream().mapToInt(Integer::intValue).sum();
    }

    void reserve(UUID entityId, int cost) {
        if (cost > 0) {
            trackedSummons.put(entityId, cost);
        }
    }

    void replace(UUID previousId, UUID replacementId, int fallbackCost) {
        Integer cost = trackedSummons.remove(previousId);
        reserve(replacementId, cost == null ? fallbackCost : cost);
    }

    void release(UUID entityId) {
        trackedSummons.remove(entityId);
    }

    void prune(ServerLevel level) {
        trackedSummons.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getKey());
            return entity == null || !entity.isAlive();
        });
    }

    void save(CompoundTag tag, String key) {
        ListTag summons = new ListTag();
        for (Map.Entry<UUID, Integer> tracked : trackedSummons.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("entity", tracked.getKey());
            entry.putInt("cost", tracked.getValue());
            summons.add(entry);
        }
        tag.put(key, summons);
    }

    void load(CompoundTag tag, String key) {
        trackedSummons.clear();
        ListTag summons = tag.getList(key, Tag.TAG_COMPOUND);
        for (Tag value : summons) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("entity") && entry.getInt("cost") > 0) {
                trackedSummons.put(entry.getUUID("entity"), entry.getInt("cost"));
            }
        }
    }
}
