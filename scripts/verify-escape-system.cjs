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

// 1.10.9 EscapeOnDeathHandler / EscapeRespawnHandler: dying to parasites more than five times inside a
// one-minute window offers an escape, and accepting it teleports the player away on respawn.
const escape = read("src/main/java/alku/csrp/feature/EscapeSystem.java");
expect(escape, /WINDOW_MS = 60_000L/, "the one-minute escape window is missing");
expect(escape, /THRESHOLD = 5/, "the five-death escape threshold is missing");
expect(escape, /"srp_offer_escape"/, "the srp_offer_escape flag key is missing");
expect(escape, /"srp_death_window"/, "the srp_death_window flag key is missing");
expect(escape, /"srp_escape_pending"/, "the srp_escape_pending flag key is missing");
expect(escape, /SAFE_TRIES = 24/, "findSafeRandom no longer uses the original 24 attempts");
expect(escape, /public static boolean registerParasiteDeath\(UUID playerId, long nowMs\)/,
        "the streak rule is no longer exposed for runtime testing");
expect(escape, /streak\.size\(\) > THRESHOLD/, "the streak rule no longer compares against the threshold");
expect(escape, /nowMs - streak\.peekFirst\(\) > WINDOW_MS/, "expired deaths are no longer dropped");
expect(escape, /LivingDeathEvent/, "the death handler does not subscribe to LivingDeathEvent");
expect(escape, /PlayerEvent\.PlayerRespawnEvent/, "the respawn handler is missing");
expect(escape, /Heightmap\.Types\.MOTION_BLOCKING/, "the escape teleport no longer picks a surface position");
expect(escape, /player\.teleportTo\(/, "the escape respawn no longer teleports the player");
expect(escape, /source\.getDirectEntity\(\) instanceof Parasite|source\.getEntity\(\) instanceof Parasite/,
        "the parasite death-cause check is missing");
expect(escape, /WorldConfig\.escapeEnabled\(\)/, "the escape system is no longer gated by its config");

// The packets and the client-side death screen button.
const offer = read("src/main/java/alku/csrp/network/EscapeOfferPayload.java");
expect(offer, /Identifier\.fromNamespaceAndPath\(Csrp\.MODID, "escape_offer"\)/,
        "the escape_offer payload id changed");
expect(offer, /public Type<\? extends CustomPacketPayload> type\(\)/, "EscapeOfferPayload.type() is missing");
const request = read("src/main/java/alku/csrp/network/EscapeRequestPayload.java");
expect(request, /Identifier\.fromNamespaceAndPath\(Csrp\.MODID, "escape_request"\)/,
        "the escape_request payload id changed");
expect(request, /public Type<\? extends CustomPacketPayload> type\(\)/,
        "EscapeRequestPayload.type() is missing");
const registrar = read("src/main/java/alku/csrp/network/SrpEscapePayloads.java");
expect(registrar, /playToClient\(EscapeOfferPayload\.TYPE/, "escape_offer is not registered to the client");
expect(registrar, /playToServer\(EscapeRequestPayload\.TYPE/, "escape_request is not registered to the server");

const screen = read("src/main/java/alku/csrp/client/EscapeDeathScreenEvents.java");
expect(screen, /instanceof DeathScreen/, "the escape button is not attached to the death screen");
expect(screen, /EscapeClientState\.offer\(\)/, "the escape button ignores the client offer flag");
expect(screen, /gui\.csrp\.escape_button/, "the escape button label is missing");
expect(screen, /gui\.csrp\.escape_button_blocks/, "the escape button block-range suffix is missing");
expect(screen, /ClientPacketDistributor\.sendToServer\(EscapeRequestPayload\.INSTANCE\)/,
        "the escape button does not request the escape");
expect(screen, /minecraft\.player\.respawn\(\)/, "the escape button does not respawn the player");
expect(read("src/main/java/alku/csrp/client/EscapeClientState.java"), /public static void setOffer\(boolean value\)/,
        "EscapeClientState.setOffer is missing");

// Config: SRPConfigWorld "EscapeSystem".
const config = read("src/main/java/alku/csrp/config/WorldConfig.java");
expect(config, /\.define\("escapeEnabled",\s*true\)/, "escapeEnabled default true is missing");
expect(config, /\.defineInRange\("escapeMinDistance",\s*200,/, "escapeMinDistance default 200 is missing");
expect(config, /\.defineInRange\("escapeMaxDistance",\s*300,/, "escapeMaxDistance default 300 is missing");
for (const accessor of ["escapeEnabled", "escapeMinDistance", "escapeMaxDistance"]) {
    expect(config, new RegExp(`public static \\w+ ${accessor}\\(\\)`), `WorldConfig.${accessor}() is missing`);
}

// Runtime tests exist for the streak rule.
const tests = read("src/main/java/alku/csrp/gametest/EscapeSystemGameTests.java");
expect(tests, /escape_system\/streak_offer/, "the escape streak GameTest is missing");
expect(tests, /escape_system\/window_expiry/, "the escape window-expiry GameTest is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("escape system contract ok");
