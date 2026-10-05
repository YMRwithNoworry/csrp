package alku.csrp.client.weather;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Port of SRP 1.10.9 {@code client/fx/ClientExtremeSnow}: the client half of the forced extreme snow
 * configured by {@code /srp_extremesnow}.
 *
 * <p>The original held the five synced values plus a wind vector derived from {@code windDeg} /
 * {@code windSpeed}, pushed an exponential-squared fog and a washed-out fog colour while the player was
 * outdoors, and spawned {@code ParticleBlizzard} snowflakes around the player.  Two 26.3 differences:</p>
 * <ul>
 *   <li>26.3's {@link ViewportEvent.RenderFog} only exposes linear near/far planes, so the original's
 *       {@code exp2DensityFor(10.0)} density (≈0.1978, i.e. about ten blocks of visibility) is applied
 *       as a ten block far plane with the same 8% near-plane ratio the project's cold-star fog uses.</li>
 *   <li>the falling snow is drawn by the project's existing blizzard renderer — a 26.3 custom-geometry
 *       rewrite of the original's {@code ParticleBlizzard} — which now also activates while this state is
 *       on (see {@link SRPBlizzardClient#isBlizzardActive()}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class ClientExtremeSnow {
    /** {@code ClientExtremeSnow.exp2DensityFor(10.0)}: the original aimed for ten blocks of visibility. */
    private static final double FOG_TARGET_BLOCKS = 10.0D;
    /** {@code ClientExtremeSnow.onFogColors}: push 75% of the way to white. */
    private static final float FOG_COLOR_PUSH = 0.75F;
    private static final double NEAR_PLANE_RATIO = 0.08D;

    private static boolean enabled;
    private static float intensity = 1.0F;
    private static boolean forceAnywhere = true;
    private static float windDeg = 30.0F;
    private static float windSpeed = 0.5F;
    private static double windX;
    private static double windZ;

    private ClientExtremeSnow() {
    }

    /** {@code ClientExtremeSnow.setState}. */
    public static void setState(boolean on, float newIntensity, boolean anywhere, float degrees,
            float speed) {
        enabled = on;
        intensity = Mth.clamp(newIntensity, 0.0F, 1.0F);
        forceAnywhere = anywhere;
        windDeg = degrees % 360.0F;
        windSpeed = Mth.clamp(speed, 0.0F, 1.0F);
        double radians = Math.toRadians(windDeg);
        double base = 0.15D + 0.55D * windSpeed;
        windX = Math.cos(radians) * base;
        windZ = Math.sin(radians) * base;
    }

    public static void reset() {
        setState(false, 1.0F, true, 30.0F, 0.5F);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static float getIntensity() {
        return intensity;
    }

    public static boolean isForceAnywhere() {
        return forceAnywhere;
    }

    public static double windX() {
        return windX;
    }

    public static double windZ() {
        return windZ;
    }

    public static float getWindDeg() {
        return windDeg;
    }

    public static float getWindSpeed() {
        return windSpeed;
    }

    /** {@code exp2DensityFor} / {@code densityForVisibility}: {@code sqrt(ln 50) / targetBlocks}. */
    public static float density() {
        return (float) Mth.clamp(Math.sqrt(Math.log(50.0D)) / FOG_TARGET_BLOCKS, 0.0D, 1.0D);
    }

    /** The forced snow's whiteout, applied instead of the cold-star fog while it is on. */
    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        if (!enabled || event.getType() != FogType.ATMOSPHERIC || !isOutdoors()) {
            return;
        }
        float far = (float) FOG_TARGET_BLOCKS;
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance((float) (far * NEAR_PLANE_RATIO));
    }

    /** {@code onFogColors}: {@code colour = colour * 0.25 + 0.75}. */
    @SubscribeEvent
    public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!enabled || !isOutdoors()) {
            return;
        }
        event.setRed(event.getRed() * (1.0F - FOG_COLOR_PUSH) + FOG_COLOR_PUSH);
        event.setGreen(event.getGreen() * (1.0F - FOG_COLOR_PUSH) + FOG_COLOR_PUSH);
        event.setBlue(event.getBlue() * (1.0F - FOG_COLOR_PUSH) + FOG_COLOR_PUSH);
    }

    /** {@code ClientExtremeSnow.isOutdoors}: any of five samples around the player sees open sky. */
    public static boolean isOutdoors() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return false;
        }
        int[][] offsets = {{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}};
        for (int[] offset : offsets) {
            BlockPos sample = BlockPos.containing(player.getX() + offset[0], player.getY(),
                    player.getZ() + offset[1]);
            BlockPos height = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, sample);
            if (level.isEmptyBlock(height)) {
                return true;
            }
        }
        return false;
    }

    /** Convenience for the renderer: the forced snow only shows in the overworld, like the original. */
    public static boolean isActiveIn(Level level) {
        return enabled && level != null && level.dimension() == Level.OVERWORLD;
    }
}
