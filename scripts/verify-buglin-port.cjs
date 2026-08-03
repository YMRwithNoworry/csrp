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

const entity = read("src/main/java/alku/csrp/entity/BuglinEntity.java");
const modEntry = read("src/main/java/alku/csrp/Csrp.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const effects = read("src/main/java/alku/csrp/registry/ModMobEffects.java");
const sounds = read("src/main/java/alku/csrp/registry/ModSounds.java");
const evolution = read("src/main/java/alku/csrp/entity/BuglinEvolutionTarget.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const model = read("src/main/java/alku/csrp/client/model/BuglinModel.java");
const animationResolver = read("src/main/java/alku/csrp/entity/ParasiteAnimations.java");
const geo = read("src/main/resources/assets/csrp/geo/buglin.geo.json");
const animations = read("src/main/resources/assets/csrp/animations/buglin.animation.json");

expect(entities, /register\("buglin"/, "Buglin entity is not registered");
expect(entities, /sized\(0\.5F,\s*0\.3F\)/, "legacy Buglin dimensions are missing");
expect(items, /registerItem\(\s*"buglin_spawn_egg"/, "Buglin spawn egg is not registered");
expect(modEntry, /CREATIVE_MODE_TABS\.register\("csrp_tab"/, "CSRP creative tab is not registered");
expect(modEntry, /icon\(\(\) -> ModItems\.BUGLIN_SPAWN_EGG\.get\(\)\.getDefaultInstance\(\)\)/,
        "CSRP creative tab does not use the Buglin spawn egg icon");
expect(modEntry, /output\.accept\(ModItems\.BUGLIN_SPAWN_EGG\.get\(\)\)/,
        "CSRP creative tab does not contain the Buglin spawn egg");
if (/EXAMPLE_ITEM|"example_item"/.test(modEntry)) failures.push("example item is still registered or referenced");
expect(effects, /register\("coth"/, "COTH contact effect is not registered");
expect(entity, /MAX_HEALTH,\s*7\.0/, "legacy health is missing");
expect(entity, /ARMOR,\s*1\.5/, "legacy armor is missing");
expect(entity, /ATTACK_DAMAGE,\s*3\.0/, "legacy attack damage is missing");
expect(entity, /KNOCKBACK_RESISTANCE,\s*0\.05/, "legacy knockback resistance is missing");
expect(entity, /MOVEMENT_SPEED,\s*0\.2/, "legacy movement speed is missing");
expect(entity, /AvoidEntityGoal<.*LivingEntity/, "legacy selective avoidance goal is missing");
expect(entity, /WaterAvoidingRandomStrollGoal/, "wandering AI is missing");
expect(entity, /FloatGoal/, "swimming AI is missing");
expect(entity, /100,\s*0/, "legacy COTH duration/amplifier is missing");
expect(entity, /GROWTH_NBT_KEY\s*=\s*"ruptergrow"/, "legacy growth NBT key is missing");
expect(entity, /random\.nextInt\(60\)\s*\+\s*60/, "60-119 second growth range is missing");
expect(entity, /EMERGENCE_TICKS\s*=\s*50/, "50 tick emergence state is missing");
expect(entity, /triggerAnim\("emergence_controller",\s*"spawn"\)/, "spawn animation trigger is missing");
expect(entity, /BuglinEvolutionTarget\.rupterType\(\)\.ifPresent/, "mature Buglin does not wait for a real Rupter type");
expect(evolution, /registerRupter/, "Rupter evolution registration contract is missing");
expect(client, /BuglinRenderer/, "Buglin renderer is not registered");
expect(model, /geo\/buglin\.geo\.json/, "Buglin geometry is not wired");
expect(model, /animations\/buglin\.animation\.json/, "Buglin animations are not wired");
expect(geo, /"identifier"\s*:\s*"geometry\.srparasites\.buglin"/, "Buglin geometry identifier is wrong");
for (const animation of ["idle", "walk", "attack"]) {
    expect(animations, new RegExp(`"animation\\.buglin\\.${animation}"\\s*:`),
            `missing extracted ${animation} animation`);
}
expect(animationResolver, /case "run", "fly" -> "walk"/, "run animation is not mapped to extracted walk");
expect(animationResolver, /case "spawn", "rush", "throw", "smash", "swipe" -> "attack"/,
        "spawn animation is not mapped to extracted attack");
for (const sound of ["lodo.growl", "lodo.hurt", "lodo.death", "lodo.mudo", "lodo.emerge"]) {
    expect(sounds, new RegExp(`register\\("${sound.replace(".", "\\.")}"\\)`), `missing ${sound} sound registration`);
}
for (const resource of [
    "src/main/resources/assets/csrp/textures/entity/buglin.png",
    "src/main/resources/assets/csrp/textures/item/buglin_spawn_egg.png",
    "src/main/resources/assets/csrp/models/item/buglin_spawn_egg.json",
    "src/main/resources/assets/csrp/sounds/mob/pure/buglin/death.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/buglin/emerge.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/buglin/hurt1.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/buglin/hurt2.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/buglin/hurt3.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/buglin/idle1.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/buglin/idle2.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/buglin/idle3.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/buglin/lodo_mudo.ogg"
]) read(resource);

if (geo && animations) {
    const geometryJson = JSON.parse(geo);
    const animationJson = JSON.parse(animations);
    const geometryBones = new Set(geometryJson["minecraft:geometry"][0].bones.map((bone) => bone.name));
    for (const [animationName, animation] of Object.entries(animationJson.animations)) {
        for (const bone of Object.keys(animation.bones ?? {})) {
            if (!geometryBones.has(bone)) failures.push(`${animationName} references missing bone ${bone}`);
        }
    }
}

for (const resourceText of [geo, animations, read("src/main/resources/assets/csrp/models/item/buglin_spawn_egg.json")]) {
    if (resourceText.includes("opensrp:")) failures.push("resource still references the opensrp namespace");
}

if (failures.length) {
    console.error("Buglin port verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Buglin port verification passed.");
