const fs = require("node:fs");
const path = require("node:path");

// Regression guard for the reported "Primitive Summoner has no walk animation" bug and the
// resolver behaviour that caused it for other mobs:
//   * pri_summoner must NOT be treated as a short-key resource (its clips are fully qualified);
//   * short-key resources must translate legacy func_78087_a.* requests onto walk/idle/attack;
//   * LegacyAnimationLibrary.findClip must accept bare requests for qualified keys and must
//     degrade a missing state clip to the closest transcribed base clip instead of playing nothing.
// Usage: node scripts/verify-animation-clip-resolution.cjs

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

const animations = read("src/main/java/alku/csrp/entity/ParasiteAnimations.java");
const library = read("src/main/java/alku/csrp/client/model/LegacyAnimationLibrary.java");

// the reported bug: pri_summoner's clips are qualified, so short-key mode must not include it
const shortKeyBlock = animations.match(
  /private static boolean usesShortAnimationKeys\(String resourceId\) \{([\s\S]*?)\n    \}/);
if (!shortKeyBlock) {
  failures.push("usesShortAnimationKeys(String) was not found");
} else if (/pri_summoner/.test(shortKeyBlock[1])) {
  failures.push("pri_summoner must not use short animation keys: its clips are "
    + "animation.pri_summoner.func_78087_a.*, and treating them as short keys left the mob "
    + "without any animation (including walking)");
}

// short-key resources name their clips idle/walk/fly/attack
for (const [pattern, message] of [
  [/action\.startsWith\("func_78087_a\.limb_swing"\)\)? \{\s*\n\s*return "walk";/,
    "short-key legacy limb_swing must resolve to the walk clip"],
  [/action\.startsWith\("func_78087_a\.age_in_ticks"\)\)? \{\s*\n\s*return "idle";/,
    "short-key legacy age_in_ticks must resolve to the idle clip"],
  [/action\.startsWith\("get_attack_timer"\)\)? \{\s*\n\s*return "attack";/,
    "short-key legacy get_attack_timer must resolve to the attack clip"]
]) expect(animations, pattern, message);

// resolver fallbacks in the clip library
for (const [pattern, message] of [
  [/private AnimationClip findClip\(String animationName\)/, "findClip(String) was not found"],
  [/while \(!candidate\.isEmpty\(\)\)/, "findClip must walk the request's segments"],
  [/AnimationClip degraded = clips\.get\(candidate\);/, "findClip must try the degraded key directly"],
  [/key\.length\(\) > candidate\.length\(\) && key\.endsWith\(candidate\)/,
    "findClip must accept a resource key that the request is a suffix of"],
  [/candidate = candidate\.substring\(0, dot\);/, "findClip must strip one segment per pass"]
]) expect(library, pattern, message);

if (failures.length) {
  console.error("Animation clip resolution verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Animation clip resolution verification passed.");
