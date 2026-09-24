package alku.csrp.client.weather;

import alku.csrp.Csrp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * 1.10.9 {@code SRPBlizzardRenderer}: the wind-driven snow streaks of the cold-star blizzard.
 *
 * <p>The original mixed a hand-written column scan ({@code World#getPrecipitationHeight} per block
 * column) with an immediate-mode {@code Tessellator} draw inside the
 * {@code EntityRenderer#renderRainSnow} mixin. 26.3 has neither the mixin nor immediate mode, so:</p>
 * <ul>
 *   <li>the column scan is kept but now uses
 *       {@code Level#getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)} - exactly the heightmap
 *       {@code WeatherEffectRenderer#extractRenderState} uses for the vanilla weather columns;</li>
 *   <li>the quads are submitted through {@link SubmitCustomGeometryEvent} with
 *       {@code RenderTypes.entityTranslucent} over the vanilla
 *       {@code textures/environment/snow.png}, the same texture the original bound.</li>
 * </ul>
 *
 * <p>Two deliberate 26.3 adaptations, both documented in
 * {@code docs/gap/R2_BLIZZARD_REPORT.md}: streaks are emitted double-sided (the 1.12.2 renderer
 * ran with culling disabled via {@code GlStateManager.disableCull}, and the modern render type used
 * here keeps culling on), and the per-frame streak budget is capped so a radius-14 blizzard cannot
 * submit tens of thousands of quads.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class SRPBlizzardRenderer {
    private static final Identifier SNOW_TEXTURE =
            Identifier.withDefaultNamespace("textures/environment/snow.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int MAX_RADIUS = 12;
    private static final int MAX_STREAKS = 1500;
    private static final int STRIDE = 10;

    private static final double[] STREAKS = new double[MAX_STREAKS * STRIDE];

    private SRPBlizzardRenderer() {
    }

    @SubscribeEvent
    public static void render(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.getCameraEntity() == null) {
            return;
        }
        float partialTicks = event.getLevelRenderState().worldPartialTicks;
        float intensity = Mth.clamp(SRPBlizzardClient.getIntensity(partialTicks), 0.0F, 1.0F);
        if (intensity <= 0.001F) {
            return;
        }

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float cameraYaw = event.getLevelRenderState().cameraRenderState.yRot;
        double cameraRightX = Math.cos(Math.toRadians(cameraYaw));
        double cameraRightZ = Math.sin(Math.toRadians(cameraYaw));
        float blackBlend = Mth.clamp(SRPBlizzardDirectionClient.getBlackBlend(partialTicks), 0.0F, 1.0F);
        float daylight = SRPBlizzardClient.daylight(partialTicks);
        float brightness = 0.86F + daylight * 0.14F;
        double time = level.getGameTime() + partialTicks;
        double snowTime = SRPBlizzardDirectionClient.getMotionPhase(partialTicks);

        double windAngle = time * 0.0012 + Math.sin(time * 3.7E-4) * 0.45;
        double windStrength = (0.8 + intensity * 2.2)
                * (0.72 + Math.sin(time * 0.065) * 0.18 + Math.sin(time * 0.017) * 0.1);
        double windX = Math.cos(windAngle) * windStrength;
        double windZ = Math.sin(windAngle) * windStrength;

        int streakCount = collectStreaks(level, camera, intensity, time, snowTime, windX, windZ,
                cameraRightX, cameraRightZ);
        if (streakCount == 0) {
            return;
        }

        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        RenderType renderType = RenderTypes.entityTranslucent(SNOW_TEXTURE);
        PoseStack poseStack = event.getPoseStack();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            float color = brightness * (1.0F - blackBlend);
            for (int streak = 0; streak < streakCount; streak++) {
                int base = streak * STRIDE;
                addStreak(buffer, pose, STREAKS[base], STREAKS[base + 1], STREAKS[base + 2],
                        STREAKS[base + 3], STREAKS[base + 4], STREAKS[base + 5], STREAKS[base + 6],
                        STREAKS[base + 7], STREAKS[base + 8], (float) STREAKS[base + 9], color);
            }
        });
    }

    private static int collectStreaks(ClientLevel level, Vec3 camera, float intensity, double time,
            double snowTime, double windX, double windZ, double cameraRightX, double cameraRightZ) {
        int radius = Math.min(8 + Mth.floor(intensity * 6.0F), MAX_RADIUS);
        int cameraBlockX = Mth.floor(camera.x);
        int cameraBlockY = Mth.floor(camera.y);
        int cameraBlockZ = Mth.floor(camera.z);
        int minRenderY = cameraBlockY - 10;
        int maxRenderY = cameraBlockY + 14;
        int laneCount = intensity > 0.7F ? 3 : 2;
        int baseSpacing = intensity > 0.55F ? 1 : 2;
        int count = 0;

        for (int x = cameraBlockX - radius; x <= cameraBlockX + radius && count < MAX_STREAKS; x++) {
            for (int z = cameraBlockZ - radius; z <= cameraBlockZ + radius && count < MAX_STREAKS; z++) {
                double dx = x + 0.5 - camera.x;
                double dz = z + 0.5 - camera.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) {
                    continue;
                }
                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int startY = Math.max(groundY, minRenderY);
                if (startY >= maxRenderY) {
                    continue;
                }
                float distanceFade = 1.0F - Mth.clamp((float) (distance / radius), 0.0F, 1.0F);

                for (int lane = 0; lane < laneCount && count < MAX_STREAKS; lane++) {
                    float randomA = hash01(x, z, lane * 11);
                    float randomB = hash01(x, z, lane * 11 + 1);
                    float randomC = hash01(x, z, lane * 11 + 2);
                    float randomD = hash01(x, z, lane * 11 + 3);
                    float randomE = hash01(x, z, lane * 11 + 4);
                    float randomF = hash01(x, z, lane * 11 + 5);
                    int laneSpacing = baseSpacing + (randomD > 0.82F ? 1 : 0);
                    int phase = Mth.floor(randomC * (laneSpacing + 1));

                    for (int y = startY + phase; y < maxRenderY && count < MAX_STREAKS; y += laneSpacing) {
                        if (randomF > 0.84F) {
                            continue;
                        }
                        int base = count * STRIDE;
                        if (!buildStreak(base, x, z, y, camera, intensity, time, snowTime, windX,
                                windZ, cameraRightX, cameraRightZ, distance, distanceFade, randomA,
                                randomB, randomC, randomD, randomE, lane, laneSpacing)) {
                            continue;
                        }
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean buildStreak(int base, int x, int z, int y, Vec3 camera, float intensity,
            double time, double snowTime, double windX, double windZ, double cameraRightX,
            double cameraRightZ, double distance, float distanceFade, float randomA, float randomB,
            float randomC, float randomD, float randomE, int lane, int laneSpacing) {
        double segmentHeight = 1.8 + randomD * 2.1;
        double fallValue = snowTime * (0.16 + intensity * 0.18) + randomC * 37.0 + lane * 1.73;
        double fall = positiveModulo(fallValue, laneSpacing + 0.35);
        double yJitter = (randomE - 0.5) * 0.9;
        double bottomWorldY = y + yJitter - fall;
        double topWorldY = bottomWorldY + segmentHeight;

        double windPhase = time * 0.045 + y * 0.13 + randomA * 8.0 + lane * 0.91;
        double sideways = Math.sin(windPhase) * 0.14 * intensity;
        double perpendicularWindX = -windZ;
        double perpendicularWindZ = windX;
        double perpendicularLength =
                Math.sqrt(perpendicularWindX * perpendicularWindX + perpendicularWindZ * perpendicularWindZ);
        if (perpendicularLength > 0.001) {
            perpendicularWindX /= perpendicularLength;
            perpendicularWindZ /= perpendicularLength;
        }

        double jitterX = (randomA - 0.5) * 0.92 + perpendicularWindX * sideways
                + lane * perpendicularWindX * 0.22;
        double jitterZ = (randomB - 0.5) * 0.92 + perpendicularWindZ * sideways
                + lane * perpendicularWindZ * 0.22;
        double baseX = x + 0.5 + jitterX;
        double baseZ = z + 0.5 + jitterZ;
        double streakX = -windX * (0.16 + intensity * 0.24);
        double streakZ = -windZ * (0.16 + intensity * 0.24);
        double endWorldX = baseX + streakX;
        double endWorldZ = baseZ + streakZ;

        double surfaceY = Math.max(surfaceY(camera, baseX, baseZ), surfaceY(camera, endWorldX, endWorldZ));
        if (topWorldY <= surfaceY + 0.02) {
            return false;
        }
        if (bottomWorldY < surfaceY + 0.02) {
            bottomWorldY = surfaceY + 0.02;
        }

        double halfWidth = 0.11 + randomB * 0.12 + intensity * 0.03;
        double widthX = cameraRightX * halfWidth;
        double widthZ = cameraRightZ * halfWidth;
        float alpha = intensity * (0.34F + distanceFade * 0.58F) * (0.74F + randomA * 0.26F);
        alpha = Math.min(alpha, 0.96F);
        if (distance < 2.0) {
            alpha *= 0.92F;
        }

        STREAKS[base] = baseX - camera.x;
        STREAKS[base + 1] = bottomWorldY - camera.y;
        STREAKS[base + 2] = baseZ - camera.z;
        STREAKS[base + 3] = endWorldX - camera.x;
        STREAKS[base + 4] = topWorldY - camera.y;
        STREAKS[base + 5] = endWorldZ - camera.z;
        STREAKS[base + 6] = widthX;
        STREAKS[base + 7] = widthZ;
        STREAKS[base + 8] = time * 0.09 + randomC * 8.0 + lane * 0.37;
        STREAKS[base + 9] = alpha;
        return true;
    }

    /**
     * 1.12.2 resolved the precipitation surface with
     * {@code World#getPrecipitationHeight(BlockPos)}. The 26.3 client only exposes heightmaps, so
     * the streak is clipped against {@code MOTION_BLOCKING} - the heightmap the vanilla weather
     * renderer itself uses for its rain/snow columns.
     */
    private static double surfaceY(Vec3 camera, double worldX, double worldZ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return camera.y;
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(worldX), Mth.floor(worldZ));
    }

    private static void addStreak(VertexConsumer buffer, PoseStack.Pose pose, double x1, double y1,
            double z1, double x2, double y2, double z2, double widthX, double widthZ,
            double textureOffset, float alpha, float color) {
        // Front winding: bottom-left, bottom-right, top-right, top-left.
        vertex(buffer, pose, x1 - widthX, y1, z1 - widthZ, 0.0D, textureOffset, color, alpha);
        vertex(buffer, pose, x1 + widthX, y1, z1 + widthZ, 1.0D, textureOffset, color, alpha);
        vertex(buffer, pose, x2 + widthX, y2, z2 + widthZ, 1.0D, textureOffset + 1.0D, color, alpha);
        vertex(buffer, pose, x2 - widthX, y2, z2 - widthZ, 0.0D, textureOffset + 1.0D, color, alpha);
        // Back winding: the 1.12.2 renderer disabled culling, so both faces must be emitted.
        vertex(buffer, pose, x1 - widthX, y1, z1 - widthZ, 0.0D, textureOffset, color, alpha);
        vertex(buffer, pose, x2 - widthX, y2, z2 - widthZ, 0.0D, textureOffset + 1.0D, color, alpha);
        vertex(buffer, pose, x2 + widthX, y2, z2 + widthZ, 1.0D, textureOffset + 1.0D, color, alpha);
        vertex(buffer, pose, x1 + widthX, y1, z1 + widthZ, 1.0D, textureOffset, color, alpha);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, double x, double y,
            double z, double u, double v, float color, float alpha) {
        buffer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(color, color, color, alpha)
                .setUv((float) u, (float) v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT);
    }

    private static double positiveModulo(double value, double modulus) {
        return value - Math.floor(value / modulus) * modulus;
    }

    private static float hash01(int x, int z, int salt) {
        long value = x * 341873128712L + z * 132897987541L + salt * 42317861L;
        value ^= value >>> 13;
        value *= 1274126177L;
        value ^= value >>> 16;
        return (float) (value & 16777215L) / 1.6777215E7F;
    }
}
