package alku.csrp.world;

import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.config.GeneralConfig;
import alku.csrp.config.WorldConfig;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.ParasiteTransformation;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Connects world activity to the original SRP evolution and generation state. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class EvolutionEvents {
    private static final double SPRINT_MIN_HORIZONTAL_DISTANCE_SQR = 1.0E-4D;
    private static final ResourceLocation PHASE_TEN_HEALTH =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "phase_ten_health");
    private static final ResourceLocation PHASE_TEN_DAMAGE =
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "phase_ten_damage");

    private EvolutionEvents() {
    }

    @SubscribeEvent
    public static void tickGeneration(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 20L == 0L) {
            SrpWorldData data = SrpWorldData.get(level);
            if (Config.generationEnabled()) {
                data.tickGeneration(level, 20);
            }
            data.tickPassivePoints(level);
        }
    }

    @SubscribeEvent
    public static void addKillPoints(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof Parasite) {
            int penalty = EvolutionSystem.parasiteDeathPenalty(event.getEntity());
            if (penalty > 0) {
                EvolutionSystem.addPoints(level, -penalty, EvolutionSystem.PointSource.PARASITE_DEATH);
            }
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Parasite) {
            int points = EvolutionSystem.VALUE_KILL;
            if (attacker instanceof LivingEntity living) {
                var pivot = living.getEffect(ModMobEffects.PIVOT);
                if (pivot != null) {
                    points *= 2;
                }
            }
            EvolutionSystem.addPoints(level, points, EvolutionSystem.PointSource.KILL);
        }
    }

    @SubscribeEvent
    public static void addSleepPoints(SleepFinishedTimeEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            int points = EvolutionSystem.sleepPoints(SrpWorldData.get(level).evolutionPhase());
            if (points > 0) {
                EvolutionSystem.addPoints(level, points, EvolutionSystem.PointSource.SLEEP);
            }
        }
    }

    @SubscribeEvent
    public static void removeInfestedBlockPoints(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        int penalty = event.getState().is(ModBlocks.INFESTED_RUBBLE) ? 4
                : event.getState().is(ModBlocks.INFESTED_TRUNK) ? 3
                : event.getState().is(ModBlocks.INFESTED_STAIN) ? 2 : 0;
        if (penalty > 0) {
            EvolutionSystem.addPoints(level, -penalty, EvolutionSystem.PointSource.BLOCK_BREAK);
        }
    }

    @SubscribeEvent
    public static void slowCropGrowth(CropGrowEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.random.nextFloat() < EvolutionSystem.cropGrowthBlockChance(
                        SrpWorldData.get(level).evolutionPhase())) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    @SubscribeEvent
    public static void preventFishingDrops(ItemFishedEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level
                && SrpWorldData.get(level).evolutionPhase() >= 6) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void evolveLightningStruckParasite(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && entity instanceof Parasite
                && ParasiteTransformation.evolve(entity)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventParasiteExperience(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof Parasite
                && event.getEntity().level() instanceof ServerLevel level
                && SrpWorldData.get(level).evolutionPhase() >= 8) {
            event.setDroppedExperience(0);
        }
    }

    @SubscribeEvent
    public static void preventCothDrops(LivingDropsEvent event) {
        // COTH carriers killed by players still use their normal loot table.
        if (event.getSource().getEntity() instanceof Parasite
                && !(event.getEntity() instanceof Parasite) && event.getEntity().hasEffect(ModMobEffects.COTH)
                && event.getEntity().level() instanceof ServerLevel level
                && SrpWorldData.get(level).evolutionPhase() >= 2) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public static void halveNonParasiteHealing(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Parasite)
                && event.getEntity().level() instanceof ServerLevel level
                && SrpWorldData.get(level).evolutionPhase() >= 9) {
            event.setAmount(event.getAmount() * 0.5F);
        }
    }

    @SubscribeEvent
    public static void gateNaturalSpawns(MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || event.getSpawnType() != MobSpawnType.NATURAL
                        && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        int phase = SrpWorldData.get(level).evolutionPhase();
        boolean parasite = event.getEntity() instanceof Parasite;
        if (parasite && (!GeneralConfig.allowMobs() || !WorldConfig.dimensionAllowsNaturalSpawning(level))) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            return;
        }
        if (parasite) {
            int cap = WorldConfig.naturalMobCap(level);
            if (cap > 0) {
                int count = 0;
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof Parasite && ++count >= cap) {
                        event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                        return;
                    }
                }
            }
        }
        if (parasite && phase == -2 || !parasite && phase >= 10) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            return;
        }
        if (parasite) {
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).getPath();
            if (!EvolutionSystem.canNaturallySpawn(path, phase)
                    || !EvolutionSystem.crossDimensionUnlocked(level, path)) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            } else {
                // 阶段表是原模组自定义生成器的最终准入规则，也负责覆盖各实体旧的阶段检查。
                event.setResult(MobSpawnEvent.PositionCheck.Result.SUCCEED);
            }
        }
    }

    @SubscribeEvent
    public static void applyCothToNaturalSpawns(FinalizeSpawnEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity() instanceof Parasite
                || event.getSpawnType() != MobSpawnType.NATURAL
                        && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        int phase = SrpWorldData.get(level).evolutionPhase();
        if (level.random.nextFloat() < EvolutionSystem.phaseCothChance(phase)) {
            InfectionMechanics.applyCoth(event.getEntity(), null);
        }
    }

    @SubscribeEvent
    public static void applyPhaseTenAttributes(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || !(entity instanceof Parasite)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        updatePhaseTenAttributes(entity, level);
    }

    private static void updatePhaseTenAttributes(LivingEntity entity, ServerLevel level) {
        var health = entity.getAttribute(Attributes.MAX_HEALTH);
        var damage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (SrpWorldData.get(level).evolutionPhase() >= 10) {
            if (health != null && health.getModifier(PHASE_TEN_HEALTH) == null) {
                health.addPermanentModifier(new AttributeModifier(PHASE_TEN_HEALTH, 0.07D,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                entity.setHealth(entity.getMaxHealth());
            }
            if (damage != null && damage.getModifier(PHASE_TEN_DAMAGE) == null) {
                damage.addPermanentModifier(new AttributeModifier(PHASE_TEN_DAMAGE, 0.07D,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            entity.getPersistentData().putBoolean("csrp_phase_ten_attributes", true);
        } else if (entity.getPersistentData().getBoolean("csrp_phase_ten_attributes")) {
            if (health != null) health.removeModifier(PHASE_TEN_HEALTH);
            if (damage != null) damage.removeModifier(PHASE_TEN_DAMAGE);
            entity.setHealth(Math.min(entity.getHealth(), entity.getMaxHealth()));
            entity.getPersistentData().remove("csrp_phase_ten_attributes");
        }
    }

    @SubscribeEvent
    public static void applyGenerationModifiers(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || !(entity instanceof Parasite)
                || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        EvolutionSystem.GenerationProfile profile = EvolutionSystem.generationProfile(level);
        updatePhaseTenAttributes(entity, level);
        if (entity.tickCount % 20 == 0 && entity instanceof Mob mob && mob.getTarget() == null
                && SrpWorldData.get(level).evolutionPhase() >= 9) {
            double range = Math.max(16.0D, mob.getAttributeValue(Attributes.FOLLOW_RANGE));
            level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(range),
                            candidate -> candidate != mob && candidate.isAlive() && !(candidate instanceof Parasite)
                                    && !(candidate instanceof Player player && player.getAbilities().instabuild))
                    .stream().min(java.util.Comparator.comparingDouble(mob::distanceToSqr))
                    .ifPresent(mob::setTarget);
        }
        double movedX = entity.getX() - entity.xo;
        double movedZ = entity.getZ() - entity.zo;
        boolean movedHorizontally = movedX * movedX + movedZ * movedZ
                > SPRINT_MIN_HORIZONTAL_DISTANCE_SQR;
        boolean shouldSprint = profile.sprinting() && entity instanceof Mob mob
                && mob.getTarget() != null && mob.getTarget().isAlive() && movedHorizontally;
        entity.setSprinting(shouldSprint);
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
