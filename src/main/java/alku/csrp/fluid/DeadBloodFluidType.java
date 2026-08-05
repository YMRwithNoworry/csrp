package alku.csrp.fluid;

import alku.csrp.Csrp;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;

public final class DeadBloodFluidType extends FluidType {
    public DeadBloodFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "block/dead_blood_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "block/dead_blood_flow");
            }

            @Override
            public int getTintColor() {
                return 0xFF7A1220;
            }
        });
    }
}
