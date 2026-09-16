package alku.csrp.celestial.client;

import alku.csrp.Csrp;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;

/**
 * Overlays an aurora sky (port of the Godot "Aurora Sky Shader") while the
 * local player is inside a snow-related biome. The aurora is drawn as a large
 * dome over the player, blended with SRC_ALPHA so the base sky stays intact.
 *
 * <p>26.3 replaced the immediate-mode shader/vertex-buffer path with the
 * {@code SubmitNodeCollector} pipeline. The original custom core shader had
 * runtime uniforms that no longer exist, so this now bakes the colour gradient
 * into per-vertex colours and submits the dome once per frame.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class AuroraSkyRenderer {
    private static final float RADIUS = 100.0F;
    private static final float BRIGHTNESS = 1.7F;
    private static final int LATITUDE_SEGMENTS = 24;
    private static final int LONGITUDE_SEGMENTS = 48;

    private static float[] cachedPositions;
    private static int[] cachedColors;

    private AuroraSkyRenderer() {
    }

    @SubscribeEvent
    public static void render(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !isNight(minecraft) || !isSnowyBiome(minecraft)) {
            return;
        }
        renderAuroraSphere(event);
    }

    private static boolean isSnowyBiome(Minecraft minecraft) {
        return minecraft.level.getBiome(minecraft.player.blockPosition())
                .value().getPrecipitationAt(minecraft.player.blockPosition(), minecraft.level.getSeaLevel())
                == Biome.Precipitation.SNOW;
    }

    private static boolean isNight(Minecraft minecraft) {
        long timeOfDay = Math.floorMod(minecraft.level.getGameTime(), 24000L);
        return timeOfDay >= 13000L && timeOfDay <= 23000L;
    }

    private static void renderAuroraSphere(SubmitCustomGeometryEvent event) {
        if (cachedPositions == null) {
            buildAuroraMesh();
        }
        PoseStack poseStack = event.getPoseStack();
        RenderType renderType = RenderTypes.debugQuads();
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (int vertex = 0; vertex < cachedColors.length; vertex++) {
                buffer.addVertex(pose, cachedPositions[vertex * 3], cachedPositions[vertex * 3 + 1],
                                cachedPositions[vertex * 3 + 2])
                        .setColor(cachedColors[vertex]);
            }
        });
    }

    private static void buildAuroraMesh() {
        // Aurora is only visible above the horizon; avoid rasterizing the discarded lower half.
        int hemisphereSegments = LATITUDE_SEGMENTS / 2;
        int vertexCount = hemisphereSegments * LONGITUDE_SEGMENTS * 4;
        cachedPositions = new float[vertexCount * 3];
        cachedColors = new int[vertexCount];
        int vertex = 0;
        for (int lat = 0; lat < hemisphereSegments; lat++) {
            float theta0 = (float) lat / LATITUDE_SEGMENTS * (float) Math.PI;
            float theta1 = (float) (lat + 1) / LATITUDE_SEGMENTS * (float) Math.PI;
            for (int lon = 0; lon < LONGITUDE_SEGMENTS; lon++) {
                float phi0 = (float) lon / LONGITUDE_SEGMENTS * (float) (Math.PI * 2.0D);
                float phi1 = (float) (lon + 1) / LONGITUDE_SEGMENTS * (float) (Math.PI * 2.0D);
                vertex = writeVertex(vertex, direction(theta0, phi0));
                vertex = writeVertex(vertex, direction(theta0, phi1));
                vertex = writeVertex(vertex, direction(theta1, phi1));
                vertex = writeVertex(vertex, direction(theta1, phi0));
            }
        }
    }

    private static int writeVertex(int vertex, Vector3f position) {
        cachedPositions[vertex * 3] = position.x;
        cachedPositions[vertex * 3 + 1] = position.y;
        cachedPositions[vertex * 3 + 2] = position.z;
        cachedColors[vertex] = auroraColor(Mth.clamp(position.y / RADIUS, 0.0F, 1.0F));
        return vertex + 1;
    }

    private static int auroraColor(float height) {
        int rgb = gradientColor(Mth.clamp(height, 0.0F, 1.0F));
        float intensity = Mth.sin((float) Math.PI * Mth.clamp(height, 0.0F, 1.0F)) * BRIGHTNESS;
        int alpha = (int) (255.0F * Mth.clamp(intensity, 0.0F, 1.0F));
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private static int gradientColor(float t) {
        float[][] stops = {
                {0.00F, 0.05F, 0.30F, 0.25F},
                {0.25F, 0.00F, 0.75F, 0.45F},
                {0.50F, 0.20F, 0.95F, 0.50F},
                {0.75F, 0.30F, 0.70F, 1.00F},
                {1.00F, 0.55F, 0.30F, 1.00F}
        };
        int segment = Math.min(stops.length - 2, (int) (t * (stops.length - 1)));
        float local = t * (stops.length - 1) - segment;
        float[] a = stops[segment];
        float[] b = stops[segment + 1];
        int red = Math.round((a[1] + (b[1] - a[1]) * local) * 255.0F);
        int green = Math.round((a[2] + (b[2] - a[2]) * local) * 255.0F);
        int blue = Math.round((a[3] + (b[3] - a[3]) * local) * 255.0F);
        return (255 << 24) | (blue << 16) | (green << 8) | red;
    }

    private static Vector3f direction(float theta, float phi) {
        float sinTheta = (float) Math.sin(theta);
        return new Vector3f(sinTheta * (float) Math.cos(phi), (float) Math.cos(theta),
                sinTheta * (float) Math.sin(phi)).mul(RADIUS);
    }

    public static void dispose() {
        cachedPositions = null;
        cachedColors = null;
    }
}
