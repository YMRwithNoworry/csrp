const fs = require("node:fs");
const path = require("node:path");

// Port of 1.10.9 compatibility/jei/SRPJEIPlugin.  JEI is a compileOnly dependency, so this plugin is
// only loaded by JEI's own discovery and cannot be exercised by the GameTest server (JEI is client
// only).  These checks therefore pin the contract: the plugin annotation, the infuser furnace category
// with the original's slot layout and arrow, the catalyst, and the sixteen advancement icons the
// original hid.
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

const plugin = read("src/main/java/alku/csrp/compat/jei/SrpJeiPlugin.java");
expect(plugin, /@JeiPlugin/, "the JEI plugin is not annotated with @JeiPlugin");
expect(plugin, /implements IModPlugin/, "the JEI plugin no longer implements IModPlugin");
expect(plugin, /public Identifier getPluginUid\(\)/, "getPluginUid is missing");
expect(plugin, /registerCategories\(IRecipeCategoryRegistration registration\)/,
        "registerCategories is missing");
expect(plugin, /registerRecipes\(IRecipeRegistration registration\)/, "registerRecipes is missing");
expect(plugin, /registerRecipeCatalysts\(IRecipeCatalystRegistration registration\)/,
        "registerRecipeCatalysts is missing");
expect(plugin, /addRecipeCategories\(\s*new InfuserFurnaceJeiCategory\(/, "the infuser category is not registered");
expect(plugin, /addRecipes\(InfuserFurnaceJeiCategory\.RECIPE_TYPE, InfuserFurnaceRecipes\.all\(\)\)/,
        "the infuser recipes are not registered from the shared table");
expect(plugin, /addCraftingStation\(InfuserFurnaceJeiCategory\.RECIPE_TYPE,\s*ModBlocks\.INFUSER_FURNACE\.get\(\)\)/,
        "the infuser furnace catalyst is not registered");
expect(plugin, /onRuntimeAvailable\(IJeiRuntime runtime\)/, "onRuntimeAvailable is missing");
expect(plugin, /removeIngredientsAtRuntime\(VanillaTypes\.ITEM_STACK, icons\)/,
        "the advancement icons are no longer hidden from JEI");

// The original's sixteen blacklisted advancement icons, verbatim.
const HIDDEN = ["dark_days_icon", "adapted_icon", "primitive_icon", "crude_icon", "pure_icon",
    "hunt_season_icon", "guerilla_icon", "ecstasy_icon", "enemy_of_enemy_icon", "fog_nullifier_icon",
    "self_destruct_icon", "potion_columbus_icon", "potion_stolas_icon", "hellfire_chemical_warfare_icon",
    "cosmic_structural_failure_icon", "roots_icon"];
for (const id of HIDDEN) {
    if (!plugin.includes(`"${id}"`)) {
        failures.push(`the JEI blacklist lost ${id}`);
    }
    const items = read("src/main/java/alku/csrp/registry/ModItems.java");
    if (!items.includes(`"${id}"`)) {
        failures.push(`${id} is blacklisted from JEI but is not a registered item`);
    }
}

const category = read("src/main/java/alku/csrp/compat/jei/InfuserFurnaceJeiCategory.java");
expect(category, /"infuser_furnace"\)/, "the category uid changed");
expect(category, /IRecipeType\.create\(UID, InfuserFurnaceRecipe\.class\)/,
        "the category no longer exposes a recipe type for its own recipes");
expect(category, /"textures\/gui\/infuser_furnace\.png"/, "the category no longer uses the mod's GUI texture");
expect(category, /WIDTH = 176/, "the category background width changed");
expect(category, /HEIGHT = 82/, "the category background height changed");
expect(category, /ARROW_X = 79/, "the arrow x offset changed");
expect(category, /ARROW_Y = 34/, "the arrow y offset changed");
expect(category, /ARROW_TICKS = 200/, "the arrow animation length changed");
expect(category, /addInputSlot\(55, 16\)\.add\(recipe\.smeltIn\(\)\)/, "the smelt input slot changed");
expect(category, /addInputSlot\(29, 34\)\.add\(recipe\.infuseIn\(\)\)/, "the infuse input slot changed");
expect(category, /addOutputSlot\(115, 34\)\.add\(recipe\.output\(\)\)/, "the first output slot changed");
expect(category, /addOutputSlot\(139, 52\)\.add\(recipe\.byproduct\(\)\)/, "the byproduct output slot changed");
expect(category, /background\.draw\(graphics, 0, 0\)/,
        "the background is no longer painted from draw() (JEI draws categories as background, draw, slots)");
expect(category, /public boolean needsRecipeBorder\(\)\s*\{\s*return false;/,
        "the category must not ask JEI for a border on top of its own GUI frame");

// JEI must stay optional: compileOnly, and the plugin must not be reachable from common code.
const build = read("build.gradle");
expect(build, /compileOnly 'maven\.modrinth:jei:/, "JEI is no longer a compileOnly dependency");
if (/implementation 'maven\.modrinth:jei:|api 'maven\.modrinth:jei:/.test(build)) {
    failures.push("JEI became a hard dependency; the mod must run without it");
}
const manifest = read("src/main/templates/META-INF/neoforge.mods.toml");
if (/jei/i.test(manifest)) {
    failures.push("neoforge.mods.toml declares a JEI dependency; JEI must stay optional");
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log(`jei plugin contract ok (${HIDDEN.length} hidden icons)`);
