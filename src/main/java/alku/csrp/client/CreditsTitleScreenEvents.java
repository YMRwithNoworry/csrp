package alku.csrp.client;

import alku.csrp.Csrp;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class CreditsTitleScreenEvents {
    private static final Map<Screen, CreditsState> ACTIVE_CREDITS = new WeakHashMap<>();

    private CreditsTitleScreenEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen titleScreen)) {
            return;
        }

        CreditsState previous = ACTIVE_CREDITS.remove(titleScreen);
        if (previous != null) {
            previous.ui().onRemoved();
        }

        CreditsState state = createCreditsUI();
        state.ui().setAllowDebugMode(false);
        state.ui().setScreenAndInit(titleScreen);
        event.addListener(state.ui().getWidget());
        ACTIVE_CREDITS.put(titleScreen, state);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        CreditsState state = ACTIVE_CREDITS.get(event.getScreen());
        if (state != null) {
            state.mouseMoved(event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        CreditsState state = ACTIVE_CREDITS.get(event.getScreen());
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && state != null
                && state.isCollapsed()
                && state.isMouseOver(event.getMouseX(), event.getMouseY())) {
            state.expand(event.getMouseX(), event.getMouseY());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        CreditsState state = ACTIVE_CREDITS.remove(event.getScreen());
        if (state != null) {
            state.ui().onRemoved();
        }
    }

    private static CreditsState createCreditsUI() {
        UIElement expandedHeading = label("感谢名单", 0xFFFFD166, 11);
        UIElement collapsedHeading = label("鸣谢", 0xFFFFD166, 9).setDisplay(false);
        UIElement details = new UIElement()
                .layout(layout -> layout.widthPercent(100).gapAll(3))
                .setAllowHitTest(false)
                .addChildren(
                        label("程序：Paojiao134", 0xFFE8EEF2, 9),
                        label("动画移植：无聊的保护者", 0xFFE8EEF2, 9));

        UIElement panel = new UIElement()
                .style(style -> style.background(
                        SDFRectTexture.of(0xC0141820)
                                .setRadius(4)
                                .setStroke(1)
                                .setBorderColor(0x6079B8D6)))
                .setAllowHitTest(false)
                .addChildren(expandedHeading, collapsedHeading, details);

        UIElement root = new UIElement()
                .layout(layout -> layout.widthPercent(100).heightPercent(100))
                .setAllowHitTest(false)
                .addChild(panel);

        CreditsState state = new CreditsState(
                ModularUI.of(UI.of(root)), panel, expandedHeading, collapsedHeading, details);
        state.applyLayout(false);
        return state;
    }

    private static UIElement label(String text, int color, float fontSize) {
        return new Label()
                .setText(text)
                .textStyle(style -> style
                        .adaptiveWidth(true)
                        .adaptiveHeight(true)
                        .fontSize(fontSize)
                        .textColor(color)
                        .textShadow(true))
                .setAllowHitTest(false);
    }

    private static final class CreditsState {
        private static final int COLLAPSED_WIDTH = 42;
        private static final int COLLAPSED_HEIGHT = 22;
        private static final int EXPANDED_WIDTH = 176;
        private static final int EXPANDED_HEIGHT = 58;

        private final ModularUI ui;
        private final UIElement panel;
        private final UIElement expandedHeading;
        private final UIElement collapsedHeading;
        private final UIElement details;
        private boolean collapsed;
        private double lastMouseX = Double.NaN;
        private double lastMouseY = Double.NaN;

        private CreditsState(ModularUI ui, UIElement panel, UIElement expandedHeading,
                             UIElement collapsedHeading, UIElement details) {
            this.ui = ui;
            this.panel = panel;
            this.expandedHeading = expandedHeading;
            this.collapsedHeading = collapsedHeading;
            this.details = details;
        }

        private ModularUI ui() {
            return ui;
        }

        private boolean isCollapsed() {
            return collapsed;
        }

        private boolean isMouseOver(double mouseX, double mouseY) {
            return panel.isMouseOver((float) mouseX, (float) mouseY);
        }

        private void mouseMoved(double mouseX, double mouseY) {
            if (Double.isNaN(lastMouseX)) {
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                return;
            }

            if (!collapsed && (mouseX != lastMouseX || mouseY != lastMouseY)) {
                applyLayout(true);
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        private void expand(double mouseX, double mouseY) {
            applyLayout(false);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        private void applyLayout(boolean collapsed) {
            this.collapsed = collapsed;
            panel.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(collapsed ? 0 : 10)
                    .bottom(26)
                    .width(collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH)
                    .height(collapsed ? COLLAPSED_HEIGHT : EXPANDED_HEIGHT)
                    .paddingHorizontal(collapsed ? 6 : 10)
                    .paddingVertical(collapsed ? 5 : 7)
                    .gapAll(collapsed ? 0 : 3));
            expandedHeading.setDisplay(!collapsed);
            collapsedHeading.setDisplay(collapsed);
            details.setDisplay(!collapsed);
        }
    }
}
