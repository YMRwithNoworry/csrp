package alku.csrp.event;

import alku.csrp.Csrp;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Displays the project feedback link when a player joins. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class WelcomeMessageEvents {
    private WelcomeMessageEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        player.sendSystemMessage(Component.translatable("message.csrp.feedback")
                .withStyle(ChatFormatting.AQUA));
    }
}
