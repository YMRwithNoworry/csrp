package alku.csrp.item;

import alku.csrp.util.NbtData;
import java.util.List;
import java.util.function.Supplier;
import alku.csrp.Config;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
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
                properties.durability(type.getDurability(1500)));
        this.sentient = sentient;
        this.next = next;
    }

    public LivingArmorItem(net.minecraft.core.Holder<ArmorMaterial> material, Type type, boolean sentient,
            Supplier<? extends Item> next, Item.Properties properties) {
        super(material, type, properties.durability(type.getDurability(1500)));
        this.sentient = sentient;
        this.next = next;
    }

    public boolean isSentient() { return sentient; }
    public Supplier<? extends Item> next() { return next; }

    public int damageTypeLimit() { return sentient ? 7 : 4; }
    public int pointLimit() { return sentient ? 13 : 18; }
    public float reductionPerPoint() { return sentient ? 0.018F : 0.0125F; }
    public float learningChance() { return sentient ? 0.50F : 0.20F; }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, Entity entity,
            int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof LivingEntity holder)) return;
        if (sentient && holder.tickCount % 40 == 0 && Config.evolutionPhase(level) >= 2
                && holder.getRandom().nextInt(10) == 0) {
            holder.addEffect(new MobEffectInstance(ModMobEffects.PREY.get(), 1200, 0, false, false));
        }
        if (!sentient && holder.tickCount % 80 == 0) evolveIfReady(stack, holder);
    }

    private void evolveIfReady(ItemStack stack, LivingEntity holder) {
        if (next == null || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(DAMAGE) < EVOLUTION_DAMAGE) return;
        NbtData.update(stack, tag -> tag.putInt(DAMAGE, 0));
        stack.shrink(1);
        var dropped = holder.spawnAtLocation(new ItemStack(next.get()));
        if (dropped != null) dropped.setUnlimitedLifetime();
        if (holder.level() instanceof ServerLevel serverLevel) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (lightning != null) {
                lightning.moveTo(holder.position());
                lightning.setVisualOnly(true);
                serverLevel.addFreshEntity(lightning);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        tooltip.add(Component.translatable("tooltip.csrp.living_progress", data.copyTag().getInt(DAMAGE), EVOLUTION_DAMAGE));
        var tag = data.copyTag();
        tooltip.add(Component.translatable("tooltip.csrp.adaptation", tag.getInt(ADAPT_COUNT), damageTypeLimit()));
        tag.getAllKeys().stream().filter(key -> key.startsWith("adapt_points_")).sorted().forEach(key -> {
            int points = Math.min(pointLimit(), tag.getInt(key));
            String source = key.substring("adapt_points_".length());
            tooltip.add(Component.translatable("tooltip.csrp.adaptation_entry", source,
                    points, pointLimit(), Math.floor(points * reductionPerPoint() * 10000.0F) / 100.0F));
        });
    }
}
