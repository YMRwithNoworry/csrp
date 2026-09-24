const fs = require("node:fs");

const read = (path) => fs.readFileSync(path, "utf8");
const failures = [];
const expect = (condition, message) => {
  if (!condition) failures.push(message);
};

const client = read("src/main/java/alku/csrp/client/weather/SRPBlizzardClient.java");
const direction = read("src/main/java/alku/csrp/client/weather/SRPBlizzardDirectionClient.java");
const events = read("src/main/java/alku/csrp/client/weather/SRPBlizzardClientEvents.java");
const sound = read("src/main/java/alku/csrp/client/weather/SoundBlizzardReverse.java");
const reversePayload = read("src/main/java/alku/csrp/network/MsgSyncBlizzardReverse.java");
const starPayload = read("src/main/java/alku/csrp/network/MsgSyncStarType.java");
const payloadRegistry = read("src/main/java/alku/csrp/world/star/SrpStarPayloads.java");
const soundCatalog = read("src/main/java/alku/csrp/registry/SoundEventCatalog.java");
const soundsJson = read("src/main/resources/assets/csrp/sounds.json");

// --- SRPBlizzardClient: cold star + vanilla rain strength -------------------------------
for (const token of ["StarWorldClientState.starType() == SrpStarType.COLD",
  "level.dimension() != Level.OVERWORLD", "level.getRainLevel(partialTicks)",
  "Mth.clamp(level.getRainLevel(partialTicks), 0.0F, 1.0F)"])
  expect(client.includes(token), `SRPBlizzardClient is missing original behaviour: ${token}`);
// 1.12.2 World#getSunBrightness is gone; the same formula must be rebuilt locally.
for (const token of ["getOverworldClockTime()", "getRainLevel(partialTicks) * 5.0F / 16.0F",
  "getThunderLevel(partialTicks) * 5.0F / 16.0F", "brightness * 0.8F + 0.2F"])
  expect(client.includes(token), `SRPBlizzardClient daylight factor is missing: ${token}`);

// --- SRPBlizzardDirectionClient: verbatim momentum state machine ------------------------
expect(direction.includes("ClientTickEvent.Post"),
  "SRPBlizzardDirectionClient no longer ticks on ClientTickEvent.Post");
for (const token of ["magnitude - 0.065F", "motion + 0.045F", "blackBlend + 0.075F",
  "blackBlend - 0.125F", "blackBlend + 0.08F", "blackBlend - 0.08F", "holdTicks < 8",
  "blackBlend >= 0.999F", "fadingBackToWhite"])
  expect(direction.includes(token), `blizzard direction state machine lost original constant: ${token}`);
expect(direction.includes("getSoundManager().play(new SoundBlizzardReverse())"),
  "the reverse switch no longer plays SoundBlizzardReverse through the sound manager");
expect(direction.includes("public static void reset()"),
  "SRPBlizzardDirectionClient must expose reset() for level changes");

// --- SoundBlizzardReverse: MovingSound replacement --------------------------------------
for (const token of ["extends AbstractTickableSoundInstance", "SoundSource.WEATHER",
  "SoundInstance.Attenuation.NONE", "ModSounds.get(\"blizzard_reverse\")", "player.getEyeY()"])
  expect(sound.includes(token), `SoundBlizzardReverse is missing original behaviour: ${token}`);
expect(/public void tick\(\)[\s\S]{0,200}stop\(\)/.test(sound),
  "SoundBlizzardReverse no longer stops itself when the player is gone");
expect(!/extends\s+MovingSound|import\s+net\.minecraft\.client\.audio\.MovingSound/.test(sound),
  "SoundBlizzardReverse still extends/imports the removed 1.12.2 MovingSound base class");

// --- SRPBlizzardClientEvents: overlay + level reset ------------------------------------
for (const token of ["RenderGuiEvent.Pre", "TINT_RED = 205", "TINT_GREEN = 215", "TINT_BLUE = 224",
  "intensity * 0.24F", "alphaByte << 24 | TINT_RED << 16 | TINT_GREEN << 8 | TINT_BLUE",
  "getGameTimeDeltaPartialTick(false)", "ClientTickEvent.Post"])
  expect(events.includes(token), `SRPBlizzardClientEvents is missing original behaviour: ${token}`);

// --- payloads --------------------------------------------------------------------------
for (const token of ["implements CustomPacketPayload", "buffer.writeBoolean(reversed)",
  "buffer.readBoolean()", "SRPBlizzardDirectionClient.setReverseRequested(payload.reversed)",
  "PacketDistributor.sendToPlayer"])
  expect(reversePayload.includes(token), `MsgSyncBlizzardReverse is missing: ${token}`);
for (const token of ["implements CustomPacketPayload", "buffer.writeVarInt(starType.value())",
  "SrpStarType.byValue(buffer.readVarInt())", "StarWorldClientState.update(payload.starType)",
  "PacketDistributor.sendToPlayer"])
  expect(starPayload.includes(token), `MsgSyncStarType is missing: ${token}`);
// Both payloads must stay on the same data source as the pre-existing star sync.
expect(starPayload.includes("alku.csrp.celestial.client.StarWorldClientState"),
  "MsgSyncStarType does not feed the shared StarWorldClientState");
for (const token of ["RegisterPayloadHandlersEvent", "registrar(\"1\")",
  "MsgSyncBlizzardReverse.STREAM_CODEC", "MsgSyncStarType.STREAM_CODEC"])
  expect(payloadRegistry.includes(token), `SrpStarPayloads is missing registration step: ${token}`);

// --- sound asset ----------------------------------------------------------------------
expect(soundCatalog.includes("\"blizzard_reverse\""),
  "SoundEventCatalog no longer registers the blizzard_reverse sound event");
expect(/"blizzard_reverse"\s*:\s*\{[\s\S]{0,200}csrp:misc\/snow_reversal/.test(soundsJson),
  "sounds.json no longer maps blizzard_reverse to the original csrp:misc/snow_reversal asset");
expect(fs.existsSync("src/main/resources/assets/csrp/sounds/misc/snow_reversal.ogg"),
  "original blizzard reverse sound file is missing");

if (failures.length) {
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}
console.log("Verified cold-star blizzard client state, wind direction machine, reverse sound, overlay and star/reverse payloads.");
