package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.ParasiteRemainsEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class ParasiteRemainsRenderer extends EntityRenderer<ParasiteRemainsEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/movingflesh.png");
    private static final Map<ResourceLocation, ResourceLocation> TEXTURE_CACHE = new HashMap<>();

    public ParasiteRemainsRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.14F;
    }

    @Override
    public void render(ParasiteRemainsEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        int variant = entity.variant();
        float age = entity.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(age * (8.0F + variant * 1.7F)));
        poseStack.mulPose(Axis.YP.rotationDegrees(age * (11.0F + variant * 1.3F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * (6.0F + variant * 1.1F)));
        float xScale = 0.24F + (variant % 3) * 0.045F;
        float yScale = 0.20F + ((variant + 1) % 3) * 0.05F;
        float zScale = 0.23F + ((variant + 2) % 3) * 0.04F;
        poseStack.scale(xScale, yScale, zScale);
        int shade = 205 + (variant % 4) * 12;
        renderCube(poseStack.last(), bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))),
                packedLight,
                shade, 218 - (variant % 3) * 14, 218 - (variant % 2) * 20);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderCube(PoseStack.Pose pose, VertexConsumer consumer, int light,
                                   int red, int green, int blue) {
        quad(pose, consumer, light, red, green, blue,
                -0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F,
                0.0F, 0.0F, 1.0F);
        quad(pose, consumer, light, red, green, blue,
                0.5F, -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, 0.5F, -0.5F,
                0.0F, 0.0F, -1.0F);
        quad(pose, consumer, light, red, green, blue,
                0.5F, -0.5F, 0.5F, 0.5F, -0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, 0.5F, 0.5F,
                1.0F, 0.0F, 0.0F);
        quad(pose, consumer, light, red, green, blue,
                -0.5F, -0.5F, -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, 0.5F, -0.5F, 0.5F, -0.5F,
                -1.0F, 0.0F, 0.0F);
        quad(pose, consumer, light, red, green, blue,
                -0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, -0.5F, -0.5F, 0.5F, -0.5F,
                0.0F, 1.0F, 0.0F);
        quad(pose, consumer, light, red, green, blue,
                -0.5F, -0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F, -0.5F, 0.5F, -0.5F, -0.5F, 0.5F,
                0.0F, -1.0F, 0.0F);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, int light,
                             int red, int green, int blue,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz) {
        vertex(pose, consumer, light, red, green, blue, x0, y0, z0, 0.0F, 1.0F, nx, ny, nz);
        vertex(pose, consumer, light, red, green, blue, x1, y1, z1, 1.0F, 1.0F, nx, ny, nz);
        vertex(pose, consumer, light, red, green, blue, x2, y2, z2, 1.0F, 0.0F, nx, ny, nz);
        vertex(pose, consumer, light, red, green, blue, x3, y3, z3, 0.0F, 0.0F, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int light,
                               int red, int green, int blue, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(ParasiteRemainsEntity entity) {
        return TEXTURE_CACHE.computeIfAbsent(entity.sourceTypeId(), source -> {
            ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                    "textures/entity/" + source.getPath() + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(candidate).isPresent()
                    ? candidate : TEXTURE;
        });
    }
}
