package alku.csrp.celestial.client;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.world.SrpStarType;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class StarWorldShaderEvents {
    private static final Identifier COLD_SHADER = Identifier.fromNamespaceAndPath(
            Csrp.MODID, "star_cold");
    private static final Identifier WARM_SHADER = Identifier.fromNamespaceAndPath(
            Csrp.MODID, "star_warm");

    private static Identifier activeShader;
    private static boolean loadAttempted;

    private StarWorldShaderEvents() {
    }

    @SubscribeEvent
    public static void updateShader(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier wanted = wantedShader(minecraft);
        if (wanted == null) {
            unload(minecraft);
            loadAttempted = false;
            if (minecraft.level == null) {
                StarWorldClientState.clear();
            }
            return;
        }

        // 26.3 post effects are requested through the local player's active list; the
        // shader's former runtime uniforms are now baked into the post-effect config.
        List<Identifier> activeEffects = minecraft.player.getActivePostEffects();
        if (activeShader != null && !activeShader.equals(wanted)) {
            activeEffects.remove(activeShader);
            activeShader = null;
            loadAttempted = false;
        }
        if (activeShader == null) {
            if (loadAttempted || !activeEffects.isEmpty()) {
                return;
            }
            loadAttempted = true;
            activeEffects.add(wanted);
            activeShader = wanted;
        } else if (!activeEffects.contains(activeShader)) {
            activeEffects.add(activeShader);
        }
    }

    private static Identifier wantedShader(Minecraft minecraft) {
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

    private static void unload(Minecraft minecraft) {
        if (activeShader != null && minecraft.player != null) {
            minecraft.player.getActivePostEffects().remove(activeShader);
        }
        activeShader = null;
    }
}
