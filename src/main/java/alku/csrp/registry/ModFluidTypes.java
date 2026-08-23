package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.fluid.DeadBloodFluidType;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModFluidTypes {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Csrp.MODID);

    public static final RegistryObject<DeadBloodFluidType> DEAD_BLOOD =
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
                            .sound(net.minecraftforge.common.SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(net.minecraftforge.common.SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    private ModFluidTypes() {
    }
}
