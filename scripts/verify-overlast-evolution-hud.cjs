const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const textureDir = path.join(root, "src/main/resources/assets/csrp/textures/gui/overlast");
const hudPath = path.join(root, "src/main/java/alku/csrp/overlast/client/EvolutionHudEvents.java");
const eventsPath = path.join(root, "src/main/java/alku/csrp/event/OverlastEvents.java");
const configPath = path.join(root, "src/main/java/alku/csrp/Config.java");
const failures = [];

function requireSource(source, fragment, description) {
    if (!source.includes(fragment)) {
        failures.push(description);
    }
}

for (let phase = -2; phase <= 8; phase++) {
    const file = path.join(textureDir, `evolutionbar${phase}.png`);
    if (!fs.existsSync(file)) {
        failures.push(`缺少阶段纹理 evolutionbar${phase}.png`);
        continue;
    }
    const png = fs.readFileSync(file);
    if (png.subarray(0, 8).toString("hex") !== "89504e470d0a1a0a") {
        failures.push(`纹理不是有效 PNG: evolutionbar${phase}.png`);
        continue;
    }
    if (png.readUInt32BE(16) !== 256 || png.readUInt32BE(20) !== 256) {
        failures.push(`纹理尺寸不是原版 256x256: evolutionbar${phase}.png`);
    }
}

const hud = fs.readFileSync(hudPath, "utf8");
const events = fs.readFileSync(eventsPath, "utf8");
const config = fs.readFileSync(configPath, "utf8");

requireSource(hud, "private static final int FULL_WIDTH = 113;", "HUD 外框宽度不是原版 113 像素");
requireSource(hud, "private static final int FULL_HEIGHT = 29;", "HUD 外框高度不是原版 29 像素");
requireSource(hud, "private static final int BAR_WIDTH = 80;", "HUD 移动条宽度不是原版 80 像素");
requireSource(hud, "private static final int BAR_TEXTURE_X = 23;", "HUD 移动条纹理 X 不是原版 23");
requireSource(hud, "private static final int BAR_TEXTURE_Y = 32;", "HUD 移动条纹理 Y 不是原版 32");
requireSource(hud, '"textures/gui/overlast/evolutionbar" + texturePhase + ".png"', "HUD 未按阶段选择原版纹理");
requireSource(hud, "drawPhaseBadge(graphics, minecraft.font, state.phase()", "HUD 未动态绘制当前演化阶段徽章");
requireSource(hud, "font.width(text)", "HUD 点数缩放未使用字体实际像素宽度");
requireSource(hud, "graphics.pose().scale(scale, scale, 1.0F)", "HUD 点数未按可用宽度缩放");
requireSource(hud, "screenWidth - SCREEN_MARGIN - pointsCenterX", "HUD 点数缩放未考虑屏幕安全边距");
requireSource(hud, "if (state.phase() >= 10)", "最终阶段没有固定显示满进度");
requireSource(hud, "state.nextThreshold() - state.currentThreshold()", "HUD 未按当前阶段阈值计算进度");
requireSource(events, "level.getGameTime() % 20L == 0L", "HUD 未按固定频率刷新");
requireSource(events, "level.players().forEach(OverlastEvents::syncHud);", "HUD 刷新未覆盖在线玩家");
requireSource(events, "!Config.overlastHudRequiresClock() || holdingClock", "进化钟显示条件丢失");
requireSource(config, '"top left", "top right", "middle left", "middle right", "bottom left", "bottom right"',
        "HUD 六种位置配置丢失");

if (failures.length > 0) {
    console.error(failures.map((failure) => `- ${failure}`).join("\n"));
    process.exit(1);
}

console.log("OverLast 进化进度条静态验证通过");
