package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModJukeboxSongs {
    public static final DeferredRegister<JukeboxSong> JUKEBOX_SONGS =
            DeferredRegister.create(Registries.JUKEBOX_SONG, Csrp.MODID);

    public static final ResourceKey<JukeboxSong> DISC_THREE_KEY = ResourceKey.create(
            Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "discthree"));

    public static final DeferredHolder<JukeboxSong, JukeboxSong> DISC_THREE = JUKEBOX_SONGS.register("discthree",
            () -> new JukeboxSong(discThreeSound(), Component.translatable("jukebox_song.csrp.discthree"), 240.0F, 3));

    private static Holder<SoundEvent> discThreeSound() {
        return BuiltInRegistries.SOUND_EVENT.getHolderOrThrow(ResourceKey.create(
                BuiltInRegistries.SOUND_EVENT.key(),
                ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "srparasites.discthree")));
    }

    private ModJukeboxSongs() {
    }
}
