package alku.csrp.world;

import alku.csrp.Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class SrpWorldData extends SavedData {
    private static final String DATA_NAME = "csrp_world_data";
    private static final Factory<SrpWorldData> FACTORY = new Factory<>(SrpWorldData::new, SrpWorldData::load);

    private int evolutionPhase = Config.evolutionPhase();
    private int evolutionPoints;
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

    public static SrpWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static SrpWorldData load(CompoundTag tag, HolderLookup.Provider registries) {
        SrpWorldData data = new SrpWorldData();
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
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
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
        return tag;
    }

    public int evolutionPhase() {
        return evolutionPhase;
    }

    public void setEvolutionPhase(int phase) {
        evolutionPhase = phase;
        setDirty();
    }

    public int evolutionPoints() {
        return evolutionPoints;
    }

    public boolean addEvolutionPoints(int points) {
        if ((points > 0 && !canGain) || (points < 0 && !canLose)) {
            return false;
        }
        evolutionPoints += points;
        setDirty();
        return true;
    }

    public int cooldown(ServerLevel level) {
        return (int) Math.max(0L, cooldownEnd - level.getGameTime());
    }

    public void setCooldown(ServerLevel level, int ticks) {
        cooldownEnd = level.getGameTime() + ticks;
        setDirty();
    }

    public void addCooldown(ServerLevel level, int ticks) {
        cooldownEnd = Math.max(cooldownEnd, level.getGameTime()) + ticks;
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
        generation = value;
        generationTicks = 0;
        setDirty();
    }

    public int generationTicks() {
        return generationTicks;
    }

    public void addGenerationTicks(int ticks) {
        generationTicks += ticks;
        setDirty();
    }

    public int ubiquitousDevelopment() {
        return ubiquitousDevelopment;
    }

    public void setUbiquitousDevelopment(int level) {
        ubiquitousDevelopment = level;
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
        colonies.add(new ColonyEntry(pos.immutable(), 0));
        setDirty();
    }

    public boolean removeColony(BlockPos pos) {
        boolean removed = colonies.removeIf(entry -> entry.pos().equals(pos));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void clearColonies() {
        colonies.clear();
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

    public void reset() {
        evolutionPhase = Config.evolutionPhase();
        evolutionPoints = 0;
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

    public record NodeEntry(BlockPos pos, int age, int type) {
    }

    public record ColonyEntry(BlockPos pos, int points) {
    }

    public record VectorEntry(BlockPos pos, int health, int radius) {
    }

    public record DislodgmentCode(int code, int value, long expiresAt) {
    }
}
