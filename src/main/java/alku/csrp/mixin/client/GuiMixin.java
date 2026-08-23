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
    // Forge's production runtime exposes the official Gui.render method as m_280421_.
    // Keep both names so the mixin also works in an unremapped development runtime.
    @Inject(method = {"render", "m_280421_"}, at = @At("HEAD"), require = 0)
    private void csrp$beginGuiDistortion(GuiGraphics graphics, float deltaTracker,
            CallbackInfo callback) {
        DerivedTextDistortion.beginRenderScope();
    }

    @Inject(method = {"render", "m_280421_"}, at = @At("RETURN"), require = 0)
    private void csrp$endGuiDistortion(GuiGraphics graphics, float deltaTracker,
            CallbackInfo callback) {
        DerivedTextDistortion.endRenderScope();
    }
}
