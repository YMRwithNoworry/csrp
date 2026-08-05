const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];

const read = (relative) => {
  const file = path.join(root, relative);
  if (!fs.existsSync(file)) {
    failures.push(`missing ${relative}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
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

const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const wand = read("src/main/java/alku/csrp/item/ParasiteEvolutionWandItem.java");
const transformation = read("src/main/java/alku/csrp/entity/ParasiteTransformation.java");

expect(items, /"itemevolve"[\s\S]*Mode\.EVOLUTION/, "Evolution Wand registration is missing");
expect(items, /"itemdevolve"[\s\S]*Mode\.DEVOLUTION/, "Devolution Wand registration is missing");
expect(items, /ITEM_EVOLVE[\s\S]*?stacksTo\(1\)/, "Evolution Wand must be unstackable");
expect(items, /ITEM_DEVOLVE[\s\S]*?stacksTo\(1\)/, "Devolution Wand must be unstackable");

expect(wand, /interactLivingEntity\(/, "entity interaction hook is missing");
expect(wand, /hand != InteractionHand\.MAIN_HAND/, "wands are not restricted to the main hand");
expect(wand, /!\(target instanceof Parasite\)/, "wands are not restricted to parasites");
expect(wand, /ParasiteTransformation\.evolve\(target\)/, "Evolution Wand does not trigger evolution");
expect(wand, /ParasiteTransformation\.devolve\(target\)/, "Devolution Wand does not trigger devolution");
expect(wand, /Component\.translatable\(tooltipKey,[\s\S]*tooltipKey \+ "\.action"/,
  "wand tooltip does not localize the highlighted action separately");
expect(wand, /withStyle\(ChatFormatting\.RED\)/, "wand tooltip is not red");

const mappings = [
  ["BUGLIN", "RUPTER"],
  ["RUPTER", "MANGLER"],
  ["SIM_ADVENTURER", "THRALL"],
  ["HOST", "HOSTII"],
  ["CRUX_INCOMPLETE", "CRUX"],
  ["RUPTER", "BUGLIN"],
  ["MANGLER", "RUPTER"],
  ["THRALL", "SIM_ADVENTURER"],
  ["HOSTII", "HOST"],
  ["CRUX", "CRUX_INCOMPLETE"],
  ["ADA_VERMIN", "MOVINGFLESH"],
];
for (const [source, target] of mappings) {
  expect(transformation, new RegExp(`ModEntities\\.${source}\\.get\\(\\).*ModEntities\\.${target}`),
    `${source} -> ${target} mapping is missing`);
}
expect(transformation, /ModEntities\.MOVINGFLESH\.get\(\).*randomPrimitive\(source\)/,
  "Moving Flesh -> random Primitive mapping is missing");

expect(transformation, /path\.startsWith\("pri_"\)[\s\S]*"ada_"/, "Primitive -> Adapted mapping is missing");
expect(transformation, /path\.startsWith\("ada_"\)[\s\S]*"pri_"/, "Adapted -> Primitive mapping is missing");
expect(transformation, /path\.startsWith\("sim_"\)[\s\S]*"fer_"/, "Assimilated -> Feral mapping is missing");
expect(transformation, /path\.startsWith\("fer_"\)[\s\S]*"sim_"/, "Feral -> Assimilated mapping is missing");
expect(transformation, /BECKON_SI[\s\S]*BECKON_SIV/, "Beckon stage mappings are incomplete");
expect(transformation, /DISPATCHER_SI[\s\S]*DISPATCHER_SIV/, "Dispatcher stage mappings are incomplete");
expect(transformation, /ROOTER_SI[\s\S]*ROOTER_SIV/, "Rooter stage mappings are incomplete");
expect(transformation, /targetType == null[\s\S]*source\.discard\(\)/,
  "Devolution does not remove parasites without a predecessor");
for (const id of ["PRI_BURROWER", "PRI_DEVOURER", "PRI_TOZOON", "ADA_BURROWER", "ADA_DEVOURER", "ADA_TOZOON"]) {
  expect(transformation, new RegExp(`ModEntities\\.${id}\\.get\\(\\)`), `${id} no-predecessor exception is missing`);
}

for (const [id, texture] of [["itemevolve", "iteme"], ["itemdevolve", "itemd"]]) {
  const model = parseJson(`src/main/resources/assets/csrp/models/item/${id}.json`);
  if (model?.parent !== "minecraft:item/handheld") failures.push(`${id}: wrong model parent`);
  if (model?.textures?.layer0 !== `csrp:item/${texture}`) failures.push(`${id}: wrong model texture`);

  const png = path.join(root, `src/main/resources/assets/csrp/textures/item/${texture}.png`);
  if (!fs.existsSync(png)) {
    failures.push(`${id}: texture is missing`);
  } else if (fs.readFileSync(png).subarray(0, 8).toString("hex") !== "89504e470d0a1a0a") {
    failures.push(`${id}: texture is not a valid PNG`);
  }
  const metadata = parseJson(`src/main/resources/assets/csrp/textures/item/${texture}.png.mcmeta`);
  if (metadata?.animation?.frametime !== 25 || metadata?.animation?.interpolate !== true) {
    failures.push(`${id}: animation metadata does not match the original asset`);
  }
}

const english = parseJson("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = parseJson("src/main/resources/assets/csrp/lang/zh_cn.json");
const translations = [
  [english, "item.csrp.itemevolve", "Evolution Wand"],
  [english, "item.csrp.itemdevolve", "Devolution Wand"],
  [english, "tooltip.csrp.itemevolve", "Target a Parasite for %s"],
  [english, "tooltip.csrp.itemevolve.action", "Evolution"],
  [english, "tooltip.csrp.itemdevolve", "Target a Parasite for %s"],
  [english, "tooltip.csrp.itemdevolve.action", "Devolution"],
  [chinese, "item.csrp.itemevolve", "进化之杖"],
  [chinese, "item.csrp.itemdevolve", "退化之杖"],
  [chinese, "tooltip.csrp.itemevolve", "令指向的寄生体%s"],
  [chinese, "tooltip.csrp.itemevolve.action", "进化"],
  [chinese, "tooltip.csrp.itemdevolve", "令指向的寄生体%s"],
  [chinese, "tooltip.csrp.itemdevolve.action", "退化"],
];
for (const [language, key, value] of translations) {
  if (language?.[key] !== value) failures.push(`${key}: expected ${value}`);
}

if (failures.length) {
  console.error("Evolution wand port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Evolution wand port verification passed (2 items, transformations, and original assets).\n");
