const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (file) => fs.readFileSync(path.join(root, file), "utf8");
const primitive = read("src/main/java/alku/csrp/entity/LongarmsEntity.java");
const adapted = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const failures = [];

function expect(source, pattern, message) {
  if (!pattern.test(source)) failures.push(message);
}

expect(primitive,
  /triggerAttackAnimation\(\)[\s\S]{0,120}?swing\(InteractionHand\.MAIN_HAND\)/,
  "Primitive Longarms attacks do not synchronize the vanilla attack timer");
expect(primitive,
  /movementAnimation\(AnimationState<LongarmsEntity> state\)[\s\S]{0,180}?ParasiteAnimations\.isAttacking\(this\)[\s\S]{0,300}?COMBAT_STILL_ATTACK[\s\S]{0,120}?SPRINT_ATTACK/,
  "Primitive Longarms movement controller does not prioritize its original attack animations");
if (/triggerAnim\("attack_controller"/.test(primitive)) {
  failures.push("Primitive Longarms still relies on a non-restarting triggered attack loop");
}

expect(adapted,
  /LONGARMS_ATTACK_STATUS_1\s*=\s*ParasiteAnimations\.play\(this,\s*"get_attack_timer\.get_parasite_status_1"\)/,
  "Adapted Longarms original combat attack animation is not wired");
expect(adapted,
  /if \(kind == Kind\.LONGARMS\)[\s\S]{0,180}?ParasiteAnimations\.isAttacking\(this\)[\s\S]{0,120}?LONGARMS_ATTACK_STATUS_1/,
  "Adapted Longarms movement controller does not prioritize its attack animation");
expect(adapted,
  /triggerAttackAnimation\(\)[\s\S]{0,160}?activeKind\(\) == Kind\.LONGARMS[\s\S]{0,100}?swing\(InteractionHand\.MAIN_HAND\)/,
  "Adapted Longarms attacks do not synchronize the vanilla attack timer");
if (/activeKind == Kind\.BOLSTER \|\| activeKind == Kind\.LONGARMS[\s\S]{0,100}?bolster_attack_controller/.test(adapted)) {
  failures.push("Adapted Longarms still triggers the Bolster attack controller");
}

if (failures.length) {
  console.error(`Longarms attack animation verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Primitive and Adapted Longarms attack animations are wired and verified.");
