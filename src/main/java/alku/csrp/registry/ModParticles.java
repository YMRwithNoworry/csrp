package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.particle.SrpColorParticleType;
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
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ASSIMILATION_SPLASH =
            PARTICLES.register("assimilation_splash", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GORE_CLOUD =
            PARTICLES.register("gore_cloud", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIOMASS =
            PARTICLES.register("biomass", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FOG =
            PARTICLES.register("fog", () -> new SimpleParticleType(false));

    // Original SRPEnumParticle entries (out109 client/particle/SRPEnumParticle.java).
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPORE =
            PARTICLES.register("spore", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RHAPPY =
            PARTICLES.register("rhappy", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EEN =
            PARTICLES.register("een", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAGE =
            PARTICLES.register("rage", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WIND =
            PARTICLES.register("wind", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SrpColorParticleType> DOT =
            PARTICLES.register("dot", () -> new SrpColorParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SrpColorParticleType> FLASH =
            PARTICLES.register("flash", () -> new SrpColorParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SrpColorParticleType> BLOOD =
            PARTICLES.register("blood", () -> new SrpColorParticleType(false));
    private ModParticles() {
    }
}
