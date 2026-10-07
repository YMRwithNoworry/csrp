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

const events = read("src/main/java/alku/csrp/event/DeadBloodConversionEvents.java");

// Original SRPEventHandlerBus#onNeighborNotify (out109 472-570).
expect(events, /class DeadBloodConversionEvents/,
        "DeadBloodConversionEvents is missing");
expect(events, /BlockEvent\.NeighborNotifyEvent event/,
        "the handler no longer listens to NeighborNotifyEvent");
expect(events, /private static boolean soakGuard/,
        "the original srpSoakGuard re-entrancy guard is missing");
// water -> parasitestain metadata 1 (MUD); lava -> parasiterubble metadata 7 (OBSIDIAN).
expect(events, /ParasiteStainBlock\.Variant\.MUD/,
        "water no longer converts to parasitestain variant 1 (MUD)");
expect(events, /ParasiteRubbleBlock\.Variant\.OBSIDIAN/,
        "lava no longer converts to parasiterubble variant 7 (OBSIDIAN)");
expect(events, /state\.is\(Blocks\.SPONGE\)[\s\S]*convertSponge/,
        "the sponge conversion branch is missing");
expect(events, /ModBlocks\.DISEASED_SPONGE\.get\(\)\.defaultBlockState\(\)/,
        "the sponge no longer becomes a Diseased Sponge");
expect(events, /getFluidState\(pos\)\.getType\(\) instanceof DeadBloodFluid/,
        "Dead Blood is no longer detected through its fluid type");
expect(events, /touchesDeadBlood\(level, pos\)/,
        "the touching-Dead-Blood check is missing");
// The Dead Blood branch must convert all four horizontal/vertical neighbours.
expect(events, /for \(Direction direction : Direction\.values\(\)\)/,
        "the Dead Blood neighbour loop is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("dead blood conversion contract ok");
