const fs = require("node:fs");
const path = require("node:path");

// Guards every texture the mod's own code binds.  A renderer that points at a texture nobody shipped
// draws the missing-texture checkerboard in game and produces no resource-load warning, so this class
// of bug (e.g. the Kirin orb boom binding a never-shipped `scary_orb.png`) is otherwise only visible
// to a player.  Vanilla-namespaced textures are skipped: they belong to the game, not to this mod.
const root = path.resolve(__dirname, "..");
const ASSETS = path.join(root, "src/main/resources/assets/csrp");
const failures = [];

function walk(dir, out = []) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) walk(full, out);
        else if (entry.name.endsWith(".java")) out.push(full);
    }
    return out;
}

// Identifier.fromNamespaceAndPath(Csrp.MODID, "textures/...") — the path may sit on the next line.
const REFERENCE = /Identifier\.fromNamespaceAndPath\(\s*(Csrp\.MODID|"([a-z_]+)")\s*,\s*"([^"]+\.(?:png|jpg))"/g;

let checked = 0;
for (const file of walk(path.join(root, "src/main/java"))) {
    const source = fs.readFileSync(file, "utf8");
    const relative = path.relative(root, file).replace(/\\/g, "/");
    let match;
    REFERENCE.lastIndex = 0;
    while ((match = REFERENCE.exec(source))) {
        const namespace = match[1] === "Csrp.MODID" ? "csrp" : match[2];
        if (namespace !== "csrp") continue;
        checked++;
        const asset = match[3];
        if (!fs.existsSync(path.join(ASSETS, asset))) {
            failures.push(`${relative} binds csrp:${asset}, which does not exist under assets/csrp/`);
        }
    }
}

// Sound files are bound the same way and fail just as silently.
const SOUND_REFERENCE = /Identifier\.fromNamespaceAndPath\(\s*(Csrp\.MODID|"([a-z_]+)")\s*,\s*"([^"]+\.ogg)"/g;
let soundsChecked = 0;
for (const file of walk(path.join(root, "src/main/java"))) {
    const source = fs.readFileSync(file, "utf8");
    const relative = path.relative(root, file).replace(/\\/g, "/");
    let match;
    SOUND_REFERENCE.lastIndex = 0;
    while ((match = SOUND_REFERENCE.exec(source))) {
        const namespace = match[1] === "Csrp.MODID" ? "csrp" : match[2];
        if (namespace !== "csrp") continue;
        soundsChecked++;
        if (!fs.existsSync(path.join(ASSETS, match[3]))) {
            failures.push(`${relative} binds csrp:${match[3]}, which does not exist under assets/csrp/`);
        }
    }
}

// The orb textures the Kirin chain depends on must stay resolvable.
for (const asset of ["textures/entity/orbscary.png", "textures/entity/orbscary_armor.png",
    "textures/entity/orbboom.png", "textures/entity/orbboom_armor.png",
    "textures/entity/layer/cosmichasking.png"]) {
    if (!fs.existsSync(path.join(ASSETS, asset))) {
        failures.push(`missing Kirin/orb texture ${asset}`);
    }
}

// Vanilla relocates textures between versions (26.3 moved textures/entity/guardian_beam.png into
// textures/entity/guardian/), and a stale path silently renders the missing-texture checkerboard.
// Check the mod's vanilla-namespaced bindings against the Minecraft jar when one is available.
const MINECRAFT_JAR = "D:/MC/gradle-spd-neoforge/caches/neoformruntime/artifacts/minecraft_26.3_client.jar";
let vanillaChecked = 0;
if (fs.existsSync(MINECRAFT_JAR)) {
    const { execFileSync } = require("node:child_process");
    let listing = "";
    try {
        listing = execFileSync("tar", ["-tf", MINECRAFT_JAR], { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
    } catch (error) {
        listing = "";
    }
    if (listing) {
        const vanilla = new Set(listing.split(/\r?\n/).filter((line) => line.startsWith("assets/minecraft/"))
                .map((line) => line.replace(/^assets\/minecraft\//, "")));
        const VANILLA_REFERENCE =
                /Identifier\.withDefaultNamespace\(\s*"([^"]+\.(?:png|jpg))"/g;
        for (const file of walk(path.join(root, "src/main/java"))) {
            const source = fs.readFileSync(file, "utf8");
            const relative = path.relative(root, file).replace(/\\/g, "/");
            let match;
            VANILLA_REFERENCE.lastIndex = 0;
            while ((match = VANILLA_REFERENCE.exec(source))) {
                vanillaChecked++;
                if (!vanilla.has(match[1])) {
                    failures.push(`${relative} binds minecraft:${match[1]}, which does not exist in the 26.3 client jar`);
                }
            }
        }
    }
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log(`texture references ok (${checked} mod textures, ${soundsChecked} sounds, ${vanillaChecked} vanilla textures)`);
