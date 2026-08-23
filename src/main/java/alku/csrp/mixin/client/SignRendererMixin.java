package alku.csrp.mixin.client;

import alku.csrp.client.DerivedTextDistortion;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignRenderer.class)
public abstract class SignRendererMixin {
    @Inject(method = {"renderSignText", "m_278841_"}, at = @At("HEAD"), require = 0)
    private void csrp$beginSignDistortion(CallbackInfo callback) {
        DerivedTextDistortion.beginRenderScope();
    }

    @Inject(method = {"renderSignText", "m_278841_"}, at = @At("RETURN"), require = 0)
    private void csrp$endSignDistortion(CallbackInfo callback) {
        DerivedTextDistortion.endRenderScope();
    }
}
