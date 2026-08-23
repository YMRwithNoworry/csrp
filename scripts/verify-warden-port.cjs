const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const { behaviorPorts } = require("./entity-port-manifest.cjs");

const root = path.resolve(__dirname, "..");
const defaultOriginalRoot = "D:\\code\\mod-decompiler-placeholder".replace(
  "mod-decompiler-placeholder",
  "\u6a21\u7ec4\u53cd\u7f16\u8bd1\u5668\\decompiled\\[\u9003\u9038\uff1a\u5bc4\u751f\u4f53] SRParasites-1.10.8"
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
const expectSameFile = (currentRelative, originalRelative) => {
  const currentFile = path.join(root, currentRelative);
  const originalFile = path.join(originalRoot, originalRelative);
  if (!fs.existsSync(currentFile) || !fs.existsSync(originalFile)) {
    failures.push(`missing texture comparison input: ${currentRelative}`);
    return;
  }
  const hash = (file) => crypto.createHash("sha256").update(fs.readFileSync(file)).digest("hex");
  if (hash(currentFile) !== hash(originalFile)) {
    failures.push(`${currentRelative}: texture differs from SRP 1.10.8`);
  }
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

const manifest = behaviorPorts.warden;
if (!manifest || manifest.originalClass !== "EntityGanro" || manifest.status !== "audited"
    || manifest.auditScope !== "entity-specific") {
  failures.push("warden entity-specific audit manifest is invalid");
}

const pure = current("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const wave = current("src/main/java/alku/csrp/entity/WardenShockwaveEntity.java");
const config = current("src/main/java/alku/csrp/config/MobsConfig.java");
const model = current("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const client = current("src/main/java/alku/csrp/client/ClientModEvents.java");
const entities = current("src/main/java/alku/csrp/registry/ModEntities.java");

const ganro = original("com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityGanro.java");
const attributes = original("com/dhanantry/scapeandrunparasites/util/SRPAttributes.java");
const mobConfig = original("com/dhanantry/scapeandrunparasites/util/config/SRPConfigMobs.java");
const skill = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAISkill.java");
const melee = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIAttackMeleeStatusAOE.java");
const originalWave = original("com/dhanantry/scapeandrunparasites/entity/monster/EntityWaveShock.java");
const originalRenderer = original("com/dhanantry/scapeandrunparasites/client/renderer/entity/pure/RenderGanro.java");

expect(ganro, /func_70105_a\(0\.901F, 4\.2F\)/, "original Warden dimensions changed");
expect(ganro, /field_70138_W = 1\.0F/, "original Warden step height changed");
expect(ganro, /new EntityBody\(this, 0\.7F, 0\.9F, 1\.0F, 0\.7F, 3\.7F, 1, 1, true\)[\s\S]*new EntityBody\(this, 0\.7F, 0\.9F, 1\.0F, 0\.7F, 3\.7F, -1, 2, true\)/,
  "original Warden tendril hitboxes changed");
expect(ganro, /func_70047_e\(\)[\s\S]{0,60}?return 3\.5F/, "original Warden eye height changed");
expect(attributes, /GANRO_HEALTH\s*=\s*80\.0[\s\S]*GANRO_ARMOR\s*=\s*15\.0[\s\S]*GANRO_ATTACK_DAMAGE\s*=\s*25\.0[\s\S]*GANRO_KD_RESISTANCE\s*=\s*1\.0/,
  "original Warden attributes changed");
expect(ganro, /EntityAISkill\(this, 80, 100, 10, true, 14\)[\s\S]*EntityAISkill\(this, 40, \(int\)\(SRPConfig\.pureFollow \* 0\.7\), 2, false, 2\)[\s\S]*EntityAIAttackMeleeStatusAOE\(this, 1\.3, false, 8\.0, 4\.0\)[\s\S]*EntityAISkill\(this, 40, 32, 8, true, 1\)[\s\S]*EntityAIEvadeDash\(this, 20, 2, 4, 3\.0, 15\)/,
  "original Warden goal parameters changed");
expect(skill, /dis < this\.distanceC && dis >= this\.distanceL[\s\S]*attackTimer >= this\.sCooldown/,
  "original skill activation timing changed");
expect(melee, /this\.attack = attackD \* attackD[\s\S]*attackEntityAsMobAOE\(target\)/,
  "original Warden area-melee reach contract changed");
expect(ganro, /nextFloat\(\) >= 0\.1F[\s\S]*isPlayer \? 0\.525 : 1\.05[\s\S]*dx \* 0\.4, VERT, dz \* 0\.4/,
  "original Warden launch effect changed");
expect(ganro, /applyStackPotion\(SRPPotions\.BLEED_E, this, 80, 0\)[\s\S]*amount \* 3\.0F/,
  "original Warden body-part damage changed");
expect(mobConfig, /ganroOrbEffects\s*=\s*new String\[\][\s\S]*minecraft:hunger[\s\S]*srparasites:needler[\s\S]*minecraft:mining_fatigue[\s\S]*minecraft:absorption/,
  "original Warden orb effects changed");
expect(ganro, /setSkin\(7\)/, "original Warden heavy variant selection changed");
expect(originalRenderer, /new ModelGanro\(\), 1\.2F[\s\S]*case 7:[\s\S]*TEXTUREH/,
  "original Warden renderer mapping changed");
expect(originalWave, /func_70105_a\(3\.1F, 0\.2F\)[\s\S]*func_75642_a\(this\.targetX, this\.targetY, this\.targetZ, 0\.6\)/,
  "original moving shockwave geometry or speed changed");
expect(originalWave, /field_70173_aa > 20 \* this\.duration[\s\S]*func_72314_b\(1\.5, 0\.2, 1\.5\)[\s\S]*field_70181_x \+= 0\.64645/,
  "original moving shockwave lifetime or hit effect changed");

expect(entities, /monster\("warden",[\s\S]{0,180}?Kind\.WARDEN\), 0\.901F, 4\.2F\)/,
  "Warden dimensions are not 0.901 x 4.2");
expect(pure, /WARDEN\(false, true, 80\.0D, 15\.0D, 25\.0D, 0\.27D, 1\.0D, 32\.0D, 5\.0F, 2\.0D\)/,
  "Warden base attributes do not match EntityGanro");
expect(pure, /case WARDEN -> (?:dimensions\.withEyeHeight\(3\.5F\)|3\.5F)/,
  "Warden eye height is not 3.5");
expect(pure, /Kind\.MONARCH \|\| kind == Kind\.VIGILANTE \|\| kind == Kind\.WARDEN[\s\S]{0,120}?(?:Attributes\.STEP_HEIGHT|ForgeMod\.STEP_HEIGHT_ADDITION\.get\(\)), 1\.0D/,
  "Warden one-block step height is missing");
expect(pure, /new WardenLeapGoal\(\)[\s\S]{0,180}?new GruntSwimmingDivingGoal\(\)[\s\S]{0,180}?new WardenShockwaveGoal\(\)[\s\S]{0,180}?new WardenChargeGoal\(\)[\s\S]{0,220}?new GruntEvasiveDashGoal\(20, 2, 4, 3\.0D, 15\)[\s\S]{0,180}?new WardenAreaMeleeGoal\(\)/,
  "Warden skill, swimming, evade, or melee goals are incomplete");

const areaMelee = isolate(pure, "private final class WardenAreaMeleeGoal",
  "private final class WardenLeapGoal", "Warden area melee goal");
expect(areaMelee, /distance <= 16\.0D && hasLineOfSight\(target\)/,
  "Warden area melee does not use the original four-block reach");
expect(areaMelee, /moveTo\(target, distance > 64\.0D \? 1\.3D : 1\.0D\)/,
  "Warden area melee movement speeds changed");
expect(pure, /random\.nextFloat\(\) >= 0\.10F[\s\S]{0,500}?target instanceof Player \? 0\.525D : 1\.05D[\s\S]{0,180}?direction\.x \* 0\.4D/,
  "Warden ten-percent launch effect is incomplete");

const leapGoal = isolate(pure, "private final class WardenLeapGoal",
  "private final class WardenChargeGoal", "Warden leap goal");
expect(leapGoal, /distance < 100\.0D \|\| distance >= 10000\.0D \|\| !hasLineOfSight\(target\)/,
  "Warden leap activation range changed");
expect(leapGoal, /activationTicks >= 80[\s\S]*2\.5D \* 0\.9D[\s\S]*1\.2D/,
  "Warden leap charge or velocity changed");
expect(pure, /performWardenLandingAttack[\s\S]{0,420}?inflate\(5\.0D, 2\.0D, 5\.0D\)[\s\S]{0,260}?knockback\(2\.5D/,
  "Warden leap landing area or knockback changed");

const chargeGoal = isolate(pure, "private final class WardenChargeGoal",
  "private final class WardenShockwaveGoal", "Warden charge goal");
expect(chargeGoal, /distance < 64\.0D \|\| distance >= 1024\.0D \|\| !hasLineOfSight\(target\)/,
  "Warden charge activation range changed");
expect(chargeGoal, /activationTicks >= 40[\s\S]*chargeTicks < 20[\s\S]*15\.0D \* \(target\.getX\(\) - getX\(\)\) \/ distance/,
  "Warden charge wind-up or target lock changed");
expect(chargeGoal, /chargeTicks == 20[\s\S]*moveTo\(targetX, targetY, targetZ, 3\.0D\)[\s\S]*chargeTicks >= 60/,
  "Warden charge movement or completion timing changed");
expect(pure, /damageWardenChargeTargets[\s\S]{0,350}?inflate\(2\.0D, 0\.0D, 2\.0D\)[\s\S]{0,260}?knockback\(0\.5D/,
  "Warden charge repeated contact damage is incomplete");

const shockGoal = isolate(pure, "private final class WardenShockwaveGoal",
  "private void performWardenLandingAttack", "Warden shockwave goal");
expect(shockGoal, /getAttributeValue\(Attributes\.FOLLOW_RANGE\) \* 0\.7D[\s\S]*distance < 4\.0D/,
  "Warden shockwave activation distance changed");
expect(shockGoal, /activationTicks >= 40[\s\S]*setWardenStatus\(100\)[\s\S]*shockwaveTicks == 40[\s\S]*spawnWardenShockwave\(target\)/,
  "Warden shockwave charge sequence is incomplete");
expect(pure, /new WardenTendrilPart\(this, true\)[\s\S]{0,180}?new WardenTendrilPart\(this, false\)/,
  "Warden tendril parts are not installed");
const tendril = isolate(pure, "private static final class WardenTendrilPart",
  "private static final class OverseerHeadPart", "Warden tendril part");
expect(tendril, /parent\.getY\(\) \+ 3\.7D[\s\S]*hurtWardenTendril[\s\S]*scalable\(0\.7F, 0\.9F\)/,
  "Warden tendril position, hitbox, or damage delegation is incomplete");
expect(pure, /hurtWardenTendril[\s\S]{0,260}?ModMobEffects\.BLEED(?:\.get\(\))?, 80, 0[\s\S]{0,120}?amount \* 3\.0F/,
  "Warden tendril triple damage or bleeding is missing");
expect(pure, /WARDEN_SKIN[\s\S]*EntityDataSerializers\.BYTE[\s\S]*tag\.putByte\("WardenSkin"/,
  "Warden skin synchronization or NBT persistence is missing");
expect(pure, /ConfiguredOrbEffects\.apply\(this, target, nearbyEntities, MobsConfig\.wardenOrbEffects\(\)\)/,
  "Warden scary-orb effects are not applied");
expect(config, /"srparasites:warden", "wardenHealthMultiplier"[\s\S]*"srparasites:warden", "wardenDamageMultiplier"[\s\S]*wardenOrbEffects/,
  "Warden config entries are incomplete");
expect(model, /WARDEN_TEXTURE[\s\S]*WARDEN_HEAVY_TEXTURE[\s\S]*getWardenSkin\(\) == 7/,
  "Warden heavy texture selection is missing");
expect(client, /PrimitiveParasiteRenderer<>\(context, "warden", 1\.2F\)/,
  "Warden renderer shadow is not 1.2");

expect(entities, /WARDEN_SHOCKWAVE[\s\S]{0,260}?register\("warden_waveshock"[\s\S]{0,300}?sized\(3\.1F, 0\.2F\)/,
  "Warden moving shockwave entity is not registered at the original size");
expect(wave, /MOVEMENT_SPEED = 0\.6D[\s\S]*DURATION_TICKS = 20 \* 60/,
  "Warden shockwave speed or lifetime changed");
expect(wave, /tickCount > 20 && \(getX\(\) == xo \|\| getZ\(\) == zo\)/,
  "Warden shockwave stuck detection changed");
expect(wave, /inflate\(1\.5D, 0\.2D, 1\.5D\)[\s\S]*hurtWardenSkillTarget\(target\)[\s\S]*movement\.y \+ 0\.64645D/,
  "Warden shockwave repeated hit area or launch effect changed");
expect(wave, /adjustBlockBreakHardness\(5\.0F\)[\s\S]*for \(int x = -2; x <= 2; x\+\+\)[\s\S]*for \(int y = 0; y <= 3; y\+\+\)/,
  "Warden shockwave block-breaking volume changed");
expect(wave, /index < 35[\s\S]*BlockParticleOption/,
  "Warden shockwave ground-debris effect is missing");

expectSameFile(
  "src/main/resources/assets/csrp/textures/entity/monster/ganro.png",
  "assets/srparasites/textures/entity/monster/ganro.png"
);
expectSameFile(
  "src/main/resources/assets/csrp/textures/entity/monster/ganroh.png",
  "assets/srparasites/textures/entity/monster/ganroh.png"
);

if (failures.length) {
  console.error(`Warden port verification failed (${failures.length} checks):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Warden -> EntityGanro entity-specific behavior audit passed.");
console.log(`Original sources: ${originalRoot}`);
