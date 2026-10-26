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
    // The sources use CRLF; normalise so the LF-based section markers below can match.
    const normalised = content.replace(/\r\n/g, "\n");
    const startIndex = normalised.indexOf(start);
    const endIndex = normalised.indexOf(end, startIndex + start.length);
    return startIndex >= 0 && endIndex > startIndex ? normalised.slice(startIndex, endIndex) : "";
}

const events = read("src/main/java/alku/csrp/infection/InfectionEvents.java");
const infection = read("src/main/java/alku/csrp/infection/InfectionMechanics.java");
const config = read("src/main/java/alku/csrp/Config.java");
const hit = method(events, "public static void infectFromParasiteHit", "public static void preventParasiteTargeting");
const ordinaryCoth = method(infection, "public static void applyCoth(LivingEntity target, Entity source, int", "/**\n     * Merges a requested COTH");
const exactCoth = method(infection, "public static void applyCothEffect(LivingEntity target, Entity source, int durationTicks, int amplifier,", "/** Original per-tier chance");
const spreadChance = method(infection, "public static double cothSpreadChance", "private static void playInfectionSound");
const spread = method(infection, "private static void spreadCoth", "private static Mob createAssimilatedHost");

expect(events, /infectFromParasiteHit\(LivingDamageEvent\.Post event\)/,
        "parasite-hit infection is not applied after final damage");
// 26.3 exposes the post-mitigation float as getHealthDamage() (the 1.20.1 getNewDamage() name is gone).
expect(hit, /event\.getHealthDamage\(\) <= 0\.0F[\s\S]*!target\.isAlive\(\)/,
        "zero-damage or fatal hits can still apply a fresh COTH effect");
expect(hit, /attacker instanceof Parasite\s*&&\s*!target\.hasEffect\(ModMobEffects\.COTH\)/,
        "parasite hits can overwrite an existing COTH effect");
expect(hit, /InfectionMechanics\.cothSpreadChance\(attacker\)/,
        "parasite hits do not use per-tier COTH spread chances");
if (/convertCothHost|convertIncompleteCothHost|convertInfectedHost/.test(hit)) {
    failures.push("parasite-hit damage performs COTH conversion outside the periodic effect tick");
}
if (/generationProfile/.test(hit)) {
    failures.push("parasite-hit infection still mistakes generation stat scaling for COTH spread chance");
}

expect(ordinaryCoth, /MobEffectInstance existing = target\.getEffect\(ModMobEffects\.COTH\);[\s\S]*?if \(existing != null\) \{\r?\n\s*return;\r?\n\s*\}/,
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

expect(spread, /Config\.cothAura\(\)[\s\S]*new AABB\(source\.getX\(\), source\.getY\(\), source\.getZ\(\),\s*source\.getX\(\) \+ 1\.0D, source\.getY\(\) \+ 1\.0D, source\.getZ\(\) \+ 1\.0D\)\.inflate\(range\)/,
        "COTH spread no longer uses the original aura-expanded unit AABB");
expect(spread, /source\.hasLineOfSight\(target\)[\s\S]*!target\.hasEffect\(ModMobEffects\.COTH\)[\s\S]*!target\.hasEffect\(ModMobEffects\.REPEL\)[\s\S]*COTH_SPREAD_DURATION_TICKS/,
        "COTH spread no longer preserves line-of-sight, repel, or 4800-tick target rules");

if (failures.length) {
    console.error("COTH application verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("COTH application verification passed.");
