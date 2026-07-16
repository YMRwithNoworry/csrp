package alku.csrp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;

public final class CorrosionMobEffect extends MobEffect {
    public CorrosionMobEffect() { super(MobEffectCategory.HARMFUL, 0x6B7A24); }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            for (EquipmentSlot slot : new EquipmentSlot[] {
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
            }) {
                var stack = entity.getItemBySlot(slot);
                if (stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() > stack.getMaxDamage() * 0.1F) {
                    stack.hurtAndBreak(3, entity, slot);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }
}
