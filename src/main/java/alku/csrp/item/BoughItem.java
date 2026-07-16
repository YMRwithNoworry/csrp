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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class BoughItem extends Item {
    public BoughItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BLOCK; }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) { return 20; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide) {
            user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6, 255, false, false));
            user.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 6, 0, false, false));
            user.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        QuenchItem.quench(user);
        if (!level.isClientSide) user.hurt(level.damageSources().generic(), Math.max(1.0F, user.getMaxHealth()));
        return stack;
    }
}
