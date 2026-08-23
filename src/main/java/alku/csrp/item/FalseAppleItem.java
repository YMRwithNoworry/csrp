package alku.csrp.item;

import alku.csrp.registry.ModEntities;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

public final class FalseAppleItem extends Item {
    public FalseAppleItem(Item.Properties properties) {
        // Legacy FoodProperties.Builder.saturationModifier(0.3F) maps to Forge's saturationMod.
        super(properties.food(new FoodProperties.Builder().nutrition(4).saturationMod(0.3F).alwaysEat()
                .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200), 1.0F)
                .effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 600), 1.0F).build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, level, user);
        if (!level.isClientSide) {
            for (int i = 0; i < 5; i++) {
                var buglin = ModEntities.BUGLIN.get().create(level);
                if (buglin != null) {
                    buglin.moveTo(user.getX() + (level.random.nextDouble() - 0.5D) * 0.8D,
                            user.getY(), user.getZ() + (level.random.nextDouble() - 0.5D) * 0.8D,
                            level.random.nextFloat() * 360.0F, 0.0F);
                    level.addFreshEntity(buglin);
                }
            }
        }
        return result;
    }
}
