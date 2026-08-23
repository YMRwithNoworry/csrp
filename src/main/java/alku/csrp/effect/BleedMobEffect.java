package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class BleedMobEffect extends MobEffect {
    public BleedMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            float damage = entity.getMaxHealth() * 0.06F;
            if (entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D) {
                damage *= 2.0F;
            }
            damage = Math.min(damage, 100.0F);
            entity.hurt(entity.damageSources().magic(), damage);
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }
}
