package alku.csrp.infection;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.GnatEntity;
import alku.csrp.entity.LiceEntity;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/** Connects parasite attacks and terminal COTH stages to infection progression. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class InfectionEvents {
    private InfectionEvents() {
    }

    @SubscribeEvent
    public static void preventParasiteFriendlyFire(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (InfectionMechanics.isHiddenAssimilated(event.getEntity())
                && (attacker instanceof Parasite || direct instanceof Parasite)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof Parasite) {
            if (attacker instanceof Parasite || direct instanceof Parasite) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void infectFromParasiteHit(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (event.getAmount() <= 0.0F || !target.isAlive() || target.level().isClientSide
                || target instanceof Parasite) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Parasite && !target.hasEffect(ModMobEffects.COTH.get())) {
            double chance = InfectionMechanics.cothSpreadChance(attacker);
            if (target.getRandom().nextDouble() < chance) {
                InfectionMechanics.applyCoth(target, attacker);
            }
        }
        MobEffectInstance coth = target.getEffect(ModMobEffects.COTH.get());
        if (coth != null && coth.getAmplifier() >= InfectionMechanics.COTH_INCOMPLETE_AMPLIFIER
                && target.getHealth() <= target.getMaxHealth()
                * InfectionMechanics.COTH_CONVERSION_HEALTH_FRACTION) {
            InfectionMechanics.convertCothHost(target);
        }
    }

    @SubscribeEvent
    public static void preventParasiteTargeting(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Parasite
                && (event.getNewTarget() instanceof Parasite
                || event.getNewTarget() instanceof LivingEntity target
                && InfectionMechanics.isHiddenAssimilated(target))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void convertTerminalCothHost(LivingDeathEvent event) {
        LivingEntity host = event.getEntity();
        if (host.level().isClientSide) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (InfectionMechanics.revealHiddenAssimilated(host, attacker)) {
            event.setCanceled(true);
            return;
        }
        if (host instanceof Player player && InfectionMechanics.convertKilledPlayer(player, attacker)) {
            return;
        }
        if ((attacker instanceof GnatEntity || attacker instanceof LiceEntity)
                && InfectionMechanics.convertFeralEndermanHost(host)) {
            event.setCanceled(true);
            return;
        }
        if (InfectionMechanics.convertKilledHost(host, attacker)) {
            event.setCanceled(true);
            return;
        }
        if (attacker instanceof Parasite) {
            return;
        }
        MobEffectInstance coth = host.getEffect(ModMobEffects.COTH.get());
        if (coth != null && coth.getAmplifier() >= InfectionMechanics.COTH_MAX_AMPLIFIER
                && InfectionMechanics.convertInfectedHost(host)) {
            event.setCanceled(true);
        }
    }
}
