const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const entities = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/registry/ModEntities.java"), "utf8");
const client = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/client/ClientModEvents.java"), "utf8");
const failures = [];

const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

for (const [constant, originalId, legacyId] of [
  ["SHOCKWAVE", "waveshock", "shockwave"],
  ["PULLING_BALL", "pullingball", "pulling_ball"],
  ["SCARY_ORB", "orbscary", "scary_orb"],
  ["HAUNTER_HOMING", "homming", "haunter_homing"]
]) {
  expect(entities, new RegExp(`${constant}\\s*=\\s*[\\s\\S]*?ENTITIES\\.register\\("${originalId}"`),
    `${constant} does not use the original 1.10.7 id ${originalId}`);
  expect(entities, new RegExp(`${constant}_LEGACY\\s*=\\s*[\\s\\S]*?ENTITIES\\.register\\("${legacyId}"`),
    `${constant} does not retain the development id ${legacyId}`);
  expect(client, new RegExp(`ModEntities\\.${constant}_LEGACY\\.get\\(\\)`),
    `${constant} legacy alias has no renderer`);
}

expect(entities, /BIOMASS\s*=\s*[\s\S]*?MobCategory\.MISC[\s\S]*?clientTrackingRange\(4\)[\s\S]*?updateInterval\(3\)/,
  "Biomass does not use the original projectile category or 64-block/3-tick tracking");
expect(entities, /VOID_ORB\s*=\s*[\s\S]*?clientTrackingRange\(16\)[\s\S]*?updateInterval\(1\)/,
  "Void Orb does not use the original 256-block/1-tick tracking");
for (const constant of ["SHOCKWAVE", "PULLING_BALL", "SCARY_ORB", "HAUNTER_HOMING"]) {
  expect(entities, new RegExp(`${constant}\\s*=\\s*[\\s\\S]*?clientTrackingRange\\(4\\)[\\s\\S]*?updateInterval\\(3\\)`),
    `${constant} does not use the original 64-block/3-tick tracking`);
}

if (failures.length) {
  console.error("Auxiliary entity id verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Original ids and tracking restored for 7 dedicated auxiliary entities.");
