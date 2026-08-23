package alku.csrp.infection;

import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.CrudeParasiteEntity;
import alku.csrp.entity.FeralEndermanEntity;
import alku.csrp.entity.FeralParasiteEntity;
import alku.csrp.entity.HijackedParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.PreeminentParasiteEntity;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.entity.PureParasiteEntity;
import alku.csrp.entity.RupterEntity;
import alku.csrp.entity.SimAdventurerEntity;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.DislodgmentSystem;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.sounds.SoundSource;

/** Server-side COTH and Viral infection progression, spread, and host conversion. */
public final class InfectionMechanics {
    public static final int COTH_BASE_DURATION_TICKS = 3_600;
    public static final int COTH_REFRESH_THRESHOLD_TICKS = 200;
    public static final int COTH_INCOMPLETE_AMPLIFIER = 1;
    public static final int COTH_MAX_AMPLIFIER = 2;
    public static final double COTH_SPREAD_RADIUS = 4.0D;
    public static final float COTH_CONVERSION_HEALTH_FRACTION = 0.35F;
    public static final float ASSIMILATED_UNHIDE_HEALTH_FRACTION = 0.30F;
    private static final String ASSIMILATION_HOST_TAG = "csrp_assimilation_host";
    private static final String HIDDEN_ASSIMILATED_TAG = "csrp_hidden_assimilated";
    private static final int ASSIMILATION_FERAL_PHASE = 7;
    private static final int ASSIMILATION_DEHIDE_PHASE = 9;
    private static final int ASSIMILATION_NAUSEA_TICKS = 100;
    private static final int ASSIMILATION_RESTORE_NAUSEA_TICKS = 60;
    private static final int ASSIMILATION_NAUSEA_AMPLIFIER = 3;
    private static final float CAMOUFLAGE_RESIST_CHANCE = 0.70F;
    private static final String[] FERALS = {
            "fer_bear", "fer_cow", "fer_enderman", "fer_horse", "fer_human",
            "fer_pig", "fer_sheep", "fer_villager", "fer_wolf"
    };
    private static final String[] PRIMITIVES = {
            "pri_longarms", "pri_summoner", "pri_devourer", "pri_reeker", "pri_arachnida",
            "pri_bolster", "pri_burrower", "pri_vermin", "pri_manducater", "pri_viscera", "pri_yelloweye"
    };
    private static final String[] ADAPTED = {
            "ada_longarms", "ada_summoner", "ada_devourer", "ada_reeker", "ada_arachnida",
            "ada_bolster", "ada_burrower", "ada_manducater", "ada_viscera", "ada_yelloweye"
    };
    private static final String[] PURE = {
            "grunt", "bomber_light", "monarch", "overseer", "vigilante", "warden", "marauder"
    };

    private InfectionMechanics() {
    }

    public static void applyCoth(LivingEntity target, Entity source) {
        applyCoth(target, source, COTH_BASE_DURATION_TICKS);
    }

    public static void applyCoth(LivingEntity target, Entity source, int minimumDurationTicks) {
        if (!isInfectable(target)
                || target.hasEffect(ModMobEffects.CAMOUFLAGE.get())
                && target.getRandom().nextFloat() < CAMOUFLAGE_RESIST_CHANCE) {
            return;
        }
        MobEffectInstance existing = target.getEffect(ModMobEffects.COTH.get());
        if (existing != null) {
            return;
        }
        int duration = Math.max(COTH_BASE_DURATION_TICKS, minimumDurationTicks);
        boolean effectChanged = target.addEffect(
                new MobEffectInstance(ModMobEffects.COTH.get(), duration, 0, false, false, true), source);
        if (effectChanged && !target.level().isClientSide) {
            playInfectionSound(target);
        }
    }

    /**
     * Merges a requested COTH duration/amplifier without shortening or weakening an existing effect.
     * The sound is emitted only for a newly infected host.
     */
    public static void applyCothEffect(LivingEntity target, Entity source, int durationTicks, int amplifier) {
        applyCothEffect(target, source, durationTicks, amplifier, false, true);
    }

    public static void applyCothEffect(LivingEntity target, Entity source, int durationTicks, int amplifier,
                                       boolean ambient, boolean visible) {
        MobEffectInstance existing = target.getEffect(ModMobEffects.COTH.get());
        boolean alreadyInfected = existing != null;
        int mergedDuration = existing == null ? durationTicks : Math.max(durationTicks, existing.getDuration());
        int mergedAmplifier = existing == null ? amplifier : Math.max(amplifier, existing.getAmplifier());
        boolean mergedAmbient = existing == null ? ambient : ambient && existing.isAmbient();
        boolean mergedVisible = existing == null ? visible : visible || existing.isVisible();
        boolean effectChanged = target.addEffect(
                new MobEffectInstance(ModMobEffects.COTH.get(), mergedDuration, mergedAmplifier,
                        mergedAmbient, mergedVisible, true), source);
        if (effectChanged && !alreadyInfected && !target.level().isClientSide) {
            playInfectionSound(target);
        }
    }

    /** Original per-tier chance for a damaging parasite hit to spread COTH. */
    public static double cothSpreadChance(Entity attacker) {
        if (!(attacker instanceof Parasite)) {
            return 0.0D;
        }
        if (attacker instanceof FeralEndermanEntity) {
            return FeralEndermanEntity.cothChance();
        }
        if (attacker instanceof HijackedParasiteEntity) {
            return Config.cothHijackedSpreadChance();
        }
        if (attacker instanceof FeralParasiteEntity) {
            return Config.cothFeralSpreadChance();
        }
        if (attacker instanceof CrudeParasiteEntity) {
            return Config.cothCrudeSpreadChance();
        }
        if (attacker instanceof AdaptedVariantEntity) {
            return Config.cothAdaptedSpreadChance();
        }
        if (attacker instanceof PureParasiteEntity || attacker instanceof PreeminentParasiteEntity) {
            return Config.cothPureSpreadChance();
        }

        String path = BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).getPath();
        if (path.startsWith("sim_")) {
            return Config.cothAssimilatedSpreadChance();
        }
        if (path.startsWith("hi_")) {
            return Config.cothHijackedSpreadChance();
        }
        if (path.startsWith("fer_")) {
            return Config.cothFeralSpreadChance();
        }
        if (path.startsWith("ada_")) {
            return Config.cothAdaptedSpreadChance();
        }
        if (path.startsWith("pri_") || attacker instanceof PrimitiveParasiteEntity) {
            return Config.cothPrimitiveSpreadChance();
        }
        return 0.0D;
    }

    private static void playInfectionSound(LivingEntity target) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                ModSounds.get("infected.growl"), SoundSource.HOSTILE,
                1.0F, 0.9F + target.getRandom().nextFloat() * 0.2F);
    }

    public static boolean isCothImmune(LivingEntity entity) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        boolean listed = Config.cothImmuneEntities().stream()
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .anyMatch(entityId::contains);
        return listed != Config.cothImmuneListInverted();
    }

    public static void tickCoth(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !isInfectable(entity)) {
            return;
        }
        MobEffectInstance coth = entity.getEffect(ModMobEffects.COTH.get());
        if (coth == null) {
            return;
        }

        int effectiveAmplifier = Math.min(COTH_MAX_AMPLIFIER, amplifier);
        if (Config.disloCothIgnoreAmplifier() && amplifier <= 1 && entity.tickCount % 20 == 0
                && entity.level() instanceof ServerLevel level
                && DislodgmentSystem.activeCodeValue(level, 0) > 0) {
            entity.forceAddEffect(new MobEffectInstance(ModMobEffects.COTH.get(), 6_666, 10,
                    coth.isAmbient(), coth.isVisible(), true), null);
            effectiveAmplifier = COTH_MAX_AMPLIFIER;
        } else if (!isCothImmune(entity) && coth.getDuration() > 0
                && coth.getDuration() <= COTH_REFRESH_THRESHOLD_TICKS) {
            int nextAmplifier = Math.max(amplifier,
                    Math.min(COTH_MAX_AMPLIFIER, effectiveAmplifier + 1));
            entity.forceAddEffect(new MobEffectInstance(ModMobEffects.COTH.get(), COTH_BASE_DURATION_TICKS,
                    nextAmplifier, coth.isAmbient(), coth.isVisible(), true), null);
        }
        if (effectiveAmplifier >= 1) {
            spreadCoth(entity);
        }
        boolean forceAssimilation = entity.level() instanceof ServerLevel serverLevel
                && SrpWorldData.get(serverLevel).evolutionPhase() >= ASSIMILATION_DEHIDE_PHASE;
        if (isHiddenAssimilated(entity)) {
            tickHiddenAssimilated(entity);
            return;
        }
        boolean belowConversionHealth = entity.getHealth()
                <= entity.getMaxHealth() * COTH_CONVERSION_HEALTH_FRACTION;
        if (effectiveAmplifier == COTH_INCOMPLETE_AMPLIFIER && belowConversionHealth
                && !isCothImmune(entity)) {
            convertIncompleteCothHost(entity);
        } else if (effectiveAmplifier >= COTH_MAX_AMPLIFIER
                && (belowConversionHealth || forceAssimilation)) {
            convertInfectedHost(entity);
        }
    }

    /** COTH II creates an Incomplete Form; COTH III creates the mapped Assimilated form. */
    public static boolean convertCothHost(LivingEntity host) {
        MobEffectInstance coth = host.getEffect(ModMobEffects.COTH.get());
        if (coth == null || coth.getAmplifier() < COTH_INCOMPLETE_AMPLIFIER) {
            return false;
        }
        if (coth.getAmplifier() >= COTH_MAX_AMPLIFIER) {
            return convertInfectedHost(host);
        }
        return convertIncompleteCothHost(host);
    }

    private static boolean convertIncompleteCothHost(LivingEntity host) {
        if (host.level().isClientSide || host.isRemoved() || !isConvertible(host) || isCothImmune(host)
                || host instanceof Player
                || !(host.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Mob converted = createIncompleteForm(host, serverLevel);
        return converted != null && replaceHost(host, converted, serverLevel);
    }

    public static boolean convertInfectedHost(LivingEntity host) {
        if (host.level().isClientSide || host.isRemoved() || !isConvertible(host) || host instanceof Player
                || !(host.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (isHiddenAssimilated(host)) {
            return revealHiddenAssimilated(host, hiddenAssimilatedThreat(host));
        }

        Mob converted = createAssimilatedHost(host, serverLevel);
        if (converted == null) {
            return false;
        }

        return replaceHost(host, converted, serverLevel);
    }

    public static boolean canForceAssimilate(LivingEntity host) {
        return host.isAlive() && !host.isRemoved() && hasMappedHost(host);
    }

    /** Immediately converts a configured host, matching the original creative Assimilation Wand. */
    public static boolean forceAssimilate(LivingEntity host) {
        if (host.level().isClientSide || host.isRemoved() || !canForceAssimilate(host)
                || !(host.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        playAssimilationStart(serverLevel, host, ASSIMILATION_NAUSEA_TICKS);
        Mob converted = createAssimilatedHost(host, serverLevel);
        if (converted == null) {
            return false;
        }
        return replaceForcedHost(host, converted, serverLevel);
    }

    public static boolean isAssimilatedBody(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return entity instanceof Parasite && id.getNamespace().equals(Csrp.MODID)
                && id.getPath().startsWith("sim_") && !id.getPath().endsWith("head");
    }

    public static boolean isHiddenAssimilated(LivingEntity entity) {
        return entity.getPersistentData().contains(HIDDEN_ASSIMILATED_TAG)
                && !entity.getPersistentData().getString(HIDDEN_ASSIMILATED_TAG).isBlank();
    }

    public static void tickHiddenAssimilated(LivingEntity disguise) {
        if (!isHiddenAssimilated(disguise) || !(disguise.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity threat = hiddenAssimilatedThreat(disguise);
        if (SrpWorldData.get(level).evolutionPhase() >= ASSIMILATION_DEHIDE_PHASE
                || (threat != null && disguise.getHealth()
                < disguise.getMaxHealth() * ASSIMILATED_UNHIDE_HEALTH_FRACTION)) {
            revealHiddenAssimilated(disguise, threat);
        }
    }

    /** Restores an idle host-backed Assimilated form before the phase-nine dehiding threshold. */
    public static boolean tryRestoreAssimilatedDisguise(LivingEntity assimilated) {
        if (!(assimilated.level() instanceof ServerLevel level)
                || SrpWorldData.get(level).evolutionPhase() >= ASSIMILATION_DEHIDE_PHASE
                || !isAssimilatedBody(assimilated)
                || !assimilated.getPersistentData().contains(ASSIMILATION_HOST_TAG)
                || (assimilated instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive())) {
            return false;
        }
        return disguiseAssimilated(assimilated, true);
    }

    /** Restores the original host as a COTH-infected disguise. */
    public static boolean disguiseAssimilated(LivingEntity assimilated) {
        return disguiseAssimilated(assimilated, false);
    }

    private static boolean disguiseAssimilated(LivingEntity assimilated, boolean automatic) {
        if (assimilated.level().isClientSide || assimilated.isRemoved() || !isAssimilatedBody(assimilated)
                || !(assimilated.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        ResourceLocation hostId = ResourceLocation.tryParse(
                assimilated.getPersistentData().getString(ASSIMILATION_HOST_TAG));
        if (hostId == null) {
            return false;
        }
        Entity created = BuiltInRegistries.ENTITY_TYPE.getOptional(hostId)
                .map(type -> type.create(serverLevel)).orElse(null);
        if (!(created instanceof Mob disguise) || (automatic && disguise instanceof Monster)) {
            return false;
        }
        playAssimilationStart(serverLevel, assimilated, ASSIMILATION_RESTORE_NAUSEA_TICKS);
        disguise.moveTo(assimilated.getX(), assimilated.getY(), assimilated.getZ(),
                assimilated.getYRot(), assimilated.getXRot());
        disguise.setCustomName(assimilated.getCustomName());
        disguise.setCustomNameVisible(assimilated.isCustomNameVisible());
        float healthFraction = assimilated.getMaxHealth() <= 0.0F
                ? 1.0F : assimilated.getHealth() / assimilated.getMaxHealth();
        disguise.setHealth(Math.max(1.0F, disguise.getMaxHealth() * Math.max(0.0F, healthFraction)));
        if (assimilated instanceof Mob sourceMob && sourceMob.isPersistenceRequired()) {
            disguise.setPersistenceRequired();
        }
        disguise.getPersistentData().putString(HIDDEN_ASSIMILATED_TAG,
                BuiltInRegistries.ENTITY_TYPE.getKey(assimilated.getType()).toString());
        disguise.addEffect(new MobEffectInstance(ModMobEffects.COTH.get(), COTH_BASE_DURATION_TICKS,
                COTH_MAX_AMPLIFIER, false, false, true));
        if (!serverLevel.addFreshEntity(disguise)) {
            return false;
        }
        playAssimilationCompletion(serverLevel, disguise);
        assimilated.discard();
        return true;
    }

    /** Recreates the exact Assimilated body saved by a disguise without awarding conversion points. */
    public static boolean revealHiddenAssimilated(LivingEntity disguise, Entity attacker) {
        if (disguise.level().isClientSide || disguise.isRemoved() || !isHiddenAssimilated(disguise)
                || !(disguise.level() instanceof ServerLevel level)) {
            return false;
        }
        ResourceLocation assimilatedId = ResourceLocation.tryParse(
                disguise.getPersistentData().getString(HIDDEN_ASSIMILATED_TAG));
        if (assimilatedId == null || !assimilatedId.getNamespace().equals(Csrp.MODID)
                || !assimilatedId.getPath().startsWith("sim_") || assimilatedId.getPath().endsWith("head")) {
            return false;
        }
        Entity created = BuiltInRegistries.ENTITY_TYPE.getOptional(assimilatedId)
                .map(type -> type.create(level)).orElse(null);
        if (!(created instanceof Mob converted)) {
            return false;
        }
        float healthFraction = disguise.getMaxHealth() <= 0.0F
                ? 1.0F : disguise.getHealth() / disguise.getMaxHealth();
        converted.moveTo(disguise.getX(), disguise.getY(), disguise.getZ(),
                disguise.getYRot(), disguise.getXRot());
        converted.finalizeSpawn(level, level.getCurrentDifficultyAt(disguise.blockPosition()),
                MobSpawnType.CONVERSION, null, null);
        converted.setHealth(Math.max(1.0F, converted.getMaxHealth() * Math.max(0.0F, healthFraction)));
        converted.setCustomName(disguise.getCustomName());
        converted.setCustomNameVisible(disguise.isCustomNameVisible());
        if (disguise instanceof Mob sourceMob && sourceMob.isPersistenceRequired()) {
            converted.setPersistenceRequired();
        }
        converted.getPersistentData().putString(ASSIMILATION_HOST_TAG,
                BuiltInRegistries.ENTITY_TYPE.getKey(disguise.getType()).toString());
        if (attacker instanceof LivingEntity livingAttacker && livingAttacker.isAlive()
                && !(livingAttacker instanceof Parasite)) {
            converted.setTarget(livingAttacker);
        }
        if (!level.addFreshEntity(converted)) {
            return false;
        }
        playAssimilationCompletion(level, converted);
        disguise.discard();
        return true;
    }

    private static LivingEntity hiddenAssimilatedThreat(LivingEntity disguise) {
        LivingEntity attacker = disguise.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && !(attacker instanceof Parasite)) {
            return attacker;
        }
        if (disguise instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()
                && !(mob.getTarget() instanceof Parasite)) {
            return mob.getTarget();
        }
        return null;
    }

    /** Gnat conversion always prefers a Feral form, then an assimilated or hijacked form. */
    public static boolean convertGnatHost(LivingEntity host) {
        if (host.level().isClientSide || host.isRemoved() || !isConvertible(host) || host instanceof Player
                || !(host.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Mob converted = createMappedHost(host, serverLevel, true);
        if (converted == null) {
            converted = createHijackedHost(host, serverLevel);
        }
        if (converted == null) {
            return false;
        }

        return replaceHost(host, converted, serverLevel);
    }

    /** Gnat and Lice kills turn an Enderman directly into its Feral form. */
    public static boolean convertFeralEndermanHost(LivingEntity host) {
        if (host.getType() != EntityType.ENDERMAN || host.level().isClientSide || host.isRemoved()
                || !(host.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        FeralEndermanEntity converted = ModEntities.FER_ENDERMAN.get().create(serverLevel);
        if (converted == null) {
            return false;
        }
        return replaceHost(host, converted, serverLevel);
    }

    private static boolean replaceHost(LivingEntity host, Mob converted, ServerLevel serverLevel) {
        float healthFraction = host.getMaxHealth() <= 0.0F ? 1.0F : host.getHealth() / host.getMaxHealth();
        MobEffectInstance coth = host.getEffect(ModMobEffects.COTH.get());
        boolean terminalCothAssimilation = coth != null && coth.getAmplifier() >= COTH_MAX_AMPLIFIER;
        boolean assimilatedEnderman = host.getType() == EntityType.ENDERMAN
                && BuiltInRegistries.ENTITY_TYPE.getKey(converted.getType()).getPath().equals("sim_enderman");
        converted.moveTo(host.getX(), host.getY(), host.getZ(), host.getYRot(), host.getXRot());
        converted.setHealth(Math.max(1.0F, converted.getMaxHealth() * Math.max(0.1F, healthFraction)));
        converted.setCustomName(host.getCustomName());
        converted.setCustomNameVisible(host.isCustomNameVisible());
        converted.setPersistenceRequired();
        if (isAssimilatedBody(converted)) {
            converted.getPersistentData().putString(ASSIMILATION_HOST_TAG,
                    BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString());
        }
        if (!serverLevel.addFreshEntity(converted)) {
            return false;
        }
        if (assimilatedEnderman) {
            SrpWorldData.get(serverLevel).recordAssimilatedEnderman();
        }
        if (terminalCothAssimilation) {
            EvolutionSystem.addPoints(serverLevel, EvolutionSystem.VALUE_COTH, EvolutionSystem.PointSource.COTH);
        }
        host.discard();
        return true;
    }

    /** Converts a COTH victim on a successful parasite kill-conversion roll. */
    public static boolean convertKilledHost(LivingEntity host, Entity attacker) {
        if (!(attacker instanceof Parasite) || host.level().isClientSide || host instanceof Parasite
                || host instanceof Player || host.isRemoved()
                || !(host.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        int phase = SrpWorldData.get(serverLevel).evolutionPhase();
        boolean guaranteed = attacker instanceof RupterEntity
                || attacker instanceof FeralParasiteEntity && phase >= ASSIMILATION_FERAL_PHASE;
        if (!guaranteed && !passesCothKillConversion(host)) {
            return false;
        }

        Mob converted = host.isBaby() ? null : createMappedHost(host, serverLevel,
                phase >= ASSIMILATION_FERAL_PHASE);
        if (converted == null) {
            converted = createIncompleteForm(host, serverLevel);
        }
        if (converted == null) {
            return false;
        }

        return replaceHost(host, converted, serverLevel);
    }

    private static Mob createIncompleteForm(LivingEntity host, ServerLevel level) {
        double bodyVolume = host.getBbWidth() * host.getBbWidth() * host.getBbHeight();
        return bodyVolume > 0.517D
                ? ModEntities.INCOMPLETEFORM_MEDIUM.get().create(level)
                : ModEntities.INCOMPLETEFORM_SMALL.get().create(level);
    }

    /** A COTH-infected player killed by a parasite leaves an Assimilated Adventurer behind. */
    public static boolean convertKilledPlayer(Player player, Entity attacker) {
        if (!(attacker instanceof Parasite) || player.level().isClientSide || player.isRemoved()
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        int phase = SrpWorldData.get(serverLevel).evolutionPhase();
        boolean guaranteed = attacker instanceof RupterEntity
                || attacker instanceof FeralParasiteEntity && phase >= ASSIMILATION_FERAL_PHASE;
        if (!guaranteed && !passesCothKillConversion(player)) {
            return false;
        }
        SimAdventurerEntity converted = ModEntities.SIM_ADVENTURER.get().create(serverLevel);
        if (converted == null) {
            return false;
        }
        converted.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        converted.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(player.blockPosition()),
                MobSpawnType.CONVERSION, null, null);
        converted.setCustomName(player.getName().copy());
        converted.setCustomNameVisible(true);
        converted.setPersistenceRequired();
        boolean keepInventory = serverLevel.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        EquipmentSlot[] inheritedSlots = {
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : inheritedSlots) {
            ItemStack equipment = player.getItemBySlot(slot);
            if (equipment.isEmpty()) {
                continue;
            }
            converted.setItemSlot(slot, equipment.copy());
            converted.setDropChance(slot, 1.0F);
            if (!keepInventory) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        serverLevel.addFreshEntity(converted);
        return true;
    }

    private static boolean passesCothKillConversion(LivingEntity host) {
        MobEffectInstance coth = host.getEffect(ModMobEffects.COTH.get());
        if (coth == null) {
            return false;
        }
        int amplifier = Math.min(COTH_MAX_AMPLIFIER, Math.max(0, coth.getAmplifier()));
        double baseChance = Config.cothConvertAtKillChance();
        double chance = baseChance + (1.0D - baseChance) * amplifier / COTH_MAX_AMPLIFIER;
        return host.getRandom().nextDouble() < chance;
    }

    private static void spreadCoth(LivingEntity source) {
        for (LivingEntity target : source.level().getEntitiesOfClass(LivingEntity.class,
                source.getBoundingBox().inflate(COTH_SPREAD_RADIUS), InfectionMechanics::isInfectable)) {
            if (target != source && source.hasLineOfSight(target)) {
                applyCoth(target, source);
            }
        }
    }

    private static Mob createAssimilatedHost(LivingEntity host, ServerLevel level) {
        if (host.isBaby() || !hasMappedHost(host)) {
            return createIncompleteForm(host, level);
        }
        if (SrpWorldData.get(level).evolutionPhase() >= ASSIMILATION_FERAL_PHASE) {
            Mob latePhaseHost = createMappedHost(host, level, true);
            if (latePhaseHost != null) {
                return latePhaseHost;
            }
        }
        int dislodgmentTier = Config.disloCothTiers()
                ? DislodgmentSystem.activeCodeValue(level, 1) : 0;
        if (dislodgmentTier > 0) {
            Mob tierParasite = createTierParasite(level, dislodgmentTier);
            if (tierParasite != null) {
                return tierParasite;
            }
        }
        return createMappedHost(host, level, false);
    }

    private static boolean replaceForcedHost(LivingEntity host, Mob converted, ServerLevel level) {
        boolean assimilatedEnderman = host.getType() == EntityType.ENDERMAN
                && BuiltInRegistries.ENTITY_TYPE.getKey(converted.getType()).getPath().equals("sim_enderman");
        converted.moveTo(host.getX(), host.getY(), host.getZ(), host.getYRot(), host.getXRot());
        converted.finalizeSpawn(level, level.getCurrentDifficultyAt(host.blockPosition()),
                MobSpawnType.CONVERSION, null, null);
        converted.setHealth(converted.getMaxHealth());
        converted.setCustomName(host.getCustomName());
        converted.setCustomNameVisible(host.isCustomNameVisible());
        converted.setPersistenceRequired();
        if (isAssimilatedBody(converted)) {
            converted.getPersistentData().putString(ASSIMILATION_HOST_TAG,
                    BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString());
        }
        if (!level.addFreshEntity(converted)) {
            return false;
        }
        if (assimilatedEnderman) {
            SrpWorldData.get(level).recordAssimilatedEnderman();
        }
        playAssimilationCompletion(level, converted);
        host.discard();
        return true;
    }

    private static void playAssimilationStart(ServerLevel level, LivingEntity entity, int nauseaTicks) {
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nauseaTicks,
                ASSIMILATION_NAUSEA_AMPLIFIER, false, false));
        level.sendParticles(ModParticles.ASSIMILATION_SPLASH.get(),
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 1,
                entity.getBbWidth(), entity.getBbHeight() * 0.5D, entity.getBbWidth(), 0.02D);
    }

    private static void playAssimilationCompletion(ServerLevel level, LivingEntity entity) {
        level.sendParticles(ParticleTypes.EXPLOSION,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 11,
                entity.getBbWidth() * 0.5D, entity.getBbHeight() * 0.35D,
                entity.getBbWidth() * 0.5D, 0.0D);
        level.levelEvent(null, 1026, entity.blockPosition(), 0);
    }

    private static Mob createMappedHost(LivingEntity host, ServerLevel level, boolean preferFeral) {
        ResourceLocation hostId = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType());
        for (String mapping : Config.cothVictimParasites()) {
            String[] parts = mapping.split(";", -1);
            if (parts.length != 2 || !parts[0].trim().equals(hostId.toString())) {
                continue;
            }
            ResourceLocation targetId = ResourceLocation.tryParse(parts[1].trim());
            if (targetId == null) {
                continue;
            }
            if (preferFeral && targetId.getPath().startsWith("sim_")) {
                ResourceLocation feralId = new ResourceLocation(Csrp.MODID,
                        "fer_" + targetId.getPath().substring("sim_".length()));
                if (BuiltInRegistries.ENTITY_TYPE.containsKey(feralId)) {
                    targetId = feralId;
                }
            }
            Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(targetId)
                    .map(type -> type.create(level)).orElse(null);
            if (entity instanceof Mob mob) {
                return mob;
            }
        }
        return null;
    }

    private static boolean hasMappedHost(LivingEntity host) {
        ResourceLocation hostId = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType());
        for (String mapping : Config.cothVictimParasites()) {
            String[] parts = mapping.split(";", -1);
            if (parts.length != 2 || !parts[0].trim().equals(hostId.toString())) {
                continue;
            }
            ResourceLocation targetId = ResourceLocation.tryParse(parts[1].trim());
            if (targetId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(targetId)) {
                return true;
            }
        }
        return false;
    }

    private static Mob createHijackedHost(LivingEntity host, ServerLevel level) {
        String hostId = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString();
        String targetPath = switch (hostId) {
            case "minecraft:blaze" -> "hi_blaze";
            case "minecraft:iron_golem" -> "hi_golem";
            case "minecraft:skeleton", "minecraft:stray", "minecraft:bogged",
                    "minecraft:wither_skeleton" -> "hi_skeleton";
            default -> null;
        };
        if (targetPath == null) {
            return null;
        }
        ResourceLocation targetId = new ResourceLocation(Csrp.MODID, targetPath);
        Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(targetId)
                .map(type -> type.create(level)).orElse(null);
        return entity instanceof Mob mob ? mob : null;
    }

    private static Mob createTierParasite(ServerLevel level, int value) {
        String[] pool = FERALS;
        if (value >= Config.disloCothTiersPrimitive()) {
            pool = PRIMITIVES;
        }
        if (value >= Config.disloCothTiersAdapted()) {
            pool = ADAPTED;
        }
        if (value >= Config.disloCothTiersPure()) {
            pool = PURE;
        }
        ResourceLocation id = new ResourceLocation(Csrp.MODID,
                pool[level.getRandom().nextInt(pool.length)]);
        Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                .map(type -> type.create(level)).orElse(null);
        return entity instanceof Mob mob ? mob : null;
    }

    private static boolean isInfectable(LivingEntity entity) {
        return entity.isAlive() && isConvertible(entity);
    }

    private static boolean isConvertible(LivingEntity entity) {
        return !(entity instanceof Parasite) && !entity.hasEffect(ModMobEffects.REPEL.get())
                && !(entity instanceof Player player && player.getAbilities().invulnerable);
    }
}
