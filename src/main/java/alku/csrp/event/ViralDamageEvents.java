package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class ViralDamageEvents {
    private ViralDamageEvents() {
    }

    @SubscribeEvent
    public static void amplifyIncomingDamage(LivingIncomingDamageEvent event) {
        MobEffectInstance viral = event.getEntity().getEffect(ModMobEffects.VIRAL);
        if (viral == null || event.getAmount() <= 0.0F) {
            return;
        }

        float multiplier = 1.0F + 0.5F * (viral.getAmplifier() + 1);
        event.setAmount(event.getAmount() * multiplier);
    }
}
