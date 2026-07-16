package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.item.HijackedArmorItem;
import alku.csrp.item.HijackedHitEffects;
import alku.csrp.item.LivingArmorItem;
import alku.csrp.item.LivingBowItem;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class EquipmentEvents {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private EquipmentEvents() {
    }

    @SubscribeEvent
    public static void adaptAndApplySetPenalty(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getAmount() <= 0.0F) return;

        if (entity instanceof Player player && wearsFullHijackedSet(player)) {
            player.removeEffect(ModMobEffects.BLEED);
            if (event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 5.5F);
        }

        String damageType = event.getSource().getMsgId().replaceAll("[^a-zA-Z0-9_]", "_");
        int learnedPoints = 0;
        float reductionPerPoint = 0.0F;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof LivingArmorItem armor)) continue;
            int limit = armor.isSentient() ? 7 : 4;
            float chance = armor.isSentient() ? 0.50F : 0.20F;
            reductionPerPoint = armor.isSentient() ? 0.018F : 0.0125F;
            String key = "adapt_" + damageType;
            var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!data.getBoolean(key) && data.getInt(LivingArmorItem.ADAPT_COUNT) < limit
                    && entity.getRandom().nextFloat() < chance) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putBoolean(key, true);
                    tag.putInt(LivingArmorItem.ADAPT_COUNT, tag.getInt(LivingArmorItem.ADAPT_COUNT) + 1);
                });
                data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            }
            if (data.getBoolean(key)) learnedPoints++;
        }
        if (learnedPoints > 0) event.setAmount(event.getAmount() * Math.max(0.0F, 1.0F - learnedPoints * reductionPerPoint));
    }

    @SubscribeEvent
    public static void recordDamageAndWeaponEffects(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null) weapon = ItemStack.EMPTY;
        if (attacker != null && !weapon.isEmpty() && isHijackedTool(weapon)) {
            HijackedHitEffects.apply(attacker, target);
        }
        if (!weapon.isEmpty() && weapon.getItem() instanceof LivingBowItem) {
            target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 200, 0, false, true));
        }

        if (attacker != null && event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) {
            for (InteractionHandSlot hand : InteractionHandSlot.values()) {
                ItemStack held = hand.get(attacker);
                if (held.getItem() instanceof LivingBowItem bow) {
                    bow.addDamage(held, event.getNewDamage(), attacker);
                    break;
                }
            }
        }

        if (target.level().isClientSide || event.getNewDamage() <= 0.0F) return;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!(stack.getItem() instanceof LivingArmorItem armor)) continue;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(LivingArmorItem.DAMAGE,
                    tag.getInt(LivingArmorItem.DAMAGE) + Math.round(event.getNewDamage())));
            int total = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getInt(LivingArmorItem.DAMAGE);
            if (!armor.isSentient() && armor.next() != null && total >= LivingArmorItem.EVOLUTION_DAMAGE) {
                ItemStack evolved = new ItemStack(armor.next().get());
                evolved.set(DataComponents.CUSTOM_DATA, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY));
                target.setItemSlot(slot, evolved);
            }
        }
    }

    @SubscribeEvent
    public static void protectHijackedBoots(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getDistance() < 6.0F) return;
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof HijackedArmorItem) || player.getCooldowns().isOnCooldown(boots.getItem())) return;
        event.setCanceled(true);
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        player.getCooldowns().addCooldown(boots.getItem(), 2400);
    }

    @SubscribeEvent
    public static void clearBleedForHijackedSet(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide && wearsFullHijackedSet(event.getEntity())) {
            event.getEntity().removeEffect(ModMobEffects.BLEED);
        }
    }

    private static boolean wearsFullHijackedSet(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!(player.getItemBySlot(slot).getItem() instanceof HijackedArmorItem)) return false;
        }
        return true;
    }

    private static boolean isHijackedTool(ItemStack stack) {
        return stack.is(ModItems.HIJACKED_IRON_SWORD) || stack.is(ModItems.HIJACKED_IRON_AXE)
                || stack.is(ModItems.HIJACKED_IRON_PICKAXE) || stack.is(ModItems.HIJACKED_IRON_SHOVEL)
                || stack.is(ModItems.HIJACKED_IRON_HOE);
    }

    private enum InteractionHandSlot {
        MAIN { @Override ItemStack get(LivingEntity entity) { return entity.getMainHandItem(); } },
        OFF { @Override ItemStack get(LivingEntity entity) { return entity.getOffhandItem(); } };
        abstract ItemStack get(LivingEntity entity);
    }
}
