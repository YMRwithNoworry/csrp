package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.HaunterHomingProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renderer for the original Pheon homing orb's rotating three-plane model. */
public final class HaunterHomingProjectileRenderer
        extends EntityRenderer<HaunterHomingProjectileEntity, HaunterHomingProjectileRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "haunter_homing_projectile"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/projectileh.png");

    private final BodyModel model;

    public HaunterHomingProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new BodyModel(context.bakeLayer(LAYER));
        shadowRadius = 0.0F;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -4.0F, -1.0F, 8.0F, 8.0F, 2.0F)
                .texOffs(0, 10).addBox(-1.0F, -4.0F, -4.0F, 2.0F, 8.0F, 8.0F)
                .texOffs(20, 0).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HaunterHomingProjectileEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        float age = state.ageInTicks;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.15D, 0.0D);
        poseStack.rotateDegrees(Axis.YP, Mth.sin(age * 0.1F) * 180.0F);
        poseStack.rotateDegrees(Axis.XP, Mth.cos(age * 0.1F) * 180.0F);
        poseStack.rotateDegrees(Axis.ZP, Mth.sin(age * 0.15F) * 360.0F);
        poseStack.scale(-0.5F, -0.5F, 0.5F);
        submitNodeCollector.submitModel(model, state, poseStack, RenderTypes.entityCutout(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        submitNodeCollector.order(1).submitModel(model, state, poseStack, RenderTypes.entityTranslucent(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, 0x80FFFFFF, null, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    /**
     * Applies the entity yaw/pitch to the body at draw time, since render submissions are
     * queued and the shared {@link ModelPart} must not be mutated per-submit.
     */
    private static final class BodyModel extends EntityModel<State> {
        private final ModelPart body;

        BodyModel(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            this.body.setRotation(state.xRot * Mth.DEG_TO_RAD, state.yRot * Mth.DEG_TO_RAD, 0.0F);
        }
    }

    public static final class State extends EntityRenderState {
        public float yRot;
        public float xRot;
    }
}
