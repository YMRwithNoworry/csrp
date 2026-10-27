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
const generator = read("src/main/java/alku/csrp/world/gen/WorldGenParasiteNodeCore.java");
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
expect(generator, /for \(int i = 0; i <= 3; i\+\+\) \{\s*BlockPos anchor = ParasiteGenContext\.horizontal\(position, i\);\s*placeGroundedTrunk\(level, anchor\);\s*placeGroundedBranches\(level, anchor, i\);\s*anchor = ParasiteGenContext\.horizontal\(anchor, i\);\s*placeGroundedTrunk\(level, anchor\);\s*placeGroundedBranches\(level, anchor, i\);\s*anchor = ParasiteGenContext\.horizontal\(anchor, i\);\s*placeGroundedTrunk\(level, anchor\);/,
        "stage-1 node arms no longer match the original main-arm and side-branch lengths");
expect(generator, /private static BlockPos placeColumn[\s\S]*for \(int i = 0; i < height; i\+\+\)[\s\S]*placeTen\(level, current\)[\s\S]*return current;/,
        "stage-2 node columns no longer include the original terminal feeler block");
expect(generator, /tendrilRun\(level, random, root, i, 2, false, true\)[\s\S]*tendrilRun\(level, random, root, i, 1, true, true\)/,
        "stage-2 node tendril branches no longer preserve the original extra-feeler distribution");
expect(generator, /isFullCube\(level, tip\.below\(\)\)/,
        "stage-2 node tendrils no longer use the original full-cube ground test");
expect(generator, /private static void placeGroundedTrunk\(ServerLevel level, BlockPos pos\) \{\s*if \(!ParasiteGenContext\.isFullCube\(level, pos\.below\(\)\)\)/,
        "stage-1 arms and stage perimeters no longer use the original full-cube ground test");

if (failures.length) {
    console.error("Node core generation verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Node core generation verification passed.");
