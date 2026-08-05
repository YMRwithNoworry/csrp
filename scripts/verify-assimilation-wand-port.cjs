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
const wand = read("src/main/java/alku/csrp/item/AssimilationWandItem.java");
const infection = read("src/main/java/alku/csrp/infection/InfectionMechanics.java");
const config = read("src/main/java/alku/csrp/Config.java");
const particles = read("src/main/java/alku/csrp/registry/ModParticles.java");
const particleClient = read("src/main/java/alku/csrp/client/particle/AssimilationSplashParticle.java");
const clientEvents = read("src/main/java/alku/csrp/client/ClientModEvents.java");

expect(items, /ITEM_ASSIMILATE[\s\S]*"itemassimilate"[\s\S]*stacksTo\(1\)/,
  "Assimilation Wand registration is missing or stackable");
expect(wand, /boolean hurtEnemy\(/, "left-click assimilation hook is missing");
expect(wand, /InfectionMechanics\.forceAssimilate\(target\)/,
  "left-click does not force host assimilation");
if (/attacker instanceof Player/.test(wand)) {
  failures.push("left-click assimilation incorrectly excludes non-player wielders");
}
expect(wand, /interactLivingEntity\(/, "right-click disguise hook is missing");
expect(wand, /hand != InteractionHand\.MAIN_HAND/, "right-click is not restricted to the main hand");
expect(wand, /InfectionMechanics\.disguiseAssimilated\(target\)/,
  "right-click does not restore the host disguise");
expect(wand, /itemassimilate\.action[\s\S]*ChatFormatting\.RED/,
  "Assimilation action tooltip is not highlighted red");

expect(infection, /canForceAssimilate\(LivingEntity host\)/, "force-assimilation eligibility is missing");
expect(infection, /hasMappedHost\(host\)/, "force assimilation does not honor configured mappings");
expect(infection, /createAssimilatedHost\(host, serverLevel\)/,
  "force assimilation bypasses existing evolution and dislodgment behavior");
expect(infection, /replaceForcedHost\(host, converted, serverLevel\)/,
  "forced assimilation still uses normal COTH replacement side effects");
expect(infection, /evolutionPhase\(\) >= ASSIMILATION_FERAL_PHASE[\s\S]*createMappedHost\(host, level, true\)[\s\S]*activeCodeValue\(level, 1\)/,
  "late-phase feral conversion does not take precedence over dislodgment replacement");
expect(infection, /finalizeSpawn\([\s\S]*MobSpawnType\.CONVERSION[\s\S]*setHealth\(converted\.getMaxHealth\(\)\)/,
  "forced conversion does not fully initialize a full-health replacement");
expect(infection, /csrp_assimilation_host/, "original host id tag is missing");
expect(infection, /getPersistentData\(\)\.putString[\s\S]*ENTITY_TYPE\.getKey\(host\.getType\(\)\)/,
  "converted assimilated mobs do not remember their original host id");
expect(infection, /ResourceLocation\.tryParse[\s\S]*ASSIMILATION_HOST_TAG/,
  "disguise does not resolve the saved host id");
expect(infection, /new MobEffectInstance\(ModMobEffects\.COTH, COTH_BASE_DURATION_TICKS,[\s\S]*COTH_MAX_AMPLIFIER/,
  "restored disguise does not receive the original COTH strength and duration");
expect(infection, /addFreshEntity\(disguise\)[\s\S]*assimilated\.discard\(\)/,
  "disguise replacement does not safely replace the assimilated body");
expect(infection, /MobEffects\.CONFUSION[\s\S]*ASSIMILATION_NAUSEA_AMPLIFIER/,
  "original conversion nausea is missing");
expect(infection, /ModParticles\.ASSIMILATION_SPLASH/,
  "original assimilation splash feedback is missing");
expect(infection, /ParticleTypes\.EXPLOSION[\s\S]*levelEvent\(null, 1026/,
  "original completion particles or conversion event are missing");
expect(particles, /ASSIMILATION_SPLASH[\s\S]*"assimilation_splash"/,
  "assimilation splash particle is not registered");
expect(particleClient, /lifetime = 20 \* \(random\.nextInt\(3\) \+ 1\)/,
  "assimilation splash does not preserve the original random lifetime");
expect(particleClient, /gravity = 1\.0F[\s\S]*friction = 0\.98F/,
  "assimilation splash does not preserve the original physics");
expect(particleClient, /nextFloat\(\) <= 0\.5F[\s\S]*nextFloat\(\) <= 0\.25F[\s\S]*sprites\.get\(textureIndex, 2\)/,
  "assimilation splash does not preserve the original texture weighting");
expect(clientEvents, /ASSIMILATION_SPLASH[\s\S]*AssimilationSplashParticle\.Provider/,
  "assimilation splash particle provider is not registered");

for (const host of ["pig", "sheep", "cow", "wolf", "horse", "zombie", "villager", "polar_bear", "enderman", "squid"]) {
  expect(config, new RegExp(`minecraft:${host};csrp:sim_`), `${host}: default assimilation mapping is missing`);
}

const model = parseJson("src/main/resources/assets/csrp/models/item/itemassimilate.json");
if (model?.parent !== "minecraft:item/handheld") failures.push("itemassimilate: wrong model parent");
if (model?.textures?.layer0 !== "csrp:item/itema") failures.push("itemassimilate: wrong model texture");

const png = path.join(root, "src/main/resources/assets/csrp/textures/item/itema.png");
if (!fs.existsSync(png)) {
  failures.push("itemassimilate: texture is missing");
} else if (fs.readFileSync(png).subarray(0, 8).toString("hex") !== "89504e470d0a1a0a") {
  failures.push("itemassimilate: texture is not a valid PNG");
}
const metadata = parseJson("src/main/resources/assets/csrp/textures/item/itema.png.mcmeta");
if (metadata?.animation?.frametime !== 25 || metadata?.animation?.interpolate !== true) {
  failures.push("itemassimilate: animation metadata does not match the original asset");
}

const splashMetadata = parseJson("src/main/resources/assets/csrp/particles/assimilation_splash.json");
const expectedSplashTextures = [1, 2, 3].map(index => `csrp:assimilation_splash_${index}`);
if (JSON.stringify(splashMetadata?.textures) !== JSON.stringify(expectedSplashTextures)) {
  failures.push("assimilation_splash: original texture set is incomplete");
}
for (const index of [1, 2, 3]) {
  const texture = path.join(root,
    `src/main/resources/assets/csrp/textures/particle/assimilation_splash_${index}.png`);
  if (!fs.existsSync(texture)) failures.push(`assimilation_splash_${index}: texture is missing`);
}

const english = parseJson("src/main/resources/assets/csrp/lang/en_us.json");
const chinese = parseJson("src/main/resources/assets/csrp/lang/zh_cn.json");
const translations = [
  [english, "item.csrp.itemassimilate", "Assimilation Wand"],
  [english, "tooltip.csrp.itemassimilate", "Target a Creature for %s"],
  [english, "tooltip.csrp.itemassimilate.action", "Assimilation"],
  [chinese, "item.csrp.itemassimilate", "同化之杖"],
  [chinese, "tooltip.csrp.itemassimilate", "令指向的生物%s"],
  [chinese, "tooltip.csrp.itemassimilate.action", "被同化"],
];
for (const [language, key, value] of translations) {
  if (language?.[key] !== value) failures.push(`${key}: expected ${value}`);
}

if (failures.length) {
  console.error("Assimilation Wand port verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Assimilation Wand port verification passed (forced conversion, disguise, and original asset).\n");
