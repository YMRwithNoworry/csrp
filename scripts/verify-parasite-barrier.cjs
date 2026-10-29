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

const block = read("src/main/java/alku/csrp/block/ParasiteBarrierBlock.java");
const blockEntity = read("src/main/java/alku/csrp/block/entity/ParasiteBarrierBlockEntity.java");
const clientTick = read("src/main/java/alku/csrp/client/ParasiteBarrierClientTick.java");
const permissions = read("src/main/java/alku/csrp/registry/ModPermissions.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const barrierItem = read("src/main/java/alku/csrp/item/ParasiteBarrierBlockItem.java");
const tests = read("src/main/java/alku/csrp/gametest/ParasiteBarrierGameTests.java");

expect(block, /implements EntityBlock/, "parasite barrier is not an entity block");
expect(block, /PermissionAPI\.getPermission\([\s\S]*?PARASITE_BARRIER_CONFIGURE\)/,
        "barrier interaction does not query its registered permission node");
expect(block, /Shapes\.empty\(\)/, "parasite barrier collision shape is not empty");
expect(block, /PushReaction\.IMMOVEABLE/, "parasite barrier piston behavior is not immovable");

expect(permissions, /new PermissionNode<>\([\s\S]*?"srpbarrier"[\s\S]*?PermissionTypes\.BOOLEAN/,
        "the original srpbarrier permission node is missing");
expect(permissions, /PermissionGatherEvent\.Nodes[\s\S]*?event\.addNodes\(PARASITE_BARRIER_CONFIGURE\)/,
        "the parasite barrier permission node is not registered");
expect(permissions, /Permissions\.COMMANDS_GAMEMASTER/,
        "the permission node default does not match command permission level 2");

expect(blockEntity, /MIN_RADIUS = 1/, "minimum barrier radius is missing");
expect(blockEntity, /MAX_RADIUS = 10/, "maximum barrier radius is missing");
expect(blockEntity, /output\.putInt\("centerChunkX", centerChunkX\)/,
        "the original centerChunkX NBT key is not written");
expect(blockEntity, /output\.putInt\("centerChunkZ", centerChunkZ\)/,
        "the original centerChunkZ NBT key is not written");
expect(blockEntity, /output\.putInt\("radiusChunks", radiusChunks\)/,
        "the original radiusChunks NBT key is not written");
expect(blockEntity, /input\.getIntOr\(legacyKey, input\.getIntOr\(temporaryKey, fallback\)\)/,
        "legacy and transitional radius/center NBT keys are not both readable");
expect(blockEntity, /mob\.getNavigation\(\)\.stop\(\)[\s\S]*?mob\.setTarget\(null\)[\s\S]*?mob\.setLastHurtByMob\(null\)/,
        "path intersections do not stop navigation and clear an in-field target");
expect(blockEntity, /TELEPORT_AFTER_TICKS = 100/, "the 100-tick repeated-ejection fallback is missing");

expect(clientTick, /Minecraft\.getInstance\(\)\.player/,
        "barrier particles are not based on the local client player");
expect(clientTick, /getMainHandItem\(\)[\s\S]*?getOffhandItem\(\)/,
        "barrier particle activation does not check both hands");
expect(clientTick, /ParticleTypes\.BLOCK_MARKER[\s\S]*?Blocks\.BARRIER/,
        "modern barrier marker particles are missing");
expect(items, /"parasite_barrier", properties -> new ParasiteBarrierBlockItem/,
        "parasite barrier is not registered with its specialized item");
expect(barrierItem, /tooltip\.csrp\.parasite_barrier\.creative_only[\s\S]*?ChatFormatting\.RED/,
        "the creative-only tooltip line is missing");
expect(barrierItem, /tooltip\.csrp\.parasite_barrier\.desc1[\s\S]*?ChatFormatting\.WHITE/,
        "the first white barrier description line is missing");
expect(barrierItem, /tooltip\.csrp\.parasite_barrier\.desc2[\s\S]*?ChatFormatting\.WHITE/,
        "the second white barrier description line is missing");

expect(tests, /saved\.contains\("centerChunkX"\)[\s\S]*?saved\.contains\("radiusChunks"\)/,
        "the barrier GameTest does not check legacy NBT serialization");
expect(tests, /parasite_barrier\/parasite_ejection/,
        "the parasite-ejection GameTest is not registered");
expect(tests, /barrier should eject a parasite beyond its bounds/,
        "the parasite-ejection GameTest assertion is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("parasite barrier contract ok");
