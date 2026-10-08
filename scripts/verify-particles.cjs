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

// Original SRPEnumParticle (out109 client/particle/SRPEnumParticle.java): fog, spore, gcloud,
// gsplash, rhappy, biomass, een, flash, dot, wind, coolerfog, rage, blood.
for (const id of ["spore", "rhappy", "een", "rage", "wind", "dot", "flash", "blood"]) {
    expect(registry, new RegExp(`register\\("${id}"`), `particle type ${id} is not registered`);
}
for (const provider of ["SporeParticle", "RHappyParticle", "EenParticle", "RageParticle",
        "WindParticle", "DotParticle", "FlashParticle", "BloodParticle"]) {
    expect(client, new RegExp(`${provider}\\.Provider::new`),
            `${provider} is not registered as a particle provider`);
}

// The coloured payload must survive the network round trip.
const options = read("src/main/java/alku/csrp/particle/SrpColorParticleOptions.java");
expect(options, /RecordCodecBuilder\.mapCodec/, "the colour particle codec is missing");
expect(options, /StreamCodec\.composite/, "the colour particle stream codec is missing");
expect(options, /public ParticleType<\?> getType\(\)/, "the colour particle getType is missing");

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
