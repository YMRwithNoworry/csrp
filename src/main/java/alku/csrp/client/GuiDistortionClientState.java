package alku.csrp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Port of the 1.10.9 client half of {@code SRPPacketGuiDistortionState}: the per-player GUI distortion
 * switch set by {@code /srpguidistortion}.
 *
 * <p>The original stored {@code SRPGuiDistortionDisabled} and {@code SRPGuiDistortionCreativeOverride}
 * in the player's persisted data and sent both to that player's client, which then applied the
 * creative/spectator rule locally — so the rule lives here too.</p>
 */
public final class GuiDistortionClientState {
    private static boolean disabled;
    private static boolean creativeOverride;

    private GuiDistortionClientState() {
    }

    public static void set(boolean newDisabled, boolean newCreativeOverride) {
        disabled = newDisabled;
        creativeOverride = newCreativeOverride;
    }

    /** {@code SRPCommandGuiDistortion.isCreativeOrSpectator}. */
    public static boolean isCreativeOrSpectator() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return player.isSpectator() || player.isCreative();
    }

    /** {@code effectivelyDisabled = disabled || (creativeOrSpectator && !creativeOverride)}. */
    public static boolean effectivelyDisabled() {
        return disabled || (isCreativeOrSpectator() && !creativeOverride);
    }
}
