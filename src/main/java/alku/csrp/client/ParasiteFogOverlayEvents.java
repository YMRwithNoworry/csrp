package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Draws the original animated block texture when the camera is inside parasite fog. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class ParasiteFogOverlayEvents {
    private static final Identifier FOG_SPRITE =
            Identifier.fromNamespaceAndPath(Csrp.MODID, "block/fog");

    private ParasiteFogOverlayEvents() {
    }

    @SubscribeEvent
    public static void renderFogOverlay(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.hud.isHidden() || !isCameraInsideFog(minecraft)) {
            return;
        }

        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        TextureAtlasSprite sprite = minecraft.getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(FOG_SPRITE);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 0, 0, width, height, 0xD9FFFFFF);
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
