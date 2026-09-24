package alku.csrp.client.weather;

import alku.csrp.Csrp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 1.10.9 {@code SRPBlizzardClientEvents}.
 *
 * <p>Two responsibilities were ported:</p>
 * <ol>
 *   <li>reset the blizzard client state when the client leaves or swaps a level. 1.12.2 listened to
 *       {@code WorldEvent.Load}/{@code Unload} on the client; 26.3 has no client-side world event
 *       pair, so the same thing is done by watching {@link Minecraft#level} identity each tick
 *       (this also covers overworld -&gt; nether -&gt; overworld swaps, which the original missed).</li>
 *   <li>the full-screen whitish tint. 1.12.2 drew it on {@code RenderGameOverlayEvent.Pre(ALL)};
 *       the 26.3 equivalent is {@link RenderGuiEvent.Pre} with
 *       {@code GuiGraphicsExtractor#fill}, the same channel the project's other tint overlays
 *       ({@code MeteorShakeClient}, {@code BoughClientEvents}) already use.</li>
 * </ol>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class SRPBlizzardClientEvents {
    private static final int TINT_RED = 205;
    private static final int TINT_GREEN = 215;
    private static final int TINT_BLUE = 224;
    private static ClientLevel lastLevel;

    private SRPBlizzardClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            lastLevel = level;
            SRPBlizzardDirectionClient.reset();
        }
    }

    @SubscribeEvent
    public static void onBlizzardOverlay(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float intensity = SRPBlizzardClient.getIntensity(partialTicks);
        if (intensity <= 0.001F) {
            return;
        }
        float alpha = Mth.clamp(intensity * 0.24F, 0.0F, 0.24F);
        int alphaByte = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        int color = alphaByte << 24 | TINT_RED << 16 | TINT_GREEN << 8 | TINT_BLUE;
        event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(),
                event.getGuiGraphics().guiHeight(), color);
    }

    /** Exposed for the world-load reset path and for the verification scripts. */
    public static void reset() {
        lastLevel = null;
        SRPBlizzardDirectionClient.reset();
    }
}
