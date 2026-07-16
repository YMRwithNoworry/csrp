package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class BleedMobEffect extends MobEffect {
    public BleedMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            float damage = Math.min(entity.getMaxHealth() * 0.02F, 100.0F);
            entity.hurt(entity.damageSources().magic(), damage);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = amplifier >= 4 ? 10 : amplifier >= 2 ? 12 : 25;
        return duration % interval == 0;
    }
}
