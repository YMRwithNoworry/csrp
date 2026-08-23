package alku.csrp.client;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.Map;
import java.util.WeakHashMap;

/** Small credits drawer shown on the vanilla title screen. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class CreditsTitleScreenEvents {
    private static final int COLLAPSED_WIDTH = 42;
    private static final int EXPANDED_WIDTH = 220;
    private static final int PANEL_HEIGHT = 96;
    private static final Map<Screen, CreditsState> STATES = new WeakHashMap<>();

    private CreditsTitleScreenEvents() {
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) {
            return;
        }
        CreditsState state = STATES.computeIfAbsent(screen, ignored -> new CreditsState());
        state.mouseMoved(event.getMouseX(), event.getMouseY());
        state.render(event.getGuiGraphics(), screen);
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof TitleScreen screen) || event.getButton() != 0) {
            return;
        }
        CreditsState state = STATES.computeIfAbsent(screen, ignored -> new CreditsState());
        if (state.isCollapsed() && state.isMouseOver(event.getMouseX(), event.getMouseY())) {
            state.setCollapsed(false);
            event.setCanceled(true);
        }
    }

    private static final class CreditsState {
        private final Label collapsedHeading = label("鸣谢");
        private final Label details = label("感谢名单");
        private boolean collapsed = true;
        private int left;
        private int top;
        private int width = COLLAPSED_WIDTH;

        private void mouseMoved(double mouseX, double mouseY) {
            if (!collapsed && !isMouseOver(mouseX, mouseY)) {
                collapsed = true;
            }
        }

        private boolean isCollapsed() {
            return collapsed;
        }

        private void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
        }

        private boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= left && mouseX <= left + width
                    && mouseY >= top && mouseY <= top + PANEL_HEIGHT;
        }

        private void render(GuiGraphics graphics, Screen screen) {
            width = collapsed ? COLLAPSED_WIDTH : EXPANDED_WIDTH;
            PanelGeometry geometry = new PanelGeometry().left(collapsed ? 0 : 10);
            left = geometry.left;
            top = Math.max(8, screen.height / 2 - PANEL_HEIGHT / 2);
            boolean dimmed = Minecraft.getInstance().screen != screen;
            graphics.fill(left, top, left + width, top + PANEL_HEIGHT, dimmed ? 0xCC10151B : 0xE610151B);
            collapsedHeading.setDisplay(collapsed);
            details.setDisplay(!collapsed);
            if (collapsedHeading.isDisplayed()) {
                graphics.drawString(Minecraft.getInstance().font, collapsedHeading.component,
                        left + 13, top + 38, 0xFFE6E9ED, false);
            }
            if (details.isDisplayed()) {
                graphics.drawString(Minecraft.getInstance().font, details.component,
                        left + 14, top + 14, 0xFFF2F4F5, false);
                graphics.drawString(Minecraft.getInstance().font,
                        Component.literal("SRParasites 1.10.8"), left + 14, top + 36, 0xFFB9C2CB, false);
                graphics.drawString(Minecraft.getInstance().font,
                        Component.literal("移植与维护：CSRP"), left + 14, top + 56, 0xFFB9C2CB, false);
                graphics.drawString(Minecraft.getInstance().font,
                        Component.literal("点击窄栏收起"), left + 14, top + 76, 0xFF7F8A96, false);
            }
        }

        private static Label label(String text) {
            return new Label(Component.literal(text));
        }
    }

    private static final class PanelGeometry {
        private int left;

        private PanelGeometry left(int left) {
            this.left = left;
            return this;
        }
    }

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
    }
}
