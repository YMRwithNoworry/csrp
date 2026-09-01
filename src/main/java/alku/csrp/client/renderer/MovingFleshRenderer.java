package alku.csrp.client.renderer;

import alku.csrp.client.model.PrimitiveParasiteModel;
import alku.csrp.entity.MovingFleshEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * Applies the accumulated merge scale and evolution flash effects to the Moving Flesh model.
 * 根据原模组动画信息实现：
 * - 合并后的渐进式缩放增长
 * - 进化前的快速振动和挤压效果（模拟爆炸前的不稳定状态）
 */
public final class MovingFleshRenderer extends ParasiteGeoRenderer<MovingFleshEntity> {
    public MovingFleshRenderer(EntityRendererProvider.Context context) {
        super(context, new PrimitiveParasiteModel<>("movingflesh"));
        shadowRadius = 0.2F;
    }

    @Override
    protected void scale(MovingFleshEntity entity, PoseStack poseStack, float partialTick) {
        // 基础缩放（合并成长效果）
        float baseScale = entity.getRenderScale(partialTick);

        // 进化闪烁效果（爆炸前的快速振动）
        float flashIntensity = entity.getEvolutionFlashIntensity(partialTick);

        if (flashIntensity > 0.0F) {
            // 原模组公式：f1 = 1.0F + sin(f * 100.0F) * f * 0.01F（快速振动）
            float vibration = 1.0F + Mth.sin(flashIntensity * 100.0F) * flashIntensity * 0.01F;

            // 原模组公式：f2 = (1.0F + f * 0.4F) * f1（XZ膨胀）
            float xzScale = (1.0F + flashIntensity * 0.4F) * vibration;

            // 原模组公式：f3 = (1.0F + f * 0.1F) / f1（Y挤压）
            float yScale = (1.0F + flashIntensity * 0.1F) / vibration;

            poseStack.scale(baseScale * xzScale, baseScale * yScale, baseScale * xzScale);
        } else {
            // 正常缩放
            poseStack.scale(baseScale, baseScale, baseScale);
        }

        super.scale(entity, poseStack, partialTick);
    }
}
