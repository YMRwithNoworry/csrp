package alku.csrp.client.sound;

import alku.csrp.entity.ParasiticScentEntity;
import alku.csrp.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Streams the original Scent music while at least one Scent is tracked client-side. */
public final class ScentMusicSound extends AbstractTickableSoundInstance {
    public ScentMusicSound() {
        super(ModSounds.SCENT_MUSIC.get(), SoundSource.MUSIC, RandomSource.create());
        looping = true;
        relative = true;
        attenuation = SoundInstance.Attenuation.NONE;
        volume = 1.0F;
        pitch = 1.0F;
    }

    @Override
    public void tick() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            stop();
            return;
        }
        for (var entity : level.entitiesForRendering()) {
            if (entity instanceof ParasiticScentEntity && entity.isAlive()) {
                return;
            }
        }
        stop();
    }
}
