package alku.csrp.item;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class OverlastFoodItem extends Item {
    private final Kind kind;

    public OverlastFoodItem(Kind kind, Properties properties) {
        super(properties.food(food(kind)));
        this.kind = kind;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, level, user);
        if (!level.isClientSide) {
            switch (kind) {
                case CHOCOLATE_SMOOTHIE -> user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2_000, 1));
                case POLLUTED_HERBAL_BOWL -> {
                    user.removeEffect(ModMobEffects.COTH.get());
                    user.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100));
                    user.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
                }
                case HERBAL_BOWL -> {
                    user.removeEffect(ModMobEffects.COTH.get());
                    user.removeEffect(ModMobEffects.VIRAL.get());
                    user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                }
                case DUMPLING -> user.addEffect(new MobEffectInstance(ModMobEffects.FORTUNATE.get(), 600));
                default -> {
                }
            }
        }
        return result;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return kind == Kind.DUMPLING ? 10 : super.getUseDuration(stack);
    }

    private static FoodProperties food(Kind kind) {
        FoodProperties.Builder builder = new FoodProperties.Builder();
        return switch (kind) {
            case CHOCOLATE_SMOOTHIE -> builder.nutrition(4).saturationMod(0.6F).build();
            case POLLUTED_HERBAL_BOWL, HERBAL_BOWL -> builder.nutrition(2).saturationMod(0.3F).build();
            case MELON_ICE -> builder.nutrition(4).saturationMod(0.4F).alwaysEat().build();
            case ICE_SUCKER -> builder.nutrition(2).saturationMod(0.2F).build();
            case DUMPLING -> builder.nutrition(4).saturationMod(0.2F).alwaysEat().build();
        };
    }

    public enum Kind {
        CHOCOLATE_SMOOTHIE,
        POLLUTED_HERBAL_BOWL,
        HERBAL_BOWL,
        MELON_ICE,
        ICE_SUCKER,
        DUMPLING
    }
}
