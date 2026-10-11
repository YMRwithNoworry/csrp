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
        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)
                || !(attacker instanceof Parasite)
                || attacker instanceof PrimitiveParasiteEntity
                || target instanceof Parasite
                || !EvolutionSystem.generationProfile(level).minimumDamage()) {
            return;
        }
        GenerationCombat.applyCompatibilityMinimumDamage(attacker, target);
    }
}
