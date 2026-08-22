package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RenderGuiEvent;

/** Draws the original animated block texture when the camera is inside parasite fog. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class ParasiteFogOverlayEvents {
    private static final ResourceLocation FOG_SPRITE =
            new ResourceLocation(Csrp.MODID, "block/fog");

    private ParasiteFogOverlayEvents() {
    }

    @SubscribeEvent
    public static void renderFogOverlay(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !isCameraInsideFog(minecraft)) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(FOG_SPRITE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.blit(0, 0, -90, width, height, sprite, 1.0F, 1.0F, 1.0F, 0.85F);
            graphics.flush();
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    private static boolean isCameraInsideFog(Minecraft minecraft) {
        if (minecraft.level == null) {
            return false;
        }
        if (minecraft.player == null) {
            return false;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        return minecraft.level.getBlockState(BlockPos.containing(eye)).is(ModBlocks.FOG.get());
    }
}
