package alku.csrp.client.weather;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Drives the {@code csrp:blizzard_reverse} post effect. The effect blacks the screen out while the storm
 * wind flips direction, mirroring the black blend the original wrote straight into its snow vertex colours.
 *
 * <p>{@code PostChain.passes} is private and has no getter in 1.20.1, so the passes are reached
 * reflectively - the same fallback chain {@code StarWorldShaderEvents} already uses.
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class BlizzardShaderEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlizzardShaderEvents.class);
    private static final ResourceLocation EFFECT = new ResourceLocation(
            Csrp.MODID, "shaders/post/blizzard_reverse.json");

    private static PostChain loadedEffect;
    private static boolean loadAttempted;
    private static final Field PASSES_FIELD = findPassesField();

    private BlizzardShaderEvents() {
    }

    @SubscribeEvent
    public static void updateShader(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        float blackBlend = minecraft.level == null || minecraft.player == null
                ? 0.0F : BlizzardDirectionClient.getBlackBlend(minecraft.getFrameTime());
        boolean shouldRender = BlizzardClient.isColdWorld() && blackBlend > 0.001F;

        if (!shouldRender) {
            unload(minecraft);
            loadAttempted = false;
            return;
        }
        if (loadedEffect != null || loadAttempted || minecraft.gameRenderer.currentEffect() != null) {
            if (loadedEffect != null && minecraft.gameRenderer.currentEffect() == loadedEffect) {
                setUniform("SRP_BlackBlend", blackBlend);
            }
            return;
        }

        loadAttempted = true;
        minecraft.gameRenderer.loadEffect(EFFECT);
        loadedEffect = minecraft.gameRenderer.currentEffect();
        if (loadedEffect == null) {
            LOGGER.warn("Unable to load the blizzard reverse post effect");
        }
    }

    private static void unload(Minecraft minecraft) {
        if (loadedEffect != null && minecraft.gameRenderer.currentEffect() == loadedEffect) {
            minecraft.gameRenderer.shutdownEffect();
        }
        loadedEffect = null;
    }

    private static Field findPassesField() {
        // Development runtimes expose official names, obfuscated runtimes keep the SRG name.
        for (String fieldName : new String[] {"passes", "f_110009_", "e"}) {
            try {
                Field field = PostChain.class.getDeclaredField(fieldName);
                if (field.trySetAccessible()) {
                    return field;
                }
            } catch (NoSuchFieldException | RuntimeException ignored) {
                // Try the next mapping form.
            }
        }
        LOGGER.warn("Unable to access PostChain passes; blizzard reverse uniforms will stay at defaults");
        return null;
    }

    private static void setUniform(String name, float value) {
        if (loadedEffect == null || PASSES_FIELD == null) {
            return;
        }
        try {
            List<PostPass> passes = (List<PostPass>) PASSES_FIELD.get(loadedEffect);
            for (PostPass pass : passes) {
                EffectInstance effect = pass.getEffect();
                effect.safeGetUniform(name).set(value);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.debug("Unable to update blizzard shader uniform {}", name, exception);
        }
    }
}
