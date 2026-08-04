package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Applies the selected per-world SRP difficulty to every parasite instance. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class SrpDifficultyEvents {
    private static final ResourceLocation HEALTH = id("difficulty_health");
    private static final ResourceLocation DAMAGE = id("difficulty_damage");
    private static final ResourceLocation ARMOR = id("difficulty_armor");
    private static final ResourceLocation KNOCKBACK = id("difficulty_knockback");

    private SrpDifficultyEvents() {
    }

    @SubscribeEvent
    public static void applyDifficulty(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof LivingEntity entity)
                || !(entity instanceof Parasite)) {
            return;
        }

        SrpDifficulty difficulty = SrpWorldData.get(level).difficulty();
        float healthFraction = entity.getMaxHealth() <= 0.0F
                ? 1.0F : entity.getHealth() / entity.getMaxHealth();
        boolean healthChanged = multiply(entity.getAttribute(Attributes.MAX_HEALTH), HEALTH,
                difficulty.healthMultiplier());
        multiply(entity.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE, difficulty.damageMultiplier());
        multiply(entity.getAttribute(Attributes.ARMOR), ARMOR, difficulty.armorMultiplier());
        multiply(entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK,
                difficulty.knockbackMultiplier());
        if (difficulty == SrpDifficulty.IMPOSSIBLE) {
            add(entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK, 1.0D);
        }
        if (healthChanged) {
            entity.setHealth(Mth.clamp(entity.getMaxHealth() * healthFraction, 1.0F, entity.getMaxHealth()));
        }
    }

    private static boolean multiply(AttributeInstance attribute, ResourceLocation id, double multiplier) {
        if (attribute == null || multiplier == 1.0D || attribute.getModifier(id) != null) {
            return false;
        }
        attribute.addPermanentModifier(new AttributeModifier(id, multiplier - 1.0D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return true;
    }

    private static void add(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute == null || attribute.getModifier(id) != null) {
            return;
        }
        attribute.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, path);
    }
}
