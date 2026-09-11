package alku.nocubessrparmory;

import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * NeoForge replacements for the small MCreator procedure hooks in the original
 * 1.12 addon. The old procedures referenced obfuscated SRP internals; this
 * implementation uses stable registry IDs and safe server-side event hooks.
 */
@EventBusSubscriber(modid = NoCubesSrpCombatAddon.MOD_ID)
public final class ArmoryGameplayEvents {
    private static final Set<String> TWISTED_SET = Set.of(
            "twistedarmorhelmet", "twistedarmorbody", "twistedarmorlegs", "twistedarmorboots");
    private static final Set<String> PESTILENT_SET = Set.of(
            "pestilentarmorhelmet", "pestilentarmorbody", "pestilentarmorlegs", "pestilentarmorboots");
    private static final Set<String> EVOLUTION_SET = Set.of(
            "evolutionarmorhelmet", "evolutionarmorbody", "evolutionarmorlegs", "evolutionarmorboots");
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final ThreadLocal<Boolean> EXTRA_DAMAGE = ThreadLocal.withInitial(() -> false);

    private ArmoryGameplayEvents() {}

    @SubscribeEvent
    public static void weaponHit(LivingDamageEvent.Post event) {
        if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getEntity().level().isClientSide) return;
        ItemStack weapon = event.getSource().getWeaponItem();
        if (weapon == null || weapon.isEmpty()) weapon = attacker.getMainHandItem();
        String id = itemId(weapon);
        LivingEntity target = event.getEntity();
        switch (id) {
            case "pestilentknife" -> addCsrpOrVanilla(target, "viral", MobEffects.POISON, 100, 0);
            case "pestilentscythe" -> addCsrpOrVanilla(target, "viral", MobEffects.POISON, 200, 0);
            case "pestilentmiasm", "pestilentshuriken" -> addCsrpOrVanilla(target, "viral", MobEffects.POISON, 100, 0);
            case "evolutionknife" -> ignite(target, 8);
            case "evolutionsickle" -> ignite(target, 16);
            case "mimicbladeyellow" -> ignite(target, 10);
            case "mimicbladegreen" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 5, false, true));
            case "mimicbladepurple" -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 1, false, true));
            case "mimicbladered" -> target.level().explode(null, target.getX(), target.getY(), target.getZ(), 2.0F, Level.ExplosionInteraction.MOB);
            case "carapaceshellbreaker" -> extraFireDamage(target, 4.0F);
            case "gorecombatbow" -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 1, false, true));
            case "twisteddagger" -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true));
                smokeAt(target);
            }
            case "twistedgreataxe", "twistedmallet" -> smokeAt(target);
            case "flamethrower" -> ignite(target, 20);
            case "hosttentacle" -> { }
            case "incinerator" -> ignite(target, 30);
            case "plasmatorch" -> ignite(target, 10);
            case "thereapertrue" -> extraDamage(target, attacker, 400.0F);
            default -> { }
        }
    }

    /** Original BossDrops procedure: two guaranteed cores plus four 40% rolls. */
    @SubscribeEvent
    public static void bossDrops(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity.getType() == EntityType.WITHER
                || entity.getType() == EntityType.ENDER_DRAGON) return;
        float health = entity.getMaxHealth();
        if (health < 199.0F) return;
        ItemStack core = health < 250.0F
                ? AddonItems.DREADNAUGHT_CORE.get().getDefaultInstance()
                : AddonItems.OVERLORD_CORE.get().getDefaultInstance();
        for (int i = 0; i < 2; i++) entity.spawnAtLocation(core.copy());
        for (int i = 0; i < 4; i++) {
            if (entity.getRandom().nextFloat() < 0.4F) entity.spawnAtLocation(core.copy());
        }
    }

    static void awardAdvancement(Player player, String id) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        AdvancementHolder advancement = serverPlayer.server.getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath(NoCubesSrpCombatAddon.MOD_ID, id));
        if (advancement != null) serverPlayer.getAdvancements().award(advancement, id);
    }

    @SubscribeEvent
    public static void heldWeaponTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        ItemStack held = player.getMainHandItem();
        String id = itemId(held);
        if (id.equals("mimicbladegreen") && player.isCrouching()) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 10, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 10, 0, false, false));
        } else if (id.equals("mimicbladepurple")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 0, false, false));
        } else if (id.equals("mimicbladeyellow")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false));
        } else if (id.equals("twisteddagger") && !player.level().isDay()) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 15, 0, false, false));
        } else if (id.equals("twistedgreataxe") && player.getOffhandItem().isEmpty()) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 14, 0, false, false));
        } else if (id.equals("twistedmallet")) {
            var sharpness = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
            if (EnchantmentHelper.getItemEnchantmentLevel(sharpness, held) == 0) held.enchant(sharpness, 2);
        }
    }

    @SubscribeEvent
    public static void goreHatchetUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (itemId(event.getItemStack()).equals("plasmatorch") && player instanceof ServerPlayer serverPlayer) {
            AdvancementHolder advancement = serverPlayer.server.getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath(NoCubesSrpCombatAddon.MOD_ID, "advplasmatorch"));
            if (advancement != null) serverPlayer.getAdvancements().award(advancement, "advplasmatorch");
            return;
        }
        if (itemId(event.getItemStack()).equals("gorehatchet")) goreHatchetBerserk(player);
    }

    @SubscribeEvent
    public static void goreHatchetUseOnBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (itemId(event.getItemStack()).equals("gorehatchet")) goreHatchetBerserk(player);
    }

    private static void goreHatchetBerserk(Player player) {
        if (!player.hasEffect(MobEffects.DAMAGE_BOOST)
                && player.getInventory().contains(AddonItems.GORE_PART.get().getDefaultInstance())) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1, false, true));
            player.getInventory().clearOrCountMatchingItems(stack -> stack.is(AddonItems.GORE_PART.get()), 1, player.inventoryMenu.getCraftSlots());
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.HORSE_EAT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void armorSetTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (wearsSet(player, TWISTED_SET)) {
            if (player.isSprinting()) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 0, false, false));
            if (player.isCrouching()) {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 14, 1, false, false));
                player.fallDistance = 0.0F;
                if (player.getRandom().nextFloat() < 0.1F) particles(player.level(), ParticleTypes.LARGE_SMOKE, player.position().add(0.0D, 0.3D, 0.0D), 1);
            }
            if (player.isSprinting() && player.getRandom().nextFloat() < 0.1F) {
                particles(player.level(), ParticleTypes.LARGE_SMOKE, player.position().add(0.0D, 0.3D, 0.0D), 1);
            }
        }
        if (wearsSet(player, PESTILENT_SET)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 1, false, false));
        }
        if (wearsSet(player, EVOLUTION_SET)) {
            if (player.isSprinting()) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 1, false, false));
            player.fallDistance = 0.0F;
            if (player.isCrouching()) {
                player.clearFire();
                player.removeAllEffects();
            }
        }
    }

    private static void particles(Level level, net.minecraft.core.particles.ParticleOptions type, Vec3 pos, int count) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(type, pos.x, pos.y, pos.z, count, 0.3D, 0.3D, 0.3D, 0.05D);
        }
    }

    private static void smokeAt(LivingEntity target) {
        if (target.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + 0.6D, target.getZ(),
                    5, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }

    private static void ignite(LivingEntity target, int seconds) {
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), seconds * 20));
    }

    private static void extraFireDamage(LivingEntity target, float amount) {
        if (EXTRA_DAMAGE.get()) return;
        EXTRA_DAMAGE.set(true);
        try {
            target.hurt(target.damageSources().onFire(), amount);
        } finally {
            EXTRA_DAMAGE.set(false);
        }
    }

    private static void extraDamage(LivingEntity target, LivingEntity attacker, float amount) {
        if (EXTRA_DAMAGE.get()) return;
        EXTRA_DAMAGE.set(true);
        try {
            target.hurt(target.damageSources().mobAttack(attacker), amount);
        } finally {
            EXTRA_DAMAGE.set(false);
        }
    }

    private static boolean wearsSet(Player player, Set<String> ids) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!ids.contains(itemId(player.getItemBySlot(slot)))) return false;
        }
        return true;
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && NoCubesSrpCombatAddon.MOD_ID.equals(key.getNamespace()) ? key.getPath() : "";
    }

    private static void addCsrpOrVanilla(LivingEntity target, String csrpId,
            Holder<MobEffect> fallback, int duration, int amplifier) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("csrp", csrpId);
        BuiltInRegistries.MOB_EFFECT.getOptional(id).ifPresentOrElse(
                effect -> target.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                        duration, amplifier, false, true)),
                () -> target.addEffect(new MobEffectInstance(fallback, duration, amplifier, false, true)));
    }
}
