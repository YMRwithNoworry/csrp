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

const events = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const evolution = read("src/main/java/alku/csrp/world/EvolutionSystem.java");

// Original SRPConfigSystems sleepPenaltyZero..Ten (out109 244-820):
// 3, 10, 25, 50, 100, 2500, 8500, 12500, 15000, 18000, 1.
expect(evolution, /SLEEP_POINTS = \{3, 10, 25, 50, 100, 2_500, 8_500, 12_500, 15_000, 18_000, 1\}/,
        "the sleep point table no longer matches sleepPenaltyZero..Ten");
// Original SRPEventHandlerBus#playerUp: bonus 5 from evolutionSleepDenied (6) onward.
expect(evolution, /return phase >= 6 \? points \* 5 : points;/,
        "the evolutionSleepDenied (phase 6) x5 bonus is missing");
// Original only penalised waking while getWorldTime() % 24000 < 13000.
expect(events, /Math\.floorMod\(level\.getOverworldClockTime\(\), 24_000L\) < 13_000L/,
        "addSleepPoints no longer checks the original 13000-tick day window");
expect(events, /EvolutionSystem\.addPoints\(level, points, EvolutionSystem\.PointSource\.SLEEP\)/,
        "addSleepPoints no longer awards the sleep penalty");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("sleep penalty contract ok");
