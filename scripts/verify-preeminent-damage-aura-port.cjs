const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
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
expect(preeminent,
  /applyFlyingAura\(\)[\s\S]{0,500}?ModEntities\.HAUNTER_DAMAGE\.get\(\)\.create\(level\(\)\)[\s\S]{0,180}?damage\.configure\(this, target\.position\(\), 2\.5F\)[\s\S]{0,100}?addFreshEntity\(damage\)/,
  "Bogle and Wraith do not create a 2.5-strength EntityDamage equivalent per target");
expect(preeminent,
  /performHaunterAoeAttack\([\s\S]{0,900}?damage\.configure\(this, target\.position\(\), 3\.0F\)/,
  "Haunter no longer creates its EntityDamage equivalent with strength 3.0");
expect(preeminent,
  /applyFlyingAura\(\)[\s\S]{0,700}?\n    }\n\n    private void applyFlightLimits/,
  "Could not isolate the flying aura implementation");
const aura = preeminent.match(/private void applyFlyingAura\(\) \{([\s\S]*?)\n    }\n\n    private void applyFlightLimits/)?.[1] ?? "";
if (/doHurtTarget\(|setDeltaMovement\(/.test(aura)) {
  failures.push("Flying aura still applies only an immediate hit or knockback");
}

expect(registry,
  /"haunter_damage"[\s\S]{0,180}?\.sized\(1\.2F, 0\.9F\)/,
  "EntityDamage equivalent no longer has the original 1.2 by 0.9 dimensions");
expect(damage, /LIFETIME_TICKS = 10/, "EntityDamage equivalent no longer uses the original lifetime");
expect(damage,
  /getBoundingBox\(\)\.inflate\(0\.3D, 0\.0D, 0\.2D\)[\s\S]{0,300}?knockBack\(owner, target, knockbackStrength\)[\s\S]{0,100}?owner\.doHurtTarget\(target\)/,
  "EntityDamage equivalent does not repeatedly knock back and attack entities in the original hitbox");
expect(damage,
  /configure\(PreeminentParasiteEntity owner, Vec3 position, float knockbackStrength\)[\s\S]{0,180}?this\.knockbackStrength = knockbackStrength/,
  "EntityDamage knockback strength is not configurable");
expect(damage,
  /contains\("knockback_strength"\)[\s\S]{0,100}?getFloat\("knockback_strength"\)[\s\S]{0,500}?putFloat\("knockback_strength", knockbackStrength\)/,
  "EntityDamage knockback strength is not preserved across saves");

if (failures.length) {
  console.error(`Preeminent damage aura verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Bogle, Wraith, and Haunter damage hitboxes are wired and verified.");
