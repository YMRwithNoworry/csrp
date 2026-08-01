package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.client.sound.ScentMusicSound;
import alku.csrp.entity.ParasiticScentEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Starts the original Scent music when its invisible controller enters the client level. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class ScentMusicEvents {
    private static ScentMusicSound current;

    private ScentMusicEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide || !(event.getEntity() instanceof ParasiticScentEntity)) {
            return;
        }
        if (current == null || current.isStopped()) {
            current = new ScentMusicSound();
            Minecraft.getInstance().getSoundManager().play(current);
        }
    }
}
