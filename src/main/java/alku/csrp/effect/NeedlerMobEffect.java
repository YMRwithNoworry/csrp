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
    public static final int TERMINAL_AMPLIFIER = 7;
    public static final float DAMAGE_FRACTION = 0.4F;
    public static final float MAX_DAMAGE = 1_000_000_000.0F;

    public NeedlerMobEffect() { super(MobEffectCategory.HARMFUL, 0xD7B34B); }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && amplifier >= TERMINAL_AMPLIFIER) {
            int remainder = amplifier - TERMINAL_AMPLIFIER;
            entity.removeEffect(ModMobEffects.NEEDLER);
            entity.addEffect(new MobEffectInstance(ModMobEffects.NEEDLER, 400, remainder, false, false));
            if (entity.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, entity.getX(),
                        entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.HOSTILE, 1.0F, 1.0F);
            }
            float damage = Math.min(entity.getMaxHealth() * DAMAGE_FRACTION, MAX_DAMAGE);
            entity.setHealth(Math.max(1.0F, entity.getHealth() - damage));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        return amplifier >= TERMINAL_AMPLIFIER && (interval <= 0 || duration % interval == 0);
    }
}
