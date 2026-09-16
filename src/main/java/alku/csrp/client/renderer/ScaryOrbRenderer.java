package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.ScaryOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class ScaryOrbRenderer extends EntityRenderer<ScaryOrbEntity, ScaryOrbRenderer.ScaryOrbRenderState> {
    private static final Identifier CORE_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/orbscary.png");
    private static final Identifier AURA_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/orbscary_armor.png");
    private static final float ORB_DIAMETER = 2.4F;
    private static final float SPHERE_RADIUS = ORB_DIAMETER * 0.5F;
    private static final int SPHERE_STACKS = 18;
    private static final int SPHERE_SLICES = 18;
    private static final int FULL_BRIGHT = 0xF000F0;

    public ScaryOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public ScaryOrbRenderState createRenderState() {
        return new ScaryOrbRenderState();
    }

    @Override
    public void extractRenderState(ScaryOrbEntity entity, ScaryOrbRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tickCount = entity.tickCount;
    }

    @Override
    public void submit(ScaryOrbRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        float age = state.tickCount + state.partialTick;
        float appear = Mth.clamp(age / 10.0F, 0.25F, 1.0F);
        float pulse = 1.0F + Mth.sin(age * 0.3F) * 0.08F;

        poseStack.pushPose();
        poseStack.scale(appear * pulse, appear * pulse, appear * pulse);
        poseStack.rotate(Axis.YP, age * 0.05F);
        submitSphere(poseStack, submitNodeCollector, RenderTypes.entityTranslucentEmissive(CORE_TEXTURE),
                SPHERE_RADIUS, 1.0F, 1.0F, 1.0F, 220);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(appear * 1.12F, appear * 1.12F, appear * 1.12F);
        poseStack.rotate(Axis.YP, -age * 0.07F);
        submitSphere(poseStack, submitNodeCollector, RenderTypes.entityTranslucentEmissive(AURA_TEXTURE),
                SPHERE_RADIUS, 1.0F, 1.0F, 1.0F, 135);
        poseStack.popPose();

        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void submitSphere(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                     RenderType renderType, float radius,
                                     float red, float green, float blue, int alpha) {
        submitNodeCollector.submitCustomGeometry(poseStack, renderType,
                (pose, consumer) -> renderSphere(pose, consumer, radius, red, green, blue, alpha));
    }

    private static void renderSphere(PoseStack.Pose pose, VertexConsumer consumer, float radius,
                                     float red, float green, float blue, int alpha) {
        for (int stack = 0; stack < SPHERE_STACKS; stack++) {
            float v0 = stack / (float) SPHERE_STACKS;
            float v1 = (stack + 1) / (float) SPHERE_STACKS;
            float phi0 = (float) Math.PI * v0;
            float phi1 = (float) Math.PI * v1;
            for (int slice = 0; slice < SPHERE_SLICES; slice++) {
                float u0 = slice / (float) SPHERE_SLICES;
                float u1 = (slice + 1) / (float) SPHERE_SLICES;
                float theta0 = (float) (Math.PI * 2.0D * u0);
                float theta1 = (float) (Math.PI * 2.0D * u1);
                renderVertex(pose, consumer, radius, phi0, theta0, u0, v0, red, green, blue, alpha);
                renderVertex(pose, consumer, radius, phi1, theta0, u0, v1, red, green, blue, alpha);
                renderVertex(pose, consumer, radius, phi1, theta1, u1, v1, red, green, blue, alpha);
                renderVertex(pose, consumer, radius, phi0, theta1, u1, v0, red, green, blue, alpha);
            }
        }
    }

    private static void renderVertex(PoseStack.Pose pose, VertexConsumer consumer, float radius,
                                     float phi, float theta, float u, float v,
                                     float red, float green, float blue, int alpha) {
        float x = Mth.sin(phi) * Mth.cos(theta);
        float y = Mth.cos(phi);
        float z = Mth.sin(phi) * Mth.sin(theta);
        consumer.addVertex(pose, x * radius, y * radius, z * radius)
                .setColor((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, x, y, z);
    }

    /** Per-frame snapshot of the orb animation. */
    public static final class ScaryOrbRenderState extends EntityRenderState {
        public float tickCount;
    }
}
