package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.network.EscapeRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Port of SRP 1.10.9 {@code client/gui/GuiGameOverEscape} (plus its {@code OpenHook}): while
 * {@link EscapeClientState#offer()} is set, the vanilla death screen gains the escape button that
 * teleports the player out of the death loop.
 *
 * <p>The original subclassed {@code GuiGameOver} to append the button.  26.3's {@code DeathScreen}
 * keeps its exit buttons in a private list, so this hook adds the same button to the screen that is
 * already open instead of replacing it — same button, same position, and the vanilla death text is
 * preserved.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class EscapeDeathScreenEvents {
    /** {@code GuiGameOverEscape.BTN_ESCAPE}. */
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;

    private EscapeDeathScreenEvents() {
    }

    @SubscribeEvent
    public static void addEscapeButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof DeathScreen screen)) {
            return;
        }
        if (!EscapeClientState.offer()) {
            return;
        }
        int min = Math.max(0, WorldConfig.escapeMinDistance());
        int max = Math.max(min, WorldConfig.escapeMaxDistance());
        Component label = Component.translatable("gui.csrp.escape_button")
                .append(String.valueOf(min))
                .append("-")
                .append(String.valueOf(max))
                .append(" ")
                .append(Component.translatable("gui.csrp.escape_button_blocks"));
        Button escape = Button.builder(label, button -> requestEscape())
                .bounds(screen.width / 2 - BUTTON_WIDTH / 2, belowExistingButtons(screen), BUTTON_WIDTH,
                        BUTTON_HEIGHT)
                .build();
        event.addListener(escape);
    }

    /** {@code GuiGameOverEscape.initGui}: six pixels below the lowest vanilla death-screen button. */
    private static int belowExistingButtons(DeathScreen screen) {
        int bottom = 0;
        for (var child : screen.children()) {
            if (child instanceof AbstractWidget widget) {
                bottom = Math.max(bottom, widget.getY() + widget.getHeight());
            }
        }
        return bottom + BUTTON_GAP;
    }

    private static void requestEscape() {
        ClientPacketDistributor.sendToServer(EscapeRequestPayload.INSTANCE);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.respawn();
        }
    }
}
