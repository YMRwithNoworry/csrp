const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const source = fs.readFileSync(
  path.join(root, "src/main/java/alku/csrp/entity/NexusParasiteEntity.java"), "utf8");
const failures = [];
const expect = (pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

expect(/public void die\(DamageSource source\)[\s\S]{0,320}?activeKind\(\) == Kind\.DISPATCHER_SIV[\s\S]{0,120}?dissipateDispatcherFog\(\)/,
  "Stage IV Dispatcher death does not start fog dissipation");
expect(/dissipateDispatcherFog\(\)[\s\S]{0,1200}?BlockPos\.betweenClosed[\s\S]{0,500}?state\.is\(ModBlocks\.FOG\.get\(\)\)[\s\S]{0,300}?state\.setValue\(FogBlock\.AIR, 2\)/,
  "Stage IV Dispatcher fog is not switched to the original dissipation stage");
expect(/otherDispatchers[\s\S]{0,700}?entity != this && entity\.isAlive\(\)[\s\S]{0,200}?Family\.DISPATCHER[\s\S]{0,900}?noneMatch/,
  "Fog shared with another live Dispatcher is not protected during cleanup");
expect(/int radius = dispatcherFogRadius\(stage\)/,
  "Dispatcher fog placement and death cleanup no longer share the same radius rule");

if (failures.length) {
  console.error(`Stage IV Dispatcher fog cleanup verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Stage IV Dispatcher death fog cleanup is wired and verified.");
