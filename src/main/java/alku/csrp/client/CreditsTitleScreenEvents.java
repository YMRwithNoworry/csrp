package alku.csrp.client;

import alku.csrp.Csrp;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Client credits entry point retained without the unavailable LowDragMC UI dependency. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class CreditsTitleScreenEvents {
    private CreditsTitleScreenEvents() {
    }
}
