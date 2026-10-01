const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const read = (file) => fs.readFileSync(path.join(root, file), "utf8");
const failures = [];
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

const remains = read("src/main/java/alku/csrp/entity/RemainEntity.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/RemainRenderer.java");
const migration = read("src/main/java/alku/csrp/world/RemainsMigration.java");
const rules = read("src/main/java/alku/csrp/event/ParasiteCombatRules.java");
const gore = read("src/main/java/alku/csrp/entity/GoreEntity.java");
const goreRenderer = read("src/main/java/alku/csrp/client/renderer/GoreRenderer.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const compatibility = read("src/main/java/alku/csrp/block/GoreBlock.java");
const residue = read("src/main/java/alku/csrp/block/InfestedResidueBlock.java");

for (const [source, pattern, message] of [
  [remains, /builder\.define\(APPEARANCE,/, "remains appearance must be client-synchronized"],
  [remains, /move\(MoverType\.SELF, movement\)/, "remains must settle on terrain independently"],
  [remains, /isPickable\(\)\s*\{\s*return false;/, "remains must not intercept selection or projectiles"],
  [remains, /isPushable\(\)\s*\{\s*return false;/, "remains must not push players"],
  [remains, /LIFETIME_TICKS = 20 \* 60/, "inactive remains must have a bounded lifetime"],
  [remains, /\+\+age >= LIFETIME_TICKS && !active && !isInfestedResidue\(\)/, "infected residue and active rebuild counters must not expire"],
  [remains, /tag\.putInt\("appearance", getAppearance\(\)\)/, "remains appearance must survive saving"],
  [remains, /tag\.getInt\("appearance"\)/, "remains appearance must survive loading"],
  [remains, /tag\.putInt\("remainsAge", age\)/, "unloading must not reset remains lifetime"],
  [remains, /count \+= plus/, "existing resurrection progress must be retained"],
  [remains, /ReinforcementSystem\.tryFromResidue/, "infested residue reinforcement must be retained"],
  [client, /ModEntities\.REMAIN\.get\(\), RemainRenderer::new/, "remains must be visible entities"],
  [renderer, /RenderType\.entityCutoutNoCull\(getTextureLocation\(entity\)\)/, "transparent blood planes must use cutout"],
  [renderer, /"sim", "pri", "ada", "pure", "fer", "mar"/, "all six remains families must have textures"],
  [compatibility, /onPlace\([\s\S]*?RemainsMigration\.convert/, "legacy gore items must immediately become entities"],
  [residue, /onPlace\([\s\S]*?RemainsMigration\.convert/, "legacy infested residue items must immediately become entities"],
  [migration, /tickMigration\(LevelTickEvent\.Post event\)/, "migration must run after server chunk loading"],
  [migration, /getChunkNow\(pos\.x, pos\.z\)/, "migration must not load additional chunks"],
  [migration, /section\.maybeHas/, "migration must skip sections without legacy remains"],
  [migration, /getEntitiesOfClass\(RemainEntity\.class/, "migration must reuse existing rebuild counters"],
  [migration, /if \(remains != null\)[\s\S]*?Blocks\.AIR\.defaultBlockState\(\)/, "legacy blocks may only be removed after entity creation succeeds"],
  [gore, /groundTicks >= 1 && \(goreType == 10 \|\| goreType == 11\)/, "death fragments must remain entities after landing"],
  [goreRenderer, /case 5, 6, 8 -> assimilated/, "feral and assimara death fragments must be visible"]
]) expect(source, pattern, message);

for (const id of ["goresim", "gorepri", "goreada", "gorepur", "gorefer", "goremar", "infestremain", "infestedremain"]) {
  if (!migration.includes(`"${id}"`)) failures.push(`${id}: missing legacy block migration`);
}
for (const source of [remains, rules]) {
  if (/\.(?:setBlock|setBlockAndUpdate|removeBlock)\(/.test(source)) {
    failures.push("death/resurrection remains must not modify terrain");
  }
}
for (const file of ["AdaptedVariantEntity.java", "FeralEndermanEntity.java"]) {
  const source = read(`src/main/java/alku/csrp/entity/${file}`);
  expect(source, /RemainEntity\.spawn\(serverLevel,[\s\S]*?"infested", "flat"\)/,
    `${file}: residue must spawn as an entity`);
  if (/INFESTED_REMAINS\.get\(\)\.defaultBlockState\(\)/.test(source)) {
    failures.push(`${file}: still places a remains block`);
  }
}
expect(read("src/main/java/alku/csrp/item/GreekFireItem.java"), /burnRemainsCluster[\s\S]*?current\.discard\(\)/,
  "Greek Fire must clean up independent remains entities");
expect(read("src/main/java/alku/csrp/item/InfestedBonemealItem.java"), /getEntitiesOfClass\(RemainEntity\.class[\s\S]*?residue\.discard\(\)/,
  "infested bonemeal must work with independent infested residue");
for (const family of ["sim", "pri", "ada", "pure", "fer", "mar"]) {
  for (const variant of ["flat", "small", "big"]) {
    const texture = `src/main/resources/assets/csrp/textures/blocks/parasitegore_${family}_${variant}.png`;
    if (!fs.existsSync(path.join(root, texture))) failures.push(`${texture}: missing remains texture`);
  }
}
if (!fs.existsSync(path.join(root, "src/main/resources/assets/csrp/textures/blocks/infestremain.png"))) {
  failures.push("infested remains entity texture is missing");
}

if (failures.length) {
  console.error("Independent remains verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}
console.log("Independent remains verified: entity spawning, cutout textures, persistence and legacy migration.");
