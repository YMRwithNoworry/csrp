const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];

function read(relativePath) {
    const file = path.join(root, relativePath);
    if (!fs.existsSync(file)) {
        failures.push(`missing ${relativePath}`);
        return "";
    }
    return fs.readFileSync(file, "utf8");
}

function expect(content, pattern, description) {
    if (!pattern.test(content)) failures.push(description);
}

function reject(content, pattern, description) {
    if (pattern.test(content)) failures.push(description);
}

const config = read("src/main/java/alku/csrp/Config.java");
const mobsConfig = read("src/main/java/alku/csrp/config/MobsConfig.java");
const helper = read("src/main/java/alku/csrp/entity/MobAttacking.java");
const architect = read("src/main/java/alku/csrp/entity/ArchitectEntity.java");
const dredge = read("src/main/java/alku/csrp/entity/DredgeEntity.java");
const heed = read("src/main/java/alku/csrp/entity/HeedEntity.java");
const pure = read("src/main/java/alku/csrp/entity/PureParasiteEntity.java");
const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");

// ---- R6 gap: SRPConfig.mobAttackingFull (out109 util/config/SRPConfig.java:65, default false) ----
// Original EntityParasiteBase#canAttackClass rejects a blacklisted mob only when mobAttackingFull is false.
expect(config, /\.define\("mobAttackingFull", false\)/,
        "mobAttackingFull is missing or no longer defaults to false");
expect(config, /public static boolean mobAttackingFull\(\)/,
        "the mobAttackingFull accessor is missing");

// The shared helper must reproduce ParasiteEventEntity#checkName's contains-based match and
// the srparasites-namespace self-exclusion.
expect(helper, /potentialElement\.contains\(entry\)/,
        "MobAttacking.checkName no longer reproduces the original contains() match");
expect(helper, /return anyMatch != isWhitelist;/,
        "MobAttacking.checkName no longer reproduces the original inverted-list comparison");
expect(helper, /SRP_NAMESPACE\.equals\(id\.getNamespace\(\)\)/,
        "MobAttacking no longer excludes this mod's own entities");
expect(helper, /if \(!Config\.mobAttackingFull\(\)\) \{\s*return true;\s*\}/s,
        "MobAttacking no longer honours mobAttackingFull");

// Every family that used to hand-roll the blacklist must now defer to the shared helper so the
// full-friendly switch applies uniformly, as it did through the single original base method.
for (const [name, source] of [["ArchitectEntity", architect], ["DredgeEntity", dredge],
        ["HeedEntity", heed], ["PureParasiteEntity", pure]]) {
    expect(source, /MobAttacking\.canAttackEntity\(/,
            `${name} no longer routes mob targeting through MobAttacking`);
}

// ---- R6 gap: EntityAIVenkrolSummon mob tables (out109 util/config/SRPConfigMobs.java:1068-1150) ----
// Original venkrolmoblist = {"ground;srparasites:rupter;1;1"}.
expect(mobsConfig, /"ground;csrp:rupter;1;1"/,
        "the stage-1 beckon table no longer matches venkrolmoblist");
// Original venkrolsiimoblist carried 12 rows including the 0.3 air carrier_flying row.
expect(mobsConfig, /"air;csrp:carrier_flying;0\.3;3"/,
        "the stage-2 beckon table no longer matches venkrolsiimoblist");
expect(mobsConfig, /"ground;csrp:pri_manducater;0\.4;6"/,
        "the stage-3 beckon table no longer matches venkrolsiiimoblist");
expect(mobsConfig, /"air;csrp:pri_yelloweye;0\.5;6"/,
        "the stage-3 beckon air row (pri_yelloweye 0.5;6) is missing");
// Original venkrolsivmoblist = beckon_si 0.3;3, beckon_sii 0.4;3, beckon_siii 0.3;4.
expect(mobsConfig, /"ground;csrp:beckon_si;0\.3;3"/,
        "the stage-4 beckon table no longer matches venkrolsivmoblist");
expect(mobsConfig, /"ground;csrp:beckon_siii;0\.3;4"/,
        "the stage-4 beckon_siii row (0.3;4) is missing");
// Original venkrolCAMinimumV = 4 and venkrolCAExtraM = 0.5F.
expect(mobsConfig, /"venkrolCAMinimumV", 4, 0, 10000/,
        "venkrolCAMinimumV default of 4 is missing");
expect(mobsConfig, /"venkrolCAExtraM", 0\.5D, 0\.0D, 100\.0D/,
        "venkrolCAExtraM default of 0.5 is missing");
// The four-field ground|air parser and the stage dispatcher.
expect(mobsConfig, /beckonSummonMobs\(int stage,\s*boolean airborne\)/s,
        "the beckon table parser is missing");
expect(mobsConfig, /case 1 -> BECKON_STAGE1_MOBS\.get\(\);/,
        "the beckon stage dispatcher no longer maps stage 1");
expect(mobsConfig, /default -> BECKON_STAGE4_MOBS\.get\(\);/,
        "the beckon stage dispatcher no longer maps stage 4");

// The summon site must consume the configured table instead of a hard-coded switch, and must
// pick the air table when the target is at least three blocks above the beckon
// (original: entityin.getY() + 3 <= target.getY()).
expect(nexus, /MobsConfig\.beckonSummonMobs\(activeKind\.stage, airborne\)/,
        "summonBeckonParasites no longer reads the configured beckon table");
expect(nexus, /boolean airborne = getY\(\) \+ 3\.0D <= target\.getY\(\);/,
        "the beckon ground/air selection no longer matches the original height test");
expect(nexus, /MobsConfig\.beckonSummonMobs\(4, false\)/,
        "the stage-4 beckon re-summon no longer reads the configured table");
reject(nexus, /roll < 0\.30F \? 1 : roll < 0\.70F \? 2 : 3/,
        "the hard-coded stage-4 beckon roll is still present");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("mob attacking + beckon summon table contract ok");
