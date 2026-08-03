package alku.csrp.item;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import alku.csrp.registry.ModItems;

public final class OverlastCanteenItem extends Item {
    private final Dose dose;

    public OverlastCanteenItem(Dose dose, Properties properties) {
        super(properties);
        this.dose = dose;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (dose == Dose.EMPTY) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide && dose != Dose.EMPTY) {
            user.addEffect(switch (dose) {
                case PURIFY -> new MobEffectInstance(ModMobEffects.PARASITES_PURIFY, 1_800);
                case INFECT -> new MobEffectInstance(ModMobEffects.PARASITES_INFECT, 1_800);
                case STRONG_INFECT -> new MobEffectInstance(ModMobEffects.PARASITES_INFECT, 3_600, 1);
                case EMPTY -> throw new IllegalStateException("Empty canteen has no dose");
            });
            if (!(user instanceof Player player) || !player.getAbilities().instabuild) {
                int damage = stack.getDamageValue() + 1;
                if (damage >= stack.getMaxDamage()) {
                    return new ItemStack(ModItems.DRINKING_POTION.get());
                }
                stack.setDamageValue(damage);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return dose == Dose.EMPTY ? 0 : 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    public enum Dose {
        EMPTY,
        PURIFY,
        INFECT,
        STRONG_INFECT
    }
}
