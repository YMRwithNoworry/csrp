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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Keeps world selections on the parent while a dedicated settings screen edits them. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class SrpDifficultyScreenEvents {
    private static final ResourceLocation METEOR_ORBIT = new ResourceLocation(Csrp.MODID,
            "textures/gui/worldsettings/meteor_orbit.png");
    private static final Map<CreateWorldScreen, SrpDifficulty> SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, SrpStarType> STAR_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Boolean> METEOR_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Boolean> FRACTURED_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Boolean> MUSHROOM_SELECTIONS = new WeakHashMap<>();

    private SrpDifficultyScreenEvents() {
    }

    public static void addSelectors(SrpWorldOptionsScreen options, CreateWorldScreen screen) {
        SrpDifficulty selected = SELECTIONS.getOrDefault(screen, SrpDifficulty.NORMAL);
        int width = Math.min(310, options.width - 20);
        int leftX = (options.width - width) / 2;
        int firstY = Math.max(32, (options.height - 120) / 2);
        CycleButton<SrpDifficulty> selector = CycleButton.<SrpDifficulty>builder(
                        difficulty -> Component.translatable(difficulty.translationKey()))
                .withValues(List.of(SrpDifficulty.values()))
                .withInitialValue(selected)
                .create(leftX, firstY, width, 20,
                        Component.translatable("options.csrp.difficulty"),
                        (button, difficulty) -> {
                            SELECTIONS.put(screen, difficulty);
                            updateTooltip(button, difficulty);
                        });
        updateTooltip(selector, selected);
        options.addOption(selector);

        SrpStarType selectedStar = STAR_SELECTIONS.getOrDefault(screen, SrpStarType.NORMAL);
        CycleButton<SrpStarType> starSelector = CycleButton.<SrpStarType>builder(
                        starType -> Component.translatable(starType.translationKey()))
                .withValues(List.of(SrpStarType.values()))
                .withInitialValue(selectedStar)
                .create(leftX, firstY + 24, width, 20,
                        Component.translatable("options.csrp.star_type"),
                        (button, starType) -> {
                            STAR_SELECTIONS.put(screen, starType);
                            updateStarTooltip(button, starType);
                        });
        updateStarTooltip(starSelector, selectedStar);
        options.addOption(starSelector);

        boolean meteorsEnabled = METEOR_SELECTIONS.getOrDefault(screen, WorldConfig.meteorsEnabled());
        CycleButton<Boolean> meteorSelector = CycleButton.<Boolean>builder(enabled -> Component.translatable(
                        enabled ? "options.csrp.meteors.enabled" : "options.csrp.meteors.disabled"))
                .withValues(List.of(Boolean.TRUE, Boolean.FALSE))
                .withInitialValue(meteorsEnabled)
                .create(leftX, firstY + 48, width, 20,
                        Component.translatable("options.csrp.meteors"),
                        (button, enabled) -> {
                            METEOR_SELECTIONS.put(screen, enabled);
                            updateMeteorTooltip(button, enabled);
                        });
        updateMeteorTooltip(meteorSelector, meteorsEnabled);
        options.addOption(meteorSelector);

        boolean fracturedEnabled = FRACTURED_SELECTIONS.getOrDefault(screen, WorldConfig.fracturedTerrainEnabled());
        CycleButton<Boolean> fracturedSelector = CycleButton.<Boolean>builder(enabled -> Component.translatable(
                        enabled ? "options.csrp.fractured_terrain.enabled" : "options.csrp.fractured_terrain.disabled"))
                .withValues(List.of(Boolean.TRUE, Boolean.FALSE))
                .withInitialValue(fracturedEnabled)
                .create(leftX, firstY + 72, width, 20,
                        Component.translatable("options.csrp.fractured_terrain"),
                        (button, enabled) -> {
                            FRACTURED_SELECTIONS.put(screen, enabled);
                            updateToggleTooltip(button, enabled, "options.csrp.fractured_terrain");
                        });
        updateToggleTooltip(fracturedSelector, fracturedEnabled, "options.csrp.fractured_terrain");
        options.addOption(fracturedSelector);

        boolean mushroomEnabled = MUSHROOM_SELECTIONS.getOrDefault(screen, WorldConfig.mushroomTreesEnabled());
        CycleButton<Boolean> mushroomSelector = CycleButton.<Boolean>builder(enabled -> Component.translatable(
                        enabled ? "options.csrp.mushroom_trees.enabled" : "options.csrp.mushroom_trees.disabled"))
                .withValues(List.of(Boolean.TRUE, Boolean.FALSE))
                .withInitialValue(mushroomEnabled)
                .create(leftX, firstY + 96, width, 20,
                        Component.translatable("options.csrp.mushroom_trees"),
                        (button, enabled) -> {
                            MUSHROOM_SELECTIONS.put(screen, enabled);
                            updateToggleTooltip(button, enabled, "options.csrp.mushroom_trees");
                        });
        updateToggleTooltip(mushroomSelector, mushroomEnabled, "options.csrp.mushroom_trees");
        options.addOption(mushroomSelector);
    }

    public static void renderMeteorPreview(SrpWorldOptionsScreen options, CreateWorldScreen screen,
            GuiGraphics graphics) {
        if (!METEOR_SELECTIONS.getOrDefault(screen, WorldConfig.meteorsEnabled())) {
            return;
        }
        int width = Math.min(310, options.width - 20);
        int x = (options.width - width) / 2 - 36;
        int y = Math.max(32, (options.height - 120) / 2) + 42;
        if (x >= 2) {
            graphics.blit(METEOR_ORBIT, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
        }
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
