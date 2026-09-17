package alku.csrp.mixin.client;

import alku.csrp.client.DerivedTextDistortion;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignRenderer.class)
public abstract class SignRendererMixin {
    @Inject(method = "submitSignText", at = @At("HEAD"))
    private void csrp$beginSignDistortion(CallbackInfo callback) {
        DerivedTextDistortion.beginRenderScope();
    }

    @Inject(method = "submitSignText", at = @At("RETURN"))
    private void csrp$endSignDistortion(CallbackInfo callback) {
        DerivedTextDistortion.endRenderScope();
    }

    /*
     * 26.3 defers text preparation: submitSignText only queues a TextFeatureRenderer.Submit, and
     * Font.prepareText runs later in TextFeatureRenderer, outside this render scope. Distort the
     * submitted line while the scope is still open so the (already wrapped) sequence is what gets
     * prepared later.
     */
    @ModifyArg(
            method = "submitSignText",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitText("
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;FF"
                            + "Lnet/minecraft/util/FormattedCharSequence;Z"
                            + "Lnet/minecraft/client/gui/Font$DisplayMode;IIII)V"),
            index = 3)
    private FormattedCharSequence csrp$distortSignLine(FormattedCharSequence line) {
        return DerivedTextDistortion.distort(line);
    }
}
