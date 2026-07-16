package alku.csrp.item;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import alku.csrp.registry.ModTiers;
import alku.csrp.Csrp;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class LivingWeaponItem extends SwordItem {
    public static final String KILLS = "srp_kills";
    private static final int EVOLUTION_HEALTH = 50_000;
    private final boolean sentient;
    private final Supplier<? extends Item> next;
    private final float reach;
    private final WeaponKind kind;

    public LivingWeaponItem(WeaponKind kind, float damage, float attackSpeed, float reach, boolean sentient,
            Supplier<? extends Item> next, Item.Properties properties) {
        super(ModTiers.LIVING, properties.attributes(SwordItem.createAttributes(ModTiers.LIVING, damage - 1.0F, attackSpeed)
                .withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "living_weapon_reach"),
                                reach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)));
        this.sentient = sentient;
        this.next = next;
        this.reach = reach;
        this.kind = kind;
    }

    public boolean isSentient() { return sentient; }
    public float getReach() { return reach; }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result && !target.level().isClientSide) applyWeaponEffect(stack, target, attacker);
        if (result && !target.level().isClientSide && target.isDeadOrDying()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putInt(KILLS, tag.getInt(KILLS) + Math.round(target.getMaxHealth()));
            });
            evolveIfReady(stack, attacker);
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind != WeaponKind.MAUL || player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }
        Vec3 look = player.getLookAngle();
        double strength = sentient ? 1.85D : 1.25D;
        player.push(look.x * strength, sentient ? 0.95D : 0.85D, look.z * strength);
        player.hurtMarked = true;
        player.getCooldowns().addCooldown(this, sentient ? 500 : 200);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void applyWeaponEffect(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        float chance = sentient ? 0.50F : 0.25F;
        int amplifier = sentient ? 1 : 0;
        switch (kind) {
            case AXE -> applyChance(attacker, target, ModMobEffects.CORROSION, 100, amplifier, chance);
            case SWORD -> applyChance(attacker, target, ModMobEffects.BLEED, 100, amplifier, chance);
            case CLEAVER -> applyChance(attacker, target, ModMobEffects.VIRAL, 100, amplifier, chance);
            case LANCE -> applyChance(attacker, target, ModMobEffects.NEEDLER, 200, amplifier,
                    sentient ? 0.25F : 0.10F);
            case SCYTHE -> {
                if (!(attacker instanceof Player player)) break;
                double radius = sentient ? 8.0D : 4.0D;
                float damage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
                for (LivingEntity nearby : target.level().getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(radius), entity -> entity != target && entity != attacker)) {
                    nearby.hurt(attacker.damageSources().playerAttack(player), damage);
                }
            }
            case MAUL -> {
                if (sentient && target.isDeadOrDying()) {
                    target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4.0D),
                            entity -> entity != attacker && entity != target).forEach(entity -> {
                        entity.hurt(attacker.damageSources().mobAttack(attacker), 8.0F);
                        entity.push(0.0D, 0.85D, 0.0D);
                    });
                }
            }
        }
    }

    private static void applyChance(LivingEntity attacker, LivingEntity target,
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration,
            int amplifier, float chance) {
        if (attacker.getRandom().nextFloat() < chance) {
            MobEffectInstance current = target.getEffect(effect);
            if (current == null) {
                target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true));
                return;
            }
            int newDuration = current.getDuration() + 40 <= duration ? duration : current.getDuration() + 10;
            int newAmplifier = Math.max(amplifier, Math.min(255, current.getAmplifier() + 1));
            target.addEffect(new MobEffectInstance(effect, newDuration, newAmplifier, false, true));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int kills = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(KILLS);
        tooltip.add(Component.translatable("tooltip.csrp.living_progress", kills, EVOLUTION_HEALTH));
    }

    protected void evolveIfReady(ItemStack stack, LivingEntity holder) {
        if (sentient || next == null || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(KILLS) < EVOLUTION_HEALTH) return;
        ItemStack evolved = new ItemStack(next.get());
        if (holder instanceof Player player) {
            stack.shrink(1);
            if (!player.getInventory().add(evolved)) player.drop(evolved, false);
        } else {
            holder.spawnAtLocation(evolved);
            stack.shrink(1);
        }
    }

    public enum WeaponKind { SCYTHE, AXE, SWORD, CLEAVER, MAUL, LANCE }
}
