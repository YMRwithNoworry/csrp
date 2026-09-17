package alku.csrp.client.weather;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Loop played while the blizzard wind reverses direction. Port of 1.10.9's {@code SoundBlizzardReverse}
 * ({@code MovingSound} -&gt; {@code AbstractTickableSoundInstance}): no attenuation, full volume, and the
 * position follows the player's eyes every tick.
 */
public final class BlizzardReverseSound extends AbstractTickableSoundInstance {
    private final LocalPlayer player;

    public BlizzardReverseSound(SoundEvent sound, LocalPlayer player) {
        super(sound, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.updatePosition();
    }

    @Override
    public void tick() {
        if (player == null || !player.isAlive() || player.level() == null) {
            stop();
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        this.x = player.getX();
        this.y = player.getEyeY();
        this.z = player.getZ();
    }
}
