package alku.csrp.client.sound;

import alku.csrp.Csrp;
import alku.csrp.config.GeneralConfig;
import alku.csrp.network.RequestEvolutionPhasePayload;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.ParasiteBiomeDecorator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Port of the client half of the 1.10.9 OST scheduler — {@code SRPEventHandlerBus#soundTwo} /
 * {@code soundThree} / {@code getMusicPhase} / {@code resetSouncTicker} plus the two packets that feed
 * it ({@code SRPPacketRequestEvoPhaseClient} every 20 ticks, {@code SRPPacketUpdateEvoPhaseClient} and
 * {@code SRPPacketMusicTrackCancelUpdateClientEvoPhase} as answers).
 *
 * <p>Every tick the scheduler counts {@code musicTimer} down; when it expires it stops the vanilla
 * MUSIC track and plays the parasite OST for the player's situation — {@code srparasites.biome} inside
 * a parasite biome, otherwise {@code srparasites.phaseone..phaseten} for the current evolution phase —
 * then rolls a fresh delay between {@code musicMin} and {@code musicMax}.  Vanilla background music is
 * kept out while a parasite track should be playing, and the scent track
 * ({@link ScentMusicSound}) takes priority over the phase music.</p>
 *
 * <p>26.3 notes: {@code SoundHandler#stop("", SoundCategory.MUSIC)} is
 * {@code SoundManager#stop(null, SoundSource.MUSIC)}; {@code PositionedSoundRecord.func_184370_a} (a
 * "replace the current music" record) is {@link SimpleSoundInstance#forMusic}; and the original's
 * "queue the vanilla track and remove it next tick" dance is replaced by refusing the sound outright in
 * {@link PlaySoundEvent}, which is the same audible result.</p>
 *
 * <p>Faithful notes: 1.10.9 gated the whole scheduler behind {@code SRPConfigWorld.originActivated}
 * (default {@code true}) and skipped it while {@code clientVector <= 0}; this port has no
 * {@code originActivated} toggle, so the original's default branch is always taken.  {@code clientScent}
 * is driven by the port's entity-based {@link ScentMusicSound} instead of the original's scent packets.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class ClientPhaseMusicScheduler {
    /** {@code SRPMusic.EVPHASE_1_MUSIC..EVPHASE_10_MUSIC}. */
    private static final String[] PHASE_MUSIC = {
            "srparasites.phaseone", "srparasites.phasetwo", "srparasites.phasethree",
            "srparasites.phasefour", "srparasites.phasefive", "srparasites.phasesix",
            "srparasites.phaseseven", "srparasites.phaseeight", "srparasites.phasenine",
            "srparasites.phaseten"
    };
    /** {@code SRPMusic.BIOME_MUSIC}. */
    private static final String BIOME_MUSIC = "srparasites.biome";
    /** {@code SRPEventHandlerBus.musicTimer} starts at 1000 ticks. */
    private static final int INITIAL_MUSIC_TIMER = 1000;
    /** {@code ClientProxy#playMovingSound case 102}: {@code clientScent = musicTimer = 5000}. */
    private static final int SCENT_MUSIC_TICKS = 5000;

    private static byte clientCurrentEvoPhase;
    private static int clientScent;
    private static int clientVector;
    private static int musicTimer = INITIAL_MUSIC_TIMER;
    private static int lockedMusic;
    private static final List<SoundInstance> MUSIC_TO_REMOVE = new ArrayList<>();

    private ClientPhaseMusicScheduler() {
    }

    /** {@code SRPPacketUpdateEvoPhaseClient.Handler}. */
    public static void applyServerState(int phase, boolean vector) {
        if (vector) {
            if (clientVector <= 0) {
                stopMusic();
            }
            clientVector = 200;
        }
        clientCurrentEvoPhase = (byte) phase;
    }

    /** {@code SRPPacketMusicTrackCancelUpdateClientEvoPhase.Handler}. */
    public static void cancelMusicTrack(int phase) {
        stopMusic();
        resetSoundTicker(200);
        clientCurrentEvoPhase = (byte) phase;
    }

    /** {@code ClientProxy#playMovingSound case 102}: the scent track takes over the MUSIC channel. */
    public static void scentMusicStarted() {
        musicTimer = SCENT_MUSIC_TICKS;
        clientScent = SCENT_MUSIC_TICKS;
    }

    /** {@code SRPEventHandlerBus#soundThree} — the client {@code PlayerTickEvent}. */
    @SubscribeEvent
    public static void onClientTick(PlayerTickEvent.Pre event) {
        if (!GeneralConfig.musicEnabled() || !(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            ClientPacketDistributor.sendToServer(new RequestEvolutionPhasePayload());
        }

        int previous = clientVector;
        if (clientVector > -10) {
            clientVector--;
        }
        if (clientVector <= 0) {
            if (previous > 0) {
                stopMusic();
            }
            return;
        }

        dropQueuedVanillaMusic();
        if (musicTimer >= 0) {
            musicTimer--;
        }
        if (clientScent >= 0) {
            clientScent--;
        }
        if (clientScent == -1) {
            stopMusic();
            clientScent = -10;
        }

        if (musicTimer <= 0 && MUSIC_TO_REMOVE.isEmpty() && clientCurrentEvoPhase > 0) {
            resetSoundTicker(0);
            SoundEvent music = musicFor(player);
            if (music == null) {
                return;
            }
            if (clientScent <= 0) {
                stopMusic();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forMusic(music));
            }
        } else {
            resetSoundTicker(0);
        }
    }

    /** {@code SRPEventHandlerBus#soundTwo} — keep vanilla music out while the OST should play. */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!GeneralConfig.musicEnabled()) {
            return;
        }
        SoundInstance sound = event.getSound();
        if (sound == null || sound.getSource() != SoundSource.MUSIC || isParasiteMusic(sound)) {
            return;
        }
        if (clientCurrentEvoPhase <= 0 && clientScent <= 0) {
            return;
        }
        // `if (SRPConfigWorld.originActivated && clientScent <= 0 && clientVector <= 0) return;`
        if (clientScent <= 0 && clientVector <= 0) {
            return;
        }
        event.setSound(null);
    }

    /** {@code SRPEventHandlerBus#resetSouncTicker}. */
    static void resetSoundTicker(int ticks) {
        if (ticks > 0) {
            if (musicTimer >= ticks) {
                musicTimer = ticks;
            }
            return;
        }
        int min = GeneralConfig.musicPhaseMin();
        int max = GeneralConfig.musicPhaseMax();
        int span = max - min + 1;
        if (span <= 0) {
            // The original computed an unused local here and then fell through to the same roll.
            span = 1;
        }
        int delay = Minecraft.getInstance().player == null
                ? min
                : min + Minecraft.getInstance().player.getRandom().nextInt(span);
        if (musicTimer >= delay || musicTimer <= 0) {
            musicTimer = delay;
        }
    }

    /** {@code SRPEventHandlerBus#getMusicPhase}. */
    private static SoundEvent musicFor(LocalPlayer player) {
        boolean parasiteBiome =
                ParasiteBiomeDecorator.parasiteBiomeIndex(player.level().getBiome(player.blockPosition())) != 0;
        if (parasiteBiome) {
            return ModSounds.get(BIOME_MUSIC);
        }
        if (clientCurrentEvoPhase < 1 || clientCurrentEvoPhase > PHASE_MUSIC.length) {
            return null;
        }
        return ModSounds.get(PHASE_MUSIC[clientCurrentEvoPhase - 1]);
    }

    private static void dropQueuedVanillaMusic() {
        if (MUSIC_TO_REMOVE.isEmpty()) {
            return;
        }
        var manager = Minecraft.getInstance().getSoundManager();
        for (int i = 0; i < MUSIC_TO_REMOVE.size(); i++) {
            SoundInstance sound = MUSIC_TO_REMOVE.get(i);
            if (manager.isActive(sound)) {
                manager.stop(sound);
                MUSIC_TO_REMOVE.remove(i);
                i--;
            } else {
                lockedMusic++;
            }
        }
        if (lockedMusic >= 100) {
            MUSIC_TO_REMOVE.clear();
        }
    }

    private static boolean isParasiteMusic(SoundInstance sound) {
        Identifier id = sound.getIdentifier();
        return id != null && Csrp.MODID.equals(id.getNamespace());
    }

    private static void stopMusic() {
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
    }

    /** The phase the scheduler is currently using, for tests and diagnostics. */
    public static int currentPhase() {
        return clientCurrentEvoPhase;
    }
}
