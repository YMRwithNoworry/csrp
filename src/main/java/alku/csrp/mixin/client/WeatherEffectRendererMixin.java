package alku.csrp.mixin.client;

import alku.csrp.client.weather.SRPBlizzardClient;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps vanilla rain and snow from being drawn over the cold-star blizzard. */
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherEffectRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/renderer/state/level/WeatherRenderState;"
                    + "Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void csrp$suppressVanillaPrecipitation(WeatherRenderState renderState, RenderPass renderPass,
            CallbackInfo callback) {
        if (SRPBlizzardClient.shouldSuppressVanillaPrecipitation()) {
            callback.cancel();
        }
    }

    @Inject(
            method = "renderOit(Lnet/minecraft/client/renderer/oit/OitStage;"
                    + "Lnet/minecraft/client/renderer/state/level/WeatherRenderState;"
                    + "Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void csrp$suppressVanillaPrecipitationOit(OitStage stage, WeatherRenderState renderState,
            RenderPass renderPass, CallbackInfo callback) {
        if (SRPBlizzardClient.shouldSuppressVanillaPrecipitation()) {
            callback.cancel();
        }
    }
}
