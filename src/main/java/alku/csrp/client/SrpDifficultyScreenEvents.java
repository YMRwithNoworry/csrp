package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.world.SrpColdStarSelection;
import alku.csrp.world.SrpDifficulty;
import alku.csrp.world.SrpDifficultySelection;
import alku.csrp.world.SrpMeteorMode;
import alku.csrp.world.SrpMeteorSelection;
import alku.csrp.world.SrpStarType;
import alku.csrp.world.SrpStarTypeSelection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** Adds the SRP 1.10 difficulty selector to vanilla's create-world screen. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class SrpDifficultyScreenEvents {
    private static final Map<CreateWorldScreen, SrpDifficulty> SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, SrpStarType> STAR_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, SrpMeteorMode> METEOR_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Boolean> FRACTURED_TERRAIN_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Boolean> MUSHROOM_TREE_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Boolean> EXTREME_SNOW_SELECTIONS = new WeakHashMap<>();

    private SrpDifficultyScreenEvents() {
    }

    @SubscribeEvent
    public static void addDifficultySelector(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) {
            return;
        }

        SrpDifficulty selected = SELECTIONS.getOrDefault(screen, SrpDifficulty.NORMAL);
        int width = Math.min(180, Math.max(120, screen.width - 20));
        CycleButton<SrpDifficulty> selector = CycleButton.<SrpDifficulty>builder(
                        difficulty -> Component.translatable(difficulty.translationKey()))
                .withValues(List.of(SrpDifficulty.values()))
                .withInitialValue(selected)
                .create((screen.width - width) / 2, screen.height - 52, width, 20,
                        Component.translatable("options.csrp.difficulty"),
                        (button, difficulty) -> {
                            SELECTIONS.put(screen, difficulty);
                            updateTooltip(button, difficulty);
                        });
        updateTooltip(selector, selected);
        event.addListener(selector);

        SrpStarType selectedStar = STAR_SELECTIONS.getOrDefault(screen, SrpStarType.NORMAL);
        CycleButton<SrpStarType> starSelector = CycleButton.<SrpStarType>builder(
                        starType -> Component.translatable(starType.translationKey()))
                .withValues(List.of(SrpStarType.values()))
                .withInitialValue(selectedStar)
                .create((screen.width - width) / 2, screen.height - 76, width, 20,
                        Component.translatable("options.csrp.star_type"),
                        (button, starType) -> {
                            STAR_SELECTIONS.put(screen, starType);
                            updateStarTooltip(button, starType);
                        });
        updateStarTooltip(starSelector, selectedStar);
        event.addListener(starSelector);

        // SRP 1.10.8 GuiSRPWorldSettings exposed a "Meteor" on/off cycle button; keep the same
        // default as the global config so servers that already enable meteors stay enabled.
        SrpMeteorMode selectedMeteor = METEOR_SELECTIONS.computeIfAbsent(screen,
                ignored -> SrpMeteorMode.of(defaultMeteorEnabled()));
        CycleButton<SrpMeteorMode> meteorSelector = CycleButton.<SrpMeteorMode>builder(
                        mode -> Component.translatable(mode.translationKey()))
                .withValues(List.of(SrpMeteorMode.values()))
                .withInitialValue(selectedMeteor)
                .create((screen.width - width) / 2, screen.height - 100, width, 20,
                        Component.translatable("options.csrp.meteor"),
                        (button, mode) -> {
                            METEOR_SELECTIONS.put(screen, mode);
                            updateMeteorTooltip(button, mode);
                        });
        updateMeteorTooltip(meteorSelector, selectedMeteor);
        event.addListener(meteorSelector);

        int toggleGap = 4;
        int toggleWidth = Math.max(72, (screen.width - 28) / 3);
        int toggleStart = Math.max(10, (screen.width - (toggleWidth * 3 + toggleGap * 2)) / 2);
        int toggleY = screen.height - 124;
        addColdStarToggle(event, screen, toggleStart, toggleWidth, toggleY,
                "options.csrp.cold_fractured_short", "options.csrp.cold_fractured_tooltip",
                FRACTURED_TERRAIN_SELECTIONS.getOrDefault(screen, true),
                value -> FRACTURED_TERRAIN_SELECTIONS.put(screen, value));
        addColdStarToggle(event, screen, toggleStart + toggleWidth + toggleGap, toggleWidth, toggleY,
                "options.csrp.cold_trees_short", "options.csrp.cold_trees_tooltip",
                MUSHROOM_TREE_SELECTIONS.getOrDefault(screen, true),
                value -> MUSHROOM_TREE_SELECTIONS.put(screen, value));
        addColdStarToggle(event, screen, toggleStart + (toggleWidth + toggleGap) * 2, toggleWidth, toggleY,
                "options.csrp.cold_snow_short", "options.csrp.cold_snow_tooltip",
                EXTREME_SNOW_SELECTIONS.getOrDefault(screen, false),
                value -> EXTREME_SNOW_SELECTIONS.put(screen, value));
    }

    public static void stageSelection(CreateWorldScreen screen) {
        SrpDifficultySelection.stage(SELECTIONS.getOrDefault(screen, SrpDifficulty.NORMAL));
        SrpStarTypeSelection.stage(STAR_SELECTIONS.getOrDefault(screen, SrpStarType.NORMAL));
        SrpMeteorMode meteor = METEOR_SELECTIONS.get(screen);
        if (meteor != null) {
            SrpMeteorSelection.stage(meteor);
        }
        SrpColdStarSelection.stage(new SrpColdStarSelection.Settings(
                FRACTURED_TERRAIN_SELECTIONS.getOrDefault(screen, true),
                MUSHROOM_TREE_SELECTIONS.getOrDefault(screen, true),
                EXTREME_SNOW_SELECTIONS.getOrDefault(screen, false)));
    }

    private static boolean defaultMeteorEnabled() {
        try {
            return Config.meteorEnabled();
        } catch (IllegalStateException | NullPointerException configNotLoaded) {
            return false;
        }
    }

    private static void addColdStarToggle(ScreenEvent.Init.Post event, CreateWorldScreen screen,
            int x, int width, int y, String labelKey, String tooltipKey, boolean initialValue,
            Consumer<Boolean> onChange) {
        CycleButton<Boolean> toggle = CycleButton.<Boolean>builder(value ->
                        Component.translatable(value ? "options.on" : "options.off"))
                .withValues(List.of(false, true))
                .withInitialValue(initialValue)
                .create(x, y, width, 20, Component.translatable(labelKey),
                        (button, value) -> {
                            onChange.accept(value);
                            button.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
                        });
        toggle.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        event.addListener(toggle);
    }

    private static void updateMeteorTooltip(CycleButton<SrpMeteorMode> button, SrpMeteorMode mode) {
        button.setTooltip(Tooltip.create(Component.translatable(mode.descriptionKey())));
    }

    private static void updateTooltip(CycleButton<SrpDifficulty> button, SrpDifficulty difficulty) {
        button.setTooltip(Tooltip.create(Component.translatable(difficulty.descriptionKey())));
    }

    private static void updateStarTooltip(CycleButton<SrpStarType> button, SrpStarType starType) {
        button.setTooltip(Tooltip.create(Component.translatable(starType.descriptionKey())));
    }
}
