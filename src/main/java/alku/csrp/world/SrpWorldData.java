package alku.csrp.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class SrpWorldData extends SavedData {
    private static final String DATA_NAME = "csrp_world_data";
    private static final Factory<SrpWorldData> FACTORY = new Factory<>(SrpWorldData::new, SrpWorldData::load);

    private boolean initialized;
    private int evolutionPhase = -1;
    private int evolutionPoints = -300;
    private long cooldownEnd;
    private boolean canGain = true;
    private boolean canLose = true;
    private int generation;
    private int generationTicks;
    private int ubiquitousDevelopment;
    private final List<Integer> lockedParasites = new ArrayList<>();
    private final List<NodeEntry> nodes = new ArrayList<>();
    private final List<ColonyEntry> colonies = new ArrayList<>();
    private final List<VectorEntry> vectors = new ArrayList<>();
    private final List<DislodgmentCode> dislodgmentCodes = new ArrayList<>();
    private final Map<String, Integer> globalAdaptations = new LinkedHashMap<>();

    public static SrpWorldData get(ServerLevel level) {
        SrpWorldData data = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        if (!data.initialized) {
            data.initialize(level);
        }
        return data;
    }

    private static SrpWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        SrpWorldData data = new SrpWorldData();
        data.initialized = tag.contains("evolution_phase");
        if (tag.contains("evolution_phase")) {
            data.evolutionPhase = tag.getInt("evolution_phase");
        }
        data.evolutionPoints = tag.getInt("evolution_points");
        data.cooldownEnd = tag.getLong("cooldown_end");
        data.canGain = !tag.contains("can_gain") || tag.getBoolean("can_gain");
        data.canLose = !tag.contains("can_lose") || tag.getBoolean("can_lose");
        data.generation = tag.getInt("generation");
        data.generationTicks = tag.getInt("generation_ticks");
        data.ubiquitousDevelopment = tag.getInt("ubiquitous_development");

        for (int id : tag.getIntArray("locked_parasites")) {
            data.lockedParasites.add(id);
        }
        readNodes(tag, data.nodes);
        readColonies(tag, data.colonies);
        readVectors(tag, data.vectors);
        readDislodgmentCodes(tag, data.dislodgmentCodes);
        readGlobalAdaptations(tag, data.globalAdaptations);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("initialized", initialized);
        tag.putInt("evolution_phase", evolutionPhase);
        tag.putInt("evolution_points", evolutionPoints);
        tag.putLong("cooldown_end", cooldownEnd);
        tag.putBoolean("can_gain", canGain);
        tag.putBoolean("can_lose", canLose);
        tag.putInt("generation", generation);
        tag.putInt("generation_ticks", generationTicks);
        tag.putInt("ubiquitous_development", ubiquitousDevelopment);
        tag.putIntArray("locked_parasites", lockedParasites);
        writeNodes(tag, nodes);
        writeColonies(tag, colonies);
        writeVectors(tag, vectors);
        writeDislodgmentCodes(tag, dislodgmentCodes);
        writeGlobalAdaptations(tag, globalAdaptations);
        return tag;
    }

    public int evolutionPhase() {
        return evolutionPhase;
    }

    public void forceEvolutionPhase(ServerLevel level, int phase) {
        int previous = evolutionPhase;
        evolutionPhase = Math.max(-2, Math.min(10, phase));
        evolutionPoints = EvolutionSystem.thresholdForPhase(evolutionPhase);
        cooldownEnd = level.getGameTime() + EvolutionSystem.cooldownSecondsForPhase(evolutionPhase) * 20L;
        if (previous != evolutionPhase) {
            EvolutionSystem.announcePhaseChange(level, previous, evolutionPhase);
        }
        setDirty();
    }

    public int evolutionPoints() {
        return evolutionPoints;
    }

    public boolean addEvolutionPoints(ServerLevel level, int points, boolean bypassCooldown) {
        if ((points > 0 && !canGain) || (points < 0 && !canLose) || evolutionPhase == -2
                || (points < 0 && evolutionPhase < 0) || (!bypassCooldown && cooldown(level) > 0)) {
            return false;
        }

        long changed = (long) evolutionPoints + points;
        if (evolutionPhase >= 0) {
            changed = Math.max(0L, changed);
        }
        evolutionPoints = (int) Math.max(Integer.MIN_VALUE,
                Math.min(EvolutionSystem.MAX_EVOLUTION_POINTS, changed));

        int previous = evolutionPhase;
        evolutionPhase = EvolutionSystem.phaseForPoints(evolutionPoints);
        if (previous != evolutionPhase) {
            cooldownEnd = level.getGameTime() + EvolutionSystem.cooldownSecondsForPhase(evolutionPhase) * 20L;
            EvolutionSystem.announcePhaseChange(level, previous, evolutionPhase);
        }
        setDirty();
        return true;
    }

    public int cooldown(ServerLevel level) {
        long ticks = Math.max(0L, cooldownEnd - level.getGameTime());
        return (int) ((ticks + 19L) / 20L);
    }

    public void setCooldown(ServerLevel level, int seconds) {
        cooldownEnd = level.getGameTime() + seconds * 20L;
        setDirty();
    }

    public void addCooldown(ServerLevel level, int seconds) {
        cooldownEnd = Math.max(cooldownEnd, level.getGameTime()) + seconds * 20L;
        setDirty();
    }

    public boolean canGain() {
        return canGain;
    }

    public void setCanGain(boolean value) {
        canGain = value;
        setDirty();
    }

    public boolean canLose() {
        return canLose;
    }

    public void setCanLose(boolean value) {
        canLose = value;
        setDirty();
    }

    public int generation() {
        return generation;
    }

    public void setGeneration(int value) {
        generation = Math.max(0, Math.min(5, value));
        generationTicks = 0;
        setDirty();
    }

    public int generationTicks() {
        return generationTicks;
    }

    public void addGenerationTicks(int ticks) {
        long next = (long) generationTicks + ticks;
        generationTicks = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, next));
        setDirty();
    }

    public void tickGeneration(ServerLevel level, int ticks) {
        if (generation >= 5) {
            return;
        }
        generationTicks = Math.max(0, generationTicks + ticks);
        int needed = EvolutionSystem.generationNeededTicks(generation, evolutionPhase);
        if (needed > 0 && generationTicks >= needed) {
            generation++;
            generationTicks = 0;
        }
        setDirty();
    }

    public int ubiquitousDevelopmentOverride() {
        return ubiquitousDevelopment;
    }

    public void setUbiquitousDevelopment(int level) {
        ubiquitousDevelopment = Math.max(0, Math.min(4, level));
        setDirty();
    }

    public List<Integer> lockedParasites() {
        return Collections.unmodifiableList(lockedParasites);
    }

    public void resetLockedParasites() {
        lockedParasites.clear();
        setDirty();
    }

    public List<NodeEntry> nodes() {
        return Collections.unmodifiableList(nodes);
    }

    public void setNode(BlockPos pos, int age, int type) {
        nodes.removeIf(entry -> entry.pos().equals(pos));
        nodes.add(new NodeEntry(pos.immutable(), age, type));
        setDirty();
    }

    public void updateNode(BlockPos pos, int age, int type) {
        setNode(pos, Math.max(1, age), Math.max(1, Math.min(4, type)));
    }

    public boolean removeNode(BlockPos pos) {
        boolean removed = nodes.removeIf(entry -> entry.pos().equals(pos));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void clearNodes() {
        nodes.clear();
        setDirty();
    }

    public List<ColonyEntry> colonies() {
        return Collections.unmodifiableList(colonies);
    }

    public void setColony(BlockPos pos) {
        colonies.removeIf(entry -> entry.pos().equals(pos));
        colonies.add(new ColonyEntry(pos.immutable(), 1));
        setDirty();
    }

    public void updateColony(BlockPos pos, int points) {
        colonies.removeIf(entry -> entry.pos().equals(pos));
        colonies.add(new ColonyEntry(pos.immutable(), Math.max(1, Math.min(100, points))));
        setDirty();
    }

    public boolean removeColony(BlockPos pos) {
        boolean removed = colonies.removeIf(entry -> entry.pos().equals(pos));
        if (removed) {
            globalAdaptations.clear();
            setDirty();
        }
        return removed;
    }

    void rollbackColony(BlockPos pos) {
        if (colonies.removeIf(entry -> entry.pos().equals(pos))) {
            setDirty();
        }
    }

    public void clearColonies() {
        colonies.clear();
        globalAdaptations.clear();
        setDirty();
    }

    public int totalColonyPoints() {
        return Math.min(100_000, colonies.stream().mapToInt(ColonyEntry::points).sum());
    }

    public ColonyEntry nearestColonyInEffectRange(BlockPos pos) {
        ColonyEntry closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ColonyEntry entry : colonies) {
            int radius = 300 + entry.points() * 40;
            double distance = entry.pos().distSqr(pos);
            if (distance <= (double) radius * radius && distance < closestDistance) {
                closest = entry;
                closestDistance = distance;
            }
        }
        return closest;
    }

    public Map<String, Integer> globalAdaptations() {
        return Collections.unmodifiableMap(globalAdaptations);
    }

    public void addGlobalResistance(String damage) {
        if (damage == null || damage.isBlank()) {
            return;
        }
        globalAdaptations.merge(damage, 1, Integer::sum);
        setDirty();
    }

    public GlobalAdaptation mostCommonGlobalAdaptation() {
        String damage = null;
        int points = 0;
        for (Map.Entry<String, Integer> entry : globalAdaptations.entrySet()) {
            if (entry.getValue() > points) {
                damage = entry.getKey();
                points = entry.getValue();
            }
        }
        return new GlobalAdaptation(damage, points);
    }

    public void resetGlobalAdaptation() {
        globalAdaptations.clear();
        setDirty();
    }

    public List<VectorEntry> vectors() {
        return Collections.unmodifiableList(vectors);
    }

    public void setVector(BlockPos pos, int health, int radius) {
        vectors.removeIf(entry -> entry.pos().equals(pos));
        vectors.add(new VectorEntry(pos.immutable(), health, radius));
        setDirty();
    }

    public void updateVector(BlockPos pos, int health, int radius) {
        setVector(pos, Math.max(1, health), Math.max(1, radius));
    }

    public boolean removeVector(BlockPos pos) {
        boolean removed = vectors.removeIf(entry -> entry.pos().equals(pos));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void clearVectors() {
        vectors.clear();
        setDirty();
    }

    public List<DislodgmentCode> activeDislodgmentCodes(ServerLevel level) {
        boolean removed = dislodgmentCodes.removeIf(code -> code.expiresAt() <= level.getGameTime());
        if (removed) {
            setDirty();
        }
        return Collections.unmodifiableList(dislodgmentCodes);
    }

    public boolean setDislodgmentCode(ServerLevel level, int code, int value, int duration) {
        activeDislodgmentCodes(level);
        if (code < 0 || code >= 30 || value < 1 || value > 6 || duration < 1
                || dislodgmentCodes.stream().anyMatch(entry -> entry.code() == code)) {
            return false;
        }
        dislodgmentCodes.add(new DislodgmentCode(code, value, level.getGameTime() + duration));
        setDirty();
        return true;
    }

    public void clearDislodgmentCodes() {
        dislodgmentCodes.clear();
        setDirty();
    }

    public void reset(ServerLevel level) {
        EvolutionSystem.InitialProgress initial = EvolutionSystem.initialProgress(level);
        initialized = true;
        evolutionPhase = initial.phase();
        evolutionPoints = initial.points();
        cooldownEnd = 0L;
        canGain = true;
        canLose = true;
        generation = 0;
        generationTicks = 0;
        ubiquitousDevelopment = 0;
        lockedParasites.clear();
        nodes.clear();
        colonies.clear();
        vectors.clear();
        dislodgmentCodes.clear();
        globalAdaptations.clear();
        setDirty();
    }

    private void initialize(ServerLevel level) {
        EvolutionSystem.InitialProgress initial = EvolutionSystem.initialProgress(level);
        initialized = true;
        evolutionPhase = initial.phase();
        evolutionPoints = initial.points();
        generation = 0;
        generationTicks = 0;
        cooldownEnd = 0L;
        setDirty();
    }

    private static void writeNodes(CompoundTag tag, List<NodeEntry> entries) {
        tag.putLongArray("node_positions", entries.stream().mapToLong(entry -> entry.pos().asLong()).toArray());
        tag.putIntArray("node_ages", entries.stream().mapToInt(NodeEntry::age).toArray());
        tag.putIntArray("node_types", entries.stream().mapToInt(NodeEntry::type).toArray());
    }

    private static void readNodes(CompoundTag tag, List<NodeEntry> output) {
        long[] positions = tag.getLongArray("node_positions");
        int[] ages = tag.getIntArray("node_ages");
        int[] types = tag.getIntArray("node_types");
        for (int i = 0; i < Math.min(positions.length, Math.min(ages.length, types.length)); i++) {
            output.add(new NodeEntry(BlockPos.of(positions[i]), ages[i], types[i]));
        }
    }

    private static void writeColonies(CompoundTag tag, List<ColonyEntry> entries) {
        tag.putLongArray("colony_positions", entries.stream().mapToLong(entry -> entry.pos().asLong()).toArray());
        tag.putIntArray("colony_points", entries.stream().mapToInt(ColonyEntry::points).toArray());
    }

    private static void readColonies(CompoundTag tag, List<ColonyEntry> output) {
        long[] positions = tag.getLongArray("colony_positions");
        int[] points = tag.getIntArray("colony_points");
        for (int i = 0; i < Math.min(positions.length, points.length); i++) {
            output.add(new ColonyEntry(BlockPos.of(positions[i]), points[i]));
        }
    }

    private static void writeVectors(CompoundTag tag, List<VectorEntry> entries) {
        tag.putLongArray("vector_positions", entries.stream().mapToLong(entry -> entry.pos().asLong()).toArray());
        tag.putIntArray("vector_health", entries.stream().mapToInt(VectorEntry::health).toArray());
        tag.putIntArray("vector_radius", entries.stream().mapToInt(VectorEntry::radius).toArray());
    }

    private static void readVectors(CompoundTag tag, List<VectorEntry> output) {
        long[] positions = tag.getLongArray("vector_positions");
        int[] health = tag.getIntArray("vector_health");
        int[] radius = tag.getIntArray("vector_radius");
        for (int i = 0; i < Math.min(positions.length, Math.min(health.length, radius.length)); i++) {
            output.add(new VectorEntry(BlockPos.of(positions[i]), health[i], radius[i]));
        }
    }

    private static void writeDislodgmentCodes(CompoundTag tag, List<DislodgmentCode> entries) {
        tag.putIntArray("dislodgment_ids", entries.stream().mapToInt(DislodgmentCode::code).toArray());
        tag.putIntArray("dislodgment_values", entries.stream().mapToInt(DislodgmentCode::value).toArray());
        tag.putLongArray("dislodgment_expiry", entries.stream().mapToLong(DislodgmentCode::expiresAt).toArray());
    }

    private static void readDislodgmentCodes(CompoundTag tag, List<DislodgmentCode> output) {
        int[] codes = tag.getIntArray("dislodgment_ids");
        int[] values = tag.getIntArray("dislodgment_values");
        long[] expiry = tag.getLongArray("dislodgment_expiry");
        for (int i = 0; i < Math.min(codes.length, Math.min(values.length, expiry.length)); i++) {
            output.add(new DislodgmentCode(codes[i], values[i], expiry[i]));
        }
    }

    private static void writeGlobalAdaptations(CompoundTag tag, Map<String, Integer> entries) {
        ListTag list = new ListTag();
        entries.forEach((damage, points) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("damage", damage);
            entry.putInt("points", points);
            list.add(entry);
        });
        tag.put("global_adaptations", list);
    }

    private static void readGlobalAdaptations(CompoundTag tag, Map<String, Integer> output) {
        for (Tag raw : tag.getList("global_adaptations", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            String damage = entry.getString("damage");
            int points = entry.getInt("points");
            if (!damage.isBlank() && points > 0) {
                output.put(damage, points);
            }
        }
    }

    public record NodeEntry(BlockPos pos, int age, int type) {
    }

    public record ColonyEntry(BlockPos pos, int points) {
    }

    public record VectorEntry(BlockPos pos, int health, int radius) {
    }

    public record DislodgmentCode(int code, int value, long expiresAt) {
    }

    public record GlobalAdaptation(String damage, int points) {
    }
}
