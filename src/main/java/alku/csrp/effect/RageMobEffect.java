package alku.csrp.effect;

import alku.csrp.Csrp;
import alku.csrp.particle.SrpParticleEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class RageMobEffect extends MobEffect {
    public RageMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xB51F1F);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                Identifier.fromNamespaceAndPath(Csrp.MODID, "rage_speed"), 0.1D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                Identifier.fromNamespaceAndPath(Csrp.MODID, "rage_damage"), 0.1D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        RandomSource random = level.getRandom();
        for (int index = 0; index < 4; index++) {
            if (random.nextFloat() < 0.1F) {
                SrpParticleEffects.spawnType12Tracking(entity);
            }
        }
        if (random.nextFloat() < 0.8F) {
            SrpParticleEffects.spawnType2Tracking(entity);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
