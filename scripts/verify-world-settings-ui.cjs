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

function refute(content, pattern, description) {
    if (pattern.test(content)) failures.push(description);
}

// ---------------------------------------------------------------------------
// 1.10.9 SRPWorldCreateButtons: a single "SRP World Options..." button opens the dedicated
// GuiSRPWorldSettings screen.  The five cycle buttons belong to that screen, not to the vanilla
// create-world screen.
// ---------------------------------------------------------------------------
const hook = read("src/main/java/alku/csrp/client/SrpDifficultyScreenEvents.java");
expect(hook, /Commands|Button\.builder\(Component\.translatable\("gui\.csrp\.worldsettings\.open"\)/,
        "the create-world screen has no \"SRP World Options...\" entry button");
expect(hook, /new SrpWorldSettingsScreen\(screen\)/,
        "the entry button does not open SrpWorldSettingsScreen");
refute(hook, /CycleButton/, "the five option buttons are still injected into the vanilla create-world screen");
expect(hook, /public static void stageSelection\(CreateWorldScreen screen\)/,
        "stageSelection(CreateWorldScreen) was renamed; CreateWorldScreenMixin depends on it");
expect(hook, /SrpDifficultySelection\.stage\(state\.difficulty\)/,
        "the staged difficulty is not handed to the integrated server");
expect(hook, /SrpStarTypeSelection\.stage\(state\.starType\)/,
        "the staged star type is not handed to the integrated server");
expect(hook, /SrpMeteorSelection\.stage\(state\.meteor\)/,
        "the staged meteor mode is not handed to the integrated server");
expect(hook, /SrpColdStarSelection\.stage\(cold && state\.fracturedTerrain\.enabled\(\),\s*\n?\s*cold && state\.mushroomTrees\.enabled\(\)\)/,
        "the cold-star toggles are not staged (fractured, mushroom)");
expect(hook, /footerRowY\(screen\)/,
        "the entry button is not positioned against the vanilla footer row");
refute(hook, /Component\.translatable\("selectWorld\./,
        "the port references a vanilla translation key instead of locating the footer by position");

const screen = read("src/main/java/alku/csrp/client/SrpWorldSettingsScreen.java");
expect(screen, /class SrpWorldSettingsScreen extends Screen/,
        "SrpWorldSettingsScreen does not extend Screen");
for (const [caption, label] of [
    ["gui.csrp.worldsettings.difficulty", "difficulty"],
    ["gui.csrp.worldsettings.meteor", "meteor"],
    ["gui.csrp.worldsettings.star", "star type"],
    ["gui.csrp.worldsettings.mushroom_trees", "mushroom trees"],
    ["gui.csrp.worldsettings.fractured", "fractured terrain"]
]) {
    expect(screen, new RegExp(`Component\\.translatable\\("${caption.replace(/\./g, "\\.")}"\\)`),
            `the ${label} option is missing from the settings screen`);
}
expect(screen, /gui\.csrp\.worldsettings\.done/, "the Done button is missing");
expect(screen, /SrpWorldPreview\.draw\(/, "the settings screen no longer draws the world preview");
expect(screen, /this\.mushroomTreesButton\.visible = cold;/, "mushroom trees is no longer cold-star only");
expect(screen, /this\.fracturedButton\.visible = cold;/, "fractured terrain is no longer cold-star only");
expect(screen, /SrpDifficultyScreenEvents\.applySelections\(/, "Done does not write the selections back");
expect(screen, /setScreenAndShow\(this\.parent\)/, "Done does not return to the create-world screen");

const preview = read("src/main/java/alku/csrp/client/SrpWorldPreview.java");
expect(preview, /STAR_SEED = 923847L/, "the preview starfield lost the original fixed seed");
expect(preview, /STAR_COUNT = 70/, "the preview starfield lost its 70 stars");
expect(preview, /enableScissor\(/, "the preview no longer clips its starfield to the panel");
for (const texture of ["earth_easy", "earth_normal", "earth_hard", "earth_impossible", "earth_cold",
    "earth_warm", "moon", "meteor_orbit"]) {
    if (!fs.existsSync(path.join(root, "src/main/resources/assets/csrp/textures/gui/worldsettings",
            texture + ".png"))) {
        failures.push("missing preview texture textures/gui/worldsettings/" + texture + ".png");
    }
}

// SRPConfig.worldGIU gate.
const config = read("src/main/java/alku/csrp/config/GeneralConfig.java");
expect(config, /\.define\("worldCreationUi",\s*true\)/,
        "GeneralConfig no longer exposes worldCreationUi (SRPConfig.worldGIU)");
expect(config, /public static boolean worldCreationUi\(\)/,
        "GeneralConfig.worldCreationUi() accessor is missing");
expect(hook, /GeneralConfig\.worldCreationUi\(\)/,
        "the world-creation UI is no longer gated by the config");

// Labels the restored screen needs.
for (const locale of ["en_us", "zh_cn"]) {
    const file = path.join(root, "src/main/resources/assets/csrp/lang", locale + ".json");
    const json = JSON.parse(fs.readFileSync(file, "utf8"));
    for (const key of ["gui.csrp.worldsettings.open", "gui.csrp.worldsettings.title",
        "gui.csrp.worldsettings.subtitle", "gui.csrp.worldsettings.done",
        "gui.csrp.worldsettings.star.warm", "gui.csrp.worldsettings.tooltip.star.warm.1"]) {
        if (!(key in json)) failures.push(locale + " is missing " + key);
    }
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("world-settings UI contract ok");
