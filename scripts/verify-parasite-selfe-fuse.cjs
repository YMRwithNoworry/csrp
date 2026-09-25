const fs = require("node:fs");
const path = require("node:path");

// Verifies the legacy self-destruct fuse after it was factored into a shared component:
// ParasiteFuseState (SELFE sync + madeRng roll + fuseTime) and SelfeFuseOwner, wired into every
// parasite family, plus the preRenderCallback swell it drives.
// Usage: node scripts/verify-parasite-selfe-fuse.cjs

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

function expect(source, pattern, message) {
  if (!pattern.test(source)) failures.push(message);
}

const owner = read("src/main/java/alku/csrp/entity/SelfeFuseOwner.java");
const state = read("src/main/java/alku/csrp/entity/ParasiteFuseState.java");
const rules = read("src/main/java/alku/csrp/event/ParasiteCombatRules.java");
const render = read("src/main/java/alku/csrp/client/renderer/SelfeFuseRender.java");

// the shared component: SELFE sync data, the madeRng roll and the fuseTime budget
for (const [pattern, message] of [
  [/public static final int FUSE_TICKS = 40;/, "the legacy fuseTime = 40 is missing"],
  [/public static final EntityDataAccessor<Integer> SELFE =/, "the shared SELFE accessor is missing"],
  [/explodesOnDeath = \(byte\) owner\.getRandom\(\)\.nextInt\(2\)/, "the roll must use nextInt(2)"],
  [/owner\.level\(\)\.broadcastEntityEvent\(owner, \(byte\) 40\)/,
    "the legacy event 40 notification is missing"],
  [/public boolean advance\(LivingEntity owner\)/, "advance(LivingEntity) is missing"],
  [/return next >= FUSE_TICKS;/, "the fuse must end at FUSE_TICKS"],
  [/\(fuse \+ partialTick\) \/ \(float\) \(FUSE_TICKS - 2\)/,
    "the flash intensity must divide by fuseTime - 2"]
]) expect(state, pattern, message);

for (const [pattern, message] of [
  [/boolean willExplodeOnDeath\(\)/, "SelfeFuseOwner.willExplodeOnDeath is missing"],
  [/void startDyingFuse\(\)/, "SelfeFuseOwner.startDyingFuse is missing"],
  [/float getSelfeFlashIntensity\(float partialTick\)/,
    "SelfeFuseOwner.getSelfeFlashIntensity is missing"]
]) expect(owner, pattern, message);

// every family wires the fuse: the primitive chain, the three assimilated classes and the feral one
const families = [
  ["src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java", "PrimitiveParasiteEntity"],
  ["src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java", "AssimilatedParasiteEntity"],
  ["src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java", "AssimilatedVariantEntity"],
  ["src/main/java/alku/csrp/entity/SimHumanEntity.java", "SimHumanEntity"],
  ["src/main/java/alku/csrp/entity/FeralParasiteEntity.java", "FeralParasiteEntity"]
];
for (const [file, name] of families) {
  const source = read(file);
  expect(source, /SelfeFuseOwner/, `${name} does not implement SelfeFuseOwner`);
  expect(source, /private final ParasiteFuseState selfeFuse = new ParasiteFuseState\(\);/,
    `${name} is missing the shared fuse state`);
  expect(source, /builder\.define\(ParasiteFuseState\.SELFE, -1\)/,
    `${name} does not register the SELFE field`);
  expect(source, /protected void tickDeath\(\)/, `${name} is missing the fuse death tick`);
  expect(source, /ParasiteCombatRules\.selfExplode\(serverLevel, this\)/,
    `${name} does not burst at the end of the fuse`);
  expect(source, /selfeFuse\.willExplodeOnDeath\(this\)/, `${name} never rolls madeRng`);
}

// the shared rules hand any fused corpse to the fuse instead of exploding instantly
expect(rules, /parasite instanceof SelfeFuseOwner fuseOwner && fuseOwner\.willExplodeOnDeath\(\)/,
  "applyDeathGore must hand fused corpses to the fuse");
expect(rules, /public static void selfExplode\(ServerLevel level, LivingEntity parasite\)/,
  "selfExplode must be reachable from the families");

// renderers show the fuse burning
for (const [pattern, message] of [
  [/static void applySwelling\(SelfeFuseOwner owner, PoseStack poseStack, float partialTick\)/,
    "the shared swell helper is missing"],
  [/float pulse = 1\.0F \+ Mth\.sin\(swell \* 100\.0F\) \* swell \* 0\.01F;/,
    "the legacy sine pulse is missing"],
  [/poseStack\.scale\(horizontalScale, verticalScale, horizontalScale\)/,
    "the legacy swell scale is missing"]
]) expect(render, pattern, message);
for (const file of ["PrimitiveParasiteRenderer.java", "AssimilatedParasiteRenderer.java",
  "SimHumanRenderer.java"]) {
  expect(read(`src/main/java/alku/csrp/client/renderer/${file}`),
    /SelfeFuseRender\.applySwelling\(fuseOwner, poseStack, partialTick\)/,
    `${file} does not apply the fuse swell`);
}

if (failures.length) {
  console.error("Parasite SELFE fuse verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Parasite SELFE fuse verification passed.");
