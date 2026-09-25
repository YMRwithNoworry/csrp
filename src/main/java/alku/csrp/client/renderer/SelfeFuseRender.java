package alku.csrp.client.renderer;

import alku.csrp.entity.SelfeFuseOwner;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

/**
 * Legacy {@code preRenderCallback} swell driven by the SELFE self-destruct fuse.
 *
 * <p>The original renderers all ran the same formula ({@code RenderEmanaAdapted.preRenderCallback}
 * and friends): the fuse intensity feeds a sine pulse and two exponent-smoothed scale factors.
 */
final class SelfeFuseRender {
    private SelfeFuseRender() {
    }

    /** Applies the original f1/f2/f3 scale while a fuse is burning; a no-op otherwise. */
    static void applySwelling(SelfeFuseOwner owner, PoseStack poseStack, float partialTick) {
        float swell = owner.getSelfeFlashIntensity(partialTick);
        if (swell <= 0.0F) {
            return;
        }
        float pulse = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
        swell = Mth.clamp(swell, 0.0F, 1.0F);
        swell *= swell;
        swell *= swell;
        float horizontalScale = (1.0F + swell * 0.4F) * pulse;
        float verticalScale = (1.0F + swell * 0.1F) / pulse;
        poseStack.scale(horizontalScale, verticalScale, horizontalScale);
    }
}
