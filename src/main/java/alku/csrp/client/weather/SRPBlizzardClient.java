package alku.csrp.client.weather;

import alku.csrp.celestial.client.StarWorldClientState;
import alku.csrp.world.SrpStarType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * 1.10.9 {@code SRPBlizzardClient}: decides whether the local client is in the cold-star
 * overworld and how strong the blizzard is.
 *
 * <p>1.12.2 read {@code SRPClientStarWorldState.isCold()} plus
 * {@code world.getRainStrength(partialTicks)}. 26.3 keeps the same two sources: the star type
 * still arrives through the celestial sync channel ({@link StarWorldClientState}) and the
 * vanilla rain strength is now {@link ClientLevel#getRainLevel(float)}.</p>
 */
public final class SRPBlizzardClient {
    private SRPBlizzardClient() {
    }

    public static boolean isColdWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return false;
        }
        return StarWorldClientState.starType() == SrpStarType.COLD;
    }

    /** 1.10.9 {@code getIntensity}: vanilla rain strength, clamped, cold star only. */
    public static float getIntensity(float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !isColdWorld()) {
            return 0.0F;
        }
        return Mth.clamp(level.getRainLevel(partialTicks), 0.0F, 1.0F);
    }

    /**
     * 1.10.9 read {@code World#getSunBrightness(partialTicks)} for the blizzard's daylight factor.
     * 26.3 dropped that accessor in favour of the {@code SUN_ANGLE} environment attribute, so the
     * same vanilla formula is rebuilt from {@code Level#getRainLevel} / {@code getThunderLevel} and
     * the overworld clock.
     */
    public static float daylight(float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return 1.0F;
        }
        double timeOfDay = Mth.frac((level.getOverworldClockTime() + partialTicks) / 24000.0 - 0.25);
        float darkness = 1.0F - (Mth.cos((float) timeOfDay * (float) Math.PI * 2.0F) * 2.0F + 0.5F);
        darkness = Mth.clamp(darkness, 0.0F, 1.0F);
        float brightness = 1.0F - darkness;
        brightness *= 1.0F - level.getRainLevel(partialTicks) * 5.0F / 16.0F;
        brightness *= 1.0F - level.getThunderLevel(partialTicks) * 5.0F / 16.0F;
        return brightness * 0.8F + 0.2F;
    }
}
