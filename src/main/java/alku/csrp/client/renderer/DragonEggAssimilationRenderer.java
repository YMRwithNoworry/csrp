package alku.csrp.client.renderer;

import alku.csrp.entity.DragonEggAssimilationEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

public final class DragonEggAssimilationRenderer extends EntityRenderer<DragonEggAssimilationEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public DragonEggAssimilationRenderer(EntityRendererProvider.Context context) {
        super(context);
        blockRenderer = context.getBlockRenderDispatcher();
        shadowRadius = 0.5F;
    }

    @Override
    public void render(DragonEggAssimilationEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float ticks = entity.getAnimationTicks() + partialTick;
        float progress = Mth.clamp(ticks / DragonEggAssimilationEntity.ANIMATION_DURATION, 0.0F, 1.0F);
        float shake = progress * progress * 0.12F;
        float lift = Mth.sin(progress * Mth.PI) * 0.8F;
        float scale = 1.0F + Mth.sin(progress * Mth.PI) * 0.35F;

        poseStack.pushPose();
        poseStack.translate(Mth.sin(ticks * 2.7F) * shake, lift, Mth.cos(ticks * 3.1F) * shake);
        poseStack.translate(0.0F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(ticks * progress * 4.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(ticks * 1.7F) * progress * 8.0F));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        blockRenderer.renderSingleBlock(Blocks.DRAGON_EGG.defaultBlockState(), poseStack, buffer,
                packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DragonEggAssimilationEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
