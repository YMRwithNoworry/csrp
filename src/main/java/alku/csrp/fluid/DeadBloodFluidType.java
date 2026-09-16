package alku.csrp.fluid;

import alku.csrp.Csrp;
import alku.csrp.registry.ModFluids;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.neoforge.fluids.FluidType;

public final class DeadBloodFluidType extends FluidType {
    public DeadBloodFluidType(Properties properties) {
        super(properties);
    }

    /** Registers the original dead blood fluid textures and tint color for the client. */
    @EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void registerFluidModels(RegisterFluidModelsEvent event) {
            event.register(new FluidModel.Unbaked(
                    new Material(Identifier.fromNamespaceAndPath(Csrp.MODID, "block/dead_blood_still")),
                    new Material(Identifier.fromNamespaceAndPath(Csrp.MODID, "block/dead_blood_flow")),
                    null,
                    FluidTintSources.constant(0xFF7A1220)),
                    ModFluids.DEADBLOOD.get(), ModFluids.DEADBLOOD_FLOWING.get());
        }
    }
}
