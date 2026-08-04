package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Replaces Kirin's opaque no-vision overlay with a readable VHS post effect. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class KirinVhsEffectEvents {
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "shaders/post/kirin_vhs.json");

    private static PostChain loadedEffect;
    private static boolean loadAttempted;

    private KirinVhsEffectEvents() {
    }

    @SubscribeEvent
    public static void updateEffect(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean shouldRender = minecraft.player != null
                && minecraft.player.hasEffect(ModMobEffects.NOVISION);

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
