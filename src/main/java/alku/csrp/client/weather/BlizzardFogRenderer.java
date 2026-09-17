package alku.csrp.client.weather;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders the eight perturbed spherical fog shells that wrap the camera during a cold star blizzard.
 * Port of 1.10.9's {@code SRPBlizzardFogRenderer}. The original only ran when an OptiFine shader pack was
 * active; on 1.20.1 that probe can never succeed, so the effect now runs on Forge's own pipeline and is
 * drawn from the mod's own render stage. Shell geometry, deformation, radii, colours and alpha limits are
 * preserved verbatim.
 */
public final class BlizzardFogRenderer {
    private static final int SHELL_COUNT = 8;
    private static final int LONGITUDE_SEGMENTS = 32;
    private static final int LATITUDE_SEGMENTS = 14;
    /** 8 shells x 448 quads x 4 vertices; the default Tesselator buffer would overflow. */
    private static final int BUFFER_BYTES = 2 * 1024 * 1024;
    private static final ResourceLocation SNOW_TEXTURE =
            new ResourceLocation(Csrp.MODID, "textures/environment/snow.png");

    private static Tesselator tesselator;

    private BlizzardFogRenderer() {
    }

    public static void render(Minecraft minecraft, float partialTick, float intensity) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        intensity = Mth.clamp(intensity, 0.0F, 1.0F);
        if (intensity <= 0.001F) {
            return;
        }

        float strength = smoothStep(intensity);
        double nearRadius = lerp(14.0D, 5.5D, strength);
        double farRadius = lerp(48.0D, 20.0D, strength);
        float baseAlpha = 0.02F + strength * 0.14F;
        float blackBlend = Mth.clamp(BlizzardDirectionClient.getBlackBlend(partialTick), 0.0F, 1.0F);
        float red = lerp(0.78F, 0.055F, blackBlend);
        float green = lerp(0.82F, 0.06F, blackBlend);
        float blue = lerp(0.86F, 0.07F, blackBlend);
        float daylight = Mth.clamp((float) minecraft.level.getSkyDarken(), 0.0F, 1.0F);
        float lightMultiplier = 0.78F + daylight * 0.22F;
        red *= lightMultiplier;
        green *= lightMultiplier;
        blue *= lightMultiplier;

        // The level's pose stack is already camera-relative at this stage, so the shells are emitted at
        // the origin of that space and need no further transform.
        double time = minecraft.level.getGameTime() + partialTick;
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderTexture(0, SNOW_TEXTURE);
        try {
            Tesselator tesselator = tesselator();
            BufferBuilder buffer = tesselator.getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int shell = SHELL_COUNT - 1; shell >= 0; shell--) {
                float shellProgress = shell / (float) (SHELL_COUNT - 1);
                double radius = lerp(nearRadius, farRadius, shellProgress);
                float shellAlpha = Mth.clamp(baseAlpha * (0.55F + shellProgress * 0.45F), 0.0F, 0.22F);
                addShell(buffer, radius, shell, time, red, green, blue, shellAlpha);
            }
            tesselator.end();
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void addShell(BufferBuilder buffer, double radius, int shellIndex, double time,
            float red, float green, float blue, float alpha) {
        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            double latitude0 = (-Math.PI / 2.0D) + Math.PI * latitude / LATITUDE_SEGMENTS;
            double latitude1 = (-Math.PI / 2.0D) + Math.PI * (latitude + 1) / LATITUDE_SEGMENTS;
            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                double longitude0 = (Math.PI * 2.0D) * longitude / LONGITUDE_SEGMENTS;
                double longitude1 = (Math.PI * 2.0D) * (longitude + 1) / LONGITUDE_SEGMENTS;
                addShellVertex(buffer, radius, longitude0, latitude0, shellIndex, time, red, green, blue, alpha);
                addShellVertex(buffer, radius, longitude1, latitude0, shellIndex, time, red, green, blue, alpha);
                addShellVertex(buffer, radius, longitude1, latitude1, shellIndex, time, red, green, blue, alpha);
                addShellVertex(buffer, radius, longitude0, latitude1, shellIndex, time, red, green, blue, alpha);
            }
        }
    }

    private static void addShellVertex(BufferBuilder buffer, double radius, double longitude, double latitude,
            int shellIndex, double time, float red, float green, float blue, float alpha) {
        double waveA = Math.sin(longitude * 3.0D + time * 0.01D + shellIndex * 1.73D);
        double waveB = Math.cos(latitude * 4.0D - time * 0.006D + shellIndex * 0.91D);
        double deformation = 1.0D + waveA * waveB * 0.018D;
        double actualRadius = radius * deformation;
        double cosLatitude = Math.cos(latitude);
        double x = Math.cos(longitude) * cosLatitude * actualRadius;
        double y = Math.sin(latitude) * actualRadius;
        double z = Math.sin(longitude) * cosLatitude * actualRadius;
        buffer.vertex(x, y, z).color(red, green, blue, alpha).endVertex();
    }

    private static Tesselator tesselator() {
        if (tesselator == null) {
            tesselator = new Tesselator(BUFFER_BYTES);
        }
        return tesselator;
    }

    private static float smoothStep(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }
}
