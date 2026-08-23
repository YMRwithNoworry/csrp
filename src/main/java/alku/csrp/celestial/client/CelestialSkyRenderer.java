package alku.csrp.celestial.client;

import net.minecraft.util.Mth;
import alku.csrp.Csrp;
import alku.csrp.celestial.CelestialCatalog;
import alku.csrp.celestial.CelestialDefinition;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class CelestialSkyRenderer {
    private static final Map<String, Long> ORBIT_STARTS = new HashMap<>();
    private static long lastDayTime;

    private CelestialSkyRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
                || minecraft.level == null || CelestialClientState.active().isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        if (CelestialClientState.isActive("dark_days")) {
            renderDarkSky(poseStack);
            poseStack.popPose();
            return;
        }
        long dayTime = minecraft.level.getDayTime();
        long timeOfDay = Math.floorMod(dayTime, 24000L);
        if (timeOfDay < 13000L || timeOfDay > 23000L) {
            poseStack.popPose();
            return;
        }
        if (timeOfDay < lastDayTime) {
            ORBIT_STARTS.clear();
        }
        lastDayTime = timeOfDay;
        float partialTick = event.getPartialTick();
        for (String id : CelestialClientState.active()) {
            CelestialDefinition definition = CelestialCatalog.get(id);
            if (definition != null && !id.equals("dark_days")) {
                renderObject(poseStack, definition, minecraft.level.getGameTime(), partialTick);
            }
        }
        poseStack.popPose();
    }

    private static void renderObject(PoseStack poseStack, CelestialDefinition definition,
            long gameTime, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        float ticks = gameTime + partialTick;
        float yaw = definition.yaw();
        float pitch = definition.pitch();
        if (definition.orbitPath() != CelestialDefinition.OrbitPath.NONE
                && definition.orbitPeriodTicks() > 0) {
            float progress;
            if (definition.oneShotOrbit()) {
                long start = ORBIT_STARTS.computeIfAbsent(definition.id(), ignored -> gameTime);
                progress = (gameTime - start + partialTick) / definition.orbitPeriodTicks();
                if (progress >= 1.0F) {
                    return;
                }
            } else {
                progress = (ticks % definition.orbitPeriodTicks()) / definition.orbitPeriodTicks();
            }
            yaw += definition.orbitYawRange() * progress;
            if (definition.orbitPath() == CelestialDefinition.OrbitPath.RING) {
                pitch = definition.orbitPitchMin();
            } else {
                float wave = (float) Math.sin(progress * Math.PI);
                pitch = definition.orbitPitchMin()
                        + (definition.orbitPitchMax() - definition.orbitPitchMin()) * wave;
            }
        } else if (definition.fastStreak()) {
            yaw += (ticks % 12000.0F) / 12000.0F * 360.0F;
        }
        if (definition.followsStars() && minecraft.level != null) {
            yaw += minecraft.level.getTimeOfDay(partialTick) * 360.0F;
        }
        yaw += ticks / 20.0F * definition.rotationSpeed();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, definition.texture());
        float brightness = minecraft.level == null ? 1.0F
                : minecraft.level.getStarBrightness(partialTick);
        float rain = minecraft.level == null ? 0.0F
                : minecraft.level.getRainLevel(partialTick);
        float opacity = Mth.clamp(definition.baseOpacity() * brightness * (1.0F - rain), 0.0F, 1.0F);
        if (opacity <= 0.001F) {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            poseStack.popPose();
            return;
        }
        RenderSystem.setShaderColor(1, 1, 1, opacity);

        float halfHeight = definition.size();
        float halfWidth = halfHeight;
        int frames = Math.max(1, definition.frameCount());
        int frameTime = Math.max(1, definition.frameTimeTicks());
        int frame = definition.animated() ? (int) (gameTime / frameTime % frames) : 0;
        float v0 = frame / (float) frames;
        float v1 = (frame + 1) / (float) frames;
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, -halfWidth, -halfHeight, -180).uv(0, v1).endVertex();
        buffer.vertex(matrix, halfWidth, -halfHeight, -180).uv(1, v1).endVertex();
        buffer.vertex(matrix, halfWidth, halfHeight, -180).uv(1, v0).endVertex();
        buffer.vertex(matrix, -halfWidth, halfHeight, -180).uv(0, v0).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderDarkSky(PoseStack poseStack) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addFace(buffer, matrix, 100, 0, 1, 2);
        addFace(buffer, matrix, -100, 0, 1, 2);
        addFace(buffer, matrix, 100, 1, 0, 2);
        addFace(buffer, matrix, -100, 1, 0, 2);
        addFace(buffer, matrix, 100, 2, 0, 1);
        addFace(buffer, matrix, -100, 2, 0, 1);
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void addFace(BufferBuilder buffer, Matrix4f matrix, float fixed, int axis,
            int first, int second) {
        float[][] points = {{-100, -100}, {100, -100}, {100, 100}, {-100, 100}};
        for (float[] point : points) {
            float[] xyz = new float[3];
            xyz[axis] = fixed;
            xyz[first] = point[0];
            xyz[second] = point[1];
            buffer.vertex(matrix, xyz[0], xyz[1], xyz[2]).color(0, 0, 0, 255).endVertex();
        }
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (!CelestialClientState.isActive("dark_days")) {
            return;
        }
        event.setRed(0);
        event.setGreen(0);
        event.setBlue(0);
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        if (!CelestialClientState.isActive("dark_days")) {
            return;
        }
        event.setNearPlaneDistance(0.0F);
        event.setFarPlaneDistance(24.0F);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
}
