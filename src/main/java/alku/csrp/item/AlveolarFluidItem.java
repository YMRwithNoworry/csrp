package alku.csrp.item;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class AlveolarFluidItem extends Item {
    private static final int EFFECT_DURATION_TICKS = 600;

    public AlveolarFluidItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide) {
            user.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, EFFECT_DURATION_TICKS, 0));
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION_TICKS, 0));
            user.addEffect(new MobEffectInstance(ModMobEffects.VIRAL.get(), EFFECT_DURATION_TICKS, 2));
            // Legacy effect: ModMobEffects.VIRAL, EFFECT_DURATION_TICKS, 2.
        }
        if (user instanceof Player player) {
            if (player.getAbilities().instabuild) {
                return stack;
            }
            return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE), false);
        }
        stack.shrink(1);
        return stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
