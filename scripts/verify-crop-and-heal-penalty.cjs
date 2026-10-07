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
const config = read("src/main/java/alku/csrp/Config.java");
const events = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const evolution = read("src/main/java/alku/csrp/world/EvolutionSystem.java");

// ---- crop growth suppression (original SRPEventHandlerBus#cropGrow) ----
// SRPConfigWorld defaults: nodeCropStopNodeOne/Two/Three = 0.3 / 0.6 / 1.0.
expect(worldConfig, /\.defineInRange\("nodeCropStopNodeOne", 0\.3D, 0\.0D, 1\.0D\)/,
        "nodeCropStopNodeOne default of 0.3 is missing");
expect(worldConfig, /\.defineInRange\("nodeCropStopNodeTwo", 0\.6D, 0\.0D, 1\.0D\)/,
        "nodeCropStopNodeTwo default of 0.6 is missing");
expect(worldConfig, /\.defineInRange\("nodeCropStopNodeThree", 1\.0D, 0\.0D, 1\.0D\)/,
        "nodeCropStopNodeThree default of 1.0 is missing");
expect(worldConfig, /public static float nodeCropStopChance\(int nodeAge\)/,
        "nodeCropStopChance accessor is missing");
// The original checks the node stage first and only then the phase table.
expect(events, /int nodeAge = data\.nearestHeartAge\(event\.getPos\(\)\);/,
        "slowCropGrowth no longer reads the nearest node stage");
expect(events, /WorldConfig\.nodeCropStopChance\(nodeAge\)/,
        "slowCropGrowth no longer applies nodeCropStopNodeOne/Two/Three");
// Original cropGrowStunnedOne..Eight: 0,0,0,0,0,0.1,0.3,0.6; Nine/Ten are dead config.
expect(evolution, /0\.0F, 0\.0F, 0\.0F, 0\.0F, 0\.0F, 0\.0F, 0\.10F, 0\.30F, 0\.60F, 0\.0F, 0\.0F/,
        "the evolution-phase crop suppression table no longer matches cropGrowStunnedOne..Eight");

// ---- biome heal penalty (original SRPEventHandlerBus#entityHeal) ----
// SRPConfigWorld defaults: biomeOne..FourHealPenalty = 0.5, blacklist {"minecraft:villager_golem"}.
expect(worldConfig, /\.defineInRange\("biomeOneHealPenalty", 0\.5D, 0\.0D, 1000\.0D\)/,
        "biomeOneHealPenalty default of 0.5 is missing");
expect(worldConfig, /\.defineInRange\("biomeTwoHealPenalty", 0\.5D, 0\.0D, 1000\.0D\)/,
        "biomeTwoHealPenalty default of 0.5 is missing");
expect(worldConfig, /\.defineInRange\("biomeThreeHealPenalty", 0\.5D, 0\.0D, 1000\.0D\)/,
        "biomeThreeHealPenalty default of 0.5 is missing");
expect(worldConfig, /\.defineInRange\("biomeFourHealPenalty", 0\.5D, 0\.0D, 1000\.0D\)/,
        "biomeFourHealPenalty default of 0.5 is missing");
expect(worldConfig, /List\.of\("minecraft:villager_golem"\)/,
        "biomeHealPenaltyBlackList default of minecraft:villager_golem is missing");
expect(worldConfig, /public static float biomeHealPenalty\(int biome\)/,
        "biomeHealPenalty accessor is missing");
expect(worldConfig, /return listed != BIOME_HEAL_PENALTY_BLACKLIST_INVERTED\.get\(\);/,
        "biomeHealPenaltyExempt no longer reproduces checkName's anyMatch != isWhitelist");
// Original SRPConfigSystems.evolutionNoParasiteHealing = 9, value = 0.5.
expect(config, /\.defineInRange\("evolutionNoParasiteHealing", 9, -2, 10\)/,
        "evolutionNoParasiteHealing default of 9 is missing");
expect(config, /\.defineInRange\("evolutionNoParasiteHealingValue", 0\.5D, 0\.0D, 1000\.0D\)/,
        "evolutionNoParasiteHealingValue default of 0.5 is missing");
expect(events, /WorldConfig\.biomeHealPenalty\(biome\)/,
        "halveNonParasiteHealing no longer applies the biome heal penalty");
expect(events, /WorldConfig\.biomeHealPenaltyExempt\(id\)/,
        "halveNonParasiteHealing no longer honours the biome heal-penalty blacklist");
expect(events, /Config\.evolutionNoParasiteHealing\(\)/,
        "halveNonParasiteHealing no longer uses the configurable no-heal phase");
expect(events, /Config\.evolutionNoParasiteHealingValue\(\)/,
        "halveNonParasiteHealing no longer uses the configurable no-heal value");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("crop growth + biome heal penalty contract ok");
