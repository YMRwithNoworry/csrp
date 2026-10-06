const fs = require("node:fs");
const path = require("node:path");

// The 1.10.9 OST scheduler, which was still missing: SRPEventHandlerBus#soundTwo / soundThree /
// getMusicPhase / resetSouncTicker on the client, fed by SRPPacketRequestEvoPhaseClient (every 20
// ticks) -> SRPPacketUpdateEvoPhaseClient (phase + "inside an infection vector") and by
// SRPPacketMusicTrackCancelUpdateClientEvoPhase.
//
//   resetSouncTicker(in>0): musicTimer = min(musicTimer, in)
//   resetSouncTicker(0):    musicTimer = rand(musicMax - musicMin + 1) + musicMin when expired
//   soundThree:             clientVector-- ; musicTimer-- ; clientScent-- ;
//                           clientScent == -1 -> stop MUSIC ;
//                           musicTimer <= 0 && no queued tracks && clientCurrentEvoPhase > 0 ->
//                               stop MUSIC ; play getMusicPhase() through a "music" record
//   getMusicPhase:          parasite biome -> BIOME_MUSIC, else EVPHASE_<phase>_MUSIC (1..10)
//
// SRPConfig defaults: musicTrue = true, musicMin = 14420, musicMax = 18200 (SRPConfig.java:35-37).
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

const general = read("src/main/java/alku/csrp/config/GeneralConfig.java");
expect(general, /"musicEnabled", true/, "musicTrue no longer defaults to true");
expect(general, /"musicPhaseMin", 14420, 1, 2147483640/, "musicMin is no longer 14420");
expect(general, /"musicPhaseMax", 18200, 1, 2147483640/, "musicMax is no longer 18200");
expect(general, /public static boolean musicEnabled\(\)/, "the musicEnabled getter is gone");
expect(general, /public static int musicPhaseMin\(\)/, "the musicPhaseMin getter is gone");
expect(general, /public static int musicPhaseMax\(\)/, "the musicPhaseMax getter is gone");

const request = read("src/main/java/alku/csrp/network/RequestEvolutionPhasePayload.java");
expect(request, /"request_evolution_phase"/, "the request payload id changed");
expect(request, /data\.evolutionPhase\(\)/, "the reply no longer carries the dimension's phase");
expect(request, /vector\.pos\(\)\.distSqr\(pos\) <= radius \* radius/,
        "the infection-vector test no longer mirrors nearestInfectionPosition");
const update = read("src/main/java/alku/csrp/network/EvolutionPhaseClientPayload.java");
expect(update, /"evolution_phase_client"/, "the update payload id changed");
expect(update, /ClientPhaseMusicScheduler\.applyServerState\(payload\.phase, payload\.vector\)/,
        "the update payload no longer feeds the scheduler");
const payloads = read("src/main/java/alku/csrp/compendium/network/CompendiumPayloads.java");
expect(payloads, /playToServer\(RequestEvolutionPhasePayload\.TYPE/, "the phase request is not registered");
expect(payloads, /playToClient\(EvolutionPhaseClientPayload\.TYPE/, "the phase update is not registered");

const client = read("src/main/java/alku/csrp/client/sound/ClientPhaseMusicScheduler.java");
expect(client, /INITIAL_MUSIC_TIMER = 1000/, "musicTimer no longer starts at 1000");
expect(client, /SCENT_MUSIC_TICKS = 5000/, "the scent music timer is no longer 5000");
expect(client, /clientVector = 200;/, "the vector flag no longer sets clientVector to 200");
expect(client, /if \(clientVector > -10\) \{\s*clientVector--;/, "the clientVector countdown changed");
expect(client, /if \(previous > 0\) \{\s*stopMusic\(\);/, "the vector-expiry stop was removed");
expect(client, /resetSoundTicker\(200\)/, "the phase-change ticker reset is gone");
expect(client, /if \(musicTimer >= 0\) \{\s*musicTimer--;/, "the musicTimer countdown changed");
expect(client, /if \(clientScent == -1\) \{\s*stopMusic\(\);\s*clientScent = -10;/, "the scent stop changed");
expect(client, /musicTimer <= 0 && MUSIC_TO_REMOVE\.isEmpty\(\) && clientCurrentEvoPhase > 0/,
        "the play condition changed");
expect(client, /SimpleSoundInstance\.forMusic\(music\)/, "the track is no longer played as a music record");
expect(client, /stop\(null, SoundSource\.MUSIC\)/, "the vanilla MUSIC track is no longer stopped");
expect(client, /manager\.stop\(sound\)/, "queued vanilla tracks are no longer dropped");
expect(client, /"srparasites\.biome"/, "BIOME_MUSIC is no longer used");
for (const name of ["phaseone", "phasetwo", "phasethree", "phasefour", "phasefive", "phasesix",
    "phaseseven", "phaseeight", "phasenine", "phaseten"]) {
    expect(client, new RegExp(`"srparasites\\.${name}"`), `the ${name} music track is no longer referenced`);
}
expect(client, /player\.tickCount % 20 == 0/, "the 20-tick phase request is gone");
expect(client, /PlaySoundEvent event\)/, "vanilla music is no longer suppressed");
expect(client, /if \(clientScent <= 0 && clientVector <= 0\) \{\s*return;\s*\}/,
        "the origin-active escape hatch of soundTwo changed");
expect(client, /event\.setSound\(null\)/, "the suppressed vanilla track is no longer dropped");
expect(client, /ParasiteBiomeDecorator\.parasiteBiomeIndex\(player\.level\(\)\.getBiome\(player\.blockPosition\(\)\)\)/,
        "the parasite-biome check no longer uses the biome numbering");

const scent = read("src/main/java/alku/csrp/client/ScentMusicEvents.java");
expect(scent, /ClientPhaseMusicScheduler\.scentMusicStarted\(\)/,
        "the scent track no longer takes over the MUSIC channel");

const sounds = JSON.parse(read("src/main/resources/assets/csrp/sounds.json"));
for (const name of ["srparasites.biome", "srparasites.phaseone", "srparasites.phaseten"]) {
    if (!(name in sounds)) failures.push(`sounds.json no longer defines ${name}`);
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("phase music contract ok");
