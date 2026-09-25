const fs = require("node:fs");
const path = require("node:path");

// Verifies the legacy self-destruct fuse: SELFE sync state, the madeRng roll on the first hit,
// the 40 tick dyingBurst that holds the corpse, and the preRenderCallback swell it drives.
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

const primitive = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const rules = read("src/main/java/alku/csrp/event/ParasiteCombatRules.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/PrimitiveParasiteRenderer.java");

// SELFE sync data (legacy DataManager.register SELFE (int))
for (const [pattern, message] of [
  [/EntityDataAccessor<Integer> SELFE = SynchedEntityData\.defineId\(/,
    "the SELFE synced data accessor is missing"],
  [/builder\.define\(SELFE, -1\)/, "SELFE must default to -1 (no fuse)"],
  [/public int getSelfeState\(\)/, "getSelfeState() is missing"]
]) expect(primitive, pattern, message);

// madeRng: the 50% self-destruct roll is decided by the first hit
for (const [pattern, message] of [
  [/public boolean willExplodeOnDeath\(\)/, "willExplodeOnDeath() (legacy madeRng) is missing"],
  [/explodesOnDeath = \(byte\) random\.nextInt\(2\)/, "the roll must use nextInt(2)"],
  [/level\(\)\.broadcastEntityEvent\(this, \(byte\) 40\)/, "the legacy event 40 notification is missing"],
  [/willExplodeOnDeath\(\);\s*\r?\n        \}/, "hurt() must trigger the madeRng roll"]
]) expect(primitive, pattern, message);

// dyingBurst: hold the corpse for fuseTime = 40, then burst
for (const [pattern, message] of [
  [/SELFE_FUSE_TICKS = 40;/, "the legacy fuseTime = 40 is missing"],
  [/public void startDyingFuse\(\)/, "startDyingFuse() is missing"],
  [/public boolean isDyingFuseActive\(\)/, "isDyingFuseActive() is missing"],
  [/protected void tickDeath\(\)/, "the death-tick fuse override is missing"],
  [/if \(!isDyingFuseActive\(\)\) \{\s*\r?\n            super\.tickDeath\(\);\s*\r?\n            return;/,
    "without a fuse the corpse must tick normally"],
  [/ParasiteCombatRules\.selfExplode\(serverLevel, this\)/,
    "the fuse must end in the legacy selfExplode"],
  [/public float getSelfeFlashIntensity\(float partialTick\)/,
    "getSelfeFlashIntensity(float) is missing"],
  [/\(fuse \+ partialTick\) \/ \(float\) \(SELFE_FUSE_TICKS - 2\)/,
    "the flash intensity must divide by fuseTime - 2"]
]) expect(primitive, pattern, message);

// death handler starts the fuse instead of exploding instantly, and selfExplode is reachable
expect(rules, /primitive\.willExplodeOnDeath\(\)\) \{\s*\r?\n            primitive\.startDyingFuse\(\);/,
  "applyDeathGore must hand the corpse to the fuse");
expect(rules, /public static void selfExplode\(ServerLevel level, LivingEntity parasite\)/,
  "selfExplode must be callable from the entity");

// preRenderCallback swell driven by the fuse intensity
for (const [pattern, message] of [
  [/parasite\.getSelfeFlashIntensity\(partialTick\)/, "the renderer must read the fuse intensity"],
  [/poseStack\.scale\(horizontalScale, verticalScale, horizontalScale\)/,
    "the renderer must apply the legacy swell scale"]
]) expect(renderer, pattern, message);

if (failures.length) {
  console.error("Parasite SELFE fuse verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Parasite SELFE fuse verification passed.");
