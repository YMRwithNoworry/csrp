const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const read = (file) => fs.readFileSync(path.join(root, file), "utf8");
const failures = [];
const expect = (source, pattern, message) => {
    if (!pattern.test(source)) failures.push(message);
};
const rules = read("src/main/java/alku/csrp/world/HighPhaseVanillaMobRules.java");
const evolution = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const infection = read("src/main/java/alku/csrp/infection/InfectionMechanics.java");
const overlast = read("src/main/java/alku/csrp/event/OverlastEvents.java");

for (const [source, pattern, message] of [
    [rules, /START_PHASE = 8;/, "vanilla exclusion must start at phase eight"],
    [rules, /evolutionPhase\(\) >= START_PHASE/, "phase nine and ten must also exclude vanilla mobs"],
    [rules, /entity instanceof Mob && !\(entity instanceof Parasite\)/,
        "players, non-mob entities and parasites must be excluded from cleanup"],
    [rules, /getKey\(entity\.getType\(\)\)\.getNamespace\(\)\.equals\("minecraft"\)/,
        "other mods' mobs must not be removed"],
    [rules, /isVanillaMob\(entity\) && active\(level\)/,
        "spawn denial must depend on this dimension's phase"],
    [rules, /filterSpawnCandidates\(LevelEvent\.PotentialSpawns event\)/,
        "all vanilla biome spawn categories must be filtered"],
    [rules, /List\.copyOf\(event\.getSpawnerDataList\(\)\)/,
        "biome spawn filtering must safely modify a snapshot"],
    [rules, /preventSpawn\(MobSpawnEvent\.PositionCheck event\)[\s\S]*?Result\.FAIL/,
        "spawn position checks must reject vanilla mobs"],
    [rules, /preventJoining\(EntityJoinLevelEvent event\)[\s\S]*?event\.setCanceled\(true\)/,
        "join checks must cover eggs, spawners, breeding, commands and chunk loads"],
    [rules, /event\.getEntity\(\)\.discard\(\)/,
        "rejected vanilla mobs must be removed without death loot"],
    [rules, /clearExistingMobs\(LevelTickEvent\.Post event\)/,
        "already loaded vanilla mobs must be cleaned after the phase changes"],
    [rules, /level\.getAllEntities\(\)[\s\S]*?blocked\.add\(\(Mob\) entity\)/,
        "cleanup must include named, tamed and persistent vanilla mobs"],
    [rules, /for \(Mob mob : blocked\)[\s\S]*?mob\.discard\(\)/,
        "cleanup must remove mobs from a snapshot without killing them"],
    [rules, /InfectionMechanics\.revealHiddenAssimilated\(mob, null\)/,
        "hidden parasites must reveal their real body before vanilla cleanup"],
    [evolution, /parasite && phase == -2 \|\| HighPhaseVanillaMobRules\.isBlocked\(level, event\.getEntity\(\)\)/,
        "natural spawn gating must share the phase-eight vanilla rule"],
    [infection, /created instanceof Mob disguise[\s\S]*?HighPhaseVanillaMobRules\.isBlocked\(serverLevel, disguise\)/,
        "phase-eight parasites must not disguise as new vanilla hosts"],
    [overlast, /restoreHost\(ServerLevel level, LivingEntity parasite\)\s*\{\s*if \(HighPhaseVanillaMobRules\.active\(level\)\)\s*\{\s*return;/,
        "purification must not recreate vanilla hosts in high phases"]
]) expect(source, pattern, message);

for (const forbidden of ["hurt(", ".kill(", ".die(", "getChunk(", "MobCategory.MONSTER", "getSpawnType()", "isPersistenceRequired()", "isTame()", "hasCustomName()"] ) {
    if (rules.includes(forbidden)) {
        failures.push(`vanilla exclusion must not use ${forbidden}`);
    }
}

if (failures.length) {
    console.error("High-phase vanilla mob verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}
console.log("High-phase vanilla mobs verified: phases 8-10, all spawn paths, existing mobs, and parasite/non-mob exemptions.");
