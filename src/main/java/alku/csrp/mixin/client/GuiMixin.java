package alku.csrp.mixin.client;

import alku.csrp.client.DerivedTextDistortion;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void csrp$beginGuiDistortion(GuiGraphics graphics, float deltaTracker,
            CallbackInfo callback) {
        DerivedTextDistortion.beginRenderScope();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void csrp$endGuiDistortion(GuiGraphics graphics, float deltaTracker,
            CallbackInfo callback) {
        DerivedTextDistortion.endRenderScope();
    }
}
