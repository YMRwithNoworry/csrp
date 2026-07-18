package alku.csrp.effect;

import alku.csrp.infection.InfectionMechanics;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class ViralMobEffect extends MobEffect {
    public ViralMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x00FF00);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        InfectionMechanics.tickViral(entity, amplifier);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(10, 40 >> Math.min(amplifier, 2));
        return duration % interval == 0;
    }
}
