package alku.csrp.client.sound;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The client half of 1.10.9's {@code network/SRPPacketMovingSound} heart branch
 * ({@code ClientProxy#playMovingSound(-1, v)}, {@code ClientProxy.java:119-126}):
 *
 * <pre>
 * Minecraft.getMinecraft().getSoundHandler().playSound(
 *     PositionedSoundRecord.func_194007_a(SRPSounds.BHEART, 1.0F, v * SRPConfigWorld.biomeHeartVol));
 * </pre>
 *
 * <p>{@code PositionedSoundRecord.func_194007_a(sound, pitch, volume)} builds a non-attenuated record
 * at the player, i.e. a one-shot local sound — not the looping {@code MovingSound} the packet's name
 * suggests.  {@code SoundCategory.NEUTRAL} is what that helper defaults to, so the port plays through
 * {@link ClientLevel#playLocalSound} with {@link SoundSource#NEUTRAL} and no distance delay.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class ClientNodeHeart {
    private ClientNodeHeart() {
    }

    /** {@code ClientProxy#playMovingSound(-1, volume)}. */
    public static void play(float volume) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.get("biome.heart"),
                SoundSource.NEUTRAL, volume * WorldConfig.biomeHeartVol(), 1.0F, false);
    }
}
