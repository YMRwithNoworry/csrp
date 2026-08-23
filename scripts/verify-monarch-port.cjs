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
const isolate = (text, start, end, label) => {
  const startIndex = text.indexOf(start);
  const endIndex = startIndex < 0 ? -1 : text.indexOf(end, startIndex + start.length);
  if (startIndex < 0 || endIndex < 0) {
    failures.push(`could not isolate ${label}`);
    return "";
  }
  return text.slice(startIndex, endIndex);
};

const manifest = behaviorPorts.monarch;
if (!manifest || manifest.originalClass !== "EntityOrch" || manifest.status !== "audited"
    || manifest.auditScope !== "entity-specific") {
  failures.push("monarch entity-specific audit manifest is invalid");
}

const pure = current("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const base = current("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const projectile = current("src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java");
const config = current("src/main/java/alku/csrp/config/MobsConfig.java");
const model = current("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const client = current("src/main/java/alku/csrp/client/ClientModEvents.java");
const entities = current("src/main/java/alku/csrp/registry/ModEntities.java");

const orch = original("com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityOrch.java");
const attributes = original("com/dhanantry/scapeandrunparasites/util/SRPAttributes.java");
const legacyConfig = original("com/dhanantry/scapeandrunparasites/util/config/SRPConfig.java");
const mobConfig = original("com/dhanantry/scapeandrunparasites/util/config/SRPConfigMobs.java");
const skill = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAISkill.java");
const projectileGoal = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIAttackProjectile.java");
const melee = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIAttackMeleeStatusAOE.java");
const swimming = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAISwimmingDiving.java");
const waterLeap = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIWaterLeapAtTargetStatus.java");
const evade = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIEvadeDash.java");
const webball = original("com/dhanantry/scapeandrunparasites/entity/projectile/EntityProjectileWebball.java");
const renderer = original("com/dhanantry/scapeandrunparasites/client/renderer/entity/pure/RenderOrch.java");

expect(orch, /func_70105_a\(1\.901F, 4\.1F\)/, "original Monarch dimensions changed");
expect(attributes, /ORCH_HEALTH\s*=\s*75\.0[\s\S]*ORCH_ARMOR\s*=\s*10\.0[\s\S]*ORCH_ATTACK_DAMAGE\s*=\s*25\.0[\s\S]*ORCH_KD_RESISTANCE\s*=\s*1\.0/,
  "original Monarch attributes changed");
expect(orch, /EntityAISkill\(this, 40, 100, 10, true, 14\)[\s\S]*setskillLeapValues\(0\.5F, 3\.5, 4\)/,
  "original Monarch skill parameters changed");
expect(orch, /EntityAISwimmingDiving\(this, 0\.12\)[\s\S]*EntityAIAttackProjectile\(this, 40, 15, 4\)/,
  "original Monarch swimming or volley parameters changed");
expect(orch, /EntityAIWaterLeapAtTargetStatus\(this, 0\.7F, 1\.5, 3, 20, 7\)/,
  "original Monarch water leap parameters changed");
expect(orch, /EntityAIAttackMeleeStatusAOE\(this, 1\.3, false, 8\.0, 2\.0\)/,
  "original Monarch melee parameters changed");
expect(orch, /EntityAIEvadeDash\(this, 17, 2, 5, 3\.5, 15\)/,
  "original Monarch evade parameters changed");
expect(skill, /distanceC\s*=\s*miniDistance \* miniDistance[\s\S]*distanceL\s*=\s*maxDistance \* maxDistance/,
  "original skill distance interpretation changed");
expect(projectileGoal, /attackTimer > this\.cooldown[\s\S]*attackTimer % this\.tickInterval == 0[\s\S]*shootingUpdate\+\+/,
  "original projectile volley cadence changed");
expect(melee, /this\.attack = attackD \* attackD[\s\S]*attackTick = this\.attacker\.getAttackSpeed\(\)/,
  "original AOE melee range or cadence changed");
expect(swimming, /field_70181_x = this\.parent\.field_70181_x - this\.yMotion[\s\S]*nextFloat\(\) < 0\.8F/,
  "original swimming and diving behavior changed");
expect(waterLeap, /this\.leapMotionY = leapMotionYIn[\s\S]*this\.jumpSpeed = speed[\s\S]*this\.jCooldown = cooldown[\s\S]*this\.jumpR = jumpDamageRange/,
  "original water leap implementation changed");
expect(evade, /eCooldown = cooldown[\s\S]*eDuration = duration[\s\S]*blockDistance = distance \* distance[\s\S]*maxDis = maxD \* maxD/,
  "original evade implementation changed");
expect(legacyConfig, /"srparasites:monarch;5;20;4"/, "original Monarch block profile changed");
expect(mobConfig, /orchOrbEffects\s*=\s*new String\[\][\s\S]*minecraft:hunger[\s\S]*srparasites:needler[\s\S]*minecraft:mining_fatigue[\s\S]*minecraft:wither/,
  "original Monarch orb effects changed");
expect(renderer, /new ModelOrch\(\), 1\.2F[\s\S]*case 1:[\s\S]*TEXTURESP[\s\S]*case 7:[\s\S]*TEXTUREH/,
  "original Monarch renderer mapping changed");
expect(webball, /WEB_BLIND_CHANCE = 0\.3F[\s\S]*WEB_BLIND_TICKS = 60[\s\S]*Blocks\.field_150321_G/,
  "original webball impact behavior changed");
expect(webball, /field_70173_aa > 60[\s\S]*nextInt\(3\) \+ 1[\s\S]*SRPBlocks\.SRPWeb/,
  "original webball timeout spread changed");

expect(entities, /monster\("monarch",[\s\S]{0,180}?Kind\.MONARCH\), 1\.901F, 4\.1F\)/,
  "Monarch dimensions are not 1.901 x 4.1");
expect(pure, /MONARCH\(false, true, 75\.0D, 10\.0D, 25\.0D, 0\.2775D, 1\.0D, 32\.0D, 5\.0F, 4\.0D\)/,
  "Monarch attributes do not match EntityOrch");
expect(pure, /case MONARCH -> (?:dimensions\.withEyeHeight\(3\.5F\)|3\.5F)/, "Monarch eye height is missing");
expect(pure, /isClimberType[\s\S]{0,180}?ModEntities\.MONARCH/,
  "Monarch climbing navigation type is missing");
expect(pure, /activeKind\(\)\.climbs && horizontalCollision/,
  "Monarch wall-climbing condition is missing");
expect(pure, /usesDefaultFloatGoal\(\)[\s\S]{0,120}?Kind\.MONARCH/,
  "Monarch still uses the conflicting default FloatGoal");
expect(base, /addBlockBreakProfiles\(profiles, 5\.0F, 20, 4, "monarch"\)/,
  "Monarch 5;20;4 shared block profile is missing");

expect(pure, /goalSelector\.addGoal\(0, new MonarchSkillLeapGoal\(\)\)[\s\S]{0,400}?new MonarchSwimmingDivingGoal\(\)[\s\S]{0,400}?new MonarchWebVolleyGoal\(\)[\s\S]{0,400}?new MonarchWaterLeapGoal\(\)[\s\S]{0,400}?new MonarchAreaMeleeGoal\(\)[\s\S]{0,400}?new MonarchEvasiveDashGoal\(17, 2, 5, 3\.5D, 15\)/,
  "Monarch goal set or priorities are incomplete");
expect(pure, /distance >= 100\.0D && distance < 10_000\.0D[\s\S]{0,120}?chargeTicks\+\+[\s\S]{0,180}?hasEffect\(ModMobEffects\.RAGE(?:\.get\(\))?\)[\s\S]{0,80}?chargeTicks\+\+[\s\S]{0,180}?chargeTicks >= 40/,
  "Monarch skill charge distance, rage acceleration, or cooldown is missing");
expect(pure, /offset\.x \/ horizontalLength \* 3\.5D \* 0\.9D[\s\S]{0,100}?0\.5D/,
  "Monarch skill leap velocity is missing");
expect(pure, /monarchSkillLeapTicks % 5 == 0 && monarchSkillLeapTicks < 40[\s\S]{0,80}?spawnMonarchBuglin\(\)/,
  "Monarch skill no longer spawns Buglins every five ticks");
expect(pure, /horizontalRange = 4[\s\S]{0,500}?horizontalRange = 0[\s\S]{0,300}?y <= 20 \+ verticalOffset[\s\S]{0,500}?adjustBlockBreakHardness\(5\.0F\)/,
  "Monarch skill block clearing no longer matches 5;20;4");

expect(pure, /MonarchSwimmingDivingGoal[\s\S]{0,700}?-0\.12D[\s\S]{0,500}?random\.nextFloat\(\) < 0\.8F/,
  "Monarch swimming and diving behavior is missing");
const monarchWaterLeap = isolate(pure, "private final class MonarchWaterLeapGoal",
  "private final class MonarchAreaMeleeGoal", "Monarch water leap goal");
expect(monarchWaterLeap, /attackTimer >= 20 && attacking == 0/,
  "Monarch water leap charge is missing");
expect(monarchWaterLeap, /horizontalLength \* 1\.5D \* 0\.9D[\s\S]{0,160}?0\.7D \+ targetY[\s\S]{0,180}?horizontalLength \* 1\.5D \* 0\.9D/,
  "Monarch water leap velocity is missing");
expect(pure, /attacking >= 3 && onGround\(\)[\s\S]{0,120}?performMonarchLandingAttack\(7\.0D\)/,
  "Monarch seven-block landing AOE is missing");
expect(pure, /distanceToSqr\(target\) < 4225\.0D[\s\S]{0,200}?attackTimer > 40[\s\S]{0,200}?shots < 4[\s\S]{0,140}?attackTimer % 15 == 0/,
  "Monarch 40/15/4 projectile volley is missing");
expect(pure, /configureLegacyFireball\(this, ParasiteProjectileEntity\.Mode\.WEB[\s\S]{0,100}?61\)[\s\S]{0,100}?setWebKind\(webKind\)[\s\S]{0,120}?ModSounds\.DORPA_RANGE/,
  "Monarch legacy webball launch or sound is missing");
expect(pure, /fireWebProjectile\(target, 1\)/, "Monarch does not use legacy webball type 2");

expect(projectile, /case WEB -> ParticleTypes\.POOF/, "webball trail does not match EXPLOSION_NORMAL");
expect(projectile, /directHit instanceof Player[\s\S]{0,160}?random\.nextFloat\(\) < 0\.30F[\s\S]{0,120}?MobEffects\.BLINDNESS, 60, 0/,
  "webball player blindness is missing");
expect(projectile, /Blocks\.COBWEB\.defaultBlockState\(\)/, "webball no longer places vanilla cobwebs on impact");
expect(projectile, /blockHit\.getBlockPos\(\)\.relative\(blockHit\.getDirection\(\)\)/,
  "webball block impact is not placed on the hit face");
expect(projectile, /tickCount > 60[\s\S]{0,100}?scatterWebsAround\(\)/,
  "webball 60-tick timeout is missing");
expect(projectile, /scatterWebsAround\(\)[\s\S]{0,300}?random\.nextInt\(3\) \+ 1[\s\S]{0,700}?ModBlocks\.SRP_WEB/,
  "webball one-to-three SRP web spread is missing");
const webImpactCase = isolate(projectile, "case WEB -> {", "case NEEDLE ->", "web impact switch case");
if (/MOVEMENT_SLOWDOWN|POISON|hurt\(/.test(webImpactCase)) {
  failures.push("webball impact still applies damage, slowness, or poison");
}

expect(pure, /distance <= 4\.0D && hasLineOfSight\(target\)[\s\S]{0,160}?attackCooldown = 10[\s\S]{0,100}?performAreaMelee\(target\)/,
  "Monarch two-block AOE melee range or cadence is missing");
expect(pure, /getNavigation\(\)\.moveTo\(target, distance > 64\.0D \? 1\.3D : 1\.0D\)/,
  "Monarch running-distance pursuit speed is missing");
expect(pure, /case MONARCH ->[\s\S]{0,180}?add\(0\.0D, 0\.5D, 0\.0D\)/,
  "Monarch successful melee no longer lifts its victim");
const monarchEvade = isolate(pure, "private final class MonarchEvasiveDashGoal", "private void startMonarchSkillLeap", "Monarch evade goal");
expect(monarchEvade, /distance > minimumDistanceSqr && distance < maximumDistanceSqr && hasLineOfSight\(target\)/,
  "Monarch evade distance and visibility gate is missing");
expect(monarchEvade, /dashStrength \* 0\.8D[\s\S]{0,700}?ParticleTypes\.ENCHANTED_HIT/,
  "Monarch evade impulse or particles are missing");

expect(pure, /MONARCH_SKIN[\s\S]*EntityDataSerializers\.BYTE[\s\S]*tag\.putByte\("MonarchSkin"[\s\S]*setMonarchSkin\(tag\.contains\("MonarchSkin"\)/,
  "Monarch skin synchronization or NBT persistence is missing");
expect(pure, /setMonarchSkin\(random\.nextBoolean\(\) \? 1 : 7\)/,
  "Monarch fragile/heavy variant selection is missing");
expect(pure, /getMonarchSkin\(\) == 1 \? Kind\.MONARCH\.maxHealth \* 0\.5D[\s\S]{0,200}?attackDamage \* 1\.5D/,
  "Monarch fragile variant attributes are missing");
expect(pure, /getMonarchSkin\(\) == 7[\s\S]{0,100}?baseHardness \* 2\.0F/,
  "heavy Monarch block hardness multiplier is missing");
expect(model, /case 1 -> MONARCH_SKIN_TEXTURE;[\s\S]*case 7 -> MONARCH_HEAVY_TEXTURE;[\s\S]*default -> MONARCH_TEXTURE;/,
  "Monarch variant texture selection is missing");
for (const texture of ["orch.png", "orchsp1.png", "orchh.png"]) {
  current(`src/main/resources/assets/csrp/textures/entity/${texture}`);
}
expect(client, /PrimitiveParasiteRenderer<>\(context, "monarch", 1\.2F\)/,
  "Monarch renderer shadow is not 1.2");

expect(config, /"srparasites:monarch", "monarchOrbEffects"[\s\S]{0,500}?minecraft:hunger[\s\S]{0,180}?csrp:needler[\s\S]{0,180}?minecraft:mining_fatigue[\s\S]{0,180}?minecraft:wither/,
  "Monarch scary-orb defaults are incomplete");
expect(pure, /activeKind\(\) == Kind\.MONARCH[\s\S]{0,120}?ConfiguredOrbEffects\.apply\(this, target, nearbyEntities, MobsConfig\.monarchOrbEffects\(\)\)/,
  "Monarch scary-orb effects are not applied");
expect(pure, /Kind\.MONARCH && entityData\.get\(MONARCH_COMBAT_STATUS\) != 0[\s\S]{0,100}?mob\.silence/,
  "Monarch combat ambient silence is missing");
expect(pure, /ModSounds\.HEAVY_MULTIPLE_STEP\.get\(\), 0\.15F, 1\.0F/,
  "Monarch heavy step sound is missing");
expect(pure, /tryMonarchSummonSupport[\s\S]{0,220}?hasLineOfSight\(target\)[\s\S]{0,800}?random\.nextInt\(4\) == 0[\s\S]{0,900}?ModEntities\.SEIZER[\s\S]{0,900}?ModEntities\.DISPATCHERTEN/,
  "Monarch hidden-target Seizer/Dispatcher support behavior is missing");

if (failures.length) {
  console.error(`Monarch port verification failed (${failures.length} checks):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Monarch -> EntityOrch entity-specific behavior audit passed.");
console.log(`Original sources: ${originalRoot}`);
