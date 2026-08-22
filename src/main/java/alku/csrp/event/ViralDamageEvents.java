package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class ViralDamageEvents {
    private ViralDamageEvents() {
    }

    @SubscribeEvent
    public static void amplifyIncomingDamage(LivingAttackEvent event) {
        MobEffectInstance viral = event.getEntity().getEffect(ModMobEffects.VIRAL.get());
        if (viral == null || event.getAmount() <= 0.0F) {
            return;
        }

        float multiplier = 1.0F + 0.5F * (viral.getAmplifier() + 1);
        event.setAmount(event.getAmount() * multiplier);
    }
}
