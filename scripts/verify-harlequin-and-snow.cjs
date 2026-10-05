const fs = require("node:fs");
const path = require("node:path");

// Two command families from 1.10.9, with very different status in the original:
//
//  * /harlequin_here WAS registered (CommonProxy#registerServerCommand) and has srphelp lang entries,
//    so it is ported and live.
//  * /srp_extremesnow was NEVER registered — the only callers of ExtremeSnowData#setEnabled are inside
//    CommandExtremeSnow itself — and it has no lang entries at all.  The system it would drive
//    (ExtremeSnowData + ExtremeSnowServer + ExtremeSnowNetwork + ClientExtremeSnow) is wired up in the
//    original but can never switch on, so the state and its consumers are ported for parity while the
//    command stays off the dispatcher: registering it would hand players an admin command the original
//    never had.
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

// --- /harlequin_here -------------------------------------------------------------------------------
const harlequin = read("src/main/java/alku/csrp/command/HarlequinCommand.java");
expect(harlequin, /HERE = "harlequin_here"/, "the command name changed");
expect(harlequin, /Commands\.hasPermission\(Commands\.LEVEL_GAMEMASTERS\)/,
        "harlequin_here is no longer permission level 2");
expect(harlequin, /DEFAULT_RADIUS_CHUNKS = 1/, "the default radiusChunks is no longer 1");
expect(harlequin, /IntegerArgumentType\.integer\(0, MAX_RADIUS_CHUNKS\)/, "the radiusChunks bounds changed");
expect(harlequin, /ParasiteBiomeDecorator\.HARLEQUIN_BIOME/, "the Harlequin biome key is not used");
expect(harlequin, /chunk\.fillBiomesFromNoise\(\(x, y, z\) -> harlequin\)/,
        "the per-chunk biome write is gone");
expect(harlequin, /chunk\.markUnsaved\(\)/, "the rewritten chunk is no longer marked unsaved");
expect(harlequin, /chunkMap\.resendBiomesForChunks\(changed\)/,
        "the client biome resend is gone");
expect(harlequin, /"Applied Harlequin to "/, "the original's report line is gone");
expect(harlequin, /"Server biome at your position now: "/, "the original's biome report line is gone");
// Vanilla's /fillbiome entry point rejects volumes over 32768 blocks, which a full-height chunk square
// far exceeds, so the command must not use it.
refute(harlequin, /FillBiomeCommand\.fill\(/, "the command went back to the volume-capped vanilla fill");
const commands = read("src/main/java/alku/csrp/command/SrpCommands.java");
expect(commands, /dispatcher\.register\(HarlequinCommand\.hereCommand\(\)\)/,
        "harlequin_here is not registered");

// --- the forced extreme snow ------------------------------------------------------------------------
const data = read("src/main/java/alku/csrp/world/ExtremeSnowData.java");
expect(data, /DATA_NAME = "srp_extreme_snow"/, "the saved-data name changed");
for (const key of ["enabled", "intensity", "forceAnywhere", "windDeg", "windSpeed"]) {
    expect(data, new RegExp(`"${key}"`), `the ${key} NBT key is gone`);
}
expect(data, /private boolean enabled = false;/, "enabled no longer defaults to false");
expect(data, /private float intensity = 1\.0F;/, "intensity no longer defaults to 1.0");
expect(data, /private boolean forceAnywhere = true;/, "forceAnywhere no longer defaults to true");
expect(data, /private float windDeg = 30\.0F;/, "windDeg no longer defaults to 30");
expect(data, /private float windSpeed = 0\.5F;/, "windSpeed no longer defaults to 0.5");

const server = read("src/main/java/alku/csrp/world/ExtremeSnowServer.java");
expect(server, /TRIES_BASE = 12\.0F/, "the per-player attempt base is no longer 12");
expect(server, /TRIES_PER_INTENSITY = 48\.0F/, "the per-player attempt slope is no longer 48");
expect(server, /RADIUS = 18/, "the placement radius is no longer 18");
expect(server, /Blocks\.SNOW\.defaultBlockState\(\)\.setValue\(SnowLayerBlock\.LAYERS, 1\)/,
        "the snow layer placement changed");
expect(server, /shouldSnow\(level, hit\)/, "the biome snow check is gone");

const client = read("src/main/java/alku/csrp/client/weather/ClientExtremeSnow.java");
expect(client, /0\.15D \+ 0\.55D \* windSpeed/, "the wind base formula changed");
expect(client, /Math\.cos\(radians\) \* base/, "the wind x component changed");
expect(client, /Math\.sin\(radians\) \* base/, "the wind z component changed");
expect(client, /Math\.sqrt\(Math\.log\(50\.0D\)\)/, "the fog density formula changed");
expect(client, /FOG_TARGET_BLOCKS = 10\.0D/, "the fog visibility target is no longer ten blocks");
expect(client, /FOG_COLOR_PUSH = 0\.75F/, "the fog colour push is no longer 0.75");
expect(client, /event\.setFarPlaneDistance\(far\)/, "the fog far plane is no longer applied");
expect(client, /event\.setRed\(event\.getRed\(\) \* \(1\.0F - FOG_COLOR_PUSH\) \+ FOG_COLOR_PUSH\)/,
        "the fog colour push is gone");
expect(client, /int\[\]\[\] offsets = \{\{0, 0\}, \{4, 0\}, \{-4, 0\}, \{0, 4\}, \{0, -4\}\}/,
        "the five-sample outdoors check changed");

const payloads = read("src/main/java/alku/csrp/compendium/network/CompendiumPayloads.java");
expect(payloads, /playToClient\(ExtremeSnowStatePayload\.TYPE, ExtremeSnowStatePayload\.STREAM_CODEC/,
        "the extreme snow state payload is not registered");

const blizzard = read("src/main/java/alku/csrp/client/weather/SRPBlizzardClient.java");
expect(blizzard, /isColdWorld\(\) \|\| ClientExtremeSnow\.isEnabled\(\)/,
        "the blizzard no longer activates for the forced snow");
expect(blizzard, /return Mth\.clamp\(ClientExtremeSnow\.getIntensity\(\), 0\.0F, 1\.0F\)/,
        "the forced snow no longer supplies the blizzard intensity");

// The command exists for parity but must stay off the dispatcher.
const command = read("src/main/java/alku/csrp/command/ExtremeSnowCommand.java");
expect(command, /NAME = "srp_extremesnow"/, "the extreme snow command name changed");
expect(command, /Deliberately not registered/, "the 'not registered' note was removed");
refute(commands, /ExtremeSnowCommand/, "srp_extremesnow was registered; the original never registered it");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("harlequin + extreme snow contract ok");
