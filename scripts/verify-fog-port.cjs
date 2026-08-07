const fs = require("node:fs");

const read = (path) => fs.readFileSync(path, "utf8");
const failures = [];
const expect = (condition, message) => {
  if (!condition) failures.push(message);
};

const block = read("src/main/java/alku/csrp/block/FogBlock.java");
const particles = read("src/main/java/alku/csrp/client/particle/CoolerFogParticle.java");
const overlay = read("src/main/java/alku/csrp/client/ParasiteFogOverlayEvents.java");
const model = read("src/main/resources/assets/csrp/models/block/fog.json");
const state = read("src/main/resources/assets/csrp/blockstates/fog.json");

expect(!model.includes("white_stained_glass"), "fog model still uses white stained glass");
expect(model.includes("csrp:block/fog"), "fog model does not use the extracted original texture");
expect(fs.existsSync("src/main/resources/assets/csrp/textures/block/fog.png"),
  "original parasite fog texture is missing");
expect(fs.existsSync("src/main/resources/assets/csrp/textures/block/fog.png.mcmeta"),
  "original parasite fog animation metadata is missing");
for (const frame of ["fog_intro1", "fog_intro2", "fog_intro3", "fog_intro4", "fog_intro5",
  "fog1", "fog2", "fog3", "fog4"]) {
  expect(fs.existsSync(`src/main/resources/assets/csrp/textures/particle/fog/${frame}.png`),
    `original parasite fog particle frame ${frame} is missing`);
}
for (const stage of [0, 1, 2]) {
  expect(state.includes(`\"air=${stage}\"`), `fog blockstate is missing original air=${stage} stage`);
}
for (const token of ["IntegerProperty.create(\"air\", 0, 2)", "randomTick", "ModParticles.FOG",
  "ModSounds.get(\"block.fog\")", "setValue(AIR, 2)", "Blocks.AIR.defaultBlockState()",
  "getBlockSupportShape"])
  expect(block.includes(token), `FogBlock is missing original behavior: ${token}`);
for (const token of ["lifetime = 144", "quadSize = 10.0F", "frameForAge", "int frameAge = age++",
  "if (age++ >= lifetime)", "PARTICLE_SHEET_TRANSLUCENT"])
  expect(particles.includes(token), `CoolerFogParticle is missing original behavior: ${token}`);
expect(overlay.includes("RenderGuiEvent.Post") && overlay.includes("0.85F")
  && overlay.includes("TextureAtlas.LOCATION_BLOCKS") && overlay.includes("graphics.blit(0, 0, -90"),
  "original animated in-fog full-screen overlay is missing");
const registry = read("src/main/java/alku/csrp/registry/ModBlocks.java");
expect(registry.includes(".replaceable()") && registry.includes(".forceSolidOff()"),
  "fog block is not replaceable and non-solid like the original air-material block");

if (failures.length) {
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}
console.log("Verified original parasite fog block, texture, particles, stages, sound and camera overlay.");
