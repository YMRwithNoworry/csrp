package alku.csrp.compendium;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;

public final class CompendiumProgress {
    private final Map<String, Integer> kills = new LinkedHashMap<>();
    private final Set<String> blocks = new LinkedHashSet<>();
    private final Set<String> celestials = new LinkedHashSet<>();
    private final Set<String> effects = new LinkedHashSet<>();
    private float damageToParasites;
    private float damageFromParasites;
    private int deathsByParasites;
    private CompoundTag unlockSnapshot;

    public static CompendiumProgress load(CompoundTag tag) {
        CompendiumProgress progress = new CompendiumProgress();
        CompoundTag killTag = tag.getCompound("kills");
        for (String key : killTag.getAllKeys()) {
            progress.kills.put(key, killTag.getInt(key));
        }
        readSet(tag.getCompound("blocks"), progress.blocks);
        readSet(tag.getCompound("celestials"), progress.celestials);
        readSet(tag.getCompound("effects"), progress.effects);
        progress.damageToParasites = tag.getFloat("damage_to_parasites");
        progress.damageFromParasites = tag.getFloat("damage_from_parasites");
        progress.deathsByParasites = tag.getInt("deaths_by_parasites");
        if (tag.contains("unlock_snapshot")) {
            progress.unlockSnapshot = tag.getCompound("unlock_snapshot").copy();
        }
        return progress;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        CompoundTag killTag = new CompoundTag();
        kills.forEach(killTag::putInt);
        tag.put("kills", killTag);
        tag.put("blocks", writeSet(blocks));
        tag.put("celestials", writeSet(celestials));
        tag.put("effects", writeSet(effects));
        tag.putFloat("damage_to_parasites", damageToParasites);
        tag.putFloat("damage_from_parasites", damageFromParasites);
        tag.putInt("deaths_by_parasites", deathsByParasites);
        if (unlockSnapshot != null) {
            tag.put("unlock_snapshot", unlockSnapshot.copy());
        }
        return tag;
    }

    public int addKill(String id) {
        return kills.merge(id, 1, Integer::sum);
    }

    public boolean unlockBlock(String id) {
        return blocks.add(id);
    }

    public boolean unlockCelestial(String id) {
        return celestials.add(id);
    }

    public boolean unlockEffect(String id) {
        return effects.add(id);
    }

    public void addDamageToParasites(float amount) {
        damageToParasites += Math.max(0.0F, amount);
    }

    public void addDamageFromParasites(float amount) {
        damageFromParasites += Math.max(0.0F, amount);
    }

    public void addDeathByParasites() {
        deathsByParasites++;
    }

    public Map<String, Integer> kills() {
        return Collections.unmodifiableMap(kills);
    }

    public Set<String> blocks() {
        return Collections.unmodifiableSet(blocks);
    }

    public Set<String> celestials() {
        return Collections.unmodifiableSet(celestials);
    }

    public Set<String> effects() {
        return Collections.unmodifiableSet(effects);
    }

    public float damageToParasites() {
        return damageToParasites;
    }

    public float damageFromParasites() {
        return damageFromParasites;
    }

    public int deathsByParasites() {
        return deathsByParasites;
    }

    public void clear() {
        kills.clear();
        blocks.clear();
        celestials.clear();
        effects.clear();
        damageToParasites = 0.0F;
        damageFromParasites = 0.0F;
        deathsByParasites = 0;
    }

    public void clearBestiaryStats() {
        kills.clear();
    }

    public void clearCombatStats() {
        damageToParasites = 0.0F;
        damageFromParasites = 0.0F;
        deathsByParasites = 0;
    }

    public void clearBlocks() {
        blocks.clear();
    }

    public void clearCelestials() {
        celestials.clear();
    }

    public void clearEffects() {
        effects.clear();
    }

    public void takeUnlockSnapshot() {
        if (unlockSnapshot != null) {
            return;
        }
        unlockSnapshot = save();
        unlockSnapshot.remove("unlock_snapshot");
    }

    public boolean restoreUnlockSnapshot() {
        if (unlockSnapshot == null) {
            return false;
        }
        CompendiumProgress restored = load(unlockSnapshot);
        kills.clear();
        kills.putAll(restored.kills);
        blocks.clear();
        blocks.addAll(restored.blocks);
        celestials.clear();
        celestials.addAll(restored.celestials);
        effects.clear();
        effects.addAll(restored.effects);
        damageToParasites = restored.damageToParasites;
        damageFromParasites = restored.damageFromParasites;
        deathsByParasites = restored.deathsByParasites;
        unlockSnapshot = null;
        return true;
    }

    public void unlockAllMobs(Iterable<String> ids) {
        ids.forEach(id -> kills.put(id, 1000));
    }

    public void unlockAllBlocks(Iterable<String> ids) {
        ids.forEach(blocks::add);
    }

    public void unlockAllEffects(Iterable<String> ids) {
        ids.forEach(effects::add);
    }

    public void unlockAllCelestials() {
        celestials.addAll(CompendiumCatalog.CELESTIALS);
    }

    private static CompoundTag writeSet(Set<String> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach(value -> tag.putBoolean(value, true));
        return tag;
    }

    private static void readSet(CompoundTag tag, Set<String> output) {
        output.addAll(tag.getAllKeys());
    }
}
