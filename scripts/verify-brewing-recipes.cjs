const fs = require("node:fs");
const path = require("node:path");

// The five brewing recipes of 1.10.9 potion/Recipe.java are data driven on 26.3
// (NeoForge removed IBrewingRecipe and RegisterBrewingRecipesEvent).  They must stay present: a
// malformed or deleted JSON silently removes the fear potion chain, the deadblood conversion and the
// thornshade decanter from the game.
const root = path.resolve(__dirname, "..");
const BREWING = path.join(root, "src/main/resources/data/csrp/recipe/brewing");
const failures = [];

function readJson(file) {
    if (!fs.existsSync(file)) {
        failures.push(`missing brewing recipe ${path.relative(root, file).replace(/\\/g, "/")}`);
        return null;
    }
    try {
        return JSON.parse(fs.readFileSync(file, "utf8"));
    } catch (error) {
        failures.push(`${path.basename(file)} is not valid JSON: ${error.message}`);
        return null;
    }
}

function potionOf(recipe) {
    const components = recipe?.output?.components;
    return components?.["minecraft:potion_contents"]?.potion;
}

const expected = [
    // 1.10.9 potion/Recipe.java: BaseToFearRecipe(alveolar, FEAR) — reagent Items.field_151145_ak,
    // which the official 1.12.2 MCP mappings resolve to Items.FLINT.
    { file: "fear_potion.json", input: "csrp:alveolar_fluid", reagent: "minecraft:flint", output: "minecraft:potion", potion: "csrp:fear" },
    // FearToSplashRecipe(FEAR) — reagent Items.field_151016_H = GUNPOWDER.
    { file: "fear_splash_potion.json", input: "minecraft:potion", inputPotion: "csrp:fear", reagent: "minecraft:gunpowder", output: "minecraft:splash_potion", potion: "csrp:fear" },
    // FearSplashToLingeringRecipe(FEAR) — reagent Items.field_185157_bK = DRAGON_BREATH.
    { file: "fear_lingering_potion.json", input: "minecraft:splash_potion", inputPotion: "csrp:fear", reagent: "minecraft:dragon_breath", output: "minecraft:lingering_potion", potion: "csrp:fear" },
    // SpongeToDeadbloodRecipe(diseasedSponge, deadblood, WATER, AWKWARD).
    { file: "deadblood_from_water.json", input: "minecraft:potion", inputPotion: "minecraft:water", reagent: "csrp:diseased_sponge", output: "csrp:deadblood_fluid" },
    { file: "deadblood_from_awkward.json", input: "minecraft:potion", inputPotion: "minecraft:awkward", reagent: "csrp:diseased_sponge", output: "csrp:deadblood_fluid" },
    // BerryToThornshadeDecanterRecipe(thornshadeBerry, thornshadeDecanter, WATER, AWKWARD).
    { file: "thornshade_decanter_from_water.json", input: "minecraft:potion", inputPotion: "minecraft:water", reagent: "csrp:thornshade_berry", output: "csrp:thornshade_decanter" },
    { file: "thornshade_decanter_from_awkward.json", input: "minecraft:potion", inputPotion: "minecraft:awkward", reagent: "csrp:thornshade_berry", output: "csrp:thornshade_decanter" }
];

for (const spec of expected) {
    const recipe = readJson(path.join(BREWING, spec.file));
    if (recipe === null) continue;
    if (recipe.type !== "minecraft:brewing") {
        failures.push(`${spec.file} has type ${recipe.type}, expected minecraft:brewing`);
    }
    if (recipe.input?.item !== spec.input) {
        failures.push(`${spec.file} input item is ${recipe.input?.item}, expected ${spec.input}`);
    }
    const inputPotion = recipe.input?.potion_contents?.potions;
    if (spec.inputPotion !== undefined && inputPotion !== spec.inputPotion) {
        failures.push(`${spec.file} input potion is ${inputPotion}, expected ${spec.inputPotion}`);
    }
    if (recipe.reagent?.item !== spec.reagent) {
        failures.push(`${spec.file} reagent is ${recipe.reagent?.item}, expected ${spec.reagent}`);
    }
    if (recipe.output?.id !== spec.output) {
        failures.push(`${spec.file} output is ${recipe.output?.id}, expected ${spec.output}`);
    }
    if (spec.potion !== undefined && potionOf(recipe) !== spec.potion) {
        failures.push(`${spec.file} output potion is ${potionOf(recipe)}, expected ${spec.potion}`);
    }
}

// Nothing else may claim to be a brewing recipe we own.
const present = fs.existsSync(BREWING) ? fs.readdirSync(BREWING).filter((n) => n.endsWith(".json")) : [];
if (present.length !== expected.length) {
    failures.push(`expected ${expected.length} brewing recipe files, found ${present.length}: ${present.join(", ")}`);
}

// The empty NEEDS-DESIGN stub must be gone: the behaviour is data driven now.
if (fs.existsSync(path.join(root, "src/main/java/alku/csrp/event/ThornshadeBrewingEvents.java"))) {
    failures.push("the empty ThornshadeBrewingEvents stub is still present");
}

// The runtime test that loads these through the real RecipeManager must exist.
const test = path.join(root, "src/main/java/alku/csrp/gametest/BrewingRecipeGameTests.java");
if (!fs.existsSync(test)) {
    failures.push("BrewingRecipeGameTests is missing");
} else {
    const source = fs.readFileSync(test, "utf8");
    if (!source.includes("brewing_recipes/legacy_srp_brews")) {
        failures.push("the brewing recipe GameTest is no longer registered");
    }
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log(`brewing recipes ok (${expected.length} recipes)`);
