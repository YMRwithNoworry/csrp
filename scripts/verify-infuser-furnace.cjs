const fs = require("node:fs");
const path = require("node:path");

// The infuser furnace is a faithful port of 1.10.9 tileentity/TileEntityInfuserFurnace +
// container/ContainerInfuserFurnace + client/gui/GuiInfuserFurnace: six slots, two independent 200 tick
// counters sharing one fuel buffer, a main output plus a bottle byproduct, and vanilla smelting on the
// side.  The GUI texture's slot frames sit at the original coordinates, so a drift in either the slot
// layout or the sprite offsets is visible in game.
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

function refute(content, pattern, description) {
    if (pattern.test(content)) failures.push(description);
}

const blockEntity = read("src/main/java/alku/csrp/block/entity/InfuserFurnaceBlockEntity.java");
expect(blockEntity, /SLOT_SMELT_IN = 0/, "the smelt input slot index changed");
expect(blockEntity, /SLOT_FUEL = 1/, "the fuel slot index changed");
expect(blockEntity, /SLOT_SMELT_OUT = 2/, "the smelt output slot index changed");
expect(blockEntity, /SLOT_INFUSE_IN = 3/, "the infuse input slot index changed");
expect(blockEntity, /SLOT_INFUSE_OUT = 4/, "the infused output slot index changed");
expect(blockEntity, /SLOT_BOTTLE_OUT = 5/, "the bottle byproduct slot index changed");
expect(blockEntity, /CONTAINER_SIZE = 6/, "the infuser furnace no longer has the original six slots");
expect(blockEntity, /COOK_TIME_TOTAL = 200/, "COOK_TIME_TOTAL is no longer 200");
expect(blockEntity, /getCount\(\) \{\s*return 4;/, "the container data no longer exposes four fields");
expect(blockEntity, /RecipeType\.SMELTING/, "vanilla smelting is no longer part of the furnace");
expect(blockEntity, /mergeInto\(SLOT_BOTTLE_OUT, recipe\.byproduct\(\)\)/, "the bottle byproduct is not produced");
expect(blockEntity, /mergeInto\(SLOT_INFUSE_OUT, recipe\.output\(\)\)/, "the infusion output is not produced");
expect(blockEntity, /output\.putInt\("BurnTime"/, "the BurnTime NBT key changed");
expect(blockEntity, /output\.putInt\("CurrentBurnTime"/, "the CurrentBurnTime NBT key changed");
expect(blockEntity, /output\.putInt\("CookSmelt"/, "the CookSmelt NBT key changed");
expect(blockEntity, /output\.putInt\("CookInfuse"/, "the CookInfuse NBT key changed");
refute(blockEntity, /Blocks\.SAND\.asItem\(\)/,
        "the infuse recipe still accepts vanilla sand instead of csrp:infestedsand");

// The nine recipes of SRPInfuserFurnaceRecipeInit.
const recipes = read("src/main/java/alku/csrp/recipe/InfuserFurnaceRecipes.java");
expect(recipes, /new ArrayList<>\(9\)/, "the infuser recipe table is no longer nine entries");
expect(recipes, /new ItemStack\(glass, 2\)/, "glass swaps no longer yield two glass");
expect(recipes, /Items\.IRON_INGOT, ModItems\.DEADBLOOD_FLUID\.get\(\)/, "the iron -> semi-organic recipe changed");
expect(recipes, /ModBlocks\.INFESTED_SAND\.get\(\)\.asItem\(\), ModItems\.DEADBLOOD_FLUID\.get\(\)/,
        "the infested sand -> infested glass recipe changed");
expect(recipes, /addGlassSwap\(table, Items\.ICE, ModBlocks\.SHROUDED_GLASS\.get\(\)\.asItem\(\)\)/,
        "the ice -> shrouded glass swap changed");
expect(recipes, /public static InfuserFurnaceRecipe find\(ItemStack smelt, ItemStack infuse\)/,
        "InfuserFurnaceRecipes.find is missing");
expect(recipes, /public static List<InfuserFurnaceRecipe> all\(\)/, "InfuserFurnaceRecipes.all is missing");
const byproductCount = (recipes.match(/bottle\.copy\(\)/g) || []).length;
if (byproductCount !== 2) {
    failures.push(`expected 2 recipes to leave a glass bottle byproduct, found ${byproductCount}`);
}

// ContainerInfuserFurnace: the original's slot order and coordinates.
const menu = read("src/main/java/alku/csrp/inventory/InfuserFurnaceMenu.java");
expect(menu, /CONTAINER_SLOTS = 6/, "the menu no longer exposes six container slots");
expect(menu, /new Slot\(container, 0, 56, 17\)/, "the smelt input slot moved off (56,17)");
expect(menu, /new FuelSlot\(container, 1, 56, 53\)/, "the fuel slot moved off (56,53)");
expect(menu, /new Slot\(container, 3, 30, 35\)/, "the infuse reagent slot moved off (30,35)");
expect(menu, /new FurnaceResultSlot\(inventory\.player, container, 4, 140, 53\)/,
        "the infused output slot moved off (140,53)");
expect(menu, /new FurnaceResultSlot\(inventory\.player, container, 5, 116, 35\)/,
        "the bottle byproduct slot moved off (116,35)");
expect(menu, /new FurnaceResultSlot\(inventory\.player, container, 2, 140, 17\)/,
        "the smelt output slot moved off (140,17)");
expect(menu, /stack\.has\(DataComponents\.COOKING_FUEL\)/, "the fuel slot no longer validates fuel");

// GuiInfuserFurnace: the mod's own background and the original sprite offsets.
const screen = read("src/main/java/alku/csrp/client/screen/InfuserFurnaceScreen.java");
expect(screen, /"textures\/gui\/infuser_furnace\.png"/, "the screen no longer uses the mod's infuser_furnace.png");
refute(screen, /container\/furnace\.png/, "the screen still falls back to the vanilla furnace background");
expect(screen, /ARROW_U = 176/, "the progress arrow u coordinate changed");
expect(screen, /ARROW_V = 14/, "the progress arrow v coordinate changed");
expect(screen, /ARROW_X = 80/, "the progress arrow x offset changed");
expect(screen, /ARROW_Y = 35/, "the progress arrow y offset changed");
expect(screen, /FLAME_X = 56/, "the burn flame x offset changed");
expect(screen, /FLAME_Y = 36/, "the burn flame y offset changed");
expect(screen, /topPos \+ FLAME_Y \+ 12 - flame/, "the burn flame is no longer drawn upwards from the bottom");

// BlockInfuserFurnace: a horizontal directional BlockContainer with four facing variants.
const block = read("src/main/java/alku/csrp/block/InfuserFurnaceBlock.java");
expect(block, /extends HorizontalDirectionalBlock/, "the infuser furnace is no longer directional");
expect(block, /setValue\(FACING, context\.getHorizontalDirection\(\)\.getOpposite\(\)\)/,
        "placement no longer faces the placer");
refute(block, /BlockStateProperties\.LIT|BooleanProperty LIT/,
        "the original infuser furnace has no lit property");
const blockstate = read("src/main/resources/assets/csrp/blockstates/infuser_furnace.json");
for (const facing of ["north", "south", "west", "east"]) {
    expect(blockstate, new RegExp(`"facing=${facing}"`), `the blockstate lost its facing=${facing} variant`);
}

// The runtime tests that drive the real tick loop.
const tests = read("src/main/java/alku/csrp/gametest/InfuserFurnaceGameTests.java");
for (const id of ["infuser_furnace/infuse_with_byproduct", "infuser_furnace/glass_swap_yields_two",
    "infuser_furnace/vanilla_smelting"]) {
    expect(tests, new RegExp(id.replace(/\//g, "\\/")), `the ${id} GameTest is missing`);
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("infuser furnace contract ok");
