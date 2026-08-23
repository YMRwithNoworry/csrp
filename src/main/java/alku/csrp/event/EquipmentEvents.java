package alku.csrp.event;

import alku.csrp.util.NbtData;
import alku.csrp.Csrp;
import alku.csrp.item.HijackedArmorItem;
import alku.csrp.item.HijackedHitEffects;
import alku.csrp.item.LivingArmorItem;
import alku.csrp.item.LivingBowItem;
import alku.csrp.effect.EffectStacking;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class EquipmentEvents {
    private static final float LIVING_POINT_REDUCTION = 0.0125F;
    private static final float SENTIENT_POINT_REDUCTION = 0.018F;
    private static final float LIVING_LEARNING_CHANCE = 0.20F;
    private static final float SENTIENT_LEARNING_CHANCE = 0.50F;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private EquipmentEvents() {
    }

    @SubscribeEvent
    public static void adaptAndApplySetPenalty(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getAmount() <= 0.0F) return;
        if (entity.level().isClientSide) return;

        if (entity instanceof Player player && wearsFullHijackedSet(player)) {
            player.removeEffect(ModMobEffects.BLEED.get());
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
            var data = NbtData.copyTag(stack);
            int points = data.getInt(key);
            if (points == 0 && data.getBoolean(legacyKey)) {
                points = 1;
                int migratedPoints = points;
                NbtData.update(stack, tag -> tag.putInt(key, migratedPoints));
                data = NbtData.copyTag(stack);
            }
            boolean canLearn = points > 0 || data.getInt(LivingArmorItem.ADAPT_COUNT) < armor.damageTypeLimit();
            float learningChance = armor.isSentient() ? SENTIENT_LEARNING_CHANCE : LIVING_LEARNING_CHANCE;
            if (canLearn && points < armor.pointLimit() && entity.getRandom().nextFloat() < learningChance) {
                boolean newType = points == 0;
                int learned = points + 1;
                NbtData.update(stack, tag -> {
                    tag.putInt(key, learned);
                    if (newType) tag.putInt(LivingArmorItem.ADAPT_COUNT,
                            tag.getInt(LivingArmorItem.ADAPT_COUNT) + 1);
                });
                points = learned;
            }
            float reductionPerPoint = armor.isSentient() ? SENTIENT_POINT_REDUCTION : LIVING_POINT_REDUCTION;
            totalReduction += Math.min(points, armor.pointLimit()) * reductionPerPoint;
            NbtData.update(stack, tag -> {
                int accumulatedDamage = tag.getInt(LivingArmorItem.DAMAGE) + Math.round(incomingDamage);
                tag.putInt(LivingArmorItem.DAMAGE,
                        Math.min(LivingArmorItem.EVOLUTION_DAMAGE, accumulatedDamage));
            });
        }
        event.setAmount(incomingDamage * Math.max(0.0F, 1.0F - totalReduction));
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            EffectStacking.apply(attacker, ModMobEffects.COTH.get(), 400, 2, 2);
        }
    }

    private static String adaptationSource(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player) return "player." + player.getName().getString();
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString();
        }
        return event.getSource().getMsgId();
    }

    @SubscribeEvent
    public static void recordDamageAndWeaponEffects(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        ItemStack weapon = event.getSource().getDirectEntity() instanceof LivingEntity living
                ? living.getMainHandItem() : null;
        if (weapon == null) weapon = ItemStack.EMPTY;
        if (attacker != null && !weapon.isEmpty() && isHijackedTool(weapon)) {
            HijackedHitEffects.apply(attacker, target);
        }
        if (attacker != null && event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) {
            for (InteractionHandSlot hand : InteractionHandSlot.values()) {
                ItemStack held = hand.get(attacker);
                if (held.getItem() instanceof LivingBowItem bow) {
                    bow.addDamage(held, event.getAmount(), attacker);
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
    public static void clearBleedForHijackedSet(PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
        if (!event.player.level().isClientSide && wearsFullHijackedSet(event.player)) {
            event.player.removeEffect(ModMobEffects.BLEED.get());
        }
    }

    private static boolean wearsFullHijackedSet(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!(player.getItemBySlot(slot).getItem() instanceof HijackedArmorItem)) return false;
        }
        return true;
    }

    private static boolean isHijackedTool(ItemStack stack) {
        return stack.is(ModItems.HIJACKED_IRON_SWORD.get()) || stack.is(ModItems.HIJACKED_IRON_AXE.get())
                || stack.is(ModItems.HIJACKED_IRON_PICKAXE.get()) || stack.is(ModItems.HIJACKED_IRON_SHOVEL.get())
                || stack.is(ModItems.HIJACKED_IRON_HOE.get());
    }

    private enum InteractionHandSlot {
        MAIN { @Override ItemStack get(LivingEntity entity) { return entity.getMainHandItem(); } },
        OFF { @Override ItemStack get(LivingEntity entity) { return entity.getOffhandItem(); } };
        abstract ItemStack get(LivingEntity entity);
    }
}
