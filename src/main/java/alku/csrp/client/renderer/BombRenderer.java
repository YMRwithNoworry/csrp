package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.BombEntity;
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

/** Renderer for the original Omboo, Host and Jinjo bomb models. */
public final class BombRenderer extends EntityRenderer<BombEntity, BombRenderer.BombRenderState> {
    public static final ModelLayerLocation OMBOO_LAYER = layer("bomb_omboo");
    public static final ModelLayerLocation HOST_LAYER = layer("bomb_host");
    public static final ModelLayerLocation JINJO_LAYER = layer("bomb_jinjo");
    private static final Identifier OMBOO_TEXTURE = texture("bombo.png");
    private static final Identifier HOST_TEXTURE = texture("bombh.png");
    private static final Identifier JINJO_TEXTURE = texture("bombj.png");

    private final ModelPart omboo;
    private final ModelPart host;
    private final ModelPart jinjo;

    public BombRenderer(EntityRendererProvider.Context context) {
        super(context);
        omboo = context.bakeLayer(OMBOO_LAYER).getChild("body");
        host = context.bakeLayer(HOST_LAYER).getChild("body");
        jinjo = context.bakeLayer(JINJO_LAYER).getChild("body");
        shadowRadius = 0.5F;
    }

    public static LayerDefinition createOmbooLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, 0.0F, -5.0F, 7.0F, 7.0F, 10.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        body.addOrReplaceChild("right", CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offsetAndRotation(4.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.43633232F));
        body.addOrReplaceChild("front", CubeListBuilder.create()
                        .texOffs(30, 14).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, -5.5F, 0.4712389F, 0.0F, 0.0F));
        body.addOrReplaceChild("left", CubeListBuilder.create()
                        .texOffs(38, 4).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offsetAndRotation(-4.0F, 4.0F, 0.0F, 0.0F, 0.0F, -0.43633232F));
        body.addOrReplaceChild("center", CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 0.3F, 0.0F, 0.0F, 0.0F, Mth.PI / 4.0F));
        body.addOrReplaceChild("back", CubeListBuilder.create()
                        .texOffs(46, 14).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, 5.5F, -0.4712389F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createHostLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        addHostCorner(body, "front_left", 16, 0, -2.0F, -2.0F, -0.36651915F, Mth.PI / 4.0F);
        addHostCorner(body, "front_right", 28, 0, 2.0F, -2.0F, -0.36651915F, -Mth.PI / 4.0F);
        addHostCorner(body, "rear_right", 40, 0, 2.0F, 2.0F, 0.36651915F, -Mth.PI / 4.0F);
        addHostCorner(body, "rear_left", 49, 3, -2.0F, 2.0F, 0.36651915F, Mth.PI / 4.0F);

        PartDefinition reverse = body.addOrReplaceChild("reverse", CubeListBuilder.create()
                        .texOffs(13, 6).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 3.0F),
                PartPose.rotation(0.0F, Mth.PI, 0.0F));
        addHostCorner(reverse, "front_left", 29, 6, -2.0F, -2.0F, -0.36651915F, Mth.PI / 4.0F);
        addHostCorner(reverse, "front_right", 0, 8, 2.0F, -2.0F, -0.36651915F, -Mth.PI / 4.0F);
        addHostCorner(reverse, "rear_right", 38, 9, 2.0F, 2.0F, 0.36651915F, -Mth.PI / 4.0F);
        addHostCorner(reverse, "rear_left", 50, 9, -2.0F, 2.0F, 0.36651915F, Mth.PI / 4.0F);
        return LayerDefinition.create(mesh, 64, 16);
    }

    private static void addHostCorner(PartDefinition parent, String name, int u, int v,
                                      float x, float y, float xRot, float yRot) {
        parent.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(u, v).addBox(-1.5F, -1.5F, -2.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(x, y, -2.0F, xRot, yRot, 0.0F));
    }

    public static LayerDefinition createJinjoLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-3.0F, 0.0F, 0.0F));
        addJinjoPart(body, "mass", 0, 0, -4.0F, -8.0F, -7.0F, 14.0F, 14.0F, 12.0F,
                0.0F, 18.5F, 0.0F, 0.0F, 0.0F, 0.0F);
        addJinjoPart(body, "lower_front", 0, 53, -4.0F, -8.0F, -7.0F, 14.0F, 14.0F, 12.0F,
                -1.1F, 6.5F, -6.0F, 0.50265485F, 0.0F, 0.0F);
        addJinjoPart(body, "column", 40, 30, -4.0F, -12.0F, -3.5F, 15.0F, 22.0F, 6.0F,
                1.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        addJinjoPart(body, "left", 52, 0, -4.0F, 0.0F, -4.0F, 10.0F, 15.0F, 15.0F,
                -8.0F, 4.0F, -5.0F, 0.0F, 0.0F, -0.43982297F);
        addJinjoPart(body, "rear", 0, 79, -3.0F, -16.0F, -7.0F, 14.0F, 19.0F, 12.0F,
                -1.1F, 1.5F, 9.0F, 0.12566371F, 0.0F, 0.0F);
        addJinjoPart(body, "upper_left", 0, 26, -4.0F, -4.5F, -6.0F, 10.0F, 17.0F, 10.0F,
                -5.0F, -6.0F, 1.0F, 0.0F, 0.0F, 0.12566371F);
        addJinjoPart(body, "crest", 90, 109, -1.0F, -16.0F, -7.0F, 8.0F, 14.0F, 10.0F,
                -1.4F, -8.5F, 2.0F, 0.06283186F, 0.0F, 0.0F);
        addJinjoPart(body, "upper_front", 52, 88, -2.5F, -10.0F, -7.0F, 10.0F, 17.0F, 14.0F,
                -1.1F, -2.5F, -7.0F, -0.12566371F, 0.0F, 0.0F);
        addJinjoPart(body, "right", 92, 20, -4.0F, -12.0F, -5.0F, 11.0F, 27.0F, 10.0F,
                8.0F, 5.0F, -1.0F, 0.0F, 0.0F, 0.18849556F);
        addJinjoPart(body, "lower_rear", 70, 57, -1.0F, -16.0F, -7.0F, 11.0F, 19.0F, 12.0F,
                -1.1F, 16.5F, 5.0F, -0.37699112F, 0.0F, 0.0F);
        return LayerDefinition.create(mesh, 135, 150);
    }

    private static void addJinjoPart(PartDefinition parent, String name, int u, int v,
                                     float boxX, float boxY, float boxZ,
                                     float width, float height, float depth,
                                     float x, float y, float z, float xRot, float yRot, float zRot) {
        parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                        .addBox(boxX, boxY, boxZ, width, height, depth),
                PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot));
    }

    @Override
    public BombRenderState createRenderState() {
        return new BombRenderState();
    }

    @Override
    public void extractRenderState(BombEntity entity, BombRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = entity.getSkin();
        state.age = entity.tickCount + partialTick;
    }

    @Override
    public void submit(BombRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        float pulse = 1.2F + Mth.sin(state.age * 0.8F) * 0.05F;
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.rotateDegrees(Axis.ZP, 180.0F);
        poseStack.scale(pulse, pulse, pulse);
        submitNodeCollector.submitModelPart(model(state.skin), poseStack,
                RenderTypes.entityCutout(texture(state.skin)),
                state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private ModelPart model(int skin) {
        return switch (skin) {
            case 1 -> host;
            case 2, 3 -> jinjo;
            default -> omboo;
        };
    }

    private Identifier texture(int skin) {
        return switch (skin) {
            case 1 -> HOST_TEXTURE;
            case 2, 3 -> JINJO_TEXTURE;
            default -> OMBOO_TEXTURE;
        };
    }

    /** Render state for the bomb skin and animation. */
    static final class BombRenderState extends EntityRenderState {
        int skin;
        float age;
    }

    private static ModelLayerLocation layer(String path) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Csrp.MODID, path), "main");
    }

    private static Identifier texture(String file) {
        return Identifier.fromNamespaceAndPath(Csrp.MODID, "textures/entity/monster/" + file);
    }
}
