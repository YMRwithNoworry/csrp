package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.PrimitiveVariantEntity;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Port of {@code SRPEventHandlerBus#onLivingDeath} / {@code #onWorldTick} (out109 lines 409-453).
 *
 * <p>When a player is killed by a Reeker carrying the Ricardo variant, the original suppressed the
 * vanilla death message for one tick by flipping the {@code showDeathMessages} game rule, then
 * broadcast its own {@code death.attack.srparasites.ricardo} line. The game rule was restored on the
 * next world tick.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class RicardoDeathEvents {
    /** Original {@code SRPEventHandlerBus.RICARDO_DEATH_RULE_RESTORE}. */
    private static final Map<ResourceKey<Level>, Long> RULE_RESTORE = new HashMap<>();

    private RicardoDeathEvents() {
    }

    @SubscribeEvent
    public static void onRicardoKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(player.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof PrimitiveVariantEntity reeker)
                || !reeker.isRicardoVariant()) {
            return;
        }
        GameRules rules = level.getGameRules();
        if (rules.get(GameRules.SHOW_DEATH_MESSAGES)) {
            rules.set(GameRules.SHOW_DEATH_MESSAGES, false, level.getServer());
            RULE_RESTORE.put(level.dimension(), level.getGameTime() + 1L);
        }
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("death.attack.csrp.ricardo", player.getDisplayName()), false);
    }

    @SubscribeEvent
    public static void restoreDeathMessages(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Long when = RULE_RESTORE.get(level.dimension());
        if (when != null && level.getGameTime() >= when) {
            level.getGameRules().set(GameRules.SHOW_DEATH_MESSAGES, true, level.getServer());
            RULE_RESTORE.remove(level.dimension());
        }
    }
}
