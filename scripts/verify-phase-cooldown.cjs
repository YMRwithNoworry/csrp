const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const config = fs.readFileSync(path.join(root, "src/main/java/alku/csrp/Config.java"), "utf8");
const worldData = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/world/SrpWorldData.java"), "utf8");
const lureBlock = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/block/EvolutionLureBlock.java"), "utf8");
const failures = [];

const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

// The original "Phase N Delay" lockout is opt-in and off out of the box.
expect(config, /\.define\("phaseCooldownEnabled", false\)/,
  "phaseCooldownEnabled must default to false");
expect(config, /public static boolean phaseCooldownEnabled\(\) \{ return safe\(PHASE_COOLDOWN_ENABLED\); \}/,
  "Config.phaseCooldownEnabled() getter is missing");

// Phase changes only arm the lockout while the option is enabled.
expect(worldData,
  /if \(Config\.phaseCooldownEnabled\(\)\) \{\s*setCooldown\(level, EvolutionSystem\.phaseDelaySeconds\(evolutionPhase\)\);\s*\}/,
  "phase change must only start the delay while phaseCooldownEnabled is true");

// Saves written while the delay was always on must not stay locked out.
expect(worldData, /if \(dataVersion < 5 && !Config\.phaseCooldownEnabled\(\)\) \{\s*cooldownEnd = 0L;\s*\}/,
  "migration must clear a stale phase timer from older saves when the option is off");
expect(worldData, /DATA_VERSION = 5;/, "DATA_VERSION must be bumped to 5 for the stale-timer migration");

// The lure/carcass timer is a separate mechanic and keeps working.
expect(lureBlock, /addCooldown\(level, tier\.cooldownSeconds\(\)\)/,
  "lure cooldown must stay active independently of the phase delay");
expect(lureBlock, /addEvolutionPoints\(level, -tier\.carcassReduction\(\), true\)/,
  "carcass must still bypass the lure/carcass timer when subtracting points");

if (failures.length) {
  console.error("Phase cooldown verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Phase cooldown verification passed (opt-in, default off).\n");
