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

function expectMissing(text, pattern, message) {
  if (pattern.test(text)) failures.push(message);
}

const entity = read("src/main/java/alku/csrp/entity/ScaryOrbEntity.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/VoidOrbRenderer.java");

expectMissing(entity, /ParticleTypes\.SOUL_FIRE_FLAME/, "Void orb still emits blue soul-fire particles");
expect(renderer, /VOID_ORB_DIAMETER\s*=\s*2\.4F/, "Void orb does not use its legacy visual diameter");
expect(renderer, /SPHERE_RADIUS\s*=\s*VOID_ORB_DIAMETER\s*\*\s*0\.5F/, "Void orb radius is not derived from its visual diameter");
expect(renderer, /orbvoid\.png/, "Void orb core texture is missing");
expect(renderer, /orbvoid_armor\.png/, "Void orb aura texture is missing");

if (failures.length) {
  console.error("Void orb visual verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Void orb visual verification passed.");
