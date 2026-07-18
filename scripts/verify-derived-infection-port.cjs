const fs = require("fs");
const path = require("path");

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

function expect(text, pattern, message) {
  if (!pattern.test(text)) failures.push(message);
}

const derived = read("src/main/java/alku/csrp/entity/DerivedParasiteEntity.java");
const kirin = read("src/main/java/alku/csrp/entity/KirinEntity.java");
const draconite = read("src/main/java/alku/csrp/entity/DraconiteEntity.java");
const projectile = read("src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java");
const infection = read("src/main/java/alku/csrp/infection/InfectionMechanics.java");
const infectionEvents = read("src/main/java/alku/csrp/infection/InfectionEvents.java");

expect(kirin, /class KirinEntity extends DerivedParasiteEntity/,
  "Kirin is not wired to the derived parasite behavior");
expect(draconite, /class DraconiteEntity extends DerivedParasiteEntity/,
  "Draconite is not wired to the derived parasite behavior");
expect(infection, /applyCoth\(/, "COTH infection application is missing");
expect(infection, /convertInfectedHost\(/, "COTH host conversion is missing");
expect(infectionEvents, /infectFromParasiteHit/, "Parasite hit infection hook is missing");
expect(infectionEvents, /convertTerminalCothHost/, "Terminal COTH death hook is missing");

expect(derived, /COSMIC_ORB_COUNT\s*=\s*3/, "Derived triple scary-orb count is missing");
expect(derived, /spawnCosmicOrb\(/, "Derived scary-orb spawn routine is missing");
expect(derived, /broadcastEntityEvent\(this, SHADOW_HIT_EVENT\)/,
  "Shadow hits do not broadcast their client feedback event");
expect(derived, /handleEntityEvent\(byte id\)/, "Derived shadow client event handler is missing");
expect(derived, /SOUL_FIRE_FLAME/, "Derived shadow hit cyan flash particles are missing");
expect(derived, /NEURAL_NEGATIVE_EFFECTS/, "Derived NeuroLock random negative-effect pool is missing");
expect(derived, /NEURAL_NEGATIVE_EFFECT_DURATION_TICKS\s*=\s*140/,
  "Derived NeuroLock effects do not retain the legacy seven-second duration");
expect(derived, /random\.nextInt\(NEURAL_NEGATIVE_EFFECTS\.size\(\)\)/,
  "Derived NeuroLock does not choose a random negative effect");
expect(derived, /removedAmplifierSum \+= effect\.getAmplifier\(\) \+ 1/,
  "Derived NeuroLock healing no longer scales with removed effect levels");
expect(derived, /NEURAL_LINK_TARGET_LIMIT\s*=\s*5/,
  "Derived NeuroLock no longer retains its five-target limit");

expect(draconite, /FIRE_BREATH_DURATION_TICKS\s*=\s*60/,
  "Draconite continuous fire-breath duration is missing");
expect(draconite, /beginMeteorRain\(/, "Draconite meteor-rain ability is missing");
expect(draconite, /LIGHT_BARRAGE_COUNT\s*=\s*20/,
  "Draconite tracking light barrage is missing");
expect(projectile, /case LIGHT/, "Projectile light-mode handling is missing");
expect(projectile, /Mode\.METEOR/, "Projectile meteor mode is missing");

if (failures.length) {
  console.error("Derived and infection port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Derived and infection port verification passed.");
