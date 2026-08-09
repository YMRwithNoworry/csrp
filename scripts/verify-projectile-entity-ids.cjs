const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const entities = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/registry/ModEntities.java"), "utf8");
const projectile = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java"), "utf8");
const nade = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/entity/NadeEntity.java"), "utf8");
const client = fs.readFileSync(path.join(root,
  "src/main/java/alku/csrp/client/ClientModEvents.java"), "utf8");
const failures = [];

const registrations = [
  ["WEB_BALL", "webball", "WEB"],
  ["SPINE_BALL", "spineball", "SPINE"],
  ["NADE_BALL", "nadeball", "ELVIA_NADE"],
  ["BALL_TALL", "balltall", "ELVIA_BALL"],
  ["BALL_MALL", "ballmall", "LENCIA_BALL"],
  ["HEBLU_LIGHT", "heblu_light", "LIGHT"],
  ["METEOR", "meteor", "METEOR"]
];

for (const [constant, id, mode] of registrations) {
  if (!new RegExp(`${constant}\\s*=\\s*[\\s\\S]*?projectile\\("${id}"`).test(entities)) {
    failures.push(`${constant} is not registered with original id ${id}`);
  }
  if (!new RegExp(`${mode}[\\s\\S]{0,80}${constant}\\.get\\(\\)`).test(entities)) {
    failures.push(`mode ${mode} does not select ${constant}`);
  }
  if (!new RegExp(`ModEntities\\.${constant}\\.get\\(\\)`).test(client)) {
    failures.push(`${constant} has no client renderer`);
  }
}

if (!/createProjectile\(Level level, ParasiteProjectileEntity\.Mode mode\)/.test(entities)) {
  failures.push("No mode-aware projectile factory exists");
}
if (!/ParasiteProjectileEntity\(EntityType<\? extends ParasiteProjectileEntity> type, Level level, Mode defaultMode\)/.test(projectile)) {
  failures.push("Projectile entity types do not preserve their default mode");
}
if (!/spawnNade\(owner, NadeEntity\.Kind\.ELVIA\)/.test(projectile)
    || !/spawnNade\(owner, mode == Mode\.YELLOWEYE_NADE \? NadeEntity\.Kind\.YELLOWEYE : NadeEntity\.Kind\.ACID\)/.test(projectile)) {
  failures.push("Nadeball impact does not create the dedicated nade entity");
}
if (!nade.includes("ELVIA") || !nade.includes("ACID") || !nade.includes("YELLOWEYE")
    || !/startDelayTicks\s*=\s*3/.test(nade) || !/fuseTicks\s*=\s*4/.test(nade)
    || !/damageSources\(\)\.magic\(\)/.test(nade)) {
  failures.push("Dedicated nade entity does not preserve all three fuse variants");
}
if (!client.includes("ModEntities.NADE.get(), NadeRenderer::new")) {
  failures.push("Dedicated nade entity has no original-style renderer");
}

if (failures.length) {
  console.error("Projectile entity id verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Original projectile ids and mode-aware creation are registered.");
