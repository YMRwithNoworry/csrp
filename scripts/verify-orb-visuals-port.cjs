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
const scaryRenderer = read("src/main/java/alku/csrp/client/renderer/ScaryOrbRenderer.java");

expectMissing(entity, /ParticleTypes\.SOUL_FIRE_FLAME/, "Void orb still emits blue soul-fire particles");
expect(renderer, /VOID_ORB_DIAMETER\s*=\s*2\.4F/, "Void orb does not use its legacy visual diameter");
expect(renderer, /SPHERE_RADIUS\s*=\s*VOID_ORB_DIAMETER\s*\*\s*0\.5F/, "Void orb radius is not derived from its visual diameter");
expect(renderer, /orbvoid\.png/, "Void orb core texture is missing");
expect(renderer, /orbvoid_armor\.png/, "Void orb aura texture is missing");
expect(scaryRenderer, /SPHERE_RADIUS\s*=\s*0\.317F/, "Scary orb renderer does not use the original sphere radius");
expect(scaryRenderer, /getRenderScale\(partialTick\)/, "Scary orb renderer does not use the dynamic legacy size curve");
expect(entity, /FUSE_WIDTH_GROWTH\s*=\s*0\.8F/, "Scary orb fuse width growth is missing");
expect(entity, /FUSE_HEIGHT_GROWTH\s*=\s*0\.482F/, "Scary orb fuse height growth is missing");
expect(entity, /setVisualDimensions\(getVisualWidth\(\) \+ FUSE_WIDTH_GROWTH/, "Scary orb does not grow during its fuse");
expect(entity, /getDimensions\(Pose pose\)[\s\S]*?getVisualWidth\(\), getVisualHeight\(\)/,
  "Scary orb collision dimensions are not synchronized with its visual dimensions");

if (failures.length) {
  console.error("Void orb visual verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Void orb visual verification passed.");
