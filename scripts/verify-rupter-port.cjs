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

const entity = read("src/main/java/alku/csrp/entity/RupterEntity.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const blocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");
const tunnel = read("src/main/java/alku/csrp/block/TunnelBlock.java");
const config = read("src/main/java/alku/csrp/Config.java");
const sounds = read("src/main/java/alku/csrp/registry/ModSounds.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const model = read("src/main/java/alku/csrp/client/model/RupterModel.java");
const modEntry = read("src/main/java/alku/csrp/Csrp.java");
const evolution = read("src/main/java/alku/csrp/entity/ManglerEvolutionTarget.java");
const buglinEvolution = read("src/main/java/alku/csrp/entity/BuglinEvolutionTarget.java");
const killMilestone = read("src/main/java/alku/csrp/event/RupterKillMilestoneEvents.java");
const advancement = read("src/main/resources/data/csrp/advancement/cut_roots.json");
const effects = read("src/main/java/alku/csrp/registry/ModMobEffects.java");
const bleedEffect = read("src/main/java/alku/csrp/effect/BleedMobEffect.java");
const viralEffect = read("src/main/java/alku/csrp/effect/ViralMobEffect.java");
const effectEvents = read("src/main/java/alku/csrp/event/ViralDamageEvents.java");
const biomeModifier = read("src/main/resources/data/csrp/neoforge/biome_modifier/rupter_spawns.json");
const loot = read("src/main/resources/data/csrp/loot_table/entities/rupter.json");
const geo = read("src/main/resources/assets/csrp/geo/rupter.geo.json");
const animations = read("src/main/resources/assets/csrp/animations/rupter.animation.json");

expect(entities, /register\("rupter"/, "Rupter entity is not registered");
expect(entities, /sized\(0\.85F,\s*1\.0F\)/, "legacy Rupter dimensions are missing");
expect(items, /"rupter_spawn_egg"/, "Rupter spawn egg is not registered");
expect(items, /"rupter_viscera"/, "Rupter Viscera is not registered");
expect(blocks, /DeferredBlock<TunnelBlock>\s+TUNNEL\s*=\s*BLOCKS\.register\("tunnel"/,
        "functional Tunnel block is not registered");
expect(blocks, /block\.tunnel\.dig/, "original Tunnel break sound is not wired");
expect(tunnel, /class TunnelBlock extends Block/, "Tunnel behavior class is missing");
expect(tunnel, /randomTick\(/, "Tunnel does not periodically spawn Buglins");
expect(tunnel, /getEntitiesOfClass\(BuglinEntity\.class,\s*tunnelArea\)/,
        "Tunnel local Buglin cap is missing");
expect(tunnel, /inflate\(16\.0D\)/, "Tunnel parasite population radius is missing");
expect(tunnel, /parasiteCount\s*<=\s*10/, "Tunnel parasite population cap is missing");
expect(tunnel, /onRemove\(/, "breaking a Tunnel does not release a Buglin");
expect(tunnel, /Difficulty\.PEACEFUL/, "Tunnel peaceful-difficulty guard is missing");
expect(tunnel, /dropFromExplosion[\s\S]*return false/, "Tunnel explosion drops are not disabled");
expect(modEntry, /output\.accept\(ModItems\.RUPTER_SPAWN_EGG\.get\(\)\)/,
        "CSRP creative tab does not contain the Rupter spawn egg");
expect(modEntry, /CreativeModeTabs\.SPAWN_EGGS[\s\S]*event\.accept\(ModItems\.RUPTER_SPAWN_EGG\.get\(\)\)/,
        "vanilla spawn eggs tab does not contain the Rupter spawn egg");
expect(modEntry, /BuglinEvolutionTarget\.registerRupter\(ModEntities\.RUPTER\)/,
        "Buglin evolution is not connected to Rupter");
expect(buglinEvolution, /registerRupter/, "Buglin evolution target contract is missing");

for (const [pattern, description] of [
    [/MAX_HEALTH,\s*10\.0/, "Wiki health 10 is missing"],
    [/ATTACK_DAMAGE,\s*5\.0/, "Wiki attack damage 5 is missing"],
    [/ARMOR,\s*5\.0/, "Wiki armor 5 is missing"],
    [/KNOCKBACK_RESISTANCE,\s*0\.2/, "Wiki knockback resistance 0.2 is missing"],
    [/MOVEMENT_SPEED,\s*0\.3/, "legacy movement speed 0.3 is missing"],
    [/FOLLOW_RANGE,\s*32\.0/, "legacy follow range 32 is missing"],
    [/xpReward\s*=\s*5/, "Wiki XP value 5 is missing"],
    [/EntityDataAccessor<Byte>.*CLIMBING/s, "synced climbing state is missing"],
    [/onClimbable\(\)/, "wall climbing override is missing"],
    [/LeapAtTargetGoal/, "large attack leap is missing"],
    [/liquid.*leap|leap.*liquid|isInWaterOrBubble\(\)/is, "small liquid leap is missing"],
    [/MobEffects\.(?:SLOWNESS|MOVEMENT_SLOWDOWN).*40,\s*1/s, "Slowness II melee effect is missing"],
    [/ModMobEffects\.COTH.*3600,\s*0/s, "guaranteed melee COTH is missing"],
    [/AreaEffectCloud/, "lingering COTH cloud is missing"],
    [/new MobEffectInstance\(ModMobEffects\.COTH,\s*\d+,\s*1\)/,
        "COTH II cloud effect is missing"],
    [/Config\.evolutionPhase\(level\(\)\)\s*<\s*4/, "legacy phase-4 aggression gate is missing"],
    [/nearby.*Rupter|Rupter.*nearby/is, "lone/pack behavior check is missing"],
    [/TUNNEL_KILL_COST\s*=\s*5/, "Tunnel kill cost 5 is missing"],
    [/killCount\s*>=\s*30/, "30-kill Mangler evolution is missing"],
    [/ManglerEvolutionTarget/, "Mangler evolution target is not used"],
    [/DamageTypeTags\.IS_FIRE/, "fire weakness damage tag is missing"],
    [/amount\s*\*\s*4\.0F/, "quadrupled fire damage is missing"]
]) expect(entity, pattern, description);

expect(entity, /RupterSpinGoal/, "Wiki random ground spinning behavior is missing");
expect(entity, /setYRot\(/, "Rupter spin goal does not rotate the entity");
expect(entity, /getJumpControl\(\)\.jump\(\)/, "Rupter cannot jump while spinning");
expect(entity, /BehaviorVariant/, "Rupter behavior variants are missing");
expect(entity, /BERSERKER\("_bleeding"\)/, "Berserker variant texture is missing");
expect(entity, /VIRULENT\("_virus"\)/, "Virulent variant texture is missing");
expect(entity, /0\.165F/, "Berserker/Virulent 16.5% variant weight is missing");
expect(entity, /getBehaviorVariant\(\)/, "Rupter behavior variant accessor is missing");
expect(entity, /BEHAVIOR_VARIANT/, "Synced behavior variant state is missing");
expect(entity, /behavior_variant/, "Behavior variant NBT persistence is missing");
expect(entity, /ModMobEffects\.BLEED.*60,\s*0/s, "Berserker Bleed hit effect is missing");
expect(entity, /ModMobEffects\.VIRAL.*80,\s*0/s, "Virulent leap Viral effect is missing");
expect(entity, /ModMobEffects\.VIRAL.*40,\s*0/s, "Virulent contact Viral effect is missing");
expect(effects, /register\("bleed",\s*BleedMobEffect::new\)/,
        "Bleed effect is not registered");
expect(effects, /register\("viral",\s*ViralMobEffect::new\)/,
        "Viral effect is not registered");
expect(bleedEffect, /applyEffectTick|shouldApplyEffectTickThisTick/,
        "Bleed effect does not tick damage");
expect(viralEffect, /class ViralMobEffect/, "Viral effect implementation is missing");
expect(effectEvents, /LivingIncomingDamageEvent/, "Viral damage multiplier hook is missing");
expect(killMilestone, /RUPTER_KILL_COUNT_KEY\s*=\s*"csrpRupterKills"/,
        "persistent Rupter kill counter is missing");
expect(killMilestone, /RUPTER_KILL_TARGET\s*=\s*1000/,
        "Wiki 1000-Rupter kill target is missing");
expect(killMilestone, /LivingDeathEvent/, "Rupter kill milestone is not connected to entity deaths");
expect(killMilestone, /CRITERION\s*=\s*"reached_1000_rupter_kills"/,
        "Cut the evil by its roots criterion id is missing");
expect(killMilestone, /award\([^,]+,\s*CRITERION\)/,
        "Cut the evil by its roots criterion is not awarded");
expect(advancement, /"trigger"\s*:\s*"minecraft:impossible"/,
        "Cut the evil by its roots advancement criterion is missing");

expect(config, /defineInRange\("evolutionPhase",\s*-1,\s*-2,\s*10\)/,
        "runtime evolution phase config is missing");
expect(evolution, /registerMangler/, "Mangler evolution registration contract is missing");
expect(client, /RupterRenderer/, "Rupter renderer is not registered");
expect(model, /getTextureVariant\(\)/, "Rupter texture variants are not wired");
expect(geo, /"identifier"\s*:\s*"geometry\.srparasites\.rupter"/,
        "Rupter geometry identifier is wrong");
for (const animation of [
    "func_78087_a.age_in_ticks",
    "func_78087_a.limb_swing",
    "func_78087_a.age_in_ticks.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_1",
    "func_78087_a.limb_swing.get_parasite_status_2",
    "func_78087_a.age_in_ticks.get_parasite_status_10"
]) {
    expect(animations, new RegExp(`"animation\\.rupter\\.${animation}"\\s*:`),
            `missing original extracted ${animation} animation`);
}
for (const variant of ["classic", "striped", "fluffy", "weird", "golden"]) {
    expect(entity, new RegExp(variant.toUpperCase()), `missing ${variant} texture variant`);
    read(`src/main/resources/assets/csrp/textures/entity/rupter_${variant}.png`);
}
for (const variant of ["bleeding", "virus"]) {
    const behaviorName = variant === "bleeding" ? "BERSERKER" : "VIRULENT";
    expect(entity, new RegExp(behaviorName), `missing ${variant} behavior variant`);
    read(`src/main/resources/assets/csrp/textures/entity/rupter_${variant}.png`);
}
for (const effect of ["bleed", "viral"]) {
    read(`src/main/resources/assets/csrp/textures/mob_effect/${effect}.png`);
}
expect(biomeModifier, /"type"\s*:\s*"neoforge:add_spawns"/, "Rupter biome modifier is missing");
expect(biomeModifier, /"weight"\s*:\s*30/, "Wiki spawn weight 30 is missing");
expect(biomeModifier, /"minCount"\s*:\s*3/, "Wiki minimum group size 3 is missing");
expect(biomeModifier, /"maxCount"\s*:\s*6/, "Wiki maximum group size 6 is missing");
expect(loot, /"chance"\s*:\s*0\.7/, "Wiki Rupter Viscera drop chance 70% is missing");
expect(loot, /"min"\s*:\s*1[\s\S]*"max"\s*:\s*2/, "Wiki Rupter Viscera count 1-2 is missing");

for (const sound of ["rupter.living", "rupter.hurt", "rupter.death", "rupter.step", "rupter.cloud"]) {
    expect(sounds, new RegExp(`register\\("${sound.replace(".", "\\.")}"\\)`),
            `missing ${sound} sound registration`);
}
for (const resource of [
    "src/main/resources/assets/csrp/textures/entity/rupter.png",
    "src/main/resources/assets/csrp/textures/item/rupter_spawn_egg.png",
    "src/main/resources/assets/csrp/textures/item/rupter_viscera.png",
    "src/main/resources/assets/csrp/models/item/rupter_spawn_egg.json",
    "src/main/resources/assets/csrp/models/item/rupter_viscera.json",
    "src/main/resources/assets/csrp/blockstates/tunnel.json",
    "src/main/resources/assets/csrp/models/block/tunnel.json",
    "src/main/resources/assets/csrp/textures/block/tunnel.png",
    "src/main/resources/assets/csrp/models/block/gore_base.json",
    "src/main/resources/assets/csrp/sounds/mob/pure/rupter/cloud.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/rupter/death.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/rupter/hurt1.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/rupter/hurt2.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/rupter/hurt3.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/rupter/idle1.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/rupter/idle2.ogg",
    "src/main/resources/assets/csrp/sounds/mob/pure/rupter/idle3.ogg",
    "src/main/resources/assets/csrp/sounds/mob/step/small/step1.ogg",
    "src/main/resources/assets/csrp/sounds/mob/step/small/step2.ogg",
    "src/main/resources/assets/csrp/sounds/mob/step/small/step3.ogg",
    "src/main/resources/assets/csrp/sounds/mob/step/small/step4.ogg"
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

for (const resourceText of [geo, animations, advancement, biomeModifier, loot]) {
    if (resourceText.includes("opensrp:") || resourceText.includes("srparasites:")) {
        failures.push("Rupter resource still references a foreign namespace");
    }
}

if (failures.length) {
    console.error("Rupter port verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Rupter port verification passed.");
