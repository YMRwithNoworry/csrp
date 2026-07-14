package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Csrp.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BUGLIN_GROWL = register("lodo.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUGLIN_HURT = register("lodo.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUGLIN_DEATH = register("lodo.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUGLIN_GROW = register("lodo.mudo");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUGLIN_EMERGE = register("lodo.emerge");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
