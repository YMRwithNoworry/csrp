package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModItems;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/** Runs the original breathing distortion after the local player drinks alveolar fluid. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class AlveolarFluidClientEvents {
    private static final Identifier EFFECT = Identifier.fromNamespaceAndPath(
            Csrp.MODID, "alveolar_breathe");
    private static final int EFFECT_DURATION_TICKS = 600;

    private static boolean effectActive;
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
        List<Identifier> activeEffects = minecraft.player.getActivePostEffects();
        if (effectActive) {
            if (!activeEffects.contains(EFFECT)) {
                activeEffects.add(EFFECT);
            }
            return;
        }
        if (loadAttempted || !activeEffects.isEmpty()) {
            return;
        }

        loadAttempted = true;
        activeEffects.add(EFFECT);
        effectActive = true;
    }

    private static void unloadEffect(Minecraft minecraft) {
        if (!effectActive) {
            return;
        }
        if (minecraft.player != null) {
            minecraft.player.getActivePostEffects().remove(EFFECT);
        }
        effectActive = false;
    }
}
