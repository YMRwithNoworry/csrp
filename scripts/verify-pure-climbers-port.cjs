const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const entity = read("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");

for (const [id, kind, width, height] of [
  ["grunt", "GRUNT", "0\\.7666F", "1\\.95F"],
  ["monarch", "MONARCH", "1\\.901F", "4\\.1F"],
  ["vigilante", "VIGILANTE", "1\\.6F", "3\\.1F"],
  ["warden", "WARDEN", "0\\.901F", "4\\.2F"]
]) {
  expect(registry,
    new RegExp(`"${id}"[\\s\\S]{0,160}?PureParasiteEntity\\.Kind\\.${kind}\\), ${width}, ${height}\\)`),
    `${id}: dimensions do not match the original constructor`);
}

expect(entity,
  /kind == Kind\.MONARCH \|\| kind == Kind\.VIGILANTE \|\| kind == Kind\.WARDEN\)[\s\S]{0,120}?(?:Attributes\.STEP_HEIGHT|ForgeMod\.STEP_HEIGHT_ADDITION\.get\(\)), 1\.0D/,
  "Monarch, Vigilante, and Warden are missing their original one-block step height");
expect(entity,
  /createNavigation\(Level level\)[\s\S]{0,180}?isClimberType\(getType\(\)\)[\s\S]{0,100}?new WallClimberNavigation\(this, level\)/,
  "Pure climbers do not use wall-climber navigation");
expect(entity,
  /isClimberType\(EntityType<\?> type\)[\s\S]{0,180}?ModEntities\.GRUNT[\s\S]{0,80}?ModEntities\.MONARCH[\s\S]{0,80}?ModEntities\.WARDEN/,
  "Grunt, Monarch, or Warden is absent from the climber navigation set");
expect(entity,
  /onClimbable\(\)[\s\S]{0,100}?Kind\.WARDEN[\s\S]{0,260}?!hasLineOfSight\(target\) && distanceToSqr\(target\) < 100\.0D[\s\S]{0,220}?hasLineOfSight\(target\) && target\.getY\(\) \+ 1\.0D < getY\(\)[\s\S]{0,180}?horizontalCollision \|\| super\.onClimbable\(\)/,
  "Warden does not preserve the original target-aware climbing rules");
expect(entity,
  /return activeKind\(\)\.climbs && horizontalCollision \|\| super\.onClimbable\(\)/,
  "Grunt and Monarch no longer climb whenever they collide with a wall");

if (failures.length) {
  console.error(`Pure climber verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Pure step-height and climber behavior is wired and verified.");
