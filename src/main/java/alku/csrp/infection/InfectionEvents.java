package alku.csrp.infection;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Connects parasite attacks and terminal COTH stages to infection progression. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class InfectionEvents {
    private static final float PARASITE_HIT_INFECTION_CHANCE = 0.35F;

    private InfectionEvents() {
    }

    @SubscribeEvent
    public static void infectFromParasiteHit(LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0.0F || event.getEntity().level().isClientSide
                || event.getEntity() instanceof Parasite) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Parasite && event.getEntity().getRandom().nextFloat()
                < PARASITE_HIT_INFECTION_CHANCE) {
            InfectionMechanics.applyCoth(event.getEntity(), attacker);
        }
    }

    @SubscribeEvent
    public static void convertTerminalCothHost(LivingDeathEvent event) {
        LivingEntity host = event.getEntity();
        if (host.level().isClientSide) {
            return;
        }
        MobEffectInstance coth = host.getEffect(ModMobEffects.COTH);
        if (coth != null && coth.getAmplifier() >= InfectionMechanics.COTH_MAX_AMPLIFIER
                && InfectionMechanics.convertInfectedHost(host)) {
            event.setCanceled(true);
        }
    }
}
