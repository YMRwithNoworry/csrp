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

const tables = read("src/main/java/alku/csrp/world/ParasiteLootTables.java");
const events = read("src/main/java/alku/csrp/event/ParasiteLootEvents.java");

// Original SRPConfigMobs defaults, out109 util/config/SRPConfigMobs.java.
// shycoLoot (pri_longarms): lurecomponent3;60;2;false, ada_longarms_drop;40;2;true, bone;10;1;true
expect(tables, /put\("pri_longarms", e\("srparasites:lurecomponent3", 60, 2, false\), e\("srparasites:ada_longarms_drop", 40, 2, true\), e\("srparasites:bone", 10, 1, true\)\);/,
        "pri_longarms loot no longer matches shycoLoot");
// mudoLoot (rupter): lurecomponent1;70;2;true
expect(tables, /put\("rupter", e\("srparasites:lurecomponent1", 70, 2, true\)\);/,
        "rupter loot no longer matches mudoLoot");
// infhumanLoot (sim_human): assimilated_flesh;80;1;false, lurecomponent2;10;1;true
expect(tables, /put\("sim_human", e\("srparasites:assimilated_flesh", 80, 1, false\), e\("srparasites:lurecomponent2", 10, 1, true\)\);/,
        "sim_human loot no longer matches infhumanLoot");
// infcowLoot (sim_cow): assimilated_flesh;80;2;false, lurecomponent2;10;1;true, srpmeshi:vilebeefr;30;1;true
expect(tables, /put\("sim_cow", e\("srparasites:assimilated_flesh", 80, 2, false\), e\("srparasites:lurecomponent2", 10, 1, true\), e\("srpmeshi:vilebeefr", 30, 1, true\)\);/,
        "sim_cow loot no longer matches infcowLoot");
// ada_burrower inherits getParasiteIDRegister() == 57 -> shycoLoot (zaaadaptedloot is dead config).
expect(tables, /put\("ada_burrower", e\("srparasites:lurecomponent3", 60, 2, false\), e\("srparasites:ada_longarms_drop", 40, 2, true\), e\("srparasites:bone", 10, 1, true\)\);/,
        "ada_burrower loot no longer resolves to shycoLoot via getParasiteIDRegister() == 57");
// pri_burrower (getParasiteIDRegister() == 318) has no case in the original switch.
expect(tables, /public static List<Entry> forEntity\(String path\)/,
        "the loot table lookup is missing");
const lootGameTest = read("src/main/java/alku/csrp/gametest/LootTableGameTests.java");
expect(lootGameTest, /parasite_loot\/legacy_roll/,
        "the parasite loot GameTest is not registered");
expect(lootGameTest, /ParasiteLootTables\.forEntity\("pri_burrower"\) == null/,
        "the GameTest no longer asserts pri_burrower has no loot table");

// The handler must reproduce the original loot() algorithm.
expect(events, /class ParasiteLootEvents/,
        "the drop handler is missing");
expect(events, /instanceof Parasite/,
        "the handler no longer restricts itself to parasites");
expect(events, /GameRules\.MOB_DROPS/,
        "the handler no longer honours the doMobLoot game rule");
expect(events, /ModMobEffects\.DEBAR/,
        "the handler no longer skips DEBAR deaths");
expect(events, /DislodgmentSystem\.activeValue\(data, 18\)/,
        "the handler no longer skips dislodgment-code-18 deaths");
expect(events, /random\.nextInt\(100\) > entry\.chance\(\) - 1/,
        "the chance roll no longer matches rand.nextInt(100) <= chance - 1");
expect(events, /int realQuantity = quantity == 0 \? 0 : random\.nextInt\(quantity\);/,
        "the count roll no longer matches rand.nextInt(quantity) + 1");
expect(events, /pooled\.get\(random\.nextInt\(pooled\.size\(\)\)\)/,
        "the pooled (always=false) selection is missing");
expect(events, /event\.getDrops\(\)\.clear\(\)/,
        "the handler no longer replaces the data-driven loot table result");
expect(events, /raw\.startsWith\("srparasites:"\)/,
        "the handler no longer remaps the original namespace to csrp");

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log("parasite loot table contract ok");
