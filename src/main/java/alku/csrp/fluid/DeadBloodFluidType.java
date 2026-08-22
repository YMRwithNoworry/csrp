package alku.csrp.fluid;

import alku.csrp.Csrp;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

public final class DeadBloodFluidType extends FluidType {
    public DeadBloodFluidType(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return new ResourceLocation(Csrp.MODID, "block/dead_blood_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return new ResourceLocation(Csrp.MODID, "block/dead_blood_flow");
            }

            @Override
            public int getTintColor() {
                return 0xFF7A1220;
            }
        });
    }
}
