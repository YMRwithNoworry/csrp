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

const config = read("src/main/java/alku/csrp/config/WorldConfig.java");
const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");
const bomb = read("src/main/java/alku/csrp/entity/BombEntity.java");
const particles = read("src/main/java/alku/csrp/particle/SrpParticleEffects.java");

expect(config, /defineInRange\("worldMobCap", 40, 0, 50000\)/,
        "worldMobCap default 40 is missing");
expect(config, /defineInRange\("worldMobCapPlusPlayer", 5, 0, 50000\)/,
        "worldMobCapPlusPlayer default 5 is missing");
expect(config, /defineInRange\("worldBeckonSpawnsCap", 20, 0, 50000\)/,
        "worldBeckonSpawnsCap default 20 is missing");
expect(config, /beckonSummonLimit\(ServerLevel level\)[\s\S]*?WORLD_MOB_CAP\.get\(\) \+ level\.players\(\)\.size\(\) \* WORLD_MOB_CAP_PLUS_PLAYER\.get\(\)[\s\S]*?\+ WORLD_BECKON_SPAWNS_CAP\.get\(\)/,
        "modern Beckon cap does not use the original formula");

const beckon = nexus.match(/private boolean summonBeckonParasites\(Kind activeKind, LivingEntity target\) \{([\s\S]*?)\n    private boolean hasBeckonWorldCapacity/);
if (!beckon) {
    failures.push("Beckon summon body is missing");
} else {
    expect(beckon[1], /activeKind\.stage == 4[\s\S]*?return spawnedCount > 0;[\s\S]*?boolean airborne = getY\(\) \+ 3\.0D <= target\.getY\(\)/,
            "stage 4 is not kept outside the original stage 1-3 fallback path");
    expect(beckon[1], /if \(activeKind\.stage == 1 && airborne\) \{\s*return false;/,
            "stage 1 does not reject the original target-above air case");
    expect(beckon[1], /if \(!hasBeckonWorldCapacity\(serverLevel\)\) \{[\s\S]*?summonCooldown = BECKON_CAPACITY_FALLBACK_INTERVAL;[\s\S]*?spawnBeckonCapacityBomb\(serverLevel, activeKind\.stage\);[\s\S]*?return false;/,
            "global-cap fallback does not retry on the original 20-tick pulse without counting as a biomass summon");
    if (/chosen\.chance\(\)/.test(beckon[1])) {
        failures.push("Beckon summon adds a chance roll absent from the original spawnBiomassFromBeckon path");
    }
}
expect(nexus, /private boolean hasBeckonWorldCapacity\(ServerLevel serverLevel\)[\s\S]*?serverLevel\.getAllEntities\(\)[\s\S]*?entity instanceof Parasite[\s\S]*?parasiteCount < WorldConfig\.beckonSummonLimit\(serverLevel\)/,
        "global cap does not count loaded parasites in the current world");

const fallback = nexus.match(/private void spawnBeckonCapacityBomb\(ServerLevel serverLevel, int stage\) \{([\s\S]*?)\n    private boolean forceEvolveNearbyParasite/);
if (!fallback) {
    failures.push("dedicated Beckon capacity bomb fallback is missing");
} else {
    expect(fallback[1], /if \(serverLevel\.getRandom\(\)\.nextInt\(\) < 40\) \{\s*return;/,
            "the original signed nextInt() < 40 fallback gate is missing");
    expect(fallback[1], /bomb\.configure\(this, 60, 0\.0F,[\s\S]*?, 2, 1, false\)/,
            "fallback bomb fuse, strength, damage, radius, skin or grief setting differs");
    expect(fallback[1], /stage == 3 \? 0\.5D : 0\.0D/,
            "stage 3 fallback bomb vertical offset is missing");
    expect(fallback[1], /strength = 0\.5D \/ \(distance \/ 4\.0D \+ 0\.1D\)[\s\S]*?stage == 3 \? 5\.0D : 3\.0D[\s\S]*?setBeckonMotion\(dx \* strength \* horizontalScale, dy \* strength \* 2\.0D,\s*strength \* horizontalScale, 0\.4D, 0\.5D\)/,
            "fallback bomb trajectory does not match the original equations and caps");
    expect(fallback[1], /if \(!serverLevel\.addFreshEntity\(bomb\)\) \{\s*return;\s*\}[\s\S]*?SrpParticleEffects\.spawnType10\(serverLevel, getX\(\), getY\(\), getZ\(\)\)/,
            "type 10 is not emitted only after the fallback bomb enters the world");
}
expect(bomb, /setBeckonMotion\(double xSpeed, double ySpeed, double zSpeed, double capX, double capY\)[\s\S]*?Math\.min\(xSpeed, capX\)[\s\S]*?Math\.min\(ySpeed, capY\)[\s\S]*?Math\.min\(zSpeed, capX\)[\s\S]*?random\.nextDouble\(\) \* 2\.0D - 1\.0D/,
        "BombEntity does not preserve the original capped Beckon motion semantics");
expect(particles, /spawnType10\(ServerLevel level, double x, double y, double z\)[\s\S]*?send\(level, x, y, z, 0\.5F, 0\.5F, 10\)/,
        "the Beckon fallback type-10 packet endpoint is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("Beckon capacity fallback contract ok");
