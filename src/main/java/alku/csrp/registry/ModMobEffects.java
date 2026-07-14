package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.effect.CothMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Csrp.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> COTH =
            EFFECTS.register("coth", CothMobEffect::new);

    private ModMobEffects() {
    }
}
