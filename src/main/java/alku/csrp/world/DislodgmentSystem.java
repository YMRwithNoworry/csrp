package alku.csrp.world;

import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.entity.DeterrentParasiteEntity;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.SrpWorldData.DislodgmentCode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.sound.PlayLevelSoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;

/** Original dislodgment triggers, code state, and code-bound gameplay effects. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DislodgmentSystem {
    private static final int DEATH_RADIUS = 7;
    private static final int[] PHASE_COST_MULTIPLIER = {0, 1, 3, 6, 8, 10, 13, 17, 20, 25, 30};
    private static final String SPAWN_CODES_APPLIED = "csrp_dislodgment_spawn_codes_applied";
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private DislodgmentSystem() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND && event.getLevel() instanceof ServerLevel level) {
            tryTrigger(level, 0, Config.dislodgmentRightClickTriggerChance(), event.getPos(), true);
        }
    }

    @SubscribeEvent
    public static void onExperiencePickup(PlayerXpEvent.PickupXp event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            tryTrigger(level, 1, Config.dislodgmentXpPickupTriggerChance(),
                    event.getEntity().blockPosition(), true);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent.Pre event) {
        if (event.getPlayer().level() instanceof ServerLevel level) {
            tryTrigger(level, 2, Config.dislodgmentItemPickupTriggerChance(),
                    event.getPlayer().blockPosition(), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerHealing(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player && player.level() instanceof ServerLevel level) {
            tryTrigger(level, 3, Config.dislodgmentHealingTriggerChance(), player.blockPosition(), true);
        }
    }

    @SubscribeEvent
    public static void onItemUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            tryTrigger(level, 4, Config.dislodgmentUseItemTriggerChance(),
                    event.getEntity().blockPosition(), true);
        }
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            tryTrigger(level, 5, Config.dislodgmentMenuCloseTriggerChance(),
                    event.getEntity().blockPosition(), true);
        }
    }

    @SubscribeEvent
    public static void onParasiteBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getState().is(ModBlocks.BIOMEHEART.get())) {
            tryTrigger(level, 17, 1.0D, event.getPos(), false);
        } else if (event.getState().is(ModBlocks.COLONYHEART.get())) {
            tryTrigger(level, 18, 1.0D, event.getPos(), false);
        } else {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
            if (Csrp.MODID.equals(blockId.getNamespace())) {
                tryTrigger(level, 11, Config.dislodgmentBlockBreakTriggerChance(), event.getPos(), false);
            }
        }
    }

    @SubscribeEvent
    public static void applySpawnCodes(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity entity)
                || !(entity instanceof Parasite)
                || entity.getPersistentData().getBoolean(SPAWN_CODES_APPLIED)) {
            return;
        }
        entity.getPersistentData().putBoolean(SPAWN_CODES_APPLIED, true);
        SrpWorldData data = SrpWorldData.get(level);
        applySpawnPotionCode(level, entity, data);
        applySpawnStatCode(entity, data);
    }

    @SubscribeEvent
    public static void consumeEquipmentDurability(LivingAttackEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0.0F
                || !(event.getEntity().level() instanceof ServerLevel level)
                || !Config.useDislodgment() || !Config.disloItemDurability()) {
            return;
        }
        int amount = activeValue(SrpWorldData.get(level), 6);
        if (amount < 1) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (event.getEntity() instanceof Parasite && source instanceof Player player) {
            damageItem(player, EquipmentSlot.MAINHAND, amount);
            damageItem(player, EquipmentSlot.OFFHAND, amount);
        }
        if (event.getEntity() instanceof Player player
                && event.getSource().getDirectEntity() instanceof Parasite) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                damageItem(player, slot, amount);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventUnburnedParasiteDamage(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Parasite) || event.getEntity().isOnFire()
                || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)
                || !(event.getEntity().level() instanceof ServerLevel level)
                || !Config.useDislodgment() || !Config.disloBurningDeath()
                || activeValue(SrpWorldData.get(level), 21) < 1) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void rejectHarmfulEffects(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Parasite)
                || !(event.getEntity().level() instanceof ServerLevel level)
                || event.getEffectInstance().getEffect().getCategory() != MobEffectCategory.HARMFUL
                || !Config.useDislodgment() || !Config.disloParasiteNoPotion()
                || activeValue(SrpWorldData.get(level), 11) < 1) {
            return;
        }
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    @SubscribeEvent
    public static void suppressParasiteNoise(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getEntity() instanceof Parasite) || !(event.getLevel() instanceof ServerLevel level)
                || event.getSound() == null || !Config.useDislodgment()) {
            return;
        }
        ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(event.getSound());
        String path = soundId.getPath();
        SrpWorldData data = SrpWorldData.get(level);
        if (Config.disloGrowlNoise() && activeValue(data, 15) > 0 && path.endsWith(".growl")
                || Config.disloWalkNoise() && activeValue(data, 16) > 0 && path.endsWith(".step")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void disableShield(ShieldBlockEvent event) {
        if (!event.getOriginalBlock() || !(event.getEntity() instanceof Player player)
                || !(event.getDamageSource().getEntity() instanceof Parasite)
                || !(player.level() instanceof ServerLevel level)
                || !Config.useDislodgment() || !Config.disloShieldFood()
                || activeValue(SrpWorldData.get(level), 17) < 1) {
            return;
        }
        ItemStack shield = player.getUseItem();
        if (!shield.isEmpty()) {
            player.getCooldowns().addCooldown(shield.getItem(), 100);
        }
        player.stopUsingItem();
        event.setBlocked(false);
    }

    @SubscribeEvent
    public static void corruptFoodAfterHit(LivingDamageEvent event) {
        if (event.getNewDamage() <= 0.0F || !(event.getEntity() instanceof Player player)
                || !(event.getSource().getEntity() instanceof Parasite)
                || !(player.level() instanceof ServerLevel level)
                || player.getAbilities().instabuild || !Config.useDislodgment() || !Config.disloShieldFood()
                || activeValue(SrpWorldData.get(level), 17) < 1) {
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (corruptOneFood(player, stack)) {
                return;
            }
        }
        corruptOneFood(player, player.getOffhandItem());
    }

    @SubscribeEvent
    public static void onParasiteDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !(dead instanceof Parasite)
                || !Config.useDislodgment()) {
            return;
        }

        SrpWorldData data = SrpWorldData.get(level);
        applyHighVersionDeath(level, dead, data);
        if (dead instanceof NexusParasiteEntity nexus) {
            int stage = nexusStage(nexus.getKind());
            if (stage > 0) {
                tryTrigger(level, 11 + stage, Config.dislodgmentNexusTriggerChance(stage),
                        dead.blockPosition(), false);
            }
        }
        tryTrigger(level, 10, Config.dislodgmentDeathTriggerChance(), dead.blockPosition(), false);
        applySummonByDeath(dead, event.getSource().getEntity(), data);
        applyDeathAreaCodes(level, dead, data);
    }

    @SubscribeEvent
    public static void tickCodes(LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
        if (!(event.getLevel() instanceof ServerLevel level) || !Config.useDislodgment()) {
            return;
        }
        SrpWorldData data = SrpWorldData.get(level);
        if (level.getGameTime() % 40L == 0L) {
            applyPeriodicCodes(level, data);
        }
        for (DislodgmentCode expired : data.expireDislodgmentCodes(level)) {
            onCodeEnded(level, expired);
        }
    }

    @SubscribeEvent
    public static void suppressDislodgmentDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Parasite && event.getEntity().level() instanceof ServerLevel level
                && Config.useDislodgment() && Config.disloLootXpCancel()
                && activeValue(SrpWorldData.get(level), 18) > 0) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public static void suppressDislodgmentExperience(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof Parasite && event.getEntity().level() instanceof ServerLevel level
                && Config.useDislodgment() && Config.disloLootXpCancel()
                && activeValue(SrpWorldData.get(level), 18) > 0) {
            event.setDroppedExperience(0);
        }
    }

    public static void clearCodes(ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        for (DislodgmentCode code : data.endAllDislodgmentCodes(level)) {
            onCodeEnded(level, code);
        }
    }

    public static boolean tryTrigger(ServerLevel level, int triggerId, double chance,
            BlockPos position, boolean requireCothSpy) {
        if (!Config.useDislodgment()) {
            return false;
        }
        SrpWorldData data = SrpWorldData.get(level);
        int phase = data.evolutionPhase();
        if (!data.dislodgmentTriggerReady(level) || phase < 1 || phase > 10
                || level.getRandom().nextDouble() > chance
                || requireCothSpy && !hasCothSpy(level, position)) {
            return false;
        }

        List<ActivationRule> candidates = activationRules().stream()
                .filter(rule -> Config.dislodgmentPhaseCodes(phase).contains(rule.code()))
                .filter(rule -> Config.dislodgmentTriggers(rule.code()).contains(triggerId))
                .toList();
        boolean started = false;
        for (int attempts = 0; attempts < 10 && !started && !candidates.isEmpty(); attempts++) {
            ActivationRule rule = candidates.get(level.getRandom().nextInt(candidates.size()));
            int value = saturatingMultiply(rule.value(), phase);
            int duration = saturatingMultiply(rule.durationSeconds(), phase);
            int cost = saturatingMultiply(rule.pointCost(), PHASE_COST_MULTIPLIER[phase]);
            started = data.startDislodgmentCode(level, rule.code(), value, duration, cost);
        }
        if (!candidates.isEmpty()) {
            data.setDislodgmentTriggerCooldown(level, Config.dislodgmentGlobalCooldown());
        }
        return started;
    }

    public static void onCodeStarted(ServerLevel level, int code, int value, long durationTicks) {
        if (!Config.useDislodgment() || code != 5 || !Config.disloDeathRaid()) {
            return;
        }
        List<LivingEntity> parasites = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && living instanceof Parasite) {
                parasites.add(living);
            }
        }
        for (LivingEntity parasite : parasites) {
            parasite.hurt(level.damageSources().fellOutOfWorld(), 10_000.0F);
        }
    }

    public static int activeCodeValue(ServerLevel level, int code) {
        return Config.useDislodgment() ? activeValue(SrpWorldData.get(level), code) : 0;
    }

    private static List<ActivationRule> activationRules() {
        List<ActivationRule> rules = new ArrayList<>();
        if (Config.disloCothIgnoreAmplifier()) {
            rules.add(new ActivationRule(0, 1, Config.disloCothIgnoreAmplifierDuration(),
                    Config.disloCothIgnoreAmplifierPointCost()));
        }
        if (Config.disloCothTiers()) {
            rules.add(new ActivationRule(1, Config.disloCothTiersValue(), Config.disloCothTiersDuration(),
                    Config.disloCothTiersPointCost()));
        }
        if (Config.disloSummonByDeath()) {
            rules.add(new ActivationRule(2, Config.disloSummonByDeathValue(),
                    Config.disloSummonByDeathDuration(), Config.disloSummonByDeathPointCost()));
        }
        if (Config.disloPotionEffect()) {
            rules.add(new ActivationRule(3, Config.disloPotionEffectValue(),
                    Config.disloPotionEffectDuration(), Config.disloPotionEffectPointCost()));
        }
        if (Config.disloStats()) {
            rules.add(new ActivationRule(4, Config.disloStatsValue(),
                    Config.disloStatsDuration(), Config.disloStatsPointCost()));
        }
        if (Config.disloDeathRaid()) {
            rules.add(new ActivationRule(5, Config.disloDeathRaidValue(),
                    Config.disloDeathRaidDuration(), Config.disloDeathRaidPointCost()));
        }
        if (Config.disloItemDurability()) {
            rules.add(new ActivationRule(6, Config.disloItemDurabilityValue(),
                    Config.disloItemDurabilityDuration(), Config.disloItemDurabilityPointCost()));
        }
        if (Config.disloHealingDeath()) {
            rules.add(new ActivationRule(7, Config.disloHealingDeathValue(),
                    Config.disloHealingDeathDuration(), Config.disloHealingDeathPointCost()));
        }
        if (Config.disloDamageDeath()) {
            rules.add(new ActivationRule(8, Config.disloDamageDeathValue(),
                    Config.disloDamageDeathDuration(), Config.disloDamageDeathPointCost()));
        }
        if (Config.disloFoodDeath()) {
            rules.add(new ActivationRule(9, Config.disloFoodDeathValue(),
                    Config.disloFoodDeathDuration(), Config.disloFoodDeathPointCost()));
        }
        if (Config.disloDeathHighVersions()) {
            rules.add(new ActivationRule(10, Config.disloDeathHighVersionsValue(),
                    Config.disloDeathHighVersionsDuration(), Config.disloDeathHighVersionsPointCost()));
        }
        if (Config.disloParasiteNoPotion()) {
            rules.add(new ActivationRule(11, 1, Config.disloParasiteNoPotionDuration(),
                    Config.disloParasiteNoPotionPointCost()));
        }
        if (Config.disloHealthDraining()) {
            rules.add(new ActivationRule(12, Config.disloHealthDrainingValue(),
                    Config.disloHealthDrainingDuration(), Config.disloHealthDrainingPointCost()));
        }
        if (Config.disloFoodDraining()) {
            rules.add(new ActivationRule(13, Config.disloFoodDrainingValue(),
                    Config.disloFoodDrainingDuration(), Config.disloFoodDrainingPointCost()));
        }
        if (Config.disloGrowlNoise()) {
            rules.add(new ActivationRule(15, 1, Config.disloGrowlNoiseDuration(),
                    Config.disloGrowlNoisePointCost()));
        }
        if (Config.disloWalkNoise()) {
            rules.add(new ActivationRule(16, 1, Config.disloWalkNoiseDuration(),
                    Config.disloWalkNoisePointCost()));
        }
        if (Config.disloShieldFood()) {
            rules.add(new ActivationRule(17, 1, Config.disloShieldFoodDuration(),
                    Config.disloShieldFoodPointCost()));
        }
        if (Config.disloLootXpCancel()) {
            rules.add(new ActivationRule(18, 1, Config.disloLootXpCancelDuration(),
                    Config.disloLootXpCancelPointCost()));
        }
        if (Config.disloBurningDeath()) {
            rules.add(new ActivationRule(21, 1, Config.disloBurningDeathDuration(),
                    Config.disloBurningDeathPointCost()));
        }
        return rules;
    }

    private static void applyPeriodicCodes(ServerLevel level, SrpWorldData data) {
        int damage = Config.disloHealthDraining() ? activeValue(data, 12) : 0;
        if (damage > 0) {
            float amount = saturatingMultiply(damage, 2);
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && !(living instanceof Parasite)) {
                    living.hurt(level.damageSources().magic(), amount);
                }
            }
        }
        int exhaustion = Config.disloFoodDraining() ? activeValue(data, 13) : 0;
        if (exhaustion > 0) {
            float amount = saturatingMultiply(exhaustion, 2);
            for (Player player : level.players()) {
                player.causeFoodExhaustion(amount);
            }
        }
    }

    private static void applyHighVersionDeath(ServerLevel level, LivingEntity dead, SrpWorldData data) {
        int value = Config.disloDeathHighVersions() ? activeValue(data, 10) : 0;
        ResourceLocation deadId = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType());
        if (value < 1 || !Csrp.MODID.equals(deadId.getNamespace()) || !deadId.getPath().startsWith("sim_")
                || level.getRandom().nextDouble() >= Config.disloDeathHighVersionsChance()) {
            return;
        }
        List<EntityType<? extends Mob>> pool = value >= Config.disloDeathHighVersionsPure()
                ? pureTypes()
                : value >= Config.disloDeathHighVersionsAdapted() ? adaptedTypes() : primitiveTypes();
        EntityType<? extends Mob> type = pool.get(level.getRandom().nextInt(pool.size()));
        Mob spawned = type.create(level);
        if (spawned == null) {
            return;
        }
        spawned.moveTo(dead.getX(), dead.getY(), dead.getZ(), dead.getYRot(), dead.getXRot());
        spawned.finalizeSpawn(level, level.getCurrentDifficultyAt(dead.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        spawned.addEffect(new MobEffectInstance(ModMobEffects.REPEL.get(), 600, 0, false, false));
        level.addFreshEntity(spawned);
        level.levelEvent(null, 1026, spawned.blockPosition(), 0);
    }

    private static List<EntityType<? extends Mob>> primitiveTypes() {
        return List.of(ModEntities.PRI_LONGARMS.get(), ModEntities.PRI_SUMMONER.get(),
                ModEntities.PRI_REEKER.get(), ModEntities.PRI_MANDUCATER.get(),
                ModEntities.PRI_ARACHNIDA.get(), ModEntities.PRI_BOLSTER.get(),
                ModEntities.PRI_DEVOURER.get(), ModEntities.PRI_VISCERA.get(),
                ModEntities.PRI_YELLOWEYE.get(), ModEntities.PRI_VERMIN.get(),
                ModEntities.PRI_TOZOON.get());
    }

    private static List<EntityType<? extends Mob>> adaptedTypes() {
        return List.of(ModEntities.ADA_LONGARMS.get(), ModEntities.ADA_SUMMONER.get(),
                ModEntities.ADA_REEKER.get(), ModEntities.ADA_MANDUCATER.get(),
                ModEntities.ADA_ARACHNIDA.get(), ModEntities.ADA_BOLSTER.get(),
                ModEntities.ADA_DEVOURER.get(), ModEntities.ADA_YELLOWEYE.get(),
                ModEntities.ADA_VERMIN.get(), ModEntities.ADA_TOZOON.get());
    }

    private static List<EntityType<? extends Mob>> pureTypes() {
        return List.of(ModEntities.GRUNT.get(), ModEntities.BOMBER_LIGHT.get(), ModEntities.MONARCH.get(),
                ModEntities.OVERSEER.get(), ModEntities.VIGILANTE.get(), ModEntities.WARDEN.get(),
                ModEntities.CARRIER_HEAVY.get());
    }

    private static boolean corruptOneFood(Player player, ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible()) {
            return false;
        }
        stack.shrink(1);
        player.spawnAtLocation(new ItemStack(ModItems.ASSIMILATED_FLESH.get()));
        return true;
    }

    private static boolean hasCothSpy(ServerLevel level, BlockPos position) {
        int required = Config.dislodgmentCothSpy();
        if (required <= 1 || position == null) {
            return true;
        }
        int count = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(position).inflate(5.0D, 3.0D, 5.0D),
                entity -> entity.hasEffect(ModMobEffects.COTH.get())).size();
        return count >= required;
    }

    private static void applySpawnPotionCode(ServerLevel level, LivingEntity entity, SrpWorldData data) {
        DislodgmentCode code = data.dislodgmentCode(3);
        List<? extends String> effects = Config.disloPotionEffects();
        if (!Config.disloPotionEffect() || code == null || code.value() < 1 || effects.isEmpty()) {
            return;
        }
        ResourceLocation effectId = ResourceLocation.tryParse(
                effects.get(level.getRandom().nextInt(effects.size())));
        if (effectId == null) {
            return;
        }
        BuiltInRegistries.MOB_EFFECT.getOptional(effectId).ifPresent(effect -> {
            int duration = (int) Math.min(Integer.MAX_VALUE,
                    Math.max(0L, code.expiresAt() - level.getGameTime()) + 50L);
            entity.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                    duration, code.value()));
        });
    }

    private static void applySpawnStatCode(LivingEntity entity, SrpWorldData data) {
        DislodgmentCode code = data.dislodgmentCode(4);
        if (!Config.disloStats() || code == null || code.value() < 1) {
            return;
        }
        double multiplier = 1.0D + code.value();
        multiplyBaseAttribute(entity, Attributes.MAX_HEALTH, multiplier);
        multiplyBaseAttribute(entity, Attributes.ARMOR, multiplier);
        multiplyBaseAttribute(entity, Attributes.ATTACK_DAMAGE, multiplier);
        entity.setHealth(entity.getMaxHealth());
    }

    private static void multiplyBaseAttribute(LivingEntity entity, Holder<Attribute> attribute, double multiplier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(instance.getBaseValue() * multiplier);
        }
    }

    private static void damageItem(Player player, EquipmentSlot slot, int amount) {
        ItemStack stack = player.getItemBySlot(slot);
        if (!stack.isEmpty() && stack.isDamageableItem()) {
            stack.hurtAndBreak(amount, player, slot);
        }
    }

    private static int nexusStage(NexusParasiteEntity.Kind kind) {
        return switch (kind) {
            case BECKON_SI, DISPATCHER_SI, ROOTER_SI -> 1;
            case BECKON_SII, DISPATCHER_SII, ROOTER_SII -> 2;
            case BECKON_SIII, DISPATCHER_SIII, ROOTER_SIII -> 3;
            case BECKON_SIV, DISPATCHER_SIV, ROOTER_SIV -> 4;
            default -> 0;
        };
    }

    private static void onCodeEnded(ServerLevel level, DislodgmentCode code) {
        if (code.code() == 2 && Config.useDislodgment() && Config.disloSummonByDeath()) {
            finishSummonByDeath(level, code.value());
        }
    }

    private static void applySummonByDeath(LivingEntity dead, Entity killer, SrpWorldData data) {
        DislodgmentCode code = data.dislodgmentCode(2);
        if (!Config.disloSummonByDeath() || code == null || code.value() < 1
                || !(killer instanceof LivingEntity livingKiller)) {
            return;
        }
        data.increaseDislodgmentValue(2, (int) dead.getMaxHealth());
        int remaining = (int) Math.min(Integer.MAX_VALUE,
                Math.max(0L, code.expiresAt() - dead.level().getGameTime()) + 50L);
        applyStackedJugg(livingKiller, remaining, dead);
    }

    private static void applyStackedJugg(LivingEntity target, int duration, LivingEntity source) {
        MobEffectInstance existing = target.getEffect(ModMobEffects.JUGG.get());
        if (existing == null) {
            target.addEffect(new MobEffectInstance(ModMobEffects.JUGG.get(), duration, 1, false, false), source);
            return;
        }
        int newDuration = existing.getDuration() + 40 <= duration ? duration : existing.getDuration() + 10;
        int amplifier = Math.min(255, existing.getAmplifier() + 1);
        target.addEffect(new MobEffectInstance(ModMobEffects.JUGG.get(), newDuration, amplifier, false, false), source);
    }

    private static void applyDeathAreaCodes(ServerLevel level, LivingEntity dead, SrpWorldData data) {
        int healing = Config.disloHealingDeath() ? activeValue(data, 7) : 0;
        if (healing > 0) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    dead.getBoundingBox().inflate(DEATH_RADIUS), entity -> entity instanceof Parasite)) {
                entity.heal(healing);
            }
            deathBurst(level, dead);
        }

        int damage = Config.disloDamageDeath() ? activeValue(data, 8) : 0;
        if (damage > 0) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    dead.getBoundingBox().inflate(DEATH_RADIUS), entity -> !(entity instanceof Parasite))) {
                entity.hurt(level.damageSources().mobAttack(dead), damage);
            }
            deathBurst(level, dead);
        }

        int exhaustion = Config.disloFoodDeath() ? activeValue(data, 9) : 0;
        if (exhaustion > 0) {
            for (Player player : level.getEntitiesOfClass(Player.class,
                    dead.getBoundingBox().inflate(DEATH_RADIUS))) {
                player.causeFoodExhaustion(exhaustion);
            }
            deathBurst(level, dead);
        }
    }

    private static void deathBurst(ServerLevel level, LivingEntity dead) {
        level.playSound(null, dead.blockPosition(), ModSounds.RATHOL_BOOM.get(),
                SoundSource.HOSTILE, 0.2F, 0.7F);
        level.sendParticles(ParticleTypes.EXPLOSION, dead.getX(), dead.getY() + dead.getBbHeight() * 0.5D,
                dead.getZ(), 7, 0.6D, 0.6D, 0.6D, 0.05D);
    }

    private static void finishSummonByDeath(ServerLevel level, int accumulatedHealth) {
        LivingEntity target = strongestJuggHolder(level);
        if (target == null || target.getEffect(ModMobEffects.JUGG.get()).getAmplifier()
                < Config.disloSummonByDeathKilling()) {
            return;
        }
        ResourceLocation payload = selectSummonPayload(accumulatedHealth);
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos position = findSpawnFloor(level, target.blockPosition().offset(
                    signedOffset(level), 0, signedOffset(level)));
            if (position != null && spawnPayloadWorm(level, target, position, payload)) {
                return;
            }
        }
    }

    private static LivingEntity strongestJuggHolder(ServerLevel level) {
        LivingEntity strongest = null;
        int amplifier = Integer.MIN_VALUE;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            MobEffectInstance effect = living.getEffect(ModMobEffects.JUGG.get());
            if (effect != null && effect.getAmplifier() >= amplifier) {
                amplifier = effect.getAmplifier();
                strongest = living;
            }
        }
        return strongest;
    }

    private static boolean spawnPayloadWorm(ServerLevel level, LivingEntity target, BlockPos position,
            ResourceLocation payload) {
        DeterrentParasiteEntity worm = ModEntities.WORM.get().create(level);
        if (worm == null) {
            return false;
        }
        worm.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D,
                level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(worm, worm.getBoundingBox().inflate(1.0D, 7.0D, 1.0D))) {
            return false;
        }
        worm.setWormPayload(1, 1);
        worm.setWormPayloadTypes(List.of(payload));
        worm.setTarget(target);
        worm.finalizeSpawn(level, level.getCurrentDifficultyAt(position), MobSpawnType.MOB_SUMMONED, null);
        return level.addFreshEntity(worm);
    }

    private static BlockPos findSpawnFloor(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.above(2).mutable();
        for (int depth = 0; depth <= 10; depth++) {
            BlockPos floor = cursor.below();
            BlockState state = level.getBlockState(cursor);
            if (state.getCollisionShape(level, cursor).isEmpty()
                    && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return cursor.immutable();
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private static ResourceLocation selectSummonPayload(int accumulatedHealth) {
        ResourceLocation fallback = new ResourceLocation(Csrp.MODID, "warden");
        ResourceLocation selected = null;
        int selectedThreshold = Integer.MIN_VALUE;
        for (String entry : Config.disloSummonByDeathMobs()) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 2) {
                continue;
            }
            try {
                int threshold = Integer.parseInt(parts[0]);
                ResourceLocation id = normalizeLegacyId(ResourceLocation.tryParse(parts[1]));
                if (id != null && selected == null) {
                    selected = id;
                }
                if (id != null && threshold < accumulatedHealth && threshold > selectedThreshold) {
                    selected = id;
                    selectedThreshold = threshold;
                }
            } catch (NumberFormatException ignored) {
                // Invalid user entries are skipped like missing legacy registry entries.
            }
        }
        return selected == null ? fallback : selected;
    }

    private static ResourceLocation normalizeLegacyId(ResourceLocation id) {
        if (id != null && "srparasites".equals(id.getNamespace())) {
            return new ResourceLocation(Csrp.MODID, id.getPath());
        }
        return id;
    }

    private static int signedOffset(ServerLevel level) {
        int value = level.getRandom().nextInt(4);
        return level.getRandom().nextBoolean() ? value : -value;
    }

    private static int activeValue(SrpWorldData data, int code) {
        DislodgmentCode entry = data.dislodgmentCode(code);
        return entry == null ? 0 : entry.value();
    }

    private static int saturatingMultiply(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) left * right));
    }

    private record ActivationRule(int code, int value, int durationSeconds, int pointCost) {
    }
}
