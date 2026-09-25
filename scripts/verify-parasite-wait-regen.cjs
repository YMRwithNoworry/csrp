const fs = require("node:fs");
const path = require("node:path");

// Verifies the legacy AI wait state machine (EntityAIWait / setWait), the killcount-gated
// regeneration (primitiveRegen / regenEff) and the 20% retaliation RAGE on being hit.
// Usage: node scripts/verify-parasite-wait-regen.cjs

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

const config = read("src/main/java/alku/csrp/Config.java");
const primitive = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const rules = read("src/main/java/alku/csrp/event/ParasiteCombatRules.java");

// config: legacy SRPConfig.primitiveRegen = 4.0
expect(config, /defineInRange\("parasiteRegen", 4\.0D, 0\.0D, 1000\.0D\)/,
  "parasiteRegen (legacy primitiveRegen = 4.0) config is missing");
expect(config, /public static float parasiteRegen\(\)/, "parasiteRegen accessor is missing");

// EntityAIWait: priority-0 goal mutexing move/look/jump while the wait timer runs
for (const [pattern, message] of [
  [/private static final int KILL_WAIT_TICKS = 10;/, "the legacy 10 tick kill wait is missing"],
  [/public void setWait\(int ticks\)/, "setWait(int) is missing"],
  [/public int getWait\(\)/, "getWait() is missing"],
  [/private final class WaitGoal extends Goal/, "the EntityAIWait equivalent goal is missing"],
  [/setFlags\(EnumSet\.of\(Flag\.MOVE, Flag\.LOOK, Flag\.JUMP\)\)/,
    "the wait goal must mutex move/look/jump"],
  [/goalSelector\.addGoal\(0, new WaitGoal\(\)\)/,
    "the wait goal must be registered at priority 0"],
  [/public boolean canUse\(\) \{\r?\n\s*return waitTicks > 0;\r?\n\s*\}/,
    "the wait goal must run while the wait timer is positive"],
  [/if \(waitTicks > 0\) \{\r?\n\s*waitTicks--;\r?\n\s*\}/, "the wait timer must tick down"]
]) expect(primitive, pattern, message);

// primitiveRegen: once per second, wounded, killcount left, and one killcount per five heals
for (const [pattern, message] of [
  [/REGEN_EFFICIENCY_TICKS = 5;/, "the legacy regenEff = 5 cadence is missing"],
  [/private void tickRegeneration\(\)/, "tickRegeneration() is missing"],
  [/tickCount % 20 != 10 \|\| Config\.parasiteRegen\(\) <= 0\.0F \|\| parasiteKills <= 1/,
    "regeneration must fire once per second and require killcount > 1"],
  [/isOnFire\(\) \|\| getHealth\(\) >= getMaxHealth\(\)/,
    "regeneration must skip burning and undamaged parasites"],
  [/heal\(Config\.parasiteRegen\(\)\)/, "regeneration must heal the configured amount"],
  [/parasiteKills--;\r?\n\s*regenUse = REGEN_EFFICIENCY_TICKS;/,
    "every fifth heal must consume one killcount"],
  [/tickRegeneration\(\);/, "the tick loop must call tickRegeneration()"]
]) expect(primitive, pattern, message);

// retaliation RAGE and the kill wait hook
expect(rules, /parasite\.getRandom\(\)\.nextInt\(5\) == 0/,
  "the 20% retaliation RAGE roll (legacy nextInt(5) == 0) is missing");
expect(rules, /ParasiteCombatEffects\.healOnKill\(killer, event\.getEntity\(\)\);\n[\s\S]{0,200}parasite\.setWait\(10\)/,
  "a parasite kill must set the 10 tick AI wait");

if (failures.length) {
  console.error("Parasite wait/regen verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Parasite wait/regen verification passed.");
