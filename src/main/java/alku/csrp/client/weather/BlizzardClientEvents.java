package alku.csrp.client.weather;

import com.mojang.blaze3d.shaders.FogShape;
import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Blizzard client hooks: the snow veil drawn in the vanilla weather slot, the clamped snow fog, the
 * whiteout overlay and the client state resets. Port of 1.10.9's {@code SRPBlizzardClientEvents} plus the
 * fog half of {@code MixinEntityRendererBlizzard}.
 *
 * <p>The fog values match the original mixin exactly: {@code far = 72 - intensity * 56},
 * {@code near = far * 0.08}, {@link FogShape#SPHERE}. The distances only apply because
 * {@code ForgeHooksClient.onFogRender} writes them into the shader fog state when the event is cancelled,
 * so the event must be cancelled on every blizzard frame.
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class BlizzardClientEvents {
    private static final float FOG_FAR_BASE = 72.0F;
    private static final float FOG_FAR_RANGE = 56.0F;
    private static final float FOG_NEAR_FACTOR = 0.08F;
    private static final float OVERLAY_ALPHA_FACTOR = 0.24F;
    private static final int OVERLAY_RED = 205;
    private static final int OVERLAY_GREEN = 215;
    private static final int OVERLAY_BLUE = 224;

    private BlizzardClientEvents() {
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        // Runs on both sides; resetting a client-only state machine on the server is harmless.
        BlizzardDirectionClient.reset();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        BlizzardDirectionClient.reset();
    }

    @SubscribeEvent
    public static void renderBlizzard(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        float intensity = BlizzardClient.getIntensity(event.getPartialTick());
        if (intensity <= BlizzardClient.MIN_INTENSITY) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        BlizzardFogRenderer.render(minecraft, event.getPartialTick(), intensity);
        BlizzardRenderer.render(minecraft, event.getCamera().getPosition(), event.getCamera().getYRot(),
                event.getPartialTick(), intensity);
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        float intensity = BlizzardClient.getIntensity((float) event.getPartialTick());
        if (intensity <= BlizzardClient.MIN_INTENSITY) {
            return;
        }
        FogType fogType = event.getType();
        if (fogType == FogType.WATER || fogType == FogType.LAVA || fogType == FogType.POWDER_SNOW) {
            return;
        }
        float far = FOG_FAR_BASE - intensity * FOG_FAR_RANGE;
        event.setFogShape(FogShape.SPHERE);
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(far * FOG_NEAR_FACTOR);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderOverlay(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        float intensity = BlizzardClient.getIntensity(event.getPartialTick());
        if (intensity <= BlizzardClient.MIN_INTENSITY) {
            return;
        }
        float alpha = Mth.clamp(intensity * OVERLAY_ALPHA_FACTOR, 0.0F, OVERLAY_ALPHA_FACTOR);
        int alphaByte = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        int color = alphaByte << 24 | OVERLAY_RED << 16 | OVERLAY_GREEN << 8 | OVERLAY_BLUE;
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
    }
}
