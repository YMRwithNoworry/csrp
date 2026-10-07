package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Port of the client-only GUI half of {@code SRPEventHandlerBus#guiPlayer} (out109 lines 1032-1048).
 *
 * <p>Opening a non-pausing screen while carrying Fear IV+ rolled {@code fearInvChance} and, on
 * success, showed the {@code feargui} message and closed the screen. The original also refused to act
 * for creative players.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class FearGuiEvents {
    private FearGuiEvents() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!Config.fearActive() || event.getNewScreen() == null || event.getNewScreen().isPauseScreen()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.getAbilities().instabuild) {
            return;
        }
        MobEffectInstance fear = player.getEffect(ModMobEffects.FEAR);
        if (fear == null || fear.getAmplifier() < 3) {
            return;
        }
        if (player.getRandom().nextDouble() < Config.fearInvChance()) {
            player.sendOverlayMessage(
                    Component.translatable("message.csrp.feargui").withStyle(ChatFormatting.RED));
            event.setCanceled(true);
        }
    }
}
