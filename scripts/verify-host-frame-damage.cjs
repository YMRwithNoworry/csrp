const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const host = read("src/main/java/alku/csrp/entity/AbstractHostEntity.java");
const hostI = read("src/main/java/alku/csrp/entity/HostEntity.java");
const hostII = read("src/main/java/alku/csrp/entity/HostIIEntity.java");
const wave = read("src/main/java/alku/csrp/entity/WaveEntity.java");

// Legacy EntityHost.buriedC: drained once per tick, refilled to 80 while a target stays within
// 9 blocks, extended by 160 on every landing melee hit.
expect(host, /if \(buriedC > 0\) \{\s*buriedC--;/,
  "buriedC is not drained once per tick like the original func_70636_d");
expect(host, /buriedC = burrowRefill;/, "buriedC is not refilled while a target closes in");
expect(host, /BURROW_HIT_EXTENSION_TICKS = 160/, "buriedC is missing the original +160 melee extension");
expect(host, /distanceToSqr\(target\) <= 9\.0/, "burrow re-entry is missing the original 9 block threshold");
expect(host, /speed\.setBaseValue\(isBurrowed\(\) \? 0\.0 : baseMovementSpeed\)/,
  "burrowed hosts do not freeze to zero movement speed");

// Legacy func_70652_k / attackEntityAsMobAOE: melee only lands once the host is burrowed and
// taller than 1.0; otherwise the swing becomes a dive that refills buriedC to 160.
expect(host, /isBurrowed\(\) && getBbHeight\(\) >= 1\.0F/,
  "AOE melee is not gated on the original burrowed + height >= 1.0 condition");
expect(host, /buriedC = BURROW_HIT_EXTENSION_TICKS;[\s\S]*?getNavigation\(\)\.stop\(\)/,
  "a submerged swing no longer converts into a dive instead of dealing damage");
expect(host, /distanceToSqr\(entityIn\) > 4\.0/,
  "the dive fallback is missing the original 2 block reach limit");
expect(host, /applyPrimitiveMinimumDamage\(victim\)[\s\S]*?victim\.hurt\(damageSources\(\)\.mobAttack\(this\), damage\)/,
  "melee hits do not stack the tier minimum (frame) damage on top of the regular hit");
expect(host, /boolean doHurtTarget\(Entity entity\) \{\s*return performAoeMelee\(entity\);/,
  "doHurtTarget no longer routes through the original AOE melee rule");
expect(host, /MOB_SWIPE\.get\(\), 1\.0F, 1\.0F[\s\S]*?MOB_SWIPE\.get\(\), 1\.0F, 1\.25F[\s\S]*?MOB_SWIPE\.get\(\), 2\.0F, 1\.0F/,
  "the original three-swipe AOE cue is missing");

// Legacy checkBurrowed(): hitbox grows 0.05/tick to 3.5 (Host) / 0.09/tick to 7.5 (HostII).
expect(host, /FULL_BB_HEIGHT = 3\.5F/, "Host full burrow hitbox height is not 3.5F");
expect(host, /MIN_BB_HEIGHT = 0\.25F/, "Host minimum hitbox height is not 0.25F");
expect(host, /BURROW_BB_STEP = 0\.05F/, "Host burrow hitbox step is not 0.05F");
expect(host, /EntityDimensions\.scalable\(base\.width\(\), burrowHitboxHeight\)/,
  "the burrow hitbox growth is not applied to entity dimensions");
expect(hostII, /fullBurrowHeight\(\)[\s\S]*?return 7\.5F/, "HostII does not grow to its original 7.5F hitbox");
expect(hostII, /burrowHeightStep\(\)[\s\S]*?return 0\.09F/, "HostII does not use its original 0.09F hitbox step");

// Legacy func_82196_d: bombs only fly while burrowed, emerged, and within 13 blocks.
expect(host, /isBurrowed\(\) && getBbHeight\(\) >= 1\.0F[\s\S]*?distanceToSqr\(target\) <= 169\.0/,
  "the bomb attack is missing the original burrowed/emerged/169 condition gate");

// Legacy shockwave windup plays the hurt cue at border == 0.
expect(host, /playShockwaveWindupSound\(\)[\s\S]*?HOST_HURT\.get\(\), 4\.0F, pitch/,
  "the shockwave windup no longer plays the original hurt cue");

// Legacy spawnRupters(): 1/150 with a target (cap 4), else 1/400 (cap 3, no target assigned).
expect(host, /random\.nextInt\(hasTarget \? 150 : 400\) != 0/,
  "Mudo summoning does not use the original 1/150 vs 1/400 cadence");
expect(host, /size\(\) >= \(hasTarget \? targetingCap : idleCap\)/,
  "Mudo summoning does not honour the original separate target/idle caps");
expect(host, /if \(hasTarget\) \{\s*minion\.setTarget\(target\);/,
  "idle summons are still assigned a target unlike the original");
expect(hostI, /spawnMinions\(ModEntities\.RUPTER, RupterEntity\.class, 4, 3\)/,
  "Host does not summon Mudo with the original 4/3 caps");
expect(hostII, /spawnMinions\(ModEntities\.MANGLER, ManglerEntity\.class, 4, 3\)/,
  "HostII does not summon Nuuh with the original 4/3 caps");

// The wave keeps applying its per-tick minimum (frame) damage to everything in range.
expect(wave, /for \(LivingEntity victim : level\(\)\.getEntitiesOfClass\(LivingEntity\.class, damageArea,[\s\S]*?applyMinimumDamage\(victim\)/,
  "the wave no longer applies its per-tick minimum damage to everything in range");

if (failures.length) {
  console.error("Host frame-damage and burrow behavior verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Host frame damage, burrow gating and Mudo summoning match the original.");
