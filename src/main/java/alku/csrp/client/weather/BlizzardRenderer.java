package alku.csrp.client.weather;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Core blizzard renderer: slanted snow streaks around the camera. Port of 1.10.9's
 * {@code SRPBlizzardRenderer}. Layer count, spacing, wind phase, gusting, streak geometry, distance fade
 * and alpha limits are preserved verbatim; only the 1.20.1 API spellings changed.
 */
public final class BlizzardRenderer {
    private static final ResourceLocation SNOW_TEXTURE =
            new ResourceLocation(Csrp.MODID, "textures/environment/snow.png");
    private static final int BUFFER_BYTES = 2 * 1024 * 1024;

    private static Tesselator tesselator;

    private BlizzardRenderer() {
    }

    public static void render(Minecraft minecraft, Vec3 cameraPos, float cameraYaw, float partialTick,
            float intensity) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        intensity = Mth.clamp(intensity, 0.0F, 1.0F);
        if (intensity <= 0.001F) {
            return;
        }

        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;
        double cameraYawRadians = Math.toRadians(cameraYaw);
        double cameraRightX = Math.cos(cameraYawRadians);
        double cameraRightZ = Math.sin(cameraYawRadians);
        float blackBlend = BlizzardDirectionClient.getBlackBlend(partialTick);
        double time = minecraft.level.getGameTime() + partialTick;
        int radius = 8 + Mth.floor(intensity * 6.0F);
        int minX = Mth.floor(cameraX) - radius;
        int maxX = Mth.floor(cameraX) + radius;
        int minZ = Mth.floor(cameraZ) - radius;
        int maxZ = Mth.floor(cameraZ) + radius;
        double windAngle = time * 0.0012D + Math.sin(time * 3.7E-4D) * 0.45D;
        double gust = 0.72D + Math.sin(time * 0.065D) * 0.18D + Math.sin(time * 0.017D) * 0.1D;
        double windStrength = (0.8D + intensity * 2.2D) * gust;
        double windX = Math.cos(windAngle) * windStrength;
        double windZ = Math.sin(windAngle) * windStrength;
        double snowTime = BlizzardDirectionClient.getMotionPhase(partialTick);
        int minRenderY = Mth.floor(cameraY) - 10;
        int maxRenderY = Mth.floor(cameraY) + 14;
        float daylight = Mth.clamp((float) minecraft.level.getSkyDarken(), 0.0F, 1.0F);
        float brightness = 0.86F + daylight * 0.14F;

        // Vertices are emitted relative to the camera, matching the camera-relative pose stack the level
        // renderer already installed at this stage.
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.polygonOffset(516, 0.05F);
        RenderSystem.setShaderTexture(0, SNOW_TEXTURE);
        try {
            Tesselator tesselator = tesselator();
            BufferBuilder buffer = tesselator.getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double dx = x + 0.5D - cameraX;
                    double dz = z + 0.5D - cameraZ;
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > radius) {
                        continue;
                    }
                    int groundY = surfaceY(minecraft, x + 0.5D, z + 0.5D);
                    int startY = Math.max(groundY, minRenderY);
                    if (startY >= maxRenderY) {
                        continue;
                    }
                    float distanceFade = 1.0F - Mth.clamp((float) (distance / radius), 0.0F, 1.0F);
                    int laneCount = intensity > 0.7F ? 3 : 2;

                    for (int lane = 0; lane < laneCount; lane++) {
                        float randomA = hash01(x, z, lane * 11);
                        float randomB = hash01(x, z, lane * 11 + 1);
                        float randomC = hash01(x, z, lane * 11 + 2);
                        float randomD = hash01(x, z, lane * 11 + 3);
                        float randomE = hash01(x, z, lane * 11 + 4);
                        float randomF = hash01(x, z, lane * 11 + 5);
                        int baseSpacing = intensity > 0.55F ? 1 : 2;
                        int laneSpacing = baseSpacing + (randomD > 0.82F ? 1 : 0);
                        int phase = Mth.floor(randomC * (laneSpacing + 1));

                        for (int y = startY + phase; y < maxRenderY; y += laneSpacing) {
                            if (randomF > 0.84F) {
                                continue;
                            }
                            double segmentHeight = 1.8D + randomD * 2.1D;
                            double fallValue = snowTime * (0.16D + intensity * 0.18D) + randomC * 37.0D + lane * 1.73D;
                            double fall = positiveModulo(fallValue, laneSpacing + 0.35D);
                            double yJitter = (randomE - 0.5D) * 0.9D;
                            double bottomWorldY = y + yJitter - fall;
                            double topWorldY = bottomWorldY + segmentHeight;
                            double windPhase = time * 0.045D + y * 0.13D + randomA * 8.0D + lane * 0.91D;
                            double sideways = Math.sin(windPhase) * 0.14D * intensity;
                            double perpendicularWindX = -windZ;
                            double perpendicularWindZ = windX;
                            double perpendicularLength = Math.sqrt(
                                    perpendicularWindX * perpendicularWindX + perpendicularWindZ * perpendicularWindZ);
                            if (perpendicularLength > 0.001D) {
                                perpendicularWindX /= perpendicularLength;
                                perpendicularWindZ /= perpendicularLength;
                            }

                            double jitterX = (randomA - 0.5D) * 0.92D + perpendicularWindX * sideways
                                    + lane * perpendicularWindX * 0.22D;
                            double jitterZ = (randomB - 0.5D) * 0.92D + perpendicularWindZ * sideways
                                    + lane * perpendicularWindZ * 0.22D;
                            double baseX = x + 0.5D + jitterX;
                            double baseZ = z + 0.5D + jitterZ;
                            double streakX = -windX * (0.16D + intensity * 0.24D);
                            double streakZ = -windZ * (0.16D + intensity * 0.24D);
                            double endWorldX = baseX + streakX;
                            double endWorldZ = baseZ + streakZ;
                            double surfaceY = Math.max(surfaceY(minecraft, baseX, baseZ),
                                    surfaceY(minecraft, endWorldX, endWorldZ));
                            if (topWorldY <= surfaceY + 0.02D) {
                                continue;
                            }
                            if (bottomWorldY < surfaceY + 0.02D) {
                                bottomWorldY = surfaceY + 0.02D;
                            }

                            double halfWidth = 0.11D + randomB * 0.12D + intensity * 0.03D;
                            double widthX = cameraRightX * halfWidth;
                            double widthZ = cameraRightZ * halfWidth;
                            float alpha = intensity * (0.34F + distanceFade * 0.58F) * (0.74F + randomA * 0.26F);
                            alpha = Math.min(alpha, 0.96F);
                            if (distance < 2.0D) {
                                alpha *= 0.92F;
                            }

                            double textureOffset = time * 0.09D + randomC * 8.0D + lane * 0.37D;
                            addQuad(buffer, baseX - cameraX, bottomWorldY - cameraY, baseZ - cameraZ,
                                    endWorldX - cameraX, topWorldY - cameraY, endWorldZ - cameraZ,
                                    widthX, widthZ, textureOffset, brightness, blackBlend, alpha);
                        }
                    }
                }
            }
            tesselator.end();
        } finally {
            RenderSystem.polygonOffset(516, 0.1F);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void addQuad(BufferBuilder buffer, double x1, double y1, double z1, double x2, double y2,
            double z2, double widthX, double widthZ, double textureOffset, float brightness, float blackBlend,
            float alpha) {
        float color = brightness * (1.0F - blackBlend);
        buffer.vertex(x1 - widthX, y1, z1 - widthZ).color(color, color, color, alpha)
                .uv(0.0F, (float) textureOffset).endVertex();
        buffer.vertex(x1 + widthX, y1, z1 + widthZ).color(color, color, color, alpha)
                .uv(1.0F, (float) textureOffset).endVertex();
        buffer.vertex(x2 + widthX, y2, z2 + widthZ).color(color, color, color, alpha)
                .uv(1.0F, (float) (textureOffset + 1.0D)).endVertex();
        buffer.vertex(x2 - widthX, y2, z2 - widthZ).color(color, color, color, alpha)
                .uv(0.0F, (float) (textureOffset + 1.0D)).endVertex();
    }

    private static int surfaceY(Minecraft minecraft, double x, double z) {
        BlockPos column = new BlockPos(Mth.floor(x), 0, Mth.floor(z));
        return minecraft.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column).getY();
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

    private static Tesselator tesselator() {
        if (tesselator == null) {
            tesselator = new Tesselator(BUFFER_BYTES);
        }
        return tesselator;
    }
}
