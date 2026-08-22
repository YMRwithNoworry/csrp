package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.entity.SimAdventurerEntity;
import alku.csrp.entity.ThrallEntity;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.SrpWorldData.GlobalAdaptation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/** Restores the original colony-wide spawn bonuses and adaptation exchange. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ColonyEvents {
    private static final String BONUSES_APPLIED = "csrp_colony_bonuses_applied";
    private static final String DAMAGE_CAP = "csrp_colony_damage_cap";

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

        multiplyBaseAttribute(entity, Attributes.MAX_HEALTH,
                points / (double) Config.colonyExtraHealthPoint() * Config.colonyExtraHealthValue());
        multiplyBaseAttribute(entity, Attributes.ARMOR,
                points / (double) Config.colonyExtraArmorPoint() * Config.colonyExtraArmorValue());
        multiplyBaseAttribute(entity, Attributes.ATTACK_DAMAGE,
                points / (double) Config.colonyExtraDamagePoint() * Config.colonyExtraDamageValue());
        multiplyBaseAttribute(entity, Attributes.KNOCKBACK_RESISTANCE,
                points / (double) Config.colonyExtraKDPoint() * Config.colonyExtraKDValue());
        entity.setHealth(entity.getMaxHealth());

        int damageCap = (int) (1.0D + points / (double) Config.colonyDamageCapPoint()
                * Config.colonyDamageCapValue());
        entity.getPersistentData().putInt(DAMAGE_CAP, Math.max(1, damageCap));
        if (entity instanceof PrimitiveParasiteEntity adaptable && adaptable.supportsDamageAdaptation()) {
            GlobalAdaptation common = data.mostCommonGlobalAdaptation();
            adaptable.seedGlobalAdaptation(common.damage(), common.points());
        }
    }

    @SubscribeEvent
    public static void preventParasiteInfighting(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Parasite
                && event.getNewTarget() instanceof Parasite) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    @SubscribeEvent
    public static void capIncomingDamage(LivingAttackEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        Entity directEntity = event.getSource().getDirectEntity();
        if (event.getEntity() instanceof Parasite
                && ((sourceEntity instanceof Parasite && sourceEntity != event.getEntity())
                || (directEntity instanceof Parasite && directEntity != event.getEntity()))) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof Parasite) || event.getAmount() <= 0.0F) {
            return;
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
        if (parasiteEntity instanceof PrimitiveParasiteEntity parasite
                && parasite.supportsDamageAdaptation()) {
            contributePrimitiveAdaptation(parasite, level);
        }
    }

    @SubscribeEvent
    public static void spawnAdventurerFromColonyThrall(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ThrallEntity thrall)
                || !(thrall.level() instanceof ServerLevel level)
                || SrpWorldData.get(level).colonies().isEmpty()) {
            return;
        }
        SimAdventurerEntity adventurer = ModEntities.SIM_ADVENTURER.get().create(level);
        if (adventurer == null) {
            return;
        }
        adventurer.moveTo(thrall.getX(), thrall.getY(), thrall.getZ(), thrall.getYRot(), thrall.getXRot());
        adventurer.finalizeSpawn(level, level.getCurrentDifficultyAt(thrall.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        adventurer.setCustomName(thrall.getCustomName());
        adventurer.setCustomNameVisible(thrall.isCustomNameVisible());
        if (thrall.isPersistenceRequired()) {
            adventurer.setPersistenceRequired();
        }
        level.addFreshEntity(adventurer);
    }

    private static void contributePrimitiveAdaptation(PrimitiveParasiteEntity parasite, ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        boolean inColonyRange = data.nearestColonyInEffectRange(parasite.blockPosition()) != null;
        MobEffectInstance link = parasite.getEffect(ModMobEffects.LINK.get());
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

    private static void multiplyBaseAttribute(LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double bonus) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            double base = instance.getBaseValue();
            instance.setBaseValue(base + base * bonus);
        }
    }
}
