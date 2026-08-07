const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");

const carrier = read("src/main/java/alku/csrp/entity/CarrierEntity.java");
const feralEnderman = read("src/main/java/alku/csrp/entity/FeralEndermanEntity.java");

if (/INFESTED_REMAINS|spreadResidue|placeResidueAtFloor/.test(carrier)) {
  failures.push("Carrier detonation still places a block after death");
}

if (!feralEnderman.includes("damaged && isAlive() && !level().isClientSide")) {
  failures.push("Feral Enderman lethal hits can still run the surviving-hurt residue branch");
}

const deathBurstStart = feralEnderman.indexOf("private void spawnDeathBurst()");
const deathBurstEnd = feralEnderman.indexOf("private void placeFeralRemains", deathBurstStart);
const deathBurst = deathBurstStart >= 0 && deathBurstEnd > deathBurstStart
  ? feralEnderman.slice(deathBurstStart, deathBurstEnd) : "";
if (!deathBurst) {
  failures.push("Feral Enderman death burst could not be inspected");
} else if (deathBurst.includes("placeFeralRemains") || deathBurst.includes("INFESTED_REMAINS")) {
  failures.push("Feral Enderman death burst still places a block");
}

if (!feralEnderman.includes("placeFeralRemains(blockPosition())")) {
  failures.push("Feral Enderman surviving-hurt residue ability was removed instead of only its death path");
}

if (failures.length) {
  console.error("Parasite death-block verification failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Verified that parasite deaths no longer place blocks.");
