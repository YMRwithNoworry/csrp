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

expect(evolution, /VALUE_KILL\s*=\s*1;/, "parasite kills do not award exactly one evolution point");
expect(evolution, /5_000_000,\s*25_000_000,\s*500_000_000/,
        "phase 7 threshold is not 25,000,000 evolution points");
expect(evolutionEvents, /int points = EvolutionSystem\.VALUE_KILL;/,
        "parasite kill event does not use the one-point value");
expect(killedHost, /attacker instanceof RupterEntity/,
        "Rupter kills are not guaranteed host conversions");
expect(killedHost, /attacker instanceof FeralParasiteEntity\s*&&\s*phase >= ASSIMILATION_FERAL_PHASE/,
        "phase 7 Feral kills are not guaranteed host conversions");
expect(killedHost, /!guaranteed\s*&&\s*!host\.hasEffect\(ModMobEffects\.COTH\)/,
        "ordinary parasite kills no longer require COTH");
expect(killedHost, /!guaranteed\s*&&[\s\S]*Config\.cothConvertAtKillChance\(\)/,
        "ordinary parasite kills no longer honor conversion chance");
expect(killedHost, /createMappedHost\(host, serverLevel,[\s\S]*phase >= ASSIMILATION_FERAL_PHASE\)/,
        "phase 7 kill conversion does not prefer mapped Feral forms");
expect(killedHost, /replaceHost\(host, converted, serverLevel, false\)/,
        "kill conversion still awards separate COTH points");
expect(infection, /"fer_" \+ targetId\.getPath\(\)\.substring\("sim_"\.length\(\)\)/,
        "sim-to-Feral entity id mapping is missing");
if (/VALUE_COTH|PointSource\.COTH/.test(killedHost) || /VALUE_COTH|PointSource\.COTH/.test(killedPlayer)) {
    failures.push("kill conversion still adds six COTH points on top of the one kill point");
}

if (failures.length) {
    console.error("Infection conversion verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Infection conversion verification passed.");
