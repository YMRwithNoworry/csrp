package alku.csrp.client;

import alku.csrp.Csrp;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Port of SRParasites 1.10.8 {@code ClientQlipShake}. The meteor launch darkens the sky briefly,
 * falling meteors shake the camera, and the root impact produces a long violent shake.
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class MeteorShakeClient {
    private static final int RAMP_UP_TICKS = 10;
    private static final int HOLD_TICKS = 10;
    private static final int FADE_TICKS = 20;
    private static final int TOTAL_TICKS = 40;
    private static final Random RNG = new Random();

    private static int delayLeft;
    private static int elapsed = TOTAL_TICKS;
    private static boolean darkScreen;
    private static boolean shakeScreen;
    private static float shakeScreenValue;
    private static int durationShake;

    private MeteorShakeClient() {
    }

    public static void triggerDelayed(int durationTicks, int delayTicks, boolean dark, boolean shake,
            float value) {
        shakeScreen = shake;
        darkScreen = dark;
        durationShake = durationTicks;
        shakeScreenValue = value;
        delayLeft = Math.max(delayLeft, Math.max(0, delayTicks));
        if (elapsed == TOTAL_TICKS) {
            elapsed = 0;
        }
        if (elapsed >= RAMP_UP_TICKS) {
            elapsed = RAMP_UP_TICKS;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        if (delayLeft > 0) {
            delayLeft--;
        } else if (elapsed < TOTAL_TICKS) {
            elapsed++;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float intensity = intensity();
        if (intensity <= 0.0F || !shakeScreen) {
            return;
        }
        float strength = shakeScreenValue * intensity;
        event.setYaw(event.getYaw() + (RNG.nextFloat() - 0.5F) * 2.0F * strength);
        event.setPitch(event.getPitch() + (RNG.nextFloat() - 0.5F) * 2.0F * strength);
        event.setRoll(event.getRoll() + (RNG.nextFloat() - 0.5F) * 2.0F * (strength * 0.7F));
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!darkScreen) {
            return;
        }
        float intensity = intensity();
        if (intensity <= 0.0F) {
            return;
        }
        int alpha = Mth.clamp((int) (0.6F * intensity * 255.0F), 0, 255);
        event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(),
                event.getGuiGraphics().guiHeight(), alpha << 24);
    }

    private static float intensity() {
        durationShake--;
        if (delayLeft > 0 || elapsed <= 0 || elapsed > TOTAL_TICKS) {
            return 0.0F;
        }
        if (elapsed <= RAMP_UP_TICKS) {
            return elapsed / (float) RAMP_UP_TICKS;
        }
        int afterRamp = elapsed - RAMP_UP_TICKS;
        if (afterRamp <= HOLD_TICKS) {
            return 1.0F;
        }
        if (durationShake > 0) {
            elapsed = RAMP_UP_TICKS + HOLD_TICKS + FADE_TICKS / 2;
            return 1.0F;
        }
        int afterHold = afterRamp - HOLD_TICKS;
        return Math.max(0.0F, 1.0F - afterHold / (float) FADE_TICKS);
    }
}
