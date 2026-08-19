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

const entities = [
    ["Rupter", read("src/main/java/alku/csrp/entity/RupterEntity.java")],
    ["Assimilated walking head", read("src/main/java/alku/csrp/entity/AssimilatedHeadEntity.java")],
    ["Adventurer walking head", read("src/main/java/alku/csrp/entity/SimAdventurerHeadEntity.java")]
];

for (const [name, entity] of entities) {
    const retreatPattern = name === "Rupter"
        ? /nearbyParasites\(\)\s*==\s*0/
        : /nearbyParasites\(\)\s*<=\s*2/;
    expect(entity, retreatPattern, `${name}: retreat threshold is missing`);
    expect(entity, /getEntitiesOfClass\(LivingEntity\.class[\s\S]*instanceof Parasite/,
            `${name}: nearby population does not count all parasite types`);
    expect(entity, /AvoidEntityGoal<>\([\s\S]*this::shouldAvoid/,
            `${name}: player and hostile retreat goal is missing`);
    expect(entity, /shouldRetreatForPackSize\(\)[\s\S]*setTarget\(null\)/,
            `${name}: stale combat targets are not cleared while retreating`);
    expect(entity, /ToxicCloudEntity\.create/, `${name}: COTH cloud creation is missing`);
    expect(entity, /new MobEffectInstance\(ModMobEffects\.COTH,\s*3600,\s*1,\s*false,\s*false,\s*true\)/,
            `${name}: COTH II cloud effect is missing`);
    expect(entity, /entity instanceof Animal \|\| entity instanceof WaterAnimal[\s\S]*entity instanceof Villager/,
            `${name}: cloud does not select passive creatures`);
}

if (failures.length) {
    console.error("Rupter and walking-head AI verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Rupter and walking-head AI verification passed.");
