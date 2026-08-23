package alku.csrp.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Throwable cleansing compound used to reverse Beckon infestation. */
public final class QuenchItem extends Item {
    public QuenchItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            Snowball projectile = new Snowball(level, player);
            projectile.setItem(new ItemStack(this));
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.0F, 1.0F);
            level.addFreshEntity(projectile);
            level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW,
                    SoundSource.PLAYERS, 0.5F, 0.35F + level.random.nextFloat() * 0.15F);
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp.quench")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
