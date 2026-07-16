const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const exists = (relative) => fs.existsSync(path.join(root, relative));
const failures = [];
const requireText = (source, needle, label) => {
    if (!source.includes(needle)) failures.push(`${label}: missing ${needle}`);
};

const modItems = read("src/main/java/alku/csrp/registry/ModItems.java");
const requiredIds = [
    "assimilated_flesh", "bone", "residue_plants", "beckon_drop", "dispatcher_drop",
    "ada_longarms_drop", "ada_summoner_drop", "ada_yelloweye_drop",
    "ada_reeker_drop", "ada_manducater_drop", "ada_bolster_drop",
    "ada_arachnida_drop", "ada_devourer_drop", "ada_vermin_drop",
    "ada_viscera_drop", "hijacked_drop", "hive_scrap", "bloody_iron_ingot",
    "bloody_rod", "bloody_bone", "lurecomponent1", "lurecomponent2",
    "lurecomponent3", "lurecomponent4", "lurecomponent5", "lurecomponent6",
    "dried_tendons", "hardened_bone_handle", "infectious_blade_fragment",
    "living_core", "vile_shell", "semiorganic_ingot", "false_apple",
    "fishlin", "shrimp", "alveoligrowth", "infested_bonemeal",
    "evclock", "levelclock", "nodecompass", "colonycompass", "origincompass",
    "srp_field_guide", "book_of_vengeance", "itemthrow", "bough",
    "thornshade_decanter", "venkrol_boots", "module_base", "tissue_spike",
    "organ_synth", "weapon_scythe", "weapon_scythe_sentient", "weapon_axe",
    "weapon_axe_sentient", "weapon_sword", "weapon_sword_sentient",
    "weapon_cleaver", "weapon_cleaver_sentient", "weapon_maul",
    "weapon_maul_sentient", "weapon_lance", "weapon_lance_sentient",
    "weapon_bow", "weapon_bow_sentient", "armor_helm", "armor_chest",
    "armor_pants", "armor_boots", "armor_helm_sentient",
    "armor_chest_sentient", "armor_pants_sentient", "armor_boots_sentient",
    "hijacked_iron_helmet", "hijacked_iron_chestpiece",
    "hijacked_iron_leggings", "hijacked_iron_boots", "hijacked_iron_sword",
    "hijacked_iron_axe", "hijacked_iron_pickaxe", "hijacked_iron_shovel",
    "hijacked_iron_hoe"
];

for (const id of requiredIds) requireText(modItems, `"${id}"`, "item registry");

for (const file of [
    "LivingWeaponItem.java", "LivingBowItem.java", "LivingArmorItem.java",
    "HijackedToolItem.java", "HijackedArmorItem.java", "FalseAppleItem.java",
    "FishlinItem.java", "QuenchItem.java", "BoughItem.java"
]) {
    if (!exists(`src/main/java/alku/csrp/item/${file}`)) failures.push(`missing item behavior: ${file}`);
}

const livingWeapon = read("src/main/java/alku/csrp/item/LivingWeaponItem.java");
for (const marker of ["50_000", "WeaponKind", "ModMobEffects.CORROSION", "ModMobEffects.BLEED",
    "ModMobEffects.VIRAL", "ModMobEffects.NEEDLER", "inflate(radius)"]) {
    requireText(livingWeapon, marker, "living weapon behavior");
}

const equipmentEvents = read("src/main/java/alku/csrp/event/EquipmentEvents.java");
for (const marker of ["0.0125F", "0.018F", "0.20F", "0.50F", "5.5F", "2400",
    "LivingArmorItem.EVOLUTION_DAMAGE", "wearsFullHijackedSet",
    "if (weapon == null) weapon = ItemStack.EMPTY;"]) {
    requireText(equipmentEvents, marker, "equipment event behavior");
}

const hijackedEffects = read("src/main/java/alku/csrp/item/HijackedHitEffects.java");
for (const marker of ["BLEED_TICKS = 100", "RAGE_TICKS = 60", "PARASITE_BONUS_DAMAGE = 3.0F",
    "target.getMaxHealth() * 0.1F"]) {
    requireText(hijackedEffects, marker, "hijacked hit behavior");
}

const livingArmor = read("src/main/java/alku/csrp/item/LivingArmorItem.java");
requireText(livingArmor, "90_000", "living armor evolution");
const falseApple = read("src/main/java/alku/csrp/item/FalseAppleItem.java");
for (const marker of ["nutrition(4)", "saturationModifier(0.3F)", "i < 5", "CONFUSION, 200",
    "BLINDNESS, 600"]) requireText(falseApple, marker, "false apple behavior");
const fishlin = read("src/main/java/alku/csrp/item/FishlinItem.java");
for (const marker of ["nutrition(3)", "saturationModifier(0.2F)", "4800, 1", "magic(), 8.0F"])
    requireText(fishlin, marker, "fishlin behavior");

const recipeIds = [
    "driedten", "hardbone", "infblade", "livingcore", "vileshell", "waxe",
    "wbow", "wcleaver", "wscythe", "wsword", "weapon_lance", "weapon_maul",
    "hijacked_iron_helmet", "hijacked_iron_chestpiece", "hijacked_iron_leggings",
    "hijacked_iron_boots", "hijacked_iron_sword", "hijacked_iron_axe",
    "hijacked_iron_pickaxe", "hijacked_iron_shovel", "hijacked_iron_hoe",
    "venkrol_boots", "evclock", "levelclock", "quench", "bough",
    "srp_field_guide"
];
for (const id of recipeIds) {
    if (!exists(`src/main/resources/data/csrp/recipe/${id}.json`)) failures.push(`missing recipe: ${id}`);
}

for (const id of requiredIds) {
    if (!exists(`src/main/resources/assets/csrp/models/item/${id}.json`)) failures.push(`missing item model: ${id}`);
}

for (const lang of ["en_us", "zh_cn"]) {
    const json = JSON.parse(read(`src/main/resources/assets/csrp/lang/${lang}.json`));
    for (const id of requiredIds) {
        if (!json[`item.csrp.${id}`]) failures.push(`${lang}: missing item.csrp.${id}`);
    }
}

const resourceText = [
    ...requiredIds.map((id) => `csrp:${id}`),
    ...requiredIds.map((id) => `item.csrp.${id}`)
];
for (const relative of [
    "src/main/resources/data/csrp/recipe",
    "src/main/resources/assets/csrp/models/item"
]) {
    if (!exists(relative)) continue;
    const files = fs.readdirSync(path.join(root, relative), { recursive: true })
        .filter((name) => name.endsWith(".json"));
    for (const name of files) {
        const body = read(path.join(relative, name));
        if (body.includes("srparasites:")) failures.push(`${relative}/${name}: legacy namespace`);
    }
}

if (failures.length) {
    console.error(failures.join("\n"));
    process.exit(1);
}
console.log(`Equipment/item parity verifier passed (${requiredIds.length} items, ${recipeIds.length} recipes).`);
