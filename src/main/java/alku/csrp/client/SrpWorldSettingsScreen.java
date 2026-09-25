package alku.csrp.client;

import alku.csrp.world.SrpDifficulty;
import alku.csrp.world.SrpMeteorMode;
import alku.csrp.world.SrpStarType;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

/**
 * Port of SRP 1.10.9 {@code client/gui/GuiSRPWorldSettings} — the dedicated "SRP World Settings"
 * screen opened from the create-world screen by the "SRP World Options..." button
 * ({@code client/gui/SRPWorldCreateButtons.java}).
 *
 * <p>The original layout is reproduced: a left column of cycle buttons at {@code leftX},
 * {@code topY + 0 / 48 / 72 / 96 / 120}, the animated {@link SrpWorldPreview} panel to their right
 * ({@code topY - 4}, 160x122) and a full-width "Done" button at {@code height - 28}. The two
 * cold-star-only buttons follow the star-type selection, exactly as
 * {@code GuiSRPWorldSettings.func_146284_a} did.</p>
 *
 * <p>Selections are written back into {@link SrpDifficultyScreenEvents} when "Done" is pressed so the
 * existing create-world mixin keeps staging them for the integrated server.</p>
 */
public final class SrpWorldSettingsScreen extends Screen {
    private static final int TOP_Y = 60;
    private static final int BUTTON_WIDTH = 176;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PREVIEW_GAP = 12;
    private static final int PREVIEW_DEFAULT_WIDTH = 160;
    private static final int PREVIEW_HEIGHT = 122;

    private final CreateWorldScreen parent;

    private SrpDifficulty difficulty;
    private SrpStarType starType;
    private SrpMeteorMode meteor;
    private SrpMeteorMode mushroomTrees;
    private SrpMeteorMode fracturedTerrain;

    private CycleButton<SrpMeteorMode> mushroomTreesButton;
    private CycleButton<SrpMeteorMode> fracturedButton;

    private int previewX;
    private int previewY;
    private int previewWidth;

    public SrpWorldSettingsScreen(CreateWorldScreen parent) {
        super(Component.translatable("gui.csrp.worldsettings.title"));
        this.parent = parent;
        this.difficulty = SrpDifficultyScreenEvents.pendingDifficulty(parent);
        this.starType = SrpDifficultyScreenEvents.pendingStarType(parent);
        this.meteor = SrpDifficultyScreenEvents.pendingMeteor(parent);
        this.mushroomTrees = SrpDifficultyScreenEvents.pendingMushroomTrees(parent);
        this.fracturedTerrain = SrpDifficultyScreenEvents.pendingFracturedTerrain(parent);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int totalWidth = BUTTON_WIDTH + PREVIEW_GAP + PREVIEW_DEFAULT_WIDTH;
        int leftX = centerX - totalWidth / 2;
        this.previewX = leftX + BUTTON_WIDTH + PREVIEW_GAP;
        this.previewY = TOP_Y - 4;
        this.previewWidth = Math.max(110, Math.min(PREVIEW_DEFAULT_WIDTH, this.width - this.previewX - 12));

        addRenderableWidget(CycleButton.<SrpDifficulty>builder(
                        value -> Component.translatable(value.translationKey()), this.difficulty)
                .withValues(List.of(SrpDifficulty.values()))
                .create(leftX, TOP_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("gui.csrp.worldsettings.difficulty"),
                        (button, value) -> {
                            this.difficulty = value;
                            button.setTooltip(Tooltip.create(Component.translatable(value.descriptionKey())));
                        }));

        addRenderableWidget(CycleButton.<SrpMeteorMode>builder(
                        value -> Component.translatable(meteorValueKey(value)), this.meteor)
                .withValues(List.of(SrpMeteorMode.values()))
                .create(leftX, TOP_Y + 48, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("gui.csrp.worldsettings.meteor"),
                        (button, value) -> {
                            this.meteor = value;
                            button.setTooltip(meteorTooltip(value));
                        }));

        addRenderableWidget(CycleButton.<SrpStarType>builder(
                        value -> Component.translatable(starValueKey(value)), this.starType)
                .withValues(List.of(SrpStarType.values()))
                .create(leftX, TOP_Y + 72, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("gui.csrp.worldsettings.star"),
                        (button, value) -> {
                            this.starType = value;
                            updateColdStarButtons();
                        }));

        this.mushroomTreesButton = addRenderableWidget(CycleButton.<SrpMeteorMode>builder(
                        value -> Component.translatable(mushroomValueKey(value)), this.mushroomTrees)
                .withValues(List.of(SrpMeteorMode.values()))
                .create(leftX, TOP_Y + 96, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("gui.csrp.worldsettings.mushroom_trees"),
                        (button, value) -> {
                            this.mushroomTrees = value;
                            button.setTooltip(mushroomTooltip(value));
                        }));

        this.fracturedButton = addRenderableWidget(CycleButton.<SrpMeteorMode>builder(
                        value -> Component.translatable(fracturedValueKey(value)), this.fracturedTerrain)
                .withValues(List.of(SrpMeteorMode.values()))
                .create(leftX, TOP_Y + 120, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.translatable("gui.csrp.worldsettings.fractured"),
                        (button, value) -> {
                            this.fracturedTerrain = value;
                            button.setTooltip(fracturedTooltip(value));
                        }));

        addRenderableWidget(Button.builder(Component.translatable("gui.csrp.worldsettings.done"),
                        button -> onDone())
                .bounds(centerX - 100, this.height - 28, 200, BUTTON_HEIGHT)
                .build());

        applyInitialTooltips();
        updateColdStarButtons();
    }

    private void applyInitialTooltips() {
        // The cycle buttons only refresh their tooltip when the value changes, so seed it here.
        children().stream()
                .filter(CycleButton.class::isInstance)
                .map(child -> (CycleButton<?>) child)
                .forEach(button -> {
                    Object value = button.getValue();
                    if (value instanceof SrpDifficulty preset) {
                        button.setTooltip(Tooltip.create(Component.translatable(preset.descriptionKey())));
                    } else if (value instanceof SrpStarType star) {
                        button.setTooltip(starTooltip(star));
                    } else if (value instanceof SrpMeteorMode mode) {
                        if (button == this.mushroomTreesButton) {
                            button.setTooltip(mushroomTooltip(mode));
                        } else if (button == this.fracturedButton) {
                            button.setTooltip(fracturedTooltip(mode));
                        } else {
                            button.setTooltip(meteorTooltip(mode));
                        }
                    }
                });
    }

    private void updateColdStarButtons() {
        boolean cold = this.starType == SrpStarType.COLD;
        if (this.mushroomTreesButton != null) {
            this.mushroomTreesButton.visible = cold;
        }
        if (this.fracturedButton != null) {
            this.fracturedButton.visible = cold;
        }
        if (!cold) {
            // GuiSRPWorldSettings cleared the fractured toggle whenever the star type left "cold".
            this.fracturedTerrain = SrpMeteorMode.OFF;
        }
    }

    private void onDone() {
        SrpDifficultyScreenEvents.applySelections(this.parent, this.difficulty, this.starType, this.meteor,
                this.mushroomTrees, this.fracturedTerrain);
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    @Override
    public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX,
            int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.translatable("gui.csrp.worldsettings.title").getString(),
                this.width / 2, 20, 0xFFFFFF);
        graphics.centeredText(this.font, Component.translatable("gui.csrp.worldsettings.subtitle").getString(),
                this.width / 2, 34, 0xAAAAAA);
        SrpWorldPreview.draw(graphics, this.previewX, this.previewY, this.previewWidth, PREVIEW_HEIGHT,
                this.difficulty.ordinal(), this.meteor.enabled(), this.starType.value(),
                partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    private static String meteorValueKey(SrpMeteorMode mode) {
        return mode.enabled() ? "gui.csrp.worldsettings.meteor.on" : "gui.csrp.worldsettings.meteor.off";
    }

    private static String starValueKey(SrpStarType starType) {
        return switch (starType) {
            case COLD -> "gui.csrp.worldsettings.star.cold";
            case WARM -> "gui.csrp.worldsettings.star.warm";
            default -> "gui.csrp.worldsettings.star.normal";
        };
    }

    private static String mushroomValueKey(SrpMeteorMode mode) {
        return mode.enabled()
                ? "gui.csrp.worldsettings.mushroom_trees.on"
                : "gui.csrp.worldsettings.mushroom_trees.off";
    }

    private static String fracturedValueKey(SrpMeteorMode mode) {
        return mode.enabled()
                ? "gui.csrp.worldsettings.fractured.on"
                : "gui.csrp.worldsettings.fractured.off";
    }

    private static Tooltip meteorTooltip(SrpMeteorMode mode) {
        return Tooltip.create(Component.translatable(mode.enabled()
                ? "gui.csrp.worldsettings.tooltip.meteor.active"
                : "gui.csrp.worldsettings.tooltip.meteor.inactive"));
    }

    private static Tooltip starTooltip(SrpStarType starType) {
        String prefix = switch (starType) {
            case COLD -> "gui.csrp.worldsettings.tooltip.star.cold.";
            case WARM -> "gui.csrp.worldsettings.tooltip.star.warm.";
            default -> "gui.csrp.worldsettings.tooltip.star.normal.";
        };
        return Tooltip.create(lines(prefix + "1", prefix + "2"));
    }

    private static Tooltip mushroomTooltip(SrpMeteorMode mode) {
        if (!mode.enabled()) {
            return Tooltip.create(Component.translatable("gui.csrp.worldsettings.tooltip.mushroom_trees.off"));
        }
        return Tooltip.create(lines("gui.csrp.worldsettings.tooltip.mushroom_trees.1",
                "gui.csrp.worldsettings.tooltip.mushroom_trees.2",
                "gui.csrp.worldsettings.tooltip.mushroom_trees.warning"));
    }

    private static Tooltip fracturedTooltip(SrpMeteorMode mode) {
        if (!mode.enabled()) {
            return Tooltip.create(Component.translatable("gui.csrp.worldsettings.tooltip.fractured.off"));
        }
        return Tooltip.create(lines("gui.csrp.worldsettings.tooltip.fractured.1",
                "gui.csrp.worldsettings.tooltip.fractured.2",
                "gui.csrp.worldsettings.tooltip.fractured.warning"));
    }

    private static Component lines(String... keys) {
        StringBuilder builder = new StringBuilder();
        for (String key : keys) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(Component.translatable(key).getString());
        }
        return Component.literal(builder.toString());
    }
}
