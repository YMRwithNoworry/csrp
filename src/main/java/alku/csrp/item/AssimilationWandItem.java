package alku.csrp.item;
import net.minecraft.world.level.Level;

import alku.csrp.infection.InfectionMechanics;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Creative tool for forcing host assimilation and restoring the infected host disguise. */
public final class AssimilationWandItem extends Item {
    public AssimilationWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) {
            InfectionMechanics.forceAssimilate(target);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
            InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !InfectionMechanics.isAssimilatedBody(target)) {
            return InteractionResult.PASS;
        }
        if (target.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return InfectionMechanics.disguiseAssimilated(target)
                ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp.itemassimilate",
                Component.translatable("tooltip.csrp.itemassimilate.action").withStyle(ChatFormatting.RED)));
    }
}
