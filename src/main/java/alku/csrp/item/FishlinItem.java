package alku.csrp.item;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

public final class FishlinItem extends Item {
    public FishlinItem(Item.Properties properties) {
        super(properties.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.2F).alwaysEdible()
                .effect(() -> new MobEffectInstance(ModMobEffects.COTH, 4800, 1, false, false), 1.0F).build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, level, user);
        if (!level.isClientSide) user.hurt(level.damageSources().magic(), 8.0F);
        return result;
    }
}
