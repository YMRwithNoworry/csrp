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

const events = read("src/main/java/alku/csrp/event/RicardoDeathEvents.java");
const lang = read("src/main/resources/assets/csrp/lang/en_us.json");

// Original SRPEventHandlerBus#onLivingDeath / #onWorldTick (out109 409-453).
expect(events, /class RicardoDeathEvents/,
        "RicardoDeathEvents is missing");
expect(events, /event\.getSource\(\)\.getEntity\(\) instanceof PrimitiveVariantEntity reeker/,
        "the killer is no longer checked for the Ricardo Reeker");
expect(events, /!reeker\.isRicardoVariant\(\)/,
        "the Ricardo variant check is missing");
expect(events, /rules\.set\(GameRules\.SHOW_DEATH_MESSAGES, false, level\.getServer\(\)\)/,
        "the vanilla death message is no longer suppressed");
expect(events, /Component\.translatable\("death\.attack\.csrp\.ricardo", player\.getDisplayName\(\)\)/,
        "the custom death message is missing");
expect(events, /level\.getGameRules\(\)\.set\(GameRules\.SHOW_DEATH_MESSAGES, true, level\.getServer\(\)\)/,
        "the game rule is no longer restored");
expect(events, /RULE_RESTORE\.put\(level\.dimension\(\), level\.getGameTime\(\) \+ 1L\)/,
        "the one-tick restore scheduling is missing");
expect(events, /LevelTickEvent\.Post event/,
        "the restore handler no longer runs on the level tick");

// The language key the original's TextComponentTranslation used.
expect(lang, /"death\.attack\.csrp\.ricardo"/,
        "the death.attack.csrp.ricardo language key is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("ricardo death message contract ok");
