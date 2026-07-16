package alku.csrp.item;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public final class LivingArmorItem extends ArmorItem {
    public static final String DAMAGE = "srp_damage";
    public static final String ADAPT_COUNT = "srp_adapt_count";
    public static final int EVOLUTION_DAMAGE = 90_000;
    private final boolean sentient;
    private final Supplier<? extends Item> next;

    public LivingArmorItem(ArmorMaterial material, Type type, boolean sentient,
            Supplier<? extends Item> next, Item.Properties properties) {
        super(net.minecraft.core.Holder.direct(material), type,
                properties.durability(type.getDurability(sentient ? 1500 : 1000)));
        this.sentient = sentient;
        this.next = next;
    }

    public LivingArmorItem(net.minecraft.core.Holder<ArmorMaterial> material, Type type, boolean sentient,
            Supplier<? extends Item> next, Item.Properties properties) {
        super(material, type, properties.durability(type.getDurability(sentient ? 1500 : 1000)));
        this.sentient = sentient;
        this.next = next;
    }

    public boolean isSentient() { return sentient; }
    public Supplier<? extends Item> next() { return next; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        tooltip.add(Component.translatable("tooltip.csrp.living_progress", data.copyTag().getInt(DAMAGE), EVOLUTION_DAMAGE));
        tooltip.add(Component.translatable("tooltip.csrp.adaptation", data.copyTag().getInt(ADAPT_COUNT), sentient ? 7 : 4));
    }
}
