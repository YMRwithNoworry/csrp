package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/** Runs the original breathing distortion after the local player drinks alveolar fluid. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class AlveolarFluidClientEvents {
    private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "shaders/post/alveolar_breathe.json");
    private static final int EFFECT_DURATION_TICKS = 600;

    private static PostChain loadedEffect;
    private static int ticksRemaining;
    private static boolean loadAttempted;

    private AlveolarFluidClientEvents() {
    }

    @SubscribeEvent
    public static void finishUsingItem(LivingEntityUseItemEvent.Finish event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() == minecraft.player && event.getItem().is(ModItems.ALVEOLAR_FLUID.get())) {
            ticksRemaining = Math.max(ticksRemaining, EFFECT_DURATION_TICKS);
            loadAttempted = false;
        }
    }

    @SubscribeEvent
    public static void updateEffect(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || ticksRemaining <= 0) {
            unloadEffect(minecraft);
            ticksRemaining = 0;
            loadAttempted = false;
            return;
        }

        ticksRemaining--;
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
