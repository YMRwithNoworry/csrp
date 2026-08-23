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

const infection = read("src/main/java/alku/csrp/infection/InfectionMechanics.java");
const evolution = read("src/main/java/alku/csrp/world/EvolutionSystem.java");
const evolutionEvents = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const killedHost = method(infection, "public static boolean convertKilledHost", "private static Mob createIncompleteForm");
const killedPlayer = method(infection, "public static boolean convertKilledPlayer", "private static void spreadCoth");
const killConversionRoll = method(infection, "private static boolean passesCothKillConversion", "private static void spreadCoth");
const replaceHost = method(infection, "private static boolean replaceHost", "/** Converts a COTH victim");

expect(evolution, /VALUE_KILL\s*=\s*1;/, "parasite kills do not award exactly one evolution point");
expect(evolution, /5_000_000,\s*25_000_000,\s*500_000_000/,
        "phase 7 threshold is not 25,000,000 evolution points");
expect(evolutionEvents, /int points = EvolutionSystem\.VALUE_KILL;/,
        "parasite kill event does not use the one-point value");
expect(killedHost, /attacker instanceof RupterEntity/,
        "Rupter kills are not guaranteed host conversions");
expect(killedHost, /attacker instanceof FeralParasiteEntity\s*&&\s*phase >= ASSIMILATION_FERAL_PHASE/,
        "phase 7 Feral kills are not guaranteed host conversions");
expect(killedHost, /!guaranteed\s*&&\s*!passesCothKillConversion\(host\)/,
        "ordinary parasite kills no longer require COTH");
expect(killedPlayer, /!guaranteed\s*&&\s*!passesCothKillConversion\(player\)/,
        "player parasite-kill conversion does not use the shared COTH roll");
expect(killConversionRoll, /getEffect\(ModMobEffects\.COTH(?:\.get\(\))?\)[\s\S]*if \(coth == null\)[\s\S]*return false;/,
        "ordinary parasite kills no longer require pre-existing COTH");
expect(killConversionRoll, /Config\.cothConvertAtKillChance\(\)/,
        "ordinary parasite kills no longer honor the configured base conversion chance");
expect(killConversionRoll,
        /\(1\.0D - baseChance\) \* amplifier \/ COTH_MAX_AMPLIFIER/,
        "COTH level no longer increases parasite-kill conversion chance");
expect(killedHost, /createMappedHost\(host, serverLevel,[\s\S]*phase >= ASSIMILATION_FERAL_PHASE\)/,
        "phase 7 kill conversion does not prefer mapped Feral forms");
expect(killedHost, /return replaceHost\(host, converted, serverLevel\);/,
        "kill conversion does not use the shared successful-assimilation settlement");
expect(replaceHost,
        /getEffect\(ModMobEffects\.COTH(?:\.get\(\))?\)[\s\S]*getAmplifier\(\) >= COTH_MAX_AMPLIFIER/,
        "assimilation points are not restricted to COTH III hosts");
expect(replaceHost,
        /if \(!serverLevel\.addFreshEntity\(converted\)\)[\s\S]*return false;[\s\S]*if \(terminalCothAssimilation\)[\s\S]*VALUE_COTH/,
        "COTH III points are not awarded only after a successful assimilation");
expect(evolution, /VALUE_COTH\s*=\s*6;/,
        "COTH III assimilation does not award the original six additional points");
expect(infection, /"fer_" \+ targetId\.getPath\(\)\.substring\("sim_"\.length\(\)\)/,
        "sim-to-Feral entity id mapping is missing");
if (/VALUE_COTH|PointSource\.COTH/.test(killedHost) || /VALUE_COTH|PointSource\.COTH/.test(killedPlayer)) {
    failures.push("kill-specific conversion paths bypass the shared COTH III settlement");
}

if (failures.length) {
    console.error("Infection conversion verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Infection conversion verification passed.");
