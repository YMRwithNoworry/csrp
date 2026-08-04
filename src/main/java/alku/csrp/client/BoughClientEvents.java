package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.item.BoughItem;
import alku.csrp.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Darkening and violent shake shown while the Bough ritual is completing. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class BoughClientEvents {
    private BoughClientEvents() {
    }

    @SubscribeEvent
    public static void renderDarkening(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!isUsingBough(player)) {
            return;
        }
        float progress = ritualProgress(player, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        int alpha = Mth.clamp((int) (progress * 210.0F), 0, 210);
        event.getGuiGraphics().fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), alpha << 24);
    }

    @SubscribeEvent
    public static void shakeCamera(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!isUsingBough(player)) {
            return;
        }
        float progress = ritualProgress(player, (float) event.getPartialTick());
        double time = player.tickCount + event.getPartialTick();
        float strength = progress * progress * 1.4F;
        event.setYaw(event.getYaw() + (float) Math.sin(time * 3.7D) * strength);
        event.setPitch(event.getPitch() + (float) Math.cos(time * 4.3D) * strength * 0.7F);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 5.1D) * strength * 0.8F);
    }

    private static boolean isUsingBough(LocalPlayer player) {
        return player != null && player.isUsingItem() && player.getUseItem().is(ModItems.BOUGH);
    }

    private static float ritualProgress(LocalPlayer player, float partialTick) {
        float elapsed = BoughItem.USE_DURATION - player.getUseItemRemainingTicks() + partialTick;
        return Mth.clamp(elapsed / BoughItem.USE_DURATION, 0.0F, 1.0F);
    }
}
