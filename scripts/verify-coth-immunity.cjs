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
expect(immunity, /BuiltInRegistries\.ENTITY_TYPE\.getKey\(entity\.getType\(\)\)[\s\S]*anyMatch\(entityId::contains\)[\s\S]*listed != Config\.cothImmuneListInverted\(\)/,
        "entity id and namespace immunity matching is incomplete");
expect(tick, /else if \(!isCothImmune\(entity\)[\s\S]*COTH_REFRESH_THRESHOLD_TICKS[\s\S]*forceAddEffect/,
        "immune hosts can still advance COTH when the current effect expires");
expect(tick, /if \(Config\.disloCothIgnoreAmplifier\(\)[\s\S]*forceAddEffect[\s\S]*else if \(!isCothImmune\(entity\)/,
        "forced Dislodgment COTH no longer bypasses normal immunity");
expect(tick, /effectiveAmplifier == COTH_INCOMPLETE_AMPLIFIER[\s\S]*belowConversionHealth[\s\S]*!isCothImmune\(entity\)[\s\S]*convertIncompleteCothHost/,
        "low-health COTH II conversion does not honor immunity");
expect(tick, /effectiveAmplifier >= COTH_MAX_AMPLIFIER[\s\S]*convertInfectedHost\(entity\)/,
        "COTH III conversion is incorrectly blocked by immunity");
expect(incomplete, /isCothImmune\(host\)/,
        "an alternate COTH II conversion entry point can bypass immunity");
expect(tick, /isCothImmune\(entity\)[\s\S]*coth\.getDuration\(\)/,
        "immunity removes the active COTH effect instead of letting it expire");

if (failures.length) {
    console.error("COTH immunity verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("COTH immunity verification passed.");
