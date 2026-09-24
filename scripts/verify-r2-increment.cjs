const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];

function read(relativePath) {
    const file = path.join(root, relativePath);
    if (!fs.existsSync(file)) {
        failures.push(`missing ${relativePath}`);
        return "";
    }
    return fs.readFileSync(file, "utf8");
}

function expect(content, pattern, description) {
    if (!pattern.test(content)) failures.push(description);
}

const evolutionEvents = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const generalConfig = read("src/main/java/alku/csrp/config/GeneralConfig.java");
const runtimeToggles = read("src/main/java/alku/csrp/config/RuntimeToggles.java");
const commands = read("src/main/java/alku/csrp/command/SrpCommands.java");
const mobsConfig = read("src/main/java/alku/csrp/config/MobsConfig.java");
const worldData = read("src/main/java/alku/csrp/world/SrpWorldData.java");

// SRP 1.10.9 raised the cull trigger from 4x to 6x the cap and the stop line from 2x to 3x.
expect(evolutionEvents, /MOB_CLEANER_TRIGGER_MULTIPLIER\s*=\s*6;/,
        "1.10.9 mob-cleaner trigger multiplier is not 6");
expect(evolutionEvents, /MOB_CLEANER_STOP_MULTIPLIER\s*=\s*3;/,
        "1.10.9 mob-cleaner stop multiplier is not 3");
expect(evolutionEvents, /MOB_CLEANER_COOLDOWN_TICKS\s*=\s*50;/,
        "mobClearCooldown of 50 ticks is missing");
expect(evolutionEvents, /mobCap \* MOB_CLEANER_TRIGGER_MULTIPLIER/,
        "the cull trigger no longer uses the 1.10.9 multiplier");
expect(evolutionEvents, /int stopAt = mobCap \* MOB_CLEANER_STOP_MULTIPLIER;/,
        "the cull no longer stops at three times the cap");
expect(evolutionEvents, /mobCleanerCooldown = MOB_CLEANER_COOLDOWN_TICKS;/,
        "a cull does not arm the cooldown");

// SRPConfig.doTileDrops / ParasiteEventEntity.canSpawnNext toggles.
expect(generalConfig, /\.define\("parasiteBlockDrops",\s*true\)/,
        "GeneralConfig no longer exposes parasiteBlockDrops (SRPConfig.doTileDrops)");
expect(generalConfig, /public static boolean parasiteBlockDrops\(\)/,
        "GeneralConfig.parasiteBlockDrops() accessor is missing");
expect(runtimeToggles, /public static boolean parasiteBlockDrops\(\)/,
        "RuntimeToggles.parasiteBlockDrops() is missing");
expect(runtimeToggles, /public static boolean toggleParasiteBlockDrops\(\)/,
        "RuntimeToggles.toggleParasiteBlockDrops() is missing");
expect(runtimeToggles, /public static boolean mobEvolution\(\)/,
        "RuntimeToggles.mobEvolution() is missing");
expect(runtimeToggles, /public static boolean toggleMobEvolution\(\)/,
        "RuntimeToggles.toggleMobEvolution() is missing");
expect(runtimeToggles, /private static boolean mobEvolution = true;/,
        "doMobEvolution no longer defaults to true");

// The three root subcommands of SRPCommandRoot that were still missing.
expect(commands, /Commands\.literal\("toggle_dotiledrops"\)/,
        "/srparasites toggle_dotiledrops is not registered");
expect(commands, /Commands\.literal\("toggle_domobevolution"\)/,
        "/srparasites toggle_domobevolution is not registered");
expect(commands, /Commands\.literal\("readconfigurationfile"\)/,
        "/srparasites readconfigurationfile is not registered");
expect(commands, /ConfigTracker\.INSTANCE\.loadConfigs\(ModConfig\.Type\.COMMON/,
        "readconfigurationfile does not reload the COMMON configuration files");
expect(commands, /RuntimeToggles\.toggleParasiteBlockDrops\(\)/,
        "toggle_dotiledrops does not flip the runtime toggle");
expect(commands, /RuntimeToggles\.toggleMobEvolution\(\)/,
        "toggle_domobevolution does not flip the runtime toggle");

// SRPConfigMobs.*CanSpawnAssimilatedNat: the "needed assimilation value" gate for simulated mobs.
expect(mobsConfig, /"simBearNeededAssimilation",\s*2,\s*-1,/,
        "simBearNeededAssimilation default 2 is missing");
expect(mobsConfig, /"simEndermanNeededAssimilation",\s*9,\s*-1,/,
        "simEndermanNeededAssimilation default 9 is missing");
expect(mobsConfig, /"simHumanNeededAssimilation",\s*5,\s*-1,/,
        "simHumanNeededAssimilation default 5 is missing");
expect(mobsConfig, /"simSquidNeededAssimilation",\s*-1,\s*-1,/,
        "simSquidNeededAssimilation default -1 is missing");
expect(mobsConfig, /"simCowNeededAssimilation",\s*4,\s*-1,/,
        "simCowNeededAssimilation default 4 is missing");
expect(mobsConfig, /"simSheepNeededAssimilation",\s*3,\s*-1,/,
        "simSheepNeededAssimilation default 3 is missing");
expect(mobsConfig, /"simWolfNeededAssimilation",\s*2,\s*-1,/,
        "simWolfNeededAssimilation default 2 is missing");
expect(mobsConfig, /"simPigNeededAssimilation",\s*4,\s*-1,/,
        "simPigNeededAssimilation default 4 is missing");
expect(mobsConfig, /"simVillagerNeededAssimilation",\s*6,\s*-1,/,
        "simVillagerNeededAssimilation default 6 is missing");
expect(mobsConfig, /"simHorseNeededAssimilation",\s*3,\s*-1,/,
        "simHorseNeededAssimilation default 3 is missing");
expect(mobsConfig, /"simDragonENeededAssimilation",\s*-1,\s*-1,/,
        "simDragonENeededAssimilation default -1 is missing");
expect(mobsConfig, /"hiGolemNeededAssimilation",\s*6,\s*-1,/,
        "hiGolemNeededAssimilation default 6 is missing");
expect(mobsConfig, /public static int neededAssimilation\(String spawnKey\)/,
        "MobsConfig.neededAssimilation(String) is missing");

// SRPSaveData.assimCounts: per-type assimilation counters live in the world save data.
expect(worldData, /private final Map<String, Integer> assimilationCounts/,
        "SrpWorldData no longer tracks per-type assimilation counters");
expect(worldData, /public int assimilationCount\(String spawnKey\)/,
        "SrpWorldData.assimilationCount(String) is missing");
expect(worldData, /public void addAssimilationCount\(String spawnKey\)/,
        "SrpWorldData.addAssimilationCount(String) is missing");
expect(worldData, /writeAssimilationCounts\(tag, data\.assimilationCounts\)/,
        "assimilation counters are not written to the save data");
expect(worldData, /readAssimilationCounts\(tag, data\.assimilationCounts\)/,
        "assimilation counters are not read back from the save data");
expect(worldData, /tag\.put\("assimilation_counts", list\)/,
        "the assimilation_counts save key is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("r2 increment contract ok");
