package alku.csrp.client.weather;

import alku.csrp.celestial.client.StarWorldClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Client-side blizzard intensity. Port of SRParasites 1.10.9's {@code SRPBlizzardClient}: the storm is
 * only visible in the overworld of a cold star system and follows the vanilla rain level so
 * {@code /weather rain} drives it.
 */
public final class BlizzardClient {
    /** Below this the storm counts as absent; every blizzard hook gates on it. */
    public static final float MIN_INTENSITY = 0.001F;

    private BlizzardClient() {
    }

    public static boolean isColdWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level.dimension() != Level.OVERWORLD) {
            return false;
        }
        return StarWorldClientState.isCold();
    }

    public static float getIntensity(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isColdWorld() || minecraft.level == null) {
            return 0.0F;
        }
        return Mth.clamp(minecraft.level.getRainLevel(partialTick), 0.0F, 1.0F);
    }
}
