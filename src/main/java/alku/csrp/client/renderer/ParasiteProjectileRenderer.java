package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.ParasiteProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ParasiteProjectileRenderer extends EntityRenderer<ParasiteProjectileEntity> {
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/scary_orb.png");
    private static final ResourceLocation LENCIA_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/lencia.png");
    private static final ResourceLocation ELVIA_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/elvia.png");
    private static final ResourceLocation NADE_PROJECTILE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/nade.png");
    private static final ResourceLocation SPINEBALL_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/spineball.png");
    private static final ResourceLocation YELLOWEYE_NADE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/monster/nade.png");
    private static final ResourceLocation ALAFHA_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/alafha.png");
    private static final ResourceLocation ANGED_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/anged.png");
    private static final ResourceLocation BIOMASS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/projectile/biomass.png");

    public ParasiteProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ParasiteProjectileEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.isYelloweyeNadeArmed()) {
            renderYelloweyeNade(entity, entityYaw, poseStack, buffer, packedLight);
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        if (!entity.shouldRenderAsBillboard()) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        float halfWidth = entity.getRenderWidth() * 0.5F;
        float halfHeight = entity.getRenderHeight() * 0.5F;
        poseStack.pushPose();
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        vertex(vertices, pose, -halfWidth, -halfHeight, 0.0F, 1.0F, packedLight);
        vertex(vertices, pose, halfWidth, -halfHeight, 1.0F, 1.0F, packedLight);
        vertex(vertices, pose, halfWidth, halfHeight, 1.0F, 0.0F, packedLight);
        vertex(vertices, pose, -halfWidth, halfHeight, 0.0F, 0.0F, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderYelloweyeNade(ParasiteProjectileEntity entity, float entityYaw, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight) {
        float halfWidth = entity.getRenderWidth() * 0.5F;
        float height = entity.getRenderHeight();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(YELLOWEYE_NADE_TEXTURE));

        quad(vertices, pose, -halfWidth, 0.0F, -halfWidth, halfWidth, height, -halfWidth,
                0.25F, 0.5F, 0.5F, 1.0F, 0.0F, 0.0F, -1.0F, packedLight);
        quad(vertices, pose, halfWidth, 0.0F, halfWidth, -halfWidth, height, halfWidth,
                0.75F, 0.5F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, packedLight);
        quadX(vertices, pose, -halfWidth, 0.0F, halfWidth, height,
                0.0F, 0.5F, 0.25F, 1.0F, -1.0F, 0.0F, 0.0F, packedLight);
        quadX(vertices, pose, halfWidth, 0.0F, -halfWidth, height,
                0.5F, 0.5F, 0.75F, 1.0F, 1.0F, 0.0F, 0.0F, packedLight);
        quadY(vertices, pose, height, -halfWidth, halfWidth,
                0.25F, 0.0F, 0.5F, 0.5F, 0.0F, 1.0F, 0.0F, packedLight);
        quadY(vertices, pose, 0.0F, halfWidth, -halfWidth,
                0.5F, 0.0F, 0.75F, 0.5F, 0.0F, -1.0F, 0.0F, packedLight);
        poseStack.popPose();
    }

    private static void quad(VertexConsumer vertices, PoseStack.Pose pose,
                             float x1, float y1, float z, float x2, float y2, float ignoredZ,
                             float u1, float v1, float u2, float v2,
                             float nx, float ny, float nz, int packedLight) {
        cubeVertex(vertices, pose, x1, y1, z, u1, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x2, y1, z, u2, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x2, y2, z, u2, v1, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x1, y2, z, u1, v1, nx, ny, nz, packedLight);
    }

    private static void quadX(VertexConsumer vertices, PoseStack.Pose pose,
                              float x, float y1, float z1, float y2,
                              float u1, float v1, float u2, float v2,
                              float nx, float ny, float nz, int packedLight) {
        cubeVertex(vertices, pose, x, y1, z1, u1, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x, y1, -z1, u2, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x, y2, -z1, u2, v1, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, x, y2, z1, u1, v1, nx, ny, nz, packedLight);
    }

    private static void quadY(VertexConsumer vertices, PoseStack.Pose pose,
                              float y, float first, float second,
                              float u1, float v1, float u2, float v2,
                              float nx, float ny, float nz, int packedLight) {
        cubeVertex(vertices, pose, first, y, first, u1, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, second, y, first, u2, v2, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, second, y, second, u2, v1, nx, ny, nz, packedLight);
        cubeVertex(vertices, pose, first, y, second, u1, v1, nx, ny, nz, packedLight);
    }

    private static void cubeVertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z,
                                   float u, float v, float nx, float ny, float nz, int packedLight) {
        vertices.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y,
                               float u, float v, int packedLight) {
        vertices.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ParasiteProjectileEntity entity) {
        return switch (entity.getMode()) {
            case LENCIA_BALL -> LENCIA_TEXTURE;
            case ELVIA_BALL -> ELVIA_TEXTURE;
            case ELVIA_NADE, ACID -> NADE_PROJECTILE_TEXTURE;
            case YELLOWEYE_SPINE -> SPINEBALL_TEXTURE;
            case YELLOWEYE_NADE -> entity.isYelloweyeNadeArmed()
                    ? YELLOWEYE_NADE_TEXTURE : NADE_PROJECTILE_TEXTURE;
            case ALAFHA_BALL -> ALAFHA_TEXTURE;
            case ANGED_BALL -> ANGED_TEXTURE;
            case BIOMASS_BALL -> BIOMASS_TEXTURE;
            default -> DEFAULT_TEXTURE;
        };
    }
}
