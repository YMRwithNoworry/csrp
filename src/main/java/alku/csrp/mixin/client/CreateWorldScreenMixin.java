package alku.csrp.mixin.client;

import alku.csrp.client.SrpDifficultyScreenEvents;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Inject(method = {"onCreate", "m_100972_"}, at = @At("HEAD"), require = 0)
    private void csrp$stageDifficulty(CallbackInfo callbackInfo) {
        SrpDifficultyScreenEvents.stageSelection((CreateWorldScreen) (Object) this);
    }
}
