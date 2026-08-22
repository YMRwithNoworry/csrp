package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;

/** Runs the original breathing distortion after the local player drinks alveolar fluid. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class AlveolarFluidClientEvents {
    private static final ResourceLocation EFFECT = new ResourceLocation(
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
    public static void updateEffect(ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
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
