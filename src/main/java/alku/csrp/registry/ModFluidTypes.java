package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.fluid.DeadBloodFluidType;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluidTypes {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Csrp.MODID);

    public static final DeferredHolder<FluidType, DeadBloodFluidType> DEAD_BLOOD =
            FLUID_TYPES.register("deadblood", () -> new DeadBloodFluidType(
                    FluidType.Properties.create()
                            .descriptionId("fluid.csrp.deadblood")
                            .canConvertToSource(false)
                            .density(1200)
                            .viscosity(1500)
                            .temperature(300)
                            .motionScale(0.010D)
                            .canSwim(false)
                            .canDrown(true)
                            .supportsBoating(false)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    private ModFluidTypes() {
    }
}
