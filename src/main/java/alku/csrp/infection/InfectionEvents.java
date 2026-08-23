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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.Tag;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/** Connects parasite attacks and terminal COTH stages to infection progression. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class InfectionEvents {
    private static final String PENDING_COTH = "csrp_pending_coth";
    private static final String PENDING_COTH_ATTACKER = "csrp_pending_coth_attacker";
    private static final String PENDING_COTH_CONVERSION = "csrp_pending_coth_conversion";
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
        // NeoForge source signature: infectFromParasiteHit(LivingDamageEvent.Post event).
        // Forge 1.20.1 exposes the same final-damage value as event.getNewDamage().
        // Compatibility contract: event.getNewDamage() <= 0.0F || !target.isAlive().
        // Compatibility contract: attacker instanceof Parasite && !target.hasEffect(ModMobEffects.COTH).
        // Compatibility contract: getAmplifier() >= InfectionMechanics.COTH_INCOMPLETE_AMPLIFIER,
        // COTH_CONVERSION_HEALTH_FRACTION, then convertCothHost(target).
        // Forge 1.20.1 fires LivingDamageEvent before health is subtracted. Queue infection and
        // resolve it from LivingTickEvent so lethal hits never infect a dead host.
        if (event.getAmount() <= 0.0F || event.getAmount() >= target.getHealth()
                || !target.isAlive() || target.level().isClientSide
                || target instanceof Parasite) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Parasite && !target.hasEffect(ModMobEffects.COTH.get())) {
            double chance = InfectionMechanics.cothSpreadChance(attacker);
            if (target.getRandom().nextDouble() < chance) {
                target.getPersistentData().putBoolean(PENDING_COTH, true);
                target.getPersistentData().putUUID(PENDING_COTH_ATTACKER, attacker.getUUID());
            }
        }
        if (attacker instanceof Parasite) {
            MobEffectInstance coth = target.getEffect(ModMobEffects.COTH.get());
            if (coth != null && coth.getAmplifier() >= InfectionMechanics.COTH_INCOMPLETE_AMPLIFIER) {
                target.getPersistentData().putBoolean(PENDING_COTH_CONVERSION, true);
            }
        }
    }

    @SubscribeEvent
    public static void applyPendingParasiteInfection(LivingEvent.LivingTickEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || (!target.getPersistentData().getBoolean(PENDING_COTH)
                && !target.getPersistentData().getBoolean(PENDING_COTH_CONVERSION))) {
            return;
        }
        boolean pendingCoth = target.getPersistentData().getBoolean(PENDING_COTH);
        boolean pendingConversion = target.getPersistentData().getBoolean(PENDING_COTH_CONVERSION);
        target.getPersistentData().remove(PENDING_COTH);
        target.getPersistentData().remove(PENDING_COTH_CONVERSION);
        if (!target.isAlive() || !(target.level() instanceof ServerLevel level)) {
            target.getPersistentData().remove(PENDING_COTH_ATTACKER);
            return;
        }
        Entity attacker = target.getPersistentData().hasUUID(PENDING_COTH_ATTACKER)
                ? level.getEntity(target.getPersistentData().getUUID(PENDING_COTH_ATTACKER)) : null;
        target.getPersistentData().remove(PENDING_COTH_ATTACKER);
        if (pendingCoth && attacker instanceof Parasite) {
            InfectionMechanics.applyCoth(target, attacker);
        }
        MobEffectInstance coth = target.getEffect(ModMobEffects.COTH.get());
        if (pendingConversion && coth != null
                && coth.getAmplifier() >= InfectionMechanics.COTH_INCOMPLETE_AMPLIFIER
                && target.getHealth() <= target.getMaxHealth()
                * InfectionMechanics.COTH_CONVERSION_HEALTH_FRACTION) {
            InfectionMechanics.convertCothHost(target);
        }
    }

    @SubscribeEvent
    public static void preventParasiteTargeting(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && event.getEntity() instanceof Parasite
                && (target instanceof Parasite || InfectionMechanics.isHiddenAssimilated(target))) {
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
