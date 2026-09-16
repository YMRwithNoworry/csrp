package alku.csrp.item;

import java.util.function.Consumer;
import java.util.function.Supplier;
import alku.csrp.Config;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

public final class LivingArmorItem extends Item {
    public static final String DAMAGE = "srp_damage";
    public static final String ADAPT_COUNT = "srp_adapt_count";
    public static final int EVOLUTION_DAMAGE = 90_000;
    private final boolean sentient;
    private final Supplier<? extends Item> next;

    public LivingArmorItem(ArmorMaterial material, ArmorType type, boolean sentient,
            Supplier<? extends Item> next, Item.Properties properties) {
        this(net.minecraft.core.Holder.direct(material), type, sentient, next, properties);
    }

    public LivingArmorItem(net.minecraft.core.Holder<ArmorMaterial> material, ArmorType type, boolean sentient,
            Supplier<? extends Item> next, Item.Properties properties) {
        super(properties.humanoidArmor(material.value(), type).durability(type.getDurability(1500)));
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
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (level.isClientSide() || !(entity instanceof LivingEntity holder)) return;
        if (sentient && holder.tickCount % 40 == 0 && Config.evolutionPhase(level) >= 2
                && holder.getRandom().nextInt(10) == 0) {
            holder.addEffect(new MobEffectInstance(ModMobEffects.PREY, 1200, 0, false, false));
        }
        if (!sentient && holder.tickCount % 80 == 0) evolveIfReady(stack, holder);
    }

    private void evolveIfReady(ItemStack stack, LivingEntity holder) {
        if (next == null || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr(DAMAGE, 0) < EVOLUTION_DAMAGE) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(DAMAGE, 0));
        stack.shrink(1);
        if (!(holder.level() instanceof ServerLevel serverLevel)) return;
        var dropped = holder.spawnAtLocation(serverLevel, new ItemStack(next.get()));
        if (dropped != null) dropped.setUnlimitedLifetime();
        LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.EVENT);
        if (lightning != null) {
            lightning.snapTo(holder.position());
            lightning.setVisualOnly(true);
            serverLevel.addFreshEntity(lightning);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        tooltip.accept(Component.translatable("tooltip.csrp.living_progress", data.copyTag().getIntOr(DAMAGE, 0), EVOLUTION_DAMAGE));
        var tag = data.copyTag();
        tooltip.accept(Component.translatable("tooltip.csrp.adaptation", tag.getIntOr(ADAPT_COUNT, 0), damageTypeLimit()));
        tag.keySet().stream().filter(key -> key.startsWith("adapt_points_")).sorted().forEach(key -> {
            int points = Math.min(pointLimit(), tag.getIntOr(key, 0));
            String source = key.substring("adapt_points_".length());
            tooltip.accept(Component.translatable("tooltip.csrp.adaptation_entry", source,
                    points, pointLimit(), Math.floor(points * reductionPerPoint() * 10000.0F) / 100.0F));
        });
    }
}
