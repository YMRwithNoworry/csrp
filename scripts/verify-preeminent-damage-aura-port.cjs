const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
// The 26.3 sources are checked out with CRLF line endings, so normalise before
// multi-line matching (single-line literals are unaffected either way).
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8").replace(/\r\n/g, "\n");
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};
const bodyOf = (source, header) => {
  const match = source.match(new RegExp(`${header}[\\s\\S]*?\\n    \\}`));
  return match ? match[0] : "";
};

const preeminent = read("src/main/java/alku/csrp/entity/PreeminentParasiteEntity.java");
const damage = read("src/main/java/alku/csrp/entity/HaunterDamageEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");

expect(preeminent,
  /\(activeKind == Kind\.BOGLE \|\| activeKind == Kind\.WRAITH\)[\s\S]{0,180}?tickCount, STEALTH_CHECK_INTERVAL\) == STEALTH_CHECK_OFFSET\)[\s\S]{0,80}?applyFlyingAura\(\)/,
  "Bogle and Wraith no longer trigger their aura at the original periodic offset");
expect(preeminent,
  /STEALTH_CHECK_INTERVAL = 20[\s\S]{0,100}?STEALTH_CHECK_OFFSET = 10/,
  "Bogle and Wraith aura timing is no longer 20 ticks with offset 10");
expect(preeminent,
  /applyFlyingAura\(\)[\s\S]{0,240}?getBoundingBox\(\)\.inflate\(3\.0D\)[\s\S]{0,180}?target != this[\s\S]{0,100}?target\.isAlive\(\)[\s\S]{0,100}?\!\(target instanceof Parasite\)/,
  "Flying aura does not search the original three-block area for non-parasite targets");
const auraMatch = preeminent.match(
  /private void applyFlyingAura\(\) \{([\s\S]*?)\n    \}\n\n    private void applyFlightLimits/);
const aura = auraMatch?.[1] ?? "";
// Isolation is now a hard precondition: without the exact aura body the
// remaining aura assertions would silently run against an empty string.
if (!aura.includes("getEntitiesOfClass(")) {
  failures.push("Could not isolate the flying aura implementation");
} else {
  expect(aura,
    /ModEntities\.HAUNTER_DAMAGE\.get\(\)\.create\(level\(\), EntitySpawnReason\.MOB_SUMMONED\)[\s\S]{0,120}?damage\.configure\(this, target\.position\(\), 2\.5F\)[\s\S]{0,120}?addFreshEntity\(damage\)/,
    "Bogle and Wraith do not create a 2.5-strength EntityDamage equivalent per target");
}
if (/doHurtTarget\(|setDeltaMovement\(/.test(aura)) {
  failures.push("Flying aura still applies only an immediate hit or knockback");
}
const haunterAttack = bodyOf(preeminent, "private boolean performHaunterAoeAttack\\(LivingEntity center\\) \\{");
if (haunterAttack.length === 0) {
  failures.push("Could not isolate the Haunter area attack implementation");
} else {
  if (!/nearby\.size\(\) > 4[\s\S]*?damage\.configure\(this, target\.position\(\), 3\.0F\)/.test(haunterAttack)) {
    failures.push("Haunter no longer creates its EntityDamage equivalent with strength 3.0");
  }
  expect(haunterAttack,
    /ModEntities\.HAUNTER_DAMAGE\.get\(\)\.create\(level\(\), EntitySpawnReason\.MOB_SUMMONED\)[\s\S]{0,120}?damage\.configure\(this, target\.position\(\), 3\.0F\)[\s\S]{0,120}?addFreshEntity\(damage\)/,
    "Haunter does not register its 3.0-strength EntityDamage equivalent");
}

expect(registry,
  /"haunter_damage"[\s\S]{0,180}?\.sized\(1\.2F, 0\.9F\)/,
  "EntityDamage equivalent no longer has the original 1.2 by 0.9 dimensions");
expect(damage, /LIFETIME_TICKS = 10/, "EntityDamage equivalent no longer uses the original lifetime");
expect(damage,
  /getBoundingBox\(\)\.inflate\(0\.3D, 0\.0D, 0\.2D\)[\s\S]{0,300}?knockBack\(owner, target, knockbackStrength\)[\s\S]{0,120}?owner\.doHurtTarget\(serverLevel, target\)/,
  "EntityDamage equivalent does not repeatedly knock back and attack entities in the original hitbox");
expect(damage,
  /configure\(PreeminentParasiteEntity owner, Vec3 position, float knockbackStrength\)[\s\S]{0,180}?this\.knockbackStrength = knockbackStrength/,
  "EntityDamage knockback strength is not configurable");
const saveRead = bodyOf(damage, "protected void readAdditionalSaveData\\(net\\.minecraft\\.world\\.level\\.storage\\.ValueInput input\\) \\{");
const saveWrite = bodyOf(damage, "protected void addAdditionalSaveData\\(net\\.minecraft\\.world\\.level\\.storage\\.ValueOutput output\\) \\{");
if (saveRead.length === 0 || saveWrite.length === 0) {
  failures.push("EntityDamage knockback strength is not preserved across saves");
} else {
  expect(saveRead, /getFloatOr\("knockback_strength", 0\.0F\)[\s\S]{0,80}?knockbackStrength = input\.getFloatOr\("knockback_strength", 0\.0F\)/,
    "EntityDamage knockback strength is not restored from saves");
  expect(saveWrite, /putFloat\("knockback_strength", knockbackStrength\)/,
    "EntityDamage knockback strength is not written to saves");
}

if (failures.length) {
  console.error(`Preeminent damage aura verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Bogle, Wraith, and Haunter damage hitboxes are wired and verified.");
