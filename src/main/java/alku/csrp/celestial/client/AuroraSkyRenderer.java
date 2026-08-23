package alku.csrp.celestial.client;

import alku.csrp.Csrp;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Draws the aurora shader over snowy night skies. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class AuroraSkyRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraSkyRenderer.class);
    private static final ResourceLocation SHADER_LOCATION =
            new ResourceLocation(Csrp.MODID, "aurora_sky");
    private static final float RADIUS = 100.0F;
    private static final float BRIGHTNESS = 1.7F;
    private static final float SPEED = 0.12F;
    private static final float HEIGHT = 42.0F;
    private static final float SCALE = 0.8F;
    private static final int LATITUDE_SEGMENTS = 24;
    private static final int LONGITUDE_SEGMENTS = 48;

    private static ShaderInstance shader;
    private static DynamicTexture gradientTexture;
    private static VertexBuffer cachedVertexBuffer;
    private static boolean loadAttempted;

    private AuroraSkyRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !isNight(minecraft) || !isSnowyBiome(minecraft)) {
            return;
        }
        ensureLoaded(minecraft);
        if (shader == null) {
            return;
        }
        renderAuroraSphere(event, minecraft);
    }

    private static boolean isSnowyBiome(Minecraft minecraft) {
        return minecraft.level.getBiome(minecraft.player.blockPosition())
                .value().getPrecipitationAt(minecraft.player.blockPosition())
                == Biome.Precipitation.SNOW;
    }

    private static boolean isNight(Minecraft minecraft) {
        long timeOfDay = Math.floorMod(minecraft.level.getDayTime(), 24000L);
        return timeOfDay >= 13000L && timeOfDay <= 23000L;
    }

    private static void ensureLoaded(Minecraft minecraft) {
        if (shader != null || loadAttempted) {
            return;
        }
        loadAttempted = true;
        try {
            shader = new ShaderInstance(minecraft.getResourceManager(), SHADER_LOCATION,
                    DefaultVertexFormat.POSITION);
            gradientTexture = createGradientTexture();
            shader.setSampler("ColorGradient", gradientTexture);
        } catch (Exception exception) {
            LOGGER.error("Failed to load aurora sky shader", exception);
            dispose();
            loadAttempted = true;
        }
    }

    private static DynamicTexture createGradientTexture() {
        NativeImage image = new NativeImage(256, 1, true);
        for (int x = 0; x < 256; x++) {
            image.setPixelRGBA(x, 0, gradientColor(x / 255.0F));
        }
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(false, true);
        return texture;
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

    private static void renderAuroraSphere(RenderLevelStageEvent event, Minecraft minecraft) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        float partialTick = event.getPartialTick();
        setUniform("AuroraTime", (minecraft.level.getGameTime() + partialTick) / 20.0F);
        setUniform("Brightness", BRIGHTNESS);
        setUniform("Speed", SPEED);
        setUniform("Height", HEIGHT);
        setUniform("Scale", SCALE);

        try {
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            if (cachedVertexBuffer == null) {
                cachedVertexBuffer = buildAuroraMesh();
            }
            cachedVertexBuffer.bind();
            cachedVertexBuffer.drawWithShader(matrix, event.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
        } finally {
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }

    private static void setUniform(String name, float value) {
        if (shader.getUniform(name) != null) {
            shader.getUniform(name).set(value);
        }
    }

    private static VertexBuffer buildAuroraMesh() {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        int hemisphereSegments = LATITUDE_SEGMENTS / 2;
        for (int lat = 0; lat < hemisphereSegments; lat++) {
            float theta0 = (float) lat / LATITUDE_SEGMENTS * (float) Math.PI;
            float theta1 = (float) (lat + 1) / LATITUDE_SEGMENTS * (float) Math.PI;
            for (int lon = 0; lon < LONGITUDE_SEGMENTS; lon++) {
                float phi0 = (float) lon / LONGITUDE_SEGMENTS * (float) (Math.PI * 2.0D);
                float phi1 = (float) (lon + 1) / LONGITUDE_SEGMENTS * (float) (Math.PI * 2.0D);
                addVertex(buffer, direction(theta0, phi0));
                addVertex(buffer, direction(theta0, phi1));
                addVertex(buffer, direction(theta1, phi1));
                addVertex(buffer, direction(theta1, phi0));
            }
        }
        BufferBuilder.RenderedBuffer rendered = buffer.end();
        VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vertexBuffer.bind();
        vertexBuffer.upload(rendered);
        VertexBuffer.unbind();
        return vertexBuffer;
    }

    private static Vector3f direction(float theta, float phi) {
        float sinTheta = (float) Math.sin(theta);
        return new Vector3f(sinTheta * (float) Math.cos(phi), (float) Math.cos(theta),
                sinTheta * (float) Math.sin(phi)).mul(RADIUS);
    }

    private static void addVertex(BufferBuilder buffer, Vector3f direction) {
        buffer.vertex(direction.x, direction.y, direction.z).endVertex();
    }

    public static void dispose() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
        if (gradientTexture != null) {
            gradientTexture.close();
            gradientTexture = null;
        }
        if (cachedVertexBuffer != null) {
            cachedVertexBuffer.close();
            cachedVertexBuffer = null;
        }
        loadAttempted = false;
    }
}
