package alku.csrp.effect;

import alku.csrp.entity.PrimitiveParasiteEntity;
import net.minecraft.world.entity.LivingEntity;

/** Accelerates every damage-source resistance already learned by a malleable parasite. */
public final class FosterMobEffect extends MarkerMobEffect {
    public FosterMobEffect() {
        super(false, 5804908);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof PrimitiveParasiteEntity parasite) {
            parasite.increaseAllResistances();
        }
        return;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }
}
