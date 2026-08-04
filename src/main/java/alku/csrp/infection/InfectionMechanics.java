package alku.csrp.infection;

import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.entity.FeralEndermanEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.SimAdventurerEntity;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.DislodgmentSystem;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

/** Server-side COTH and Viral infection progression, spread, and host conversion. */
public final class InfectionMechanics {
    public static final int COTH_BASE_DURATION_TICKS = 3_600;
    public static final int COTH_REFRESH_THRESHOLD_TICKS = 200;
    public static final int COTH_MAX_AMPLIFIER = 2;
    public static final double COTH_SPREAD_RADIUS = 4.0D;
    public static final float COTH_CONVERSION_HEALTH_FRACTION = 0.35F;
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
                || target.hasEffect(ModMobEffects.CAMOUFLAGE)
                && target.getRandom().nextFloat() < CAMOUFLAGE_RESIST_CHANCE) {
            return;
        }
        int durationFloor = Math.max(COTH_BASE_DURATION_TICKS, minimumDurationTicks);
        MobEffectInstance existing = target.getEffect(ModMobEffects.COTH);
        int amplifier = existing == null ? 0 : existing.getAmplifier();
        int duration = existing == null ? durationFloor : Math.max(existing.getDuration(), durationFloor);
        target.addEffect(new MobEffectInstance(ModMobEffects.COTH, duration, amplifier, false, false), source);
    }

    public static void tickCoth(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !isInfectable(entity)) {
            return;
        }
        MobEffectInstance coth = entity.getEffect(ModMobEffects.COTH);
        if (coth == null) {
            return;
        }

        int effectiveAmplifier = Math.min(COTH_MAX_AMPLIFIER, amplifier);
        if (Config.disloCothIgnoreAmplifier() && amplifier <= 1 && entity.tickCount % 20 == 0
                && entity.level() instanceof ServerLevel level
                && DislodgmentSystem.activeCodeValue(level, 0) > 0) {
            entity.addEffect(new MobEffectInstance(ModMobEffects.COTH, 6_666, 10, false, false));
            effectiveAmplifier = COTH_MAX_AMPLIFIER;
        }
        if (coth.getDuration() > 0 && coth.getDuration() <= COTH_REFRESH_THRESHOLD_TICKS) {
            effectiveAmplifier = Math.min(COTH_MAX_AMPLIFIER, effectiveAmplifier + 1);
            entity.addEffect(new MobEffectInstance(ModMobEffects.COTH, COTH_BASE_DURATION_TICKS,
                    effectiveAmplifier, false, false));
        }
        if (effectiveAmplifier >= 1) {
            spreadCoth(entity);
        }
        if (effectiveAmplifier >= COTH_MAX_AMPLIFIER
                && entity.getHealth() <= entity.getMaxHealth() * COTH_CONVERSION_HEALTH_FRACTION) {
            convertInfectedHost(entity);
        }
    }

    public static boolean convertInfectedHost(LivingEntity host) {
        if (host.level().isClientSide || host.isRemoved() || !isConvertible(host) || host instanceof Player
                || !(host.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Mob converted = createAssimilatedHost(host, serverLevel);
        if (converted == null) {
            return false;
        }

        replaceHost(host, converted, serverLevel);
        return true;
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

        replaceHost(host, converted, serverLevel);
        return true;
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
        replaceHost(host, converted, serverLevel);
        return true;
    }

    private static void replaceHost(LivingEntity host, Mob converted, ServerLevel serverLevel) {
        float healthFraction = host.getMaxHealth() <= 0.0F ? 1.0F : host.getHealth() / host.getMaxHealth();
        boolean assimilatedEnderman = host.getType() == EntityType.ENDERMAN
                && BuiltInRegistries.ENTITY_TYPE.getKey(converted.getType()).getPath().equals("sim_enderman");
        converted.moveTo(host.getX(), host.getY(), host.getZ(), host.getYRot(), host.getXRot());
        converted.setHealth(Math.max(1.0F, converted.getMaxHealth() * Math.max(0.1F, healthFraction)));
        converted.setCustomName(host.getCustomName());
        converted.setCustomNameVisible(host.isCustomNameVisible());
        converted.setPersistenceRequired();
        serverLevel.addFreshEntity(converted);
        if (assimilatedEnderman) {
            SrpWorldData.get(serverLevel).recordAssimilatedEnderman();
        }
        EvolutionSystem.addPoints(serverLevel, EvolutionSystem.VALUE_COTH, EvolutionSystem.PointSource.COTH);
        host.discard();
    }

    /** Converts every non-parasite mob killed by a parasite, using Moving Flesh as the fallback. */
    public static boolean convertKilledHost(LivingEntity host, Entity attacker) {
        if (!(attacker instanceof Parasite) || host.level().isClientSide || host instanceof Parasite
                || host instanceof Player || host.isRemoved()
                || !(host.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Mob converted = createMappedHost(host, serverLevel, false);
        if (converted == null) {
            converted = createHijackedHost(host, serverLevel);
        }
        if (converted == null) {
            converted = ModEntities.MOVINGFLESH.get().create(serverLevel);
        }
        if (converted == null) {
            return false;
        }

        replaceHost(host, converted, serverLevel);
        return true;
    }

    /** A COTH-infected player killed by a parasite leaves an Assimilated Adventurer behind. */
    public static boolean convertKilledPlayer(Player player, Entity attacker) {
        if (!(attacker instanceof Parasite) || !player.hasEffect(ModMobEffects.COTH)
                || player.level().isClientSide || player.isRemoved()
                || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        SimAdventurerEntity converted = ModEntities.SIM_ADVENTURER.get().create(serverLevel);
        if (converted == null) {
            return false;
        }
        converted.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        converted.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(player.blockPosition()),
                MobSpawnType.CONVERSION, null);
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
        EvolutionSystem.addPoints(serverLevel, EvolutionSystem.VALUE_COTH, EvolutionSystem.PointSource.COTH);
        return true;
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
        int dislodgmentTier = Config.disloCothTiers()
                ? DislodgmentSystem.activeCodeValue(level, 1) : 0;
        if (dislodgmentTier > 0) {
            Mob tierParasite = createTierParasite(level, dislodgmentTier);
            if (tierParasite != null) {
                return tierParasite;
            }
        }
        return createMappedHost(host, level, SrpWorldData.get(level).evolutionPhase() >= 7);
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
                ResourceLocation feralId = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
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
        ResourceLocation targetId = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, targetPath);
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
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                pool[level.getRandom().nextInt(pool.length)]);
        Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                .map(type -> type.create(level)).orElse(null);
        return entity instanceof Mob mob ? mob : null;
    }

    private static boolean isInfectable(LivingEntity entity) {
        return entity.isAlive() && isConvertible(entity);
    }

    private static boolean isConvertible(LivingEntity entity) {
        return !(entity instanceof Parasite) && !entity.hasEffect(ModMobEffects.REPEL)
                && !(entity instanceof Player player && player.getAbilities().invulnerable);
    }
}
