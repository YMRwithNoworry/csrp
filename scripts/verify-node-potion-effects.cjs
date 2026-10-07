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
const worldData = read("src/main/java/alku/csrp/world/SrpWorldData.java");
const colonyEvents = read("src/main/java/alku/csrp/world/ColonyEvents.java");

// Original SRPConfigWorld.potionEffectForNodes (out109 65):
// {"3;minecraft:speed;2", "4;minecraft:fire_resistance;1", "7;minecraft:invisibility;1"}.
expect(worldConfig, /"3;minecraft:speed;2"/, "the speed node effect is missing");
expect(worldConfig, /"4;minecraft:fire_resistance;1"/, "the fire resistance node effect is missing");
expect(worldConfig, /"7;minecraft:invisibility;1"/, "the invisibility node effect is missing");
expect(worldConfig, /public static List<\? extends String> nodePotionEffects\(\)/,
        "nodePotionEffects accessor is missing");

// Original SRPWorldData#totalNodePoints: sum of convertDayToAgeNode over every node.
expect(worldData, /public int totalNodePoints\(\)/,
        "totalNodePoints is missing");
expect(worldData, /points \+= nodeStageFor\(entry\.age\(\)\);/,
        "totalNodePoints no longer sums the node stages");

// Original SRPEventHandlerBus#applyNode: threshold gate + 7777 ticks.
expect(colonyEvents, /applyNodeEffects\(entity, data\.totalNodePoints\(\)\)/,
        "the node effect application is missing from the spawn bonuses");
expect(colonyEvents, /totalNodePoints < Integer\.parseInt\(parts\[0\]\.trim\(\)\)/,
        "the node point threshold gate is missing");
expect(colonyEvents, /new MobEffectInstance\(\s*BuiltInRegistries\.MOB_EFFECT\.wrapAsHolder\(effect\), 7777, amplifier\)/s,
        "the original 7777-tick node effect duration is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("node potion effect contract ok");
