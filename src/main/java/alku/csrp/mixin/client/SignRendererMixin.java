package alku.csrp.mixin.client;

import alku.csrp.client.DerivedTextDistortion;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
