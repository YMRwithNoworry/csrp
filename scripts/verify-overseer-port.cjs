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

const manifest = behaviorPorts.overseer;
if (!manifest || manifest.originalClass !== "EntityAlafha" || manifest.status !== "audited"
    || manifest.auditScope !== "entity-specific") {
  failures.push("overseer entity-specific audit manifest is invalid");
}

const pure = current("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const projectile = current("src/main/java/alku/csrp/entity/ParasiteProjectileEntity.java");
const biomass = current("src/main/java/alku/csrp/entity/BiomassEntity.java");
const config = current("src/main/java/alku/csrp/config/MobsConfig.java");
const model = current("src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java");
const projectileRenderer = current("src/main/java/alku/csrp/client/renderer/ParasiteProjectileRenderer.java");
const client = current("src/main/java/alku/csrp/client/ClientModEvents.java");
const entities = current("src/main/java/alku/csrp/registry/ModEntities.java");

const alafha = original("com/dhanantry/scapeandrunparasites/entity/monster/pure/EntityAlafha.java");
const ranged = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIAttackProjectile.java");
const melee = original("com/dhanantry/scapeandrunparasites/entity/ai/EntityAIAttackMeleeNotGround.java");
const ball = original("com/dhanantry/scapeandrunparasites/entity/projectile/EntityProjectileAlafhaBall.java");
const biomassProjectile = original("com/dhanantry/scapeandrunparasites/entity/projectile/EntityProjectileBiomass.java");
const originalConfig = original("com/dhanantry/scapeandrunparasites/util/config/SRPConfigMobs.java");
const attributes = original("com/dhanantry/scapeandrunparasites/util/SRPAttributes.java");
const originalRenderer = original("com/dhanantry/scapeandrunparasites/client/renderer/entity/pure/RenderAlafha.java");

expect(alafha, /func_70105_a\(1\.9F, 2\.6F\)/, "original Overseer dimensions changed");
expect(alafha, /new EntityBody\(this, 1\.2F, 1\.2F, 1\.0F, 3\.0F, 0\.0F, -1, 1, false, 0\.2F\)/,
  "original Overseer head part changed");
expect(alafha, /amount \* 3\.0F/, "original Overseer head damage multiplier changed");
expect(alafha, /func_70047_e\(\)[\s\S]{0,60}?return 1\.6F/, "original Overseer eye height changed");
expect(attributes, /ALAFHA_HEALTH\s*=\s*80\.0/, "original Overseer health changed");
expect(attributes, /ALAFHA_ARMOR\s*=\s*20\.0/, "original Overseer armor changed");
expect(attributes, /ALAFHA_ATTACK_DAMAGE\s*=\s*30\.0F/, "original Overseer projectile damage changed");
expect(attributes, /ALAFHA_KD_RESISTANCE\s*=\s*0\.4/, "original Overseer knockback resistance changed");
expect(attributes, /ALAFHA_MELLE\s*=\s*22\.0/, "original Overseer melee damage changed");
expect(alafha, /EntityAIAttackProjectile\(this, 20, 10, 4\)/, "original Overseer volley parameters changed");
expect(alafha, /EntityAIAttackMeleeNotGround\(this, 4\.5, 16\.0, 0\.045, true\)/,
  "original Overseer melee-rush parameters changed");
expect(ranged, /cooldown - 10[\s\S]*attackTimer > this\.cooldown[\s\S]*attackTimer % this\.tickInterval == 0/,
  "original projectile cadence changed");
expect(melee, /this\.currentCD = 80[\s\S]*this\.off = 4/, "original melee charge parameters changed");
expect(melee, /this\.cooldown > 140[\s\S]*this\.cooldown = 0/, "original melee reset cadence changed");
expect(originalConfig, /alafhaSummoningCooldown\s*=\s*10/, "original summon cooldown changed");
expect(originalConfig, /alafhaTotalActiveMobs\s*=\s*6/, "original summon capacity changed");
expect(originalConfig, /alafhaLimit\s*=\s*6/, "original per-cast summon limit changed");
expect(originalConfig, /srparasites:rupter;1;1[\s\S]*srparasites:grunt;0\.5;1/,
  "original Overseer summon table changed");
expect(ball, /func_72314_b\(3\.0, 3\.0, 3\.0\)/, "original Alafha-ball radius changed");
expect(ball, /applyStackPotion\(SRPPotions\.DLER_E, entitylivingbase, 300, 0\)/,
  "original Alafha-ball Needler effect changed");
expect(ball, /setWaitTime\(30\)[\s\S]*setDuration\(60\)[\s\S]*SRPPotions\.DLER_E, 360, 0/,
  "original Alafha-ball lingering cloud changed");
expect(biomassProjectile, /setFuse\(80\)[\s\S]*setSkin\(this\.kin\)/,
  "original biomass projectile payload changed");
expect(originalRenderer, /new ModelAlafha\(\), 1\.3F/, "original Overseer shadow radius changed");
expect(originalRenderer, /case 7:[\s\S]*TEXTUREH/, "original Overseer heavy texture mapping changed");

expect(entities, /monster\("overseer",[\s\S]{0,180}?Kind\.OVERSEER\), 1\.9F, 2\.6F\)/,
  "Overseer dimensions are not 1.9 x 2.6");
expect(pure, /OVERSEER\(true, false, 80\.0D, 20\.0D, 22\.0D, 0\.27D, 0\.40D/,
  "Overseer base attributes do not match EntityAlafha");
expect(pure, /case OVERSEER -> dimensions\.withEyeHeight\(1\.6F\)/,
  "Overseer eye height is not 1.6");
expect(pure, /new OverseerHeadPart\(this\)[\s\S]{0,100}?bodyParts = new PartEntity<\?>\[\]\{overseerHeadPart\}/,
  "Overseer head part is not installed");
const headPart = isolate(pure, "private static final class OverseerHeadPart",
  "private static final class VigilanteTendrilPart", "Overseer head part");
expect(headPart, /amount \* 3\.0F/, "Overseer head triple damage is missing");
expect(headPart, /scalable\(1\.2F, 1\.2F\)/, "Overseer head hitbox size is missing");
expect(pure, /class OverseerMoveControl[\s\S]{0,900}?0\.05D \* speedModifier/,
  "Overseer additive flight control is missing");
expect(pure, /new OverseerMeleeRushGoal\(\)[\s\S]{0,160}?new OverseerFlightLimitGoal\(\)[\s\S]{0,160}?new OverseerSummonGoal\(\)[\s\S]{0,160}?new OverseerRandomFlightGoal\(\)/,
  "Overseer goal priorities are incomplete");

const volleyGoal = isolate(pure, "private final class OverseerVolleyGoal",
  "private final class OverseerMeleeRushGoal", "Overseer volley goal");
expect(volleyGoal, /distanceToSqr\(target\) < 4225\.0D/, "Overseer volley range is not 65 blocks");
expect(volleyGoal, /attackTimer == 10[\s\S]*alafha\.shootingpost/, "Overseer pre-shot sound is missing");
expect(volleyGoal, /shots < 4[\s\S]*attackTimer % 10 == 0[\s\S]*fireOverseerProjectile/,
  "Overseer four-shot cadence is missing");
if (/Mode\.NEEDLE/.test(volleyGoal)) failures.push("Overseer volley still launches Needle projectiles");
expect(pure, /configureLegacyFireball\(this, ParasiteProjectileEntity\.Mode\.ALAFHA_BALL[\s\S]{0,120}?overseerProjectileDamage\(\), 3\.0D, 140/,
  "Overseer volley does not launch the original Alafha ball");

const meleeGoal = isolate(pure, "private final class OverseerMeleeRushGoal",
  "private final class OverseerFlightLimitGoal", "Overseer melee goal");
expect(meleeGoal, /chargeTicks < 80/, "Overseer melee charge is not 80 ticks");
expect(meleeGoal, /distanceToSqr\(target\) \* 0\.85D < 256\.0D/,
  "Overseer melee-rush tracking range is missing");
expect(meleeGoal, /deltaX \/ horizontal \* 0\.045D[\s\S]*deltaZ \/ horizontal \* 0\.045D/,
  "Overseer horizontal melee-rush impulse is missing");
expect(meleeGoal, /target\.getY\(\) >= getY\(\) \+ 4\.0D \? 0\.52D : -0\.2D/,
  "Overseer vertical melee-rush impulse is missing");
expect(meleeGoal, /attackDistance <= 20\.25D[\s\S]*attackCooldown = 20/,
  "Overseer melee reach or cadence is missing");
expect(meleeGoal, /chargeTicks > 140[\s\S]*chargeTicks = 0/, "Overseer melee reset is missing");

const summonGoal = isolate(pure, "private final class OverseerSummonGoal",
  "private final class VigilanteRangedGoal", "Overseer summon goal");
expect(summonGoal, /castingTicks % 20 == 0 && castingTicks >= 40/,
  "Overseer summon attempts do not begin at tick 40 every 20 ticks");
expect(summonGoal, /castingTicks >= 80 \|\| successfulLaunches >= MobsConfig\.overseerSummonLimit\(\)/,
  "Overseer summon cast does not end at 80 ticks or its configured limit");
expect(summonGoal, /distanceToSqr\(target\) < 256\.0D && target\.onGround\(\)/,
  "Overseer summon target conditions changed");
expect(pure, /configureBiomassBall\(this, start, acceleration, option, 4, target\)/,
  "Overseer does not launch skin-4 biomass projectiles");
expect(pure, /summonTracker\.prune\(serverLevel\)/, "Overseer summon-capacity pruning is missing");
expect(pure, /summonTracker\.save\(tag, "OverseerTrackedSummons"\)/,
  "Overseer summon-capacity NBT is missing");
expect(biomass, /spawnFromProjectile[\s\S]{0,500}?spawnBiomass\(level, summoner, owner, reservationId/,
  "Biomass projectile does not hand off its reservation");
expect(biomass, /replaceTrackedSummon\(reservationId, biomass\.getUUID\(\), option\.cost\(\)\)/,
  "Biomass projectile reservation transfer is missing");

expect(projectile, /impactAlafhaBall[\s\S]{0,500}?inflate\(effectRadius\)[\s\S]{0,220}?ModMobEffects\.NEEDLER, 300, 0/,
  "Alafha ball does not apply radius-three stacked Needler");
expect(projectile, /double effectRadius = 3\.0D[\s\S]{0,180}?inflate\(effectRadius\)/,
  "Alafha ball effect radius is not fixed at three blocks");
expect(projectile, /target\.hurt\(damageSources\(\)\.indirectMagic\(this, owner\), MobsConfig\.overseerProjectileDamage\(\)\)/,
  "Alafha ball damage does not use the configured 30-point projectile damage");
expect(projectile, /SoundEvents\.GENERIC_EXPLODE\.value\(\)[\s\S]{0,100}?SoundSource\.BLOCKS[\s\S]{0,120}?spawnLingeringAlafhaCloud\(owner\)/,
  "Alafha ball impact sound does not match the original block sound channel");
expect(projectile, /spawnLingeringAlafhaCloud[\s\S]{0,900}?setRadius\(2\.0F\)[\s\S]*setRadiusOnUse\(-0\.5F\)[\s\S]*setWaitTime\(30\)[\s\S]*ModMobEffects\.NEEDLER, 360, 0/,
  "Alafha ball lingering Needler cloud is incomplete");
expect(projectile, /case ALAFHA_BALL, BIOMASS_BALL -> ParticleTypes\.POOF/,
  "Alafha and biomass projectiles do not use the original POOF flight particles");
const alafhaCloud = isolate(projectile, "private void spawnLingeringAlafhaCloud",
  "private void spawnLingeringWitherCloud", "Alafha lingering cloud branches");
expect(alafhaCloud, /if \(owner instanceof DraconiteEntity\)[\s\S]{0,400}?cloud\.setOwner\(owner\)[\s\S]{0,500}?spawnOrbBoom\(owner, 15, 1\)/,
  "Draconite Alafha cloud does not retain its owner and orb follow-up");
const normalAlafhaCloud = isolate(alafhaCloud, "        cloud.setRadius(2.0F);",
  "        level().addFreshEntity(cloud);", "normal Alafha lingering cloud");
if (/setOwner\(owner\)/.test(normalAlafhaCloud)) {
  failures.push("normal Alafha lingering cloud should not assign an owner");
}
expect(projectile, /owner instanceof DraconiteEntity[\s\S]{0,400}?setRadius\(5\.0F\)[\s\S]*ModMobEffects\.COTH, 300, 0/,
  "Draconite Alafha-ball branch was not preserved");
expect(config, /"srparasites:rupter;1;1", "srparasites:grunt;0\.5;1"/,
  "Overseer default summon table is missing");
expect(config, /return OVERSEER_SUMMON_COOLDOWN\.get\(\);/,
  "Overseer summon cooldown no longer matches the 1.10.8 raw-tick runtime");
expect(pure, /OVERSEER_SKIN[\s\S]*EntityDataSerializers\.BYTE/, "Overseer skin is not synchronized");
expect(model, /getOverseerSkin\(\) == 7 \? OVERSEER_HEAVY_TEXTURE : OVERSEER_TEXTURE/,
  "Overseer heavy texture selection is missing");
expect(client, /ModEntities\.OVERSEER[\s\S]{0,120}?"overseer", 1\.3F/,
  "Overseer shadow radius is not 1.3");
expect(projectileRenderer, /case ALAFHA_BALL -> ALAFHA_TEXTURE[\s\S]*case BIOMASS_BALL -> BIOMASS_TEXTURE/,
  "Overseer projectile textures are not selected");
for (const texture of ["alafha.png", "biomass.png"]) {
  expectFile(`src/main/resources/assets/csrp/textures/entity/projectile/${texture}`);
}
expectFile("src/main/resources/assets/csrp/textures/entity/monster/alafhah.png");

if (failures.length) {
  console.error(`Overseer port verification failed (${failures.length} checks):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Overseer -> EntityAlafha entity-specific behavior audit passed.");
console.log(`Original sources: ${originalRoot}`);
