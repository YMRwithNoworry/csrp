package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.RemainEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renders the original blood planes directly, without any block model or occupied block cell. */
public final class RemainRenderer extends EntityRenderer<RemainEntity> {
    private static final String[] FAMILIES = {"sim", "pri", "ada", "pure", "fer", "mar"};
    private static final String[] VARIANTS = {"flat", "small", "big"};
    private static final ResourceLocation[] TEXTURES = textures();

    public RemainRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    private static ResourceLocation[] textures() {
        ResourceLocation[] textures = new ResourceLocation[19];
        for (int family = 0; family < FAMILIES.length; family++) {
            for (int variant = 0; variant < VARIANTS.length; variant++) {
                textures[family * 3 + variant] = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                        "textures/blocks/parasitegore_" + FAMILIES[family] + "_" + VARIANTS[variant] + ".png");
            }
        }
        textures[RemainEntity.INFESTED_APPEARANCE] = ResourceLocation.fromNamespaceAndPath(
                Csrp.MODID, "textures/blocks/infestremain.png");
        return textures;
    }

    @Override
    public void render(RemainEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        if (entity.isInvisible()) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        PoseStack.Pose pose = poseStack.last();
        float u = entity.isInfestedResidue() ? 0.0F : 0.5F;
        float v = entity.isInfestedResidue() ? 1.0F : 0.5F;
        vertex(vertices, pose, -0.5F, 0.00625F, -0.5F, u, 0.0F, packedLight, 0, 1, 0);
        vertex(vertices, pose, -0.5F, 0.00625F, 0.5F, u, v, packedLight, 0, 1, 0);
        vertex(vertices, pose, 0.5F, 0.00625F, 0.5F, 1.0F, v, packedLight, 0, 1, 0);
        vertex(vertices, pose, 0.5F, 0.00625F, -0.5F, 1.0F, 0.0F, packedLight, 0, 1, 0);
        if (!entity.isInfestedResidue() && entity.getAppearance() % 3 != 0) {
            cross(vertices, pose, -0.5F, -0.5F, 0.5F, 0.5F, packedLight, -0.707F, 0.707F);
            cross(vertices, pose, -0.5F, 0.5F, 0.5F, -0.5F, packedLight, 0.707F, 0.707F);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void cross(VertexConsumer vertices, PoseStack.Pose pose,
            float x1, float z1, float x2, float z2, int light, float nx, float nz) {
        vertex(vertices, pose, x1, 0.0F, z1, 0.0F, 0.5F, light, nx, 0, nz);
        vertex(vertices, pose, x2, 0.0F, z2, 0.5F, 0.5F, light, nx, 0, nz);
        vertex(vertices, pose, x2, 1.0F, z2, 0.5F, 0.0F, light, nx, 0, nz);
        vertex(vertices, pose, x1, 1.0F, z1, 0.0F, 0.0F, light, nx, 0, nz);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int light, float nx, float ny, float nz) {
        vertices.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(RemainEntity entity) {
        return TEXTURES[entity.getAppearance()];
    }
}
