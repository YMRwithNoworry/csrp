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

const config = read("src/main/java/alku/csrp/Config.java");
const events = read("src/main/java/alku/csrp/event/CreatureParasiteTaskEvents.java");

// Original SRPConfig defaults (out109 util/config/SRPConfig.java:21-24):
// entitiesWillAttack {"minecraft:villager_golem"}, entitiesWillAvoid {"minecraft:villager"}.
expect(config, /List\.of\("minecraft:villager_golem"\)/,
        "entitiesWillAttack default of minecraft:villager_golem is missing");
expect(config, /List\.of\("minecraft:villager"\)/,
        "entitiesWillAvoid default of minecraft:villager is missing");
expect(config, /public static boolean mobHuntsParasites\(String id\)/,
        "mobHuntsParasites accessor is missing");
expect(config, /public static boolean mobFleesParasites\(String id\)/,
        "mobFleesParasites accessor is missing");
expect(config, /return listed != inverted;/,
        "the entity list match no longer reproduces checkName's anyMatch != isWhitelist");

// Original SRPEventHandlerBus#setNewCreatureTask: attack goal else avoid goal at priority 5.
expect(events, /class CreatureParasiteTaskEvents/,
        "CreatureParasiteTaskEvents is missing");
expect(events, /Config\.mobHuntsParasites\(id\)/,
        "the attack branch is missing");
expect(events, /NearestAttackableTargetGoal<>\(mob, LivingEntity\.class,\s*10, true, false, \(target, serverLevel\) -> target instanceof Parasite\)/s,
        "the parasite target goal is missing");
expect(events, /Config\.mobFleesParasites\(id\)/,
        "the avoid branch is missing");
expect(events, /new AvoidEntityGoal<LivingEntity>\(mob, LivingEntity\.class,\s*target -> target instanceof Parasite,\s*AVOID_DISTANCE, AVOID_WALK_SPEED, AVOID_SPRINT_SPEED, target -> true\)/s,
        "the parasite avoid goal is missing");
expect(events, /AVOID_DISTANCE = 12\.0F/,
        "the original avoid distance of 12.0F is missing");
expect(events, /AVOID_WALK_SPEED = 0\.8D/,
        "the original avoid walk speed of 0.8 is missing");
expect(events, /GOAL_PRIORITY = 5/,
        "the original goal priority 5 is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("creature parasite task contract ok");
