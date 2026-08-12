const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const source = fs.readFileSync(
  path.join(root, "src/main/java/alku/csrp/entity/LongarmsEntity.java"), "utf8");
const adapted = fs.readFileSync(
  path.join(root, "src/main/java/alku/csrp/entity/AdaptedVariantEntity.java"), "utf8");
const failures = [];
const expect = (pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

expect(/goalSelector\.addGoal\(2, new LongarmsMeleeGoal\(\)\)/,
  "Primitive Longarms continuous melee goal is missing");
expect(/LongarmsMeleeGoal[\s\S]{0,500}?canContinueToUse\(\)[\s\S]{0,100}?return canUse\(\)/,
  "Primitive Longarms does not keep its melee goal while the target remains alive");
expect(/LongarmsMeleeGoal[\s\S]{0,1200}?distanceToSqr\(target\) <= 8\.0D && cooldown == 0[\s\S]{0,180}?performAoeAttack\(target\)[\s\S]{0,180}?cooldown = ATTACK_INTERVAL_TICKS/,
  "Primitive Longarms no longer repeats its AOE melee attack on cooldown at close range");
expect(/ATTACK_INTERVAL_TICKS = 10/,
  "Primitive Longarms original fast melee interval is missing");
expect(/getParasiteStatus\(\) == STATUS_SHOCKWAVE[\s\S]{0,100}?getNavigation\(\)\.stop\(\)[\s\S]{0,500}?performAoeAttack\(target\)/,
  "Primitive Longarms no longer attacks at close range while charging its shockwave");
const primitiveShockwave = source.match(
  /private final class ShockwaveGoal extends Goal \{([\s\S]*?)\n    }\n}/)?.[1] ?? "";
if (/setFlags\(/.test(primitiveShockwave)) {
  failures.push("Primitive Longarms shockwave still blocks its concurrent melee goal");
}

for (const forbidden of [
  "ATTACKS_BEFORE_REST",
  "ATTACK_REST_TICKS",
  "MELEE_RESTING",
  "MeleeAttacksSinceRest",
  "MeleeRestTicks",
  "LongarmsRecoveryGoal",
  "recordMeleeAttack",
  "isRestingAfterMeleeAttacks"
]) {
  if (source.includes(forbidden)) {
    failures.push(`Primitive Longarms still contains the non-original forced melee pause: ${forbidden}`);
  }
}

const adaptedShockwave = adapted.match(
  /private final class ShockwaveGoal extends Goal \{([\s\S]*?)\n    }\n\n    private final class CloakGoal/)?.[1] ?? "";
if (!adaptedShockwave) {
  failures.push("Could not isolate Adapted Longarms shockwave goal");
} else if (/setFlags\(/.test(adaptedShockwave)) {
  failures.push("Adapted Longarms shockwave still blocks its concurrent melee goal");
}
if (!/case LONGARMS[\s\S]{0,200}?new ShockwaveGoal\(\)[\s\S]{0,200}?new LongarmsMeleeGoal\(\)/.test(adapted)) {
  failures.push("Adapted Longarms dedicated continuous melee goal is missing");
}
if (!/LongarmsMeleeGoal[\s\S]{0,500}?canContinueToUse\(\)[\s\S]{0,100}?return canUse\(\)/.test(adapted)) {
  failures.push("Adapted Longarms melee goal does not remain active while its target is alive");
}
if (!/LONGARMS_MELEE_RANGE_SQR = 16\.0D[\s\S]*?LONGARMS_ATTACK_INTERVAL_TICKS = 10/.test(adapted)) {
  failures.push("Adapted Longarms original four-block melee range or ten-tick cadence is missing");
}
if (!/LongarmsMeleeGoal[\s\S]{0,1500}?longarmsShockwaveCharging[\s\S]{0,150}?getNavigation\(\)\.stop\(\)[\s\S]{0,800}?distanceToSqr\(target\) <= LONGARMS_MELEE_RANGE_SQR[\s\S]{0,200}?doHurtTarget\(target\)/.test(adapted)) {
  failures.push("Adapted Longarms does not keep checking its full melee range while charging a shockwave");
}
if (!/ShockwaveGoal[\s\S]{0,900}?longarmsShockwaveCharging = true[\s\S]{0,1500}?longarmsShockwaveCharging = false/.test(adapted)) {
  failures.push("Adapted Longarms shockwave charging state is not bounded by the skill goal");
}

if (failures.length) {
  console.error(`Longarms continuous melee verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Primitive Longarms continuous melee behavior is wired and verified.");
