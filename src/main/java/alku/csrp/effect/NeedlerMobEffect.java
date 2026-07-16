package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import alku.csrp.registry.ModMobEffects;

public final class NeedlerMobEffect extends MobEffect {
    public NeedlerMobEffect() { super(MobEffectCategory.HARMFUL, 0xD7B34B); }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && amplifier >= 7) {
            int remainder = amplifier - 7;
            entity.removeEffect(ModMobEffects.NEEDLER);
            if (remainder >= 0) {
                entity.addEffect(new MobEffectInstance(ModMobEffects.NEEDLER, 400, remainder, false, false));
            }
            float health = entity.getHealth() - entity.getMaxHealth() * 0.4F;
            entity.setHealth(Math.max(0.0F, health));
            if (health <= 0.0F) entity.hurt(entity.damageSources().magic(), Float.MAX_VALUE);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        return amplifier >= 7 && (interval <= 0 || duration % interval == 0);
    }
}
