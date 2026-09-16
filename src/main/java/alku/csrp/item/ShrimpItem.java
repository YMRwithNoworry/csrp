package alku.csrp.item;

import alku.csrp.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class ShrimpItem extends Item {
    public ShrimpItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(2)
                .saturationModifier(0.2F)
                .build(), Consumable.builder().sound(ModSounds.SHRIMP_EAT).build()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.csrp.shrimp.delicacy")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        builder.accept(Component.translatable("tooltip.csrp.shrimp.arrow")
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.csrp.shrimp.enderman")
                .withStyle(ChatFormatting.GRAY));
    }
}
