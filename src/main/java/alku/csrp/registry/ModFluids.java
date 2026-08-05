package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.fluid.DeadBloodFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Csrp.MODID);

    public static final DeferredHolder<Fluid, DeadBloodFluid> DEADBLOOD =
            FLUIDS.register("deadblood", DeadBloodFluid::new);
    public static final DeferredHolder<Fluid, DeadBloodFluid> DEADBLOOD_FLOWING =
            FLUIDS.register("deadblood_flowing", DeadBloodFluid::new);

    private ModFluids() {
    }
}
