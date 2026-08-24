const fs = require("fs");

const read = (relativePath) => fs.readFileSync(relativePath, "utf8");
const failures = [];

const camouflage = fs.readFileSync("src/main/resources/assets/csrp/textures/mob_effect/camouflage.png");
if (camouflage.subarray(1, 4).toString("ascii") !== "PNG") {
  failures.push("camouflage effect texture is not a real PNG image");
}

const purifierModel = JSON.parse(read("src/main/resources/assets/csrp/models/item/injected_purifier.json"));
if (purifierModel.textures?.layer0 !== "minecraft:item/arrow") {
  failures.push("injected purifier still references the removed tipped-arrow texture");
}

const enchantments = read("src/main/java/alku/csrp/registry/ModEnchantments.java");
const mod = read("src/main/java/alku/csrp/Csrp.java");
if (!/ENCHANTMENTS\.register\(\s*"parasite_killer"/u.test(enchantments)) {
  failures.push("parasite killer is not registered as a Forge 1.20.1 enchantment");
}
if (!mod.includes("ModEnchantments.ENCHANTMENTS.register(modEventBus)")) {
  failures.push("enchantment deferred register is not attached to the mod event bus");
}

const armorMaterials = read("src/main/java/alku/csrp/registry/ModArmorMaterials.java");
const expectedArmorNames = ["livings", "sentients", "hijacked_iron", "venkrol_boot", "mobility"];
for (const name of expectedArmorNames) {
  if (!armorMaterials.includes(`material("${name}"`)) {
    failures.push(`armor material ${name} does not match its texture prefix`);
  }
}
if (!armorMaterials.includes('Csrp.MODID + ":" + name')) {
  failures.push("armor material names do not include the csrp namespace");
}
for (const name of expectedArmorNames) {
  const layer1 = `src/main/resources/assets/csrp/textures/models/armor/${name}_layer_1.png`;
  if (!fs.existsSync(layer1)) failures.push(`missing armor layer texture: ${layer1}`);
}
for (const name of expectedArmorNames.filter((name) => name !== "venkrol_boot")) {
  const layer2 = `src/main/resources/assets/csrp/textures/models/armor/${name}_layer_2.png`;
  if (!fs.existsSync(layer2)) failures.push(`missing armor layer texture: ${layer2}`);
}

if (failures.length > 0) {
  failures.forEach((failure) => console.error(failure));
  process.exit(1);
}

console.log("Runtime texture, model, and enchantment registry fixes verified.");
