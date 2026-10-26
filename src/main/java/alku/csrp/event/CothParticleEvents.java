package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.infection.InfectionMechanics;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** 逐 tick 处理原版 COTH 转换准备粒子；扩散和转换仍由效果脉冲限频。 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class CothParticleEvents {
    private CothParticleEvents() {
    }

    @SubscribeEvent
    public static void onLivingEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) {
            InfectionMechanics.tickCothParticles(living);
        }
    }
}
