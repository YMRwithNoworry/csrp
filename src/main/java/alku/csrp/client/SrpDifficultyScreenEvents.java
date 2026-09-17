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
    private static final Map<CreateWorldScreen, SrpMeteorMode> MUSHROOM_TREES_SELECTIONS =
            new WeakHashMap<>();
    private static final Map<CreateWorldScreen, SrpMeteorMode> FRACTURED_SELECTIONS =
            new WeakHashMap<>();

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
                        difficulty -> Component.translatable(difficulty.translationKey()), selected)
                .withValues(List.of(SrpDifficulty.values()))
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
                        starType -> Component.translatable(starType.translationKey()), selectedStar)
                .withValues(List.of(SrpStarType.values()))
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
                        mode -> Component.translatable(mode.translationKey()), selectedMeteor)
                .withValues(List.of(SrpMeteorMode.values()))
                .create((screen.width - width) / 2, screen.height - 100, width, 20,
                        Component.translatable("options.csrp.meteor"),
                        (button, mode) -> {
                            METEOR_SELECTIONS.put(screen, mode);
                            updateMeteorTooltip(button, mode);
                        });
        updateMeteorTooltip(meteorSelector, selectedMeteor);
        event.addListener(meteorSelector);

        // 1.10.9 GuiSRPWorldSettings added two cold-star-only options (BTN_MUSHROOM_TREES = 15 and
        // BTN_FRACTURED_TERRAIN = 16) at topY + 96 / topY + 120; the same layout is kept here and
        // both buttons follow the star-type selection.
        SrpMeteorMode selectedMushroomTrees = MUSHROOM_TREES_SELECTIONS.getOrDefault(screen,
                SrpMeteorMode.OFF);
        CycleButton<SrpMeteorMode> mushroomTreesSelector = CycleButton.<SrpMeteorMode>builder(
                        mode -> Component.translatable(mushroomTreesKey(mode)), selectedMushroomTrees)
                .withValues(List.of(SrpMeteorMode.values()))
                .create((screen.width - width) / 2, screen.height - 124, width, 20,
                        Component.translatable("options.csrp.mushroom_trees"),
                        (button, mode) -> {
                            MUSHROOM_TREES_SELECTIONS.put(screen, mode);
                            updateMushroomTreesTooltip(button, mode);
                        });
        updateMushroomTreesTooltip(mushroomTreesSelector, selectedMushroomTrees);
        mushroomTreesSelector.visible = selectedStar == SrpStarType.COLD;
        event.addListener(mushroomTreesSelector);

        SrpMeteorMode selectedFractured = FRACTURED_SELECTIONS.getOrDefault(screen, SrpMeteorMode.OFF);
        CycleButton<SrpMeteorMode> fracturedSelector = CycleButton.<SrpMeteorMode>builder(
                        mode -> Component.translatable(fracturedKey(mode)), selectedFractured)
                .withValues(List.of(SrpMeteorMode.values()))
                .create((screen.width - width) / 2, screen.height - 148, width, 20,
                        Component.translatable("options.csrp.fractured"),
                        (button, mode) -> {
                            FRACTURED_SELECTIONS.put(screen, mode);
                            updateFracturedTooltip(button, mode);
                        });
        updateFracturedTooltip(fracturedSelector, selectedFractured);
        fracturedSelector.visible = selectedStar == SrpStarType.COLD;
        event.addListener(fracturedSelector);
    }

    private static String mushroomTreesKey(SrpMeteorMode mode) {
        return mode.enabled() ? "options.csrp.mushroom_trees.on" : "options.csrp.mushroom_trees.off";
    }

    private static String fracturedKey(SrpMeteorMode mode) {
        return mode.enabled() ? "options.csrp.fractured.on" : "options.csrp.fractured.off";
    }

    private static void updateMushroomTreesTooltip(CycleButton<SrpMeteorMode> button, SrpMeteorMode mode) {
        button.setTooltip(Tooltip.create(Component.translatable(mushroomTreesKey(mode) + ".description")));
    }

    private static void updateFracturedTooltip(CycleButton<SrpMeteorMode> button, SrpMeteorMode mode) {
        button.setTooltip(Tooltip.create(Component.translatable(fracturedKey(mode) + ".description")));
    }

    public static void stageSelection(CreateWorldScreen screen) {
        SrpDifficultySelection.stage(SELECTIONS.getOrDefault(screen, SrpDifficulty.NORMAL));
        SrpStarType starType = STAR_SELECTIONS.getOrDefault(screen, SrpStarType.NORMAL);
        SrpStarTypeSelection.stage(starType);
        SrpMeteorMode meteor = METEOR_SELECTIONS.get(screen);
        if (meteor != null) {
            SrpMeteorSelection.stage(meteor);
        }
        boolean cold = starType == SrpStarType.COLD;
        SrpColdStarSelection.stage(
                cold && FRACTURED_SELECTIONS.getOrDefault(screen, SrpMeteorMode.OFF).enabled(),
                cold && MUSHROOM_TREES_SELECTIONS.getOrDefault(screen, SrpMeteorMode.OFF).enabled());
    }

    private static boolean defaultMeteorEnabled() {
        try {
            return Config.meteorEnabled();
        } catch (IllegalStateException | NullPointerException configNotLoaded) {
            return false;
        }
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
