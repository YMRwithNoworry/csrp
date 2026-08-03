package alku.csrp.item;

import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class InjectedPurifierItem extends Item {
    public InjectedPurifierItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level instanceof ServerLevel serverLevel && user instanceof Player player) {
            serverLevel.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(2.0D),
                            target -> target instanceof Parasite || target instanceof Animal || target instanceof Villager)
                    .stream().min(Comparator.comparingDouble(user::distanceToSqr))
                    .ifPresent(target -> target.addEffect(new MobEffectInstance(ModMobEffects.PARASITES_PURIFY,
                            target instanceof Parasite ? 600 : target instanceof Villager ? 6_000 : 18_000)));
            stack.consume(1, player);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 20;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }
}
