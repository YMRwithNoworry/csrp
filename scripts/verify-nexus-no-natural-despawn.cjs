const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (...segments) =>
  fs.readFileSync(path.join(root, ...segments), "utf8");

const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const handler = read("src/main/java/alku/csrp/entity/ParasiteDespawnHandler.java");
const mixin = read("src/main/java/alku/csrp/mixin/MobDespawnMixin.java");
const failures = [];
const check = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

// Nexus 家族（召唤柱/调度柱/支庇柱/支庇柱囊块）必须豁免原版自然消失。
check(nexus, /public boolean removeWhenFarAway\(double [A-Za-z]+\)\s*\{\s*return false;\s*\}/,
  "NexusParasiteEntity no longer refuses the far-away despawn (removeWhenFarAway -> false)");
check(nexus, /protected boolean shouldDespawnInPeaceful\(\)\s*\{\s*return false;\s*\}/,
  "NexusParasiteEntity despawns again on Peaceful difficulty");
check(nexus, /原版.*canDespawn|canDespawn|rsDespawn/,
  "The Nexus despawn exemption lost its reference to the original canDespawn/SRPConfig.rsDespawn rule");

// 所有柱子实体类型都必须走这个豁免（否则柱子换个阶段注册就会重新消失）。
for (const id of [
  "BECKON_SI", "BECKON_SII", "BECKON_SIII", "BECKON_SIV",
  "DISPATCHER_SI", "DISPATCHER_SII", "DISPATCHER_SIII", "DISPATCHER_SIV",
  "ROOTER_SI", "ROOTER_SII", "ROOTER_SIII", "ROOTER_SIV", "ROOTERBALL"
]) {
  const pattern = new RegExp(
    `EntityType<NexusParasiteEntity>>\\s+${id}\\s*=[\\s\\S]{0,200}?NexusParasiteEntity\\.Kind\\.${id}`);
  if (!pattern.test(entities)) {
    failures.push(`Registered pillar type ${id} is not a NexusParasiteEntity (exemption would not apply)`);
  }
}

// 豁免只针对 Nexus：普通寄生体的 cyst/recall 流程必须仍然接线。
check(handler, /class ParasiteDespawnHandler|ParasiteCanisterBlockEntity\.placeFromDespawn/,
  "ParasiteDespawnHandler lost the cyst fallback for ordinary parasites");
check(mixin, /@Inject\(method = "removeWhenFarAway", at = @At\("HEAD"\)\)/,
  "MobDespawnMixin no longer hooks Mob#removeWhenFarAway, so the exemption proves nothing");

if (failures.length) {
  console.error(`Nexus natural-despawn exemption verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Nexus pillars (Beckon/Dispatcher/Rooter) are exempt from natural despawn.");
