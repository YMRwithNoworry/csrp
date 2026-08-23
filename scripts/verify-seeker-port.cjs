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
const manifest = behaviorPorts.seeker;
if (!manifest || manifest.originalClass !== "EntitySoo" || manifest.status !== "audited"
    || manifest.auditScope !== "entity-specific") {
  failures.push("seeker entity-specific audit manifest is invalid");
}

const pure = current("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const entities = current("src/main/java/alku/csrp/registry/ModEntities.java");
const client = current("src/main/java/alku/csrp/client/ClientModEvents.java");
const sounds = current("src/main/java/alku/csrp/entity/ParasiteSoundProfiles.java");
const originalSoo = original("com/dhanantry/scapeandrunparasites/entity/monster/pure/EntitySoo.java");
const originalFlight = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIFlightAttack.java");
const originalLimits = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIFlightLimits.java");
const attributes = original("com/dhanantry/scapeandrunparasites/util/SRPAttributes.java");
const mobConfig = original("com/dhanantry/scapeandrunparasites/util/config/SRPConfigMobs.java");

expect(originalSoo, /func_70105_a\(1\.9F, 2\.6F\)[\s\S]*func_189654_d\(true\)[\s\S]*field_70158_ak = true/,
  "original Seeker flight dimensions or physics changed");
expect(originalSoo, /new EntitySoo\.AIMoveControl\(this\)[\s\S]*alafhaMaxY != 256[\s\S]*EntityAIFlightLimits\(this, SRPConfigMobs\.alafhaMaxY, true\)/,
  "original Seeker move control or flight limit changed");
expect(originalSoo, /adaptationCap = 0\.95F[\s\S]*scentCool = 800/, "original Seeker adaptation or scent cooldown changed");
expect(originalSoo, /EntityAIHurtByTarget\(this, true[\s\S]*EntityAIFlightAttack\(this, 64\.0\)[\s\S]*new EntitySoo\.AIMoveRandom\(\)/,
  "original Seeker goals changed");
expect(originalSoo, /field_70122_E[\s\S]*field_70163_u \+ 5\.0[\s\S]*, 0\.5\)/,
  "original Seeker ground lift changed");
expect(originalSoo, /scentCool < 0[\s\S]*srpTicks == 10[\s\S]*useScent[\s\S]*deveScentUse[\s\S]*scentCap[\s\S]*setTargetToKill[\s\S]*setDieToE\(true\)[\s\S]*setCanFollow\(true\)[\s\S]*scentCool = 800/,
  "original Seeker scent behavior changed");
expect(originalSoo, /func_180430_e\(float distance, float damageMultiplier\) \{\s*\}/,
  "original Seeker fall-damage immunity changed");
expect(originalFlight, /distance = distance \* distance[\s\S]*delay >= 6[\s\S]*attackSurr\(\)/,
  "original Seeker flight-target behavior changed");
expect(originalLimits, /field_70181_x -= 0\.04/, "original Seeker flight limit force changed");
expect(attributes, /ALAFHA_HEALTH = 80\.0[\s\S]*ALAFHA_ARMOR = 20\.0[\s\S]*ALAFHA_MELLE = 22\.0/,
  "original Seeker attributes changed");
expect(mobConfig, /alafhaHealthMultiplier = 1\.0F[\s\S]*alafhaDamageMultiplier = 1\.0F[\s\S]*alafhaArmorMultiplier = 1\.0F[\s\S]*alafhaKDResistanceMultiplier = 1\.0F/,
  "original Seeker shared config changed");

expect(entities, /monster\("seeker",[\s\S]{0,180}?Kind\.SEEKER\), 1\.9F, 2\.6F\)/,
  "Seeker registration dimensions are not 1.9 x 2.6");
expect(pure, /kind == Kind\.SEEKER[\s\S]{0,100}?new SeekerMoveControl\(this\)[\s\S]{0,80}?noPhysics = true/,
  "Seeker custom flight physics are missing");
expect(pure, /Kind\.OVERSEER \|\| kind == Kind\.SEEKER \? MobsConfig\.overseerHealth\(\)/,
  "Seeker health does not use the original Overseer config");
expect(pure, /case SEEKER -> (?:dimensions\.withEyeHeight\(1\.6F\)|1\.6F)/,
  "Seeker eye height is not 1.6");
expect(pure, /case SEEKER -> \{[\s\S]{0,180}?new FlightPursuitGoal\(0\.50D\)[\s\S]{0,180}?new OverseerFlightLimitGoal\(\)[\s\S]{0,180}?new SeekerRandomFlightGoal\(\)/,
  "Seeker flight, limit, or random-flight goals are incomplete");
expect(pure, /activeKind == Kind\.SEEKER && onGround\(\)[\s\S]{0,120}?getY\(\) \+ 5\.0D[\s\S]{0,80}?, 0\.5D\)/,
  "Seeker ground lift does not match EntitySoo");
expect(pure, /case GRUNT, BOMBER_LIGHT, OVERSEER, SEEKER, WARDEN -> 0\.95F/,
  "Seeker adaptation cap is not 95 percent");
expect(pure, /activeKind\(\) == Kind\.SEEKER[\s\S]{0,140}?random\.nextBoolean\(\)[\s\S]{0,100}?getAdaptationHitStatus\(\) > 0[\s\S]{0,100}?ModSounds\.get\("mob\.silence"\)/,
  "Seeker adapted hurt silence is missing");
expect(pure, /activeKind\(\) == Kind\.SEEKER \? 2\.0F : super\.getSoundVolume\(\)/,
  "Seeker sound volume is not 2.0");
expect(pure, /private void tickSeekerScent\(\)[\s\S]*?setTargetToKill\(target, false\)[\s\S]*setDieAfterKilling\(true\)[\s\S]*setCanFollow\(true\)/,
  "Seeker scent deployment is incomplete");
expect(pure, /private static final class SeekerMoveControl[\s\S]{0,1400}?0\.05D \* speedModifier[\s\S]*mob\.yBodyRot = mob\.getYRot\(\)/,
  "Seeker custom move-control acceleration or rotation is incomplete");
expect(pure, /public void tick\(\)[\s\S]{0,80}?if \(activeKind\.flying\)[\s\S]{0,100}?setNoGravity\(true\)/,
  "Seeker no-gravity tick state is missing");
expect(sounds, /register\("alafha", "overseer", "architect", "seeker"\)/,
  "Seeker alafha sound profile is missing");
expect(client, /ModEntities\.SEEKER\.get\(\), NoopRenderer::new/,
  "Seeker renderer does not preserve the original no-renderer contract");

if (failures.length) {
  console.error(`Seeker port verification failed (${failures.length} checks):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Seeker -> EntitySoo entity-specific behavior audit passed.");
console.log(`Original sources: ${originalRoot}`);
