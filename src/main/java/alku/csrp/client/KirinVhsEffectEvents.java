package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;

/** Replaces Kirin's opaque no-vision overlay with a readable VHS post effect. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class KirinVhsEffectEvents {
    private static final ResourceLocation EFFECT = new ResourceLocation(
            Csrp.MODID, "shaders/post/kirin_vhs.json");

    private static PostChain loadedEffect;
    private static boolean loadAttempted;

    private KirinVhsEffectEvents() {
    }

    @SubscribeEvent
    public static void updateEffect(ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
        Minecraft minecraft = Minecraft.getInstance();
        boolean shouldRender = minecraft.player != null
                && minecraft.player.hasEffect(ModMobEffects.NOVISION.get());

        if (!shouldRender) {
            unloadEffect(minecraft);
            loadAttempted = false;
            return;
        }
        if (loadedEffect != null || loadAttempted || minecraft.gameRenderer.currentEffect() != null) {
            return;
        }

        loadAttempted = true;
        minecraft.gameRenderer.loadEffect(EFFECT);
        loadedEffect = minecraft.gameRenderer.currentEffect();
    }

    private static void unloadEffect(Minecraft minecraft) {
        if (loadedEffect == null) {
            return;
        }
        if (minecraft.gameRenderer.currentEffect() == loadedEffect) {
            minecraft.gameRenderer.shutdownEffect();
        }
        loadedEffect = null;
    }
}
