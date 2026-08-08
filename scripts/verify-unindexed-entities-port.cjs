const fs = require("fs");
const path = require("path");
const { all } = require("./entity-port-manifest.cjs");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const expect = (source, token, description) => {
  if (!source.includes(token)) failures.push(description);
};

const unindexed = [
  "carrier_worm", "seeker", "worker", "architect", "anc_pod",
  "anc_dreadnaut_ten", "movingflesh", "sim_adventurerhead"
];
for (const id of unindexed) {
  if (!all.includes(id)) failures.push(`${id}: absent from 127-ID registration manifest`);
}

const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const common = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const creative = read("src/main/java/alku/csrp/Csrp.java");
const carrier = read("src/main/java/alku/csrp/entity/CarrierWormEntity.java");
const burrowing = read("src/main/java/alku/csrp/entity/BurrowingVariantEntity.java");
const pure = read("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const config = read("src/main/java/alku/csrp/Config.java");

for (const [source, token, description] of [
  [entities, 'monster("carrier_worm"', "Worm Carrier registration missing"],
  [entities, 'monster("seeker"', "Seeker registration missing"],
  [entities, 'monster("sim_dragonehead"', "original dragon-head ID missing"],
  [entities, 'monster("sim_dragonhead"', "dragon-head compatibility ID missing"],
  [common, "CarrierWormEntity.createAttributes()", "Worm Carrier attributes missing"],
  [common, "PureParasiteEntity.Kind.SEEKER", "Seeker attributes missing"],
  [client, "ModEntities.CARRIER_WORM.get(), NoopRenderer::new", "Worm Carrier legacy no-op renderer missing"],
  [client, "ModEntities.SEEKER.get(), NoopRenderer::new", "Seeker legacy no-op renderer missing"],
  [creative, "ModItems.CARRIER_WORM_SPAWN_EGG", "Worm Carrier creative entry missing"],
  [creative, "ModItems.SEEKER_SPAWN_EGG", "Seeker creative entry missing"],
  [creative, "ModItems.SIM_DRAGONEHEAD_SPAWN_EGG", "original dragon-head creative entry missing"],
  [carrier, ".add(Attributes.MAX_HEALTH, 77.0D)", "Worm Carrier reset health mismatch"],
  [carrier, ".add(Attributes.ARMOR, 20.0D)", "Worm Carrier reset armor mismatch"],
  [carrier, ".add(Attributes.ATTACK_DAMAGE, 22.0D)", "Worm Carrier reset damage mismatch"],
  [carrier, "BODY_SEGMENTS = 4", "Worm Carrier body count mismatch"],
  [carrier, "return 1.9D;", "Worm Carrier follow spacing mismatch"],
  [burrowing, "source.is(DamageTypes.DROWN)", "burrowing creature drowning immunity missing"],
  [burrowing, "return super.causeFallDamage(distance, damageMultiplier, source);", "normal fall damage not restored"],
  [pure, "SeekerRandomFlightGoal", "Seeker random-flight behavior missing"],
  [pure, "seekerCreationPhase", "Seeker creation-phase gate missing"],
  [pure, "scentCooldown = 800", "Seeker Scent cooldown mismatch"],
  [pure, "tickCount % 21 != 10", "Seeker Scent cycle gate mismatch"],
  [pure, "setDieAfterKilling(true)", "Seeker Scent lifetime flag missing"],
  [pure, "setCanFollow(true)", "Seeker following Scent flag missing"],
  [config, "scentEnabled", "Scent enabled config missing"],
  [config, "scentCap", "Scent cap config missing"],
  [config, "scentDevelopmentLevel", "Scent development config missing"]
]) expect(source, token, description);

for (const id of ["carrier_worm", "worker", "architect", "anc_pod", "anc_dreadnaut_ten"]) {
  const loot = JSON.parse(read(`src/main/resources/data/csrp/loot_table/entities/${id}.json`));
  if (!Array.isArray(loot.pools) || loot.pools.length !== 0) {
    failures.push(`${id}: original empty loot behavior is not preserved`);
  }
}

if (failures.length) {
  console.error("Unindexed registered-creature verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Verified all eight creatures omitted from the legacy bestiary index.");
