package alku.csrp.world;

import alku.csrp.Config;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class SrpWorldData extends SavedData {
    private static final String DATA_NAME = "csrp_world_data";
    private static final int DATA_VERSION = 4;
    private static final Codec<SrpWorldData> CODEC =
            CompoundTag.CODEC.xmap(SrpWorldData::load, SrpWorldData::save);
    private static final SavedDataType<SrpWorldData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("csrp", DATA_NAME), SrpWorldData::new, CODEC);
    private static final int[] DISLODGMENT_PHASE_COOLDOWN_MULTIPLIER = {1, 4, 3, 3, 4, 5, 6, 7, 8, 9, 10};

    private boolean initialized;
    private int dataVersion = DATA_VERSION;
    private int evolutionPhase = -1;
    private int evolutionPoints = -300;
    private SrpDifficulty difficulty = SrpDifficulty.NORMAL;
    private SrpStarType starType = SrpStarType.NORMAL;
    /**
     * Create-world "meteor infection" choice. {@code null} means the world never
     * stored one (older saves), so {@link Config#meteorEnabled()} stays authoritative.
     */
    private SrpMeteorMode meteorInfection;
    private double difficultyPointRemainder;
    private long cooldownEnd;
    private boolean canGain = true;
    private boolean canLose = true;
    private int generation;
    private int generationTicks;
    private int assimilatedEndermen;
    private double passivePointRemainder;
    private int ubiquitousDevelopment;
    private boolean eveMode;
    private long dislodgmentTriggerCooldownEnd;
    private long reinforcementCooldownEnd;
    private final List<Integer> lockedParasites = new ArrayList<>();
    private final List<NodeEntry> nodes = new ArrayList<>();
    private final List<ColonyEntry> colonies = new ArrayList<>();
    private final List<VectorEntry> vectors = new ArrayList<>();
    private final List<DislodgmentCode> dislodgmentCodes = new ArrayList<>();
    private final long[] dislodgmentCooldownEnds = new long[30];
    private final Map<String, Integer> globalAdaptations = new LinkedHashMap<>();

    public static SrpWorldData get(ServerLevel level) {
        SrpWorldData data = level.getDataStorage().computeIfAbsent(TYPE);
        if (!data.initialized) {
            data.initialize(level);
        }
        data.migrateRemovedPhaseCooldown();
        if (level.dimension() != Level.OVERWORLD) {
            ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                data.setEveMode(get(overworld).eveMode());
            }
        }
        return data;
    }

    private static SrpWorldData load(CompoundTag tag) {
        SrpWorldData data = new SrpWorldData();
        data.dataVersion = tag.getIntOr("data_version", 0);
        data.initialized = tag.contains("evolution_phase");
        if (tag.contains("evolution_phase")) {
            data.evolutionPhase = tag.getIntOr("evolution_phase", 0);
        }
        data.evolutionPoints = tag.getIntOr("evolution_points", 0);
        data.difficulty = SrpDifficulty.byId(tag.getStringOr("srp_difficulty", ""));
        data.starType = SrpStarType.byId(tag.getStringOr("star_type", ""));
        data.meteorInfection = tag.contains("meteor_infection")
                ? SrpMeteorMode.byId(tag.getStringOr("meteor_infection", ""))
                : null;
        data.difficultyPointRemainder = tag.getDoubleOr("difficulty_point_remainder", 0.0D);
        data.cooldownEnd = tag.getLongOr("cooldown_end", 0L);
        data.canGain = !tag.contains("can_gain") || tag.getBooleanOr("can_gain", false);
        data.canLose = !tag.contains("can_lose") || tag.getBooleanOr("can_lose", false);
        data.generation = tag.getIntOr("generation", 0);
        data.generationTicks = tag.getIntOr("generation_ticks", 0);
        data.assimilatedEndermen = tag.getIntOr("assimilated_endermen", 0);
        data.passivePointRemainder = tag.getDoubleOr("passive_point_remainder", 0.0D);
        data.ubiquitousDevelopment = tag.getIntOr("ubiquitous_development", 0);
        data.eveMode = tag.getBooleanOr("eve_mode", false);
        data.dislodgmentTriggerCooldownEnd = tag.getLongOr("dislodgment_trigger_cooldown_end", 0L);
        data.reinforcementCooldownEnd = tag.getLongOr("reinforcement_cooldown_end", 0L);
        long[] dislodgmentCooldowns = tag.getLongArray("dislodgment_cooldown_ends").orElse(new long[0]);
        System.arraycopy(dislodgmentCooldowns, 0, data.dislodgmentCooldownEnds, 0,
                Math.min(dislodgmentCooldowns.length, data.dislodgmentCooldownEnds.length));

        for (int id : tag.getIntArray("locked_parasites").orElse(new int[0])) {
            data.lockedParasites.add(id);
        }
        readNodes(tag, data.nodes);
        readColonies(tag, data.colonies);
        readVectors(tag, data.vectors);
        readDislodgmentCodes(tag, data.dislodgmentCodes);
        readGlobalAdaptations(tag, data.globalAdaptations);
        return data;
    }

    private static CompoundTag save(SrpWorldData data) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("initialized", data.initialized);
        tag.putInt("data_version", DATA_VERSION);
        tag.putInt("evolution_phase", data.evolutionPhase);
        tag.putInt("evolution_points", data.evolutionPoints);
        tag.putString("srp_difficulty", data.difficulty.id());
        tag.putString("star_type", data.starType.id());
        if (data.meteorInfection != null) {
            tag.putString("meteor_infection", data.meteorInfection.id());
        }
        tag.putDouble("difficulty_point_remainder", data.difficultyPointRemainder);
        tag.putLong("cooldown_end", data.cooldownEnd);
        tag.putBoolean("can_gain", data.canGain);
        tag.putBoolean("can_lose", data.canLose);
        tag.putInt("generation", data.generation);
        tag.putInt("generation_ticks", data.generationTicks);
        tag.putInt("assimilated_endermen", data.assimilatedEndermen);
        tag.putDouble("passive_point_remainder", data.passivePointRemainder);
        tag.putInt("ubiquitous_development", data.ubiquitousDevelopment);
        tag.putBoolean("eve_mode", data.eveMode);
        tag.putLong("dislodgment_trigger_cooldown_end", data.dislodgmentTriggerCooldownEnd);
        tag.putLong("reinforcement_cooldown_end", data.reinforcementCooldownEnd);
        tag.putLongArray("dislodgment_cooldown_ends", data.dislodgmentCooldownEnds);
        tag.putIntArray("locked_parasites",
                data.lockedParasites.stream().mapToInt(Integer::intValue).toArray());
        writeNodes(tag, data.nodes);
        writeColonies(tag, data.colonies);
        writeVectors(tag, data.vectors);
        writeDislodgmentCodes(tag, data.dislodgmentCodes);
        writeGlobalAdaptations(tag, data.globalAdaptations);
        return tag;
    }

    public int evolutionPhase() {
        return eveMode ? 10 : evolutionPhase;
    }

    public void forceEvolutionPhase(ServerLevel level, int phase) {
        int previous = evolutionPhase;
        evolutionPhase = Math.max(-2, Math.min(10, phase));
        evolutionPoints = EvolutionSystem.thresholdForPhase(evolutionPhase);
        if (previous != evolutionPhase) {
            EvolutionSystem.announcePhaseChange(level, previous, evolutionPhase);
        }
        setDirty();
    }

    public int evolutionPoints() {
        return eveMode ? EvolutionSystem.MAX_EVOLUTION_POINTS : evolutionPoints;
    }

    public SrpDifficulty difficulty() {
        return difficulty;
    }

    public void setDifficulty(SrpDifficulty difficulty) {
        if (this.difficulty == difficulty) {
            return;
        }
        this.difficulty = difficulty;
        difficultyPointRemainder = 0.0D;
        setDirty();
    }

    public SrpStarType starType() {
        return starType;
    }

    public void setStarType(SrpStarType starType) {
        if (this.starType == starType) {
            return;
        }
        this.starType = starType;
        setDirty();
    }

    /** @return the create-world choice, or {@code null} when the world follows the config. */
    public SrpMeteorMode meteorInfection() {
        return meteorInfection;
    }

    /** Effective gate for the periodic meteor infection event. */
    public boolean meteorInfectionEnabled() {
        return meteorInfection == null ? Config.meteorEnabled() : meteorInfection.enabled();
    }

    public void setMeteorInfection(SrpMeteorMode mode) {
        if (meteorInfection == mode) {
            return;
        }
        meteorInfection = mode;
        setDirty();
    }

    private int applyDifficultyPointMultiplier(int points) {
        if (points <= 0 || difficulty.pointMultiplier() == 1.0D) {
            return points;
        }
        double scaled = points * difficulty.pointMultiplier() + difficultyPointRemainder;
        int wholePoints = (int) Math.floor(scaled);
        difficultyPointRemainder = scaled - wholePoints;
        setDirty();
        return wholePoints;
    }

    public boolean addEvolutionPoints(ServerLevel level, int points) {
        return addEvolutionPoints(level, points, false);
    }

    public boolean addEvolutionPoints(ServerLevel level, int points, boolean bypassCooldown) {
        if (!canAddEvolutionPoints(points) || (!bypassCooldown && cooldown(level) > 0)) {
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
            setCooldown(level, EvolutionSystem.phaseDelaySeconds(evolutionPhase));
            EvolutionSystem.announcePhaseChange(level, previous, evolutionPhase);
        }
        setDirty();
        return true;
    }

    public boolean addDifficultyScaledEvolutionPoints(ServerLevel level, int points) {
        if (!canAddEvolutionPoints(points)) {
            return false;
        }
        int adjusted = applyDifficultyPointMultiplier(points);
        return adjusted == 0 || addEvolutionPoints(level, adjusted);
    }

    private boolean canAddEvolutionPoints(int points) {
        return !((points > 0 && !canGain) || (points < 0 && !canLose) || evolutionPhase == -2
                || (points < 0 && evolutionPhase < 0));
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
        return eveMode ? 5 : generation;
    }

    public void setGeneration(int value) {
        generation = Math.max(0, Math.min(5, value));
        generationTicks = 0;
        setDirty();
    }

    public int generationTicks() {
        return generationTicks;
    }

    public int assimilatedEndermen() {
        return assimilatedEndermen;
    }

    public void recordAssimilatedEnderman() {
        if (assimilatedEndermen < Integer.MAX_VALUE) {
            assimilatedEndermen++;
            setDirty();
        }
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

    public void tickPassivePoints(ServerLevel level) {
        if (!canGain || evolutionPhase < 0) {
            return;
        }
        passivePointRemainder += EvolutionSystem.passivePointsPerSecond(evolutionPhase);
        int wholePoints = (int) Math.floor(passivePointRemainder);
        if (wholePoints > 0
                && EvolutionSystem.addPoints(level, wholePoints, EvolutionSystem.PointSource.PASSIVE)) {
            passivePointRemainder -= wholePoints;
        }
        setDirty();
    }

    public int ubiquitousDevelopmentOverride() {
        return eveMode ? 4 : ubiquitousDevelopment;
    }

    public void setUbiquitousDevelopment(int level) {
        ubiquitousDevelopment = Math.max(0, Math.min(4, level));
        setDirty();
    }

    public boolean eveMode() {
        return eveMode;
    }

    public void setEveMode(boolean enabled) {
        if (eveMode == enabled) {
            return;
        }
        eveMode = enabled;
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
        colonies.add(new ColonyEntry(pos.immutable(), Math.max(1, Math.min(Config.colonyPointCap(), points))));
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
        return Math.min(Config.colonyTotalPointCap(), colonies.stream().mapToInt(ColonyEntry::points).sum());
    }

    public boolean reinforcementReady(ServerLevel level) {
        return level.getGameTime() >= reinforcementCooldownEnd;
    }

    public void startReinforcementCooldown(ServerLevel level, int ticks) {
        reinforcementCooldownEnd = level.getGameTime() + ticks;
        setDirty();
    }

    public ColonyEntry nearestColonyInEffectRange(BlockPos pos) {
        return nearestColonyInRange(pos, true);
    }

    public ColonyEntry nearestColonyInConstructionRange(BlockPos pos) {
        return nearestColonyInRange(pos, false);
    }

    public static int colonyConstructionRadius(int points) {
        int cappedPoints = Math.max(0, Math.min(Config.colonyPointCap(), points));
        return Config.colonyBaseRadius()
                + cappedPoints / Config.colonySpreadPoint() * Config.colonySpreadValue();
    }

    public static int colonyEffectRadius(int points) {
        int cappedPoints = Math.max(0, Math.min(Config.colonyPointCap(), points));
        return Config.colonyBaseEffectRadius()
                + cappedPoints / Config.colonyEffectSpreadPoint() * Config.colonyEffectSpreadValue();
    }

    private ColonyEntry nearestColonyInRange(BlockPos pos, boolean effectRange) {
        ColonyEntry closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ColonyEntry entry : colonies) {
            int radius = effectRange ? colonyEffectRadius(entry.points())
                    : colonyConstructionRadius(entry.points());
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
        return Collections.unmodifiableList(new ArrayList<>(dislodgmentCodes));
    }

    public boolean setDislodgmentCode(ServerLevel level, int code, int value, int durationSeconds) {
        return startDislodgmentCode(level, code, value, durationSeconds, 0);
    }

    public boolean startDislodgmentCode(ServerLevel level, int code, int value, int durationSeconds,
            int evolutionPointCost) {
        if (code < 0 || code >= 30 || value < 0 || durationSeconds < 0 || evolutionPointCost < 0
                || dislodgmentCodes.stream().anyMatch(entry -> entry.code() == code)
                || dislodgmentCooldownEnds[code] > level.getGameTime()) {
            return false;
        }
        if (Config.useEvolutionPhases() && evolutionPointCost > 0 && !eveMode) {
            long remaining = (long) evolutionPoints - evolutionPointCost;
            if (remaining < EvolutionSystem.thresholdForPhase(evolutionPhase)) {
                return false;
            }
            evolutionPoints = (int) remaining;
        }
        long durationTicks = Math.min(Long.MAX_VALUE - level.getGameTime(), (long) durationSeconds * 20L);
        dislodgmentCodes.add(new DislodgmentCode(code, value, level.getGameTime() + durationTicks));
        setDirty();
        DislodgmentSystem.onCodeStarted(level, code, value, durationTicks);
        return true;
    }

    public DislodgmentCode dislodgmentCode(int code) {
        return dislodgmentCodes.stream().filter(entry -> entry.code() == code).findFirst().orElse(null);
    }

    public boolean increaseDislodgmentValue(int code, int amount) {
        for (int index = 0; index < dislodgmentCodes.size(); index++) {
            DislodgmentCode entry = dislodgmentCodes.get(index);
            if (entry.code() != code) {
                continue;
            }
            long value = Math.max(0L, (long) entry.value() + amount);
            dislodgmentCodes.set(index, new DislodgmentCode(code,
                    (int) Math.min(Integer.MAX_VALUE, value), entry.expiresAt()));
            setDirty();
            return true;
        }
        return false;
    }

    public List<DislodgmentCode> expireDislodgmentCodes(ServerLevel level) {
        List<DislodgmentCode> expired = dislodgmentCodes.stream()
                .filter(code -> code.expiresAt() <= level.getGameTime()).toList();
        if (!expired.isEmpty()) {
            for (DislodgmentCode code : expired) {
                startDislodgmentCodeCooldown(level, code.code());
            }
            dislodgmentCodes.removeAll(expired);
            setDirty();
        }
        return expired;
    }

    public List<DislodgmentCode> endAllDislodgmentCodes(ServerLevel level) {
        List<DislodgmentCode> ended = new ArrayList<>(dislodgmentCodes);
        for (DislodgmentCode code : ended) {
            startDislodgmentCodeCooldown(level, code.code());
        }
        dislodgmentCodes.clear();
        setDirty();
        return ended;
    }

    public long dislodgmentCooldown(ServerLevel level, int code) {
        if (code < 0 || code >= dislodgmentCooldownEnds.length) {
            return 0L;
        }
        return Math.max(0L, dislodgmentCooldownEnds[code] - level.getGameTime());
    }

    private void startDislodgmentCodeCooldown(ServerLevel level, int code) {
        if (code < 0 || code >= dislodgmentCooldownEnds.length) {
            return;
        }
        int phase = Math.max(0, Math.min(10, evolutionPhase()));
        long seconds = (long) Config.dislodgmentCodeCooldown(code)
                * DISLODGMENT_PHASE_COOLDOWN_MULTIPLIER[phase];
        long ticks = Math.min(Long.MAX_VALUE - level.getGameTime(), seconds * 20L);
        dislodgmentCooldownEnds[code] = level.getGameTime() + ticks;
    }

    public boolean dislodgmentTriggerReady(ServerLevel level) {
        return level.getGameTime() >= dislodgmentTriggerCooldownEnd;
    }

    public void setDislodgmentTriggerCooldown(ServerLevel level, int ticks) {
        dislodgmentTriggerCooldownEnd = level.getGameTime() + Math.max(0, ticks);
        setDirty();
    }

    public void clearDislodgmentCodes() {
        dislodgmentCodes.clear();
        Arrays.fill(dislodgmentCooldownEnds, 0L);
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
        assimilatedEndermen = 0;
        passivePointRemainder = 0.0D;
        ubiquitousDevelopment = 0;
        dislodgmentTriggerCooldownEnd = 0L;
        lockedParasites.clear();
        nodes.clear();
        colonies.clear();
        vectors.clear();
        dislodgmentCodes.clear();
        Arrays.fill(dislodgmentCooldownEnds, 0L);
        globalAdaptations.clear();
        setDirty();
    }

    private void initialize(ServerLevel level) {
        EvolutionSystem.InitialProgress initial = EvolutionSystem.initialProgress(level);
        initialized = true;
        evolutionPhase = initial.phase();
        evolutionPoints = initial.points();
        difficulty = level == level.getServer().overworld()
                ? SrpDifficultySelection.consumeOrDefault()
                : SrpWorldData.get(level.getServer().overworld()).difficulty();
        starType = level == level.getServer().overworld()
                ? SrpStarTypeSelection.consumeOrDefault()
                : SrpWorldData.get(level.getServer().overworld()).starType();
        meteorInfection = level == level.getServer().overworld()
                ? SrpMeteorSelection.consume()
                : SrpWorldData.get(level.getServer().overworld()).meteorInfection;
        difficultyPointRemainder = 0.0D;
        generation = 0;
        generationTicks = 0;
        assimilatedEndermen = 0;
        passivePointRemainder = 0.0D;
        cooldownEnd = 0L;
        setDirty();
    }

    private void migrateRemovedPhaseCooldown() {
        if (dataVersion >= DATA_VERSION) {
            return;
        }
        if (dataVersion < 3) {
            cooldownEnd = 0L;
        }
        dataVersion = DATA_VERSION;
        setDirty();
    }

    private static void writeNodes(CompoundTag tag, List<NodeEntry> entries) {
        tag.putLongArray("node_positions", entries.stream().mapToLong(entry -> entry.pos().asLong()).toArray());
        tag.putIntArray("node_ages", entries.stream().mapToInt(NodeEntry::age).toArray());
        tag.putIntArray("node_types", entries.stream().mapToInt(NodeEntry::type).toArray());
    }

    private static void readNodes(CompoundTag tag, List<NodeEntry> output) {
        long[] positions = tag.getLongArray("node_positions").orElse(new long[0]);
        int[] ages = tag.getIntArray("node_ages").orElse(new int[0]);
        int[] types = tag.getIntArray("node_types").orElse(new int[0]);
        for (int i = 0; i < Math.min(positions.length, Math.min(ages.length, types.length)); i++) {
            output.add(new NodeEntry(BlockPos.of(positions[i]), ages[i], types[i]));
        }
    }

    private static void writeColonies(CompoundTag tag, List<ColonyEntry> entries) {
        tag.putLongArray("colony_positions", entries.stream().mapToLong(entry -> entry.pos().asLong()).toArray());
        tag.putIntArray("colony_points", entries.stream().mapToInt(ColonyEntry::points).toArray());
    }

    private static void readColonies(CompoundTag tag, List<ColonyEntry> output) {
        long[] positions = tag.getLongArray("colony_positions").orElse(new long[0]);
        int[] points = tag.getIntArray("colony_points").orElse(new int[0]);
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
        long[] positions = tag.getLongArray("vector_positions").orElse(new long[0]);
        int[] health = tag.getIntArray("vector_health").orElse(new int[0]);
        int[] radius = tag.getIntArray("vector_radius").orElse(new int[0]);
        for (int i = 0; i < Math.min(positions.length, Math.min(health.length, radius.length)); i++) {
            output.add(new VectorEntry(BlockPos.of(positions[i]), Math.max(1, health[i]),
                    Math.max(1, Math.min(200_000, radius[i]))));
        }
    }

    private static void writeDislodgmentCodes(CompoundTag tag, List<DislodgmentCode> entries) {
        tag.putIntArray("dislodgment_ids", entries.stream().mapToInt(DislodgmentCode::code).toArray());
        tag.putIntArray("dislodgment_values", entries.stream().mapToInt(DislodgmentCode::value).toArray());
        tag.putLongArray("dislodgment_expiry", entries.stream().mapToLong(DislodgmentCode::expiresAt).toArray());
    }

    private static void readDislodgmentCodes(CompoundTag tag, List<DislodgmentCode> output) {
        int[] codes = tag.getIntArray("dislodgment_ids").orElse(new int[0]);
        int[] values = tag.getIntArray("dislodgment_values").orElse(new int[0]);
        long[] expiry = tag.getLongArray("dislodgment_expiry").orElse(new long[0]);
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
        for (Tag raw : tag.getListOrEmpty("global_adaptations")) {
            CompoundTag entry = (CompoundTag) raw;
            String damage = entry.getStringOr("damage", "");
            int points = entry.getIntOr("points", 0);
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
