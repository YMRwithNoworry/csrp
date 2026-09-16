package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.VoidOrbEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class VoidOrbRenderer extends EntityRenderer<VoidOrbEntity, VoidOrbRenderer.State> {
    private static final Identifier CORE_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/orbvoid.png");
    private static final Identifier AURA_TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/orbvoid_armor.png");
    private static final float VOID_ORB_DIAMETER = 2.4F;
    private static final float SPHERE_RADIUS = VOID_ORB_DIAMETER * 0.5F;
    private static final int SPHERE_STACKS = 18;
    private static final int SPHERE_SLICES = 18;
    private static final int FULL_BRIGHT = 0xF000F0;

    public VoidOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VoidOrbEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.tickCount = entity.tickCount;
        state.renderScale = entity.getRenderScale(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        float age = state.tickCount + state.partialTick;
        float scale = state.renderScale;
        float pulse = 1.0F + Mth.sin(age * 0.35F) * 0.05F;

        poseStack.pushPose();
        poseStack.scale(scale * pulse, scale * pulse, scale * pulse);
        poseStack.rotate(Axis.YP, age * 0.07F);
        submitSphere(poseStack, submitNodeCollector, RenderTypes.entityTranslucentEmissive(CORE_TEXTURE),
                SPHERE_RADIUS, 0.88F, 0.95F, 1.0F, 235);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(scale * 1.12F, scale * 1.12F, scale * 1.12F);
        poseStack.rotate(Axis.YP, -age * 0.09F);
        submitSphere(poseStack, submitNodeCollector, RenderTypes.entityTranslucentEmissive(AURA_TEXTURE),
                SPHERE_RADIUS, 0.55F, 0.70F, 1.0F, 145);
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
    public static final class State extends EntityRenderState {
        public float tickCount;
        public float renderScale;
    }
}
