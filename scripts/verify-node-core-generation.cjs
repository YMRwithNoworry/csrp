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
    return fs.readFileSync(file, "utf8").replace(/\r\n/g, "\n");
}

function method(content, start, end) {
    const startIndex = content.indexOf(start);
    const endIndex = content.indexOf(end, startIndex + start.length);
    return startIndex >= 0 && endIndex > startIndex ? content.slice(startIndex, endIndex) : "";
}

function expect(content, pattern, description) {
    if (!pattern.test(content)) failures.push(description);
}

const systems = read("src/main/java/alku/csrp/world/SrpCoreSystems.java");
const placeNode = method(systems, "public static boolean placeNode", "public static boolean placeColony");
const checkCores = method(systems, "private static void checkCoresAndSpread", "private static void spawnArchitectIfMissing");
const dailyState = method(systems, "private static void advanceDailyState", "private static void growVectors");
const stageUpdate = method(systems, "static void updateNodeStage", "private static void updateActiveState");

expect(placeNode, /level\.setBlock\(pos,[\s\S]*?new WorldGenParasiteNodeCore\(1, type\)\.generate\(level, RandomSource\.create\(\), pos\)[\s\S]*?BlockInfestation\.infestAround/,
        "a newly placed node does not generate the original stage-1 core structure before biome infestation");
expect(checkCores, /updateNodeStage\(level, entry, stage\)/,
        "the periodic node reconciliation path does not generate structures on stage transitions");
expect(dailyState, /updateNodeStage\(level, entry, stage\)/,
        "the daily node-age progression path does not generate structures on stage transitions");
expect(stageUpdate, /int previousStage = state\.getValue\(SrpCoreBlock\.ACTIVE\)[\s\S]*stage <= previousStage[\s\S]*level\.setBlock\(pos,[\s\S]*if \(stage >= 2\)[\s\S]*new WorldGenParasiteNodeCore\(stage, entry\.type\(\)\)\.generate\(level, RandomSource\.create\(\), pos\.above\(\)\)/,
        "stage-2/3 core generation is not restricted to an increasing stage transition or uses the wrong height/type");

if (failures.length) {
    console.error("Node core generation verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Node core generation verification passed.");
