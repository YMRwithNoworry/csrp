const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

const wave = read("src/main/java/alku/csrp/entity/WaveEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModEntities.java");
const attributes = read("src/main/java/alku/csrp/registry/CommonModEvents.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const host = read("src/main/java/alku/csrp/entity/AbstractHostEntity.java");
const deterrent = read("src/main/java/alku/csrp/entity/DeterrentParasiteEntity.java");
const legacy = read("src/main/java/alku/csrp/entity/LegacyAuxiliaryEntity.java");

expect(registry, /EntityType<WaveEntity>> WAVE[\s\S]*?sized\(1\.5F, 0\.2F\)/,
  "wave is not registered with its original dedicated type and dimensions");
expect(attributes, /ModEntities\.WAVE\.get\(\), WaveEntity\.createAttributes\(\)\.build\(\)/,
  "wave attributes are not registered");
expect(client, /ModEntities\.WAVE\.get\(\), NoopRenderer::new/,
  "wave does not preserve the original empty-model renderer");
expect(wave, /class WaveEntity extends PathfinderMob implements Parasite/,
  "wave is not an independently navigating parasite");
expect(wave, /Attributes\.MOVEMENT_SPEED, 0\.45D/, "wave movement speed is not original");
expect(wave, /new MeleeAttackGoal\(this, 1\.0D, false\)/, "wave target navigation is missing");
expect(wave, /tickCount > 40[\s\S]*?getX\(\) == xo \|\| getZ\(\) == zo[\s\S]*?20 \* durationSeconds/,
  "wave stuck and duration removal rules are missing");
expect(wave, /getFluidState\(blockPosition\(\)\)\.isEmpty\(\)/,
  "wave does not disappear in liquid");
expect(wave, /inflate\(0\.4D, 0\.2D, 0\.4D\)/, "wave damage bounds are not original");
expect(wave, /viral\.getAmplifier\(\) \+ 1/, "wave minimum damage does not scale with Viral");
expect(wave, /target\.getAbsorptionAmount\(\), amount \* 0\.5F/,
  "wave minimum damage does not preserve absorption splitting");
expect(wave, /for \(int index = 0; index < 15; index\+\+\)[\s\S]*?BlockParticleOption/,
  "wave does not emit the original 15 ground debris particles per tick");
expect(wave, /boolean hurt\(DamageSource source, float amount\)[\s\S]*?return false;/,
  "wave is not invulnerable");
expect(wave, /boolean canBeAffected\(MobEffectInstance effect\)[\s\S]*?return false;/,
  "wave can still receive potion effects");
expect(wave, /causeFallDamage[\s\S]*?discard\(\)/, "wave does not disappear on a fall");

expect(host, /performShockwave\(\)[\s\S]*?ModEntities\.WAVE[\s\S]*?Config\.primitiveMinimumDamage\(\), 1, 60, target/,
  "Host and Hostii do not spawn the original long-duration wave");
expect(host, /chargeTicks == 20[\s\S]*?performShockwave\(\)/,
  "Host wave release timing is missing");
expect(deterrent, /chargeTicks == 40 \|\| chargeTicks == 80/,
  "Kyphosis does not release both original waves");
expect(deterrent, /Config\.primitiveMinimumDamage\(\), 1, 12, target/,
  "Kyphosis does not use the original short wave duration");
expect(deterrent, /level\(\)\.noCollision\(wave\)/,
  "Kyphosis does not cancel a blocked wave spawn");
if (/\bWAVE\b/.test(legacy)) failures.push("LegacyAuxiliaryEntity still carries the wave id");

if (failures.length) {
  console.error("Original wave behavior verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Original moving wave behavior and callers are restored.");
