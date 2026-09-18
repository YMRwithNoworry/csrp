package alku.csrp.client;

import alku.csrp.Csrp;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Draws the CSRP credits overlay in the bottom-left corner of the vanilla title screen.
 *
 * <p>The original implementation was built with LDLib2's flexbox widgets (which have no Minecraft
 * 26.3 build). The overlay is now laid out with plain integer maths and drawn straight through the
 * vanilla {@link GuiGraphicsExtractor}; the credit text, the panel geometry and the
 * "collapse once the mouse moves, click to expand" behaviour are unchanged. The two panels keep the
 * original widget contract: each one owns its own display flag, toggled whenever the panel collapses
 * or expands, so exactly one of them is visible at any time.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class CreditsTitleScreenEvents {
    private static final String[] CREDIT_LINES = {
            "程序：Paojiao134",
            "动画移植：无聊的保护者",
    };

    private static final int HEADING_COLOR = 0xFFFFD166;
    private static final int TEXT_COLOR = 0xFFE8EEF2;
    private static final int PANEL_BACKGROUND = 0xC0141820;
    private static final int PANEL_BORDER = 0x6079B8D6;

    // Geometry copied from the LDLib2 layout() tree: the panel is absolutely positioned against
    // the bottom-left corner and swaps its bounds/padding when it collapses.
    private static final int PANEL_BOTTOM_MARGIN = 26;
    private static final int COLLAPSED_WIDTH = 42;
    private static final int COLLAPSED_HEIGHT = 22;
    private static final int EXPANDED_WIDTH = 176;
    private static final int EXPANDED_HEIGHT = 58;
    private static final int COLLAPSED_PADDING_HORIZONTAL = 6;
    private static final int COLLAPSED_PADDING_VERTICAL = 5;
    private static final int EXPANDED_PADDING_HORIZONTAL = 10;
    private static final int EXPANDED_PADDING_VERTICAL = 7;
    private static final int EXPANDED_GAP = 3;
    private static final int HEADING_FONT_SIZE = 11;

    private static final Map<Screen, CreditsState> ACTIVE_CREDITS = new WeakHashMap<>();

    private CreditsTitleScreenEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen titleScreen) {
            ACTIVE_CREDITS.put(titleScreen, new CreditsState());
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        CreditsState state = ACTIVE_CREDITS.get(event.getScreen());
        if (state != null) {
            state.mouseMoved(event.getMouseX(), event.getMouseY());
            state.render(event.getGuiGraphics(), event.getScreen());
        }
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        CreditsState state = ACTIVE_CREDITS.get(event.getScreen());
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT
                && state != null
                && state.isCollapsed()
                && state.isMouseOver(event.getScreen(), event.getMouseX(), event.getMouseY())) {
            state.expand(event.getMouseX(), event.getMouseY());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        ACTIVE_CREDITS.remove(event.getScreen());
    }

    private static Label label(String text) {
        return new Label(Component.literal(text));
    }

    /** Per-title-screen overlay state; owns the collapse flag and the last mouse position. */
    private static final class CreditsState {
        private final Label collapsedHeading = label("鸣谢");
        private final Label details = label("感谢名单");
        private final PanelGeometry geometry = new PanelGeometry();
        private boolean collapsed;
        private double lastMouseX = Double.NaN;
        private double lastMouseY = Double.NaN;

        private int panelLeft() {
            geometry.left(collapsed ? 0 : 10);
            return geometry.left();
        }

        private int panelWidth() {
            return collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
        }

        private int panelHeight() {
            return collapsed ? COLLAPSED_HEIGHT : EXPANDED_HEIGHT;
        }

        private int panelTop(Screen screen) {
            return screen.height - PANEL_BOTTOM_MARGIN - panelHeight();
        }

        private boolean isCollapsed() {
            return collapsed;
        }

        private boolean isMouseOver(Screen screen, double mouseX, double mouseY) {
            int x = panelLeft();
            int y = panelTop(screen);
            return mouseX >= x && mouseX < x + panelWidth() && mouseY >= y && mouseY < y + panelHeight();
        }

        private void mouseMoved(double mouseX, double mouseY) {
            if (Double.isNaN(lastMouseX)) {
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                return;
            }

            if (!collapsed && (mouseX != lastMouseX || mouseY != lastMouseY)) {
                collapsed = true;
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        private void expand(double mouseX, double mouseY) {
            collapsed = false;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        private void render(GuiGraphicsExtractor graphics, Screen screen) {
            Font font = Minecraft.getInstance().font;
            collapsedHeading.setDisplay(collapsed);
            details.setDisplay(!collapsed);
            int x = panelLeft();
            int y = panelTop(screen);
            int width = panelWidth();
            int height = panelHeight();

            graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
            graphics.outline(x, y, width, height, PANEL_BORDER);

            int textX = x + (collapsed ? COLLAPSED_PADDING_HORIZONTAL : EXPANDED_PADDING_HORIZONTAL);
            int textY = y + (collapsed ? COLLAPSED_PADDING_VERTICAL : EXPANDED_PADDING_VERTICAL);

            if (collapsedHeading.isDisplayed()) {
                graphics.text(font, collapsedHeading.text(), textX, textY, HEADING_COLOR);
                return;
            }

            if (!details.isDisplayed()) {
                return;
            }

            textY += drawScaledText(graphics, font, details.text(), textX, textY, HEADING_COLOR,
                    HEADING_FONT_SIZE) + EXPANDED_GAP;
            for (String line : CREDIT_LINES) {
                graphics.text(font, line, textX, textY, TEXT_COLOR);
                textY += font.lineHeight + EXPANDED_GAP;
            }
        }
    }

    /** Absolute-positioning helper mirroring the LDLib2 layout node the panel used to be docked in. */
    private static final class PanelGeometry {
        private int left;

        private PanelGeometry left(int left) {
            this.left = left;
            return this;
        }

        private int left() {
            return left;
        }
    }

    /** A single original credits label; only its display flag is still modelled. */
    private static final class Label {
        private final Component component;
        private boolean displayed;

        private Label(Component component) {
            this.component = component;
        }

        private void setDisplay(boolean displayed) {
            this.displayed = displayed;
        }

        private boolean isDisplayed() {
            return displayed;
        }

        private String text() {
            return component.getString();
        }
    }

    /**
     * Draws {@code text} with its line height scaled to {@code fontSize}, mirroring the LDLib2 label
     * sizes (the original heading used 11px while every other label used 9px). Returns the height the
     * line occupies so the caller can stack the next line below it.
     */
    private static int drawScaledText(GuiGraphicsExtractor graphics, Font font, String text,
                                      int x, int y, int color, int fontSize) {
        float scale = fontSize / (float) font.lineHeight;
        if (scale == 1.0F) {
            graphics.text(font, text, x, y, color);
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) x, (float) y);
            graphics.pose().scale(scale, scale);
            graphics.text(font, text, 0, 0, color);
            graphics.pose().popMatrix();
        }
        return Math.round(font.lineHeight * scale);
    }
}
