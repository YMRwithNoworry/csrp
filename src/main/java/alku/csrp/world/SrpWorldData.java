package alku.csrp.world;

import alku.csrp.Config;
import alku.csrp.config.WorldConfig;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public final class SrpWorldData extends SavedData {
    private static final String DATA_NAME = "csrp_world_data";
    // 7: the generation timer moved from the per-second counter "generation_ticks" to the absolute
    // world total time "generation_started_at" at which the current generation started.
    private static final int DATA_VERSION = 7;
    private static final int[] DISLODGMENT_PHASE_COOLDOWN_MULTIPLIER = {1, 4, 3, 3, 4, 5, 6, 7, 8, 9, 10};
    /** Upper bound kept on the stored generation start time so the command math cannot overflow. */
    private static final long MAX_GENERATION_CLOCK = Long.MAX_VALUE / 2L;

    private boolean initialized;
    private int dataVersion = DATA_VERSION;
    private int evolutionPhase = -1;
    private int evolutionPoints = -300;
    private SrpDifficulty difficulty = SrpDifficulty.NORMAL;
    private SrpStarType starType = SrpStarType.NORMAL;
    private boolean meteorsEnabled;
    /** 1.10.9 {@code SRPStarWorldData.fracturedTerrain} — only meaningful while the star type is COLD. */
    private boolean fracturedTerrain = true;
    /** 1.10.9 {@code SRPStarWorldData.mushroomTrees} — cold-star tree density toggle. */
    private boolean mushroomTrees = true;
    private double difficultyPointRemainder;
    private long cooldownEnd;
    private boolean canGain = true;
    private boolean canLose = true;
    private int generation;
    /** World total time at which the current generation started (1.10.9 {@code dimGenerationTime}). */
    private long generationStartedAt;
    /**
     * Elapsed ticks carried over from a pre-v7 save key "generation_ticks", or -1 once consumed.
     * Converted into {@link #generationStartedAt} the first time the data is read with a level.
     */
    private long legacyGenerationTicks = -1L;
    /** Latest world total time seen with a level in hand; the level-less accessors measure against it. */
    private long lastObservedGameTime;
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
        SrpWorldData data = level.getDataStorage().computeIfAbsent(SrpWorldData::load,
                SrpWorldData::new, DATA_NAME);
        if (!data.initialized) {
            data.initialize(level);
        }
        data.lastObservedGameTime = level.getGameTime();
        data.migrateRemovedPhaseCooldown();
        data.migrateGenerationStartTime(level);
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
        data.dataVersion = tag.getInt("data_version");
        data.initialized = tag.contains("evolution_phase");
        if (tag.contains("evolution_phase")) {
            data.evolutionPhase = tag.getInt("evolution_phase");
        }
        data.evolutionPoints = tag.getInt("evolution_points");
        data.difficulty = SrpDifficulty.byId(tag.getString("srp_difficulty"));
        data.starType = SrpStarType.byId(tag.getString("star_type"));
        data.meteorsEnabled = tag.contains("meteors_enabled")
                ? tag.getBoolean("meteors_enabled") : WorldConfig.meteorsEnabled();
        // Added in data version 6 (1.10.9). Pre-v6 saves get the 1.10.9 defaults, i.e. both enabled.
        data.fracturedTerrain = !tag.contains("fractured_terrain") || tag.getBoolean("fractured_terrain");
        data.mushroomTrees = !tag.contains("mushroom_trees") || tag.getBoolean("mushroom_trees");
        data.difficultyPointRemainder = tag.getDouble("difficulty_point_remainder");
        data.cooldownEnd = tag.getLong("cooldown_end");
        data.canGain = !tag.contains("can_gain") || tag.getBoolean("can_gain");
        data.canLose = !tag.contains("can_lose") || tag.getBoolean("can_lose");
        data.generation = tag.getInt("generation");
        if (tag.contains("generation_started_at")) {
            data.generationStartedAt = tag.getLong("generation_started_at");
        } else {
            // Pre-v7 saves only carry the elapsed tick counter. Keep it around until a level is
            // available; a missing key means "no progress" so an ancient save is never overdue.
            data.legacyGenerationTicks = Math.max(0L, tag.getInt("generation_ticks"));
        }
        data.assimilatedEndermen = tag.getInt("assimilated_endermen");
        data.passivePointRemainder = tag.getDouble("passive_point_remainder");
        data.ubiquitousDevelopment = tag.getInt("ubiquitous_development");
        data.eveMode = tag.getBoolean("eve_mode");
        data.dislodgmentTriggerCooldownEnd = tag.getLong("dislodgment_trigger_cooldown_end");
        data.reinforcementCooldownEnd = tag.getLong("reinforcement_cooldown_end");
        long[] dislodgmentCooldowns = tag.getLongArray("dislodgment_cooldown_ends");
        System.arraycopy(dislodgmentCooldowns, 0, data.dislodgmentCooldownEnds, 0,
                Math.min(dislodgmentCooldowns.length, data.dislodgmentCooldownEnds.length));

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
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("initialized", initialized);
        tag.putInt("data_version", DATA_VERSION);
        tag.putInt("evolution_phase", evolutionPhase);
        tag.putInt("evolution_points", evolutionPoints);
        tag.putString("srp_difficulty", difficulty.id());
        tag.putString("star_type", starType.id());
        tag.putBoolean("meteors_enabled", meteorsEnabled);
        tag.putBoolean("fractured_terrain", fracturedTerrain);
        tag.putBoolean("mushroom_trees", mushroomTrees);
        tag.putDouble("difficulty_point_remainder", difficultyPointRemainder);
        tag.putLong("cooldown_end", cooldownEnd);
        tag.putBoolean("can_gain", canGain);
        tag.putBoolean("can_lose", canLose);
        tag.putInt("generation", generation);
        tag.putLong("generation_started_at", generationStartedAt);
        tag.putInt("assimilated_endermen", assimilatedEndermen);
        tag.putDouble("passive_point_remainder", passivePointRemainder);
        tag.putInt("ubiquitous_development", ubiquitousDevelopment);
        tag.putBoolean("eve_mode", eveMode);
        tag.putLong("dislodgment_trigger_cooldown_end", dislodgmentTriggerCooldownEnd);
        tag.putLong("reinforcement_cooldown_end", reinforcementCooldownEnd);
        tag.putLongArray("dislodgment_cooldown_ends", dislodgmentCooldownEnds);
        tag.putIntArray("locked_parasites", lockedParasites);
        writeNodes(tag, nodes);
        writeColonies(tag, colonies);
        writeVectors(tag, vectors);
        writeDislodgmentCodes(tag, dislodgmentCodes);
        writeGlobalAdaptations(tag, globalAdaptations);
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

    public boolean meteorsEnabled() {
        return meteorsEnabled;
    }

    public void setStarType(SrpStarType starType) {
        if (this.starType == starType) {
            return;
        }
        this.starType = starType;
        // 1.10.9: fractured terrain only exists under a cold star; switching away clears the flag.
        if (starType != SrpStarType.COLD) {
            fracturedTerrain = false;
        }
        setDirty();
    }

    public boolean fracturedTerrainEnabled() {
        return fracturedTerrain;
    }

    public void setFracturedTerrainEnabled(boolean enabled) {
        if (fracturedTerrain == enabled) {
            return;
        }
        fracturedTerrain = enabled;
        setDirty();
    }

    public boolean mushroomTreesEnabled() {
        return mushroomTrees;
    }

    public void setMushroomTreesEnabled(boolean enabled) {
        if (mushroomTrees == enabled) {
            return;
        }
        mushroomTrees = enabled;
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
        // The original only ever advanced generations from here, after every rejection check passed.
        checkGeneration(level);

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

    /**
     * Clamps the stored counter to 0..5 and returns it. The stored start time goes back to 0, mirroring
     * the original commands, which call {@code setGeneration(gen, dim)} and then
     * {@code setGenerationTime(0, dim)} back to back (SRPCommandGeneration:74-75,
     * SRPCommandRoot:71-72, SRPCommandEvolution:353-354, SRPCommandUDevelopment:172-173). A dimension
     * whose start time is 0 is overdue as soon as the world is older than the needed time, so the next
     * accepted point change advances it by one generation - exactly as in the original.
     */
    public int setGeneration(int value) {
        generation = Math.max(0, Math.min(5, value));
        generationStartedAt = 0L;
        setDirty();
        return generation;
    }

    /**
     * Ticks left until the next generation, i.e. the original {@code getGenerationNeededTime(World, int)};
     * 0 once generation 5 is reached.
     */
    public int generationTicks() {
        if (generation >= 5) {
            return 0;
        }
        long remaining = neededGenerationTicks() - (lastObservedGameTime - generationStartedAt);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, remaining));
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

    /**
     * Original {@code /srpgeneration addticks}: {@code setGenerationTime(getGenerationTime(id) + n, id)}.
     * Adding to the stored start time shrinks the elapsed time, so a positive value <em>delays</em> the
     * next generation by {@code ticks} ticks - exactly like the original.
     */
    public void addGenerationTicks(int ticks) {
        long base = Math.max(0L, Math.min(MAX_GENERATION_CLOCK, generationStartedAt));
        generationStartedAt = Math.max(0L, Math.min(MAX_GENERATION_CLOCK, base + (long) ticks));
        setDirty();
    }

    /**
     * Ticks the current generation still needs, recomputed from the live evolution phase and difficulty
     * the way the original {@code getGenerationNeededTime(byte)} does on every check.
     */
    private int neededGenerationTicks() {
        return EvolutionSystem.generationNeededTicks(generation, evolutionPhase, difficulty);
    }

    /**
     * Original {@code SRPSaveData#checkGeneration(int, World)}: advance one generation when more time has
     * passed since the stored start time than the current generation needs.
     */
    private void checkGeneration(ServerLevel level) {
        if (generation >= 5) {
            return;
        }
        long now = level.getGameTime();
        lastObservedGameTime = now;
        if (now - generationStartedAt > neededGenerationTicks()) {
            generation++;
            generationStartedAt = now;
            setDirty();
        }
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
        applyConfiguredGeneration(level);
        assimilatedEndermen = 0;
        passivePointRemainder = 0.0D;
        ubiquitousDevelopment = 0;
        dislodgmentTriggerCooldownEnd = 0L;
        // Back to the 1.10.9 creation defaults; fractured terrain stays off unless the star is COLD.
        fracturedTerrain = starType == SrpStarType.COLD;
        mushroomTrees = true;
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
        meteorsEnabled = level == level.getServer().overworld()
                ? SrpMeteorSelection.consumeOrDefault(WorldConfig.meteorsEnabled())
                : SrpWorldData.get(level.getServer().overworld()).meteorsEnabled();
        // Both selection slots are one-shot, so consume them before mirroring from the overworld.
        boolean primary = level == level.getServer().overworld();
        boolean stagedFracturedTerrain = SrpStarWorldSelection.consumeFracturedTerrainOrDefault();
        boolean stagedMushroomTrees = SrpStarWorldSelection.consumeMushroomTreesOrDefault();
        fracturedTerrain = primary
                ? stagedFracturedTerrain
                : SrpWorldData.get(level.getServer().overworld()).fracturedTerrainEnabled();
        mushroomTrees = primary
                ? stagedMushroomTrees
                : SrpWorldData.get(level.getServer().overworld()).mushroomTreesEnabled();
        if (starType != SrpStarType.COLD) {
            fracturedTerrain = false;
        }
        difficultyPointRemainder = 0.0D;
        applyConfiguredGeneration(level);
        assimilatedEndermen = 0;
        passivePointRemainder = 0.0D;
        cooldownEnd = 0L;
        setDirty();
    }

    /**
     * Original world-creation defaults: {@code SRPConfigSystems.generationDefa} for every dimension,
     * overridden by the matching entries of {@code SRPConfigSystems.generationDimStart}
     * ("Generation Dimension Starting List", formatted {@code "<dimension_id>;<generation>"}).
     * Malformed entries are skipped silently, exactly like the original.
     */
    private void applyConfiguredGeneration(ServerLevel level) {
        int configured = Math.max(0, Math.min(5, Config.generationDefaultValue()));
        String location = level.dimension().location().toString();
        String path = level.dimension().location().getPath();
        String legacyId = legacyDimensionId(level);
        for (String entry : Config.generationDimensionStartingList()) {
            int separator = entry == null ? -1 : entry.indexOf(';');
            if (separator <= 0) {
                continue;
            }
            String dimension = entry.substring(0, separator).trim();
            if (!dimension.equals(location) && !dimension.equals(path)
                    && (legacyId == null || !dimension.equals(legacyId))) {
                continue;
            }
            try {
                configured = Math.max(0, Math.min(5, Integer.parseInt(entry.substring(separator + 1).trim())));
            } catch (NumberFormatException ignored) {
                // Malformed "Generation Dimension Starting List" entry: keep the previous value.
            }
        }
        generation = configured;
        // The original stores 0 for a brand new dimension (SRPSaveData.addDim: dimGenerationTime.add(0)),
        // so the timer runs from the world time origin, not from the moment the dimension appeared.
        generationStartedAt = 0L;
    }

    /**
     * The original "Generation Dimension Starting List" keys dimensions by their legacy numeric id
     * ({@code int dim = Integer.parseInt(split[0].trim())}). Those ids no longer exist in 1.20.1, so the
     * three vanilla ones are still accepted for configuration compatibility.
     */
    private static String legacyDimensionId(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) {
            return "0";
        }
        if (level.dimension() == Level.NETHER) {
            return "-1";
        }
        if (level.dimension() == Level.END) {
            return "1";
        }
        return null;
    }

    /**
     * Data version 7 turned the elapsed-tick counter "generation_ticks" into the absolute start time
     * "generation_started_at". Convert a carried-over counter without losing progress: at most one
     * full generation may count as elapsed, so the dimension is never dumped straight into the next one.
     */
    private void migrateGenerationStartTime(ServerLevel level) {
        if (legacyGenerationTicks < 0L) {
            return;
        }
        long elapsed = Math.max(0L, Math.min(legacyGenerationTicks, neededGenerationTicks()));
        legacyGenerationTicks = -1L;
        generationStartedAt = level.getGameTime() - elapsed;
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
