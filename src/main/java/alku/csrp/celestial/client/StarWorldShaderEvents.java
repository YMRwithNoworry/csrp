package alku.csrp.celestial.client;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.world.SrpStarType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class StarWorldShaderEvents {
    private static final ResourceLocation COLD_SHADER = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "shaders/post/star_cold.json");
    private static final ResourceLocation WARM_SHADER = ResourceLocation.fromNamespaceAndPath(
            Csrp.MODID, "shaders/post/star_warm.json");

    private static PostChain loadedEffect;
    private static ResourceLocation activeShader;
    private static boolean loadAttempted;
    private static long startedAt;
    private static float fade;
    private static float handLight;

    private StarWorldShaderEvents() {
    }

    @SubscribeEvent
    public static void updateShader(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation wanted = wantedShader(minecraft);
        if (wanted == null) {
            unload(minecraft);
            loadAttempted = false;
            if (minecraft.level == null) {
                StarWorldClientState.clear();
            }
            return;
        }

        if (activeShader != null && !activeShader.equals(wanted)) {
            unload(minecraft);
            loadAttempted = false;
        }
        if (loadedEffect == null) {
            if (loadAttempted || minecraft.gameRenderer.currentEffect() != null) {
                return;
            }
            loadAttempted = true;
            minecraft.gameRenderer.loadEffect(wanted);
            loadedEffect = minecraft.gameRenderer.currentEffect();
            if (loadedEffect != null) {
                activeShader = wanted;
                startedAt = System.nanoTime();
                fade = 0.0F;
            }
            return;
        }
        if (minecraft.gameRenderer.currentEffect() != loadedEffect) {
            loadedEffect = null;
            activeShader = null;
            loadAttempted = false;
            return;
        }

        fade = Math.min(1.0F, fade + 0.035F);
        float targetHandLight = holdingLight(minecraft) ? 1.0F : 0.0F;
        handLight += (targetHandLight - handLight) * 0.085F;
        BlockPos eye = BlockPos.containing(minecraft.player.getEyePosition());
        float exposure = minecraft.level.canSeeSky(eye)
                ? minecraft.level.getBrightness(LightLayer.SKY, eye) / 15.0F : 0.0F;
        loadedEffect.setUniform("SRP_Time", (System.nanoTime() - startedAt) / 1_000_000_000.0F);
        loadedEffect.setUniform("SRP_Exposure", Math.clamp(exposure, 0.0F, 1.0F));
        loadedEffect.setUniform("SRP_Fade", fade);
        loadedEffect.setUniform("SRP_HandLight", handLight);
    }

    private static ResourceLocation wantedShader(Minecraft minecraft) {
        if (!WorldConfig.starWorldShadersEnabled() || minecraft.level == null || minecraft.player == null
                || minecraft.level.dimension() != Level.OVERWORLD) {
            return null;
        }
        BlockPos eye = BlockPos.containing(minecraft.player.getEyePosition());
        if (!minecraft.level.canSeeSky(eye)) {
            return null;
        }
        SrpStarType starType = StarWorldClientState.starType();
        if (starType == SrpStarType.COLD && WorldConfig.coldStarShaderEnabled()) {
            return COLD_SHADER;
        }
        if (starType == SrpStarType.WARM && WorldConfig.warmStarShaderEnabled()) {
            return WARM_SHADER;
        }
        return null;
    }

    private static boolean holdingLight(Minecraft minecraft) {
        return lightEmission(minecraft.player.getMainHandItem()) > 0
                || lightEmission(minecraft.player.getOffhandItem()) > 0;
    }

    private static int lightEmission(ItemStack stack) {
        if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.LANTERN)
                || stack.is(Items.SOUL_LANTERN)) {
            return 15;
        }
        if (stack.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock().defaultBlockState().getLightEmission();
        }
        return 0;
    }

    private static void unload(Minecraft minecraft) {
        if (loadedEffect != null && minecraft.gameRenderer.currentEffect() == loadedEffect) {
            minecraft.gameRenderer.shutdownEffect();
        }
        loadedEffect = null;
        activeShader = null;
        fade = 0.0F;
        handLight = 0.0F;
    }
}
