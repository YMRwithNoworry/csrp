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

const config = read("src/main/java/alku/csrp/Config.java");
const worldConfig = read("src/main/java/alku/csrp/config/WorldConfig.java");
const events = read("src/main/java/alku/csrp/event/CothSpawnEvents.java");

// Original SRPConfigSystems: cothActive = true, mobSpawningCOTHChanceSix/Seven/Eight = 0.2/0.4/0.8.
expect(config, /\.define\("cothActive", true\)/,
        "cothActive default of true is missing");
expect(config, /\.defineInRange\("mobSpawningCothChanceSix", 0\.2D, 0\.0D, 1\.0D\)/,
        "mobSpawningCothChanceSix default of 0.2 is missing");
expect(config, /\.defineInRange\("mobSpawningCothChanceSeven", 0\.4D, 0\.0D, 1\.0D\)/,
        "mobSpawningCothChanceSeven default of 0.4 is missing");
expect(config, /\.defineInRange\("mobSpawningCothChanceEight", 0\.8D, 0\.0D, 1\.0D\)/,
        "mobSpawningCothChanceEight default of 0.8 is missing");
expect(config, /public static boolean cothActive\(\)/,
        "cothActive accessor is missing");
expect(config, /public static double mobSpawningCothChance\(int phase\)/,
        "mobSpawningCothChance(phase) accessor is missing");
// Original setCOTH only has cases 1..8, so phases 9/10 must fall through to 0.0.
expect(config, /case 8 -> MOB_SPAWNING_COTH_CHANCE_EIGHT\.get\(\);\s*default -> 0\.0D;/s,
        "the per-phase COTH table no longer stops at phase 8");

// Original SRPConfigWorld.nodesActivated / coloniesActivated default to true.
expect(worldConfig, /\.define\("nodesActivated", true\)/,
        "nodesActivated default of true is missing");
expect(worldConfig, /\.define\("coloniesActivated", true\)/,
        "coloniesActivated default of true is missing");
expect(worldConfig, /public static boolean nodesActivated\(\)/,
        "nodesActivated accessor is missing");

// Original onEntitySpawn: node-range COTH first, then the non-parasite phase roll.
expect(events, /class CothSpawnEvents/,
        "CothSpawnEvents is missing");
expect(events, /WorldConfig\.nodesActivated\(\) && data\.nearestHeartAge\(entity\.blockPosition\(\)\) != -1/,
        "the node-range COTH branch is missing");
expect(events, /SPAWN_COTH_TICKS = 3600/,
        "the original 3600-tick COTH duration is missing");
expect(events, /entity instanceof Parasite \|\| InfectionMechanics\.isCothImmune\(entity\)/,
        "the phase roll no longer skips parasites and immune entities");
expect(events, /Config\.mobSpawningCothChance\(data\.evolutionPhase\(\)\)/,
        "the per-phase COTH roll is missing");
expect(events, /Config\.cothActive\(\)/,
        "the handler no longer honours cothActive");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("COTH on spawn contract ok");
