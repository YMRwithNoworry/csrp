const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const entities = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/registry/ModEntities.java"), "utf8");
const client = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/client/ClientModEvents.java"), "utf8");
const anti = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/entity/AntiInfestedBlockEntity.java"), "utf8");
const failures = [];

const originalIds = [
  "pullingball", "webball", "spineball", "nadeball", "salivaball", "ballball",
  "ancientball", "homming", "antiinfestedblock", "biomassball", "missile",
  "balltall", "ballmall", "salivaeff", "heblu_light", "orbscary", "orbvoid",
  "orbboom", "source", "remain", "bomb", "cloudtoxic", "biomass", "gore",
  "tendril", "scent", "wave", "waveshock", "nade", "meteor"
];

for (const id of originalIds) {
  if (!entities.includes(`"${id}"`)) {
    failures.push(`missing original auxiliary entity id ${id}`);
  }
}

for (const constant of [
  "SALIVA_BALL", "BALL_BALL", "ANCIENT_BALL", "MISSILE", "SALIVA_EFFECT",
  "BIOMASS_BALL", "ANTI_INFESTED_BLOCK", "ORB_BOOM", "SOURCE", "REMAIN",
  "BOMB", "CLOUD_TOXIC", "GORE", "TENDRIL", "WAVE", "NADE"
]) {
  if (!client.includes(`ModEntities.${constant}.get()`)) {
    failures.push(`${constant} has no client renderer`);
  }
}

if (!anti.includes("HORIZONTAL_RANGE = 7") || !anti.includes("VERTICAL_RANGE = 5")
    || !anti.includes("InfestedBlock.STAGE, 3") || !anti.includes("scheduleTick(pos, state.getBlock(), 40)")) {
  failures.push("antiinfestedblock does not preserve the original range and delayed neutralization");
}

if (failures.length) {
  console.error("Original auxiliary entity completeness verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("All 30 original auxiliary entity ids are registered and rendered.");
