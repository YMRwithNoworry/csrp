const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (file) => fs.readFileSync(path.join(root, file), "utf8");
const abomination = read("src/main/java/alku/csrp/entity/AbominationEntity.java");
const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");
const effects = read("src/main/java/alku/csrp/event/StatusEffectEvents.java");
const evolution = read("src/main/java/alku/csrp/world/EvolutionSystem.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const failures = [];

const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

expect(effects, /private static final Map<LivingEntity, LivingEntity> ROOTER_OWNERS/,
  "Parasites do not retain their Rooter owner");
expect(effects, /rooter\.hurt\(event\.getSource\(\), transferred\)/,
  "Pivot damage is not forwarded to the Rooter");
expect(effects, /event\.setAmount\(event\.getAmount\(\) \* 0\.05F\)/,
  "Pivot damage does not leave the original 5% damage on the linked parasite");
expect(effects, /0\.2375F/,
  "Pivot does not use the original Rooter damage ratio");
expect(effects, /0\.5D \* \(parate\.getAmplifier\(\) \+ 1\)/,
  "Parate kill growth multiplier is missing");
expect(effects, /Attributes\.MAX_HEALTH/,
  "Parate does not increase maximum health");
expect(abomination, /private int supportCooldown = 10;/,
  "Abomination support does not use the original initial timing");
expect(abomination, /supportCooldown = 240;/,
  "Abomination support does not use the original 12 second cooldown");
expect(abomination, /getBoundingBox\(\)\.inflate\(20\.0D\)/,
  "Abomination support range is not the original 20 blocks");
expect(abomination, /KNOCKBACK_RESISTANCE, 0\.6D/,
  "Abomination knockback resistance is not the original 0.6");
expect(abomination, /\? 4 : 1/,
  "Many Bodies is missing its original four-hit damage cap");
expect(abomination, /BODIES\(0\.211037D, 24\)/,
  "Many Bodies experience reward is not the original 24");
expect(abomination, /HEAD\(0\.272037D, 75\)/,
  "Giant Head experience reward is not the original 75");
expect(abomination, /getTicksUntilNextAttack\(\)[\s\S]*?return 3;/,
  "Abomination attack interval is not the original three ticks");
expect(abomination, /getDeathSound\(\)[\s\S]*?bodies\.growl/,
  "Many Bodies death sound is not the original growl event");
expect(abomination, /ModMobEffects\.PIVOT/,
  "Abomination does not apply Pivot");
expect(abomination, /ModMobEffects\.PARATE/,
  "Abomination does not apply Parate");
expect(abomination, /StatusEffectEvents\.linkToRooter\(ally, this\)/,
  "Abomination does not link supported parasites to itself");
expect(nexus, /StatusEffectEvents\.linkToRooter\(ally, this\)/,
  "Nexus Rooter support does not link supported parasites to itself");
expect(effects, /abomination\.getKind\(\) == AbominationEntity\.Kind\.BODIES/,
  "Many Bodies is not immune to Pivot");
expect(evolution, /abomination\.getKind\(\) == AbominationEntity\.Kind\.BODIES\)[\s\S]*?return 3;/,
  "Many Bodies death does not remove the original three evolution points");
expect(client, /ModEntities\.ABO_HEAD\.get\(\), NoopRenderer::new/,
  "abo_head is rendered even though 1.10.7 never registers its renderer");

if (failures.length) {
  console.error("Abomination behavior contract verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Abomination behavior contract verification passed.");
