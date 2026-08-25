package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.world.SrpDifficulty;
import alku.csrp.world.SrpDifficultySelection;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ScreenEvent;

/** Adds the SRP 1.10 difficulty selector to vanilla's create-world screen. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class SrpDifficultyScreenEvents {
    private static final ResourceLocation METEOR_ORBIT = new ResourceLocation(Csrp.MODID,
            "textures/gui/worldsettings/meteor_orbit.png");
    private static final Map<CreateWorldScreen, SrpDifficulty> SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, SrpStarType> STAR_SELECTIONS = new WeakHashMap<>();
    private static final Map<CreateWorldScreen, Boolean> METEOR_SELECTIONS = new WeakHashMap<>();

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

        boolean meteorsEnabled = METEOR_SELECTIONS.getOrDefault(screen, WorldConfig.meteorsEnabled());
        CycleButton<Boolean> meteorSelector = CycleButton.<Boolean>builder(enabled -> Component.translatable(
                        enabled ? "options.csrp.meteors.enabled" : "options.csrp.meteors.disabled"))
                .withValues(List.of(Boolean.TRUE, Boolean.FALSE))
                .withInitialValue(meteorsEnabled)
                .create((screen.width - width) / 2, screen.height - 100, width, 20,
                        Component.translatable("options.csrp.meteors"),
                        (button, enabled) -> {
                            METEOR_SELECTIONS.put(screen, enabled);
                            updateMeteorTooltip(button, enabled);
                        });
        updateMeteorTooltip(meteorSelector, meteorsEnabled);
        event.addListener(meteorSelector);
    }

    @SubscribeEvent
    public static void renderMeteorPreview(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)
                || !METEOR_SELECTIONS.getOrDefault(screen, WorldConfig.meteorsEnabled())) {
            return;
        }
        int width = Math.min(180, Math.max(120, screen.width - 20));
        int x = (screen.width - width) / 2 - 38;
        if (x >= 2) {
            event.getGuiGraphics().blit(METEOR_ORBIT, x, screen.height - 106,
                    0.0F, 0.0F, 32, 32, 32, 32);
        }
    }

    public static void stageSelection(CreateWorldScreen screen) {
        SrpDifficultySelection.stage(SELECTIONS.getOrDefault(screen, SrpDifficulty.NORMAL));
        SrpStarTypeSelection.stage(STAR_SELECTIONS.getOrDefault(screen, SrpStarType.NORMAL));
        SrpMeteorSelection.stage(METEOR_SELECTIONS.getOrDefault(screen, WorldConfig.meteorsEnabled()));
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
}
