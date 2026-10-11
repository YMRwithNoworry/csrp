package alku.csrp.world;

import alku.csrp.Csrp;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/**
 * Compatibility hook retained for worlds that store an SRP difficulty selection.
 * SRP 1.10.9 uses that selection for evolution progress, not mob attribute multipliers.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SrpDifficultyEvents {
    private SrpDifficultyEvents() {
    }

    @SubscribeEvent
    public static void applyDifficulty(EntityJoinLevelEvent event) {
        // The original mod does not scale parasite attributes from the world difficulty choice.
    }

    public static void refreshDifficulty(ServerLevel level) {
        // Kept as a no-op API for the in-game difficulty command.
    }
}
