package alku.csrp.item;

import alku.csrp.registry.ModMobEffects;
import alku.csrp.entity.Parasite;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;

public final class HijackedHitEffects {
    public static final int BLEED_TICKS = 100;
    public static final int RAGE_TICKS = 60;
    public static final float PARASITE_BONUS_DAMAGE = 3.0F;

    private HijackedHitEffects() {
    }

    public static void apply(LivingEntity attacker, LivingEntity target) {
        if (target == null || target.isDeadOrDying()) return;
        target.addEffect(new MobEffectInstance(ModMobEffects.BLEED.get(), BLEED_TICKS, 0, false, true));
        if (target instanceof Parasite) target.hurt(attacker.damageSources().mobAttack(attacker), PARASITE_BONUS_DAMAGE);
        if (target.getHealth() <= target.getMaxHealth() * 0.1F) {
            target.addEffect(new MobEffectInstance(ModMobEffects.RAGE.get(), RAGE_TICKS, 0, false, true));
        }
    }
}
