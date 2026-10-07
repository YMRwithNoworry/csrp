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
const status = read("src/main/java/alku/csrp/event/StatusEffectEvents.java");
const use = read("src/main/java/alku/csrp/event/FearUseEvents.java");
const gui = read("src/main/java/alku/csrp/client/FearGuiEvents.java");

// Original SRPConfigSystems defaults (out109 78-86): fearActive true, fearUnfair true,
// fearFallDamage 1.5, fearAirDamage 1.5, fearBlockChance/fearItemChance/fearInvChance 0.0.
expect(config, /\.define\("fearActive", true\)/, "fearActive default is missing");
expect(config, /\.define\("fearUnfair", true\)/, "fearUnfair default is missing");
expect(config, /\.defineInRange\("fearFallDamage", 1\.5D, 0\.0D, 100\.0D\)/, "fearFallDamage default of 1.5 is missing");
expect(config, /\.defineInRange\("fearAirDamage", 1\.5D, 0\.0D, 100\.0D\)/, "fearAirDamage default of 1.5 is missing");
expect(config, /\.defineInRange\("fearBlockChance", 0\.0D, 0\.0D, 1\.0D\)/, "fearBlockChance default of 0.0 is missing");
expect(config, /\.defineInRange\("fearItemChance", 0\.0D, 0\.0D, 1\.0D\)/, "fearItemChance default of 0.0 is missing");
expect(config, /\.defineInRange\("fearInvChance", 0\.0D, 0\.0D, 1\.0D\)/, "fearInvChance default of 0.0 is missing");
expect(config, /\.defineList\("fearItemBlackList", List\.of\(\)/, "the empty fearItemBlackList default is missing");
expect(config, /public static boolean fearItemExempt\(String itemId\)/,
        "fearItemExempt accessor is missing");
expect(config, /return listed != FEAR_ITEM_BLACKLIST_INVERTED\.get\(\);/,
        "fearItemExempt no longer reproduces checkName's anyMatch != isWhitelist");

// Original SRPEventHandlerBus#mobFear: fall branch, then fearUnfair / true-airborne air branch.
expect(status, /Config\.fearActive\(\) && fear != null/,
        "the fear damage multiplier no longer honours fearActive");
expect(status, /Config\.fearFallDamage\(\) \* fearAmp/,
        "the configurable fear fall multiplier is missing");
expect(status, /Config\.fearUnfair\(\)/,
        "the fearUnfair branch is missing");
expect(status, /victim\.isInWater\(\) \|\| victim\.isInLava\(\)/,
        "the fluid/support check for the fair-airborne branch is missing");
expect(status, /victim\.isFallFlying\(\)/,
        "the elytra airborne check is missing");
expect(status, /victim\.fallDistance >= 2\.0F/,
        "the original 2.0 fall-distance airborne threshold is missing");

// Original #itemPlayer / #blockPlayer.
expect(use, /LivingEntityUseItemEvent\.Start event/,
        "the Fear item-use handler is missing");
expect(use, /fear\.getAmplifier\(\) < 1/,
        "the Fear I+ gate for item use is missing");
expect(use, /Config\.fearItemExempt\(itemId\)/,
        "the Fear item blacklist is missing");
expect(use, /event\.setDuration\(-1\)/,
        "the original setDuration(-1) is missing");
expect(use, /PlayerInteractEvent\.RightClickBlock event/,
        "the Fear block-use handler is missing");
expect(use, /amplifier >= 2 && player\.getRandom\(\)\.nextDouble\(\) < Config\.fearBlockChance\(\)/,
        "the Fear II+ block failure is missing");
expect(use, /amplifier >= 1 && player\.getRandom\(\)\.nextDouble\(\) < Config\.fearItemChance\(\)/,
        "the Fear I+ item failure is missing");
expect(use, /event\.setUseBlock\(TriState\.FALSE\)/, "the block denial is missing");
expect(use, /event\.setUseItem\(TriState\.FALSE\)/, "the item denial is missing");

// Original #guiPlayer (client-only).
expect(gui, /ScreenEvent\.Opening event/,
        "the Fear GUI handler is missing");
expect(gui, /fear\.getAmplifier\(\) < 3/,
        "the Fear IV+ gate for the GUI failure is missing");
expect(gui, /Config\.fearInvChance\(\)/,
        "the fearInvChance roll is missing");
expect(gui, /isPauseScreen\(\)/,
        "the original doesGuiPauseGame() exclusion is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("fear effect contract ok");
