#!/usr/bin/env node
/**
 * Verifies that the parasite "Dislodgment" (寄群扰动) system matches the original
 * SRParasites 1.10.9 implementation and the codes listed on the wiki page
 * https://www.mcmod.cn/item/951516.html.
 *
 * Ground truth: D:\code\MC模组\_srp-orig\decomp-1.10.9\...\util\config\SRPConfigSystems.java
 *               (parasite_dislodgment + parasite_dislodgment_000..025)
 *               ...\util\SRPAttributes.java (trigger lists), ...\util\ParasiteEventWorld.java
 *               ...\util\handlers\SRPEventHandlerBus.java (per-code application)
 * Every default asserted below is copied verbatim from that source.
 */
'use strict';

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const failures = [];
const read = (file) => {
    const full = path.join(root, file);
    if (!fs.existsSync(full)) {
        failures.push(`missing ${file}`);
        return '';
    }
    return fs.readFileSync(full, 'utf8');
};
const expect = (text, pattern, message) => {
    if (!pattern.test(text)) failures.push(message);
};

const config = read('src/main/java/alku/csrp/Config.java');
const system = read('src/main/java/alku/csrp/world/DislodgmentSystem.java');
const data = read('src/main/java/alku/csrp/world/SrpWorldData.java');
const tables = read('src/main/java/alku/csrp/world/NaturalSpawnTables.java');
const events = read('src/main/java/alku/csrp/world/EvolutionEvents.java');
const parasite = read('src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java');
const head = read('src/main/java/alku/csrp/entity/AssimilatedHeadEntity.java');
const remain = read('src/main/java/alku/csrp/entity/RemainEntity.java');

/* ------------------------------------------------------------------ *
 * 1. Original "parasite_dislodgment" category.
 * ------------------------------------------------------------------ */
expect(config, /defineInRange\("dislodgmentGlobalCooldown", 200,/, 'original disloGlobalCooldown 200 is missing');
expect(config, /defineInRange\("dislodgmentCothSpy", 4,/, 'original disloCOTHSpy 4 is missing');
expect(config, /defineInRange\("dislodgmentUnlockPhase", 11,/, 'original evolutionDislodgment unlock phase 11 is missing');
expect(config, /defineInRange\("dislodgmentUnlockDevelopment", 1,/, 'original deveDisloUse 1 is missing');
expect(system, /public static boolean dislodgmentUnlocked\(ServerLevel level\)/,
    'the original dislodgment unlock gate is missing (ParasiteEventWorld#setDisloWorldPhase)');
expect(system, /Config\.dislodgmentUnlockDevelopment\(\)/,
    'the unlock gate never consults the ubiquitous development level');

const triggerChances = [
    ['dislodgmentRightClickTriggerChance', '0.01D'],
    ['dislodgmentXpPickupTriggerChance', '0.03D'],
    ['dislodgmentItemPickupTriggerChance', '0.03D'],
    ['dislodgmentHealingTriggerChance', '0.001D'],
    ['dislodgmentUseItemTriggerChance', '0.01D'],
    ['dislodgmentMenuCloseTriggerChance', '0.001D'],
    ['dislodgmentDeathTriggerChance', '0.001D'],
    ['dislodgmentBlockBreakTriggerChance', '0.1D']
];
for (const [key, value] of triggerChances) {
    expect(config, new RegExp(`defineInRange\\(\"${key}\", ${value},`),
        `original trigger chance "${key}" default ${value} is missing`);
}
const nexusChances = [
    ['dislodgmentNexusOneTriggerChance', '0.05D'],
    ['dislodgmentNexusTwoTriggerChance', '0.06D'],
    ['dislodgmentNexusThreeTriggerChance', '0.07D'],
    ['dislodgmentNexusFourTriggerChance', '0.1D']
];
for (const [key, value] of nexusChances) {
    expect(config, new RegExp(`defineInRange\\(\"${key}\", ${value},`),
        `original nexus trigger chance "${key}" default ${value} is missing`);
}

/* ------------------------------------------------------------------ *
 * 2. All 26 wiki codes exist as configurable codes with the original knobs.
 * ------------------------------------------------------------------ */
const TRIGGERS_ALL = '0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 16, 17, 18';
const CODES = [
    // [code, wiki meaning, key, enabled, price, value, duration, cooldown, trigger list]
    [0, 'COTH ignores the potion level and converts directly', 'CothIgnoreAmplifier', 'true', 100, null, 60, 240, '1, 10, 14, 16'],
    [1, 'COTH converts into a higher tier', 'CothTiers', 'true', 200, 1, 40, 240, '12, 13, 14, 15, 16'],
    [2, 'killing a parasite spawns a specific parasite', 'SummonByDeath', 'true', 200, 1, 60, 200, '10, 15, 16'],
    [3, 'parasites spawn with potion effects', 'PotionEffect', 'true', 200, 1, 120, 300, '4, 13, 14, 15, 16'],
    [4, 'parasites spawn with extra attribute bonuses', 'Stats', 'true', 1000, 2, 60, 300, '14, 15, 17, 18'],
    [5, 'no natural spawn list while active', 'DeathRaid', 'true', 10, 10, 10, 10, TRIGGERS_ALL],
    [6, 'parasites consume far more durability', 'ItemDurability', 'true', 100, 2, 120, 240, '4, 12, 13, 16'],
    [7, 'parasite death heals nearby parasites', 'HealingDeath', 'true', 500, 100, 40, 240, '1, 3, 10, 12, 16'],
    [8, 'parasite death damages nearby non-parasites', 'DamageDeath', 'true', 500, 10, 60, 300, '0, 5, 13, 16'],
    [9, 'parasite death drains nearby player hunger', 'FoodDeath', 'true', 500, 100, 60, 240, '3, 12, 13, 16'],
    [10, 'killing a low tier spawns a higher one', 'DeathHighVersions', 'true', 300, 1, 120, 360, TRIGGERS_ALL],
    [11, 'parasites are immune to negative effects', 'ParasiteNoPotion', 'true', 100, null, 60, 240, '3, 4, 16'],
    [12, 'non-parasites periodically lose health', 'HealthDraining', 'true', 50000, 10, 3, 300, '14, 15, 17, 18'],
    [13, 'players periodically lose hunger', 'FoodDraining', 'true', 500, 200, 3, 300, '12, 13, 14, 15, 17, 18'],
    [14, 'the next phase\'s spawn list is used', 'NextPhaseList', 'true', 50000, 1, 30, 240, '15, 16, 17, 18'],
    [15, 'parasites make no howl sounds', 'GrowlNoise', 'true', 100, null, 60, 240, '0, 4, 10, 11'],
    [16, 'parasites make no step sounds', 'WalkNoise', 'true', 100, null, 60, 240, '0, 5, 10, 11'],
    [17, 'shields are disabled and food is destroyed', 'ShieldFood', 'true', 180, null, 400, 550, TRIGGERS_ALL],
    [18, 'parasites drop no loot or experience', 'LootXpCancel', 'true', 100, null, 60, 240, '2, 10, 16'],
    [19, 'the parasite kill count grows periodically', 'KillcountInc', 'true', 100, 10, 60, 240, '1, 10, 16'],
    [20, 'specific parasites gain a new body', 'GiveBodies', 'true', 150, null, 300, 150, TRIGGERS_ALL],
    [21, 'parasites only die while burning', 'BurningDeath', 'true', 50000, null, 60, 180, TRIGGERS_ALL],
    [22, 'killing specific tiers weakens the killer', 'SameVersionDyeing', 'true', 100, 1, 300, 60, TRIGGERS_ALL],
    [25, 'breaking parasite blocks spawns parasites', 'ParasiteBlock', 'true', 10, 1, 10, 10, TRIGGERS_ALL]
];

for (const [code, meaning, key, enabled, price, value, duration, cooldown] of CODES) {
    const lower = key.charAt(0).toLowerCase() + key.slice(1);
    expect(config, new RegExp(`define\\(\"dislo${key}\"`),
        `dislodgment code ${code} (${meaning}) is not configurable`);
    expect(config, new RegExp(`\"dislo${key}PointCost\"`),
        `dislodgment code ${code} (${meaning}) has no point cost knob`);
    expect(config, new RegExp(`\"dislo${key}Duration\"`),
        `dislodgment code ${code} (${meaning}) has no duration knob`);
    expect(config, new RegExp(`\"dislo${key}Triggers\"`),
        `dislodgment code ${code} (${meaning}) has no trigger list`);
    expect(config, new RegExp(`\"dislo${key}Cooldown\"`),
        `dislodgment code ${code} (${meaning}) has no cooldown knob`);
    expect(config, new RegExp(`case ${code} -> DISLO_${key.replace(/([a-z])([A-Z])/g, '$1_$2').toUpperCase()}_TRIGGERS`),
        `dislodgmentTriggers() does not expose code ${code} (${meaning})`);
    expect(config, new RegExp(`case ${code} -> DISLO_${key.replace(/([a-z])([A-Z])/g, '$1_$2').toUpperCase()}_COOLDOWN`),
        `dislodgmentCodeCooldown() does not expose code ${code} (${meaning})`);
    expect(system, new RegExp('ActivationRule\\(' + code + ','),
        `dislodgment code ${code} (${meaning}) can never activate`);
}

// Codes 23 and 24 are "no effect" on the wiki; the original still ships their knobs, so only the
// two documented-inert codes may be absent from the activation table.
expect(config, /disloParasiteBlockValue1\", 9,/, 'original disloParasiteBlockValue1 default 9 is missing');
expect(config, /disloParasiteBlockValue2\", 15,/, 'original disloParasiteBlockValue2 default 15 is missing');
expect(config, /disloParasiteBlockValue3\", 21,/, 'original disloParasiteBlockValue3 default 21 is missing');
expect(config, /disloParasiteBlockChance\", 0\.2D,/, 'original disloParasiteBlockChance default 0.2 is missing');
expect(config, /disloCothTiersPrimitive\", 9,/, 'original disloCOTHTiersValue1 default 9 is missing');
expect(config, /disloCothTiersAdapted\", 15,/, 'original disloCOTHTiersValue2 default 15 is missing');
expect(config, /disloCothTiersPure\", 21,/, 'original disloCOTHTiersValue3 default 21 is missing');
expect(config, /disloSummonByDeathKilling\", 5,/, 'original disloSummonByDeathKilling default 5 is missing');
expect(config, /disloRemainPlus\", 3,/, 'original canraadaptedremainplus default 3 is missing');
expect(config, /disloRemainHealth\", 1\.5D,/, 'original canraadaptedremainhealth default 1.5 is missing');

/* ------------------------------------------------------------------ *
 * 3. Original activation maths: value, duration and cost scale with the phase.
 * ------------------------------------------------------------------ */
expect(system, /PHASE_COST_MULTIPLIER = \{0, 1, 3, 6, 8, 10, 13, 17, 20, 25, 30\}/,
    'the original per-phase point cost multipliers are missing');
expect(system, /int value = saturatingMultiply\(rule\.value\(\), phase\)/,
    'an active code value must be its base value scaled by the phase');
expect(system, /int duration = saturatingMultiply\(rule\.durationSeconds\(\), phase\)/,
    'an active code duration must be its base duration scaled by the phase');
expect(system, /int cost = saturatingMultiply\(rule\.pointCost\(\), PHASE_COST_MULTIPLIER\[phase\]\)/,
    'an active code cost must be its base price scaled by the phase cost multiplier');
expect(data, /dislodgmentCooldownEnds\[code\]/,
    'per-code cooldowns are not tracked');
expect(data, /Config\.dislodgmentCodeCooldown\(code\)/,
    'per-code cooldown seconds are not read from the config');

/* ------------------------------------------------------------------ *
 * 4. Per-code consumers.
 * ------------------------------------------------------------------ */
expect(system, /activeValue\(SrpWorldData\.get\(level\), 6\)[\s\S]{0,1500}damageItem\(player, EquipmentSlot\.MAINHAND/,
    'code 6 does not consume durability from the attacking player');
expect(system, /activeValue\(SrpWorldData\.get\(level\), 21\) < 1/,
    'code 21 does not gate parasite damage while unburned');
expect(system, /activeValue\(SrpWorldData\.get\(level\), 11\) < 1/,
    'code 11 does not reject harmful effects');
expect(system, /path\.endsWith\("\.growl"\)/,
    'code 15 does not suppress howl sounds');
expect(system, /path\.endsWith\("\.step"\)/,
    'code 16 does not suppress step sounds');
expect(system, /activeValue\(SrpWorldData\.get\(level\), 17\) < 1/,
    'code 17 does not disable shields / corrupt food');
expect(system, /activeValue\(SrpWorldData\.get\(level\), 18\) > 0/,
    'code 18 does not cancel loot and experience');
expect(system, /dislodgmentCode\(3\)[\s\S]{0,400}Config\.disloPotionEffect\(\)/,
    'code 3 is not applied to spawning parasites');
expect(system, /dislodgmentCode\(4\)[\s\S]{0,400}Config\.disloStats\(\)/,
    'code 4 is not applied to spawning parasites');
expect(system, /private static void applySummonByDeath/,
    'code 2 has no death-side payload');
expect(system, /private static void applyHighVersionDeath/,
    'code 10 has no higher-version spawn');
expect(system, /private static void applyDeathAreaCodes/,
    'codes 7-9 have no death-area handler');

// Codes 5 and 14: the natural spawn list.
expect(system, /public static boolean naturalSpawningSuppressed\(ServerLevel level\)/,
    'code 5 has no natural-spawn suppression');
expect(system, /public static int naturalSpawnPhaseOffset\(ServerLevel level\)/,
    'code 14 has no spawn-phase offset');
expect(tables, /DislodgmentSystem\.naturalSpawningSuppressed\(level\)/,
    'the natural spawn list ignores code 5');
expect(tables, /phase \+= DislodgmentSystem\.naturalSpawnPhaseOffset\(level\)/,
    'the natural spawn list ignores code 14');

// Code 19: the parasite kill count grows every second.
expect(system, /public static int killCountIncrement\(ServerLevel level\)/,
    'code 19 has no kill-count increment accessor');
expect(parasite, /public void addDislodgmentKillCount\(int amount\)/,
    'the parasite has no way to grow its kill count for code 19');
expect(events, /addDislodgmentKillCount\(DislodgmentSystem\.killCountIncrement\(level\)\)/,
    'code 19 is never applied to living parasites');

// Code 20: bodies for heads and dormant remains.
expect(system, /public static boolean bodiesGranted\(ServerLevel level\)/,
    'code 20 has no accessor');
expect(head, /DislodgmentSystem\.bodiesGranted\(serverLevel\)/,
    'code 20 never rebuilds an infected head body');
expect(remain, /DislodgmentSystem\.bodiesGranted\(serverLevel\)/,
    'code 20 never wakes a dormant remain');

// Code 22: the killer is dyed when a marked tier dies.
expect(system, /private static void applySameVersionDyeing/,
    'code 22 has no killer-dyeing handler');
expect(system, /ModMobEffects\.FERAL\.get\(\), duration, amplifier/,
    'code 22 does not stack the feral mark on the killer');
expect(system, /removeEffect\(ModMobEffects\.PRIMITIVE\.get\(\)\)/,
    'code 22 does not clear the other tier marks');

// Code 25: parasite blocks rebuild parasites by value tier.
expect(system, /private static void spawnFromParasiteBlock/,
    'code 25 has no parasite-block spawn');
expect(system, /Config\.disloParasiteBlockValue3\(\) \? pureTypes\(\)/,
    'code 25 does not map its value onto the pure tier');
expect(system, /Config\.disloParasiteBlockValue2\(\) \? adaptedTypes\(\)/,
    'code 25 does not map its value onto the adapted tier');
expect(system, /Config\.disloParasiteBlockValue1\(\) \? primitiveTypes\(\)/,
    'code 25 does not map its value onto the primitive tier');
expect(system, /Config\.disloParasiteBlockChance\(\)/,
    'code 25 ignores the original spawn chance');
expect(system, /spawnFromParasiteBlock\(level, event\.getPos\(\), SrpWorldData\.get\(level\)\)/,
    'breaking a parasite block never runs code 25');

/* ------------------------------------------------------------------ *
 * 5. Trigger surface: every original trigger id has a call site.
 * ------------------------------------------------------------------ */
const triggerIds = [
    [0, 'onRightClickBlock'],
    [1, 'onExperiencePickup'],
    [2, 'onItemPickup'],
    [3, 'onPlayerHealing'],
    [4, 'onItemUseFinished'],
    [5, 'onContainerClosed'],
    [10, 'onParasiteDeath'],
    [11, 'onParasiteBlockBroken']
];
for (const [id, handler] of triggerIds) {
    expect(system, new RegExp(`public static void ${handler}`),
        `original trigger ${id} has no handler (${handler})`);
}
expect(system, /tryTrigger\(level, 11 \+ stage, Config\.dislodgmentNexusTriggerChance\(stage\)/,
    'the four nexus death triggers 12-15 are not wired');
expect(system, /tryTrigger\(level, 17, 1\.0D/,
    'original trigger 17 (node/colony) is not wired');
expect(system, /tryTrigger\(level, 18, 1\.0D/,
    'original trigger 18 (colony) is not wired');

if (failures.length) {
    console.error('Dislodgment (寄群扰动) port verification failed:');
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}
console.log('Dislodgment (寄群扰动) port verification passed.');
console.log(`  codes asserted: ${CODES.length} (+ codes 23/24 documented inert)`);
