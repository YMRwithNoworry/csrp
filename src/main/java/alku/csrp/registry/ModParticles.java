package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Csrp.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KIRIN_WARNING =
            PARTICLES.register("kirin_warning", () -> new SimpleParticleType(true));

    private ModParticles() {
    }
}
