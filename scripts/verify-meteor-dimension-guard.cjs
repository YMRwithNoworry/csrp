const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const read = (file) => fs.readFileSync(path.join(root, file), "utf8");
const config = read("src/main/java/alku/csrp/Config.java");
const system = read("src/main/java/alku/csrp/world/MeteorInfectionSystem.java");
const commands = read("src/main/java/alku/csrp/command/SrpCommands.java");
const failures = [];
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

expect(config, /List\.of\("minecraft:the_nether", "minecraft:the_end"\)/,
  "The default meteor dimension blacklist must include the End");
expect(system, /return !Level\.END\.equals\(level\.dimension\(\)\)\s*&&\s*!Config\.meteorDimensionBlacklist\(\)/,
  "Meteor eligibility must hard-block the End and honor the configured blacklist");
expect(system, /public static void tick\(LevelTickEvent\.Post event\)[\s\S]*?if \(!isDimensionAllowed\(level\)\)/,
  "Periodic meteors must use the shared dimension guard");
expect(system, /public static boolean spawnMeteor\(ServerLevel level\)[\s\S]*?if \(!isDimensionAllowed\(level\)\)/,
  "Player-targeted meteor launches must use the shared dimension guard");
expect(system, /public static boolean spawnMeteorAround\(ServerLevel level, BlockPos center\)[\s\S]*?if \(!isDimensionAllowed\(level\)\)/,
  "Center-targeted meteor launches must use the shared dimension guard");
expect(system, /public static boolean spawnMeteor\(ServerLevel level, Vec3 origin, Vec3 target\)[\s\S]*?if \(!isDimensionAllowed\(level\)\)/,
  "Direct meteor launches must use the shared dimension guard");
expect(commands, /Meteor impacts are disabled in this dimension/,
  "Meteor commands must explain when the current dimension is unsupported");

if (failures.length) {
  console.error("Meteor dimension guard verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Meteor dimension guard verification passed.");
