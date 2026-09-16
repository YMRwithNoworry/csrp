package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.GoreEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Original four-size gore model used for assimilated, primitive, adapted and pure payloads. */
public final class GoreRenderer extends EntityRenderer<GoreEntity, GoreRenderer.GoreRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Csrp.MODID, "gore"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            Csrp.MODID, "textures/entity/monster/gore.png");

    private final ModelPart assimilated;
    private final ModelPart primitive;
    private final ModelPart adapted;
    private final ModelPart pure;

    public GoreRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(LAYER);
        assimilated = root.getChild("sim");
        primitive = root.getChild("pri");
        adapted = root.getChild("ada");
        pure = root.getChild("pure");
        shadowRadius = 0.1F;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition sim = root.addOrReplaceChild("sim", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 21.0F, 0.0F, 0.0F, -Mth.PI / 4.0F, 0.0F));
        addDecoration(sim, "dec", 80, 4, 8.0F, 16.0F, false);
        addDecoration(sim, "dec_1", 48, 20, 8.0F, 16.0F, true);

        PartDefinition pri = root.addOrReplaceChild("pri", CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 0.0F, -Mth.PI / 4.0F, 0.0F));
        addDecoration(pri, "dec_2", 64, 36, 9.0F, 18.0F, true);
        addDecoration(pri, "dec_3", 0, 40, 9.0F, 18.0F, false);

        PartDefinition ada = root.addOrReplaceChild("ada", CubeListBuilder.create()
                        .texOffs(56, 0).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, 0.0F, -Mth.PI / 4.0F, 0.0F));
        addDecoration(ada, "dec_4", 38, 52, 10.0F, 20.0F, true);
        addDecoration(ada, "dec_5", 82, 52, 10.0F, 20.0F, false);

        PartDefinition pure = root.addOrReplaceChild("pure", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.0F, -Mth.PI / 4.0F, 0.0F));
        addDecoration(pure, "dec_6", 58, 72, 11.0F, 22.0F, true);
        addDecoration(pure, "dec_7", 0, 76, 11.0F, 22.0F, false);
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addDecoration(PartDefinition parent, String name, int u, int v,
                                      float radius, float size, boolean rotateY) {
        parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                        .addBox(-0.5F, -radius, -radius, 1.0F, size, size),
                PartPose.rotation(Mth.PI / 4.0F, rotateY ? Mth.PI / 2.0F : 0.0F, 0.0F));
    }

    @Override
    public GoreRenderState createRenderState() {
        return new GoreRenderState();
    }

    @Override
    public void extractRenderState(GoreEntity entity, GoreRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = entity.getSkin();
    }

    @Override
    public void submit(GoreRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.rotateDegrees(Axis.ZP, 180.0F);
        ModelPart model = model(state.skin);
        if (model != null) {
            submitNodeCollector.submitModelPart(model, poseStack, RenderTypes.entityCutout(TEXTURE),
                    state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        }
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private ModelPart model(int skin) {
        return switch (skin) {
            case 1, 10 -> assimilated;
            case 2 -> primitive;
            case 3 -> adapted;
            case 4 -> pure;
            default -> null;
        };
    }

    /** Per-frame snapshot of the data the geometry submission needs. */
    public static final class GoreRenderState extends EntityRenderState {
        public int skin;
    }
}
