package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Connects world activity to the original SRP evolution and generation state. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class EvolutionEvents {
    private EvolutionEvents() {
    }

    @SubscribeEvent
    public static void tickGeneration(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 20L == 0L) {
            SrpWorldData.get(level).tickGeneration(level, 20);
        }
    }

    @SubscribeEvent
    public static void addKillPoints(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || event.getEntity() instanceof Parasite) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Parasite) {
            int points = EvolutionSystem.VALUE_KILL;
            if (attacker instanceof LivingEntity living) {
                var pivot = living.getEffect(ModMobEffects.PIVOT);
                if (pivot != null) {
                    points *= 2 * (pivot.getAmplifier() + 1);
                }
            }
            EvolutionSystem.addPoints(level, points, EvolutionSystem.PointSource.KILL);
        }
    }

    @SubscribeEvent
    public static void applyGenerationModifiers(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || !(entity instanceof Parasite)
                || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        EvolutionSystem.GenerationProfile profile = EvolutionSystem.generationProfile(level);
        if (profile.sprinting() && entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != null) {
            entity.setSprinting(true);
        }
        if (entity.tickCount % 20 != 0 || entity.getHealth() >= entity.getMaxHealth()) {
            return;
        }
        float healing = profile.mobHealing();
        if (entity.hasEffect(MobEffects.POISON)) {
            healing += profile.poisonHealing();
        }
        if (healing > 0.0F) {
            entity.heal(healing);
        }
    }
}
