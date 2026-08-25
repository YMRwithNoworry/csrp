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

const meteor = read("src/main/java/alku/csrp/entity/MeteorEntity.java");
const events = read("src/main/java/alku/csrp/world/MeteorEvents.java");
const impact = read("src/main/java/alku/csrp/world/MeteorImpactGenerator.java");
const worldConfig = read("src/main/java/alku/csrp/config/WorldConfig.java");
const orb = read("src/main/java/alku/csrp/entity/OrbBoomEntity.java");
const payload = read("src/main/java/alku/csrp/network/MeteorShakePayload.java");
const client = read("src/main/java/alku/csrp/client/MeteorClientEvents.java");
const commands = read("src/main/java/alku/csrp/command/SrpCommands.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const clientModEvents = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/MeteorRenderer.java");

for (const [pattern, description] of [
    [/MAX_LIFETIME\s*=\s*1200/, "meteor 1200-tick lifetime is missing"],
    [/life\s*>=\s*25\s*\?\s*findEntityCollision/, "meteor delayed entity collision is missing"],
    [/tickCount\s*%\s*20\s*==\s*0/, "main meteor 20-tick behavior cadence is missing"],
    [/random\.nextBoolean\(\)[\s\S]*spawnFragment/, "main meteor fragment chance is missing"],
    [/fragment\.setDeltaMovement\(Vec3\.ZERO\)/, "fragment does not start with original zero velocity"],
    [/spawnImpactPulse\(level,\s*hitPos,\s*isMainMeteor\(\)\s*\?\s*40\s*:\s*8\)/,
        "meteor impact does not create the original sized shockwave"],
    [/fellOutOfWorld\(\)[\s\S]*450\.0F\s*\*\s*strength/,
        "main meteor does not use armor-bypassing scaled impact damage"],
    [/ModMobEffects\.COTH[\s\S]*1200,\s*0/, "main meteor COTH field is missing"],
    [/ParticleTypes\.SMOKE/, "meteor smoke trail is missing"],
    [/ParticleTypes\.BUBBLE/, "meteor underwater bubble trail is missing"],
    [/ParticleTypes\.FLAME/, "meteor flame trail is missing"],
    [/ParticleTypes\.EXPLOSION_EMITTER/, "main meteor explosion trail is missing"],
    [/putDouble\("acceleration_x"/, "meteor acceleration persistence is missing"],
    [/putInt\("life"/, "meteor lifetime persistence is missing"]
]) expect(meteor, pattern, description);

expect(entities, /EntityType<MeteorEntity>>\s+METEOR[\s\S]*sized\(4\.5F,\s*4\.5F\)/,
        "dedicated 4.5-block meteor entity is missing");
expect(orb, /ownerId\s*==\s*null\s*&&\s*tickCount\s*%\s*10\s*==\s*0[\s\S]*pulse\(false\)/,
        "unowned meteor shockwave does not pulse every 10 ticks");
expect(orb, /if\s*\(!burst\)[\s\S]*damageSources\(\)\.magic\(\),\s*10\.0F/,
        "unowned meteor shockwave does not deal 10 damage");

for (const [pattern, description] of [
    [/meteorCheckInterval",\s*3600/, "default meteor check interval is not 3600"],
    [/meteorChance",\s*0\.5D/, "default meteor chance is not 0.5"],
    [/meteorDamageRadius",\s*110/, "default meteor damage radius is not 110"],
    [/meteorMinimumRadius",\s*80/, "default meteor minimum radius is not 80"],
    [/meteorMaximumRadius",\s*120/, "default meteor maximum radius is not 120"],
    [/meteorRequiresNoVector",\s*true/, "meteor vectorless gate is missing"],
    [/meteorDimensionBlacklist",\s*List\.of\("minecraft:the_nether"\)/,
        "default Nether meteor blacklist is missing"],
    [/meteorMinimumWorldTicks/, "meteor world-age gate is missing"]
]) expect(worldConfig, pattern, description);

for (const [pattern, description] of [
    [/evolutionPhase\(\)\s*<\s*0/, "natural meteor phase gate is missing"],
    [/vectors\(\)\.isEmpty\(\)/, "natural meteor existing-vector gate is missing"],
    [/canSeeSky/, "natural meteor exposed-player preference is missing"],
    [/nextInt\(Math\.max\(1,\s*WorldConfig\.meteorMaximumRadius\(\)\)\)/,
        "natural meteor random radius roll is missing"],
    [/level\.getMaxBuildHeight\(\)/, "meteor does not spawn at world height"],
    [/new MeteorShakePayload\(0,\s*0\.0F,\s*true\)/, "meteor arrival darkening packet is missing"]
]) expect(events, pattern, description);

for (const [pattern, description] of [
    [/carveTunnel[\s\S]*10,\s*30/, "main meteor angled tunnel dimensions are missing"],
    [/depth\s*=\s*Math\.max\(depth,\s*impact\.getY\(\)\s*-\s*tunnel\.lowestY\(\)\s*\+\s*3\)/,
        "tunnel depth does not open into the crater"],
    [/radius\s*=\s*Math\.max\(radius,\s*\(int\)\s*\(depth\s*\*\s*1\.6F\)\)/,
        "deep meteor crater does not expand dynamically"],
    [/clearVegetation\(level,\s*impact,\s*radius\s*\*\s*2,\s*depth\)/,
        "meteor impact vegetation clearing is missing"],
    [/deadBloodPool[\s\S]*Math\.max\(4,\s*radius\s*\/\s*6\),\s*10,\s*4/,
        "four-layer dead-blood pool dimensions are missing"],
    [/meteorBody\(/, "main meteor body generation is missing"],
    [/PARASITE_LOOT_RARE/, "main meteor rare loot is missing"],
    [/scheduleNearbyWater/, "post-impact water updates are missing"]
]) expect(impact, pattern, description);

expect(payload, /boolean darken/, "meteor payload does not carry the darkening flag");
expect(client, /darkTicks\s*=\s*40/, "meteor arrival darkening duration is missing");
expect(client, /RenderGuiEvent\.Post/, "meteor arrival dark overlay is not rendered");
expect(commands, /argument\("x"[\s\S]*argument\("y"[\s\S]*argument\("z"[\s\S]*argument\("radius"/,
        "spawnmeteor x y z radius command form is missing");
expect(clientModEvents, /registerLayerDefinition\(MeteorRenderer\.LAYER,\s*MeteorRenderer::createBodyLayer\)/,
        "meteor model layer is not registered");
expect(clientModEvents, /ModEntities\.METEOR\.get\(\),\s*MeteorRenderer::new/,
        "dedicated meteor renderer is not registered");
for (const [pattern, description] of [
    [/MAIN_SCALE\s*=\s*1\.9F/, "original main meteor render scale is missing"],
    [/FRAGMENT_SCALE\s*=\s*0\.6F/, "original fragment render scale is missing"],
    [/addTendril\(/, "meteor's animated tendril model is missing"],
    [/LightTexture\.FULL_BRIGHT/, "meteor is not rendered at full brightness"],
    [/Axis\.XP\.rotationDegrees\(pitch\)/, "meteor model does not follow its flight pitch"],
    [/boolean shouldRender\([\s\S]*?return true;/, "large meteor model can be incorrectly frustum-culled"]
]) expect(renderer, pattern, description);

if (failures.length) {
    console.error("Meteor port verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Meteor entity, natural spawning, impact, visuals and command verification passed.");
