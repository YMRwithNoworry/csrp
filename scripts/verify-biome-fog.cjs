const fs = require("node:fs");
const path = require("node:path");

// The biome half of 1.10.9's parasite fog: SRPEventHandlerBus#playerTick (server) ramps a per-player
// fog towards SRPConfigWorld.biomeFogDensity and pushes it with network/SRPPacketFog, and the client
// hooks consume it (see verify-parasite-fog.cjs).  The ramp is gated on
// ParasiteEventWorld#canBiomeStillExist(pos, false) < 1 && !(biome instanceof BiomeParasiteBase),
// which needs the node effect-range query the original kept in SRPWorldData.
//
// Every number below is copied from the decompiled 1.10.9 sources:
//   util/handlers/SRPEventHandlerBus.java:2237-2277   world/SRPWorldData.java:862-1016
//   util/config/SRPConfigWorld.java:56,59,67-72,76-78,85-87,120-122,155-157,528-530
//   util/SRPReference.java:581-588                    util/ParasiteEventWorld.java:55-61
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
for (const [name, value] of [
    ["nodeTwoTime", "10"], ["nodeThreeTime", "40"],
    ["nodeRangeEffectsOne", "400"], ["nodeRangeEffectsTwo", "1600"], ["nodeRangeEffectsThree", "8000"],
    ["nodeRangeSpreadOne", "200"], ["nodeRangeSpreadTwo", "800"], ["nodeRangeSpreadThree", "4000"],
    ["biomeOneFogRed", "38.0D"], ["biomeOneFogGreen", "36.0D"], ["biomeOneFogBlue", "36.0D"],
    ["biomeTwoFogRed", "81.0D"], ["biomeTwoFogGreen", "107.0D"], ["biomeTwoFogBlue", "8.0D"],
    ["biomeThreeFogRed", "199.0D"], ["biomeThreeFogGreen", "201.0D"], ["biomeThreeFogBlue", "91.0D"],
    ["biomeFourFogRed", "100.0D"], ["biomeFourFogGreen", "100.0D"], ["biomeFourFogBlue", "100.0D"]
]) {
    expect(worldConfig, new RegExp(`"${name}", ${value.replace(".", "\\.")}`),
            `the ${name} config default changed (original ${value})`);
}
expect(worldConfig, /"biomeFogDensity", 0\.06D, 0\.0D, 1\.0D/,
        "biomeFogDensity is no longer 0.06 within 0..1");
expect(worldConfig, /defineInRange\(name, defaultValue, 0\.0D, 255\.0D\)/,
        "the biome fog colours are no longer 0..255 channels");
expect(worldConfig, /case 1 -> NODE_RANGE_EFFECTS_ONE\.get\(\);[\s\S]{0,400}default -> -1;/,
        "nodeRangeEffects no longer maps stages 1..3 and returns -1 otherwise");

const worldData = read("src/main/java/alku/csrp/world/SrpWorldData.java");
expect(worldData, /nodeAge >= WorldConfig\.nodeThreeTime\(\)[\s\S]{0,120}nodeAge >= WorldConfig\.nodeTwoTime\(\) \? 2 : 1/,
        "convertDayToAgeNode no longer uses the two node age thresholds");
expect(worldData, /return range < 0 \? -1 : \(squared \? range \* range : range\);/,
        "getDistanceEffectByAge/Spread no longer square the radius (or no longer return -1)");
expect(worldData, /distanceSquared <= range && \(distance == -1\.0 \|\| distance <= distanceSquared\)/,
        "nearestHeartAge lost the original's 'farthest covering node wins' rule");
expect(worldData, /public int nearestHeartAge\(BlockPos pos\)/, "nearestHeartAge is gone");
expect(worldData, /public int nearestHeartType\(BlockPos pos\)/, "nearestHeartType is gone");

const coreSystems = read("src/main/java/alku/csrp/world/SrpCoreSystems.java");
expect(coreSystems, /return SrpWorldData\.nodeStageFor\(age\);/,
        "SrpCoreSystems.nodeStage no longer uses the configurable node thresholds");

const decorator = read("src/main/java/alku/csrp/world/ParasiteBiomeDecorator.java");
expect(decorator, /public static int parasiteBiomeIndex\(Holder<Biome> biome\)/,
        "the biome numbering helper is gone");
expect(decorator, /return nodeType == 3 \? 3 : 1;/,
        "SRPReference#getBiomeFromInt no longer maps only 3 onto harlequin");

const server = read("src/main/java/alku/csrp/world/ParasiteBiomeAmbienceServer.java");
expect(server, /RAMP_UP = 4\.5E-4F/, "the ramp-up rate is no longer 4.5E-4F");
expect(server, /RAMP_DOWN = 8\.0E-4F/, "the ramp-down rate is no longer 8.0E-4F");
expect(server, /if \(heartAge < 1 && !inParasiteBiome\)/,
        "the 'no node influence and no parasite biome' gate changed");
expect(server, /fog = Math\.min\(fog \+ RAMP_UP, density\)/, "the ramp-up clamp changed");
expect(server, /fog = Math\.max\(fog - RAMP_DOWN, 0\.0F\)/, "the ramp-down clamp changed");
expect(server, /if \(fog < density\) \{\s*BiomeFogPayload\.sendTo\(player, fog, fogRed, fogGreen, fogBlue\);/,
        "the packet is no longer sent only while the density is still ramping");
expect(server, /WorldConfig\.biomeFogRed\(biome\) \/ 255\.0F/, "the biome colour is no longer / 255");
expect(server, /private static float fog;/, "the original's shared static accumulator is gone");
expect(server, /ParasiteBiomeDecorator\.nodeBiomeIndex\(data\.nearestHeartType\(pos\)\)/,
        "the node-driven colour no longer goes through getBiomeFromInt");

const payload = read("src/main/java/alku/csrp/network/BiomeFogPayload.java");
expect(payload, /Identifier\.fromNamespaceAndPath\(Csrp\.MODID, "biome_fog"\)/, "the payload id changed");
expect(payload, /ClientParasiteFog\.setBiomeFog\(payload\.density, payload\.red,\s*payload\.green, payload\.blue\)/,
        "the payload no longer feeds the client fog");
const payloads = read("src/main/java/alku/csrp/compendium/network/CompendiumPayloads.java");
expect(payloads, /playToClient\(BiomeFogPayload\.TYPE, BiomeFogPayload\.STREAM_CODEC,\s*BiomeFogPayload::handle\)/,
        "biome_fog is not registered as a client-bound payload");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("biome fog contract ok");
