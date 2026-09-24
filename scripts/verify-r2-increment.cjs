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

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("r2 increment contract ok");
