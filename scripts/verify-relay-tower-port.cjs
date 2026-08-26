const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];
const read = (relative) => {
    const file = path.join(root, relative);
    if (!fs.existsSync(file)) {
        failures.push(`missing ${relative}`);
        return "";
    }
    return fs.readFileSync(file, "utf8");
};
const exists = (relative) => fs.existsSync(path.join(root, relative));
const expect = (source, needle, label) => {
    if (!source.includes(needle)) failures.push(`${label}: missing ${needle}`);
};
const json = (relative) => {
    const source = read(relative);
    if (!source) return null;
    try {
        return JSON.parse(source);
    } catch (error) {
        failures.push(`${relative}: invalid JSON (${error.message})`);
        return null;
    }
};
const png = (relative) => {
    const file = path.join(root, relative);
    if (!fs.existsSync(file)) {
        failures.push(`missing ${relative}`);
        return;
    }
    if (fs.readFileSync(file).subarray(0, 8).toString("hex") !== "89504e470d0a1a0a") {
        failures.push(`${relative}: invalid PNG signature`);
    }
};

const kinds = [
    "inborn", "assimilated", "assimara", "hijacked", "feral", "crude", "primitive",
    "adapted", "nexus", "deterrent", "pure", "preeminent", "ancient", "derived",
    "desmoid", "eschar", "resistance", "ideal", "origin", "phase", "vectors", "dislodgement"
];
const blocks = read("src/main/java/alku/csrp/registry/ModBlocks.java");
const blockEntities = read("src/main/java/alku/csrp/registry/ModBlockEntities.java");
const items = read("src/main/java/alku/csrp/registry/ModItems.java");
const menus = read("src/main/java/alku/csrp/registry/ModMenus.java");
const terminal = read("src/main/java/alku/csrp/block/entity/RelayTerminalBlockEntity.java");
const menu = read("src/main/java/alku/csrp/inventory/RelayTerminalMenu.java");
const screen = read("src/main/java/alku/csrp/client/screen/RelayTerminalScreen.java");
const factory = read("src/main/java/alku/csrp/relay/RelayScanReportFactory.java");
const report = read("src/main/java/alku/csrp/item/RelayReportItem.java");
const payloads = read("src/main/java/alku/csrp/compendium/network/CompendiumPayloads.java");
const relayPayload = read("src/main/java/alku/csrp/relay/network/RelayReportOpenPayload.java");
const network = read("src/main/java/alku/csrp/network/CsrpNetwork.java");
const nodeLamp = read("src/main/java/alku/csrp/block/NodeLampBlock.java");

for (const id of ["relay_base", "relay_middle", "relay_roof", "semiorganic_block", "node_redstone_lamp"]) {
    expect(blocks, `"${id}"`, "block registry");
    expect(items, `"${id}"`, "block item registry");
    json(`src/main/resources/assets/csrp/blockstates/${id}.json`);
    json(`src/main/resources/assets/csrp/models/block/${id}.json`);
    json(`src/main/resources/assets/csrp/models/item/${id}.json`);
    json(`src/main/resources/data/csrp/loot_table/blocks/${id}.json`);
}
expect(blockEntities, "RelayTerminalBlockEntity::new", "relay block entity registration");
expect(menus, "RelayTerminalMenu::new", "relay menu registration");
expect(screen, "handleInventoryButtonClick", "relay scan button");
if (!payloads.includes("RelayReportOpenPayload.STREAM_CODEC")
    && !(relayPayload.includes("FriendlyByteBuf") && relayPayload.includes("static RelayReportOpenPayload decode")
      && network.includes("RelayReportOpenPayload.class"))) {
    failures.push("report open payload registration: missing Forge FriendlyByteBuf codec");
}

for (const marker of [
    "SCAN_TICKS = 110", "COOLDOWN_TICKS = 400", "nextScanTick = serverLevel.getGameTime() + COOLDOWN_TICKS",
    "RelayScanReportFactory.createReports", "relay.scan.activate", "relay.paper.output",
    "tag.putUUID(\"ScanPlayer\"", "tag.putString(\"ScanKind\""
]) expect(terminal, marker, "relay scan lifecycle");
for (const marker of ["RelayModuleItem", "!isScanning()", "SCAN_BUTTON", "relay.startScan(serverPlayer)"]) {
    expect(menu, marker, "relay inventory contract");
}

for (const kind of kinds) {
    expect(items, `"module_${kind}"`, `${kind} module registry`);
    json(`src/main/resources/assets/csrp/models/item/module_${kind}.json`);
    png(`src/main/resources/assets/csrp/textures/item/module_${kind}.png`);
    json(`src/main/resources/data/csrp/recipes/module_${kind}.json`);
}
for (const marker of [
    "case PHASE", "case VECTORS", "case DISLODGEMENT", "VECTOR_HALF_RANGE = 2_500",
    "TotalParasites", "ShareTenths", "GenerationTicks", "VectorY", "Health", "Events",
    "List.of(Tier.values())", "activeDislodgmentCodes(level)"
]) expect(factory, marker, "relay report factory");
for (const tier of [
    "INBORN", "ASSIMILATED", "ASSIMARA", "HIJACKED", "FERAL", "CRUDE", "PRIMITIVE",
    "ADAPTED", "NEXUS", "DETERRENT", "PURE", "PREEMINENT", "DERIVED", "ANCIENT"
]) expect(factory, `${tier}("${tier.toLowerCase()}"`, `${tier} scan profile`);

for (const id of ["relay_scan_report", "phase_report", "vector_map", "dislodgement_report"]) {
    expect(items, `"${id}"`, `${id} item registry`);
    json(`src/main/resources/assets/csrp/models/item/${id}.json`);
}
for (const marker of [
    "addScanLines", "addPhaseLines", "addVectorLines",
    "addDislodgementLines", "tooltip.csrp.relay_report.printed"
]) expect(report, marker, "report reader");
if (!report.includes("PacketDistributor.sendToPlayer") && !report.includes("CsrpNetwork.sendToPlayer")) {
    failures.push("report reader: missing Forge player packet dispatch");
}

for (const recipe of [
    "semiorganic_block", "semiorganic_block_undo", "node_redstone_lamp",
    "relay_base", "relay_middle", "relay_roof"
]) json(`src/main/resources/data/csrp/recipes/${recipe}.json`);
for (const texture of [
    "node_redstone_lamp", "semiorganic_block", "relay_bottom_top", "relay_bottom_bottom",
    "relay_bottom_side", "relay_middle_top", "relay_middle_bottom", "relay_middle_side",
    "relay_top_top", "relay_top_bottom", "relay_top_side"
]) png(`src/main/resources/assets/csrp/textures/block/${texture}.png`);
for (const texture of ["phase_report", "vector_map", "dislodgement_report"]) {
    png(`src/main/resources/assets/csrp/textures/item/${texture}.png`);
}
const deadBlood = read("src/main/java/alku/csrp/item/DeadBloodFluidItem.java");
for (const marker of ["DURATION_TICKS = 600", "ModMobEffects.VIRAL, DURATION_TICKS, 1",
    "Items.GLASS_BOTTLE", "UseAnim.DRINK"]) expect(deadBlood, marker, "Dead Blood Fluid behavior");
expect(items, "\"deadblood_fluid\"", "Dead Blood Fluid item registry");
json("src/main/resources/assets/csrp/models/item/deadblood_fluid.json");
png("src/main/resources/assets/csrp/textures/item/deadblood_fluid.png");

for (const marker of [
    "RANGE = 250.0D", "level.scheduleTick(pos, this, 100)", "RANGE_LEVEL) * 3",
    "distanceSquared <= 2_500.0D", "distanceSquared <= 40_000.0D",
    "dispatcher_si", "dispatcher_siv", "message.csrp.node_lamp.strength"
]) expect(nodeLamp, marker, "node lamp behavior");

for (const language of ["en_us", "zh_cn"]) {
    const lang = json(`src/main/resources/assets/csrp/lang/${language}.json`);
    if (!lang) continue;
    for (const key of [
        "container.csrp.relay_terminal", "message.csrp.relay.started", "message.csrp.relay.complete",
        "report.csrp.scan.title", "report.csrp.phase.title", "report.csrp.vector.title",
        "report.csrp.dislodgement.title", "message.csrp.node_lamp.strength"
    ]) if (!lang[key]) failures.push(`${language}: missing ${key}`);
}

if (failures.length) {
    console.error("Relay Tower port verification failed:");
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}

console.log("Relay Tower port verification passed (22 modules, 4 report families, 3-block tower).\n");
