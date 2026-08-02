package alku.csrp.celestial.client;

import alku.csrp.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class DarkDaysRumbleSound extends AbstractTickableSoundInstance {
    public DarkDaysRumbleSound() {
        super(ModSounds.DARK_DAYS_RUMBLE.get(), SoundSource.AMBIENT, RandomSource.create());
        looping = true;
        relative = true;
        attenuation = SoundInstance.Attenuation.NONE;
        volume = 1.0F;
        pitch = 1.0F;
    }

    @Override
    public void tick() {
        if (!CelestialClientState.isActive("dark_days")) stop();
    }
}
