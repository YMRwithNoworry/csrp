package alku.csrp.client;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class MeteorClientEvents {
    private static int shakeTicks;
    private static int totalTicks;
    private static float shakeStrength;

    private MeteorClientEvents() {
    }

    public static void startShake(int ticks, float strength) {
        if (ticks <= 0 || strength <= 0.0F) {
            return;
        }
        if (strength >= shakeStrength || ticks > shakeTicks) {
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
