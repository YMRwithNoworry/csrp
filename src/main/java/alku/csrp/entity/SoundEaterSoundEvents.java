package alku.csrp.entity;

import alku.csrp.Csrp;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Port of {@code events/SoundEaterBlockSoundHandler} (out109).
 *
 * <p>Block break and block place events broadcast a noise to every nearby Sound Eater
 * ({@code SimHumanEntity} with the legacy skin 111) so it walks over to investigate.
 * Radii and lifetimes are copied from the original handler.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SoundEaterSoundEvents {
    private SoundEaterSoundEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SimHumanEntity.broadcastSound(level, event.getPos(),
                    SimHumanEntity.BLOCK_BREAK_SOUND_RADIUS, SimHumanEntity.BLOCK_BREAK_SOUND_LIFE_TICKS);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SimHumanEntity.broadcastSound(level, event.getPos(),
                    SimHumanEntity.BLOCK_PLACE_SOUND_RADIUS, SimHumanEntity.BLOCK_PLACE_SOUND_LIFE_TICKS);
        }
    }
}
