package alku.csrp.entity;

import alku.csrp.Csrp;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Restores EntityParasiteBase combat rules for parasite entities outside the shared port base. */
@Mod.EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteCombatEvents {
    private ParasiteCombatEvents() {
    }

    @SubscribeEvent
    public static void applyOriginalCombatRules(LivingAttackEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || event.isCanceled()
                || event.getAmount() <= 0.0F) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (target instanceof Parasite
                && !(target instanceof PrimitiveParasiteEntity)
                && EvolutionSystem.generationProfile(level).damageCap()
                && !event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                && !GenerationCombat.ignoresDamageCap(event.getSource())
                && !target.isInvulnerableTo(event.getSource())) {
            float cappedDamage = GenerationCombat.compatibilityDamageCap(target, event.getAmount());
            if (cappedDamage < event.getAmount()) {
                event.setCanceled(true);
                GenerationCombat.applyCompatibilityCapDamage(target, event.getSource(), cappedDamage);
                return;
            }
        }

        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)
                || !(attacker instanceof Parasite)
                || attacker instanceof PrimitiveParasiteEntity
                || attacker instanceof RupterEntity
                || attacker instanceof FeralEndermanEntity
                || attacker instanceof SimAdventurerHeadEntity
                || target instanceof Parasite
                || !EvolutionSystem.generationProfile(level).minimumDamage()) {
            return;
        }
        GenerationCombat.applyCompatibilityMinimumDamage(attacker, target);
    }
}
