package alku.csrp.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** Emits the legacy concussion smoke trail until its final grounded ticks. */
public final class DodSmokeTrailMobEffect extends MarkerMobEffect {
    public DodSmokeTrailMobEffect() {
        super(false, 4210752);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        var effect = entity.getEffect(alku.csrp.registry.ModMobEffects.DOD_SMOKE_TRAIL);
        int remaining = effect == null ? 0 : effect.getDuration();
        if (entity.onGround() && remaining <= 10) {
            entity.removeEffect(alku.csrp.registry.ModMobEffects.DOD_SMOKE_TRAIL);
            return true;
        }
        level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getEyeHeight(),
                entity.getZ(), 6, 0.15D, 0.15D, 0.15D, 0.02D);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
