package alku.csrp.config;

import alku.csrp.Csrp;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.PrimitiveVariantEntity;
import alku.csrp.entity.PureParasiteEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class OriginalConfigEvents {
    private static final ResourceLocation HEALTH_MULTIPLIER = id("global_health_multiplier");
    private static final ResourceLocation ARMOR_MULTIPLIER = id("global_armor_multiplier");
    private static final ResourceLocation DAMAGE_MULTIPLIER = id("global_damage_multiplier");
    private static final ResourceLocation KNOCKBACK_MULTIPLIER = id("global_knockback_resistance_multiplier");

    private OriginalConfigEvents() {
    }

    @SubscribeEvent
    public static void applyOriginalMobProperties(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof LivingEntity entity)
                || !(entity instanceof Parasite)) {
            return;
        }
        float healthRatio = entity.getMaxHealth() <= 0.0F ? 1.0F : entity.getHealth() / entity.getMaxHealth();
        applyConfiguredMobAttributes(entity);
        applyMultiplier(entity.getAttribute(Attributes.MAX_HEALTH), HEALTH_MULTIPLIER,
                GeneralConfig.globalHealthMultiplier());
        applyMultiplier(entity.getAttribute(Attributes.ARMOR), ARMOR_MULTIPLIER,
                GeneralConfig.globalArmorMultiplier());
        applyMultiplier(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MULTIPLIER,
                GeneralConfig.globalDamageMultiplier());
        applyMultiplier(entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_MULTIPLIER,
                GeneralConfig.globalKnockbackResistanceMultiplier());
        double followRange = MobsConfig.followRange(entity);
        AttributeInstance follow = entity.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null && followRange >= 0.0D) {
            follow.setBaseValue(followRange);
        }
        entity.setHealth(entity.getMaxHealth() * healthRatio);
    }

    private static void applyConfiguredMobAttributes(LivingEntity entity) {
        if (entity instanceof PrimitiveVariantEntity primitive) {
            primitive.applyConfiguredAttributes();
        } else if (entity instanceof alku.csrp.entity.VisceraEntity viscera) {
            viscera.applyConfiguredAttributes();
        } else if (entity instanceof AdaptedVariantEntity adapted) {
            adapted.applyConfiguredAttributes();
        } else if (entity instanceof PureParasiteEntity pure) {
            pure.applyConfiguredAttributes();
        }
    }

    private static void applyMultiplier(AttributeInstance attribute, ResourceLocation id, double multiplier) {
        if (attribute == null) return;
        attribute.removeModifier(id);
        if (multiplier != 1.0D) {
            attribute.addPermanentModifier(new AttributeModifier(id, multiplier - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, path);
    }
}
