const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const tiers = ["one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"];
const cooldowns = [10, 20, 50, 250, 300, 600, 600, 1200, 1200, 1200];
const reductions = [10, 20, 50, 500, 4000, 80000, 350000, 6250000, 50000000, 72000000];
const scentLevels = [1, 1, 1, 2, 3, 4, 5, 6, 7, 8];
const failures = [];

const read = (relative) => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) {
    failures.push(`missing ${relative}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
};

const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const parseJson = (relative) => {
  const text = read(relative);
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch (error) {
    failures.push(`${relative}: invalid JSON (${error.message})`);
    return null;
  }
};

const block = read("src/main/java/alku/csrp/block/EvolutionLureBlock.java");
const item = read("src/main/java/alku/csrp/item/EvolutionLureItem.java");
const blocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const sounds = read("src/main/java/alku/csrp/registry/ModSounds.java");
const scent = read("src/main/java/alku/csrp/entity/ParasiticScentEntity.java");
const english = read("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = read("src/main/resources/assets/csrp/lang/zh_cn.json");

expect(blocks, /"evolutionlure"/, "evolution lure block registration is missing");
expect(block, /CARCASS_OFFSET\s*=\s*3/, "Carcass diagonal offset is not three blocks");
expect(block, /addCooldown\(level, tier\.cooldownSeconds\(\)\)/, "single lure does not add cooldown");
expect(block, /addEvolutionPoints\(level, -tier\.carcassReduction\(\), true\)/,
  "Carcass does not bypass cooldown while subtracting evolution points");
expect(block, /ModEntities\.SCENT\.get\(\)\.create\(level\)/, "Carcass does not create a Scent");
expect(block, /setDieAfterKilling\(true\)/, "Carcass Scent is not configured to die after killing");
expect(block, /setCanFollow\(true\)/, "Carcass Scent is not configured to follow its target");
expect(block, /setVisualOnly\(true\)/, "Carcass lightning is not visual-only");
expect(block, /getCloneItemStack\(LevelReader level, BlockPos pos, BlockState state\)/,
  "pick-block does not preserve the lure tier");
expect(item, /setValue\(EvolutionLureBlock\.TIER, tier\)/, "lure items do not retain their tier on placement");
expect(scent, /void setScentLevel\(int level\)/, "Scent level setter is missing");
expect(sounds, /LURE_USE = register\("lure\.use"\)/, "lure use sound registration is missing");
expect(sounds, /CARCASS_USE = register\("lure\.carcass"\)/, "Carcass sound registration is missing");

for (let index = 0; index < tiers.length; index++) {
  const tier = tiers[index];
  const constant = tier.toUpperCase();
  const number = String(index + 1);
  const numeric = (value) => value.toLocaleString("en-US").replaceAll(",", "_");
  expect(block, new RegExp(`${constant}\\(\\"${tier}\\", ${numeric(cooldowns[index])}, ${numeric(reductions[index])}, ${scentLevels[index]}\\)`),
    `${tier}: tier behavior values are incorrect`);
  expect(items, new RegExp(`EVOLUTION_LURE_${constant} = evolutionLure\\(\\s*\\"evolutionlure_${tier}\\"`),
    `${tier}: item registration is missing`);

  const model = parseJson(`src/main/resources/assets/csrp/models/block/evolutionlure_${tier}.json`);
  if (model && model.textures?.all !== `csrp:block/lure${number}`) {
    failures.push(`${tier}: block model uses the wrong texture`);
  }
  parseJson(`src/main/resources/assets/csrp/models/item/evolutionlure_${tier}.json`);

  const texture = path.join(root, `src/main/resources/assets/csrp/textures/block/lure${number}.png`);
  if (!fs.existsSync(texture)) {
    failures.push(`${tier}: texture is missing`);
  } else {
    const signature = fs.readFileSync(texture).subarray(0, 8).toString("hex");
    if (signature !== "89504e470d0a1a0a") failures.push(`${tier}: texture is not a valid PNG`);
  }

  for (const language of [english, chinese]) {
    expect(language, new RegExp(`"item\\.csrp\\.evolutionlure_${tier}"`),
      `${tier}: localized item name is missing`);
  }
  if (index < 6) parseJson(`src/main/resources/data/csrp/recipes/evolutionlure_${tier}.json`);
}

const blockstate = parseJson("src/main/resources/assets/csrp/blockstates/evolutionlure.json");
for (const tier of tiers) {
  if (blockstate && !blockstate.variants?.[`tier=${tier}`]) failures.push(`${tier}: blockstate variant is missing`);
}

const loot = read("src/main/resources/data/csrp/loot_table/blocks/evolutionlure.json");
parseJson("src/main/resources/data/csrp/loot_table/blocks/evolutionlure.json");
for (const tier of tiers) {
  expect(loot, new RegExp(`"tier": "${tier}"`), `${tier}: loot condition is missing`);
  expect(loot, new RegExp(`"csrp:evolutionlure_${tier}"`), `${tier}: loot item is missing`);
}

parseJson("src/main/resources/assets/csrp/sounds.json");
for (const relative of ["use1.ogg", "use2.ogg", "carcass.ogg"]) {
  const file = path.join(root, "src/main/resources/assets/csrp/sounds/block/lure", relative);
  if (!fs.existsSync(file) || fs.statSync(file).size === 0) failures.push(`sound ${relative} is missing or empty`);
}

if (failures.length) {
  console.error("Evolution lure port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Evolution lure port verification passed (10 tiers, 6 recipes).\n");
