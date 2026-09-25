package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Shared damage-based status calculations used by legacy parasite tiers. */
public final class ParasiteCombatEffects {
    private static final float FEAR_DAMAGE_THRESHOLD = 8.0F;

    private ParasiteCombatEffects() {
    }

    static float healthWithAbsorption(LivingEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    public static void applyFearFromDamage(LivingEntity target, float healthBefore, Entity source) {
        if (target.level().isClientSide) {
            return;
        }
        float dealt = Math.max(0.0F, healthBefore - healthWithAbsorption(target));
        if (dealt <= FEAR_DAMAGE_THRESHOLD) {
            return;
        }
        int level = Math.min(3, 1 + Math.max(0, Mth.floor((dealt - FEAR_DAMAGE_THRESHOLD) / 4.0F)));
        int duration = Mth.clamp(300 + 40 * (level - 1), 200, 500);
        target.addEffect(new MobEffectInstance(ModMobEffects.FEAR,
                duration, level - 1, false, true), source);
    }

    static float damageAfterKillingResistance(DamageSource source, float amount, Holder<MobEffect> effect) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return amount;
        }
        MobEffectInstance resistance = attacker.getEffect(effect);
        if (resistance == null) {
            return amount;
        }
        float reduction = Mth.clamp((float) Config.parasiteKillingReduction()
                * (resistance.getAmplifier() + 1), 0.0F, 0.95F);
        return Math.max(0.0F, amount * (1.0F - reduction));
    }

    static void spawnVomitCloud(LivingEntity owner, double forwardDistance, float radius,
                                int cloudDuration, int effectDuration, int severeAmplifier) {
        Vec3 direction = owner.getViewVector(1.0F);
        ToxicCloudEntity cloud = ToxicCloudEntity.create(owner.level(),
                owner.getX() + direction.x * forwardDistance, owner.getY(),
                owner.getZ() + direction.z * forwardDistance);
        cloud.setOwner(owner);
        cloud.setRadius(radius);
        cloud.setDuration(cloudDuration);
        cloud.setRadiusPerTick(-radius / cloudDuration);
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VOMIT, effectDuration, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, effectDuration,
                severeAmplifier, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effectDuration,
                severeAmplifier, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, effectDuration,
                severeAmplifier, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, effectDuration,
                severeAmplifier, false, true));
        owner.level().addFreshEntity(cloud);
    }
    /**
     * Legacy attackEntityAsMobMinimum: a parasite melee hit chips at least the tier's minimum
     * damage through armor, multiplied by the victim's VIRA amplifier. EntityParasiteBase computes
     * MinimumDamage * (amp + 1) while VIRA is present and adds the base value on top, which is the
     * same as base * (amp + 2) here.
     */
    public static void applyMinimumDamage(LivingEntity attacker, LivingEntity target, float base) {
        if (base <= 0.0F || target instanceof Parasite || target == attacker || !target.isAlive()
                || target instanceof Player player && player.getAbilities().instabuild
                || !(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        MobEffectInstance viral = target.getEffect(ModMobEffects.VIRAL);
        float amount = base * (viral == null ? 1.0F : viral.getAmplifier() + 2);
        float absorptionDamage = Math.min(target.getAbsorptionAmount(), amount * 0.5F);
        if (absorptionDamage > 0.0F) {
            target.setAbsorptionAmount(target.getAbsorptionAmount() - absorptionDamage);
        }
        target.setHealth(Math.max(0.0F, target.getHealth() - (amount - absorptionDamage)));
        serverLevel.broadcastEntityEvent(target, (byte) 2);
        if (target.getHealth() <= 0.0F) {
            target.die(attacker.damageSources().mobAttack(attacker));
        }
    }

    /**
     * Legacy attackEntityAsMob / attackEntityAsMobFood: hitting a player drains the tier's
     * foodSteal value as exhaustion and may convert one food item into assimilated flesh
     * (the original infected_drop) at the shared foodRott chance.
     */
    public static void stealFood(LivingEntity attacker, LivingEntity victim, float exhaustion, float theftChance) {
        if (!(victim instanceof Player player) || player.getAbilities().instabuild
                || !(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (exhaustion > 0.0F) {
            player.causeFoodExhaustion(exhaustion);
        }
        if (theftChance <= 0.0F || attacker.getRandom().nextDouble() >= theftChance) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) {
                continue;
            }
            stack.shrink(1);
            ItemEntity drop = new ItemEntity(serverLevel, player.getX(), player.getY(), player.getZ(),
                    new ItemStack(ModItems.ASSIMILATED_FLESH.get()));
            drop.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(drop);
            return;
        }
    }

    /**
     * Legacy geneMobHealing: a kill heals the killer for a share of the victim's maximum health,
     * gated by the generation profile mobHealing gene (0 below generation 2).
     */
    public static void healOnKill(LivingEntity killer, LivingEntity victim) {
        if (victim == null || !(killer.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float multiplier = EvolutionSystem.generationProfile(serverLevel).mobHealing();
        if (multiplier <= 0.0F) {
            return;
        }
        killer.heal(victim.getMaxHealth() * multiplier);
    }
}
