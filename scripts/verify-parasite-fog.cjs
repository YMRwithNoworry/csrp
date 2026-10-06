const fs = require("node:fs");
const path = require("node:path");

// The parasite-blood half of 1.10.9's fog (SRPEventHandlerBus, client side):
//
//   FogDensity: if (entity.isInsideOfMaterial(SRPBlocks.PARASITEBLOOD)) { setFog(FogMode.EXP); density = 1.0F; }
//   FogColors:  if (entity.isInsideOfMaterial(SRPBlocks.PARASITEBLOOD)) { red 0.1F; green 0.6F; blue 0.2F; }
//
// SRPBlocks.PARASITEBLOOD is a Material (the dead-blood fluid's), not a block, so the 26.3 test is
// LocalPlayer#isInFluidType(csrp:deadblood).  The biome half of the same two hooks needs
// biomeFogDensity plus the node-influence radius query and is still tracked in PORT_BACKLOG.md.
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

/** Strips `//` and block comments so the code-only guards below ignore the class javadoc. */
function codeOnly(source) {
    return source.replace(/\/\*[\s\S]*?\*\//g, "").replace(/\/\/[^\n]*/g, "");
}

const fog = read("src/main/java/alku/csrp/client/weather/ClientParasiteFog.java");
expect(fog, /@EventBusSubscriber\(modid = Csrp\.MODID, value = Dist\.CLIENT\)/,
        "the fog is no longer a client-only subscriber");
expect(fog, /BLOOD_DENSITY = 1\.0F/, "the parasite-blood density is no longer 1.0F");
expect(fog, /BLOOD_RED = 0\.1F/, "the parasite-blood red is no longer 0.1F");
expect(fog, /BLOOD_GREEN = 0\.6F/, "the parasite-blood green is no longer 0.6F");
expect(fog, /BLOOD_BLUE = 0\.2F/, "the parasite-blood blue is no longer 0.2F");
expect(fog, /player\.isInFluidType\(ModFluidTypes\.DEAD_BLOOD\.get\(\)\)/,
        "the parasite-blood test no longer uses the dead-blood fluid type");
expect(fog, /Math\.sqrt\(Math\.log\(50\.0D\)\) \/ density/,
        "the exponential-density to visibility conversion changed");
expect(fog, /NEAR_PLANE_RATIO = 0\.08D/, "the near-plane ratio changed");
expect(fog, /event\.setFarPlaneDistance\(far\)/, "the far plane is no longer applied");
expect(fog, /event\.setNearPlaneDistance\(\(float\) \(far \* NEAR_PLANE_RATIO\)\)/,
        "the near plane is no longer applied");
expect(fog, /event\.setRed\(BLOOD_RED\);\s*event\.setGreen\(BLOOD_GREEN\);\s*event\.setBlue\(BLOOD_BLUE\);/,
        "the fog colour write changed");
// The original overrode the fog for every fog mode (EntityViewRenderEvent.FogDensity fires for all of
// them), so the port must not filter on FogType here.
refute(codeOnly(fog), /FogType/, "the parasite-blood fog grew a FogType filter the original did not have");

// --- the biome branch of the same two hooks --------------------------------------------------------
expect(fog, /biomeFog > WorldConfig\.biomeFogDensity\(\) \* 0\.2F && !hasNightVision\(\)/,
        "the FogColors threshold is no longer biomeFogDensity * 0.2 with the night-vision exemption");
expect(fog, /biomeFog > 0\.0F && !hasNightVision\(\) \? biomeFog : 0\.0F/,
        "the FogDensity branch is no longer `fog > 0 && !nightVision`");
expect(fog, /player\.hasEffect\(MobEffects\.NIGHT_VISION\)/,
        "the night-vision check no longer uses MobEffects.NIGHT_VISION");
expect(fog, /public static void setBiomeFog\(float density, float red, float green, float blue\)/,
        "the SRPPacketFog handler entry point is gone");
expect(fog, /if \(level != trackedLevel\)/, "the world-unload reset of the fog statics is gone");
// The original exempts only the biome branch from night vision; the blood branch is checked first.
expect(fog, /if \(inParasiteBlood\(\)\) \{\s*return BLOOD_DENSITY;\s*\}\s*return biomeFog > 0\.0F && !hasNightVision\(\)/,
        "the blood branch is no longer evaluated before the night-vision exemption");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("parasite blood fog contract ok");
