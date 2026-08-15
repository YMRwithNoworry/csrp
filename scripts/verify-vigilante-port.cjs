const fs = require("node:fs");
const path = require("node:path");
const { behaviorPorts } = require("./entity-port-manifest.cjs");

const root = path.resolve(__dirname, "..");
const defaultOriginalRoot = "D:\\code\\mod-decompiler-placeholder".replace(
  "mod-decompiler-placeholder", "\u6a21\u7ec4\u53cd\u7f16\u8bd1\u5668\\decompiled\\[\u9003\u9038\uff1a\u5bc4\u751f\u4f53] SRParasites-1.10.8"
);
const originalRoot = path.resolve(process.env.SRP_DECOMPILED_ROOT || defaultOriginalRoot);
const failures = [];

const readRequired = (file, label = file) => {
  if (!fs.existsSync(file)) {
    failures.push(`missing ${label}: ${file}`);
    return "";
  }
  return fs.readFileSync(file, "utf8").replace(/\r\n/g, "\n");
};
const current = (relative) => readRequired(path.join(root, relative), relative);
const original = (relative) => readRequired(path.join(originalRoot, relative), `original ${relative}`);
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};
const expectFile = (relative) => {
  if (!fs.existsSync(path.join(root, relative))) failures.push(`missing ${relative}`);
};
const isolate = (text, start, end, label) => {
  const startIndex = text.indexOf(start);
  const endIndex = startIndex < 0 ? -1 : text.indexOf(end, startIndex + start.length);
  if (startIndex < 0 || endIndex < 0) {
    failures.push(`could not isolate ${label}`);
    return "";
  }
  return text.slice(startIndex, endIndex);
};

const manifest = behaviorPorts.vigilante;
if (!manifest || manifest.originalClass !== "EntityAnged" || manifest.status !== "audited"
    || manifest.auditScope !== "entity-specific") {
  failures.push("vigilante entity-specific audit manifest is invalid");
}

const pure = current("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const projectile = current("src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java");
const config = current("src/main/java/alku/csrp/config/MobsConfig.java");
const configEvents = current("src/main/java/alku/csrp/config/OriginalConfigEvents.java");
const legacyConfig = current("src/main/java/alku/csrp/Config.java");
const model = current("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const renderer = current("src/main/java/alku/csrp/client/renderer/ParasiteProjectileRenderer.java");
const client = current("src/main/java/alku/csrp/client/ClientModEvents.java");
const entities = current("src/main/java/alku/csrp/registry/ModEntities.java");
const sounds = current("src/main/java/alku/csrp/registry/ModSounds.java");

const anged = original("com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityAnged.java");
const attributes = original("com/dhanantry/scapeandrunparasites/util/SRPAttributes.java");
const mobConfig = original("com/dhanantry/scapeandrunparasites/util/config/SRPConfigMobs.java");
const ranged = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIAttackRangedStatus.java");
const melee = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIAttackMeleeStatus.java");
const switchGoal = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIAttackMeleeRangeSwitch.java");
const ball = original("com/dhanantry/scapeandrunparasites/entity/projectile/EntityProjectileAngedball.java");
const originalRenderer = original("com/dhanantry/scapeandrunparasites/client/renderer/entity/pure/RenderAnged.java");

expect(anged, /func_70105_a\(1\.6F, 3\.1F\)/, "original Vigilante dimensions changed");
expect(anged, /new EntityBody\(this, 0\.7F, 0\.9F, 1\.0F, 1\.1F, 2\.3F, 1, 1, true\)[\s\S]*new EntityBody\(this, 0\.7F, 0\.9F, 1\.0F, 1\.1F, 2\.3F, -1, 2, true\)/,
  "original Vigilante tendril parts changed");
expect(anged, /func_70047_e\(\)[\s\S]{0,60}?return 3\.0F/, "original Vigilante eye height changed");
expect(attributes, /ANGED_HEALTH\s*=\s*70\.0[\s\S]*ANGED_ARMOR\s*=\s*25\.0[\s\S]*ANGED_ATTACK_DAMAGE\s*=\s*23\.0[\s\S]*ANGED_KD_RESISTANCE\s*=\s*1\.0[\s\S]*ANGED_RANGED_ATTACK_DAMAGE\s*=\s*27\.0/,
  "original Vigilante attributes changed");
expect(anged, /EntityAIAttackMeleeRangeSwitch\(this, 5\.0F\)[\s\S]*EntityAIAttackMeleeStatus\(this, 1\.5, false, 0\.0\)[\s\S]*EntityAIAttackRangedStatus\(this, 1\.5, 20, \(float\)SRPConfig\.pureFollow \/ 2\.0F, false\)/,
  "original Vigilante goal parameters changed");
expect(ranged, /maxRangedAttackTime[\s\S]*attackRadius[\s\S]*if \(!this\.entityHost\.field_70122_E\)/,
  "original Vigilante ranged ground gate changed");
expect(melee, /this\.attacker\.getAttackSpeed\(\)[\s\S]*this\.attacker\.func_70652_k\(target\)/,
  "original Vigilante melee cadence changed");
expect(switchGoal, /this\.distance = meleeDistance \* meleeDistance[\s\S]*func_70068_e\(this\.parent\) < this\.distance/,
  "original Vigilante melee/ranged switch distance changed");
expect(mobConfig, /angedOrbEffects\s*=\s*new String\[\][\s\S]*minecraft:hunger[\s\S]*srparasites:needler[\s\S]*minecraft:mining_fatigue[\s\S]*minecraft:speed/,
  "original Vigilante orb effects changed");
expect(ball, /EnumParticleTypes\.SLIME[\s\S]*SRPAttributes\.ANGED_RANGED_ATTACK_DAMAGE[\s\S]*setRadius\(2\.5F, 0\.5F\)[\s\S]*setWaitTime\(10\)[\s\S]*MobEffects\.field_76436_u, 300, 0[\s\S]*SRPPotions\.CORRO_E, 100, 0/,
  "original Vigilante projectile impact changed");
expect(anged, /setSkin\(7\)/, "original Vigilante heavy variant selection changed");
expect(originalRenderer, /new ModelAnged\(\), 1\.2F[\s\S]*case 7:[\s\S]*TEXTUREH/, "original Vigilante renderer mapping changed");

expect(entities, /monster\("vigilante",[\s\S]{0,180}?Kind\.VIGILANTE\), 1\.6F, 3\.1F\)/,
  "Vigilante dimensions are not 1.6 x 3.1");
expect(pure, /VIGILANTE\(false, false, 70\.0D, 25\.0D, 23\.0D, 0\.20D, 1\.0D, 32\.0D, 5\.0F, 2\.0D\)/,
  "Vigilante base attributes do not match EntityAnged");
expect(pure, /case VIGILANTE -> dimensions\.withEyeHeight\(3\.0F\)/,
  "Vigilante eye height is not 3.0");
expect(pure, /new VigilanteMeleeGoal\(\)[\s\S]{0,180}?new VigilanteRangedGoal\(\)[\s\S]{0,180}?new VigilanteRangeSwitchGoal\(\)/,
  "Vigilante goal priorities are incomplete");
expect(pure, /distanceToSqr\(target\) < 25\.0D && hasLineOfSight\(target\)/,
  "Vigilante five-block melee switch is missing");
const rangedGoal = isolate(pure, "private final class VigilanteRangedGoal",
  "private final class WardenChargeGoal", "Vigilante ranged goal");
expect(rangedGoal, /rangedAttackTime = -1[\s\S]*seeTime/, "Vigilante ranged timer state is missing");
expect(rangedGoal, /rangedDistance = followRange \* 0\.5D[\s\S]*maximumRangedDistance = rangedDistance \* rangedDistance[\s\S]*distance <= maximumRangedDistance && seeTime >= 10[\s\S]*getNavigation\(\)\.stop\(\)/,
  "Vigilante ranged navigation stop range is missing");
expect(rangedGoal, /tickCount % 21 == 10 && distance > followRange \* followRange/,
  "Vigilante follow-range target check no longer uses the legacy status cadence");
expect(rangedGoal, /rangedAttackTime = 20/, "Vigilante projectile cadence is not 20 ticks");
expect(rangedGoal, /fireVigilanteProjectile\(target\)/, "Vigilante does not launch its dedicated projectile");
expect(pure, /configureLegacyFireball\(this, ParasiteProjectileEntity\.Mode\.ANGED_BALL[\s\S]{0,180}?vigilanteRangedDamage\(\)/,
  "Vigilante projectile payload is not configured");
expect(pure, /ModSounds\.EMANA_SHOOTING\.get\(\)/, "Vigilante shooting sound is missing");
expect(pure, /target\.knockback\(1\.0D, getX\(\) - target\.getX\(\), getZ\(\) - target\.getZ\(\)\)/,
  "Vigilante melee knockback does not match EntityAnged");
expect(pure, /VIGILANTE_SKIN[\s\S]*EntityDataSerializers\.BYTE[\s\S]*tag\.putByte\("VigilanteSkin"/,
  "Vigilante skin synchronization or NBT persistence is missing");
expect(pure, /Config\.tendrilHealth\(\)[\s\S]*reduceAllResistances\(Config\.purePointDamageCap\(\) \/ 2\)/,
  "Vigilante tendril health or resistance cut configuration is missing");
expect(pure, /ConfiguredOrbEffects\.apply\(this, target, nearbyEntities, MobsConfig\.vigilanteOrbEffects\(\)\)/,
  "Vigilante scary-orb effects are not applied");
expect(model, /VIGILANTE_TEXTURE[\s\S]*VIGILANTE_HEAVY_TEXTURE[\s\S]*getVigilanteSkin\(\) == 7/,
  "Vigilante heavy texture selection is missing");
expect(client, /PrimitiveParasiteRenderer<>\(context, "vigilante", 1\.2F\)/,
  "Vigilante renderer shadow is not 1.2");
expect(projectile, /mode == Mode\.ANGED_BALL[\s\S]{0,120}?return true/, "Vigilante projectile parasite collision rule is missing");
expect(projectile, /impactAngedBall[\s\S]{0,850}?MobsConfig\.vigilanteRangedDamage\(\)[\s\S]{0,500}?MobEffects\.BLINDNESS, 300, 0[\s\S]{0,160}?ModMobEffects\.CORROSION, 100, 0/,
  "Vigilante projectile direct damage or cloud effects are incomplete");
expect(projectile, /cloud\.setRadius\(2\.5F\)[\s\S]*cloud\.setRadiusOnUse\(-0\.5F\)[\s\S]*cloud\.setWaitTime\(10\)[\s\S]*cloud\.setDuration\(100\)/,
  "Vigilante projectile cloud geometry is incomplete");
expect(renderer, /case ANGED_BALL -> ANGED_TEXTURE/, "Vigilante projectile texture is not selected");
expect(config, /"srparasites:vigilante", "vigilanteRangedDamageMultiplier"[\s\S]*vigilanteOrbEffects/,
  "Vigilante config entries are missing");
expect(configEvents, /entity instanceof PureParasiteEntity pure[\s\S]{0,100}?pure\.applyConfiguredAttributes\(\)/,
  "Vigilante configured attributes are not applied when it joins the world");
expect(pure, /applyConfiguredAttributes\(\)[\s\S]{0,800}?MobsConfig\.vigilanteHealth\(\)[\s\S]{0,300}?MobsConfig\.vigilanteArmor\(\)[\s\S]{0,300}?MobsConfig\.vigilanteMeleeDamage\(\)[\s\S]{0,300}?MobsConfig\.vigilanteKnockbackResistance\(\)/,
  "Vigilante configured base attributes are incomplete");
expect(legacyConfig, /defineInRange\("tendrilHealth", 0\.5D, 0\.5D, 100\.0D\)[\s\S]*defineInRange\("purePointDamageCap", 12, 0, 1000\)/,
  "shared tendril configuration is missing");
for (const texture of [
  "textures/entity/monster/anged.png",
  "textures/entity/monster/angedh.png",
  "textures/entity/projectile/anged.png"
]) {
  expectFile(`src/main/resources/assets/csrp/${texture}`);
}
expect(sounds, /EMANA_SHOOTING = register\("emana\.shooting"\)/,
  "Vigilante shooting sound registry is missing");

if (failures.length) {
  console.error(`Vigilante port verification failed (${failures.length} checks):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Vigilante -> EntityAnged entity-specific behavior audit passed.");
console.log(`Original sources: ${originalRoot}`);
