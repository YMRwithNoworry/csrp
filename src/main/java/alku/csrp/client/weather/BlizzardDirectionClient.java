package alku.csrp.client.weather;

import alku.csrp.Csrp;
import alku.csrp.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Wind direction state machine for the cold star blizzard. Line-by-line port of 1.10.9's
 * {@code SRPBlizzardDirectionClient}: a direction change request brakes the wind to zero, blends the
 * storm to black, holds for eight ticks, flips the committed direction and fades back to white. Every
 * easing constant is preserved from the original ({@code 0.065 / 0.075 / 0.125 / 0.08 / 0.045 / 8}).
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class BlizzardDirectionClient {
    private static final float BRAKE_STEP = 0.065F;
    private static final float BLACKEN_STEP = 0.075F;
    private static final float WHITEN_STEP = 0.125F;
    private static final float APPROACH_STEP = 0.045F;
    private static final float BLEND_APPROACH_STEP = 0.08F;
    private static final int HOLD_TICKS = 8;

    private static boolean reverseRequested;
    private static int committedDirection = 1;
    private static float motion = 1.0F;
    private static float previousMotion = 1.0F;
    private static boolean braking;
    private static int holdTicks;
    private static double motionPhase;
    private static double previousMotionPhase;
    private static float blackBlend;
    private static float previousBlackBlend;
    private static boolean fadingBackToWhite;

    private BlizzardDirectionClient() {
    }

    /** Called by the network layer whenever the server reports a storm direction change. */
    public static void setReverseRequested(boolean reverse) {
        if (reverseRequested == reverse) {
            return;
        }
        reverseRequested = reverse;
        playSwitchSound();
        int desiredDirection = reverse ? -1 : 1;
        if (desiredDirection != committedDirection) {
            braking = true;
            holdTicks = 0;
            if (reverse) {
                fadingBackToWhite = false;
            }
        } else if (braking) {
            braking = false;
            holdTicks = 0;
        }
    }

    public static float getMotion(float partialTick) {
        return previousMotion + (motion - previousMotion) * partialTick;
    }

    public static double getMotionPhase(float partialTick) {
        return previousMotionPhase + (motionPhase - previousMotionPhase) * partialTick;
    }

    public static float getBlackBlend(float partialTick) {
        return previousBlackBlend + (blackBlend - previousBlackBlend) * partialTick;
    }

    public static boolean isReversed() {
        return committedDirection < 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            reset();
            return;
        }
        previousMotion = motion;
        previousMotionPhase = motionPhase;
        previousBlackBlend = blackBlend;

        int desiredDirection = reverseRequested ? -1 : 1;
        if (desiredDirection != committedDirection) {
            braking = true;
        }

        if (braking) {
            float magnitude = Math.abs(motion);
            magnitude = Math.max(0.0F, magnitude - BRAKE_STEP);
            motion = magnitude * committedDirection;
            if (reverseRequested && committedDirection > 0) {
                blackBlend = Math.min(1.0F, blackBlend + BLACKEN_STEP);
            }

            if (magnitude <= 0.001F) {
                motion = 0.0F;
                if (!reverseRequested && committedDirection < 0) {
                    fadingBackToWhite = true;
                }
                if (fadingBackToWhite) {
                    blackBlend = Math.max(0.0F, blackBlend - WHITEN_STEP);
                }

                boolean colorReady = reverseRequested ? blackBlend >= 0.999F : blackBlend <= 0.001F;
                if (desiredDirection == committedDirection) {
                    braking = false;
                    holdTicks = 0;
                } else if (!colorReady) {
                    holdTicks = 0;
                } else if (holdTicks < HOLD_TICKS) {
                    holdTicks++;
                } else {
                    committedDirection = desiredDirection;
                    braking = false;
                    holdTicks = 0;
                    fadingBackToWhite = false;
                }
            }
        } else {
            float target = committedDirection;
            if (motion < target) {
                motion = Math.min(target, motion + APPROACH_STEP);
            } else if (motion > target) {
                motion = Math.max(target, motion - APPROACH_STEP);
            }

            if (committedDirection < 0) {
                blackBlend = Math.min(1.0F, blackBlend + BLEND_APPROACH_STEP);
            } else {
                blackBlend = Math.max(0.0F, blackBlend - BLEND_APPROACH_STEP);
            }
        }

        motionPhase += motion;
    }

    private static void playSwitchSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.getSoundManager().play(new BlizzardReverseSound(ModSounds.BLIZZARD_REVERSE.get(), minecraft.player));
    }

    public static void reset() {
        reverseRequested = false;
        committedDirection = 1;
        motion = 1.0F;
        previousMotion = 1.0F;
        braking = false;
        holdTicks = 0;
        motionPhase = 0.0;
        previousMotionPhase = 0.0;
        blackBlend = 0.0F;
        previousBlackBlend = 0.0F;
        fadingBackToWhite = false;
    }
}
