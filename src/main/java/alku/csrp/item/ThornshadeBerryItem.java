package alku.csrp.item;

import alku.csrp.block.ThornshadeBlock;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.TooltipDisplay;
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
        if (!level.isClientSide()) {
            level.setBlock(targetPos, placed, 11);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME.heldItemTransformedTo(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide()) {
            if (user instanceof Player player) {
                player.getFoodData().eat(2, 0.1F);
            }
            user.setInvulnerableTime(0);
            user.hurt(level.damageSources().magic(), 4.0F);
            level.playSound(null, user.blockPosition(), SoundEvents.GENERIC_EAT.value(),
                    SoundSource.PLAYERS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            level.playSound(null, user.blockPosition(), ModSounds.MOVING_FLESH_GROW.get(),
                    SoundSource.PLAYERS, 0.6F, 1.0F);
        }
        if (!(user instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 10;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.csrp.thornshade_berry")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
