package alku.csrp.celestial.client;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Lightweight 1.20.1-compatible aurora hook. The shader is optional. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class AuroraSkyRenderer {
    private AuroraSkyRenderer() {}
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY
                && Minecraft.getInstance().level != null) {
            // Rendering is intentionally delegated to the vanilla sky in Forge 1.20.1.
        }
    }
    public static void dispose() {}
}
