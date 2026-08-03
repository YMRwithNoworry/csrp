package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
            if (entity.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, entity.getX(),
                        entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.HOSTILE, 1.0F, 1.0F);
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
