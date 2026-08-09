const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const bomb = read("src/main/java/alku/csrp/entity/BombEntity.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/BombRenderer.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const config = read("src/main/java/alku/csrp/config/MobsConfig.java");
const hosts = read("src/main/java/alku/csrp/entity/AbstractHostEntity.java")
  + read("src/main/java/alku/csrp/entity/HostEntity.java")
  + read("src/main/java/alku/csrp/entity/HostIIEntity.java");
const omboo = read("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const jinjo = read("src/main/java/alku/csrp/entity/PreeminentParasiteEntity.java");
const iki = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const primitiveIki = read("src/main/java/alku/csrp/entity/VerminEntity.java");
const sheep = read("src/main/java/alku/csrp/entity/MarauderizedSheepEntity.java");
const legacy = read("src/main/java/alku/csrp/entity/LegacyAuxiliaryEntity.java");

expect(registry, /EntityType<BombEntity>> BOMB[\s\S]*?sized\(0\.68F, 0\.68F\)[\s\S]*?fireImmune\(\)/,
  "bomb is not registered as its original dedicated fire-immune entity type");
expect(client, /ModEntities\.BOMB\.get\(\), BombRenderer::new/,
  "bomb still uses a placeholder renderer");
expect(renderer, /createOmbooLayer\(\)[\s\S]*?createHostLayer\(\)[\s\S]*?createJinjoLayer\(\)/,
  "one or more original bomb models are missing");
expect(renderer, /case 1 -> host;[\s\S]*?case 2, 3 -> jinjo;/,
  "bomb skin-to-model mapping is not original");

for (const texture of ["bombo.png", "bombh.png", "bombj.png"]) {
  if (!fs.existsSync(path.join(root, "src/main/resources/assets/csrp/textures/entity/monster", texture))) {
    failures.push(`missing original bomb texture ${texture}`);
  }
}

expect(bomb, /movement\.add\(0\.0D, -0\.04D, 0\.0D\)/, "bomb gravity is not 0.04");
expect(bomb, /movement\.scale\(0\.98D\)/, "bomb movement damping is not 0.98");
expect(bomb, /movement\.x \* 0\.7D, movement\.y \* -0\.5D, movement\.z \* 0\.7D/,
  "bomb ground bounce is missing");
expect(bomb, /canGrief && level\(\)\.getGameRules\(\)\.getBoolean\(GameRules\.RULE_MOBGRIEFING\)/,
  "bomb block damage does not honor both griefing controls");
expect(bomb, /ModMobEffects\.VIRAL, 300, 0/, "bomb direct Viral duration is not original");
expect(bomb, /setWaitTime\(5\)/, "bomb toxic-cloud wait time is not 5 ticks");
expect(bomb, /setDuration\(60\)/, "bomb toxic-cloud duration is not 60 ticks");
expect(bomb, /MobEffects\.POISON, 300, 0/, "bomb toxic cloud lacks Poison");
expect(bomb, /ModMobEffects\.COTH, 3600, 0/, "bomb toxic cloud lacks original COTH duration");
expect(bomb, /ModMobEffects\.VIRAL, 3600, 0/, "bomb toxic cloud lacks original Viral duration");
expect(bomb, /getSkin\(\) == 2[\s\S]*?spawnJinjoPayload/,
  "Jinjo spawning bomb does not release its configured payload");
for (const tag of ["Fuse", "parasitetype", "stren", "cangrief"]) {
  expect(bomb, new RegExp(`put(?:Short|Int|Float|Boolean)\\(\"${tag}\"`),
    `bomb does not save original ${tag} data`);
}

expect(config, /HOST_BOMB_DAMAGE[\s\S]*?7\.0D/, "Host bomb damage config is missing");
expect(config, /HERD_BOMB_DAMAGE[\s\S]*?14\.0D/, "Hostii bomb damage config is missing");
expect(config, /OMBOO_BOMB_DAMAGE[\s\S]*?20\.0D/, "Omboo bomb damage config is missing");
expect(config, /JINJO_EXPLOSION_MULTIPLIER[\s\S]*?6\.0D/, "Jinjo explosion multiplier is missing");
expect(hosts, /spawnBomb\(target, 80, MobsConfig\.hostBombDamage\(\), 4\)/,
  "Host does not use the original bomb parameters");
expect(hosts, /spawnBomb\(target, 40, MobsConfig\.herdBombDamage\(\), 5\)/,
  "Hostii does not use the original bomb parameters");
expect(omboo, /configure\(this, 80, 1\.0F, MobsConfig\.ombooBombDamage\(\), 4, 0/,
  "Omboo does not drop its original bomb");
expect(jinjo, /configure\(this, 80, spawningBomb \? 4\.0F : 8\.0F,[\s\S]*?7, spawningBomb \? 2 : 3/,
  "Jinjo does not select the original bomb strength and skin");
expect(iki, /configure\(AdaptedVariantEntity\.this, 60, 0\.0F,[\s\S]*?2, 1, false\)/,
  "Adapted Iki does not drop its original harmless-explosion bomb");
expect(primitiveIki, /Config\.worldGnatCap\(\)[\s\S]*?configure\(this, 60, 0\.0F,[\s\S]*?2, 1, false\)/,
  "Primitive Iki does not switch from Gnats to its original bomb at the configured cap");
expect(iki, /Config\.worldGnatCap\(\)/,
  "Adapted Iki does not use the original configured global Gnat cap");
expect(sheep, /ModEntities\.NADE_BALL[\s\S]*?Mode\.ELVIA_NADE/,
  "Marauderized Sheep does not use the original nadeball projectile");

for (const [name, source] of [["Hosts", hosts], ["Omboo", omboo], ["Jinjo", jinjo],
  ["Primitive Iki", primitiveIki], ["Adapted Iki", iki], ["Marauderized Sheep", sheep]]) {
  if (/Mode\.BOMB/.test(source)) failures.push(`${name} still uses the generic BOMB projectile mode`);
}
if (/\bBOMB\b/.test(legacy)) failures.push("LegacyAuxiliaryEntity still carries the bomb id");

if (failures.length) {
  console.error("Original bomb behavior verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Original bomb physics, effects, models and callers are restored.");
