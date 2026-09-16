package alku.csrp.celestial.client;

import alku.csrp.Csrp;
import alku.csrp.celestial.CelestialCatalog;
import alku.csrp.celestial.CelestialDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class CelestialSkyRenderer {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final Map<String, Long> ORBIT_STARTS = new HashMap<>();
    private static long lastDayTime;

    private CelestialSkyRenderer() {
    }

    @SubscribeEvent
    public static void render(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || CelestialClientState.active().isEmpty()) return;
        if (CelestialClientState.isActive("dark_days")) {
            renderDarkSky(event);
            return;
        }
        long dayTime = level.getOverworldClockTime();
        long timeOfDay = Math.floorMod(dayTime, 24000L);
        if (timeOfDay < 13000L || timeOfDay > 23000L) return;
        if (timeOfDay < lastDayTime) ORBIT_STARTS.clear();
        lastDayTime = timeOfDay;
        float partialTick = event.getLevelRenderState().worldPartialTicks;
        for (String id : CelestialClientState.active()) {
            CelestialDefinition definition = CelestialCatalog.get(id);
            if (definition != null && !id.equals("dark_days")) {
                renderObject(event, definition, level, partialTick);
            }
        }
    }

    private static void renderObject(SubmitCustomGeometryEvent event, CelestialDefinition definition,
            ClientLevel level, float partialTick) {
        long gameTime = level.getGameTime();
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
        if (definition.followsStars()) {
            double dayFraction = Mth.frac(level.getOverworldClockTime() / 24000.0 - 0.25);
            yaw += (float) dayFraction * 360.0F;
        }
        yaw += ticks / 20.0F * definition.rotationSpeed();

        float brightness = event.getLevelRenderState().skyRenderState.starBrightness;
        float rain = level.getRainLevel(partialTick);
        float opacity = Math.clamp(definition.baseOpacity() * brightness * (1.0F - rain), 0.0F, 1.0F);
        if (opacity <= 0.001F) {
            return;
        }
        int alpha = (int) (opacity * 255.0F);

        float halfHeight = definition.size();
        float halfWidth = halfHeight;
        int frames = Math.max(1, definition.frameCount());
        int frameTime = Math.max(1, definition.frameTimeTicks());
        int frame = definition.animated() ? (int) (gameTime / frameTime % frames) : 0;
        float v0 = frame / (float) frames;
        float v1 = (frame + 1) / (float) frames;

        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector submitNodeCollector = event.getSubmitNodeCollector();
        RenderType renderType = RenderTypes.entityTranslucent(definition.texture());
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, -yaw);
        poseStack.rotateDegrees(Axis.XP, pitch);
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            buffer.addVertex(pose, -halfWidth, -halfHeight, -180).setColor(255, 255, 255, alpha)
                    .setUv(0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT)
                    .setNormal(pose, 0, 0, 1);
            buffer.addVertex(pose, halfWidth, -halfHeight, -180).setColor(255, 255, 255, alpha)
                    .setUv(1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT)
                    .setNormal(pose, 0, 0, 1);
            buffer.addVertex(pose, halfWidth, halfHeight, -180).setColor(255, 255, 255, alpha)
                    .setUv(1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT)
                    .setNormal(pose, 0, 0, 1);
            buffer.addVertex(pose, -halfWidth, halfHeight, -180).setColor(255, 255, 255, alpha)
                    .setUv(0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT)
                    .setNormal(pose, 0, 0, 1);
        });
        poseStack.popPose();
    }

    private static void renderDarkSky(SubmitCustomGeometryEvent event) {
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector submitNodeCollector = event.getSubmitNodeCollector();
        RenderType renderType = RenderTypes.debugQuads();
        poseStack.pushPose();
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            addFace(buffer, pose, 100, 0, 1, 2);
            addFace(buffer, pose, -100, 0, 1, 2);
            addFace(buffer, pose, 100, 1, 0, 2);
            addFace(buffer, pose, -100, 1, 0, 2);
            addFace(buffer, pose, 100, 2, 0, 1);
            addFace(buffer, pose, -100, 2, 0, 1);
        });
        poseStack.popPose();
    }

    private static void addFace(VertexConsumer buffer, PoseStack.Pose pose, float fixed, int axis,
            int first, int second) {
        float[][] points = {{-100, -100}, {100, -100}, {100, 100}, {-100, 100}};
        for (float[] point : points) {
            float[] xyz = new float[3];
            xyz[axis] = fixed;
            xyz[first] = point[0];
            xyz[second] = point[1];
            buffer.addVertex(pose, xyz[0], xyz[1], xyz[2]).setColor(0, 0, 0, 255);
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
    }
}
