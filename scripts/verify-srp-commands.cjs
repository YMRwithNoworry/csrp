const fs = require("node:fs");
const path = require("node:path");

// Command-surface parity with 1.10.9.  The original registered 18 commands from CommonProxy plus
// util/CommandExtremeSnow; this script pins the ones that exist so they cannot silently disappear, and
// records the four that are still missing (they are tracked in PORT_BACKLOG.md with full specs).
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

const srp = read("src/main/java/alku/csrp/command/SrpCommands.java");
const compendium = read("src/main/java/alku/csrp/compendium/CompendiumCommands.java");
const celestial = read("src/main/java/alku/csrp/celestial/CelestialCommands.java");
const distortion = read("src/main/java/alku/csrp/command/GuiDistortionCommand.java");

// SRPCommandRoot / Evolution / Origin / Node / Colony / Dislodgment / Generation / UDevelopment.
for (const builder of ["srParasites", "srEvolution", "srGeneration", "srUDevelopment", "srNodes",
    "srColonies", "srVectors", "srDislodgment", "srDifficulty", "dqq", "srHelp", "srSummonNidus",
    "srMeteor"]) {
    expect(srp, new RegExp(`dispatcher\\.register\\(${builder}\\(\\)\\)`),
            `SrpCommands no longer registers ${builder}()`);
}
// SRPCommandGuiDistortion plus its two aliases.
expect(srp, /dispatcher\.register\(GuiDistortionCommand\.command\(\)\)/, "srpguidistortion is not registered");
expect(distortion, /NAME = "srpguidistortion"/, "the distortion command name changed");
expect(distortion, /List\.of\("srpguidist", "srpdistortion"\)/, "the distortion aliases changed");

// CommandSRPHelp / CommandSRPGuide / CommandSRPGuideClear / SRPCommandBestiaryStats.
expect(compendium, /Commands\.literal\("srpguide"\)/, "srpguide is not registered");
expect(compendium, /clearGuideCommand\(\)/, "srpguideclear is not registered");
expect(compendium, /statsCommand\("srpbestiarystats", true\)/, "srpbestiarystats is not registered");

// CommandForceCelestial (registered as /srp_celestial) and its subcommands.  The original's
// <list|debug|clear|all|dark_days|dark_days_end|id>; dark_days is covered by the per-id toggle because
// CelestialCatalog defines it.
expect(celestial, /Commands\.literal\("srp_celestial"\)/, "srp_celestial is not registered");
for (const sub of ["list", "clear", "all", "debug", "dark_days_end"]) {
    expect(celestial, new RegExp(`Commands\\.literal\\("${sub}"\\)`),
            `srp_celestial lost its ${sub} subcommand`);
}
expect(celestial, /Component\.literal\("\[SERVER\] dim="/,
        "srp_celestial debug no longer prints the server state line");
expect(celestial, /"\[SERVER\] active="/, "srp_celestial debug no longer lists the active ids");
expect(celestial, /"\[SERVER\] forced="/, "srp_celestial debug no longer lists the forced ids");
expect(celestial, /darkDaysEndTime=/, "srp_celestial debug no longer reports darkDaysEndTime");
expect(celestial, /darkDaysEndingSoundPlayed=/, "srp_celestial debug no longer reports the ending-sound flag");
const catalog = read("src/main/java/alku/csrp/celestial/CelestialCatalog.java");
expect(catalog, /"dark_days"/, "CelestialCatalog lost the dark_days definition that /srp_celestial dark_days needs");

// The four commands still missing, so a future pass can see at a glance that the gap is known.
const backlog = read("PORT_BACKLOG.md");
for (const name of ["srp_extremesnow", "harlequin_here", "harlequin_convert", "harlequin_scatter"]) {
    if (!backlog.includes(name)) {
        failures.push(`${name} is neither implemented nor recorded in PORT_BACKLOG.md`);
    }
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("srp command surface ok");
