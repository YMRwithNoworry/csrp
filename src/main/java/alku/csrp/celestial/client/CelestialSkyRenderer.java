package alku.csrp.celestial.client;

import alku.csrp.Csrp;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Celestial state hook retained for the 1.20.1 renderer event API. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class CelestialSkyRenderer {
    private CelestialSkyRenderer() {}
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY
                && Minecraft.getInstance().level != null) CelestialClientState.active();
    }
    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (CelestialClientState.isActive("dark_days")) {
            event.setRed(0); event.setGreen(0); event.setBlue(0);
        }
    }
    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        if (CelestialClientState.isActive("dark_days")) {
            event.setNearPlaneDistance(0.0F); event.setFarPlaneDistance(24.0F);
            event.setFogShape(FogShape.SPHERE); event.setCanceled(true);
        }
    }
}
