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

const worldConfig = read("src/main/java/alku/csrp/config/WorldConfig.java");
const core = read("src/main/java/alku/csrp/world/SrpCoreSystems.java");

// Original SRPConfigWorld origin / EIV defaults (out109 util/config/SRPConfigWorld.java:608-622).
expect(worldConfig, /\.define\("originActivated", true\)/,
        "originActivated default of true is missing");
expect(worldConfig, /\.defineInRange\("originCreatingRand", 10, 0, 1000000\)/,
        "originCreatingRand default of 10 is missing");
expect(worldConfig, /\.defineInRange\("originCreatingRandZero", 3, 0, 1000000\)/,
        "originCreatingRandZero default of 3 is missing");
expect(worldConfig, /\.defineInRange\("originCreatingDistanceMin", 1000, 0, 100000000\)/,
        "originCreatingDistanceMin default of 1000 is missing");
expect(worldConfig, /\.defineInRange\("originCreatingDistanceMax", 10000, 0, 100000000\)/,
        "originCreatingDistanceMax default of 10000 is missing");
expect(worldConfig, /\.defineInRange\("originHealth", 350, 0, 2000000000\)/,
        "originHealth default of 350 is missing");
expect(worldConfig, /\.defineInRange\("originRadius", 200, 0, 2000000000\)/,
        "originRadius default of 200 is missing");
for (const accessor of ["originActivated", "originCreatingRand", "originCreatingRandZero",
        "originCreatingDistanceMin", "originCreatingDistanceMax", "originHealth", "originRadius"]) {
    expect(worldConfig, new RegExp(`public static (boolean|int) ${accessor}\\(\\)`),
            `WorldConfig.${accessor}() accessor is missing`);
}

// Original EIVUtil#createRandomOrigin: once-a-day roll, random player, random angle and distance,
// position = player + (dx, 64, dz), then placeVector with originHealth / originRadius.
expect(core, /public static void createRandomOrigin\(ServerLevel level\)/,
        "createRandomOrigin is missing");
expect(core, /data\.vectors\(\)\.isEmpty\(\) \? WorldConfig\.originCreatingRandZero\(\) : WorldConfig\.originCreatingRand\(\)/,
        "the first-origin vs existing-origin chance split is missing");
expect(core, /level\.getRandom\(\)\.nextInt\(chance\) != 0/,
        "the 1-in-N origin roll is missing");
expect(core, /level\.getRandom\(\)\.nextInt\(players\.size\(\)\)/,
        "the random-player pick is missing");
expect(core, /double angle = level\.getRandom\(\)\.nextDouble\(\) \* Math\.PI \* 2\.0D;/,
        "the random-angle calculation is missing");
expect(core, /player\.blockPosition\(\)\.offset\(dx, 64, dz\)/,
        "the original player + (dx, 64, dz) position is missing");
expect(core, /placeVector\(level, pos, WorldConfig\.originHealth\(\), WorldConfig\.originRadius\(\)\)/,
        "createRandomOrigin no longer creates the vector through placeVector");
expect(core, /growVectors\(level, data\);\s*createRandomOrigin\(level\);/s,
        "createRandomOrigin is not called from the daily tick");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("origin creation contract ok");
