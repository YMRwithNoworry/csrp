package alku.csrp.compendium.client;

import alku.csrp.compendium.CompendiumCatalog;
import alku.csrp.compendium.CompendiumProgress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class CompendiumScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(
            "csrp", "textures/gui/compendium/background.png");
    private static final int PANEL_WIDTH = 332;
    private static final int PANEL_HEIGHT = 212;
    private static final int LIST_WIDTH = 128;
    private static final int ROW_HEIGHT = 15;
    private static final int PREVIEW_LEFT = 92;
    private static final int PREVIEW_TOP = 18;
    private static final int PREVIEW_RIGHT = 192;
    private static final int PREVIEW_BOTTOM = 110;
    private static final int PREVIEW_INSET = 10;
    private static final float PREVIEW_BASE_SCALE = 20.0F;
    private static final float PREVIEW_BOUNDS_PADDING = 1.15F;
    private static final int PREVIEW_MIN_SCALE = 2;
    private static final int PREVIEW_MAX_SCALE = 34;
    private final CompendiumProgress progress;
    private final List<CompendiumEntry> mobs;
    private Category category = Category.PARASITES;
    private int selectedIndex;
    private int scroll;
    private float modelYaw;
    private float modelPitch;
    private float modelZoom = 1.0F;
    private LivingEntity previewEntity;

    public CompendiumScreen(CompendiumProgress progress, List<CompendiumEntry> entries) {
        super(Component.translatable("screen.csrp.compendium.title"));
        this.progress = progress;
        this.mobs = entries.stream().sorted(Comparator.comparing(CompendiumEntry::tier)
                .thenComparing(entry -> CompendiumLanguage.get(entry.nameKey()))).toList();
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int tabWidth = PANEL_WIDTH / Category.values().length;
        for (int index = 0; index < Category.values().length; index++) {
            Category tab = Category.values()[index];
            addRenderableWidget(Button.builder(Component.translatable(tab.key), button -> selectCategory(tab))
                    .bounds(left + index * tabWidth, top - 22, tabWidth - 2, 20).build());
        }
    }

    private void selectCategory(Category selected) {
        category = selected;
        selectedIndex = 0;
        scroll = 0;
        previewEntity = null;
        resetModelView();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.blit(BACKGROUND, left, top, 0.0F, 0.0F, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.fill(left + 3, top + 3, left + LIST_WIDTH, top + PANEL_HEIGHT - 3, 0xD029231D);
        graphics.fill(left + LIST_WIDTH + 3, top + 3, left + PANEL_WIDTH - 3, top + PANEL_HEIGHT - 3,
                0x18FFFFFF);
        renderList(graphics, left, top, mouseX, mouseY);
        renderDetails(graphics, left + LIST_WIDTH + 8, top + 8, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderList(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        List<ListEntry> entries = currentEntries();
        int visible = (PANEL_HEIGHT - 12) / ROW_HEIGHT;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - visible)));
        graphics.enableScissor(left + 3, top + 3, left + LIST_WIDTH, top + PANEL_HEIGHT - 3);
        for (int row = 0; row < visible && scroll + row < entries.size(); row++) {
            int index = scroll + row;
            int y = top + 7 + row * ROW_HEIGHT;
            if (index == selectedIndex) {
                graphics.fill(left + 5, y - 2, left + LIST_WIDTH - 3, y + 11, 0xFF634938);
            }
            ListEntry entry = entries.get(index);
            graphics.drawString(font, entry.unlocked ? entry.name : "???", left + 9, y,
                    entry.unlocked ? 0xFFF1E4C7 : 0xFF746A5C, false);
        }
        graphics.disableScissor();
    }

    private void renderDetails(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        List<ListEntry> entries = currentEntries();
        if (entries.isEmpty() || selectedIndex >= entries.size()) {
            graphics.drawString(font, Component.translatable("screen.csrp.compendium.empty"), x, y, 0xFF4B3A2C,
                    false);
            return;
        }
        ListEntry selected = entries.get(selectedIndex);
        if (!selected.unlocked) {
            graphics.drawCenteredString(font, "?", x + 105, y + 80, 0xFF59483A);
            graphics.drawCenteredString(font, Component.translatable("screen.csrp.compendium.locked"),
                    x + 105, y + 101, 0xFF59483A);
            return;
        }
        graphics.drawString(font, selected.name, x, y, 0xFF392A20, false);
        if (category == Category.PARASITES) {
            renderMobDetails(graphics, mobs.get(selected.sourceIndex), x, y + 14, mouseX, mouseY);
        } else if (category == Category.STATS) {
            renderStats(graphics, x, y + 18);
        } else {
            renderWrapped(graphics, descriptionFor(selected.id), x, y + 20, 205, 0xFF49372B);
        }
    }

    private void renderMobDetails(GuiGraphics graphics, CompendiumEntry entry, int x, int y, int mouseX, int mouseY) {
        int kills = progress.kills().getOrDefault(entry.entityId(), 0);
        graphics.drawString(font, Component.translatable("screen.csrp.compendium.tier",
                translatedTier(entry.tier())), x, y, 0xFF654936, false);
        graphics.drawString(font, Component.translatable("screen.csrp.compendium.kills", kills), x, y + 11,
                0xFF654936, false);
        LivingEntity entity = preview(entry);
        if (entity != null) {
            int scale = previewScale(entity, entry);
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics,
                    x + PREVIEW_LEFT, y + PREVIEW_TOP, x + PREVIEW_RIGHT, y + PREVIEW_BOTTOM,
                    scale, 0.0F, modelYaw, modelPitch, entity);
        }
        if (kills >= entry.minimumStatKills() && entity != null) {
            double damage = entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            graphics.drawString(font, Component.translatable("screen.csrp.compendium.health",
                    Math.round(entity.getMaxHealth())), x, y + 29, 0xFF49372B, false);
            graphics.drawString(font, Component.translatable("screen.csrp.compendium.damage",
                    String.format("%.1f", damage)), x, y + 40, 0xFF49372B, false);
            renderDrops(graphics, entry, x, y + 53);
        } else {
            graphics.drawString(font, Component.translatable("screen.csrp.compendium.stats_requirement",
                    entry.minimumStatKills()), x, y + 29, 0xFF785C48, false);
        }
        if (kills >= entry.minimumLoreKills()) {
            renderWrapped(graphics, cleanLore(CompendiumLanguage.get(entry.loreKey())), x, y + 112, 207, 0xFF49372B);
        } else {
            graphics.drawString(font, Component.translatable("screen.csrp.compendium.lore_requirement",
                    entry.minimumLoreKills()), x, y + 112, 0xFF785C48, false);
        }
    }

    private void renderDrops(GuiGraphics graphics, CompendiumEntry entry, int x, int y) {
        List<String> drops = CompendiumClient.drops(entry.path());
        graphics.drawString(font, Component.translatable("screen.csrp.compendium.drops"), x, y, 0xFF49372B,
                false);
        int shown = 0;
        for (String drop : drops) {
            ResourceLocation id = ResourceLocation.tryParse(drop);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || shown >= 6) {
                continue;
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            graphics.renderItem(stack, x + shown * 19, y + 10);
            if (minecraft != null && mouseInDropSlot(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos(),
                    x + shown * 19, y + 10)) {
                graphics.renderTooltip(font, stack, (int) (minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth()),
                        (int) (minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight()));
            }
            shown++;
        }
    }

    private boolean mouseInDropSlot(double rawX, double rawY, int x, int y) {
        if (minecraft == null) {
            return false;
        }
        double scaledX = rawX * width / minecraft.getWindow().getScreenWidth();
        double scaledY = rawY * height / minecraft.getWindow().getScreenHeight();
        return scaledX >= x && scaledX < x + 16 && scaledY >= y && scaledY < y + 16;
    }

    private void renderStats(GuiGraphics graphics, int x, int y) {
        int totalKills = progress.kills().values().stream().mapToInt(Integer::intValue).sum();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.csrp.compendium.total_kills", totalKills));
        mobs.stream().map(CompendiumEntry::tier).distinct().forEach(tier -> {
            int tierKills = mobs.stream().filter(entry -> entry.tier().equals(tier))
                    .mapToInt(entry -> progress.kills().getOrDefault(entry.entityId(), 0)).sum();
            lines.add(Component.translatable("screen.csrp.compendium.tier_kills", translatedTier(tier), tierKills));
        });
        lines.add(Component.translatable("screen.csrp.compendium.damage_to", String.format("%.1f", progress.damageToParasites())));
        lines.add(Component.translatable("screen.csrp.compendium.damage_from", String.format("%.1f", progress.damageFromParasites())));
        lines.add(Component.translatable("screen.csrp.compendium.deaths", progress.deathsByParasites()));
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), x, y + index * 10, 0xFF49372B, false);
        }
    }

    private LivingEntity preview(CompendiumEntry entry) {
        if (previewEntity != null && BuiltInRegistries.ENTITY_TYPE.getKey(previewEntity.getType()).toString()
                .equals(entry.entityId())) {
            return previewEntity;
        }
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(entry.entityId());
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return null;
        }
        var entity = BuiltInRegistries.ENTITY_TYPE.get(id).create(minecraft.level);
        previewEntity = entity instanceof LivingEntity living ? living : null;
        return previewEntity;
    }

    private int previewScale(LivingEntity entity, CompendiumEntry entry) {
        float availableWidth = PREVIEW_RIGHT - PREVIEW_LEFT - PREVIEW_INSET * 2.0F;
        float availableHeight = PREVIEW_BOTTOM - PREVIEW_TOP - PREVIEW_INSET * 2.0F;
        float entityWidth = Math.max(0.1F, entity.getBbWidth()) * PREVIEW_BOUNDS_PADDING;
        float entityHeight = Math.max(0.1F, entity.getBbHeight()) * PREVIEW_BOUNDS_PADDING;
        float fittedScale = Math.min(availableWidth / entityWidth, availableHeight / entityHeight);
        float configuredScale = PREVIEW_BASE_SCALE * entry.renderScale();
        int scale = Math.round(Math.min(configuredScale, fittedScale) * modelZoom);
        return Math.max(PREVIEW_MIN_SCALE, Math.min(PREVIEW_MAX_SCALE, scale));
    }

    private void resetModelView() {
        modelYaw = 0.0F;
        modelPitch = 0.0F;
        modelZoom = 1.0F;
    }

    private List<ListEntry> currentEntries() {
        if (category == Category.PARASITES) {
            List<ListEntry> result = new ArrayList<>();
            for (int index = 0; index < mobs.size(); index++) {
                CompendiumEntry entry = mobs.get(index);
                int kills = progress.kills().getOrDefault(entry.entityId(), 0);
                result.add(new ListEntry(entry.entityId(), CompendiumLanguage.get(entry.nameKey()), kills > 0, index));
            }
            return result;
        }
        if (category == Category.BLOCKS) {
            List<ListEntry> result = new ArrayList<>();
            for (String path : CompendiumCatalog.BLOCKS) {
                ResourceLocation id = new ResourceLocation("csrp", path);
                String nameKey = "tile.srparasites." + path + ".name";
                String name = CompendiumLanguage.get(nameKey);
                if (name.equals(nameKey) && BuiltInRegistries.BLOCK.containsKey(id)) {
                    name = BuiltInRegistries.BLOCK.get(id).getName().getString();
                }
                result.add(new ListEntry(id.toString(), name.equals(nameKey) ? titleCase(path) : name,
                        progress.blocks().contains(id.toString()), 0));
            }
            return result;
        }
        if (category == Category.EFFECTS) {
            List<ListEntry> result = new ArrayList<>();
            for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
                ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
                if (id.getNamespace().equals("csrp") && !id.getPath().equals("corrosion")) {
                    result.add(new ListEntry(id.toString(), Component.translatable(effect.getDescriptionId()).getString(),
                            progress.effects().contains(id.toString()), 0));
                }
            }
            return result;
        }
        if (category == Category.CELESTIAL) {
            return namedEntries(CompendiumCatalog.CELESTIALS, progress.celestials(), "bestiary.celestial.");
        }
        if (category == Category.SYSTEMS) {
            return namedEntries(CompendiumCatalog.SYSTEMS, CompendiumCatalog.SYSTEMS, "bestiary.system.");
        }
        return List.of(new ListEntry("stats", Component.translatable("screen.csrp.compendium.stats").getString(),
                true, 0));
    }

    private static List<ListEntry> namedEntries(List<String> ids, Iterable<String> unlocked, String prefix) {
        List<String> unlockedList = new ArrayList<>();
        unlocked.forEach(unlockedList::add);
        List<ListEntry> entries = new ArrayList<>();
        for (String id : ids) {
            String key = prefix + id + ".name";
            String translated = CompendiumLanguage.get(key);
            entries.add(new ListEntry(id, translated.equals(key) ? titleCase(id) : translated,
                    unlockedList.contains(id), 0));
        }
        return entries;
    }

    private String descriptionFor(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] keys = switch (category) {
            case CELESTIAL -> new String[]{"bestiary.celestial." + id + ".desc", "celestial.srparasites." + id + ".desc"};
            case SYSTEMS -> new String[]{"bestiary.system." + id + ".desc", "bestiary.systems." + id};
            case BLOCKS -> new String[]{"bestiary.block.srparasites." + path + ".desc"};
            case EFFECTS -> new String[]{"bestiary.effect." + legacyEffectPath(path) + ".desc"};
            default -> new String[0];
        };
        for (String key : keys) {
            String value = CompendiumLanguage.get(key);
            if (!value.equals(key)) {
                return cleanLore(value);
            }
        }
        return Component.translatable("screen.csrp.compendium.discovered").getString();
    }

    private static String legacyEffectPath(String path) {
        return switch (path) {
            case "contamination" -> "conta";
            case "corrosion" -> "corrosive";
            case "distorted_enlightenment" -> "distorted_enlightenment";
            case "effect_pos" -> "effectpos";
            case "effect_neg" -> "effectneg";
            default -> path;
        };
    }

    private void renderWrapped(GuiGraphics graphics, String text, int x, int y, int width, int color) {
        int line = 0;
        for (String paragraph : text.split("\\|")) {
            for (var sequence : font.split(Component.literal(paragraph.trim()), width)) {
                if (y + line * 10 > (height + PANEL_HEIGHT) / 2 - 8) {
                    return;
                }
                graphics.drawString(font, sequence, x, y + line++ * 10, color, false);
            }
            line++;
        }
    }

    private static String cleanLore(String text) {
        return text.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }

    private static String translatedTier(String tier) {
        String key = "screen.csrp.compendium.tier." + tier.toLowerCase();
        String translated = Component.translatable(key).getString();
        return translated.equals(key) ? titleCase(tier) : translated;
    }

    private static String titleCase(String value) {
        String normalized = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        if (mouseX >= left + 3 && mouseX < left + LIST_WIDTH && mouseY >= top + 3
                && mouseY < top + PANEL_HEIGHT - 3) {
            int row = (int) ((mouseY - top - 5) / ROW_HEIGHT);
            int index = scroll + row;
            if (index >= 0 && index < currentEntries().size()) {
                if (selectedIndex != index) {
                    resetModelView();
                }
                selectedIndex = index;
                previewEntity = null;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (category == Category.PARASITES && button == 0) {
            modelYaw += (float) dragX * 2.0F;
            modelPitch = Math.max(-45.0F, Math.min(45.0F, modelPitch + (float) dragY * 2.0F));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (width - PANEL_WIDTH) / 2;
        if (mouseX < left + LIST_WIDTH) {
            scroll -= (int) Math.signum(scrollY);
        } else if (category == Category.PARASITES) {
            modelZoom = Math.max(0.4F, Math.min(2.5F, modelZoom + (float) scrollY * 0.1F));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ListEntry(String id, String name, boolean unlocked, int sourceIndex) {
    }

    private enum Category {
        PARASITES("screen.csrp.compendium.parasites"),
        BLOCKS("screen.csrp.compendium.blocks"),
        CELESTIAL("screen.csrp.compendium.celestial"),
        EFFECTS("screen.csrp.compendium.effects"),
        SYSTEMS("screen.csrp.compendium.systems"),
        STATS("screen.csrp.compendium.stats");

        private final String key;

        Category(String key) {
            this.key = key;
        }
    }
}
