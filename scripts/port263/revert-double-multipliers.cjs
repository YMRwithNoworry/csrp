// Reverts the double application introduced by the per-mob multiplier stacking rounds.
//
// The audit script originally only recognised accessors shaped `return CONST.get()`, so keys whose
// accessor already applies the multiplier (e.g. `bolsterHealth()` returns
// `35.0D * BOLSTER_HEALTH_MULTIPLIER.get()`) looked dead. The stacking edits therefore multiplied
// by the same multiplier a second time. This script removes the extra factor from the eight sites.
const fs = require("node:fs");
const path = require("node:path");

const dir = path.join(__dirname, "..", "..", "src", "main", "java", "alku", "csrp", "entity");

/** Applies a replacement that tolerates either LF or CRLF in the file. */
function replace(source, from, to) {
  const variants = [from, from.replace(/\n/g, "\r\n")];
  for (const variant of variants) {
    if (source.includes(variant)) {
      return source.split(variant).join(to.includes("\n") && variant.includes("\r\n")
        ? to.replace(/\n/g, "\r\n") : to);
    }
  }
  return null;
}

const cases = ["arachnida", "bolster", "burrower", "devourer", "manducater", "tozoon"];

let primitives = fs.readFileSync(path.join(dir, "PrimitiveVariantEntity.java"), "utf8");
let changed = 0;
for (const name of cases) {
  const stacked = [
    `                    MobsConfig.${name}Health() * MobsConfig.${name}HealthMultiplier(),`,
    `                    MobsConfig.${name}Armor() * MobsConfig.${name}ArmorMultiplier(),`,
    `                    MobsConfig.${name}Damage() * MobsConfig.${name}DamageMultiplier(),`,
    `                    Math.min(1.0D, MobsConfig.${name}KnockbackResistance()`,
    `                            * MobsConfig.${name}KnockbackMultiplier()));`
  ].join("\n");
  const plain = [
    `                    MobsConfig.${name}Health(), MobsConfig.${name}Armor(),`,
    `                    MobsConfig.${name}Damage(), MobsConfig.${name}KnockbackResistance());`
  ].join("\n");
  const updated = replace(primitives, stacked, plain);
  if (updated === null) {
    console.log(`  (no match) primitive ${name}`);
    continue;
  }
  primitives = updated;
  changed++;
  console.log(`  reverted primitive ${name}`);
}
fs.writeFileSync(path.join(dir, "PrimitiveVariantEntity.java"), primitives);

let adapted = fs.readFileSync(path.join(dir, "AdaptedVariantEntity.java"), "utf8");
const adaptedUpdated = replace(adapted,
  [
    "                    MobsConfig.adaptedArachnidaHealth() * MobsConfig.arachnidaHealthMultiplier(),",
    "                    MobsConfig.adaptedArachnidaArmor() * MobsConfig.arachnidaArmorMultiplier(),",
    "                    MobsConfig.adaptedArachnidaDamage() * MobsConfig.arachnidaDamageMultiplier(),",
    "                    Math.min(1.0D, MobsConfig.adaptedArachnidaKnockbackResistance()",
    "                            * MobsConfig.arachnidaKnockbackMultiplier()));"
  ].join("\n"),
  [
    "                    MobsConfig.adaptedArachnidaHealth(), MobsConfig.adaptedArachnidaArmor(),",
    "                    MobsConfig.adaptedArachnidaDamage(), MobsConfig.adaptedArachnidaKnockbackResistance());"
  ].join("\n"));
if (adaptedUpdated === null) {
  console.log("  (no match) adapted arachnida");
} else {
  fs.writeFileSync(path.join(dir, "AdaptedVariantEntity.java"), adaptedUpdated);
  changed++;
  console.log("  reverted adapted arachnida");
}

let preeminent = fs.readFileSync(path.join(dir, "PreeminentParasiteEntity.java"), "utf8");
const preeminentUpdated = replace(preeminent,
  [
    "        // Legacy SRPConfigMobs.jinjo* per-mob multipliers (default 1.0F in the original) apply to the",
    "        // heavy bomber only.",
    "        boolean bomber = kind == Kind.BOMBER_HEAVY;",
    "        double health = bomber ? MobsConfig.heavyBomberHealthMultiplier() : 1.0D;",
    "        double damage = bomber ? MobsConfig.heavyBomberDamageMultiplier() : 1.0D;",
    "        double armor = bomber ? MobsConfig.heavyBomberArmorMultiplier() : 1.0D;",
    "        AttributeSupplier.Builder attributes = Mob.createMobAttributes()",
    "                .add(Attributes.MAX_HEALTH, kind.maxHealth * health)",
    "                .add(Attributes.ARMOR, kind.armor * armor)",
    "                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage * damage)"
  ].join("\n"),
  [
    "        AttributeSupplier.Builder attributes = Mob.createMobAttributes()",
    "                .add(Attributes.MAX_HEALTH, kind.maxHealth)",
    "                .add(Attributes.ARMOR, kind.armor)",
    "                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)"
  ].join("\n"));
if (preeminentUpdated === null) {
  console.log("  (no match) preeminent heavy bomber");
} else {
  fs.writeFileSync(path.join(dir, "PreeminentParasiteEntity.java"), preeminentUpdated);
  changed++;
  console.log("  reverted preeminent heavy bomber");
}

console.log(`reverted ${changed} site(s)`);
