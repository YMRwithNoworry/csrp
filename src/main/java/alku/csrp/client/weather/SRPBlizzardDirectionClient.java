package alku.csrp.client.weather;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 1.10.9 {@code SRPBlizzardDirectionClient}: the momentum / braking state machine that flips the
 * blizzard wind direction, plus the black-blend value used to tint the fog and the snow streaks.
 *
 * <p>The whole state machine is pure arithmetic and was ported verbatim (same gains, same hold
 * time, same "fade back to white" branch). Only two pieces needed a 26.3 rewrite:</p>
 * <ul>
 *   <li>the tick hook is {@link ClientTickEvent.Post} instead of {@code ClientTickEvent(Phase.END)};</li>
 *   <li>the switch sound is a {@link SoundBlizzardReverse} instance pushed straight into the
 *       {@code SoundManager}, instead of the 1.12.2 {@code MovingSound} constructor.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class SRPBlizzardDirectionClient {
    private static boolean reverseRequested = false;
    private static int committedDirection = 1;
    private static float motion = 1.0F;
    private static float previousMotion = 1.0F;
    private static boolean braking = false;
    private static int holdTicks = 0;
    private static double motionPhase = 0.0;
    private static double previousMotionPhase = 0.0;
    private static float blackBlend = 0.0F;
    private static float previousBlackBlend = 0.0F;
    private static boolean fadingBackToWhite = false;

    private SRPBlizzardDirectionClient() {
    }

    /** Called from the {@code MsgSyncBlizzardReverse} handler. */
    public static void setReverseRequested(boolean reverse) {
        if (reverseRequested != reverse) {
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
    }

    public static float getMotion(float partialTicks) {
        return previousMotion + (motion - previousMotion) * partialTicks;
    }

    public static double getMotionPhase(float partialTicks) {
        return previousMotionPhase + (motionPhase - previousMotionPhase) * partialTicks;
    }

    public static float getBlackBlend(float partialTicks) {
        return previousBlackBlend + (blackBlend - previousBlackBlend) * partialTicks;
    }

    public static boolean isReversed() {
        return committedDirection < 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
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
            magnitude = Math.max(0.0F, magnitude - 0.065F);
            motion = magnitude * committedDirection;
            if (reverseRequested && committedDirection > 0) {
                blackBlend = Math.min(1.0F, blackBlend + 0.075F);
            }

            if (magnitude <= 0.001F) {
                motion = 0.0F;
                if (!reverseRequested && committedDirection < 0) {
                    fadingBackToWhite = true;
                }

                if (fadingBackToWhite) {
                    blackBlend = Math.max(0.0F, blackBlend - 0.125F);
                }

                boolean colorReady = reverseRequested ? blackBlend >= 0.999F : blackBlend <= 0.001F;
                if (desiredDirection == committedDirection) {
                    braking = false;
                    holdTicks = 0;
                } else if (!colorReady) {
                    holdTicks = 0;
                } else if (holdTicks < 8) {
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
                motion = Math.min(target, motion + 0.045F);
            } else if (motion > target) {
                motion = Math.max(target, motion - 0.045F);
            }

            if (committedDirection < 0) {
                blackBlend = Math.min(1.0F, blackBlend + 0.08F);
            } else {
                blackBlend = Math.max(0.0F, blackBlend - 0.08F);
            }
        }

        motionPhase += motion;
    }

    private static void playSwitchSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.getSoundManager().play(new SoundBlizzardReverse());
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
