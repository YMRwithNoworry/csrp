const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const failures = [];

function read(relative) {
    return fs.readFileSync(path.join(root, relative), "utf8");
}

function requireFragment(source, fragment, message) {
    if (!source.includes(fragment)) {
        failures.push(message);
    }
}

function checkPng(relative, width, height) {
    const file = path.join(root, relative);
    if (!fs.existsSync(file)) {
        failures.push(`缺少纹理 ${relative}`);
        return;
    }
    const png = fs.readFileSync(file);
    if (png.subarray(0, 8).toString("hex") !== "89504e470d0a1a0a") {
        failures.push(`纹理不是 PNG: ${relative}`);
    } else if (png.readUInt32BE(16) !== width || png.readUInt32BE(20) !== height) {
        failures.push(`纹理尺寸错误: ${relative}`);
    }
}

const tiers = ["common", "uncommon", "rare"];
for (const tier of tiers) {
    const id = tier === "common" ? "parasiteloot" : `parasiteloot_${tier}`;
    for (const relative of [
        `src/main/resources/assets/csrp/blockstates/${id}.json`,
        `src/main/resources/assets/csrp/models/block/parasiteloot_${tier}.json`,
        `src/main/resources/assets/csrp/models/item/${id}.json`,
        `src/main/resources/data/csrp/loot_table/blocks/${id}.json`
    ]) {
        if (!fs.existsSync(path.join(root, relative))) {
            failures.push(`缺少资源 ${relative}`);
        } else if (relative.endsWith(".json")) {
            JSON.parse(read(relative));
        }
    }
    checkPng(`src/main/resources/assets/csrp/textures/block/parasiteloot_${tier}.png`, 16, 16);
}
checkPng("src/main/resources/assets/csrp/textures/gui/parasite_loot.png", 256, 256);
checkPng("src/main/resources/assets/csrp/textures/gui/blood_bubble.png", 7, 7);

const blockEntity = read("src/main/java/alku/csrp/block/entity/ParasiteLootBlockEntity.java");
const menu = read("src/main/java/alku/csrp/inventory/ParasiteLootMenu.java");
const block = read("src/main/java/alku/csrp/block/ParasiteLootBlock.java");
const screen = read("src/main/java/alku/csrp/client/screen/ParasiteLootScreen.java");

requireFragment(blockEntity, "public static final int CONTAINER_SIZE = 27;", "寄生增生物不是 27 格容器");
requireFragment(blockEntity, "generateLootIfNeeded();", "没有在首次打开时生成战利品");
requireFragment(block, "COMMON(0.5F)", "良性增生物生成概率不是 50%");
requireFragment(block, "UNCOMMON(0.1F)", "恶性增生物生成概率不是 10%");
requireFragment(block, "RARE(0.2F)", "转移性肿瘤生成概率不是 20%");
requireFragment(blockEntity, "ModItems.ADA_SUMMONER_DROP.get()", "恶性增生物缺少适应种战利品池");
requireFragment(blockEntity, "ModItems.LURECOMPONENT6.get()", "转移性肿瘤缺少扭曲脊柱信号");
requireFragment(blockEntity, "stack.getItem() instanceof BlockItem", "容器未禁止方块物品");
requireFragment(blockEntity, "getNamespace().equals(Csrp.MODID)", "容器未限制为 CSRP 物品");
requireFragment(menu, "0.5F + 7.5F * (1.0F - clamped)", "取物伤害公式不匹配 Wiki");
requireFragment(menu, "EffectStacking.apply(player, ModMobEffects.VIRAL", "取物未叠加 Viral");
requireFragment(menu, "ModMobEffects.CORROSION", "取物未施加 Corrosion");
requireFragment(menu, "ItemStack.isSameItemSameComponents(before, after)", "用 CSRP 物品替换时未跳过伤害");
requireFragment(block, "loot.clearContent();", "破坏方块时没有清空库存");
requireFragment(screen, "private static final int MAX_BUBBLES = 28;", "GUI 未移植原版血泡上限");
requireFragment(screen, "barX + 160", "GUI 未移植 160 像素 fullness 条");
requireFragment(screen, '"textures/gui/parasite_loot.png"', "GUI 未使用原版背景纹理");
requireFragment(screen, "random.nextFloat() < 0.35F", "GUI 未移植原版血泡分裂概率");
requireFragment(screen, "parent.size < 12.8F", "GUI 血泡分裂尺寸门槛不匹配原版");
requireFragment(screen, "MAX_BUBBLES - bubbles.size()", "GUI 分裂血泡未受总数上限约束");

if (failures.length) {
    console.error(failures.map((failure) => `- ${failure}`).join("\n"));
    process.exit(1);
}

console.log("Parasitic Growth port verification passed.");
