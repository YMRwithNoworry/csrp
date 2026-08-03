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
                if (stack.isDamageableItem()) {
                    int minimumRemaining = Math.max(1, (int) Math.ceil(stack.getMaxDamage() * 0.1D));
                    int remaining = stack.getMaxDamage() - stack.getDamageValue();
                    int damage = Math.min(3 * (amplifier + 1), remaining - minimumRemaining);
                    if (damage > 0) {
                        stack.hurtAndBreak(damage, entity, slot);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
