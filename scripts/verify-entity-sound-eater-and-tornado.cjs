const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};
const entity = (name) => read(`src/main/java/alku/csrp/entity/${name}.java`);

// ---------------------------------------------------------------------------
// 1. Sound Eater (out109 monster/infected/EntityInfHuman skin 111 + ai/SoundEaterSoundHelper)
//
// out109 facts:
//   EntityInfHuman:627-634  1% roll -> setSkin(111), FOLLOW_RANGE 12, MOVEMENT_SPEED 0.32
//   EntityInfHuman:151-188  tickSoundMemory, 16x4x16 hearing box, loudness = walkedThisTick
//                           (x3 while sprinting), notifyHeardSound(pos, 60) + setTarget
//   EntityInfHuman:440-465  notifyHeardSound / getHeardSoundPos / tickSoundMemory / clearHeardSound
//   SoundEaterSoundHelper   broadcastSound(world, pos, radius, lifeTicks) for skin 111
//   SoundEaterBlockSoundHandler  break -> radius 16 / life 100, place -> radius 12 / life 80
// ---------------------------------------------------------------------------
const human = entity("SimHumanEntity");
expect(human, /EntityDataAccessor<Boolean> SOUND_EATER/, "SimHumanEntity has no synced Sound Eater flag");
expect(human, /SOUND_EATER_CHANCE = 0\.01F/, "the original 1% Sound Eater roll is missing");
expect(human, /SOUND_EATER_SKIN = 111/, "the legacy skin 111 marker is missing");
expect(human, /SOUND_EATER_FOLLOW_RANGE = 12\.0D/, "the Sound Eater follow range is not the original 12");
expect(human, /SOUND_EATER_MOVEMENT_SPEED = 0\.32D/, "the Sound Eater movement speed is not the original 0.32");
expect(human, /SPRINT_LOUDNESS_MULTIPLIER = 3\.0D/, "sprinting is not three times as loud");
expect(human, /HEARING_RANGE_HORIZONTAL = 16\.0D/, "the hearing box is not 16 blocks wide");
expect(human, /HEARING_RANGE_VERTICAL = 4\.0D/, "the hearing box is not 4 blocks tall");
expect(human, /MOVEMENT_SOUND_MEMORY_TICKS = 60/, "movement noise memory is not 60 ticks");
expect(human, /BLOCK_BREAK_SOUND_RADIUS = 16\.0D/, "block break broadcast radius is not 16");
expect(human, /BLOCK_BREAK_SOUND_LIFE_TICKS = 100/, "block break broadcast lifetime is not 100");
expect(human, /BLOCK_PLACE_SOUND_RADIUS = 12\.0D/, "block place broadcast radius is not 12");
expect(human, /BLOCK_PLACE_SOUND_LIFE_TICKS = 80/, "block place broadcast lifetime is not 80");
expect(human, /public SpawnGroupData finalizeSpawn\([\s\S]{0,500}?random\.nextFloat\(\) < SOUND_EATER_CHANCE/,
  "the Sound Eater variant is not rolled on spawn");
expect(human, /public void notifyHeardSound\(BlockPos pos, int lifeTicks\)/,
  "notifyHeardSound is missing");
expect(human, /public BlockPos getHeardSoundPos\(\)/, "getHeardSoundPos is missing");
expect(human, /public void tickSoundMemory\(\)/, "tickSoundMemory is missing");
expect(human, /public void clearHeardSound\(\)/, "clearHeardSound is missing");
expect(human, /public static void broadcastSound\(ServerLevel level, BlockPos pos, double radius, int lifeTicks\)/,
  "the SoundEaterSoundHelper broadcast is missing");
expect(human, /getLastHurtByMob\(\) == null && getTarget\(\) != null[\s\S]{0,80}?setTarget\(null\)/,
  "the Sound Eater does not drop a target once the noise and the revenge target are gone");
expect(human, /candidate\.isSpectator\(\) && !candidate\.isCreative\(\) && candidate\.isAlive\(\)/,
  "spectating / creative players are not excluded from the hearing scan");
expect(human, /isSprinting\(\)/, "the sprinting loudness multiplier is not applied");
expect(human, /output\.putBoolean\("sound_eater", isSoundEater\(\)\)/,
  "the Sound Eater flag is not persisted");
expect(human, /input\.getBooleanOr\("sound_eater", false\)/, "the Sound Eater flag is not restored");

const soundEvents = entity("SoundEaterSoundEvents");
expect(soundEvents, /@EventBusSubscriber\(modid = Csrp\.MODID\)/, "sound events are not on the mod event bus");
expect(soundEvents, /BreakBlockEvent event/, "block break is not handled");
expect(soundEvents, /BlockEvent\.EntityPlaceEvent event/, "block place is not handled");
expect(soundEvents, /SimHumanEntity\.broadcastSound\(level, event\.getPos\(\),\s*SimHumanEntity\.BLOCK_BREAK_SOUND_RADIUS, SimHumanEntity\.BLOCK_BREAK_SOUND_LIFE_TICKS\)/,
  "block break does not broadcast with the original radius/lifetime");
expect(soundEvents, /SimHumanEntity\.broadcastSound\(level, event\.getPos\(\),\s*SimHumanEntity\.BLOCK_PLACE_SOUND_RADIUS, SimHumanEntity\.BLOCK_PLACE_SOUND_LIFE_TICKS\)/,
  "block place does not broadcast with the original radius/lifetime");

// ---------------------------------------------------------------------------
// 2. Venkrol tornado (out109 entity/logic/VenkrolTornadoLogic.java:15-171)
//
// Call site out109 EntityVenkrolSIV:110-118 — server side, every tick, while
// venkrolTornadoEnabled && isThundering() && isRaining() && canSeeSky(pos.above()).
// ---------------------------------------------------------------------------
const nexus = entity("NexusParasiteEntity");
expect(nexus, /level\(\)\.isThundering\(\) && level\(\)\.isRaining\(\)/,
  "the tornado is not gated on a thunderstorm");
expect(nexus, /level\(\)\.canSeeSky\(blockPosition\(\)\.above\(\)\)/,
  "the tornado is not gated on open sky above the Venkrol");
expect(nexus, /final double maxRadius = 120\.0D/, "the tornado radius is not the original 120");
expect(nexus, /getX\(\) \+ maxRadius, getY\(\) \+ 50\.0D, getZ\(\) \+ maxRadius/,
  "the tornado box is not 120 wide / 50 tall");
expect(nexus, /target\.getY\(\) < getY\(\)/, "entities below the Venkrol are not excluded");
expect(nexus, /Csrp\.MODID\.equals\(id\.getNamespace\(\)\)/,
  "mod-namespace entities are not immune to the tornado");
for (const [distance, factor] of [[50.0, 0.05], [25.0, 0.1], [15.0, 0.2], [10.0, 0.35], [5.0, 0.55]]) {
  expect(nexus, new RegExp(`horizDist >= ${distance.toFixed(1)}D\\) \\{\\s*pullTierFactor = ${factor}D;`),
    `the tornado pull tier at ${distance} blocks is not the original ${factor}`);
}
expect(nexus, /pullTierFactor = 1\.0D;/, "the innermost pull tier is not the original 1.0");
expect(nexus, /double pullStrength = 0\.08D \* pullTierFactor;/, "base pull strength is not 0.08");
expect(nexus, /double swirlStrength = 0\.07D \* pullTierFactor;/, "base swirl strength is not 0.07");
expect(nexus, /double innerLiftRadius = 15\.0D;/, "the inner lift radius is not the original 15");
expect(nexus, /liftAccel = 0\.25D \* liftFactor;/, "the lift acceleration is not the original 0.25");
expect(nexus, /boolean inFlingZone = heightAboveVenkrol > 16\.0D && horizDist < 12\.0D;/,
  "the fling zone is not the original y>16 && r<12");
expect(nexus, /if \(heightAboveVenkrol > 18\.0D && horizDist > 18\.0D\) \{\s*return;\s*\}/,
  "the tornado does not skip targets above 18 / beyond 18");
expect(nexus, /pullStrength \*= 1\.4D \+ \(6\.0D - 1\.4D\) \* flingFactor;/, "the fling pull multiplier is not 1.4..6.0");
expect(nexus, /swirlStrength \*= 1\.0D \+ \(2\.3D - 1\.0D\) \* flingFactor;/, "the fling swirl multiplier is not 1.0..2.3");
expect(nexus, /Math\.max\(-1\.6D, motionY - 0\.16D \* flingFactor\)/, "the downward fling clamp is not -1.6");
expect(nexus, /Math\.min\(1\.2D, motionY \+ liftAccel\)/, "the upward lift clamp is not 1.2");
expect(nexus, /dotAway > 0\.0D \? 1\.0D : 0\.25D/, "the force scale against outward motion is not 0.25");
expect(nexus, /player\.isSpectator\(\)/, "spectating players are not exempt from the tornado");
expect(nexus, /player\.isCreative\(\) && player\.getAbilities\(\)\.flying/,
  "creative-flying players are not exempt from the tornado");
expect(nexus, /player\.getItemBySlot\(EquipmentSlot\.FEET\)\.is\(ModItems\.VENKROL_BOOTS\)/,
  "Venkrol Boots wearers are not exempt from the tornado");
expect(nexus, /target\.isPassenger\(\) \|\| target\.isRemoved\(\)/, "passengers / removed entities are not skipped");
expect(nexus, /target\.syncVelocity = true;/, "the tornado does not mark the target velocity as changed");

if (failures.length) {
  console.error(`verify-entity-sound-eater-and-tornado: ${failures.length} failure(s)`);
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}
console.log("verify-entity-sound-eater-and-tornado: ok");
