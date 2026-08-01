package alku.csrp.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import alku.csrp.registry.ModMobEffects;

/** Deals periodic damage and spreads the legacy contamination effect nearby. */
public final class ContaminationMobEffect extends MarkerMobEffect {
    public ContaminationMobEffect() {
        super(true, 10350848);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || entity.tickCount % 40 != 0) {
            return true;
        }
        if (entity.getHealth() > 1.0F) {
            entity.hurt(entity.damageSources().magic(), 1.0F);
        }
        MobEffectInstance current = entity.getEffect(ModMobEffects.CONTAMINATION);
        int duration = current == null ? 0 : current.getDuration();
        if (duration <= 0) {
            return true;
        }
        AABB area = entity.getBoundingBox().inflate(4.0D, 3.0D, 4.0D);
        for (LivingEntity nearby : entity.level().getEntitiesOfClass(LivingEntity.class, area)) {
            EffectStacking.apply(nearby, ModMobEffects.CONTAMINATION, duration, amplifier);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }
}
