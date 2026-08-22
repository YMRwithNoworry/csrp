package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModMobEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RenderGuiEvent;

/** Original SRP full-screen overlays for its vision-affecting status effects. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class StatusEffectOverlayEvents {
    private static final ResourceLocation VIRAL = texture("screen_viral.png");
    private static final ResourceLocation BLEED = texture("screen_bleed.png");
    private static final ResourceLocation VOMIT = texture("screen_vomit.png");
    private static final ResourceLocation DISTORTED = texture("screen_distorted.png");
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

        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            if (player.hasEffect(ModMobEffects.VIRAL.get())) {
                drawFullScreen(graphics, VIRAL, width, height);
            }
            if (player.hasEffect(ModMobEffects.BLEED.get())) {
                drawFullScreen(graphics, BLEED, width, height);
            }
            if (player.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT.get())) {
                float pulse = 0.22F + 0.08F * (float) Math.sin(player.tickCount * 0.2F);
                RenderSystem.setShaderColor(0.85F, 0.65F, 1.0F, pulse);
                graphics.blit(DISTORTED, 0, 0, 0.0F, 0.0F,
                        width, height, 32, 32);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (player.hasEffect(ModMobEffects.VOMIT.get())) {
                int textureHeight = height * 8;
                graphics.blit(VOMIT, 0, vomitY, 0.0F, 0.0F,
                        width, textureHeight, width, textureHeight);
                if (++vomitY >= 0) {
                    vomitY = -height * 7;
                }
            } else {
                vomitY = 0;
            }
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }

    private static void drawFullScreen(GuiGraphics graphics, ResourceLocation texture, int width, int height) {
        graphics.blit(texture, 0, 0, 0.0F, 0.0F, width, height, width, height);
    }

    private static ResourceLocation texture(String file) {
        return new ResourceLocation(Csrp.MODID, "textures/gui/" + file);
    }
}
