package alku.csrp.effect;

import alku.csrp.infection.InfectionMechanics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class CothMobEffect extends MobEffect {
    public CothMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A1C1C);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        InfectionMechanics.tickCoth(entity, amplifier);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
