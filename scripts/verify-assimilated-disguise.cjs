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

function method(content, start, end) {
    const startIndex = content.indexOf(start);
    const endIndex = content.indexOf(end, startIndex + start.length);
    return startIndex >= 0 && endIndex > startIndex ? content.slice(startIndex, endIndex) : "";
}

const infection = read("src/main/java/alku/csrp/infection/InfectionMechanics.java");
const infectionEvents = read("src/main/java/alku/csrp/infection/InfectionEvents.java");
const evolutionEvents = read("src/main/java/alku/csrp/world/EvolutionEvents.java");
const tickCoth = method(infection, "public static void tickCoth", "public static boolean convertInfectedHost");
const restore = method(infection, "public static boolean tryRestoreAssimilatedDisguise",
        "/** Restores the original host as a COTH-infected disguise. */");
const hiddenTick = method(infection, "public static void tickHiddenAssimilated",
        "/** Restores an idle host-backed Assimilated form before the phase-nine dehiding threshold. */");
const disguise = method(infection, "public static boolean disguiseAssimilated",
        "/** Recreates the exact Assimilated body saved by a disguise without awarding conversion points. */");
const reveal = method(infection, "public static boolean revealHiddenAssimilated",
        "private static LivingEntity hiddenAssimilatedThreat");

expect(infection, /ASSIMILATED_UNHIDE_HEALTH_FRACTION\s*=\s*0\.30F;/,
        "Assimilated disguise threshold is not the original strict 30 percent");
expect(infection, /COTH_CONVERSION_HEALTH_FRACTION\s*=\s*0\.35F;/,
        "ordinary COTH conversion threshold was changed by the disguise implementation");
expect(tickCoth, /getHealth\(\)[\s\S]*<=\s*entity\.getMaxHealth\(\)\s*\*\s*COTH_CONVERSION_HEALTH_FRACTION/,
        "ordinary COTH conversion no longer retains its existing 35 percent boundary");
expect(hiddenTick, /getHealth\(\)[\s\S]*<\s*disguise\.getMaxHealth\(\)\s*\*\s*ASSIMILATED_UNHIDE_HEALTH_FRACTION/,
        "hidden Assimilated forms do not reveal strictly below 30 percent health");
expect(hiddenTick, /evolutionPhase\(\)\s*>=\s*ASSIMILATION_DEHIDE_PHASE[\s\S]*\|\|\s*\(threat != null/,
        "phase-nine reveal or threatened low-health reveal condition is missing");
expect(infection, /HIDDEN_ASSIMILATED_TAG\s*=\s*"csrp_hidden_assimilated"/,
        "disguised hosts do not remember their exact Assimilated form");
expect(restore, /evolutionPhase\(\)\s*>=\s*ASSIMILATION_DEHIDE_PHASE/,
        "phase nine does not prevent disguise restoration");
expect(restore, /isAssimilatedBody\(assimilated\)/,
        "non-Assimilated entities can enter the disguise restoration path");
expect(restore, /contains\(ASSIMILATION_HOST_TAG\)/,
        "naturally spawned Assimilated entities can disguise without an original host");
expect(restore, /mob\.getTarget\(\) != null\s*&&\s*mob\.getTarget\(\)\.isAlive\(\)/,
        "Assimilated entities can restore their disguise while fighting");
expect(restore, /disguiseAssimilated\(assimilated, true\)/,
        "automatic disguise restoration does not use the passive-host filter");
expect(disguise, /automatic\s*&&\s*disguise instanceof Monster/,
        "hostile original hosts can incorrectly regain an automatic disguise");
expect(disguise, /putString\(HIDDEN_ASSIMILATED_TAG,[\s\S]*getKey\(assimilated\.getType\(\)\)/,
        "the exact Assimilated entity id is not persisted on the disguise");
expect(disguise, /healthFraction[\s\S]*disguise\.setHealth/,
        "disguise restoration does not preserve relative health");
expect(reveal, /getOptional\(assimilatedId\)[\s\S]*type\.create\(level\)/,
        "reveal does not recreate the exact saved Assimilated entity type");
expect(reveal, /healthFraction[\s\S]*converted\.setHealth/,
        "reveal does not preserve relative health");
expect(reveal, /converted\.setTarget\(livingAttacker\)/,
        "a revealed Assimilated entity does not retaliate against its attacker");
expect(infectionEvents, /revealHiddenAssimilated\(host, attacker\)[\s\S]*event\.setCanceled\(true\)/,
        "lethal damage does not reveal a disguised Assimilated entity before death");
expect(infectionEvents, /isHiddenAssimilated\(event\.getEntity\(\)\)[\s\S]*attacker instanceof Parasite[\s\S]*event\.setCanceled\(true\)/,
        "parasites can damage an allied disguised Assimilated entity");
expect(infectionEvents, /(?:getNewAboutToBeSetTarget\(\)|getNewTarget\(\))[\s\S]*isHiddenAssimilated\(target\)/,
        "parasites can target an allied disguised Assimilated entity");
expect(evolutionEvents, /tickCount % 20 == 0\s*&&\s*InfectionMechanics\.tryRestoreAssimilatedDisguise\(entity\)/,
        "idle Assimilated entities are not periodically offered disguise restoration");
expect(evolutionEvents, /tickCount % 20 == 0\s*&&\s*InfectionMechanics\.isHiddenAssimilated\(entity\)[\s\S]*tickHiddenAssimilated\(entity\)/,
        "hidden Assimilated state is not checked independently of the COTH effect");
if (/VALUE_COTH|PointSource\.COTH/.test(reveal)) {
    failures.push("revealing an existing Assimilated entity awards duplicate evolution points");
}

if (failures.length) {
    console.error("Assimilated disguise verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Assimilated disguise verification passed.");
