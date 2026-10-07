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

const golem = read("src/main/java/alku/csrp/entity/HiGolemEntity.java");
const mount = read("src/main/java/alku/csrp/event/HiGolemMountEvents.java");

// Original EntityHiGolem#chargeMoving (out109 370-376): grab a target within 9 blocks.
expect(golem, /getPassengers\(\)\.isEmpty\(\) && distanceToSqr\(target\) <= 9\.0D/,
        "the 9-block grab gate is missing");
expect(golem, /target\.startRiding\(HiGolemEntity\.this, true, true\)/,
        "the forced ride is missing");
// Original EntityHiGolem#func_184232_k: slowness on self, damage to the passenger, 1.5-block offset.
expect(golem, /addEffect\(new MobEffectInstance\(MobEffects\.SLOWNESS, 20, 200, false, false\)\)/,
        "the original slowness amplifier 200 is missing");
expect(golem, /tickCount % 20 != 10/,
        "the original srpTicks == 10 passenger damage cadence is missing");
expect(golem, /doHurtTarget\(\(net\.minecraft\.server\.level\.ServerLevel\) level\(\), living\)/,
        "the passenger damage is missing");
expect(golem, /getX\(\) \+ look\.x \* 1\.5D/,
        "the original 1.5-block passenger offset is missing");
expect(golem, /positionRider\(Entity passenger, Entity\.MoveFunction moveFunction\)/,
        "the positionRider override is missing");

// Original SRPEventHandlerBus#onEntityMount (out109 1374-1386).
expect(mount, /EntityMountEvent event/,
        "the mount event handler is missing");
expect(mount, /event\.isDismounting\(\)/,
        "the dismount check is missing");
expect(mount, /event\.getEntityBeingMounted\(\) instanceof HiGolemEntity/,
        "the HiGolem mount check is missing");
expect(mount, /player\.getAbilities\(\)\.instabuild/,
        "the creative exemption is missing");
expect(mount, /player\.getHealth\(\) > 0\.0F/,
        "the original alive check is missing");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("hi golem grab contract ok");
