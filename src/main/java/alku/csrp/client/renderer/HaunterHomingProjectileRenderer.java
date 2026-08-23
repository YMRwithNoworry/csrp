package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.HaunterHomingProjectileEntity;
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
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renderer for the original Pheon homing orb's rotating three-plane model. */
public final class HaunterHomingProjectileRenderer extends EntityRenderer<HaunterHomingProjectileEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(Csrp.MODID, "haunter_homing_projectile"), "main");
    private static final ResourceLocation TEXTURE = new ResourceLocation(Csrp.MODID,
            "textures/entity/projectile/projectileh.png");

    private final ModelPart body;

    public HaunterHomingProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        body = context.bakeLayer(LAYER).getChild("body");
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
    public void render(HaunterHomingProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        body.setRotation(pitch * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, 0.0F);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.15D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(age * 0.1F) * 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(age * 0.1F) * 180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.15F) * 360.0F));
        poseStack.scale(-0.5F, -0.5F, 0.5F);
        body.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        body.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)), packedLight,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(HaunterHomingProjectileEntity entity) {
        return TEXTURE;
    }
}
