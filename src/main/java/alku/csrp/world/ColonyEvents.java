package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.PrimitiveParasiteEntity;
import alku.csrp.world.SrpWorldData.GlobalAdaptation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
        }
    }

    @SubscribeEvent
    public static void capIncomingDamage(LivingIncomingDamageEvent event) {
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
        if (!(event.getEntity() instanceof PrimitiveParasiteEntity parasite)
                || !(parasite.level() instanceof ServerLevel level)
                || parasite.isOnFire()) {
            return;
        }
        SrpWorldData data = SrpWorldData.get(level);
        if (data.nearestColonyInEffectRange(parasite.blockPosition()) == null) {
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
