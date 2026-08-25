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

const meteor = read("src/main/java/alku/csrp/entity/MeteorEntity.java");
const events = read("src/main/java/alku/csrp/world/MeteorEvents.java");
const impact = read("src/main/java/alku/csrp/world/MeteorImpactGenerator.java");
const loader = read("src/main/java/alku/csrp/world/MeteorStructureLoader.java");
const worldConfig = read("src/main/java/alku/csrp/config/WorldConfig.java");
const worldData = read("src/main/java/alku/csrp/world/SrpWorldData.java");
const selection = read("src/main/java/alku/csrp/world/SrpMeteorSelection.java");
const worldScreen = read("src/main/java/alku/csrp/client/SrpDifficultyScreenEvents.java");
const orb = read("src/main/java/alku/csrp/entity/OrbBoomEntity.java");
const payload = read("src/main/java/alku/csrp/network/MeteorShakePayload.java");
const client = read("src/main/java/alku/csrp/client/MeteorClientEvents.java");
const commands = read("src/main/java/alku/csrp/command/SrpCommands.java");
const entities = read("src/main/java/alku/csrp/registry/ModEntities.java");
const clientModEvents = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const renderer = read("src/main/java/alku/csrp/client/renderer/MeteorRenderer.java");

for (const [pattern, description] of [
    [/MAX_LIFETIME\s*=\s*1200/, "meteor 1200-tick lifetime is missing"],
    [/getHitResultOnMoveVector\([\s\S]*life\s*>=\s*25/, "meteor delayed entity collision is missing"],
    [/tickCount\s*%\s*20\s*==\s*0/, "main meteor 20-tick behavior cadence is missing"],
    [/random\.nextBoolean\(\)[\s\S]*spawnFragment/, "main meteor fragment chance is missing"],
    [/fragment\.setDeltaMovement\(Vec3\.ZERO\)/, "fragment does not start with original zero velocity"],
    [/ProjectileUtil\.rotateTowardsMovement\(this,\s*0\.2F\)/,
        "meteor does not smoothly face its movement direction"],
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
reject(meteor, /playSound\(|sendParticles\(/,
        "meteor impact adds sound or particles that the original impact handler did not emit");

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
    [/initialVelocity[\s\S]*scale\(10\.5D\)[\s\S]*setDeltaMovement\(initialVelocity\)/,
        "main meteor does not launch with the original 10.5 blocks/tick velocity"],
    [/data\.meteorsEnabled\(\)/, "per-world meteor gate is missing"],
    [/elapsed\s*<=\s*interval/, "original meteor check cadence is missing"],
    [/data\.evolutionPhase\(\)\s*<\s*0/, "natural meteor phase gate is missing"],
    [/data\.vectors\(\)\.isEmpty\(\)/, "natural meteor existing-vector gate is missing"],
    [/canSeeSky/, "natural meteor exposed-player preference is missing"],
    [/nextInt\(Math\.max\(1,\s*WorldConfig\.meteorMaximumRadius\(\)\)\)/,
        "natural meteor random radius roll is missing"],
    [/level\.getMaxBuildHeight\(\)/, "meteor does not spawn at world height"],
    [/new MeteorShakePayload\(0,\s*0\.0F,\s*true\)/, "meteor arrival darkening packet is missing"]
]) expect(events, pattern, description);

for (const [pattern, description] of [
    [/stainArrivalColumn\(level,\s*impact\.below\(10\),\s*35,\s*20\)/,
        "main meteor arrival stain column is missing"],
    [/for\s*\(int layer\s*=\s*0;\s*layer\s*<\s*80[\s\S]*layer\s*<\s*80[\s\S]*layer\s*<\s*6/,
        "original 166-layer arrival volume is missing"],
    [/carveTunnel[\s\S]*10,\s*30/, "main meteor angled tunnel dimensions are missing"],
    [/depth\s*=\s*Math\.max\(depth,\s*impact\.getY\(\)\s*-\s*tunnel\.lowestY\(\)\s*\+\s*3\)/,
        "tunnel depth does not open into the crater"],
    [/radius\s*=\s*Math\.max\(radius,\s*\(int\)\s*\(depth\s*\*\s*1\.6F\)\)/,
        "deep meteor crater does not expand dynamically"],
    [/radius\s*\*\s*0\.28F[\s\S]*random\.nextInt\(3\)[\s\S]*cookedFlesh\(\)/,
        "original cooked-flesh crater core is missing"],
    [/distance\s*>=\s*radius\s*-\s*1\.5D/, "original crater rim threshold is missing"],
    [/distance\s*>=\s*radius\s*\*\s*0\.55D[\s\S]*random\.nextInt\(4\)/,
        "original outer crater stain chance is missing"],
    [/clearVegetation\(level,\s*impact,\s*radius\s*\*\s*2,\s*depth\)/,
        "meteor impact vegetation clearing is missing"],
    [/deadBloodPool[\s\S]*Math\.max\(4,\s*radius\s*\/\s*6\),\s*10,\s*4/,
        "four-layer dead-blood pool dimensions are missing"],
    [/above\(14\)\.offset\(-24,\s*0,\s*-24\)/, "main meteor structure origin is incorrect"],
    [/MeteorStructureLoader\.place\(level,\s*"meteor"/, "original main meteor structure is not placed"],
    [/structureOrigin\.offset\(22,\s*14,\s*22\)/, "main structure post-processing center is incorrect"],
    [/switch\s*\(random\.nextInt\(9\)\)[\s\S]*meteor_fragment_large3[\s\S]*meteor_fragment_small6/,
        "all nine original fragment structures are not selected uniformly"],
    [/PARASITE_LOOT_RARE/, "main meteor rare loot is missing"],
    [/getFluidState\(\)\.is\(FluidTags\.WATER\)/, "post-impact water updates are missing"]
]) expect(impact, pattern, description);

for (const [pattern, description] of [
    [/LEGACY_NAMESPACE\s*=\s*"srparasites:"/, "legacy meteor structure namespace rewriting is missing"],
    [/Map\.entry\("dermoid_cyst",\s*"gluttonous_cyst"\)/,
        "legacy dermoid cyst block mapping is missing"],
    [/value\.equals\(LEGACY_NAMESPACE\s*\+\s*"dermoid_cyst"\)[\s\S]*parasitic_cyst/,
        "legacy dermoid cyst block entity mapping is missing"],
    [/template\.load\(level\.holderLookup\(Registries\.BLOCK\),\s*root\)/,
        "rewritten meteor structure is not loaded through the modern registry lookup"],
    [/setIgnoreEntities\(false\)[\s\S]*setKeepLiquids\(false\)/,
        "original meteor structure placement settings are missing"]
]) expect(loader, pattern, description);
reject(loader, /static final Map<String, StructureTemplate>\s+CACHE/,
        "meteor templates are cached across resource reloads or server registries");

for (const [content, pattern, description] of [
    [selection, /AtomicReference<Boolean>[\s\S]*consumeOrDefault/, "create-world meteor selection handoff is missing"],
    [worldData, /putBoolean\("meteors_enabled"[\s\S]*SrpMeteorSelection\.consumeOrDefault/,
        "per-world meteor choice is not persisted"],
    [worldScreen, /CycleButton<Boolean>[\s\S]*options\.csrp\.meteors/,
        "create-world meteor toggle is missing"],
    [worldScreen, /meteor_orbit\.png[\s\S]*\.blit\(METEOR_ORBIT/,
        "original meteor orbit preview is not rendered"]
]) expect(content, pattern, description);

expect(payload, /boolean darken/, "meteor payload does not carry the darkening flag");
expect(client, /darkTicks\s*=\s*40/, "meteor arrival darkening duration is missing");
expect(client, /RenderGuiEvent\.Post/, "meteor arrival dark overlay is not rendered");
expect(commands, /argument\("x"[\s\S]*argument\("y"[\s\S]*argument\("z"[\s\S]*argument\("radius"/,
        "spawnmeteor x y z radius command form is missing");
reject(commands, /literal\("spawnmeteor"\)\s*\.executes\(/,
        "spawnmeteor retains a non-original no-argument command form");
expect(clientModEvents, /registerLayerDefinition\(MeteorRenderer\.LAYER,\s*MeteorRenderer::createBodyLayer\)/,
        "meteor model layer is not registered");
expect(clientModEvents, /ModEntities\.METEOR\.get\(\),\s*MeteorRenderer::new/,
        "dedicated meteor renderer is not registered");
for (const [pattern, description] of [
    [/PartPose\.offset\(0\.0F,\s*-29\.3F,\s*0\.0F\)/, "original meteor model root offset is missing"],
    [/MAIN_SCALE_XZ\s*=\s*0\.5F\s*\+\s*1\.4F\s*\*\s*SCALE_WOBBLE/,
        "original non-uniform main meteor XZ scale is missing"],
    [/MAIN_SCALE_Y\s*=\s*0\.5F\s*\+\s*1\.1F\s*\/\s*SCALE_WOBBLE/,
        "original non-uniform main meteor Y scale is missing"],
    [/FRAGMENT_SCALE_XZ\s*=\s*-0\.8F\s*\+\s*1\.4F\s*\*\s*SCALE_WOBBLE/,
        "original non-uniform fragment XZ scale is missing"],
    [/FRAGMENT_SCALE_Y\s*=\s*-0\.8F\s*\+\s*1\.1F\s*\/\s*SCALE_WOBBLE/,
        "original non-uniform fragment Y scale is missing"],
    [/poseStack\.translate\(0\.0D,\s*-1\.501D,\s*0\.0D\)/,
        "original meteor render translation is missing"],
    [/rear_lobe_9/, "ninth rear meteor body lobe is missing"],
    [/int\[\]\s+sizes\s*=\s*\{4,\s*6,\s*4,\s*6,\s*4,\s*2\}/,
        "original six-segment tendril geometry is missing"],
    [/LightTexture\.FULL_BRIGHT/, "original full-bright meteor rendering is missing"],
    [/animateTendril\(4,\s*third,\s*-first,\s*third,\s*-second,\s*-first,\s*second\)/,
        "original fifth tendril animation is missing"],
    [/boolean shouldRender\([\s\S]*?return true;/, "large meteor model can be incorrectly frustum-culled"]
]) expect(renderer, pattern, description);
reject(renderer, /Axis\.XP\.rotationDegrees\(/,
        "meteor renderer applies a non-original whole-model pitch rotation");

if (failures.length) {
    console.error("Meteor port verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Meteor structures, entity, spawning, impact, visuals, world setting and command verification passed.");
