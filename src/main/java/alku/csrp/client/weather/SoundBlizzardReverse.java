package alku.csrp.client.weather;

import alku.csrp.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 1.10.9 {@code SoundBlizzardReverse}: a weather sound pinned to the player's eyes.
 *
 * <p>1.12.2 extended {@code MovingSound} (removed in the modern client). The 26.3 equivalent is
 * {@link AbstractTickableSoundInstance}, which keeps the same "follow the player and die with
 * them" behaviour through {@link #tick()}. The original also used
 * {@code AttenuationType.NONE} and {@code SoundCategory.WEATHER}; both survive unchanged.</p>
 */
public final class SoundBlizzardReverse extends AbstractTickableSoundInstance {
    public SoundBlizzardReverse() {
        super(ModSounds.get("blizzard_reverse"), SoundSource.WEATHER, RandomSource.create());
        looping = false;
        relative = false;
        attenuation = SoundInstance.Attenuation.NONE;
        volume = 1.0F;
        pitch = 1.0F;
        updatePosition();
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive()) {
            stop();
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        x = player.getX();
        y = player.getEyeY();
        z = player.getZ();
    }
}
