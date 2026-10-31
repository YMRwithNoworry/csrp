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
const evolution = read("src/main/java/alku/csrp/world/EvolutionSystem.java");
const nexus = read("src/main/java/alku/csrp/entity/NexusParasiteEntity.java");
const primitive = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
const dredge = read("src/main/java/alku/csrp/entity/DredgeEntity.java");
const adapted = read("src/main/java/alku/csrp/entity/AdaptedVariantEntity.java");

// ---- R6 gap: the three missing generation genes (SRPConfigSystems generation*0..5) ----
// Original arrays (out109 util/config/SRPConfigSystems.java:1448-1500 etc.):
//   generationLookWalls  = false,false,false,false,true,true
//   generationWaterLeap  = false,false,false,true,true,true
//   generationResidue    = false,false,false,false,true,true
expect(evolution, /GENERATION_LOOK_WALLS = \{false, false, false, false, true, true\}/,
        "generationLookWalls table no longer matches the original defaults");
expect(evolution, /GENERATION_WATER_LEAP = \{false, false, false, true, true, true\}/,
        "generationWaterLeap table no longer matches the original defaults");
expect(evolution, /GENERATION_RESIDUE = \{false, false, false, false, true, true\}/,
        "generationResidue table no longer matches the original defaults");
expect(evolution, /boolean lookWalls, boolean waterLeap, boolean residue,/,
        "GenerationProfile no longer exposes lookWalls/waterLeap/residue");

// gene 2 (lookWalls) must gate target acquisition the way
// `shouldCheckSight || !getGeneMod(2)` did.
expect(primitive, /EvolutionSystem\.generationProfile\(serverLevel, this\)\.lookWalls\(\) \|\| hasLineOfSight\(target\)/,
        "the base target goal no longer applies the lookWalls gene to its sight check");

// gene 4 (waterLeap) must gate the liquid leap in handleWater.
expect(dredge, /!EvolutionSystem\.generationProfile\(serverLevel, this\)\.waterLeap\(\)/,
        "the liquid leap no longer honours the waterLeap gene");

// gene 8 (residue) must gate EntityAIBlockResidue.
expect(adapted, /EvolutionSystem\.generationProfile\(serverLevel, AdaptedVariantEntity\.this\)\.residue\(\)/,
        "the block-residue goal no longer honours the residue gene");
expect(adapted, /EvolutionSystem\.generationProfile\(serverLevel, this\)\.ordinaryOrb\(\)/,
        "ordinary orbs no longer honour the entity-specific gene");

// ---- R6 gap: EntityAINexusGrow config gates ----
// Original SRPConfigSystems: evolutionNests (11), deveNestsUse (3), maximumStageList {"1;3"}.
expect(config, /"evolutionNests", 11, -1, 11/,
        "evolutionNests default of 11 is missing");
expect(config, /"deveNestsUse", 3, 0, 4/,
        "deveNestsUse default of 3 is missing");
expect(config, /\.defineList\("maximumStageList", List\.of\("1;3"\)/,
        "maximumStageList default of {\"1;3\"} is missing");
expect(config, /public static int nexusStageLock\(Level level\)/,
        "the nexusStageLock accessor is missing");
expect(config, /public static int legacyDimensionId\(Level level\)/,
        "the legacy dimension-id mapping is missing");
expect(config, /if \(level\.dimension\(\) == Level\.END\) \{\s*return 1;/s,
        "legacy dimension id 1 (End) is no longer mapped");
expect(nexus, /dimensionStageLock >= activeKind\.stage \+ 1/,
        "the dimension stage lock is no longer applied in evolve()");
expect(nexus, /data\.evolutionPhase\(\) < Config\.evolutionNests\(\)/,
        "the nest phase gate is missing from trySpawnRooterSi");
expect(nexus, /data\.ubiquitousDevelopmentOverride\(\) < Config\.deveNestsUse\(\)/,
        "the nest development gate is missing from trySpawnRooterSi");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("generation genes + nexus grow gate contract ok");
