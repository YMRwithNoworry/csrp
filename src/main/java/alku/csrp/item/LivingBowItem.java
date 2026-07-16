package alku.csrp.item;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;

public final class LivingBowItem extends BowItem {
    public static final String DAMAGE = "srp_damage";
    private static final int EVOLUTION_DAMAGE = 50_000;
    private final boolean sentient;
    private final Supplier<? extends Item> next;

    public LivingBowItem(boolean sentient, Supplier<? extends Item> next, Item.Properties properties) {
        super(properties.durability(sentient ? 1500 : 1000));
        this.sentient = sentient;
        this.next = next;
    }

    public boolean isSentient() { return sentient; }
    public Supplier<? extends Item> next() { return next; }

    public void addDamage(ItemStack stack, float damage, LivingEntity holder) {
        if (holder.level().isClientSide) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(DAMAGE,
                tag.getInt(DAMAGE) + Math.round(damage)));
        CompoundData.evolve(stack, holder, sentient, next, DAMAGE, EVOLUTION_DAMAGE);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int damage = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(DAMAGE);
        tooltip.add(Component.translatable("tooltip.csrp.living_progress", damage, EVOLUTION_DAMAGE));
    }

    private static final class CompoundData {
        private static void evolve(ItemStack stack, LivingEntity holder, boolean sentient,
                Supplier<? extends Item> next, String key, int threshold) {
            if (sentient || next == null || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getInt(key) < threshold) return;
            ItemStack evolved = new ItemStack(next.get());
            stack.shrink(1);
            if (holder instanceof net.minecraft.world.entity.player.Player player && player.getInventory().add(evolved)) return;
            holder.spawnAtLocation(evolved);
        }
    }
}
