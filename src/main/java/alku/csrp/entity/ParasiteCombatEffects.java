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
final class ParasiteCombatEffects {
    private static final float FEAR_DAMAGE_THRESHOLD = 8.0F;

    private ParasiteCombatEffects() {
    }

    static float healthWithAbsorption(LivingEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    static void applyFearFromDamage(LivingEntity target, float healthBefore, Entity source) {
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
     * Legacy damageCap (EntityParasiteBase.attackEntityFrom): damage at or above
     * maxHealth / cap + maxHealth % cap * 0.5 is clamped to that value and grants RAGE II for
     * 200 ticks. Fire and void damage stay uncapped, and the cap only applies while the current
     * generation profile grants the gene.
     */
    static float damageAfterIncomingCap(LivingEntity self, DamageSource source, float amount) {
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return amount;
        }
        int cap = Config.infectedDamageCap();
        if (cap <= 1 || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || !EvolutionSystem.generationProfile(serverLevel).damageCap()) {
            return amount;
        }
        float maximumHealth = self.getMaxHealth();
        float capped = maximumHealth / cap + maximumHealth % cap * 0.5F;
        if (amount >= capped && !self.hasEffect(ModMobEffects.RAGE)) {
            self.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 200, 1, false, false), self);
        }
        return Math.min(amount, capped);
    }

    /**
     * Legacy attackEntityAsMobMinimum: assimilated melee always chips at least the configured
     * minimum damage through armor, multiplied by the victim's VIRA amplifier (+2), the way
     * EntityPInfected passes MiniDamage into the base implementation.
     */
    static void applyMinimumMeleeDamage(LivingEntity attacker, LivingEntity target) {
        float base = Config.infectedMinimumDamage();
        if (base <= 0.0F || target instanceof Parasite || target == attacker || !target.isAlive()
                || target instanceof Player player && player.getAbilities().instabuild
                || !(attacker.level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel).minimumDamage()) {
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
     * Legacy attackEntityAsMobFood: a hit may steal one food item out of the victim's inventory and
     * drop it as assimilated flesh (the original infected_drop).
     */
    static void stealFoodFromPlayer(LivingEntity attacker, LivingEntity target) {
        if (!(target instanceof Player player) || player.getAbilities().instabuild
                || !(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float chance = Config.infectedFoodSteal();
        if (chance <= 0.0F || attacker.getRandom().nextDouble() >= chance) {
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
     * Legacy geneMobHealing: a kill heals the killer for a share of the victim's maximum health.
     * The share is the generation profile's mobHealing gene, which is 0 below generation 2, so low
     * generation parasites do not regenerate from kills.
     */
    static void healOnKill(LivingEntity killer, LivingEntity victim) {
        if (victim == null || !(killer.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float multiplier = EvolutionSystem.generationProfile(serverLevel).mobHealing();
        if (multiplier <= 0.0F) {
            return;
        }
        killer.heal(victim.getMaxHealth() * multiplier * Config.infectedKillHeal());
    }
}
