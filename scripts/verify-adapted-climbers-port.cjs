const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const entity = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");

expect(registry,
  /"ada_longarms"[\s\S]{0,160}?AdaptedVariantEntity\.Kind\.LONGARMS\), 0\.901F, 3\.5F\)/,
  "Adapted Longarms dimensions do not match EntityShycoAdapted");
expect(registry,
  /"ada_viscera"[\s\S]{0,160}?AdaptedVariantEntity\.Kind\.VISCERA\), 1\.511F, 3\.655F\)/,
  "Adapted Viscera dimensions do not match EntityGimAdapted");
expect(entity,
  /kind == Kind\.LONGARMS \|\| kind == Kind\.VISCERA\)[\s\S]{0,120}?(?:Attributes\.STEP_HEIGHT|ForgeMod\.STEP_HEIGHT_ADDITION\.get\(\)), 1\.0D/,
  "Adapted Longarms and Viscera are missing their original one-block step height");
expect(entity,
  /createNavigation\(Level level\)[\s\S]{0,360}?isAdditionalClimberType\(getType\(\)\)[\s\S]{0,100}?new WallClimberNavigation\(this, level\)/,
  "Adapted climbers do not use wall-climber navigation");
expect(entity,
  /isAdditionalClimberType\(EntityType<\?> type\)[\s\S]{0,160}?ADA_LONGARMS[\s\S]{0,80}?ADA_VISCERA/,
  "Adapted Longarms or Viscera is absent from the climber navigation set");
expect(entity,
  /onClimbable\(\)[\s\S]{0,700}?Kind\.LONGARMS \|\| activeKind\(\) == Kind\.VISCERA[\s\S]{0,260}?!hasLineOfSight\(target\) && distanceToSqr\(target\) < 100\.0D[\s\S]{0,220}?hasLineOfSight\(target\) && target\.getY\(\) \+ 1\.0D < getY\(\)[\s\S]{0,180}?horizontalCollision \|\| super\.onClimbable\(\)/,
  "Adapted climbers do not preserve the original target-aware climbing rules");

if (failures.length) {
  console.error(`Adapted climber verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Adapted Longarms and Viscera climbing behavior is wired and verified.");
