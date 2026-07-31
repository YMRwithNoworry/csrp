package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.SrpWorldData.GlobalAdaptation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Restores the original colony-wide spawn bonuses and adaptation exchange. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ColonyEvents {
    private static final String BONUSES_APPLIED = "csrp_colony_bonuses_applied";
    private static final String DAMAGE_CAP = "csrp_colony_damage_cap";
    private static final String DAMAGE_ADAPTATIONS = "csrp_damage_adaptations";
    private static final String COLONY_SPAWNED = "csrp_colony_spawned";

    private ColonyEvents() {
    }

    @SubscribeEvent
    public static void applySpawnBonuses(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity entity)
                || !(entity instanceof Parasite)
                || entity.getPersistentData().getBoolean(BONUSES_APPLIED)) {
            return;
        }

        SrpWorldData data = SrpWorldData.get(level);
        int points = data.totalColonyPoints();
        entity.getPersistentData().putBoolean(BONUSES_APPLIED, true);
        if (points <= 0) {
            return;
        }

        multiplyBaseAttribute(entity, Attributes.MAX_HEALTH, points / 20.0D * 0.1D);
        multiplyBaseAttribute(entity, Attributes.ARMOR, points / 20.0D * 0.1D);
        multiplyBaseAttribute(entity, Attributes.ATTACK_DAMAGE, points / 20.0D * 0.1D);
        multiplyBaseAttribute(entity, Attributes.KNOCKBACK_RESISTANCE, points / 20.0D * 0.1D);
        entity.setHealth(entity.getMaxHealth());

        int damageCap = (int) (1.0D + points / 15.0D * 0.5D);
        entity.getPersistentData().putInt(DAMAGE_CAP, Math.max(1, damageCap));
        if (entity instanceof PrimitiveParasiteEntity adaptable) {
            GlobalAdaptation common = data.mostCommonGlobalAdaptation();
            adaptable.seedGlobalAdaptation(common.damage(), common.points());
        } else {
            GlobalAdaptation common = data.mostCommonGlobalAdaptation();
            seedGenericAdaptation(entity, common.damage(), common.points());
        }
    }

    @SubscribeEvent
    public static void capIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Parasite) || event.getAmount() <= 0.0F) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity entity
                && !(entity instanceof PrimitiveParasiteEntity)
                && entity.level() instanceof ServerLevel level
                && EvolutionSystem.generationProfile(level).adaptation()) {
            applyGenericAdaptation(entity, event);
        }
        int cap = event.getEntity().getPersistentData().getInt(DAMAGE_CAP);
        if (cap <= 1) {
            return;
        }
        float maximum = event.getEntity().getMaxHealth() / cap
                + event.getEntity().getMaxHealth() % cap * 0.5F;
        event.setAmount(Math.min(event.getAmount(), maximum));
    }

    @SubscribeEvent
    public static void contributeGlobalAdaptation(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity parasiteEntity)
                || !(parasiteEntity instanceof Parasite)
                || !(parasiteEntity.level() instanceof ServerLevel level)
                || parasiteEntity.isOnFire()) {
            return;
        }
        if (parasiteEntity instanceof PrimitiveParasiteEntity parasite) {
            contributePrimitiveAdaptation(parasite, level);
        } else if (parasiteEntity instanceof LivingEntity entity) {
            contributeGenericAdaptation(entity, level);
        }
    }

    private static void contributePrimitiveAdaptation(PrimitiveParasiteEntity parasite, ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        boolean inColonyRange = data.nearestColonyInEffectRange(parasite.blockPosition()) != null;
        MobEffectInstance link = parasite.getEffect(ModMobEffects.LINK);
        boolean linkedChance = link != null
                && parasite.getRandom().nextDouble() < Config.adaptationChance() * (link.getAmplifier() + 1);
        if (!inColonyRange && !linkedChance) {
            return;
        }

        GlobalAdaptation common = data.mostCommonGlobalAdaptation();
        parasite.removeInheritedGlobalAdaptation(common.damage(), common.points());
        String damage = parasite.mostCommonAdaptedDamage();
        if (damage != null) {
            data.addGlobalResistance(damage);
        }
    }

    private static void seedGenericAdaptation(LivingEntity entity, String damage, int points) {
        if (damage == null || damage.isBlank() || points <= 0) {
            return;
        }
        CompoundTag adaptations = entity.getPersistentData().getCompound(DAMAGE_ADAPTATIONS);
        adaptations.putInt(damage, 1);
        entity.getPersistentData().put(DAMAGE_ADAPTATIONS, adaptations);
        entity.getPersistentData().putBoolean(COLONY_SPAWNED, true);
    }

    private static void applyGenericAdaptation(LivingEntity entity, LivingIncomingDamageEvent event) {
        String damage = PrimitiveParasiteEntity.damageTypeId(event.getSource());
        CompoundTag adaptations = entity.getPersistentData().getCompound(DAMAGE_ADAPTATIONS);
        int hits = adaptations.getInt(damage);
        float reduction = Math.min(8, hits) * 0.1F;
        event.setAmount(event.getAmount() * (1.0F - reduction));
        if (hits < 8) {
            adaptations.putInt(damage, hits + 1);
            entity.getPersistentData().put(DAMAGE_ADAPTATIONS, adaptations);
        }
    }

    private static void contributeGenericAdaptation(LivingEntity entity, ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        boolean inColonyRange = data.nearestColonyInEffectRange(entity.blockPosition()) != null;
        MobEffectInstance link = entity.getEffect(ModMobEffects.LINK);
        boolean linkedChance = link != null
                && entity.getRandom().nextDouble() < Config.adaptationChance() * (link.getAmplifier() + 1);
        if (!inColonyRange && !linkedChance) {
            return;
        }
        CompoundTag adaptations = entity.getPersistentData().getCompound(DAMAGE_ADAPTATIONS);
        GlobalAdaptation common = data.mostCommonGlobalAdaptation();
        if (entity.getPersistentData().getBoolean(COLONY_SPAWNED) && common.damage() != null) {
            int inherited = adaptations.getInt(common.damage());
            if (inherited <= common.points()) {
                adaptations.remove(common.damage());
            } else {
                adaptations.putInt(common.damage(), inherited - common.points());
            }
        }
        String damage = null;
        int points = 0;
        for (String key : adaptations.getAllKeys()) {
            int value = adaptations.getInt(key);
            if (value > points) {
                damage = key;
                points = value;
            }
        }
        if (damage != null) {
            data.addGlobalResistance(damage);
        }
    }

    private static void multiplyBaseAttribute(LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double bonus) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            double base = instance.getBaseValue();
            instance.setBaseValue(base + base * bonus);
        }
    }
}
