package alku.csrp.item;

import alku.csrp.registry.ModEntities;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

public final class FalseAppleItem extends Item {
    public FalseAppleItem(Item.Properties properties) {
        super(properties.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).alwaysEdible().build(),
                Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(
                        java.util.List.of(new MobEffectInstance(MobEffects.NAUSEA, 200),
                                new MobEffectInstance(MobEffects.BLINDNESS, 600)))).build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, level, user);
        if (!level.isClientSide()) {
            for (int i = 0; i < 5; i++) {
                var buglin = ModEntities.BUGLIN.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                if (buglin != null) {
                    buglin.snapTo(user.getX() + (level.getRandom().nextDouble() - 0.5D) * 0.8D,
                            user.getY(), user.getZ() + (level.getRandom().nextDouble() - 0.5D) * 0.8D,
                            level.getRandom().nextFloat() * 360.0F, 0.0F);
                    level.addFreshEntity(buglin);
                }
            }
        }
        return result;
    }
}
