package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Original SRP full-screen overlays for its vision-affecting status effects. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class StatusEffectOverlayEvents {
    private static final Identifier VIRAL = texture("screen_viral.png");
    private static final Identifier BLEED = texture("screen_bleed.png");
    private static final Identifier VOMIT = texture("screen_vomit.png");
    private static final Identifier DISTORTED = texture("screen_distorted.png");
    private static int vomitY;

    private StatusEffectOverlayEvents() {
    }

    @SubscribeEvent
    public static void renderOverlays(RenderGuiEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            vomitY = 0;
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (player.hasEffect(ModMobEffects.VIRAL)) {
            drawFullScreen(graphics, VIRAL, width, height);
        }
        if (player.hasEffect(ModMobEffects.BLEED)) {
            drawFullScreen(graphics, BLEED, width, height);
        }
        if (player.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT)) {
            float pulse = 0.22F + 0.08F * (float) Math.sin(player.tickCount * 0.2F);
            graphics.blit(RenderPipelines.GUI_TEXTURED, DISTORTED, 0, 0, 0.0F, 0.0F,
                    width, height, 32, 32, ARGB.color(pulse, 0xD9A6FF));
        }
        if (player.hasEffect(ModMobEffects.VOMIT)) {
            int textureHeight = height * 8;
            graphics.blit(RenderPipelines.GUI_TEXTURED, VOMIT, 0, vomitY, 0.0F, 0.0F,
                    width, textureHeight, width, textureHeight);
            if (++vomitY >= 0) {
                vomitY = -height * 7;
            }
        } else {
            vomitY = 0;
        }
    }

    private static void drawFullScreen(GuiGraphicsExtractor graphics, Identifier texture, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, width, height, width, height);
    }

    private static Identifier texture(String file) {
        return Identifier.fromNamespaceAndPath(Csrp.MODID, "textures/gui/" + file);
    }
}
