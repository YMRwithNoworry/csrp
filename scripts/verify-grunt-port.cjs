const fs = require("fs");
const path = require("path");
const { behaviorPorts } = require("./entity-port-manifest.cjs");

const root = path.resolve(__dirname, "..");
const defaultOriginalRoot = "D:\\code\\模组反编译器\\decompiled\\SRParasites-1.10.7";
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

const manifest = behaviorPorts.grunt;
if (!manifest || manifest.originalClass !== "EntityFlog" || manifest.status !== "audited"
    || manifest.auditScope !== "entity-specific") {
  failures.push("grunt entity-specific audit manifest is invalid");
}

const pure = current("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const base = current("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const model = current("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const entities = current("src/main/java/alku/csrp/registry/ModEntities.java");
const flog = original("com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityFlog.java");
const evade = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIEvadeDash.java");
const skill = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAISkill.java");
const waterLeap = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIWaterLeapAtTargetStatus.java");
const config = original("com/dhanantry/scapeandrunparasites/util/config/SRPConfig.java");
const attributes = original("com/dhanantry/scapeandrunparasites/util/SRPAttributes.java");
const renderer = original("com/dhanantry/scapeandrunparasites/client/renderer/entity/pure/RenderFlog.java");

expect(flog, /func_70105_a\(0\.7666F, 1\.95F\)/, "original Grunt dimensions changed");
expect(attributes, /FLOG_HEALTH\s*=\s*20\.0/, "original Grunt health changed");
expect(attributes, /FLOG_ARMOR\s*=\s*7\.0/, "original Grunt armor changed");
expect(attributes, /FLOG_ATTACK_DAMAGE\s*=\s*13\.0/, "original Grunt damage changed");
expect(attributes, /FLOG_KD_RESISTANCE\s*=\s*0\.4/, "original Grunt knockback resistance changed");
expect(flog, /func_110148_a\(SharedMonsterAttributes\.field_111263_d\)\.func_111128_a\(0\.274172325\)/,
  "original Grunt movement speed changed");
expect(flog, /EntityAIAttackMeleeStatusAOE\(this, 1\.5, false, 0\.0, 3\.0\)/,
  "original Grunt AOE melee parameters changed");
expect(flog, /EntityAISkill\(this, 40, 100, 10, true, 14\)/,
  "original Grunt skill parameters changed");
expect(skill, /distanceC\s*=\s*miniDistance \* miniDistance[\s\S]*distanceL\s*=\s*maxDistance \* maxDistance/,
  "original skill distance interpretation changed");
expect(flog, /setskillLeapValues\(1\.1F, 3\.5, 0\)/, "original Grunt skill leap values changed");
expect(flog, /EntityAIEvadeDash\(this, 20, 2, 4, 1\.5, 15\)/,
  "original Grunt evade parameters changed");
expect(flog, /EntityAIWaterLeapAtTargetStatus\(this, 0\.7F, 1\.5, 3, 20, 0\)/,
  "original Grunt water leap parameters changed");
expect(waterLeap, /this\.leapMotionY = leapMotionYIn[\s\S]*this\.jumpSpeed = speed[\s\S]*this\.jCooldown = cooldown/,
  "original water leap implementation changed");
expect(config, /"srparasites:grunt;3;20;1"/, "original Grunt block-breaking profile changed");
expect(evade, /particleStatus\(\(byte\)10\)/, "original Grunt evade particles changed");
expect(evade, /eDuration = duration[\s\S]*inC = false[\s\S]*if \(this\.inC\)/,
  "original Grunt evade duration control flow changed");
expect(renderer, /case 5:[\s\S]*TEXTUREV[\s\S]*case 6:[\s\S]*TEXTUREB[\s\S]*case 7:[\s\S]*TEXTUREH/,
  "original Grunt texture mapping changed");

expect(entities, /monster\("grunt",[\s\S]{0,180}?Kind\.GRUNT\), 0\.7666F, 1\.95F\)/,
  "Grunt dimensions are not 0.7666 x 1.95");
expect(pure, /GRUNT\(false, true, 20\.0D, 7\.0D, 13\.0D, 0\.274172325D, 0\.40D, 32\.0D, 3\.0F, 1\.0D\)/,
  "Grunt attributes do not match EntityFlog");
expect(pure, /xpReward\s*=\s*75/, "Grunt experience reward is missing");
expect(pure, /withEyeHeight\(1\.73F\)/, "Grunt eye height is missing");
expect(pure, /SoundEvents\.SPIDER_STEP, 0\.15F, 1\.0F/, "Grunt step sound is missing");
expect(pure, /isClimberType[\s\S]{0,180}?ModEntities\.GRUNT/, "Grunt climbing navigation is missing");
expect(pure, /activeKind\(\)\.climbs && horizontalCollision/, "Grunt wall climbing is missing");
expect(pure, /usesDefaultFloatGoal\(\)[\s\S]{0,100}?activeKind\(\) != Kind\.GRUNT/,
  "Grunt still uses the conflicting default FloatGoal");

expect(pure, /GRUNT_SKIN[\s\S]*EntityDataSerializers\.BYTE/, "Grunt skin is not synchronized");
expect(pure, /random\.nextDouble\(\) < Config\.variantSpawnChance\(\)[\s\S]{0,180}?5 \+ random\.nextInt\(3\)/,
  "Grunt 5/6/7 natural variant selection is missing");
expect(pure, /tag\.putByte\("GruntSkin", entityData\.get\(GRUNT_SKIN\)\)/,
  "Grunt skin NBT save is missing");
expect(pure, /setGruntSkin\(tag\.contains\("GruntSkin"\) \? tag\.getByte\("GruntSkin"\) : 0\)/,
  "Grunt skin NBT load is missing");
expect(pure, /getGruntSkin\(\) == 5[\s\S]{0,200}?ModMobEffects\.VIRAL, 40, 0/,
  "virulent Grunt collision effect is missing");
expect(pure, /case GRUNT[\s\S]{0,250}?getGruntSkin\(\) == 5[\s\S]{0,100}?VIRAL, 40, 0[\s\S]{0,120}?getGruntSkin\(\) == 6[\s\S]{0,100}?BLEED, 40, 0/,
  "Grunt variant melee effects are missing");
expect(pure, /getGruntSkin\(\) == 7 \? baseHardness \* 2\.0F : baseHardness/,
  "heavy Grunt hardness multiplier is missing");
expect(model, /case 5 -> GRUNT_VIRULENT_TEXTURE;[\s\S]*case 6 -> GRUNT_BLEEDING_TEXTURE;[\s\S]*case 7 -> GRUNT_HEAVY_TEXTURE;/,
  "Grunt variant texture selection is missing");
for (const texture of ["flog.png", "flogv.png", "flogb.png", "flogh.png"]) {
  current(`src/main/resources/assets/csrp/textures/entity/monster/${texture}`);
}
expect(model, /textures\/entity\/monster\/flogv\.png[\s\S]*textures\/entity\/monster\/flogb\.png[\s\S]*textures\/entity\/monster\/flogh\.png/,
  "Grunt variant textures point outside the extracted texture directory");

expect(pure, /new GruntAreaMeleeGoal\(\)/, "Grunt AOE melee goal is missing");
expect(pure, /distanceToSqr\(target\) <= 9\.0D[\s\S]{0,300}?performAreaMelee\(target\)[\s\S]{0,100}?attackCooldown = 20/,
  "Grunt three-block AOE melee or attack cadence is missing");
expect(pure, /center\.getBoundingBox\(\)\.inflate\(radius\)[\s\S]{0,180}?hasLineOfSight\(target\) \|\| !super\.doHurtTarget\(target\)/,
  "Grunt AOE melee no longer requires line of sight for every victim");
expect(pure, /getNavigation\(\)\.moveTo\(target, 1\.5D\)/, "Grunt melee pursuit speed is missing");
expect(pure, /playSound\(ModSounds\.MOB_SWIPE\.get\(\), 2\.0F, 1\.0F\)/,
  "Grunt swipe sound is missing");
expect(pure, /boolean gruntAttack[\s\S]{0,180}?triggerAttackAnimation\(\)[\s\S]{0,900}?swing\(InteractionHand\.MAIN_HAND\)/,
  "Grunt attack animation is not broadcast when the AOE starts");
expect(pure, /Kind\.GRUNT[\s\S]{0,180}?ParasiteAnimations\.isAttacking\(this\)[\s\S]{0,120}?VIGILANTE_ATTACK_WALK/,
  "Grunt attack pose does not take priority over locomotion");

expect(pure, /goalSelector\.addGoal\(0, new GruntSkillLeapGoal\(\)\)/,
  "Grunt long-range skill does not use the original priority");
expect(pure, /distance >= 100\.0D && distance < 10_000\.0D[\s\S]{0,100}?chargeTicks\+\+[\s\S]{0,140}?chargeTicks >= 40 && onGround\(\) && !hasEffect\(MobEffects\.MOVEMENT_SLOWDOWN\)/,
  "Grunt skill does not charge for 40 ticks from 10 to 100 blocks before its grounded leap");
expect(pure, /chargeTicks >= 40[\s\S]{0,140}?chargeTicks = 0;[\s\S]{0,100}?startGruntSkillLeap\(target\)/,
  "Grunt skill charge is not cleared before a completed leap");
expect(pure, /offset\.x \/ horizontalLength \* 3\.5D \* 0\.9D[\s\S]{0,100}?1\.1D/,
  "Grunt skill leap velocity is missing");
const skillGoal = isolate(pure, "private final class GruntSkillLeapGoal", "private final class GruntAreaMeleeGoal", "Grunt skill goal");
if (/setFlags\(/.test(skillGoal)) failures.push("Grunt skill goal still blocks concurrent pursuit and melee");

expect(pure, /new GruntWaterLeapGoal\(\)/, "Grunt water leap goal is missing");
expect(pure, /0\.7D \+ heightBonus[\s\S]{0,150}?horizontalLength \* 1\.5D \* 0\.9D/,
  "Grunt water leap velocity is missing");
expect(pure, /if \(cooldown < 20\)/, "Grunt water leap cooldown is missing");
expect(pure, /if \(cooldown < 20\)[\s\S]{0,120}?return onGround\(\)/,
  "Grunt water leap no longer waits for ground contact");
const waterGoal = isolate(pure, "private final class GruntWaterLeapGoal", "private final class GruntSkillLeapGoal", "Grunt water leap goal");
if (/setFlags\(/.test(waterGoal)) failures.push("Grunt water leap still conflicts with swimming or melee");
expect(pure, /GruntSwimmingDivingGoal[\s\S]{0,700}?-0\.12D[\s\S]{0,500}?random\.nextFloat\(\) < 0\.8F/,
  "Grunt diving swim behavior is missing");

expect(pure, /new GruntEvasiveDashGoal\(20, 2, 4, 1\.5D, 15\)/,
  "Grunt evade parameters are missing");
const evadeGoal = isolate(pure, "private final class GruntEvasiveDashGoal", "private final class GruntSwimmingDivingGoal", "Grunt evade goal");
if (/setFlags\(/.test(evadeGoal)) failures.push("Grunt evade still owns MOVE and can suppress close-range melee");
expect(evadeGoal, /distance > minimumDistanceSqr && distance < maximumDistanceSqr && hasLineOfSight\(target\)[\s\S]{0,120}?cooldown\+\+/,
  "Grunt evade cooldown no longer requires the original distance and visibility window");
expect(evadeGoal, /dashStrength \* 0\.8D[\s\S]{0,250}?sendParticles\(ParticleTypes\.ENCHANTED_HIT[\s\S]{0,120}?41/,
  "Grunt evade impulse or original magic-critical particle count is missing");
if (/dashTicks|durationTicks\s*=/.test(evadeGoal)) {
  failures.push("Grunt evade adds a duration pause that the original runtime never enters");
}
expect(pure, /new EvasiveDashGoal\(100, 0\.75D\)/, "Monarch evade behavior was changed by the Grunt port");
expect(pure, /new EvasiveDashGoal\(100, 0\.70D\)/, "Warden evade behavior was changed by the Grunt port");

expect(base, /addBlockBreakProfiles\(profiles, 3\.0F, 20, 1, "grunt"\)/,
  "Grunt 3;20;1 block-breaking profile is missing");
expect(base, /hardness > adjustBlockBreakHardness\(profile\.hardness\(\)\)/,
  "Grunt heavy variant cannot modify the shared block-breaking profile");
expect(pure, /if \(activeKind == Kind\.GRUNT \|\| activeKind == Kind\.BOMBER_LIGHT[\s\S]{0,100}?\|\| activeKind\.blockHardness <= 0\.0F/,
  "Grunt still runs the duplicate Pure block-breaking loop");

if (failures.length) {
  console.error(`Grunt port verification failed (${failures.length} checks):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Grunt -> EntityFlog entity-specific behavior audit passed.");
console.log(`Original sources: ${originalRoot}`);
