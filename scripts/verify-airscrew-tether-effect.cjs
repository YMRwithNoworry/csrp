const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const tetherTexture = path.join(root, "src/main/resources/assets/csrp/textures/entity/airscrew_tether.png");

const airscrew = read("src/main/java/alku/csrp/entity/AirscrewEntity.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/AirscrewRenderer.java");
const clientEvents = read("src/main/java/alku/csrp/client/ClientModEvents.java");

for (const hook of ["PULL_TARGET_IDS", "syncPullTargets", "getPullTargetsForRendering"]) {
  if (!airscrew.includes(hook)) failures.push(`Airscrew target sync missing: ${hook}`);
}

for (const hook of [
  "TETHER_TEXTURE",
  "RenderType.entityCutoutNoCull(TETHER_TEXTURE)",
  "shouldRender(AirscrewEntity airscrew, Frustum frustum",
  "renderTether(airscrew, target, partialTick, poseStack, bufferSource)",
  "setUv(u, v)",
  "LightTexture.FULL_BRIGHT",
  "getPullTargetsForRendering"
]) {
  if (!renderer.includes(hook)) failures.push(`Airscrew tether renderer missing: ${hook}`);
}

if (renderer.includes("RenderType.lightning()")) {
  failures.push("Airscrew tether still uses the invisible lightning ribbon path");
}

if (!clientEvents.includes("AirscrewRenderer::new")) {
  failures.push("Airscrew renderer registration missing");
}

if (!fs.existsSync(tetherTexture)) {
  failures.push("Airscrew legacy tether texture is missing");
} else {
  const signature = fs.readFileSync(tetherTexture).subarray(0, 8).toString("hex");
  if (signature !== "89504e470d0a1a0a") failures.push("Airscrew tether texture is not a PNG");
}

if (failures.length) {
  console.error("Airscrew tether verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Verified Airscrew target synchronization, legacy textured tether rendering, and tether texture.");
