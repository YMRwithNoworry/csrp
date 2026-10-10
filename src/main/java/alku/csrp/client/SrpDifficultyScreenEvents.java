package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.world.SrpDifficulty;
import alku.csrp.world.SrpDifficultySelection;
import alku.csrp.world.SrpMeteorSelection;
import alku.csrp.world.SrpStarType;
import alku.csrp.world.SrpStarTypeSelection;
import alku.csrp.world.SrpStarWorldSelection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Keeps world selections on the create-world screen while its world tab edits them. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class SrpDifficultyScreenEvents {
    private static final Map<CreateWorldScreen, SrpDifficulty> SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, SrpStarType> STAR_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Boolean> METEOR_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Boolean> FRACTURED_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Boolean> MUSHROOM_SELECTIONS = new WeakHashMap<>();

    private SrpDifficultyScreenEvents() {
    }

    /** Adds a full-width block below vanilla's world controls. */
    public static void addWorldTabSelectors(GridLayout layout, CreateWorldScreen screen) {
        GridLayout options = new GridLayout().rowSpacing(4);
        GridLayout.RowHelper rows = options.createRowHelper(1);
        int width = 310;

        SrpDifficulty selected = SELECTIONS.getOrDefault(screen, SrpDifficulty.NORMAL);
        CycleButton<SrpDifficulty> selector = CycleButton.<SrpDifficulty>builder(
                        difficulty -> Component.translatable(difficulty.translationKey()))
                .withValues(List.of(SrpDifficulty.values()))
                .withInitialValue(selected)
                .create(0, 0, width, 20,
                        Component.translatable("options.csrp.difficulty"),
                        (button, difficulty) -> {
                            SELECTIONS.put(screen, difficulty);
                            updateTooltip(button, difficulty);
                        });
        updateTooltip(selector, selected);
        rows.addChild(selector);

        SrpStarType selectedStar = STAR_SELECTIONS.getOrDefault(screen, SrpStarType.NORMAL);
        CycleButton<SrpStarType> starSelector = CycleButton.<SrpStarType>builder(
                        starType -> Component.translatable(starType.translationKey()))
                .withValues(List.of(SrpStarType.values()))
                .withInitialValue(selectedStar)
                .create(0, 0, width, 20,
                        Component.translatable("options.csrp.star_type"),
                        (button, starType) -> {
                            STAR_SELECTIONS.put(screen, starType);
                            updateStarTooltip(button, starType);
                        });
        updateStarTooltip(starSelector, selectedStar);
        rows.addChild(starSelector);

        boolean meteorsEnabled = METEOR_SELECTIONS.getOrDefault(screen, WorldConfig.meteorsEnabled());
        CycleButton<Boolean> meteorSelector = CycleButton.<Boolean>builder(enabled -> Component.translatable(
                        enabled ? "options.csrp.meteors.enabled" : "options.csrp.meteors.disabled"))
                .withValues(List.of(Boolean.TRUE, Boolean.FALSE))
                .withInitialValue(meteorsEnabled)
                .create(0, 0, width, 20,
                        Component.translatable("options.csrp.meteors"),
                        (button, enabled) -> {
                            METEOR_SELECTIONS.put(screen, enabled);
                            updateMeteorTooltip(button, enabled);
                        });
        updateMeteorTooltip(meteorSelector, meteorsEnabled);
        rows.addChild(meteorSelector);

        boolean fracturedEnabled = FRACTURED_SELECTIONS.getOrDefault(screen, WorldConfig.fracturedTerrainEnabled());
        CycleButton<Boolean> fracturedSelector = CycleButton.<Boolean>builder(enabled -> Component.translatable(
                        enabled ? "options.csrp.fractured_terrain.enabled" : "options.csrp.fractured_terrain.disabled"))
                .withValues(List.of(Boolean.TRUE, Boolean.FALSE))
                .withInitialValue(fracturedEnabled)
                .create(0, 0, width, 20,
                        Component.translatable("options.csrp.fractured_terrain"),
                        (button, enabled) -> {
                            FRACTURED_SELECTIONS.put(screen, enabled);
                            updateToggleTooltip(button, enabled, "options.csrp.fractured_terrain");
                        });
        updateToggleTooltip(fracturedSelector, fracturedEnabled, "options.csrp.fractured_terrain");
        rows.addChild(fracturedSelector);

        boolean mushroomEnabled = MUSHROOM_SELECTIONS.getOrDefault(screen, WorldConfig.mushroomTreesEnabled());
        CycleButton<Boolean> mushroomSelector = CycleButton.<Boolean>builder(enabled -> Component.translatable(
                        enabled ? "options.csrp.mushroom_trees.enabled" : "options.csrp.mushroom_trees.disabled"))
                .withValues(List.of(Boolean.TRUE, Boolean.FALSE))
                .withInitialValue(mushroomEnabled)
                .create(0, 0, width, 20,
                        Component.translatable("options.csrp.mushroom_trees"),
                        (button, enabled) -> {
                            MUSHROOM_SELECTIONS.put(screen, enabled);
                            updateToggleTooltip(button, enabled, "options.csrp.mushroom_trees");
                        });
        updateToggleTooltip(mushroomSelector, mushroomEnabled, "options.csrp.mushroom_trees");
        rows.addChild(mushroomSelector);

        // WorldTab's vanilla grid has rows 0-2. Span both columns so the CSRP
        // buttons form one stable block below the seed and generation controls.
        layout.addChild(options, 3, 0, 1, 2);
    }

    public static void stageSelection(CreateWorldScreen screen) {
        SrpDifficultySelection.stage(SELECTIONS.getOrDefault(screen, SrpDifficulty.NORMAL));
        SrpStarTypeSelection.stage(STAR_SELECTIONS.getOrDefault(screen, SrpStarType.NORMAL));
        SrpMeteorSelection.stage(METEOR_SELECTIONS.getOrDefault(screen, WorldConfig.meteorsEnabled()));
        SrpStarWorldSelection.stage(
                FRACTURED_SELECTIONS.getOrDefault(screen, WorldConfig.fracturedTerrainEnabled()),
                MUSHROOM_SELECTIONS.getOrDefault(screen, WorldConfig.mushroomTreesEnabled()));
    }

    private static void updateTooltip(CycleButton<SrpDifficulty> button, SrpDifficulty difficulty) {
        button.setTooltip(Tooltip.create(Component.translatable(difficulty.descriptionKey())));
    }

    private static void updateStarTooltip(CycleButton<SrpStarType> button, SrpStarType starType) {
        button.setTooltip(Tooltip.create(Component.translatable(starType.descriptionKey())));
    }

    private static void updateMeteorTooltip(CycleButton<Boolean> button, boolean enabled) {
        button.setTooltip(Tooltip.create(Component.translatable(enabled
                ? "options.csrp.meteors.enabled.description"
                : "options.csrp.meteors.disabled.description")));
    }

    /** 供 fractured_terrain / mushroom_trees 两个开关复用：描述键由 base + enabled|disabled + .description 组成。 */
    private static void updateToggleTooltip(CycleButton<Boolean> button, boolean enabled, String baseKey) {
        button.setTooltip(Tooltip.create(Component.translatable(
                baseKey + (enabled ? ".enabled.description" : ".disabled.description"))));
    }
}
