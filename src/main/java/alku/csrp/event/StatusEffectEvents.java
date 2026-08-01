package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.DraconiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Runtime hooks for SRP effects whose behavior crosses entity boundaries. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class StatusEffectEvents {
    private StatusEffectEvents() {
    }

    @SubscribeEvent
    public static void distortedEnlightenmentDamage(LivingIncomingDamageEvent event) {
        var victim = event.getEntity();
        var attacker = event.getSource().getEntity();
        if (attacker == null) {
            return;
        }
        boolean victimDistorted = victim.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT);
        boolean attackerDistorted = attacker instanceof net.minecraft.world.entity.LivingEntity living
                && living.hasEffect(ModMobEffects.DISTORTED_ENLIGHTENMENT);
        if (victimDistorted && attacker instanceof Parasite) {
            event.setAmount(event.getAmount() * (attacker instanceof DraconiteEntity ? 5.0F : 0.8F));
        }
        if (attackerDistorted && victim instanceof Parasite) {
            event.setAmount(event.getAmount() * 0.8F);
        }
    }

    @SubscribeEvent
    public static void restoreGravity(MobEffectEvent.Remove event) {
        if (event.getEffect().value() == ModMobEffects.DISTORTED_ENLIGHTENMENT.get()) {
            event.getEntity().setNoGravity(false);
        }
    }

    @SubscribeEvent
    public static void restoreGravityOnExpiry(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().is(ModMobEffects.DISTORTED_ENLIGHTENMENT)) {
            event.getEntity().setNoGravity(false);
        }
    }
}
