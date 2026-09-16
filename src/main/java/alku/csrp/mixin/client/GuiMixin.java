package alku.csrp.mixin.client;

import alku.csrp.client.DerivedTextDistortion;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void csrp$beginGuiDistortion(DeltaTracker deltaTracker, boolean shouldRenderLevel,
            boolean resourcesLoaded, CallbackInfo callback) {
        DerivedTextDistortion.beginRenderScope();
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void csrp$endGuiDistortion(DeltaTracker deltaTracker, boolean shouldRenderLevel,
            boolean resourcesLoaded, CallbackInfo callback) {
        DerivedTextDistortion.endRenderScope();
    }
}
