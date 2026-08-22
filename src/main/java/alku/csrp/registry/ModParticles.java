package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Csrp.MODID);

    public static final RegistryObject<SimpleParticleType> KIRIN_WARNING =
            PARTICLES.register("kirin_warning", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> ASSIMILATION_SPLASH =
            PARTICLES.register("assimilation_splash", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> GORE_CLOUD =
            PARTICLES.register("gore_cloud", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> BIOMASS =
            PARTICLES.register("biomass", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> FOG =
            PARTICLES.register("fog", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
