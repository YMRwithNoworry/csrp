package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/** Keeps parasite sounds on Minecraft's normal 16-block linear attenuation curve. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteSoundAttenuationEvents {
    private static final float MAX_POSITIONAL_VOLUME = 1.0F;

    private ParasiteSoundAttenuationEvents() {
    }

    @SubscribeEvent
    public static void attenuateEntitySound(PlayLevelSoundEvent.AtEntity event) {
        if (event.getEntity() instanceof Parasite) {
            capVolume(event);
        }
    }

    @SubscribeEvent
    public static void attenuateHostileSound(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSource() == SoundSource.HOSTILE && event.getSound() != null
                && event.getSound().value().getLocation().getNamespace().equals(Csrp.MODID)) {
            capVolume(event);
        }
    }

    private static void capVolume(PlayLevelSoundEvent event) {
        event.setNewVolume(Math.min(event.getNewVolume(), MAX_POSITIONAL_VOLUME));
    }
}
