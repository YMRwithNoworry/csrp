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

function expectPng(relativePath) {
  const file = path.join(root, relativePath);
  const signature = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  if (!fs.existsSync(file) || !fs.readFileSync(file).subarray(0, signature.length).equals(signature)) {
    failures.push(`missing valid PNG ${relativePath}`);
  }
}

const kirin = read("src/main/java/alku/csrp/entity/KirinEntity.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/VoidOrbRenderer.java");
const particles = read("src/main/java/alku/csrp/registry/ModParticles.java");
const warningParticle = read("src/main/java/alku/csrp/client/particle/KirinWarningParticle.java");
const clientEvents = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const particleDefinition = read("src/main/resources/assets/csrp/particles/kirin_warning.json");

expect(kirin, /BLINK_POS/, "Kirin blink target position is not synchronized to clients");
expect(kirin, /BLINK_TICKS/, "Kirin blink charge ticks are not synchronized to clients");
expect(kirin, /spawnBlinkWarningParticles/, "Kirin does not create its legacy blink warning rings");
expect(kirin, /ParticleTypes\.PORTAL/, "Kirin no longer emits its ambient portal particles");
expect(kirin, /ModParticles\.KIRIN_WARNING/, "Kirin blink warning is not routed through the custom particle type");
expect(renderer, /void render\(/, "Void orb renderer still has no render implementation");
expect(renderer, /renderSphere/, "Void orb renderer does not render the legacy sphere effect");
expect(renderer, /orbvoid\.png/, "Void orb renderer does not use the imported legacy core texture");
expect(renderer, /orbvoid_armor\.png/, "Void orb renderer does not use the imported legacy aura texture");
expect(particles, /KIRIN_WARNING/, "Kirin warning particle type is not registered");
expect(warningParticle, /class KirinWarningParticle/, "Kirin warning particle renderer is missing");
expect(warningParticle, /sizeBlocks/, "Kirin warning particle does not preserve its legacy size");
expect(clientEvents, /registerSpriteSet\(ModParticles\.KIRIN_WARNING/, "Kirin warning particle provider is not registered on the client");
expect(particleDefinition, /csrp:kirin_warning/, "Kirin warning particle atlas definition is missing its legacy texture");

expectPng("src/main/resources/assets/csrp/textures/entity/orbvoid.png");
expectPng("src/main/resources/assets/csrp/textures/entity/orbvoid_armor.png");
expectPng("src/main/resources/assets/csrp/textures/particle/kirin_warning.png");

if (failures.length) {
  console.error("Kirin visual-effects verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Kirin visual-effects verification passed.");
