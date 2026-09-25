package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.config.GeneralConfig;
import alku.csrp.world.SrpColdStarSelection;
import alku.csrp.world.SrpDifficulty;
import alku.csrp.world.SrpDifficultySelection;
import alku.csrp.world.SrpMeteorMode;
import alku.csrp.world.SrpMeteorSelection;
import alku.csrp.world.SrpStarType;
import alku.csrp.world.SrpStarTypeSelection;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * SRP world-creation integration, mirroring 1.10.9 {@code client/gui/SRPWorldCreateButtons}:
 * the create-world screen gets a single <em>"SRP World Options..."</em> button (gated by the
 * {@code worldCreationUi} config, the port of {@code SRPConfig.worldGIU}) which opens the dedicated
 * {@link SrpWorldSettingsScreen}.  The five cycle buttons live in that screen — the original never
 * injected them into the vanilla create-world screen.
 *
 * <p>The per-screen selections are kept here so {@link #stageSelection(CreateWorldScreen)} can hand
 * them to the integrated server exactly once, as before.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class SrpDifficultyScreenEvents {
    /** {@code SRPWorldCreateButtons.BTN_SRP_WORLD_SETTINGS}. */
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MARGIN = 8;
    private static final Map<CreateWorldScreen, Selections> SELECTIONS = new WeakHashMap<>();

    private SrpDifficultyScreenEvents() {
    }

    /** The five create-world choices, shared between the entry button and the settings screen. */
    static final class Selections {
        SrpDifficulty difficulty = SrpDifficulty.NORMAL;
        SrpStarType starType = SrpStarType.NORMAL;
        SrpMeteorMode meteor = SrpMeteorMode.of(defaultMeteorEnabled());
        SrpMeteorMode mushroomTrees = SrpMeteorMode.OFF;
        SrpMeteorMode fracturedTerrain = SrpMeteorMode.OFF;
    }

    @SubscribeEvent
    public static void addWorldOptionsButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) {
            return;
        }
        if (!worldCreationUiEnabled()) {
            return;
        }
        SELECTIONS.computeIfAbsent(screen, ignored -> new Selections());
        Button button = Button.builder(Component.translatable("gui.csrp.worldsettings.open"),
                        clicked -> Minecraft.getInstance().setScreenAndShow(new SrpWorldSettingsScreen(screen)))
                .bounds(BUTTON_MARGIN, footerRowY(screen), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        button.setTooltip(Tooltip.create(Component.translatable("gui.csrp.worldsettings.title")));
        event.addListener(button);
    }

    /**
     * Places the button in the footer row next to vanilla's create/cancel buttons, so it follows
     * 26.3's {@code HeaderAndFooterLayout} instead of a hard-coded screen offset.  The footer is the
     * bottom-most button row; the left margin keeps our button clear of the centred vanilla ones.
     *
     * <p>The row is located by position rather than by comparing a vanilla translation key, so the
     * port never has to reference or ship vanilla lang entries.</p>
     */
    private static int footerRowY(CreateWorldScreen screen) {
        int footerY = Integer.MIN_VALUE;
        for (var child : screen.children()) {
            if (child instanceof Button button) {
                footerY = Math.max(footerY, button.getY());
            }
        }
        return footerY == Integer.MIN_VALUE ? screen.height - 28 : footerY;
    }

    private static boolean worldCreationUiEnabled() {
        try {
            return GeneralConfig.worldCreationUi();
        } catch (IllegalStateException | NullPointerException configNotLoaded) {
            return true;
        }
    }

    static SrpDifficulty pendingDifficulty(CreateWorldScreen screen) {
        return selections(screen).difficulty;
    }

    static SrpStarType pendingStarType(CreateWorldScreen screen) {
        return selections(screen).starType;
    }

    static SrpMeteorMode pendingMeteor(CreateWorldScreen screen) {
        return selections(screen).meteor;
    }

    static SrpMeteorMode pendingMushroomTrees(CreateWorldScreen screen) {
        return selections(screen).mushroomTrees;
    }

    static SrpMeteorMode pendingFracturedTerrain(CreateWorldScreen screen) {
        return selections(screen).fracturedTerrain;
    }

    private static Selections selections(CreateWorldScreen screen) {
        return SELECTIONS.computeIfAbsent(screen, ignored -> new Selections());
    }

    /** Called by {@link SrpWorldSettingsScreen} when "Done" is pressed. */
    static void applySelections(CreateWorldScreen screen, SrpDifficulty difficulty, SrpStarType starType,
            SrpMeteorMode meteor, SrpMeteorMode mushroomTrees, SrpMeteorMode fracturedTerrain) {
        Selections state = selections(screen);
        state.difficulty = difficulty;
        state.starType = starType;
        state.meteor = meteor;
        state.mushroomTrees = mushroomTrees;
        state.fracturedTerrain = fracturedTerrain;
    }

    /** Called from {@code CreateWorldScreenMixin} while the world is being created. */
    public static void stageSelection(CreateWorldScreen screen) {
        Selections state = SELECTIONS.get(screen);
        if (state == null) {
            return;
        }
        SrpDifficultySelection.stage(state.difficulty);
        SrpStarTypeSelection.stage(state.starType);
        SrpMeteorSelection.stage(state.meteor);
        boolean cold = state.starType == SrpStarType.COLD;
        SrpColdStarSelection.stage(cold && state.fracturedTerrain.enabled(),
                cold && state.mushroomTrees.enabled());
    }

    private static boolean defaultMeteorEnabled() {
        try {
            return Config.meteorEnabled();
        } catch (IllegalStateException | NullPointerException configNotLoaded) {
            return false;
        }
    }
}
