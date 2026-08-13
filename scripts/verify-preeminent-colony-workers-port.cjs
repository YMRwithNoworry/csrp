const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const preeminent = fs.readFileSync(
  path.join(root, "src/main/java/alku/csrp/entity/PreeminentParasiteEntity.java"), "utf8");
const architect = fs.readFileSync(
  path.join(root, "src/main/java/alku/csrp/entity/ArchitectEntity.java"), "utf8");
const failures = [];
const expect = (source, pattern, message) => {
  if (!pattern.test(source)) failures.push(message);
};

for (const [name, source] of [["Architect", architect], ["Heavy Bomber", preeminent]]) {
  expect(source,
    /COLONY_WORKER_CYCLE_TICKS = 21[\s\S]{0,100}?COLONY_WORKER_CYCLE_OFFSET = 10/,
    `${name} worker deployment does not use the original 21-tick srpTicks cycle`);
  expect(source,
    /COLONY_WORKER_SEARCH_RANGE = 16\.0D[\s\S]{0,100}?MAX_NEARBY_COLONY_WORKERS = 4/,
    `${name} worker deployment range or cap differs from the original`);
  expect(source,
    /new AABB\(getX\(\), getY\(\), getZ\(\), getX\(\) \+ 1\.0D, getY\(\) \+ 1\.0D,[\s\S]{0,100}?getZ\(\) \+ 1\.0D\)\.inflate\(COLONY_WORKER_SEARCH_RANGE\)[\s\S]{0,220}?getEntitiesOfClass\(WorkerEntity\.class, searchArea\)\.size\(\)[\s\S]{0,100}?>= MAX_NEARBY_COLONY_WORKERS/,
    `${name} does not preserve the original four-worker cap within 16 blocks`);
  expect(source,
    /nearestColonyInConstructionRange\(blockPosition\(\)\)[\s\S]{0,220}?ModEntities\.WORKER\.get\(\)\.create\(/,
    `${name} worker deployment is not restricted to a colony construction range`);
  expect(source,
    /worker\.moveTo\(getX\(\), getY\(\), getZ\(\), getYRot\(\), getXRot\(\)\)[\s\S]{0,180}?worker\.setColonyTask\(colony\.pos\(\), WorkerEntity\.colonyRadius\(colony\)\)[\s\S]{0,140}?addFreshEntity\(worker\)/,
    `${name} workers do not inherit position, rotation and colony task before spawning`);
  if (source.includes("worker.finalizeSpawn(")) {
    failures.push(`${name} incorrectly applies natural-spawn initialization to summoned Workers`);
  }
}

expect(architect,
  /tickCount % COLONY_WORKER_CYCLE_TICKS == COLONY_WORKER_CYCLE_OFFSET[\s\S]{0,100}?random\.nextInt\(10\) == 0[\s\S]{0,100}?spawnColonyWorker\(serverLevel\)/,
  "Architect (EntityTenn) is missing its original 1/10 worker deployment");
expect(preeminent,
  /activeKind == Kind\.BOMBER_HEAVY[\s\S]{0,140}?tickCount % COLONY_WORKER_CYCLE_TICKS == COLONY_WORKER_CYCLE_OFFSET[\s\S]{0,100}?random\.nextInt\(7\) == 0[\s\S]{0,100}?spawnColonyWorker\(\)/,
  "Heavy Bomber (EntityJinjo) is missing its original 1/7 worker deployment");

if (/Kind\.CARRIER_COLONY \|\| activeKind == Kind\.BOMBER_HEAVY[\s\S]{0,220}?spawnColonyWorker\(\)/.test(preeminent)
    || /activeKind == Kind\.CARRIER_COLONY[\s\S]{0,180}?random\.nextInt\([^)]+\)[\s\S]{0,100}?spawnColonyWorker\(\)/.test(preeminent)) {
  failures.push("Colony Carrier (EntityVesta) incorrectly deploys Workers");
}

const tickBeforeTargetGate = preeminent.match(
  /public void tick\(\) \{([\s\S]*?)if \(target == null \|\| !target\.isAlive\(\)\) \{/)?.[1] ?? "";
if (!tickBeforeTargetGate.includes("spawnColonyWorker()")) {
  failures.push("Heavy Bomber worker deployment incorrectly requires a combat target");
}

if (failures.length) {
  console.error(`Preeminent colony worker verification failed (${failures.length}):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Architect and Heavy Bomber worker deployment is wired and verified.");
