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
const biomass = read("src/main/java/alku/csrp/entity/BiomassEntity.java");
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

const tick = nexus.match(/private void tickBeckonSummoning\(Kind activeKind\) \{([\s\S]*?)\n    private boolean summonBeckonPulse/);
if (!tick) {
    failures.push("Beckon pulse scheduler is missing");
} else {
    expect(tick[1], /beckonBatchCount < maximumBatch[\s\S]*?getUsedSummonCapacity\(\) < getSummonCapacity\(\)[\s\S]*?summonBeckonPulse\(activeKind, target, false\)/,
            "ordinary Beckon pulses no longer respect the parent's summon-point capacity");
    expect(tick[1], /LivingEntity target = getTarget\(\);[\s\S]*?if \(target == null\) \{[\s\S]*?\}\s*if \(beckonLocked\) \{[\s\S]*?--beckonAttackTimer/,
            "Beckon lock timeout stops advancing while its target is absent");
    if (/getEntitiesOfClass|nearbyParasiteCount/.test(tick[1])) {
        failures.push("nearby-entity counting can suppress the original global-cap Bomb fallback");
    }
}

const beckon = biomass.match(/public static boolean spawnFromBeckon\(NexusParasiteEntity parent, LivingEntity target, int stage,[\s\S]*?\) \{([\s\S]*?)\n    private void setBeckonMotion/);
if (!beckon) {
    failures.push("Beckon Biomass summon body is missing");
} else {
    expect(beckon[1], /parent\.hasBeckonWorldCapacity\(level\)[\s\S]*?level\.addFreshEntity\(biomass\)[\s\S]*?parent\.reserveTrackedSummon\(biomass\.getUUID\(\), cost\)/,
            "stage 1-3 summons do not use the global cap and capacity tracker before recording success");
    expect(beckon[1], /parent\.spawnBeckonCapacityBomb\(level, stage\);\s*index\+\+;/,
            "global-cap fallback does not emit the original bomb and continue the table walk");
    expect(beckon[1], /parent\.getSummonCapacity\(\) - parent\.getUsedSummonCapacity\(\) < cost\)[\s\S]*?index\+\+;/,
            "per-Beckon point-cap check is missing");
    if (/option\.chance\(\)/.test(beckon[1])) {
        failures.push("Beckon Biomass summon adds a chance roll absent from the original spawnBiomassFromBeckon path");
    }
}
const worldCapacity = nexus.match(/boolean hasBeckonWorldCapacity\(ServerLevel serverLevel\) \{([\s\S]*?)\n    \}/);
if (!worldCapacity) {
    failures.push("global Beckon capacity counter is missing");
} else {
    expect(worldCapacity[1], /for \(Entity entity : serverLevel\.getAllEntities\(\)\) \{\s*if \(entity instanceof Parasite\) \{\s*parasiteCount\+\+;/,
            "global cap does not count every Parasite, including parasite waves");
    if (/instanceof WaveEntity/.test(worldCapacity[1])) {
        failures.push("global Beckon cap wrongly excludes parasite waves");
    }
}
expect(nexus, /BECKON_BURIED_EVENT = 50[\s\S]*?TEMPORARY_BECKON_LIFETIME = 300[\s\S]*?handleEntityEvent\(byte id\)[\s\S]*?id == BECKON_BURIED_EVENT[\s\S]*?setFloorTimer\(beckonBuriedDepth\(activeKind\.stage\)\)[\s\S]*?triggerAnim\("floor_controller", "beckon_buried"\)[\s\S]*?private void makeTemporaryBeckon\(\) \{\s*temporaryLifetimeTicks = TEMPORARY_BECKON_LIFETIME;\s*canGrow = false;/,
        "temporary Beckons do not preserve the original 300-tick, no-growth and buried client state");
expect(nexus, /triggerableAnim\("beckon_buried", BECKON_BURIED\)/,
        "temporary Beckon burial animation is not registered on the floor controller");
expect(nexus, /serverLevel\.addFreshEntity\(spawned\)\)[\s\S]*?broadcastEntityEvent\(spawned, BECKON_BURIED_EVENT\)/,
        "temporary Beckon burial status is not broadcast after the entity is added");
expect(nexus, /private static float beckonBuriedDepth\(int stage\)[\s\S]*?case 1 -> 2\.6F;\s*case 2 -> 4\.4F;\s*case 3 -> 5\.8F;/,
        "Beckon burial depths no longer match the original stage values");
expect(nexus, /private List<NexusParasiteEntity> nearbyBeckons\(int stage\) \{\s*AABB area = new AABB\(getX\(\), getY\(\), getZ\(\), getX\(\) \+ 1\.0D, getY\(\) \+ 1\.0D, getZ\(\) \+ 1\.0D\)\s*\.inflate\(getAttributeValue\(Attributes\.FOLLOW_RANGE\) \+ 5\.0D\)/,
        "Beckon group searches no longer use the original unit AABB and follow-range expansion");

const fallback = nexus.match(/void spawnBeckonCapacityBomb\(ServerLevel serverLevel, int stage\) \{([\s\S]*?)\n    private boolean forceEvolveNearbyParasite/);
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
    expect(fallback[1], /if \(!serverLevel\.addFreshEntity\(bomb\)\) \{\s*return;\s*\}[\s\S]*?broadcastEntityEvent\(bomb, \(byte\) 0\)[\s\S]*?SrpParticleEffects\.spawnType10\(serverLevel, getX\(\), getY\(\), getZ\(\)\)/,
            "fallback bomb strength is not synchronized after entering the world before type 10 is emitted");
}
expect(bomb, /setBeckonMotion\(double xSpeed, double ySpeed, double zSpeed, double capX, double capY\)[\s\S]*?Math\.min\(xSpeed, capX\)[\s\S]*?Math\.min\(ySpeed, capY\)[\s\S]*?Math\.min\(zSpeed, capX\)[\s\S]*?random\.nextDouble\(\) \* 2\.0D - 1\.0D/,
        "BombEntity does not preserve the original capped Beckon motion semantics");
expect(bomb, /handleEntityEvent\(byte id\)[\s\S]*?strength = id;[\s\S]*?if \(id >= 2\)[\s\S]*?canGrief = true;/,
        "BombEntity does not apply the original strength status event on clients");
expect(particles, /spawnType10\(ServerLevel level, double x, double y, double z\)[\s\S]*?send\(level, x, y, z, 0\.5F, 0\.5F, 10\)/,
        "the Beckon fallback type-10 packet endpoint is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("Beckon capacity fallback contract ok");
