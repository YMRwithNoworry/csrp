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
const tick = method(infection, "public static void tickCoth", "/** COTH II creates an Incomplete Form");
const conversion = method(infection, "public static boolean convertCothHost", "public static boolean convertInfectedHost");

expect(infection, /COTH_REFRESH_THRESHOLD_TICKS\s*=\s*200;/,
        "COTH no longer advances in its final ten seconds");
expect(infection, /COTH_INCOMPLETE_AMPLIFIER\s*=\s*1;/,
        "COTH II amplifier marker is missing");
expect(infection, /COTH_MAX_AMPLIFIER\s*=\s*2;/,
        "COTH III amplifier marker is missing");
expect(tick, /coth\.getDuration\(\) > 0[\s\S]*coth\.getDuration\(\) <= COTH_REFRESH_THRESHOLD_TICKS/,
        "COTH expiry does not enter the stage refresh path");
expect(tick, /Math\.min\(COTH_MAX_AMPLIFIER, effectiveAmplifier \+ 1\)/,
        "COTH expiry does not advance to the next stage");
expect(tick, /forceAddEffect\(new MobEffectInstance\(ModMobEffects\.COTH, COTH_BASE_DURATION_TICKS/,
        "COTH stage refresh still relies on an in-place merge that can expire immediately");
expect(tick, /effectiveAmplifier == COTH_INCOMPLETE_AMPLIFIER\s*&&\s*belowConversionHealth[\s\S]*convertIncompleteCothHost/,
        "low-health COTH II hosts do not create an Incomplete Form");
expect(tick, /effectiveAmplifier >= COTH_MAX_AMPLIFIER[\s\S]*belowConversionHealth \|\| forceAssimilation[\s\S]*convertInfectedHost/,
        "low-health COTH III hosts do not create an Assimilated form");
expect(conversion, /getAmplifier\(\) < COTH_INCOMPLETE_AMPLIFIER[\s\S]*return false;/,
        "COTH I can incorrectly trigger health-based conversion");
expect(conversion, /getAmplifier\(\) >= COTH_MAX_AMPLIFIER[\s\S]*convertInfectedHost\(host\)/,
        "COTH III does not create an Assimilated form");
expect(conversion, /convertIncompleteCothHost\(host\)/,
        "COTH II does not create an Incomplete Form");
expect(conversion, /createIncompleteForm\(host, serverLevel\)[\s\S]*replaceHost/,
        "COTH II conversion does not use the host-sized Incomplete Form");

if (failures.length) {
    console.error("COTH progression verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("COTH progression verification passed.");
