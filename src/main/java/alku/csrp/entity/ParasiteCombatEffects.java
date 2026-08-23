package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        target.addEffect(new MobEffectInstance(ModMobEffects.FEAR.get(),
                duration, level - 1, false, true), source);
    }

    static float damageAfterKillingResistance(DamageSource source, float amount, MobEffect effect) {
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
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VOMIT.get(), effectDuration, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL.get(), effectDuration,
                severeAmplifier, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effectDuration,
                severeAmplifier, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, effectDuration,
                severeAmplifier, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.CORROSION.get(), effectDuration,
                severeAmplifier, false, true));
        owner.level().addFreshEntity(cloud);
    }
}
