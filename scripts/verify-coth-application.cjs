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

const events = read("src/main/java/alku/csrp/infection/InfectionEvents.java");
const infection = read("src/main/java/alku/csrp/infection/InfectionMechanics.java");
const config = read("src/main/java/alku/csrp/Config.java");
const hit = method(events, "public static void infectFromParasiteHit", "public static void preventParasiteTargeting");
const ordinaryCoth = method(infection, "public static void applyCoth(LivingEntity target, Entity source, int", "/**\n     * Merges a requested COTH");
const exactCoth = method(infection, "public static void applyCothEffect(LivingEntity target, Entity source, int durationTicks, int amplifier,", "/** Original per-tier chance");
const spreadChance = method(infection, "public static double cothSpreadChance", "private static void playInfectionSound");

expect(events, /infectFromParasiteHit\(LivingDamageEvent\.Post event\)/,
        "parasite-hit infection is not applied after final damage");
expect(hit, /event\.getNewDamage\(\) <= 0\.0F[\s\S]*!target\.isAlive\(\)/,
        "zero-damage or fatal hits can still apply a fresh COTH effect");
expect(hit, /attacker instanceof Parasite\s*&&\s*!target\.hasEffect\(ModMobEffects\.COTH\)/,
        "parasite hits can overwrite an existing COTH effect");
expect(hit, /InfectionMechanics\.cothSpreadChance\(attacker\)/,
        "parasite hits do not use per-tier COTH spread chances");
expect(hit, /getAmplifier\(\) >= InfectionMechanics\.COTH_INCOMPLETE_AMPLIFIER[\s\S]*COTH_CONVERSION_HEALTH_FRACTION[\s\S]*convertCothHost/,
        "damage does not immediately trigger the matching COTH II or III conversion");
if (/generationProfile/.test(hit)) {
    failures.push("parasite-hit infection still mistakes generation stat scaling for COTH spread chance");
}

expect(ordinaryCoth, /MobEffectInstance existing = target\.getEffect\(ModMobEffects\.COTH\);[\s\S]*if \(existing != null\)\s*\{\s*return;/,
        "ordinary COTH application can still replace an existing potion or command effect");
expect(exactCoth, /Math\.max\(durationTicks, existing\.getDuration\(\)\)/,
        "skill COTH can shorten an existing effect");
expect(exactCoth, /Math\.max\(amplifier, existing\.getAmplifier\(\)\)/,
        "skill COTH can lower an existing effect level");

for (const [getter, value] of [
    ["cothAssimilatedSpreadChance", "0.1D"],
    ["cothHijackedSpreadChance", "0.05D"],
    ["cothFeralSpreadChance", "0.2D"],
    ["cothCrudeSpreadChance", "0.4D"],
    ["cothPrimitiveSpreadChance", "0.5D"],
    ["cothAdaptedSpreadChance", "0.6D"],
    ["cothPureSpreadChance", "0.8D"]
]) {
    expect(config, new RegExp(`defineInRange\\("${getter}", ${value.replace(".", "\\.")}`),
            `${getter} does not use the original default`);
    expect(spreadChance, new RegExp(`Config\\.${getter}\\(\\)`),
            `${getter} is not used by parasite tier classification`);
}

if (failures.length) {
    console.error("COTH application verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("COTH application verification passed.");
