package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * The generation (迭代) combat genes shared by every parasite: the armor-bypassing minimum damage
 * and the damage-cap black list of the original {@code EntityParasiteBase}.
 */
public final class GenerationCombat {
    private GenerationCombat() {
    }

    /**
     * Original {@code EntityParasiteBase#attackEntityAsMobMinimum}: every melee hit of a parasite
     * adds its tier minimum damage on top of the ordinary attack, amplified by the Viral effect and
     * split between absorption and health. Locked until the generation unlocks "Minimum Damage".
     *
     * @return true when damage was applied
     */
    public static boolean applyMinimumDamage(LivingEntity attacker, LivingEntity target, float baseDamage) {
        if (attacker == null || target == null || target == attacker || baseDamage <= 0.0F
                || !target.isAlive() || target.level() != attacker.level()
                || !(attacker.level() instanceof ServerLevel level)) {
            return false;
        }
        if (target instanceof Parasite || !EvolutionSystem.generationProfile(level).minimumDamage()) {
            return false;
        }
        if (target instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        float health = target.getHealth();
        if (health <= 0.0F) {
            return false;
        }
        float damage = baseDamage;
        MobEffectInstance viral = target.getEffect(ModMobEffects.VIRAL.get());
        if (viral != null) {
            damage += baseDamage * (viral.getAmplifier() + 1);
        }
        float absorption = target.getAbsorptionAmount();
        if (absorption > 0.0F) {
            target.setHealth(health - damage * 0.5F);
            target.setAbsorptionAmount(Math.max(0.0F, absorption - damage * 0.5F));
        } else {
            target.setHealth(health - damage);
        }
        level.broadcastEntityEvent(target, (byte) 2);
        if (target.getHealth() <= 0.0F) {
            Entity source = attacker;
            target.die(attacker.damageSources().mobAttack(attacker));
            if (source instanceof LivingEntity living) {
                living.setLastHurtMob(target);
            }
        }
        return true;
    }

    /**
     * Original {@code EntityParasiteBase#hurt} damage-cap black list: blacklisted damage sources are
     * never reduced to {@code maxHealth / damageCap}.
     */
    public static boolean ignoresDamageCap(DamageSource source) {
        List<? extends String> blacklist = Config.damageCapBlackList();
        if (blacklist.isEmpty() || source == null) {
            return false;
        }
        String typeId = damageTypeId(source);
        if (typeId != null && blacklist.contains(typeId)) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity living && !living.getMainHandItem().isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(living.getMainHandItem().getItem()).toString();
            if (blacklist.contains(itemId)) {
                return true;
            }
        }
        if (direct != null
                && blacklist.contains(BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType()).toString())) {
            return true;
        }
        Entity cause = source.getEntity();
        return cause != null
                && blacklist.contains(BuiltInRegistries.ENTITY_TYPE.getKey(cause.getType()).toString());
    }

    private static String damageTypeId(DamageSource source) {
        ResourceKey<DamageType> key = source.typeHolder().unwrapKey().orElse(null);
        return key == null ? null : key.location().toString();
    }
}
