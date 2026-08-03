package alku.csrp.overlast.client;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.overlast.network.EvolutionHudPayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class EvolutionHudEvents {
    private static final int FULL_WIDTH = 113;
    private static final int FULL_HEIGHT = 29;
    private static final int BAR_WIDTH = 80;
    private static final int BAR_TEXTURE_X = 23;
    private static final int BAR_TEXTURE_Y = 32;
    private static final int TEXTURE_SIZE = 256;

    public static final KeyMapping TOGGLE = new KeyMapping("key.csrp.overlast_hud",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_COMMA, "key.categories.csrp");

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
        if (!EvolutionHudState.shouldRender() || minecraft.options.hideGui || minecraft.player == null) {
            return;
        }
        EvolutionHudPayload state = EvolutionHudState.state();
        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        String position = Config.overlastHudPosition();
        int x = position.endsWith("right") ? screenWidth - 2 : 150;
        int y = position.startsWith("bottom") ? screenHeight - 80
                : position.startsWith("middle") ? screenHeight / 2 - 30 : 10;
        int progress = progressWidth(state);
        int texturePhase = Math.max(-2, Math.min(8, state.phase()));
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                "textures/gui/overlast/evolutionbar" + texturePhase + ".png");

        if (progress > 0) {
            graphics.blit(texture, x - BAR_WIDTH - 10, y + 3, BAR_TEXTURE_X, BAR_TEXTURE_Y,
                    progress, FULL_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        }
        graphics.blit(texture, x - FULL_WIDTH, y, 0, 0,
                FULL_WIDTH, FULL_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.drawCenteredString(minecraft.font, Component.literal(formatPoints(state.points())),
                x - FULL_WIDTH - 15, y + 13, 0xFFFFFFFF);
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

    @EventBusSubscriber(modid = Csrp.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE);
        }
    }
}
