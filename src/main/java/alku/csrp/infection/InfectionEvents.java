package alku.csrp.infection;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.FeralEndermanEntity;
import alku.csrp.entity.GnatEntity;
import alku.csrp.entity.LiceEntity;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Connects parasite attacks and terminal COTH stages to infection progression. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class InfectionEvents {
    private InfectionEvents() {
    }

    @SubscribeEvent
    public static void infectFromParasiteHit(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Parasite) {
            Entity attacker = event.getSource().getEntity();
            Entity direct = event.getSource().getDirectEntity();
            if (attacker instanceof Parasite || direct instanceof Parasite) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getAmount() <= 0.0F || event.getEntity().level().isClientSide
                || event.getEntity() instanceof Parasite) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Parasite && event.getEntity().level() instanceof ServerLevel level) {
            float chance = attacker instanceof FeralEndermanEntity
                    ? FeralEndermanEntity.cothChance()
                    : EvolutionSystem.generationProfile(level).cothChance();
            if (event.getEntity().getRandom().nextFloat() < chance) {
                InfectionMechanics.applyCoth(event.getEntity(), attacker);
            }
        }
    }

    @SubscribeEvent
    public static void preventParasiteTargeting(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Parasite
                && event.getNewAboutToBeSetTarget() instanceof Parasite) {
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
        MobEffectInstance coth = host.getEffect(ModMobEffects.COTH);
        if (coth != null && coth.getAmplifier() >= InfectionMechanics.COTH_MAX_AMPLIFIER
                && InfectionMechanics.convertInfectedHost(host)) {
            event.setCanceled(true);
        }
    }
}
