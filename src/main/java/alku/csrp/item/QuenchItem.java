package alku.csrp.item;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared quench action used by the converted recipe and future fluid interactions. */
public final class QuenchItem extends Item {
    public QuenchItem(Item.Properties properties) {
        super(properties);
    }

    public static void quench(LivingEntity entity) {
        entity.removeEffect(ModMobEffects.BLEED);
        entity.removeEffect(ModMobEffects.VIRAL);
        entity.removeEffect(ModMobEffects.RAGE);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            quench(player);
            stack.consume(1, player);
        }
        return InteractionResultHolder.consume(stack);
    }
}
