package alku.csrp.item;

import alku.csrp.block.ThornshadeBlock;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Edible Thornshade fruit which also plants a seedling on a block's top face. */
public final class ThornshadeBerryItem extends Item {
    public ThornshadeBerryItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        var targetPos = context.getClickedPos().above();
        ThornshadeBlock block = ModBlocks.THORNSHADE.get();
        var placed = block.initialState(level, targetPos);
        if (!level.getBlockState(targetPos).canBeReplaced() || !placed.canSurvive(level, targetPos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(targetPos, placed, 11);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide) {
            if (user instanceof Player player) {
                player.getFoodData().eat(2, 0.1F);
            }
            user.invulnerableTime = 0;
            user.hurt(level.damageSources().magic(), 4.0F);
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EAT,
                    SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
            level.playSound(null, user.blockPosition(), ModSounds.MOVING_FLESH_GROW.get(),
                    SoundSource.PLAYERS, 0.6F, 1.0F);
        }
        if (!(user instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 10;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp.thornshade_berry")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
