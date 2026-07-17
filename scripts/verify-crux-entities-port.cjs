const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (file) => {
  const full = path.join(root, file);
  if (!fs.existsSync(full)) {
    failures.push(`missing ${file}`);
    return "";
  }
  return fs.readFileSync(full, "utf8");
};
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const attributes = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const crudeBase = read("src/main/java/alku/csrp/entity/CrudeParasiteEntity.java");
const crux = read("src/main/java/alku/csrp/entity/CruxEntity.java");
const incomplete = read("src/main/java/alku/csrp/entity/IncompleteCruxEntity.java");

expect(entities, /monster\("crux", CruxEntity::new, 1\.13333F, 3\.3F\)/,
  "Crux legacy entity registration is missing");
expect(entities, /monster\("crux_incomplete", IncompleteCruxEntity::new, 1\.31F, 1\.1F\)/,
  "Incomplete Crux legacy entity registration is missing");
expect(items, /CRUX_SPAWN_EGG/, "Crux spawn egg is missing");
expect(items, /CRUX_INCOMPLETE_SPAWN_EGG/, "Incomplete Crux spawn egg is missing");
expect(attributes, /ModEntities\.CRUX\.get\(\), CruxEntity\.createAttributes\(\)/,
  "Crux attributes are not registered");
expect(attributes, /ModEntities\.CRUX_INCOMPLETE\.get\(\), IncompleteCruxEntity\.createAttributes\(\)/,
  "Incomplete Crux attributes are not registered");
expect(client, /"crux", 0\.7F/, "Crux renderer is missing");
expect(client, /"crux_incomplete", 0\.45F/, "Incomplete Crux renderer is missing");
expect(crudeBase, /usesDamageAdaptation\(\)[\s\S]*return false/, "Crude parasites still use primitive adaptation");
expect(crux, /Attributes\.MAX_HEALTH, 70\.0/, "Crux legacy health is missing");
expect(crux, /Attributes\.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE/, "Crux legacy damage is missing");
expect(crux, /performAoeAttack/, "Crux area melee behavior is missing");
expect(crux, /FallingBlockEntity\.fall/, "Crux block throw behavior is missing");
expect(crux, /DAMAGE_STACK_CAP = 10/, "Crux kill damage cap is missing");
expect(crux, /DAMAGE_GAIN_PER_KILL = 0\.12/, "Crux kill damage gain is missing");
expect(incomplete, /MIN_GROW_TICKS = 20 \* 20/, "Incomplete Crux minimum growth time is missing");
expect(incomplete, /MAX_GROW_TICKS = 60 \* 20/, "Incomplete Crux maximum growth time is missing");
expect(incomplete, /BURST_TICKS = 70/, "Incomplete Crux burst fuse is missing");
expect(incomplete, /ModEntities\.CRUX\.get\(\)\.create/, "Incomplete Crux does not transform into Crux");

for (const id of ["crux", "crux_incomplete"]) {
  for (const resource of [
    `geo/${id}.geo.json`,
    `animations/${id}.animation.json`,
    `textures/entity/${id}.png`,
    `textures/item/${id}_spawn_egg.png`,
    `models/item/${id}_spawn_egg.json`
  ]) {
    read(`src/main/resources/assets/csrp/${resource}`);
  }
  const geometry = JSON.parse(read(`src/main/resources/assets/csrp/geo/${id}.geo.json`));
  const animations = JSON.parse(read(`src/main/resources/assets/csrp/animations/${id}.animation.json`));
  const bones = new Set(geometry["minecraft:geometry"][0].bones.map((bone) => bone.name));
  for (const [name, animation] of Object.entries(animations.animations)) {
    for (const bone of Object.keys(animation.bones ?? {})) {
      if (!bones.has(bone)) failures.push(`${id}/${name} references missing bone ${bone}`);
    }
  }
  JSON.parse(read(`src/main/resources/data/csrp/loot_table/entities/${id}.json`));
}

const cruxLoot = read("src/main/resources/data/csrp/loot_table/entities/crux.json");
const incompleteLoot = read("src/main/resources/data/csrp/loot_table/entities/crux_incomplete.json");
expect(cruxLoot, /csrp:lurecomponent2/, "Crux corresponding drop is missing");
expect(incompleteLoot, /csrp:assimilated_flesh/, "Incomplete Crux corresponding drop is missing");

if (failures.length) {
  console.error("Crux entity port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Crux entity port verification passed.");
