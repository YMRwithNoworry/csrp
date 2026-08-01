package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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
}
