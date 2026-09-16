package alku.csrp.overlast.client;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.overlast.network.EvolutionHudPayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class EvolutionHudEvents {
    private static final int FULL_WIDTH = 113;
    private static final int FULL_HEIGHT = 29;
    private static final int BAR_WIDTH = 80;
    private static final int BAR_TEXTURE_X = 23;
    private static final int BAR_TEXTURE_Y = 32;
    private static final int TEXTURE_SIZE = 256;
    private static final int POINT_TEXT_MAX_WIDTH = 48;
    private static final int SCREEN_MARGIN = 2;
    private static final int PHASE_BADGE_WIDTH = 11;
    private static final int PHASE_BADGE_HEIGHT = 10;
    private static final int PHASE_TEXT_MAX_WIDTH = 8;
    private static final int PHASE_BADGE_BORDER = 0xFF6B174F;
    private static final int PHASE_BADGE_BACKGROUND = 0xFF090008;

    public static final KeyMapping TOGGLE = new KeyMapping("key.csrp.overlast_hud",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_COMMA, KeyMapping.Category.MISC);

    private EvolutionHudEvents() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        while (TOGGLE.consumeClick()) {
            EvolutionHudState.toggle();
        }
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!EvolutionHudState.shouldRender() || minecraft.gui.hud.isHidden() || minecraft.player == null) {
            return;
        }
        EvolutionHudPayload state = EvolutionHudState.state();
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        String position = Config.overlastHudPosition();
        int x = position.endsWith("right") ? screenWidth - 2 : 150;
        int y = position.startsWith("bottom") ? screenHeight - 80
                : position.startsWith("middle") ? screenHeight / 2 - 30 : 10;
        int progress = progressWidth(state);
        int texturePhase = Math.max(-2, Math.min(8, state.phase()));
        Identifier texture = Identifier.fromNamespaceAndPath(Csrp.MODID,
                "textures/gui/overlast/evolutionbar" + texturePhase + ".png");

        if (progress > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x - BAR_WIDTH - 10, y + 3, BAR_TEXTURE_X, BAR_TEXTURE_Y,
                    progress, FULL_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x - FULL_WIDTH, y, 0, 0,
                FULL_WIDTH, FULL_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        drawPhaseBadge(graphics, minecraft.font, state.phase(), x - FULL_WIDTH, y);

        int pointsCenterX = x - FULL_WIDTH - 15;
        int safeHalfWidth = Math.max(1, Math.min(pointsCenterX - SCREEN_MARGIN,
                screenWidth - SCREEN_MARGIN - pointsCenterX));
        int pointTextMaxWidth = Math.min(POINT_TEXT_MAX_WIDTH, safeHalfWidth * 2);
        drawFittedCenteredString(graphics, minecraft.font, Component.literal(formatPoints(state.points())),
                pointsCenterX, y + 13, pointTextMaxWidth, 0xFFFFFFFF);
    }

    private static void drawPhaseBadge(GuiGraphicsExtractor graphics, Font font, int phase, int x, int y) {
        graphics.fill(x, y, x + PHASE_BADGE_WIDTH, y + PHASE_BADGE_HEIGHT, PHASE_BADGE_BORDER);
        graphics.fill(x + 1, y + 1, x + PHASE_BADGE_WIDTH - 1, y + PHASE_BADGE_HEIGHT - 1,
                PHASE_BADGE_BACKGROUND);
        drawFittedCenteredString(graphics, font, Component.literal(Integer.toString(phase)),
                x + PHASE_BADGE_WIDTH / 2, y + 1, PHASE_TEXT_MAX_WIDTH, 0xFFFFB6E6);
    }

    private static void drawFittedCenteredString(GuiGraphicsExtractor graphics, Font font, Component text,
            int centerX, int y, int maxWidth, int color) {
        int textWidth = Math.max(1, font.width(text));
        float scale = Math.min(1.0F, Math.max(1, maxWidth) / (float) textWidth);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y);
        graphics.pose().scale(scale, scale);
        graphics.centeredText(font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    private static int progressWidth(EvolutionHudPayload state) {
        if (state.phase() >= 10) {
            return BAR_WIDTH;
        }
        int span = state.nextThreshold() - state.currentThreshold();
        if (span <= 0) {
            return 0;
        }
        long progress = (long) (state.points() - state.currentThreshold()) * BAR_WIDTH / span;
        return (int) Math.max(0L, Math.min(BAR_WIDTH, progress));
    }

    private static String formatPoints(int points) {
        if (points >= 1_000_000) {
            return String.format(Locale.ROOT, "%.2fwP", points / 10_000.0D);
        }
        return String.format(Locale.ROOT, "%.1fP", (double) points);
    }

    @EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE);
        }
    }
}
