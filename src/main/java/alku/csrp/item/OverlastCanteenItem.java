package alku.csrp.item;

import java.util.List;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class OverlastCanteenItem extends Item {
    public static final int MAX_SIPS = 6;
    public static final int MAX_CANTEEN_DURABILITY = 30;
    private static final String SIPS_TAG = "sips";
    private static final String DURABILITY_TAG = "durability";

    private final Dose dose;

    public OverlastCanteenItem(Dose dose, Properties properties) {
        super(properties);
        this.dose = dose;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (dose == Dose.EMPTY || getSips(stack) <= 0) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level.isClientSide || dose == Dose.EMPTY || !(user instanceof Player player)) {
            return stack;
        }

        player.addEffect(switch (dose) {
            case PURIFY -> new MobEffectInstance(ModMobEffects.PARASITES_PURIFY, 1_800);
            case INFECT -> new MobEffectInstance(ModMobEffects.PARASITES_INFECT, 1_800);
            case STRONG_INFECT -> new MobEffectInstance(ModMobEffects.PARASITES_INFECT, 3_600, 1);
            case EMPTY -> throw new IllegalStateException("Empty canteen has no dose");
        });

        int sips = getSips(stack) - 1;
        int durability = getCanteenDurability(stack) - 1;
        if (durability <= 0) {
            return ItemStack.EMPTY;
        }
        if (sips <= 0) {
            ItemStack emptyCanteen = new ItemStack(ModItems.DRINKING_POTION.get());
            setState(emptyCanteen, 0, durability);
            return emptyCanteen;
        }

        setState(stack, sips, durability);
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

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getSips(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Mth.clamp((float) getSips(stack) / MAX_SIPS, 0.0F, 1.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float remaining = Mth.clamp((float) getSips(stack) / MAX_SIPS, 0.0F, 1.0F);
        return Mth.hsvToRgb(remaining / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        int sips = getSips(stack);
        ChatFormatting sipColor = sips >= 5 ? ChatFormatting.GREEN
                : sips >= 2 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        tooltip.add(Component.translatable("tooltip.csrp.canteen.sips", sips).withStyle(sipColor));
        tooltip.add(Component.translatable("tooltip.csrp.canteen.durability", getCanteenDurability(stack))
                .withStyle(ChatFormatting.GOLD));
    }

    public static int getSips(ItemStack stack) {
        if (!(stack.getItem() instanceof OverlastCanteenItem canteen)) {
            return 0;
        }
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.contains(SIPS_TAG) ? data.copyTag().getInt(SIPS_TAG)
                : canteen.dose == Dose.EMPTY ? 0 : Math.max(0, MAX_SIPS - stack.getDamageValue());
    }

    public static int getCanteenDurability(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.contains(DURABILITY_TAG) ? data.copyTag().getInt(DURABILITY_TAG)
                : Math.max(0, MAX_CANTEEN_DURABILITY - stack.getDamageValue());
    }

    public static void setState(ItemStack stack, int sips, int durability) {
        stack.remove(DataComponents.DAMAGE);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(SIPS_TAG, sips);
            tag.putInt(DURABILITY_TAG, durability);
        });
    }

    public enum Dose {
        EMPTY,
        PURIFY,
        INFECT,
        STRONG_INFECT
    }
}
