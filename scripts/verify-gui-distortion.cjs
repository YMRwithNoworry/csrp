const fs = require("node:fs");
const path = require("node:path");

// Port of 1.10.9 network/SRPCommandGuiDistortion + SRPPacketGuiDistortionState.  The lang keys for this
// command were already migrated, so a missing command is invisible until someone tries to use it — which
// is exactly what happened before this was restored.  These checks pin the whole chain: command and
// aliases, the two persisted flags, the sync payload, the client gate, and the messages.
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

const command = read("src/main/java/alku/csrp/command/GuiDistortionCommand.java");
expect(command, /NAME = "srpguidistortion"/, "the command name changed");
expect(command, /ALIASES = List\.of\("srpguidist", "srpdistortion"\)/, "the command aliases changed");
expect(command, /Commands\.hasPermission\(Commands\.LEVEL_GAMEMASTERS\)/,
        "the command is no longer permission level 2");
expect(command, /"SRPGuiDistortionDisabled"/, "the SRPGuiDistortionDisabled flag key changed");
expect(command, /"SRPGuiDistortionCreativeOverride"/, "the SRPGuiDistortionCreativeOverride flag key changed");
expect(command, /MODES = List\.of\("on", "off", "toggle", "status"\)/, "the four modes changed");
expect(command, /getPersistentData\(\)\.putBoolean\(DISABLED_KEY/, "the disabled flag is no longer persisted");
expect(command, /GuiDistortionStatePayload\.send\(target, newDisabled, newCreativeOverride\)/,
        "the command no longer syncs the state to the client");
expect(command, /getPlayerList\(\)\.getPlayerByName\(rawTarget\)/, "player lookup by name is missing");
expect(command, /"all"\.equalsIgnoreCase\(rawTarget\) \|\| "@a"\.equalsIgnoreCase\(rawTarget\)/,
        "the all/@a target handling is missing");
expect(command, /console_requires_player/, "the console-without-player message is missing");
expect(command, /public static boolean\[\] computeFlags\(/, "computeFlags is missing");
expect(command, /public static boolean isEffectivelyDisabled\(/, "isEffectivelyDisabled is missing");

// Every message key the original used must be referenced, and must exist in the lang file.
const MESSAGE_KEYS = ["invalid_argument", "player_not_found", "console_requires_player",
    "state.disabled", "state.enabled", "creative_override.disabled", "creative_override.enabled",
    "gamemode.creative_or_spectator", "gamemode.normal", "status.detailed", "set_other", "set_self",
    "set_all", "usage.text"];
const lang = JSON.parse(read("src/main/resources/assets/csrp/lang/en_us.json"));
for (const key of MESSAGE_KEYS) {
    const full = `commands.csrp.guidistortion.${key}`;
    if (!command.includes(full)) failures.push(`the command no longer uses ${full}`);
    if (lang[full] === undefined) failures.push(`${full} is missing from en_us.json`);
}

// Registration: its own root command plus both aliases.
const commands = read("src/main/java/alku/csrp/command/SrpCommands.java");
expect(commands, /dispatcher\.register\(GuiDistortionCommand\.command\(\)\)/,
        "the distortion command is not registered");
expect(commands, /GuiDistortionCommand\.aliasCommands\(\)/, "the distortion command aliases are not registered");

// Sync payload.
const payload = read("src/main/java/alku/csrp/network/GuiDistortionStatePayload.java");
expect(payload, /"gui_distortion_state"/, "the payload id changed");
expect(payload, /GuiDistortionClientState\.set\(payload\.disabled, payload\.creativeOverride\)/,
        "the payload no longer updates the client state");
const payloads = read("src/main/java/alku/csrp/compendium/network/CompendiumPayloads.java");
expect(payloads, /playToClient\(GuiDistortionStatePayload\.TYPE, GuiDistortionStatePayload\.STREAM_CODEC/,
        "the payload is not registered as playToClient");

// Client gate: the effect must actually consult the switch.
const clientState = read("src/main/java/alku/csrp/client/GuiDistortionClientState.java");
expect(clientState, /return disabled \|\| \(isCreativeOrSpectator\(\) && !creativeOverride\);/,
        "the client no longer applies the creative/spectator rule");
const distortion = read("src/main/java/alku/csrp/client/DerivedTextDistortion.java");
expect(distortion, /!GuiDistortionClientState\.effectivelyDisabled\(\)/,
        "the distortion rendering ignores the /srpguidistortion switch");

// Runtime tests.
const tests = read("src/main/java/alku/csrp/gametest/GuiDistortionGameTests.java");
expect(tests, /gui_distortion\/flag_transitions/, "the flag transition GameTest is missing");
expect(tests, /gui_distortion\/command_registered/, "the command registration GameTest is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log(`gui distortion contract ok (${MESSAGE_KEYS.length} message keys)`);
