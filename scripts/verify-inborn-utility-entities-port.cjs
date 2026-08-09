const fs = require("node:fs");
const path = require("node:path");

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

const buglin = read("src/main/java/alku/csrp/entity/BuglinEntity.java");
const worker = read("src/main/java/alku/csrp/entity/WorkerEntity.java");
const movingFlesh = read("src/main/java/alku/csrp/entity/MovingFleshEntity.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");

expect(buglin, /AvoidEntityGoal<.*8\.0F, 1\.0, 1\.0/, "Buglin avoid speed must match EntityLodo");
expect(buglin, /new HurtByTargetGoal\(this\)\.setAlertOthers\(\)/,
  "Buglin hurt-by target alert is missing");
expect(buglin, /rupter\.finalizeSpawn\(serverLevel, serverLevel\.getCurrentDifficultyAt\(blockPosition\(\)/,
  "Buglin growth does not initialize the Rupter spawn state");
expect(buglin, /rupter\.setCustomName\(getCustomName\(\)\)/, "Buglin growth does not copy identity");
expect(buglin, /rupter\.setPersistenceRequired\(\)/, "Buglin growth does not preserve persistence");

for (const [pattern, message] of [
  [/MAX_HEALTH, 7\.0D/, "Worker health does not reuse Lodo health"],
  [/ARMOR, 1\.5D/, "Worker armor does not reuse Lodo armor"],
  [/ATTACK_DAMAGE, 3\.0D/, "Worker damage does not reuse Lodo damage"],
  [/KNOCKBACK_RESISTANCE, 0\.05D/, "Worker knockback resistance does not reuse Lodo value"],
  [/MOVEMENT_SPEED, 0\.30D/, "Worker movement speed is wrong"],
  [/FOLLOW_RANGE, 16\.0D/, "Worker follow range is wrong"],
  [/findFloor\(serverLevel, new BlockPos\(x, current\.getY\(\), z\), 5\)/,
    "Worker does not use the original five-step floor search"],
  [/BlockPos placement = floor\.below\(\)/, "Worker does not replace the surface block"],
  [/Math\.floorMod\(x, BUILDING_GRID\) != 0 && Math\.floorMod\(z, BUILDING_GRID\) != 0/,
    "Worker defense grid condition is not the original 13/26 grid"]
]) expect(worker, pattern, message);

for (const [pattern, message] of [
  [/EVOLUTION_DELAY_TICKS = 70/, "Moving Flesh fuse length is wrong"],
  [/EVOLUTION_FUSE_INCREMENT = 2/, "Moving Flesh fuse increment is wrong"],
  [/REGEN_PER_TICK = 0\.007F/, "Moving Flesh regeneration rate is wrong"],
  [/EntityDataAccessor<Integer> MERGE_VALUE/, "Moving Flesh merge value is not synchronized"],
  [/setMergeValue\(getMergeValue\(\) \+ other\.getMergeValue\(\)\)/,
    "Moving Flesh merge values are not accumulated"],
  [/getAttribute\(Attributes\.MOVEMENT_SPEED\)[\s\S]*?getValue\(\) - 0\.01D/,
    "Moving Flesh merge speed penalty is missing"],
  [/MobsConfig\.mergeSystemRandom\(\)/, "Moving Flesh random merge config is missing"],
  [/MobsConfig\.mergeSystemMobList\(\)/, "Moving Flesh merge table config is missing"],
  [/MobsConfig\.mergeSystemMobHealth\(\)/, "Moving Flesh health config is missing"],
  [/BuiltInRegistries\.ENTITY_TYPE/, "Moving Flesh configured entity lookup is missing"],
  [/EvolutionSystem\.addPoints\(serverLevel, EvolutionSystem\.VALUE_MERGE/,
    "Moving Flesh merge evolution points are missing"]
]) expect(movingFlesh, pattern, message);

expect(config, /mergeSystemRandom/, "Moving Flesh random merge config is not registered");
expect(config, /mergeSystemMobHealth/, "Moving Flesh health config is not registered");
expect(config, /mergeSystemMobList/, "Moving Flesh table config is not registered");
expect(config, /srparasites:pri_summoner;0/, "Original nine-entry merge table is incomplete");
expect(config, /validMergeMobEntry/, "Moving Flesh table validator is missing");
expect(config, /entity instanceof WorkerEntity\) return -1\.0D/,
  "Worker follow range is incorrectly overridden by generic primitive config");
expect(config, /entity instanceof MovingFleshEntity\) return ADAPTED_FOLLOW\.get\(\)/,
  "Moving Flesh follow range does not use the adapted config");

if (failures.length) {
  console.error("Inborn utility entity verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Inborn utility entity verification passed.");
