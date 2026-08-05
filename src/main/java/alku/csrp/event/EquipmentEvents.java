package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.item.HijackedArmorItem;
import alku.csrp.item.HijackedHitEffects;
import alku.csrp.item.LivingArmorItem;
import alku.csrp.item.LivingBowItem;
import alku.csrp.effect.EffectStacking;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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
        if (entity.level().isClientSide) return;

        if (entity instanceof Player player && wearsFullHijackedSet(player)) {
            player.removeEffect(ModMobEffects.BLEED);
            if (event.getSource().is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 5.5F);
        }

        boolean hasLivingArmor = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (entity.getItemBySlot(slot).getItem() instanceof LivingArmorItem) {
                hasLivingArmor = true;
                break;
            }
        }
        if (!hasLivingArmor) return;

        if (event.getSource().getEntity() != null
                && (event.getSource().is(DamageTypeTags.IS_FIRE) || entity.isOnFire())) {
            event.setAmount(event.getAmount() * 4.0F);
            return;
        }

        String damageType = adaptationSource(event);
        String legacyDamageType = event.getSource().getMsgId().replaceAll("[^a-zA-Z0-9_]", "_");
        float incomingDamage = event.getAmount();
        float totalReduction = 0.0F;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof LivingArmorItem armor)) continue;
            String key = "adapt_points_" + damageType;
            String legacyKey = "adapt_" + legacyDamageType;
            var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            int points = data.getInt(key);
            if (points == 0 && data.getBoolean(legacyKey)) {
                points = 1;
                int migratedPoints = points;
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(key, migratedPoints));
                data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            }
            boolean canLearn = points > 0 || data.getInt(LivingArmorItem.ADAPT_COUNT) < armor.damageTypeLimit();
            if (canLearn && points < armor.pointLimit() && entity.getRandom().nextFloat() < armor.learningChance()) {
                boolean newType = points == 0;
                int learned = points + 1;
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    tag.putInt(key, learned);
                    if (newType) tag.putInt(LivingArmorItem.ADAPT_COUNT,
                            tag.getInt(LivingArmorItem.ADAPT_COUNT) + 1);
                });
                points = learned;
            }
            totalReduction += Math.min(points, armor.pointLimit()) * armor.reductionPerPoint();
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(LivingArmorItem.DAMAGE,
                    tag.getInt(LivingArmorItem.DAMAGE) + Math.round(incomingDamage)));
        }
        event.setAmount(incomingDamage * Math.max(0.0F, 1.0F - totalReduction));
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            EffectStacking.apply(attacker, ModMobEffects.COTH, 400, 2, 2);
        }
    }

    private static String adaptationSource(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player) return "player." + player.getName().getString();
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString();
        }
        return event.getSource().getMsgId();
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
        if (attacker != null && event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) {
            for (InteractionHandSlot hand : InteractionHandSlot.values()) {
                ItemStack held = hand.get(attacker);
                if (held.getItem() instanceof LivingBowItem bow) {
                    bow.addDamage(held, event.getNewDamage(), attacker);
                    break;
                }
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
