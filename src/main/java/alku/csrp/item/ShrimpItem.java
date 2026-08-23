package alku.csrp.item;
import net.minecraft.world.level.Level;

import alku.csrp.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class ShrimpItem extends Item {
    public ShrimpItem(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(2)
                .saturationMod(0.2F)
                .build()));
    }

    @Override
    public SoundEvent getEatingSound() {
        return ModSounds.get("shrimp.eat");
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.csrp.shrimp.delicacy")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.csrp.shrimp.arrow")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.csrp.shrimp.enderman")
                .withStyle(ChatFormatting.GRAY));
    }
}
