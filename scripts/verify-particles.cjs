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

const registry = read("src/main/java/alku/csrp/registry/ModParticles.java");
const client = read("src/main/java/alku/csrp/client/ClientModEvents.java");
const config = read("src/main/java/alku/csrp/Config.java");
const spreading = read("src/main/java/alku/csrp/block/ParasiteSpreadingBlock.java");
const infested = read("src/main/java/alku/csrp/block/InfestedBlock.java");
const loot = read("src/main/java/alku/csrp/block/ParasiteLootBlock.java");
const plank = read("src/main/java/alku/csrp/block/ParasitePlankBlock.java");
const ore = read("src/main/java/alku/csrp/block/InfestedOreBlock.java");
const bulb = read("src/main/java/alku/csrp/block/EscaBulbBlock.java");
const burstPayload = read("src/main/java/alku/csrp/network/SrpParticleBurstPayload.java");
const payloadHandlers = read("src/main/java/alku/csrp/network/SrpParticlePayloads.java");
const serverEffects = read("src/main/java/alku/csrp/particle/SrpParticleEffects.java");
const clientEffects = read("src/main/java/alku/csrp/client/SrpParticleEffectsClient.java");
const goreSplash = read("src/main/java/alku/csrp/client/particle/GoreSplashParticle.java");
const biomassParticle = read("src/main/java/alku/csrp/client/particle/BiomassParticle.java");
const biomassEntity = read("src/main/java/alku/csrp/entity/BiomassEntity.java");
const adaptedEntity = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");
const summonerEntity = read("src/main/java/alku/csrp/entity/SummonerEntity.java");
const remainEntity = read("src/main/java/alku/csrp/entity/RemainEntity.java");
const entityParticles = read("src/main/java/alku/csrp/particle/SrpEntityParticleEffects.java");
const rageEffect = read("src/main/java/alku/csrp/effect/RageMobEffect.java");
const canister = read("src/main/java/alku/csrp/block/entity/ParasiteCanisterBlockEntity.java");
const repelEvents = read("src/main/java/alku/csrp/event/CothItemPreventEvents.java");

// Original SRPEnumParticle (out109 client/particle/SRPEnumParticle.java): fog, spore, gcloud,
// gsplash, rhappy, biomass, een, flash, dot, wind, coolerfog, rage, blood.
for (const id of ["spore", "rhappy", "een", "rage", "wind", "dot", "flash", "blood",
        "gore_cloud", "gore_splash"]) {
    expect(registry, new RegExp(`register\\("${id}"`), `particle type ${id} is not registered`);
}
for (const provider of ["SporeParticle", "RHappyParticle", "EenParticle", "RageParticle",
        "WindParticle", "DotParticle", "FlashParticle", "BloodParticle", "GoreCloudParticle",
        "GoreSplashParticle"]) {
    expect(client, new RegExp(`${provider}\\.Provider::new`),
            `${provider} is not registered as a particle provider`);
}

// Original SRPEnumParticle entries that have data-bearing sprite providers.
const options = read("src/main/java/alku/csrp/particle/SrpColorParticleOptions.java");
expect(options, /RecordCodecBuilder\.mapCodec/, "the colour particle codec is missing");
expect(options, /StreamCodec\.composite/, "the colour particle stream codec is missing");
expect(options, /public ParticleType<\?> getType\(\)/, "the colour particle getType is missing");

expect(goreSplash, /family == 4[\s\S]*?for \(int texture = 1; texture < 6; texture\+\+\)[\s\S]*?return familyStart;/,
        "GSPLASH vomit texture selection does not match the original weighted chain");
expect(burstPayload, /record SrpParticleBurstPayload\(double x, double y, double z, float width, float height, byte particleType\)/,
        "SRPPacketParticle coordinate, bounds or type fields are missing");
expect(burstPayload, /writeDouble\(x\)[\s\S]*?writeDouble\(y\)[\s\S]*?writeDouble\(z\)[\s\S]*?writeFloat\(width\)[\s\S]*?writeFloat\(height\)[\s\S]*?writeByte\(particleType\)/,
        "SRPPacketParticle wire field order does not match the original");
expect(payloadHandlers, /playToClient\(SrpParticleBurstPayload\.TYPE,[\s\S]*?SrpParticleBurstPayload\.STREAM_CODEC,[\s\S]*?SrpParticleBurstPayload::handle/,
        "SRPPacketParticle client payload is not registered");
expect(serverEffects, /spawnType1[\s\S]*?send\(level, x, y, z, width, height, 1\)[\s\S]*?spawnType2[\s\S]*?send\(level, x, y, z, width, height, 2\)[\s\S]*?spawnType3[\s\S]*?send\(level, x, y, z, width, height, 3\)[\s\S]*?spawnType4[\s\S]*?send\(level, x, y, z, width, height, 4\)/,
        "server particle dispatcher does not preserve original packet types 1-4");
expect(serverEffects, /spawnType10[\s\S]*?send\(level, x, y, z, 0\.5F, 0\.5F, 10\)[\s\S]*?spawnType11[\s\S]*?send\(level, x, y, z, 0\.5F, 0\.5F, 11\)/,
        "server particle dispatcher does not preserve original packet types 10/11");
expect(serverEffects, /spawnType12Tracking\(Entity entity\)[\s\S]*?sendTracking\(entity, 12\)[\s\S]*?spawnType2Tracking\(Entity entity\)[\s\S]*?sendTracking\(entity, 2\)[\s\S]*?sendToPlayersTrackingEntityAndSelf\(entity, payload\)/,
        "Rage status particles do not use the original tracking audience");
expect(serverEffects, /for \(ServerPlayer player : level\.getServer\(\)\.getPlayerList\(\)\.getPlayers\(\)\)[\s\S]*?PacketDistributor\.sendToPlayer\(player, payload\)/,
        "particle burst is not sent to every connected player");
expect(clientEffects, /case 1 -> spawnVolume\(level, random, goreSplash\(0\), x, y, z, 1,[\s\S]*?case 2 -> spawnVolume\(level, random, goreCloud\(0, 0, 0\), x, y, z, 4,[\s\S]*?case 3 -> spawnVolume\(level, random, ModParticles\.RHAPPY\.get\(\), x, y, z, 6,[\s\S]*?case 4 -> spawnVolume\(level, random, goreCloud\(125, 227, 118\), x, y, z, 24,/,
        "client packet types 1-4 do not match the original particle counts and colors");
expect(rageEffect, /for \(int index = 0; index < 4; index\+\+\)[\s\S]*?random\.nextFloat\(\) < 0\.1F[\s\S]*?spawnType12Tracking\(entity\)[\s\S]*?random\.nextFloat\(\) < 0\.8F[\s\S]*?spawnType2Tracking\(entity\)[\s\S]*?return true;[\s\S]*?shouldApplyEffectTickThisTick[\s\S]*?return true;/,
        "RAGE no longer emits original probabilistic type-12/type-2 particles each tick");
expect(canister, /if \(cyst\.consumeOne\(\)\)[\s\S]*?SrpParticleEffects\.spawnType2\(\(ServerLevel\) level,[\s\S]*?pos\.getY\(\) \+ 0\.7D[\s\S]*?0\.5F, 0\.5F\)/,
        "canister consumption does not emit original type-2 GCLOUD particles");
expect(repelEvents, /private static void applyRepel\(ServerLevel level, LivingEntity target, int durationTicks\)[\s\S]*?SrpParticleEffects\.spawnType3\(level, target\.getX\(\), target\.getY\(\), target\.getZ\(\),[\s\S]*?target\.getBbWidth\(\), target\.getBbHeight\(\)\)/,
        "successful COTH-prevent item use does not emit original type-3 RHAPPY particles");
expect(clientEffects, /TYPE10_SPLASH_COUNT = 3[\s\S]*?TYPE10_SPRAY_COUNT = 5[\s\S]*?TYPE10_CLOUD_COUNT = 5[\s\S]*?TYPE11_SPRAY_COUNT = 33[\s\S]*?TYPE11_CLOUD_COUNT = 13/,
        "client packet burst particle counts differ from the original");
expect(clientEffects, /case 10 -> \{[\s\S]*?TYPE10_SPLASH_COUNT[\s\S]*?TYPE10_SPRAY_COUNT[\s\S]*?TYPE10_CLOUD_COUNT[\s\S]*?case 11 -> \{[\s\S]*?TYPE11_SPRAY_COUNT[\s\S]*?TYPE11_CLOUD_COUNT[\s\S]*?case 12 -> spawnVolume\(level, random, ModParticles\.RAGE\.get\(\), x, y, z, 1,/,
        "client packet types 10-12 do not match the original particle counts");
expect(clientEffects, /random\.nextGaussian\(\) \* 0\.02D[\s\S]*?random\.nextFloat\(\) \* width \* 2\.0F - width[\s\S]*?y \+ 0\.5D \+ random\.nextFloat\(\) \* height/,
        "particle burst position or Gaussian velocity differs from the original");
expect(clientEffects, /sampleX = \(float\) x \+ random\.nextFloat\(\)[\s\S]*?strength = 0\.5D \/ \(distance \/ 4\.0D \+ 0\.1D\)[\s\S]*?y \+ 1\.2D/,
        "directed GSPLASH velocity sampling differs from the original");

// Original SRPConfigSystems.rsBlockParticleS / rsBlockParticleF (out109 44-45) default to 0.05.
expect(config, /\.defineInRange\("rsBlockParticleS", 0\.05D, 0\.0D, 1\.0D\)/,
        "rsBlockParticleS default of 0.05 is missing");
expect(config, /\.defineInRange\("rsBlockParticleF", 0\.05D, 0\.0D, 1\.0D\)/,
        "rsBlockParticleF default of 0.05 is missing");

// Original block randomDisplayTick wiring.
expect(spreading, /SrpBlockParticles\.spawnSpore\(level, pos, random\)/,
        "ParasiteSpreadingBlock no longer spawns the spore particle");
expect(infested, /SrpBlockParticles\.spawnFog\(level, pos, random\)/,
        "InfestedBlock no longer spawns the stage-3 fog particle");
expect(infested, /stage == 2[\s\S]*stage == 3/,
        "InfestedBlock no longer branches on the stage");
expect(loot, /SrpBlockParticles\.spawnSpore\(level, pos, random\)/,
        "ParasiteLootBlock no longer spawns the spore particle");
expect(plank, /SrpBlockParticles\.spawnSpore\(level, pos, random\)/,
        "ParasitePlankBlock no longer spawns the spore particle");
expect(ore, /SrpBlockParticles\.spawnSpore\(level, pos, random\)/,
        "InfestedOreBlock no longer spawns the spore particle");
expect(bulb, /SrpBlockParticles\.spawnBlood\(level, pos, random\)/,
        "EscaBulbBlock no longer spawns the blood particle");

// Entity particle call sites preserve the original sampling order and payloads.
expect(entityParticles,
        /velocityX = random\.nextGaussian\(\) \* 0\.02D;[\s\S]*?velocityY = random\.nextGaussian\(\) \* 0\.02D;[\s\S]*?velocityZ = random\.nextGaussian\(\) \* 0\.02D;[\s\S]*?x = entity\.getX\(\) \+ random\.nextFloat\(\) \* entity\.getBbWidth\(\) \* 2\.0F - entity\.getBbWidth\(\);[\s\S]*?y = entity\.getY\(\) \+ 0\.5D \+ random\.nextFloat\(\) \* sampleHeight;[\s\S]*?z = entity\.getZ\(\) \+ random\.nextFloat\(\) \* entity\.getBbWidth\(\) \* 2\.0F - entity\.getBbWidth\(\);/,
        "entity volume particles no longer use the original Gaussian velocity and bounds sampling");
expect(biomassEntity,
        /int stage = \(int\) getStage\(\);[\s\S]*?int interval = stage == 3 \? 3 : stage == 2 \|\| stage == 6 \? 5 : 10;[\s\S]*?int count = stage == 3 \? 3 : 2;[\s\S]*?spawnVolume\(this, random, ModParticles\.BIOMASS\.get\(\), count\)/,
        "Biomass stage particle intervals/counts or original volume sampling changed");
expect(biomassEntity,
        /case 2, 4, 6 -> 0\.3D;[\s\S]*?case 3 -> 0\.6D;[\s\S]*?for \(int index = 0; index < 11; index\+\+\)[\s\S]*?x = \(float\) getX\(\) \+ random\.nextFloat\(\);[\s\S]*?y = \(float\) getY\(\) \+ random\.nextFloat\(\) \+ 1\.0D;[\s\S]*?strength = 0\.5D \/ \(distance \+ 0\.1D\);[\s\S]*?level\(\)\.getRandom\(\)\.nextFloat\(\) \* level\(\)\.getRandom\(\)\.nextFloat\(\) \+ 0\.7F[\s\S]*?dy \*= strength \* \(intensity \* 0\.5D\)/,
        "Biomass hatch burst lost the original 11-particle stage-scaled trajectory");
expect(biomassEntity,
        /broadcastEntityEvent\(this, \(byte\) 7\);[\s\S]*?broadcastEntityEvent\(this, \(byte\) 18\);/,
        "Biomass hatch does not broadcast the original explosion and burst events");
expect(biomassEntity,
        /id == 7[\s\S]*?ParticleTypes\.EXPLOSION, 11[\s\S]*?id == 18[\s\S]*?spawnExplosionParticles\(\)/,
        "Biomass entity events no longer spawn the original explosion and stage-aware burst");
expect(biomassEntity,
        /level\.broadcastEntityEvent\(this, \(byte\) 18\);\s*discard\(\);/,
        "Biomass burst event is not sent immediately before entity removal");
expect(biomassParticle,
        /new SrpColorParticleOptions\(ModParticles\.DOT\.get\(\), 165, 255, 0, 0\)[\s\S]*?0\.0D, 0\.0D, 0\.0D\);[\s\S]*?new SrpColorParticleOptions\(ModParticles\.DOT\.get\(\), 60, 229, 27, 0\)[\s\S]*?0\.0D, 0\.0D, 0\.0D\)/,
        "Biomass particles no longer emit both original zero-velocity DOT particles");
expect(biomassParticle,
        /if \(random\.nextDouble\(\) <= 0\.5D\) \{\s*return 1;\s*\}[\s\S]*?if \(random\.nextDouble\(\) <= 0\.5D\) \{\s*return 2;\s*\}[\s\S]*?return random\.nextDouble\(\) <= 0\.5D \? 3 : 0;/,
        "Biomass texture selection no longer matches the original 50/25/12.5/12.5 weighting");
expect(adaptedEntity,
        /sendResidueParticles\(\) \{[\s\S]*?broadcastEntityEvent\(this, \(byte\) 13\);/,
        "Adapted residue particles are not broadcast as original entity event 13");
expect(adaptedEntity,
        /spawnResidueParticles\(\) \{[\s\S]*?GORE_SPLASH\.get\(\), 4, -1, -1, 4\), 11/,
        "Adapted residue event no longer emits 11 original vomit-family GSPLASH particles");
expect(adaptedEntity,
        /if \(id == 13\) \{[\s\S]*?spawnResidueParticles\(\)/,
        "Adapted entity event 13 no longer spawns residue particles");
for (const [name, entity] of [["Adapted", adaptedEntity], ["primitive Summoner", summonerEntity]]) {
    expect(entity,
            /spawnVomitCloud\(this, random,[\s\S]*?GORE_CLOUD\.get\(\), 123, 0, 196, 0\), 6/,
            `${name} vomit no longer emits six original purple GCLOUD particles`);
    expect(entity,
            /spawnVolume\(this, random, ModParticles\.BIOMASS\.get\(\), 11\)/,
            `${name} summon burst no longer uses 11 original BIOMASS particles`);
    if (entity.includes("ParticleTypes.WITCH")) {
        failures.push(`${name} vomit still uses vanilla Witch particles`);
    }
}
expect(entityParticles,
        /spawnVomitCloud\(LivingEntity entity[\s\S]*?baseVelocityX = -Mth\.sin\(yaw\) \* 0\.2D[\s\S]*?baseVelocityZ = Mth\.cos\(yaw\) \* 0\.2D[\s\S]*?Vec3 direction = entity\.getViewVector\(1\.0F\)[\s\S]*?entity\.getX\(\) \+ direction\.x \* 1\.2D[\s\S]*?entity\.getY\(\) \+ entity\.getEyeHeight\(\) - 0\.2D[\s\S]*?entity\.getZ\(\) \+ direction\.z \* 1\.2D[\s\S]*?velocityY = 0\.01D \+ random\.nextDouble\(\) \* 0\.1D[\s\S]*?velocityX = baseVelocityX \+ \(random\.nextDouble\(\) - 0\.5D\) \* 0\.25D[\s\S]*?velocityZ = baseVelocityZ \+ \(random\.nextDouble\(\) - 0\.5D\) \* 0\.25D/,
        "Summoner vomit trajectory or position no longer matches EntityCanra and EntityCanraAdapted");
expect(remainEntity,
        /spawnVolume\(this, random, particle, 2, getBbHeight\(\) \+ 0\.5D\)/,
        "Remain biomass event no longer uses its original extended vertical sampling range");

// Every particle needs its sprite definition.
for (const id of ["spore", "rhappy", "een", "rage", "wind", "dot", "flash", "blood"]) {
    const file = `src/main/resources/assets/csrp/particles/${id}.json`;
    if (!fs.existsSync(path.join(root, file))) {
        failures.push(`missing sprite definition ${file}`);
    }
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("particle contract ok");
