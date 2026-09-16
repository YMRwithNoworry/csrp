package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.block.TrophyBlock;
import alku.csrp.block.entity.TrophyBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Renders the animated Void/Boom Orb suspended above its trophy base. */
public final class TrophyBlockEntityRenderer
        implements BlockEntityRenderer<TrophyBlockEntity, TrophyBlockEntityRenderer.TrophyRenderState> {
    private static final Identifier VOID_CORE = texture("orbvoid.png");
    private static final Identifier VOID_AURA = texture("orbvoid_armor.png");
    private static final Identifier BOOM_CORE = texture("orbboom.png");
    private static final Identifier BOOM_AURA = texture("orbboom_armor.png");
    private static final float LEGACY_TROPHY_SCALE = 5.0F;
    private static final float LEGACY_SPHERE_RADIUS = 0.317F;
    private static final float LEGACY_AURA_SCALE = 1.12F;
    private static final int STACKS = 18;
    private static final int SLICES = 18;
    private static final int FULL_BRIGHT = 0xF000F0;

    public TrophyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public TrophyRenderState createRenderState() {
        return new TrophyRenderState();
    }

    @Override
    public void extractRenderState(TrophyBlockEntity trophy, TrophyRenderState state, float partialTick,
            Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(trophy, state, partialTick, cameraPosition, breakProgress);
        state.valid = trophy.getBlockState().getBlock() instanceof TrophyBlock;
        state.voidOrb = trophy.getBlockState().getBlock() instanceof TrophyBlock block
                && block.kind() == TrophyBlock.Kind.VOID;
        state.age = (trophy.getLevel() == null ? 0L : trophy.getLevel().getGameTime()) + partialTick;
    }

    @Override
    public void submit(TrophyRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        if (!state.valid) {
            return;
        }
        float age = state.age;
        float pulse = 1.0F + Mth.sin(age * 0.12F) * 0.05F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.35D, 0.5D);
        poseStack.rotate(Axis.YP.rotation(age * 0.035F));
        poseStack.scale(LEGACY_TROPHY_SCALE * pulse, LEGACY_TROPHY_SCALE * pulse,
                LEGACY_TROPHY_SCALE * pulse);
        RenderType coreType = RenderTypes.entityTranslucentEmissive(
                state.voidOrb ? VOID_CORE : BOOM_CORE);
        submitNodeCollector.submitCustomGeometry(poseStack, coreType, (pose, consumer) ->
                renderSphere(pose, consumer, LEGACY_SPHERE_RADIUS, 235));
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.35D, 0.5D);
        poseStack.rotate(Axis.YP.rotation(-age * 0.05F));
        poseStack.scale(LEGACY_TROPHY_SCALE, LEGACY_TROPHY_SCALE, LEGACY_TROPHY_SCALE);
        RenderType auraType = RenderTypes.entityTranslucentEmissive(
                state.voidOrb ? VOID_AURA : BOOM_AURA);
        submitNodeCollector.submitCustomGeometry(poseStack, auraType, (pose, consumer) ->
                renderSphere(pose, consumer, LEGACY_SPHERE_RADIUS * LEGACY_AURA_SCALE, 125));
        poseStack.popPose();
    }

    private static void renderSphere(PoseStack.Pose pose, VertexConsumer consumer, float radius, int alpha) {
        for (int stack = 0; stack < STACKS; stack++) {
            float v0 = stack / (float) STACKS;
            float v1 = (stack + 1) / (float) STACKS;
            float phi0 = (float) Math.PI * v0;
            float phi1 = (float) Math.PI * v1;
            for (int slice = 0; slice < SLICES; slice++) {
                float u0 = slice / (float) SLICES;
                float u1 = (slice + 1) / (float) SLICES;
                float theta0 = (float) (Math.PI * 2.0D * u0);
                float theta1 = (float) (Math.PI * 2.0D * u1);
                vertex(pose, consumer, radius, phi0, theta0, u0, v0, alpha);
                vertex(pose, consumer, radius, phi1, theta0, u0, v1, alpha);
                vertex(pose, consumer, radius, phi1, theta1, u1, v1, alpha);
                vertex(pose, consumer, radius, phi0, theta1, u1, v0, alpha);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float radius,
            float phi, float theta, float u, float v, int alpha) {
        float x = Mth.sin(phi) * Mth.cos(theta);
        float y = Mth.cos(phi);
        float z = Mth.sin(phi) * Mth.sin(theta);
        consumer.addVertex(pose, x * radius, y * radius, z * radius)
                .setColor(255, 255, 255, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT)
                .setNormal(pose, x, y, z);
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(Csrp.MODID, "textures/entity/" + name);
    }

    /** Per-frame snapshot of the trophy data needed to submit the orb geometry. */
    public static final class TrophyRenderState extends BlockEntityRenderState {
        public boolean valid;
        public boolean voidOrb;
        public float age;
    }
}
