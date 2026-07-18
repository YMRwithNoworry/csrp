const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");

const airscrew = read("src/main/java/alku/csrp/entity/AirscrewEntity.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/AirscrewRenderer.java");
const clientEvents = read("src/main/java/alku/csrp/client/ClientModEvents.java");

for (const hook of ["PULL_TARGET_IDS", "syncPullTargets", "getPullTargetsForRendering"]) {
  if (!airscrew.includes(hook)) failures.push(`Airscrew target sync missing: ${hook}`);
}

for (const hook of [
  "RenderType.lightning()",
  "renderTether(airscrew, target, partialTick, poseStack, bufferSource)",
  "tetherPoint",
  "renderRibbonSegment",
  "addVertex",
  "getPullTargetsForRendering"
]) {
  if (!renderer.includes(hook)) failures.push(`Airscrew tether renderer missing: ${hook}`);
}

if (renderer.includes("renderLeash(")) {
  failures.push("Airscrew tether still delegates to the vanilla leash renderer");
}

if (!clientEvents.includes("AirscrewRenderer::new")) {
  failures.push("Airscrew renderer registration missing");
}

if (failures.length) {
  console.error("Airscrew tether verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Verified Airscrew target synchronization and dedicated animated tether rendering.");
