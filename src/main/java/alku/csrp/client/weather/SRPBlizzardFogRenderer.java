package alku.csrp.client.weather;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 1.10.9 {@code SRPBlizzardFogRenderer}: eight nested, slowly deforming spheres that tint the
 * camera surroundings during a cold-star blizzard.
 *
 * <p>1.12.2 built them with {@code Tessellator}/{@code BufferBuilder} in immediate mode inside the
 * {@code EntityRenderer#renderRainSnow} mixin. 26.3 removed that path, so the spheres are emitted
 * through {@link SubmitCustomGeometryEvent} - the same channel the project's
 * {@code AuroraSkyRenderer} already uses for its sky dome. {@code RenderTypes.debugQuads()} is the
 * exact 26.3 counterpart of the original state block: position+colour vertex format, translucent
 * blending, no culling, no texture.</p>
 *
 * <p>One 1.12.2 gate could not be carried over verbatim: the shells only appeared when an OptiFine
 * shader pack was active, probed reflectively through
 * {@code net.optifine.shaders.Shaders#currentShaderName}. 26.3 has no OptiFine, so the probe now
 * tries OptiFine first and falls back to Iris; when neither is installed the shells stay hidden,
 * which is the original behaviour without a shader pack.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class SRPBlizzardFogRenderer {
    private static final int SHELL_COUNT = 8;
    private static final int LONGITUDE_SEGMENTS = 32;
    private static final int LATITUDE_SEGMENTS = 14;
    private static final int VERTICES_PER_SHELL = LATITUDE_SEGMENTS * LONGITUDE_SEGMENTS * 4;
    private static final float MAX_SHELL_ALPHA = 0.22F;

    private static final float[] UNIT_X = new float[SHELL_COUNT * VERTICES_PER_SHELL];
    private static final float[] UNIT_Y = new float[SHELL_COUNT * VERTICES_PER_SHELL];
    private static final float[] UNIT_Z = new float[SHELL_COUNT * VERTICES_PER_SHELL];
    private static final float[] PHASE_A = new float[SHELL_COUNT * VERTICES_PER_SHELL];
    private static final float[] PHASE_B = new float[SHELL_COUNT * VERTICES_PER_SHELL];

    private static boolean shaderLookupAttempted;
    private static java.lang.reflect.Field optifineShaderNameField;
    private static java.lang.reflect.Method irisPackInUseMethod;

    static {
        buildShells();
    }

    private SRPBlizzardFogRenderer() {
    }

    @SubscribeEvent
    public static void render(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.getCameraEntity() == null) {
            return;
        }
        if (!isShaderPackActive()) {
            return;
        }
        float partialTicks = event.getLevelRenderState().worldPartialTicks;
        float intensity = Mth.clamp(SRPBlizzardClient.getIntensity(partialTicks), 0.0F, 1.0F);
        if (intensity <= 0.001F) {
            return;
        }

        float strength = smoothStep(intensity);
        double nearRadius = lerp(14.0, 5.5, strength);
        double farRadius = lerp(48.0, 20.0, strength);
        float baseAlpha = 0.02F + strength * 0.14F;
        float blackBlend = Mth.clamp(SRPBlizzardDirectionClient.getBlackBlend(partialTicks), 0.0F, 1.0F);
        float red = lerp(0.78F, 0.055F, blackBlend);
        float green = lerp(0.82F, 0.06F, blackBlend);
        float blue = lerp(0.86F, 0.07F, blackBlend);
        float lightMultiplier = 0.78F + SRPBlizzardClient.daylight(partialTicks) * 0.22F;
        red *= lightMultiplier;
        green *= lightMultiplier;
        blue *= lightMultiplier;
        double time = level.getGameTime() + partialTicks;

        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        RenderType renderType = RenderTypes.debugQuads();
        for (int shell = SHELL_COUNT - 1; shell >= 0; shell--) {
            float shellProgress = shell / (float) (SHELL_COUNT - 1);
            double radius = lerp(nearRadius, farRadius, shellProgress);
            float shellAlpha =
                    Mth.clamp(baseAlpha * (0.55F + shellProgress * 0.45F), 0.0F, MAX_SHELL_ALPHA);
            int argb = packColor(red, green, blue, shellAlpha);
            int shellIndex = shell;
            double shellRadius = radius;
            collector.submitCustomGeometry(event.getPoseStack(), renderType, (pose, buffer) -> {
                int base = shellIndex * VERTICES_PER_SHELL;
                for (int vertex = 0; vertex < VERTICES_PER_SHELL; vertex++) {
                    int index = base + vertex;
                    double waveA = Math.sin(PHASE_A[index] + time * 0.01);
                    double waveB = Math.cos(PHASE_B[index] - time * 0.006);
                    double actualRadius = shellRadius * (1.0 + waveA * waveB * 0.018);
                    buffer.addVertex(pose, (float) (UNIT_X[index] * actualRadius),
                                    (float) (UNIT_Y[index] * actualRadius),
                                    (float) (UNIT_Z[index] * actualRadius))
                            .setColor(argb);
                }
            });
        }
    }

    private static void buildShells() {
        for (int shell = 0; shell < SHELL_COUNT; shell++) {
            int vertex = shell * VERTICES_PER_SHELL;
            for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
                double latitude0 = (-Math.PI / 2) + Math.PI * latitude / (double) LATITUDE_SEGMENTS;
                double latitude1 = (-Math.PI / 2) + Math.PI * (latitude + 1) / (double) LATITUDE_SEGMENTS;
                for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                    double longitude0 = (Math.PI * 2) * longitude / (double) LONGITUDE_SEGMENTS;
                    double longitude1 = (Math.PI * 2) * (longitude + 1) / (double) LONGITUDE_SEGMENTS;
                    vertex = writeVertex(vertex, longitude0, latitude0, shell);
                    vertex = writeVertex(vertex, longitude1, latitude0, shell);
                    vertex = writeVertex(vertex, longitude1, latitude1, shell);
                    vertex = writeVertex(vertex, longitude0, latitude1, shell);
                }
            }
        }
    }

    private static int writeVertex(int index, double longitude, double latitude, int shell) {
        double cosLatitude = Math.cos(latitude);
        UNIT_X[index] = (float) (Math.cos(longitude) * cosLatitude);
        UNIT_Y[index] = (float) Math.sin(latitude);
        UNIT_Z[index] = (float) (Math.sin(longitude) * cosLatitude);
        PHASE_A[index] = (float) (longitude * 3.0 + shell * 1.73);
        PHASE_B[index] = (float) (latitude * 4.0 + shell * 0.91);
        return index + 1;
    }

    private static int packColor(float red, float green, float blue, float alpha) {
        int r = Mth.clamp((int) (red * 255.0F), 0, 255);
        int g = Mth.clamp((int) (green * 255.0F), 0, 255);
        int b = Mth.clamp((int) (blue * 255.0F), 0, 255);
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        return a << 24 | r << 16 | g << 8 | b;
    }

    /**
     * 1.12.2 checked {@code net.optifine.shaders.Shaders#currentShaderName} reflectively. The same
     * probe is kept for OptiFine and extended with Iris' {@code Iris#isPackInUseQuick()}, so the
     * shells still only show up under a third-party shader pack.
     */
    public static boolean isShaderPackActive() {
        if (!shaderLookupAttempted) {
            shaderLookupAttempted = true;
            try {
                Class<?> shaders = Class.forName("net.optifine.shaders.Shaders", false,
                        SRPBlizzardFogRenderer.class.getClassLoader());
                optifineShaderNameField = shaders.getDeclaredField("currentShaderName");
                optifineShaderNameField.setAccessible(true);
            } catch (Throwable ignored) {
                optifineShaderNameField = null;
            }
            try {
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris", false,
                        SRPBlizzardFogRenderer.class.getClassLoader());
                irisPackInUseMethod = iris.getMethod("isPackInUseQuick");
            } catch (Throwable ignored) {
                irisPackInUseMethod = null;
            }
        }

        if (optifineShaderNameField != null) {
            try {
                Object value = optifineShaderNameField.get(null);
                if (value instanceof String shaderName) {
                    String trimmed = shaderName.trim();
                    if (!trimmed.isEmpty() && !"OFF".equalsIgnoreCase(trimmed)) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
                // fall through to the Iris probe
            }
        }

        if (irisPackInUseMethod != null) {
            try {
                Object value = irisPackInUseMethod.invoke(null);
                if (value instanceof Boolean inUse && inUse) {
                    return true;
                }
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    private static float smoothStep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    /**
     * Replacement for the linear-fog block of the 1.12.2 {@code MixinEntityRendererBlizzard}
     * ({@code EntityRenderer#setupFog} TAIL: {@code fogEnd = 72 - intensity * 56},
     * {@code fogStart = fogEnd * 0.08}, skipped while the camera is in water or lava).
     * 26.3 exposes the same numbers through {@link ViewportEvent.RenderFog}; the water/lava guard
     * becomes a {@link FogType} check.
     */
    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.ATMOSPHERIC || !SRPBlizzardClient.isColdWorld()) {
            return;
        }
        float intensity = Mth.clamp(SRPBlizzardClient.getIntensity((float) event.getPartialTick()),
                0.0F, 1.0F);
        if (intensity <= 0.001F) {
            return;
        }
        float fogEnd = 72.0F - intensity * 56.0F;
        event.setFarPlaneDistance(fogEnd);
        event.setNearPlaneDistance(fogEnd * 0.08F);
    }

    /**
     * 26.3 substitute for the 1.12.2 {@code MixinRenderGlobalBlizzardSky}, which suppressed
     * {@code WorldProvider#calcSunriseSunsetColors} so no warm sunrise/-set glow showed through the
     * blizzard. 26.3 bakes that colour into {@code SkyRenderState} before any mod render event and
     * only exposes a dimension-type {@code CustomSkyboxRenderer} to replace it, so the horizon glow
     * is instead washed out with the blizzard's own fog colour.
     */
    @SubscribeEvent
    public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!SRPBlizzardClient.isColdWorld()) {
            return;
        }
        float partialTicks = (float) event.getPartialTick();
        float intensity = Mth.clamp(SRPBlizzardClient.getIntensity(partialTicks), 0.0F, 1.0F);
        if (intensity <= 0.001F) {
            return;
        }
        float blackBlend = Mth.clamp(SRPBlizzardDirectionClient.getBlackBlend(partialTicks), 0.0F, 1.0F);
        float lightMultiplier = 0.78F + SRPBlizzardClient.daylight(partialTicks) * 0.22F;
        float blend = smoothStep(intensity);
        event.setRed(Mth.lerp(blend, event.getRed(), lerp(0.78F, 0.055F, blackBlend) * lightMultiplier));
        event.setGreen(Mth.lerp(blend, event.getGreen(), lerp(0.82F, 0.06F, blackBlend) * lightMultiplier));
        event.setBlue(Mth.lerp(blend, event.getBlue(), lerp(0.86F, 0.07F, blackBlend) * lightMultiplier));
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }
}
