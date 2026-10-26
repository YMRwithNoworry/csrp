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

function method(content, start, end) {
    const startIndex = content.indexOf(start);
    const endIndex = content.indexOf(end, startIndex + start.length);
    return startIndex >= 0 && endIndex > startIndex ? content.slice(startIndex, endIndex) : "";
}

const config = read("src/main/java/alku/csrp/Config.java");
const infection = read("src/main/java/alku/csrp/infection/InfectionMechanics.java");
const spawnEvents = read("src/main/java/alku/csrp/event/CothSpawnEvents.java");
const infectionEvents = read("src/main/java/alku/csrp/infection/InfectionEvents.java");
const pop = method(infection, "public static boolean popCothOnPlayerDeath", "private static boolean passesCothKillConversion");
const immunity = method(infection, "public static boolean isCothImmune", "public static void tickCoth");
const tick = method(infection, "public static void tickCoth", "/** COTH II creates an Incomplete Form");
const incomplete = method(infection, "private static boolean convertIncompleteCothHost",
        "public static boolean convertInfectedHost");

expect(config, /defineList\("cothImmuneEntities",\s*List\.of\([\s\S]*"minecraft:iron_golem"[\s\S]*"minecraft:snow_golem"[\s\S]*"wyrmsofnyrus"[\s\S]*"srrevenants"/,
        "default COTH immunity list is incomplete");
expect(config, /define\("cothImmuneListInverted", false\)/,
        "COTH immunity list inversion option is missing");
expect(config, /cothImmuneEntities\(\)[\s\S]*COTH_IMMUNE_ENTITIES\.get\(\)/,
        "COTH immunity list is not exposed");
expect(config, /cothImmuneListInverted\(\)[\s\S]*COTH_IMMUNE_LIST_INVERTED\.get\(\)/,
        "COTH immunity inversion setting is not exposed");
expect(immunity, /hasCothImmunityKey\(entity\)[\s\S]*cothImmunityKey\(entity\) <= 0[\s\S]*BuiltInRegistries\.ENTITY_TYPE\.getKey\(entity\.getType\(\)\)[\s\S]*anyMatch\(entityId::contains\)[\s\S]*return listed != Config\.cothImmuneListInverted\(\)/,
        "persistent COTH stage and entity id/namespace immunity matching are incomplete");
expect(spawnEvents, /if \(!\(entity instanceof Parasite\)\)\s*\{\s*InfectionMechanics\.initializeCothImmunity\(entity\);/,
        "non-parasite spawns do not initialize the original COTH immunity key");
expect(tick, /if \(isCothImmune\(entity\)\)\s*\{\s*entity\.removeEffect\(ModMobEffects\.COTH\);\s*return;/,
        "immune hosts are not immediately cleared from COTH");
expect(tick, /amplifier > 0 && !\(entity instanceof Player\) && !hasCothImmunityKey\(entity\)[\s\S]*removeEffect\(ModMobEffects\.COTH\)/,
        "tagless non-player COTH effects do not follow the original expiry/removal behavior");
expect(tick, /if \(Config\.disloCothIgnoreAmplifier\(\)[\s\S]*forceAddEffect[\s\S]*cothImmunityKey\(entity\) > 0\s*&&\s*advanceCothImmunityKey\(entity\) >= 3/,
        "forced Dislodgment COTH no longer advances an eligible immunity key");
expect(tick, /coth\.getDuration\(\) > 0[\s\S]*coth\.getDuration\(\) < COTH_REFRESH_THRESHOLD_TICKS[\s\S]*forceAddEffect/,
        "COTH expiry no longer enters the original strict refresh path");
expect(tick, /belowConversionHealth \|\| cothImmunityKey\(entity\) > 1[\s\S]*effectiveAmplifier == COTH_INCOMPLETE_AMPLIFIER[\s\S]*advanceCothImmunityKey\(entity\) >= 3[\s\S]*convertIncompleteCothHost/,
        "low-health COTH II conversion does not honor the original immunity-key progression");
expect(tick, /effectiveAmplifier >= COTH_MAX_AMPLIFIER[\s\S]*advanceCothImmunityKey\(entity\) >= 3[\s\S]*convertInfectedHost/,
        "COTH III conversion does not honor the original immunity-key progression");
expect(incomplete, /isCothImmune\(host\)/,
        "an alternate COTH II conversion entry point can bypass immunity");
expect(config, /define\("cothPopping", false\)/,
        "the original disabled-by-default COTHPopping option is missing");
expect(pop, /!Config\.cothPlayer\(\) \|\| !Config\.cothPopping\(\)[\s\S]*coth\.getAmplifier\(\) < COTH_MAX_AMPLIFIER[\s\S]*serverLevel\.isEmptyBlock\(position\)[\s\S]*boneMealEffect\(serverLevel, position, Direction\.DOWN\)[\s\S]*removeEffect\(ModMobEffects\.COTH\)/,
        "COTHPopping does not reproduce player COTH III death behavior");
expect(infectionEvents, /popCothOnPlayerDeath\(player\)/,
        "player death does not invoke COTHPopping");

if (failures.length) {
    console.error("COTH immunity verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("COTH immunity verification passed.");
