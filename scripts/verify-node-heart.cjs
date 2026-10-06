const fs = require("node:fs");
const path = require("node:path");

// The node heart sound, the last piece of 1.10.9's SRPEventHandlerBus#playerTick (lines 2278-2291):
//
//   this.heart++;
//   if (this.heart < SRPConfigWorld.biomeHeartFreq) return;      // default 30
//   this.heart = 0;
//   if (isParaBiome) {
//       int age = data.nearestHeartAge(pos, spread = true, 0);
//       if (age > 0) {
//           int totalS = data.getDistanceSpreadByAge(age, true);          // squared spread radius
//           float vol = ((float) data.isInRangeOfHeart(pos, totalS) / totalS - 1.0F) * -1.0F;
//           send(new SRPPacketMovingSound(-1, vol));
//       }
//   }
//
// and the client side (ClientProxy#playMovingSound case -1, ClientProxy.java:119-126):
//   playSound(PositionedSoundRecord.func_194007_a(SRPSounds.BHEART, 1.0F, v * SRPConfigWorld.biomeHeartVol));
//
// SRPWorldData#isInRangeOfHeart (SRPWorldData.java:842-860) bumps the radius by one and returns the
// FIRST node inside it, not the nearest one.
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
expect(worldConfig, /"biomeHeartFreq", 30, 0, 100/, "biomeHeartFreq is no longer 30 within 0..100");
expect(worldConfig, /"biomeHeartVol", 0\.4D, 0\.0D, 1000\.0D/,
        "biomeHeartVol is no longer 0.4 within 0..1000");
expect(worldConfig, /public static int biomeHeartFreq\(\)/, "the biomeHeartFreq getter is gone");
expect(worldConfig, /public static float biomeHeartVol\(\)/, "the biomeHeartVol getter is gone");

const worldData = read("src/main/java/alku/csrp/world/SrpWorldData.java");
expect(worldData, /public int isInRangeOfHeart\(BlockPos pos, double distance\)/,
        "isInRangeOfHeart is gone");
expect(worldData, /double radius = distance \+ 1\.0D;/,
        "isInRangeOfHeart no longer bumps the radius by one before comparing");
expect(worldData, /if \(distanceSquared < radius \* radius\) \{\s*return \(int\) distanceSquared;/,
        "isInRangeOfHeart no longer returns the first node's squared distance");
expect(worldData, /public int nearestHeartAge\(BlockPos pos, boolean spread\)/,
        "the spread-aware nearestHeartAge overload is gone");
expect(worldData, /public int nearestHeartType\(BlockPos pos, boolean spread\)/,
        "the spread-aware nearestHeartType overload is gone");
expect(worldData, /spread\s*\?\s*getDistanceSpreadByAge\(stage, true\)\s*:\s*getDistanceEffectByAge\(stage, true\)/,
        "coveringHeart no longer switches between the spread and effect tables");

const server = read("src/main/java/alku/csrp/world/ParasiteBiomeAmbienceServer.java");
expect(server, /private static int heart;/, "the original's shared heart counter is gone");
expect(server, /if \(\+\+heart < WorldConfig\.biomeHeartFreq\(\)\) \{\s*return;\s*\}\s*heart = 0;/,
        "the heart counter no longer fires every biomeHeartFreq player ticks");
expect(server, /if \(inParasiteBiome\) \{\s*playHeart\(data, pos, player\);/,
        "the heart is no longer gated on the player standing in a parasite biome");
expect(server, /int age = data\.nearestHeartAge\(pos, true\);/, "the heart no longer uses the spread query");
expect(server, /int spreadSquared = data\.getDistanceSpreadByAge\(age, true\);/,
        "the heart no longer uses the squared spread radius");
expect(server, /data\.isInRangeOfHeart\(pos, spreadSquared\)/,
        "the heart no longer measures its distance with isInRangeOfHeart");
expect(server, /float volume = \(\(float\) distanceSquared \/ spreadSquared - 1\.0F\) \* -1\.0F;/,
        "the heart volume formula changed");

const payload = read("src/main/java/alku/csrp/network/NodeHeartPayload.java");
expect(payload, /Identifier\.fromNamespaceAndPath\(Csrp\.MODID, "node_heart"\)/, "the payload id changed");
expect(payload, /ClientNodeHeart\.play\(payload\.volume\)/, "the payload no longer plays the heart sound");
const payloads = read("src/main/java/alku/csrp/compendium/network/CompendiumPayloads.java");
expect(payloads, /playToClient\(NodeHeartPayload\.TYPE, NodeHeartPayload\.STREAM_CODEC,\s*NodeHeartPayload::handle\)/,
        "node_heart is not registered as a client-bound payload");

const client = read("src/main/java/alku/csrp/client/sound/ClientNodeHeart.java");
expect(client, /ModSounds\.get\("biome\.heart"\)/, "the client no longer plays the biome.heart sound event");
expect(client, /SoundSource\.NEUTRAL, volume \* WorldConfig\.biomeHeartVol\(\), 1\.0F, false\)/,
        "the heart volume/pitch no longer match PositionedSoundRecord.func_194007_a(sound, 1.0F, v * biomeHeartVol)");
const catalog = read("src/main/java/alku/csrp/registry/SoundEventCatalog.java");
expect(catalog, /"biome\.heart"/, "the biome.heart sound event is no longer registered");

// The same original packet also carried the evolution phase jingles (cases 1..10).  The port plays
// them through the vanilla ClientboundSoundPacket, so the four values it carries must still match
// ClientProxy#playMovingSound: EVPHASE_<phase>, SoundCategory.NEUTRAL, pitch 1.0F, volume 0.75F.
const evolution = read("src/main/java/alku/csrp/world/EvolutionSystem.java");
expect(evolution, /if \(advanced && current >= 1\)/, "the phase jingle is no longer gated on advancing to phase 1+");
expect(evolution, /Holder\.direct\(ModSounds\.evolutionPhase\(current\)\), SoundSource\.NEUTRAL, player\.getX\(\), player\.getY\(\), player\.getZ\(\), 0\.75F, 1\.0F,/,
        "the phase jingle no longer matches EVPHASE_n / NEUTRAL / volume 0.75F / pitch 1.0F");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("node heart contract ok");
