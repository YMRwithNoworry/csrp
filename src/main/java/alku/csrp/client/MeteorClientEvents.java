package alku.csrp.client;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class MeteorClientEvents {
    private static int shakeTicks;
    private static int totalTicks;
    private static float shakeStrength;
    private static int darkTicks;

    private MeteorClientEvents() {
    }

    public static void startEffect(int ticks, float strength, boolean darken) {
        if (darken) {
            darkTicks = 40;
        }
        if (ticks > 0 && strength > 0.0F && (strength >= shakeStrength || ticks > shakeTicks)) {
            shakeTicks = ticks;
            totalTicks = ticks;
            shakeStrength = strength;
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && shakeTicks > 0 && !Minecraft.getInstance().isPaused()) {
            shakeTicks--;
            if (shakeTicks == 0) {
                shakeStrength = 0.0F;
            }
        }
        if (event.phase == TickEvent.Phase.END && darkTicks > 0 && !Minecraft.getInstance().isPaused()) {
            darkTicks--;
        }
    }

    @SubscribeEvent
    public static void darkenScreen(RenderGuiEvent.Post event) {
        if (darkTicks <= 0) {
            return;
        }
        int elapsed = 40 - darkTicks;
        float intensity = elapsed < 10 ? elapsed / 10.0F
                : elapsed < 20 ? 1.0F : Math.max(0.0F, darkTicks / 20.0F);
        int alpha = Math.min(153, Math.max(0, (int) (153.0F * intensity)));
        Minecraft minecraft = Minecraft.getInstance();
        event.getGuiGraphics().fill(0, 0, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(), alpha << 24);
    }

    @SubscribeEvent
    public static void shakeCamera(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || shakeTicks <= 0) {
            return;
        }
        float fade = totalTicks <= 0 ? 0.0F : shakeTicks / (float) totalTicks;
        double time = player.tickCount + event.getPartialTick();
        float strength = shakeStrength * Math.min(1.0F, fade * 2.0F);
        event.setYaw(event.getYaw() + (float) Math.sin(time * 3.7D) * strength);
        event.setPitch(event.getPitch() + (float) Math.cos(time * 4.1D) * strength * 0.7F);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 5.3D) * strength * 0.8F);
    }
}
