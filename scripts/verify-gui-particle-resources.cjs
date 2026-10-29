const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const assets = "src/main/resources/assets/csrp";
const failures = [];
const exists = (relative) => fs.existsSync(path.join(root, relative));

const iconTextures = {
  adapted_icon: "potion_adapted",
  crude_icon: "potion_crude",
  dark_days_icon: "abhorrence",
  potion_stolas_icon: "potion_spotted",
  primitive_icon: "potion_primitive",
  fog_nullifier_icon: "potion_dod_smoke_trail",
  ecstasy_icon: "brew_fear",
  guerilla_icon: "guerilla",
  roots_icon: "potion_nexus",
  enemy_of_enemy_icon: "potion_repel",
  cosmic_structural_failure_icon: "potion_link",
  pure_icon: "potion_pure",
  hellfire_chemical_warfare_icon: "potion_overheating",
  potion_columbus_icon: "potion_senses",
  hunt_season_icon: "potion_vomit",
  self_destruct_icon: "potion_thornshade_thorns"
};

for (const [id, texture] of Object.entries(iconTextures)) {
  const modelPath = `${assets}/models/item/${id}.json`;
  const definitionPath = `${assets}/items/${id}.json`;
  const texturePath = `${assets}/textures/item/${texture}.png`;
  if (!exists(modelPath)) {
    failures.push(`missing icon model ${modelPath}`);
    continue;
  }
  if (!exists(definitionPath)) failures.push(`missing icon item definition ${definitionPath}`);
  const legacyTexturePath = `${assets}/textures/gui/${texture}.png`;
  if (!exists(texturePath)) failures.push(`missing icon atlas texture ${texturePath}`);
  if (!exists(legacyTexturePath)) failures.push(`missing original GUI texture ${legacyTexturePath}`);
  if (exists(texturePath) && exists(legacyTexturePath) &&
      !fs.readFileSync(path.join(root, texturePath)).equals(fs.readFileSync(path.join(root, legacyTexturePath)))) {
    failures.push(`${texturePath} must retain the original GUI texture bytes`);
  }
  const model = JSON.parse(fs.readFileSync(path.join(root, modelPath), "utf8"));
  if (model.textures?.layer0 !== `csrp:item/${texture}`) {
    failures.push(`${modelPath} must bind csrp:item/${texture}`);
  }
}

const particleTextures = [
  "flesh_assimilated1", "flesh_assimilated2", "flesh_assimilated3",
  "flesh_pure1", "flesh_pure2", "flesh_pure3",
  "flesh_primitive1", "flesh_primitive2", "flesh_primitive3",
  "flesh_adapted1", "flesh_adapted2", "flesh_adapted3"
].map((id) => `${assets}/textures/particle/gore/${id}.png`).concat(
  ["vomit1", "vomit2", "vomit3", "vomit4", "vomit5", "vomit6"]
    .map((id) => `${assets}/textures/particle/vomit/${id}.png`)
);
for (const texturePath of particleTextures) {
  if (!exists(texturePath)) failures.push(`missing particle texture ${texturePath}`);
}

const particles = fs.readFileSync(path.join(root, "src/main/java/alku/csrp/registry/ModParticles.java"), "utf8");
const clientEvents = fs.readFileSync(path.join(root, "src/main/java/alku/csrp/client/ClientModEvents.java"), "utf8");
for (const [id, provider] of [["gore_cloud", "GoreCloudParticle"], ["gore_splash", "GoreSplashParticle"]]) {
  const definitionPath = `${assets}/particles/${id}.json`;
  if (!exists(definitionPath)) {
    failures.push(`missing particle definition ${definitionPath}`);
    continue;
  }
  if (!particles.includes(`PARTICLES.register("${id}"`)) {
    failures.push(`${id} particle type is not registered`);
  }
  if (!clientEvents.includes(`ModParticles.${id.toUpperCase()}.get(), ${provider}.Provider::new`)) {
    failures.push(`${id} particle sprite provider is not registered`);
  }
  const definition = JSON.parse(fs.readFileSync(path.join(root, definitionPath), "utf8"));
  for (const texture of definition.textures || []) {
    const [namespace, textureId] = texture.includes(":") ? texture.split(":") : ["minecraft", texture];
    if (namespace === "csrp" && !exists(`${assets}/textures/particle/${textureId}.png`)) {
      failures.push(`${definitionPath} references missing particle atlas sprite ${texture}`);
    }
  }
}

function checkResourcePaths(directory, relative = "") {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const entryPath = path.join(directory, entry.name);
    const resourcePath = path.join(relative, entry.name);
    if (/[ ()]/.test(entry.name)) failures.push(`invalid resource path segment ${resourcePath}`);
    if (entry.isDirectory()) checkResourcePaths(entryPath, resourcePath);
  }
}
checkResourcePaths(path.join(root, assets));

if (failures.length) {
  console.error(`GUI and particle verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log(`GUI and particle verification passed (${Object.keys(iconTextures).length} icons and ${particleTextures.length} particle textures checked).`);
