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

function expect(content, pattern, description) {
    if (!pattern.test(content)) failures.push(description);
}

const blood = read("src/main/java/alku/csrp/block/DeadBloodBlock.java");
const pots = read("src/main/java/alku/csrp/event/PottedFlowerEvents.java");
const bloodTests = read("src/main/java/alku/csrp/gametest/DeadBloodBottleGameTests.java");
const potTests = read("src/main/java/alku/csrp/gametest/PottedFlowerGameTests.java");

expect(blood, /hand == InteractionHand\.MAIN_HAND[\s\S]*stack\.is\(Items\.GLASS_BOTTLE\)[\s\S]*state\.getFluidState\(\)\.getType\(\) instanceof DeadBloodFluid/,
        "Dead Blood bottle collection no longer accepts source and flowing states of the registered fluid");
expect(blood, /stack\.shrink\(1\)[\s\S]*player\.setItemInHand\(hand, filled\)[\s\S]*player\.getInventory\(\)\.add\(filled\)[\s\S]*player\.drop\(filled, false, Prediction\.SERVER_ONLY\)/,
        "Dead Blood bottle collection does not preserve survival, stacked-bottle, and full-inventory results");
expect(blood, /SoundEvents\.BOTTLE_FILL[\s\S]*SoundSource\.PLAYERS/,
        "Dead Blood bottle collection no longer plays the original player bottle-fill sound");
expect(pots, /event\.getFace\(\) == Direction\.UP[\s\S]*Blocks\.FLOWER_POT[\s\S]*ModItems\.ASSIMILATED_BLOSSOM/,
        "assimilated blossom potting no longer requires a non-top click on an empty vanilla flower pot");
expect(pots, /ModBlocks\.legacyBlock\("potted_assimilated_blossom"\)[\s\S]*getAbilities\(\)\.instabuild[\s\S]*shrink\(1\)/,
        "assimilated blossom potting no longer places the registered pot block and consumes only in survival");
expect(pots, /setCancellationResult[\s\S]*event\.setCanceled\(true\)/,
        "successful assimilated blossom potting no longer consumes the interaction");
expect(bloodTests, /sourcePos[\s\S]*flowingPos[\s\S]*GameType\.CREATIVE/,
        "Dead Blood GameTest no longer covers source, flowing, and creative collection");
expect(potTests, /SURVIVAL_POT[\s\S]*CREATIVE_POT[\s\S]*TOP_POT[\s\S]*FULL_POT/,
        "flower-pot GameTest no longer covers survival, creative, top-face, and occupied-pot behavior");

if (failures.length) {
    console.error("Bottle and potted-flower interaction verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Bottle and potted-flower interaction verification passed.");
