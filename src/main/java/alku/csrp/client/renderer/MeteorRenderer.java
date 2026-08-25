package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.MeteorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Recreates the original meteor's irregular body and five animated tendrils. */
public final class MeteorRenderer extends EntityRenderer<MeteorEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(Csrp.MODID, "meteor"), "main");
    private static final ResourceLocation TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/projectile/meteor.png");
    private static final float SCALE_WOBBLE = 1.0F + Mth.sin(100.0F) * 0.01F;
    private static final float MAIN_SCALE_XZ = 0.5F + 1.4F * SCALE_WOBBLE;
    private static final float MAIN_SCALE_Y = 0.5F + 1.1F / SCALE_WOBBLE;
    private static final float FRAGMENT_SCALE_XZ = -0.8F + 1.4F * SCALE_WOBBLE;
    private static final float FRAGMENT_SCALE_Y = -0.8F + 1.1F / SCALE_WOBBLE;

    private final ModelPart body;
    private final ModelPart lowerBody;
    private final ModelPart[] lobes;
    private final List<ModelPart[]> tendrils = new ArrayList<>();

    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
        body = context.bakeLayer(LAYER).getChild("body");
        lowerBody = body.getChild("lower_body");
        ModelPart rearBody = lowerBody.getChild("rear_body");
        lobes = new ModelPart[]{
                rearBody.getChild("rear_lobe_1"), rearBody.getChild("rear_lobe_2"),
                rearBody.getChild("rear_lobe_3"), rearBody.getChild("rear_lobe_4"),
                rearBody.getChild("rear_lobe_5"), rearBody.getChild("rear_lobe_6"),
                rearBody.getChild("rear_lobe_7"), rearBody.getChild("rear_lobe_8"),
                rearBody.getChild("rear_lobe_9")
        };
        tendrils.add(findTendril(body.getChild("front_cap"), "front"));
        tendrils.add(findTendril(body.getChild("upper_cap"), "upper"));
        tendrils.add(findTendril(body.getChild("side_cap"), "side_left"));
        tendrils.add(findTendril(body.getChild("side_cap"), "side_right"));
        tendrils.add(findTendril(body.getChild("lower_cap"), "lower"));
        shadowRadius = 0.0F;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, -29.3F, 0.0F));

        PartDefinition lowerBody = addBox(body, "lower_body", 0, 0,
                -25.0F, 0.0F, -25.0F, 50.0F, 25.0F, 50.0F,
                0.0F, 16.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        addBox(lowerBody, "lower_plate_1", 234, 136,
                -31.0F, -21.0F, -10.0F, 34.0F, 40.0F, 18.0F,
                27.0F, 4.0F, -28.0F, 0.0F, 0.0F, 0.0F);
        addBox(lowerBody, "lower_plate_2", 132, 110,
                -13.0F, -12.0F, -9.0F, 26.0F, 47.0F, 25.0F,
                -15.0F, -27.0F, -33.0F, 0.0F, 0.0F, 0.0F);
        addBox(lowerBody, "lower_plate_3", 480, 171,
                -13.0F, -21.0F, -10.0F, 13.0F, 38.0F, 22.0F,
                33.0F, -6.0F, 19.0F, 0.0F, 0.0F, 0.0F);
        addBox(lowerBody, "lower_plate_4", 338, 148,
                -13.0F, -21.0F, -10.0F, 15.0F, 32.0F, 19.0F,
                -17.0F, 39.0F, -9.0F, 0.0F, 0.0F, 0.0F);
        addBox(lowerBody, "lower_plate_5", 0, 0,
                -13.0F, -30.0F, -12.0F, 18.0F, 34.0F, 7.0F,
                -5.0F, 10.0F, 36.0F, 0.0F, 0.0F, 0.0F);
        addBox(lowerBody, "lower_plate_6", 406, 167,
                -13.0F, -21.0F, -9.0F, 7.0F, 29.0F, 30.0F,
                -18.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.0F);
        addBox(lowerBody, "lower_plate_7", 474, 125,
                -13.0F, -14.0F, -15.0F, 19.0F, 31.0F, 15.0F,
                13.0F, 30.0F, 16.0F, 0.0F, 0.0F, 0.0F);
        addBox(lowerBody, "lower_plate_8", 188, 194,
                -13.0F, -21.0F, -9.0F, 19.0F, 40.0F, 17.0F,
                -22.0F, -8.0F, 27.0F, 0.0F, 0.0F, 0.0F);
        addBox(lowerBody, "lower_plate_9", 92, 182,
                -13.0F, -30.0F, -12.0F, 35.0F, 33.0F, 13.0F,
                -1.0F, 2.0F, 39.0F, 0.0F, 0.0F, 0.0F);
        addBox(lowerBody, "lower_plate_10", 0, 141,
                -13.0F, -21.0F, -10.0F, 20.0F, 44.0F, 26.0F,
                29.0F, -29.0F, -22.0F, 0.0F, 0.0F, 0.0F);

        PartDefinition rearBody = addBox(lowerBody, "rear_body", 260, 199,
                -29.0F, -13.0F, -25.0F, 29.0F, 35.0F, 30.0F,
                0.0F, -57.0F, 0.0F, Mth.PI, 0.61994094F, 0.0F);
        addBox(rearBody, "rear_lobe_1", 460, 210,
                -13.0F, -21.0F, -10.0F, 26.0F, 47.0F, 27.0F,
                30.0F, -23.0F, -30.0F, 0.0F, 0.0F, 0.0F);
        addBox(rearBody, "rear_lobe_2", 316, 213,
                -13.0F, -21.0F, -10.0F, 27.0F, 45.0F, 29.0F,
                38.0F, 11.0F, -7.0F, 0.0F, 0.0F, 0.0F);
        addBox(rearBody, "rear_lobe_3", 0, 211,
                -13.0F, -14.0F, -15.0F, 30.0F, 50.0F, 27.0F,
                2.0F, -27.0F, -34.0F, 0.0F, 0.0F, 0.0F);
        addBox(rearBody, "rear_lobe_4", 304, 201,
                -13.0F, -35.0F, -14.0F, 22.0F, 50.0F, 20.0F,
                -14.0F, -7.0F, 43.0F, 0.0F, 0.0F, 0.0F);
        addBox(rearBody, "rear_lobe_5", 108, 221,
                -28.0F, -25.0F, -10.0F, 33.0F, 50.0F, 31.0F,
                -14.0F, -10.0F, -29.0F, 0.0F, 0.0F, 0.0F);
        addBox(rearBody, "rear_lobe_6", 408, 211,
                -20.0F, -31.0F, -33.0F, 25.0F, 57.0F, 29.0F,
                -32.0F, -8.0F, 23.0F, 0.0F, 0.0F, 0.0F);
        addBox(rearBody, "rear_lobe_7", 160, 208,
                -13.0F, -38.0F, -14.0F, 25.0F, 49.0F, 25.0F,
                11.0F, -76.0F, 3.0F, 0.0F, 0.0F, 0.0F);
        addBox(rearBody, "rear_lobe_8", 408, 211,
                -13.0F, -21.0F, -9.0F, 24.0F, 54.0F, 30.0F,
                -1.0F, -94.0F, 1.0F, 0.81681406F, -2.19911486F, 0.0F);
        addBox(rearBody, "rear_lobe_9", 40, 204,
                -13.0F, -21.0F, -10.0F, 18.0F, 51.0F, 27.0F,
                33.0F, -24.0F, 19.0F, 0.0F, 0.0F, 0.0F);

        PartDefinition frontCap = addBox(body, "front_cap", 200, 0,
                -11.0F, -15.0F, -5.0F, 22.0F, 25.0F, 30.0F,
                -9.0F, -44.0F, -38.0F, -1.1030481F, 0.0F, 0.0F);
        addBox(body, "upper_mass_1", 177, 55,
                -15.0F, 0.0F, 0.0F, 36.0F, 32.0F, 23.0F,
                0.0F, -71.0F, -29.0F, 0.0F, 0.0F, 0.0F);
        addBox(body, "side_mass_1", 369, 45,
                -7.0F, -15.0F, -11.0F, 32.0F, 43.0F, 19.0F,
                32.0F, 6.0F, 4.0F, 0.08726646F, Mth.HALF_PI, 0.0F);
        addBox(body, "upper_mass_2", 0, 75,
                0.0F, -8.0F, 0.0F, 39.0F, 39.0F, 27.0F,
                -28.0F, -76.0F, -14.0F, 0.0F, 0.0F, 0.0F);
        addBox(body, "side_mass_2", 471, 47,
                -7.0F, -21.0F, -4.0F, 22.0F, 62.0F, 16.0F,
                -36.0F, -29.0F, -4.0F, -0.08726646F, Mth.HALF_PI, 0.0F);
        addBox(body, "upper_mass_3", 272, 87,
                -15.0F, 0.0F, 0.0F, 36.0F, 26.0F, 23.0F,
                7.0F, -69.0F, 4.0F, 0.0F, 1.5079645F, 0.0F);
        addBox(body, "upper_mass_4", 390, 107,
                0.0F, -8.0F, 0.0F, 21.0F, 39.0F, 21.0F,
                -4.0F, -97.0F, -22.0F, 0.0F, 0.0F, 0.0F);
        PartDefinition upperCap = addBox(body, "upper_cap", 304, 0,
                -11.0F, -15.0F, -16.0F, 22.0F, 31.0F, 20.0F,
                10.0F, -15.0F, 49.0F, -0.27925268F, -0.13665928F, 3.0960395F);
        PartDefinition sideCap = addBox(body, "side_cap", 388, 0,
                -11.0F, -15.0F, -11.0F, 22.0F, 27.0F, 18.0F,
                19.0F, 50.0F, -25.0F, 0.4118977F, -2.321986F, 3.1241393F);
        PartDefinition lowerCap = addBox(body, "lower_cap", 468, 0,
                -11.0F, -15.0F, -5.0F, 22.0F, 23.0F, 24.0F,
                -36.0F, 25.0F, -10.0F, -Mth.PI / 9.0F, Mth.HALF_PI, 0.0F);

        addTendril(frontCap, "front", -6.0F, -5.0F, -4.0F, -2.6179938F,
                new int[][]{{150, 0}, {188, 0}, {536, 0}, {150, 19}, {286, 51}, {274, 0}});
        addTendril(upperCap, "upper", 6.0F, -5.0F, 4.0F, Mth.PI / 6.0F,
                new int[][]{{330, 51}, {105, 75}, {128, 78}, {209, 110}, {529, 107}, {234, 114}});
        addTendril(sideCap, "side_left", -6.0F, -5.0F, 4.0F, -Mth.PI / 6.0F,
                new int[][]{{272, 184}, {100, 185}, {470, 185}, {470, 185}, {304, 186}, {272, 184}});
        addTendril(sideCap, "side_right", 6.0F, -5.0F, 4.0F, Mth.PI / 6.0F,
                new int[][]{{66, 141}, {90, 146}, {528, 154}, {450, 171}, {304, 186}, {272, 184}});
        addTendril(lowerCap, "lower", 6.0F, -5.0F, -4.0F, 2.6179938F,
                new int[][]{{272, 184}, {100, 185}, {470, 185}, {470, 185}, {304, 186}, {272, 184}});
        return LayerDefinition.create(mesh, 580, 580);
    }

    private static PartDefinition addBox(PartDefinition parent, String name, int textureX, int textureY,
                                         float boxX, float boxY, float boxZ,
                                         float width, float height, float depth,
                                         float pivotX, float pivotY, float pivotZ,
                                         float xRot, float yRot, float zRot) {
        return parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(textureX, textureY)
                        .addBox(boxX, boxY, boxZ, width, height, depth),
                PartPose.offsetAndRotation(pivotX, pivotY, pivotZ, xRot, yRot, zRot));
    }

    private static void addTendril(PartDefinition parent, String name, float x, float y, float z,
                                    float baseYaw, int[][] textures) {
        int[] sizes = {4, 6, 4, 6, 4, 2};
        int[] lengths = {15, 14, 17, 15, 18, 17};
        int[] jointOffsets = {13, 13, 11, 14, 12};
        float[] bends = {-0.5061455F, 0.68765974F, 0.61784655F,
                -0.82030475F, -0.57595867F, -0.55850536F};
        PartDefinition joint = parent.addOrReplaceChild(name + "_joint_0", CubeListBuilder.create(),
                PartPose.offset(x, y, z));
        for (int index = 0; index < sizes.length; index++) {
            float halfSize = sizes[index] / 2.0F;
            PartDefinition segment = joint.addOrReplaceChild(name + "_segment_" + index,
                    CubeListBuilder.create().texOffs(textures[index][0], textures[index][1])
                            .addBox(-halfSize, -halfSize, index == 0 ? 0.0F : -1.0F,
                                    sizes[index], sizes[index], lengths[index]),
                    PartPose.offsetAndRotation(0.0F, 0.0F, index == 0 ? 0.0F : 1.0F,
                            bends[index], index == 0 ? baseYaw : 0.0F, 0.0F));
            if (index < jointOffsets.length) {
                joint = segment.addOrReplaceChild(name + "_joint_" + (index + 1),
                        CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, jointOffsets[index]));
            }
        }
    }

    private static ModelPart[] findTendril(ModelPart parent, String name) {
        ModelPart[] joints = new ModelPart[6];
        ModelPart joint = parent.getChild(name + "_joint_0");
        for (int index = 0; index < joints.length; index++) {
            joints[index] = joint;
            if (index + 1 < joints.length) {
                joint = joint.getChild(name + "_segment_" + index)
                        .getChild(name + "_joint_" + (index + 1));
            }
        }
        return joints;
    }

    @Override
    public void render(MeteorEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        animate(age);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float scaleXZ = entity.isMainMeteor() ? MAIN_SCALE_XZ : FRAGMENT_SCALE_XZ;
        float scaleY = entity.isMainMeteor() ? MAIN_SCALE_Y : FRAGMENT_SCALE_Y;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(-scaleXZ, -scaleY, scaleXZ);
        poseStack.translate(0.0D, -1.501D, 0.0D);
        body.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void animate(float age) {
        float first = Mth.cos(age * 0.21109599F) * 0.5164299F;
        float second = Mth.cos(age * 0.10975879F) * 0.722022F;
        float third = Mth.cos(age * 0.15110986F) * 0.61975884F;
        animateTendril(0, -second, second, -first, third, second, -third);
        animateTendril(1, first, -second, -third, second, -first, second);
        animateTendril(2, first, -second, first, -third, first, -second);
        animateTendril(3, -third, -second, first, -third, second, second);
        animateTendril(4, third, -first, third, -second, -first, second);

        float bodyFirst = Mth.cos(age * 0.15986F) * 0.18429872F;
        float bodySecond = Mth.cos(age * 0.186F) * 0.249872F;
        float bodyThird = Mth.cos(age * 0.13986F) * 0.218872F;
        float bodyFourth = Mth.cos(age * 0.143096F) * 0.1429872F;
        float bodyFifth = Mth.cos(age * 0.119758785F) * 0.20872F;
        float bodySixth = Mth.cos(age * 0.13986F) * 0.21975887F;
        lowerBody.zRot = bodyFirst * 0.5F;
        lobes[0].zRot = bodyThird;
        lobes[1].zRot = bodySecond;
        lobes[2].zRot = bodySecond;
        lobes[3].zRot = bodySixth;
        lobes[4].zRot = bodySixth;
        lobes[5].zRot = bodyFifth;
        lobes[6].zRot = bodyFourth;
        lobes[7].zRot = bodyFifth;
        lobes[8].zRot = bodyThird;
    }

    private void animateTendril(int group, float first, float second, float third,
                                float fourth, float fifth, float sixth) {
        ModelPart[] joints = tendrils.get(group);
        joints[0].xRot = first;
        joints[1].xRot = second;
        joints[2].xRot = third;
        joints[3].xRot = fourth;
        joints[4].xRot = fifth;
        joints[5].xRot = sixth;
    }

    @Override
    public boolean shouldRender(MeteorEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity entity) {
        return TEXTURE;
    }
}
