package alku.nocubessrparmory;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A compatibility launcher for the old MCreator RangedItem classes. It keeps
 * the old hold-to-fire cadence, ammo rules and per-shot durability while spawning
 * the original custom arrow entities.
 */
final class ArmoryLauncherItem extends BowItem {
    enum Kind {
        EVOLUTION_BOW(2.0F, 8.5F, 2, true, 15, "arrow", 5, "entitybulletevolutionbow"),
        GORE_BOW(1.0F, 7.0F, 1, true, 20, "arrow", 0, "entitybulletgorecombatbow"),
        TWISTED_BOW(1.0F, 4.0F, 0, true, 18, "arrow", 0, "entitybullettwistedbow"),
        FLAMETHROWER(1.0F, 8.0F, 2, false, 1, "capsulefuel", 0, "entitybulletflamethrower"),
        INCINERATOR(1.0F, 11.0F, 2, false, 1, "capsulefuel", 0, "entitybulletincinerator"),
        PLASMA_TORCH(1.0F, 4.0F, 2, false, 1, "capsulefuel", 0, "entitybulletplasmatorch"),
        HEAD_BOMB(1.0F, 2.0F, 0, false, 10, "headbomb", 0, "entitybulletheadbomb"),
        HOST_BOMB(0.7F, 1.0F, 1, false, 7, "hostbomb", 0, "entitybullethostbomb"),
        HOST_TENTACLE(1.0F, 1.0F, 1, false, 15, "hostbomb", 0, "entitybullethosttentacle"),
        OVERLORD_BLADE(1.1F, 10.5F, 0, false, 200, "", 0, "entitybulletoverlordblade"),
        PESTILENT_MIASM(1.0F, 5.5F, 1, true, 22, "arrow", 0, "entitybulletpestilentmiasm"),
        PESTILENT_SHURIKEN(0.8F, 2.0F, 0, false, 5, "pestilentshuriken", 0, "entitybulletpestilentshuriken"),
        TWISTED_BOMB(1.0F, 2.0F, 0, false, 10, "twistedbomb", 0, "entitybullettwistedbomb");

        final float velocity;
        final float damage;
        final int knockback;
        final boolean crit;
        final int cooldown;
        final String ammo;
        final int fireSeconds;
        final String entityId;

        Kind(float velocity, float damage, int knockback, boolean crit, int cooldown,
                String ammo, int fireSeconds, String entityId) {
            this.velocity = velocity;
            this.damage = damage;
            this.knockback = knockback;
            this.crit = crit;
            this.cooldown = cooldown;
            this.ammo = ammo;
            this.fireSeconds = fireSeconds;
            this.entityId = entityId;
        }
    }

    private final Kind kind;

    ArmoryLauncherItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    ArmoryLauncherItem(Kind kind, Properties properties, float attackDamage) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(NoCubesSrpCombatAddon.MOD_ID, kind.name().toLowerCase() + "_damage"),
                        attackDamage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(NoCubesSrpCombatAddon.MOD_ID, kind.name().toLowerCase() + "_speed"),
                        -2.4D, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build()));
        this.kind = kind;
    }

    Kind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind.ammo.isEmpty() || hasInfinity(level, stack) || !findAmmo(player, stack).isEmpty()
                || player.getAbilities().instabuild) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!(user instanceof Player player) || level.isClientSide) return;
        int elapsed = getUseDuration(stack, user) - remainingUseDuration;
        if (elapsed <= 0 || player.getCooldowns().isOnCooldown(this)) return;
        ItemStack ammo = findAmmo(player, stack);
        if (ammo.isEmpty() && !kind.ammo.isEmpty() && !hasInfinity(level, stack)
                && !player.getAbilities().instabuild) return;
        fire(level, player, stack, ammo);
    }

    private void fire(Level level, Player player, ItemStack weapon, ItemStack ammo) {
        ArmoryArrowEntity projectile = ArmoryEntities.create(kind, level, player, new ItemStack(this));
        Vec3 look = player.getLookAngle();
        projectile.shoot(look.x, look.y, look.z, kind.velocity * 2.0F, 0.0F);
        level.addFreshEntity(projectile);
        String advancement = switch (kind) {
            case GORE_BOW -> "advgorebow";
            case HEAD_BOMB -> "advheadbomb";
            case PESTILENT_MIASM -> "advpestilentmiasm";
            case PESTILENT_SHURIKEN -> "advpestilentshuriken";
            case TWISTED_BOMB -> "advrupterbomb";
            case TWISTED_BOW -> "advtwistedbow";
            default -> "";
        };
        if (!advancement.isEmpty()) ArmoryGameplayEvents.awardAdvancement(player, advancement);
        var shotSound = switch (kind) {
            case FLAMETHROWER, INCINERATOR, PLASMA_TORCH, OVERLORD_BLADE -> SoundEvents.BLAZE_SHOOT;
            case HEAD_BOMB, HOST_BOMB, HOST_TENTACLE, PESTILENT_SHURIKEN, TWISTED_BOMB -> SoundEvents.PLAYER_ATTACK_SWEEP;
            default -> SoundEvents.ARROW_SHOOT;
        };
        level.playSound(null, player.getX(), player.getY(), player.getZ(), shotSound, SoundSource.PLAYERS, 0.8F,
                0.8F + level.random.nextFloat() * 0.25F);
        player.getCooldowns().addCooldown(this, kind.cooldown);
        consumeAmmo(level, player, weapon, ammo);
        if (weapon.isDamageableItem()) {
            weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
    }

    private ItemStack findAmmo(Player player, ItemStack weapon) {
        if (player.getAbilities().instabuild) return ItemStack.EMPTY;
        if (kind.ammo.equals(itemId(weapon))) return weapon;
        Predicate<ItemStack> predicate;
        if (kind.ammo.equals("arrow")) predicate = stack -> stack.is(ItemTags.ARROWS);
        else predicate = stack -> kind.ammo.equals(itemId(stack));
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && predicate.test(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private void consumeAmmo(Level level, Player player, ItemStack weapon, ItemStack ammo) {
        if (player.getAbilities().instabuild || hasInfinity(level, weapon) || ammo.isEmpty()) return;
        if (ammo == weapon) weapon.shrink(1);
        else ammo.consume(1, player);
    }

    private static boolean hasInfinity(Level level, ItemStack stack) {
        Holder<Enchantment> infinity = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.INFINITY);
        return EnchantmentHelper.getItemEnchantmentLevel(infinity, stack) > 0;
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && NoCubesSrpCombatAddon.MOD_ID.equals(id.getNamespace()) ? id.getPath() : "";
    }
}
