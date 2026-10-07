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

const config = read("src/main/java/alku/csrp/Config.java");
const events = read("src/main/java/alku/csrp/event/CothItemPreventEvents.java");

// Original SRPConfigSystems.COTHItemPrevent (out109 124):
// {"minecraft:golden_apple;0.1;300", "minecraft:golden_carrot;0.03;150"}.
expect(config, /"minecraft:golden_apple;0\.1;300"/,
        "the golden apple COTH item default is missing");
expect(config, /"minecraft:golden_carrot;0\.03;150"/,
        "the golden carrot COTH item default is missing");
expect(config, /public static List<\? extends String> cothItemPrevent\(\)/,
        "cothItemPrevent accessor is missing");

// Original SRPEventHandlerBus#itemEntity: consume + chance unless creative, always apply in creative.
expect(events, /class CothItemPreventEvents/,
        "CothItemPreventEvents is missing");
expect(events, /PlayerInteractEvent\.EntityInteract event/,
        "the entity-interact handler is missing");
expect(events, /Config\.cothItemPrevent\(\)/,
        "the handler no longer reads the configured items");
expect(events, /player\.getAbilities\(\)\.instabuild/,
        "the creative branch is missing");
expect(events, /stack\.shrink\(1\)/,
        "the non-creative item consumption is missing");
expect(events, /player\.getRandom\(\)\.nextDouble\(\) < chance/,
        "the configured chance roll is missing");
expect(events, /new MobEffectInstance\(ModMobEffects\.REPEL, seconds \* 20, 0\)/,
        "the Repel (original EPEL_E) application is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("COTH item prevent contract ok");
