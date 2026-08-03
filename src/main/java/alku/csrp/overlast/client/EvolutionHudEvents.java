package alku.csrp.overlast.client;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.overlast.network.EvolutionHudPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class EvolutionHudEvents {
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
        int width = 116;
        int height = 18;
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        String position = Config.overlastHudPosition();
        int x = position.endsWith("right") ? screenWidth - width - 10 : 10;
        int y = position.startsWith("bottom") ? screenHeight - height - 45
                : position.startsWith("middle") ? (screenHeight - height) / 2 : 10;
        int span = Math.max(1, state.nextThreshold() - state.currentThreshold());
        int progress = Math.max(0, Math.min(width - 4,
                (int) ((long) (state.points() - state.currentThreshold()) * (width - 4) / span)));
        int color = phaseColor(state.phase());
        graphics.fill(x, y, x + width, y + height, 0xCC151515);
        graphics.fill(x + 2, y + 2, x + 2 + progress, y + height - 2, color);
        graphics.renderOutline(x, y, width, height, 0xFFE4E4E4);
        Component text = Component.translatable("hud.csrp.overlast.evolution", state.phase(), state.points());
        graphics.drawString(minecraft.font, text, x + 5, y + 5, 0xFFFFFFFF, true);
    }

    private static int phaseColor(int phase) {
        if (phase < 0) return 0xFF4D6380;
        if (phase <= 2) return 0xFF5A8F45;
        if (phase <= 5) return 0xFFC29B36;
        if (phase <= 7) return 0xFFC45135;
        return 0xFF8E2536;
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
