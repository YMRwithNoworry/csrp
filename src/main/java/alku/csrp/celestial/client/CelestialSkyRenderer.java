package alku.csrp.celestial.client;

import alku.csrp.Csrp;
import alku.csrp.celestial.CelestialCatalog;
import alku.csrp.celestial.CelestialDefinition;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.FogShape;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CelestialSkyRenderer {
    private static final Map<String, Long> ORBIT_STARTS = new HashMap<>();
    private static long lastDayTime;

    private CelestialSkyRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY
                || Minecraft.getInstance().level == null || CelestialClientState.active().isEmpty()) return;
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(event.getModelViewMatrix());
        if (CelestialClientState.isActive("dark_days")) {
            renderDarkSky(poseStack);
            return;
        }
        long dayTime = Minecraft.getInstance().level.getDayTime();
        long timeOfDay = Math.floorMod(dayTime, 24000L);
        if (timeOfDay < 13000L || timeOfDay > 23000L) return;
        if (timeOfDay < lastDayTime) ORBIT_STARTS.clear();
        lastDayTime = timeOfDay;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (String id : CelestialClientState.active()) {
            CelestialDefinition definition = CelestialCatalog.get(id);
            if (definition != null && !id.equals("dark_days")) {
                renderObject(poseStack, definition, Minecraft.getInstance().level.getGameTime(), partialTick);
            }
        }
    }

    private static void renderObject(PoseStack poseStack, CelestialDefinition definition,
            long gameTime, float partialTick) {
        float ticks = gameTime + partialTick;
        float yaw = definition.yaw();
        float pitch = definition.pitch();
        if (definition.orbitPath() != CelestialDefinition.OrbitPath.NONE && definition.orbitPeriodTicks() > 0) {
            float progress;
            if (definition.oneShotOrbit()) {
                long start = ORBIT_STARTS.computeIfAbsent(definition.id(), ignored -> gameTime);
                progress = (gameTime - start + partialTick) / definition.orbitPeriodTicks();
                if (progress >= 1.0F) return;
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
        if (definition.followsStars() && Minecraft.getInstance().level != null) {
            yaw += Minecraft.getInstance().level.getTimeOfDay(partialTick) * 360.0F;
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
        float brightness = Minecraft.getInstance().level == null ? 1.0F
                : Minecraft.getInstance().level.getStarBrightness(partialTick);
        float rain = Minecraft.getInstance().level == null ? 0.0F
                : Minecraft.getInstance().level.getRainLevel(partialTick);
        float opacity = Math.clamp(definition.baseOpacity() * brightness * (1.0F - rain), 0.0F, 1.0F);
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
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -halfWidth, -halfHeight, -180).setUv(0, v1);
        buffer.addVertex(matrix, halfWidth, -halfHeight, -180).setUv(1, v1);
        buffer.addVertex(matrix, halfWidth, halfHeight, -180).setUv(1, v0);
        buffer.addVertex(matrix, -halfWidth, halfHeight, -180).setUv(0, v0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
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
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        addFace(buffer, matrix, 100, 0, 1, 2);
        addFace(buffer, matrix, -100, 0, 1, 2);
        addFace(buffer, matrix, 100, 1, 0, 2);
        addFace(buffer, matrix, -100, 1, 0, 2);
        addFace(buffer, matrix, 100, 2, 0, 1);
        addFace(buffer, matrix, -100, 2, 0, 1);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
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
            buffer.addVertex(matrix, xyz[0], xyz[1], xyz[2]).setColor(0, 0, 0, 255);
        }
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (!CelestialClientState.isActive("dark_days")) return;
        event.setRed(0);
        event.setGreen(0);
        event.setBlue(0);
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        if (!CelestialClientState.isActive("dark_days")) return;
        event.setNearPlaneDistance(0.0F);
        event.setFarPlaneDistance(24.0F);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
}
