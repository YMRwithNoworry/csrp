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

const block = read("src/main/java/alku/csrp/block/DermoidCystBlock.java");
const blockEntity = read("src/main/java/alku/csrp/block/entity/DermoidCystBlockEntity.java");
const registry = read("src/main/java/alku/csrp/registry/ModBlockEntities.java");
const tests = read("src/main/java/alku/csrp/gametest/DermoidCystGameTests.java");

expect(block, /implements EntityBlock/, "Dermoid Cyst is not an entity block");
expect(block, /new DermoidCystBlockEntity\(pos, state\)/,
        "Dermoid Cyst block entity construction is missing");
expect(block, /player\.openMenu\(cyst\)/, "Dermoid Cyst menu opening is missing");
expect(block, /AbstractContainerMenu\.getRedstoneSignalFromContainer\(cyst\)/,
        "Dermoid Cyst comparator output is missing");
expect(block, /Containers\.updateNeighboursAfterDestroy\(state, level, pos\)/,
        "Dermoid Cyst comparator neighbour update on removal is missing");
expect(block, /ModSounds\.get\("flesh\.growl"\)/,
        "Dermoid Cyst removal growl is missing");

expect(blockEntity, /CONTAINER_SIZE = 27/, "the original 27-slot size is missing");
expect(blockEntity, /getMaxStackSize\(\)[\s\S]*?return 64;/,
        "the original stack limit of 64 is missing");
expect(blockEntity, /ChestMenu\.threeRows\(containerId, inventory, this\)/,
        "the 3-row chest menu is missing");
expect(blockEntity, /ContainerHelper\.saveAllItems\(output, items\)/,
        "Dermoid Cyst inventory saving is missing");
expect(blockEntity, /ContainerHelper\.loadAllItems\(input, items\)/,
        "Dermoid Cyst inventory loading is missing");
expect(blockEntity, /OPENER_RECHECK_INTERVAL = 80/, "the 80-tick opener check is missing");
expect(blockEntity, /OPENER_RANGE = 5\.0D/, "the 5-block opener range is missing");
expect(blockEntity, /playFleshGrowl\(\)/, "Dermoid Cyst open/close growl calls are missing");
expect(blockEntity, /isNamedBoris\(\)/, "Dermoid Cyst Boris name handling is missing");

expect(registry,
        /DERMOID_CYST\s*=\s*BLOCK_ENTITIES\.register\("dermoid_cyst"[\s\S]*?DermoidCystBlockEntity::new[\s\S]*?legacyBlock\("dermoid_cyst"\)/,
        "Dermoid Cyst block entity registration is missing");
expect(tests, /getContainerSize\(\) == 27/, "the GameTest does not check the 27-slot capacity");
expect(tests, /getMaxStackSize\(\) == 64/, "the GameTest does not check the 64-item stack limit");
expect(tests, /getRedstoneSignalFromContainer\(cyst\) == 15/,
        "the GameTest does not check full-container comparator output");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("dermoid cyst container contract ok");
