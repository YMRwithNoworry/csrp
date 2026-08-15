const fs = require("node:fs");
const path = require("node:path");
const { behaviorPorts } = require("./entity-port-manifest.cjs");

const root = path.resolve(__dirname, "..");
const defaultOriginalRoot = "D:\\code\\模组反编译器\\decompiled\\[逃逸：寄生体] SRParasites-1.10.8";
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

const manifest = behaviorPorts.bomber_light;
if (!manifest || manifest.originalClass !== "EntityOmboo" || manifest.status !== "audited"
    || manifest.auditScope !== "entity-specific") {
  failures.push("bomber_light entity-specific audit manifest is invalid");
}

const pure = current("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const bomb = current("src/main/java/alku/csrp/entity/BombEntity.java");
const model = current("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const client = current("src/main/java/alku/csrp/client/ClientModEvents.java");
const entities = current("src/main/java/alku/csrp/registry/ModEntities.java");
const config = current("src/main/java/alku/csrp/config/MobsConfig.java");
const omboo = original("com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityOmboo.java");
const originalBomb = original("com/dhanantry/scapeandrunparasites/entity/projectile/EntityBomb.java");
const limits = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIFlightLimits.java");
const renderer = original("com/dhanantry/scapeandrunparasites/client/renderer/entity/pure/RenderOmboo.java");
const originalModel = original("com/dhanantry/scapeandrunparasites/client/model/entity/pure/ModelOmboo.java");
const attributes = original("com/dhanantry/scapeandrunparasites/util/SRPAttributes.java");

expect(omboo, /func_70105_a\(1\.7F, 2\.4F\)/, "original Omboo dimensions changed");
expect(attributes, /OMBOO_HEALTH\s*=\s*75\.0/, "original Omboo health changed");
expect(attributes, /OMBOO_ARMOR\s*=\s*20\.0/, "original Omboo armor changed");
expect(attributes, /OMBOO_ATTACK_DAMAGE\s*=\s*25\.0/, "original Omboo attack damage changed");
expect(attributes, /OMBOO_BOMBDAMAGE\s*=\s*20\.0/, "original Omboo bomb damage changed");
expect(omboo, /EntityAIFlightAttack\(this, SRPConfig\.pureFollow\)/, "original Omboo flight targeting changed");
expect(omboo, /EntityAIFlightLimits\(this, 7, false\)/, "original Omboo minimum flight height changed");
expect(omboo, /EntityAIFlightLimits\(this, 20, true\)/, "original Omboo combat flight limit changed");
expect(omboo, /this\.ccc >= 15[\s\S]*!target\.field_70122_E[\s\S]*this\.ccc = 7[\s\S]*< 25\.0/,
  "original Omboo bombing cadence or range changed");
expect(omboo, /setFuse\(80\)[\s\S]*setStren\(1\.0F\)[\s\S]*OMBOO_BOMBDAMAGE, 4/,
  "original Omboo bomb payload changed");
expect(omboo, /nextInt\(7\) == 0[\s\S]*func_70068_e[\s\S]*> 3\.0/,
  "original Omboo charge trigger changed");
expect(omboo, /field_72448_b \+ 10\.0[\s\S]*setCharging\(true\)/,
  "original Omboo charge destination changed");
expect(omboo, /func_174813_aQ\(\)\.func_72326_a[\s\S]*func_70652_k[\s\S]*setCharging\(false\)/,
  "original Omboo contact attack changed");
expect(omboo, /nextBoolean\(\) && this\.getHitStatus\(\) > 0 \? SRPSounds\.MOBSILENCE : SRPSounds\.OMBOO_HURT/,
  "original Omboo adaptive hurt silence changed");
expect(omboo, /nextInt\(15\) - 7[\s\S]*nextInt\(11\) - 5[\s\S]*nextInt\(6\) - 2[\s\S]*nextInt\(4\) \+ 3/,
  "original Omboo random flight modes changed");
expect(renderer, /new ModelOmboo\(\), 1\.3F/, "original Omboo shadow radius changed");
expect(renderer, /case 7:[\s\S]*TEXTUREH/, "original Omboo heavy texture mapping changed");
expect(originalModel, /pops1 = new ModelRenderer\[\]\{this\.mpop6, this\.jointp7, this\.mpop8, this\.mpop16, this\.mpop5\}/,
  "original Omboo first pulse group changed");
expect(originalModel, /ageInTicks \* 0\.08F\) \* 0\.05[\s\S]*ageInTicks \* 0\.13F\) \* 0\.06[\s\S]*ageInTicks \* 0\.33F\) \* 0\.02[\s\S]*ageInTicks \* 0\.23F\) \* 0\.04/,
  "original Omboo pulse frequencies changed");

expect(entities, /monster\("bomber_light",[\s\S]{0,180}?Kind\.BOMBER_LIGHT\), 1\.7F, 2\.4F\)/,
  "Light Bomber dimensions are not 1.7 x 2.4");
expect(pure, /BOMBER_LIGHT\(true, false, 75\.0D, 20\.0D, 25\.0D, 0\.27D, 0\.15D, 32\.0D/,
  "Light Bomber attributes do not match EntityOmboo");
expect(pure, /case BOMBER_LIGHT -> dimensions\.withEyeHeight\(2\.4F\)/,
  "Light Bomber eye height is not 2.4");
expect(pure, /kind == Kind\.BOMBER_LIGHT[\s\S]{0,100}?new OmbooMoveControl\(this\)/,
  "Light Bomber does not use the original acceleration move controller");
expect(pure, /tickCount % 21 != 10[\s\S]{0,180}?getY\(\) \+ 5\.0D[\s\S]{0,300}?blockPosition\(\)\.below\(\)[\s\S]{0,180}?0\.5D/,
  "Light Bomber ground takeoff or near-ground lift is missing");
expect(pure, /class OmbooFlightLimitsGoal[\s\S]*configuredLimit != 256 && shouldPushOmbooDown\(configuredLimit, target\)[\s\S]*verticalAdjustment -= 0\.04D[\s\S]*shouldPushOmbooDown\(20, target\)[\s\S]*verticalAdjustment -= 0\.04D[\s\S]*hasBlockBelow\(7\)[\s\S]*verticalAdjustment \+= 0\.04D/,
  "Light Bomber independent configured, 20-block, and 7-block flight limits are missing");
expect(pure, /target == null \? !hasBlockBelow\(limit\) : target\.getY\(\) \+ limit > getY\(\)/,
  "Light Bomber targeted flight-limit predicate differs from EntityAIFlightLimits");
expect(config, /lightBomberFlightHeightLimit", 256, 0, 256/,
  "Light Bomber configurable flight height limit is missing");

expect(pure, /OMBOO_FLAGS[\s\S]*EntityDataSerializers\.BYTE/, "Light Bomber charge flag is not synchronized");
expect(pure, /random\.nextInt\(7\) == 0[\s\S]{0,100}?distanceToSqr\(target\) > 3\.0D/,
  "Light Bomber charge trigger does not match EntityOmboo");
expect(pure, /eye\.y \+ 10\.0D[\s\S]{0,100}?setOmbooCharging\(true\)/,
  "Light Bomber charge destination is missing");
expect(pure, /getBoundingBox\(\)\.intersects\(target\.getBoundingBox\(\)\)[\s\S]{0,100}?doHurtTarget\(target\)[\s\S]{0,100}?setOmbooCharging\(false\)/,
  "Light Bomber charge does not damage on contact");
expect(pure, /class OmbooRandomFlightGoal[\s\S]*distance > 100\.0D[\s\S]*distance < 36\.0D[\s\S]*random\.nextInt\(4\) \+ 3[\s\S]*random\.nextInt\(15\) - 7/,
  "Light Bomber random flight modes are missing");
expect(pure, /lostTargetTicks >= 6[\s\S]{0,100}?clearTarget\(\)/,
  "Light Bomber does not clear lost or distant targets after six checks");
expect(pure, /int cycleTick = tickCount % 21;[\s\S]{0,80}?cycleTick > 0 && cycleTick <= 10/,
  "Light Bomber flight target scan does not use the original 1-10/21 tick window");
expect(pure, /Config\.mobAttackingEnabled\(\) && candidate instanceof Mob[\s\S]{0,200}?Animal[\s\S]{0,120}?Creeper[\s\S]{0,120}?WaterAnimal/,
  "Light Bomber proactive target exclusions are missing");
expect(pure, /Config\.mobAttackingBlacklist\(\)\.stream\(\)\.anyMatch\(id::contains\)[\s\S]{0,120}?Config\.mobAttackingBlacklistInverted\(\) \? listed : !listed/,
  "Light Bomber mob-attacking blacklist or whitelist behavior is missing");

expect(pure, /checkTicks < 15[\s\S]*checkTicks = 7[\s\S]*x \* x \+ z \* z < 25\.0D/,
  "Light Bomber bomb cadence, grounded target delay, or five-block range is missing");
expect(pure, /bomb\.configure\(this, 80, 1\.0F, MobsConfig\.ombooBombDamage\(\), 4, 0,[\s\S]{0,80}?MobsConfig\.ombooGriefing\(\)\)/,
  "Light Bomber does not create the original independent bomb payload");
const bombGoal = pure.match(/class LightBomberBombGoal[\s\S]*?\n    }\n\n    private final class OverseerVolleyGoal/)?.[0] ?? "";
if (/triggerAttackAnimation\(\)/.test(bombGoal)) {
  failures.push("Light Bomber incorrectly plays a melee attack animation when dropping a bomb");
}
expect(omboo, /new EntityBomb\(this\.parent\.field_70170_p, this\.parent,[\s\S]{0,160}?out\.func_82149_j\(this\.parent\)/,
  "original Omboo bomb placement changed");
expect(pure, /fireBomb[\s\S]{0,500}?bomb\.configure[\s\S]{0,180}?bomb\.moveTo\(getX\(\), getY\(\), getZ\(\), getYRot\(\), getXRot\(\)\)/,
  "Light Bomber bomb does not preserve the original final feet-level placement");
expect(bomb, /movement\.add\(0\.0D, -0\.04D, 0\.0D\)[\s\S]*movement\.scale\(0\.98D\)[\s\S]*movement\.x \* 0\.7D, movement\.y \* -0\.5D/,
  "Omboo bomb gravity, drag, or ground bounce is missing");
expect(bomb, /setFuse\(fuseTicks - 1\)[\s\S]*if \(fuseTicks <= 0\)[\s\S]*explode\(\)/,
  "Omboo bomb 80-tick fuse lifecycle is missing");
expect(bomb, /target\.hurt[\s\S]*ModMobEffects\.VIRAL, 300[\s\S]*applyPrimitiveMinimumDamage/,
  "Omboo bomb direct damage, Viral, or minimum damage is missing");
expect(bomb, /setRadius\(rangeRadius\)[\s\S]*setWaitTime\(5\)[\s\S]*setDuration\(60\)[\s\S]*MobEffects\.POISON, 300[\s\S]*ModMobEffects\.COTH, 3600[\s\S]*ModMobEffects\.VIRAL, 3600/,
  "Omboo bomb poison/COTH/Viral cloud is incomplete");
expect(originalBomb, /field_70181_x -= 0\.04F[\s\S]*\*= 0\.98F[\s\S]*field_70122_E[\s\S]*\*= 0\.7F[\s\S]*\*= -0\.5/,
  "original bomb physics changed");

expect(pure, /OMBOO_SKIN[\s\S]*EntityDataSerializers\.BYTE/, "Light Bomber skin is not synchronized");
expect(pure, /activeKind\(\) == Kind\.BOMBER_LIGHT[\s\S]{0,180}?variantSpawnChance[\s\S]{0,180}?setOmbooSkin\(7\)/,
  "Light Bomber heavy variant selection is missing");
expect(pure, /tag\.putByte\("OmbooSkin"[\s\S]*setOmbooSkin\(tag\.contains\("OmbooSkin"\)/,
  "Light Bomber skin NBT persistence is missing");
expect(model, /textures\/entity\/monster\/omboo\.png[\s\S]*textures\/entity\/monster\/ombooh\.png[\s\S]*getOmbooSkin\(\) == 7/,
  "Light Bomber normal/heavy texture mapping is missing");
expect(model, /OMBOO_PULSE_GROUP_ONE = \{"mpop6", "jointp7", "mpop8", "mpop16", "mpop5"\}[\s\S]*OMBOO_PULSE_GROUP_TWO = \{"jointp11", "mpop1", "mpop13", "mpop19"\}[\s\S]*OMBOO_PULSE_GROUP_THREE = \{"jointp17", "jointp18", "mpop4", "jointp2", "mpop3"\}[\s\S]*OMBOO_PULSE_GROUP_FOUR = \{"mpop9", "mpop12", "jointp15", "mpop10", "mpop14"\}/,
  "Light Bomber legacy pulse bone groups are missing");
expect(model, /ageInTicks \* 0\.08F\) \* 0\.05F[\s\S]*ageInTicks \* 0\.13F\) \* 0\.06F[\s\S]*ageInTicks \* 0\.33F\) \* 0\.02F[\s\S]*ageInTicks \* 0\.23F\) \* 0\.04F/,
  "Light Bomber legacy pulse frequencies are missing");
expect(client, /BOMBER_LIGHT[\s\S]{0,120}?"bomber_light", 1\.3F/,
  "Light Bomber renderer shadow radius is not 1.3");
for (const texture of ["omboo.png", "ombooh.png"]) {
  current(`src/main/resources/assets/csrp/textures/entity/monster/${texture}`);
}

expect(pure, /entityData\.get\(OMBOO_COMBAT_STATUS\) != 0[\s\S]{0,180}?ModSounds\.get\("mob\.silence"\)/,
  "Light Bomber combat ambient silence is missing");
expect(pure, /activeKind\(\) == Kind\.BOMBER_LIGHT \|\| activeKind\(\) == Kind\.MONARCH[\s\S]{0,120}?random\.nextBoolean\(\) && getAdaptationHitStatus\(\) > 0[\s\S]{0,100}?ModSounds\.get\("mob\.silence"\)/,
  "Light Bomber adaptive hurt silence is missing");
expect(pure, /activeKind == Kind\.GRUNT \|\| activeKind == Kind\.BOMBER_LIGHT[\s\S]{0,300}?return;/,
  "Light Bomber still runs the duplicate Pure-tier block-breaking loop");

if (failures.length) {
  console.error(`Light Bomber port verification failed (${failures.length} checks):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Light Bomber -> EntityOmboo entity-specific behavior audit passed.");
console.log(`Original sources: ${originalRoot}`);
