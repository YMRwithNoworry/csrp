package alku.csrp.event;

import alku.csrp.network.CsrpNetwork;
import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.entity.Parasite;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModItems;
import alku.csrp.overlast.network.EvolutionHudPayload;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class OverlastEvents {
    public static final ResourceKey<Enchantment> PARASITE_KILLER = ResourceKey.create(
            Registries.ENCHANTMENT, new ResourceLocation(Csrp.MODID, "parasite_killer"));
    private static final Map<String, EntityType<?>> CURED_FORMS = Map.ofEntries(
            Map.entry("sim_bigspider", EntityType.SPIDER),
            Map.entry("sim_bear", EntityType.POLAR_BEAR),
            Map.entry("sim_cow", EntityType.COW),
            Map.entry("sim_enderman", EntityType.ENDERMAN),
            Map.entry("sim_horse", EntityType.HORSE),
            Map.entry("sim_human", EntityType.ZOMBIE),
            Map.entry("sim_adventurer", EntityType.ZOMBIE),
            Map.entry("sim_pig", EntityType.PIG),
            Map.entry("sim_sheep", EntityType.SHEEP),
            Map.entry("sim_villager", EntityType.VILLAGER),
            Map.entry("sim_wolf", EntityType.WOLF));
    private static final List<MobEffect> PURIFIED_EFFECTS = List.of(
            ModMobEffects.COTH.get(), ModMobEffects.FEAR.get(), ModMobEffects.BLEED.get(),
            ModMobEffects.CORROSIVE.get(), ModMobEffects.VIRAL.get(), ModMobEffects.REPEL.get());

    private OverlastEvents() {
    }

    @SubscribeEvent
    public static void tickNaturalEvolution(LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % 20L == 0L) {
            level.players().forEach(OverlastEvents::syncHud);
        }
        if (level.getGameTime() % 1_200L != 0L) return;
        int phase = SrpWorldData.get(level).evolutionPhase();
        if (phase < 3 || phase > 7 || Config.overlastNaturalEvolutionScale() <= 0.0D) {
            return;
        }
        int span = EvolutionSystem.thresholdForPhase(phase + 1) - EvolutionSystem.thresholdForPhase(phase);
        int points = (int) Math.floor((span / 4_000.0D) * Config.overlastNaturalEvolutionScale());
        if (points > 0) {
            EvolutionSystem.addPoints(level, points, EvolutionSystem.PointSource.PASSIVE);
        }
    }

    @SubscribeEvent
    public static void tickEffects(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)
                || !(living.level() instanceof ServerLevel level) || living.tickCount % 20 != 0) {
            return;
        }
        MobEffectInstance purify = living.getEffect(ModMobEffects.PARASITES_PURIFY.get());
        if (purify != null) {
            PURIFIED_EFFECTS.forEach(living::removeEffect);
            if (living instanceof Parasite && purify.getDuration() <= 40) {
                restoreHost(level, living);
                return;
            }
        }
        if (living instanceof Player) {
            applyInfection(living);
        }
    }

    private static void applyInfection(LivingEntity living) {
        MobEffectInstance infection = living.getEffect(ModMobEffects.PARASITES_INFECT.get());
        if (infection == null) {
            return;
        }
        int strength = Math.min(1, infection.getAmplifier());
        if (!living.hasEffect(ModMobEffects.PARASITES_PURIFY.get())) {
            InfectionMechanics.applyCothEffect(living, null, 60, strength == 0 ? 1 : 3, false, false);
        }
        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, strength == 0 ? 2 : 3, false, false));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, false, false));
        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, strength == 0 ? 1 : 2, false, false));
        living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
    }

    private static void restoreHost(ServerLevel level, LivingEntity parasite) {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(parasite.getType()).getPath();
        EntityType<?> restoredType = CURED_FORMS.get(path);
        if (restoredType == null) {
            return;
        }
        Entity restored = restoredType.create(level);
        if (restored != null) {
            restored.moveTo(parasite.getX(), parasite.getY(), parasite.getZ(), parasite.getYRot(), parasite.getXRot());
            level.addFreshEntity(restored);
            parasite.discard();
            level.levelEvent(2001, parasite.blockPosition(), 0);
        }
    }

    @SubscribeEvent
    public static void fortunateOreDrops(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !player.hasEffect(ModMobEffects.FORTUNATE.get())
                || !isFortunateOre(event.getState().getBlock())) {
            return;
        }
        // Forge 1.20.1 has no cancellable BlockDropsEvent; the break event only
        // provides XP/state, so bonus drops are handled by the block loot table.
    }

    private static boolean isFortunateOre(Block block) {
        return block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE
                || block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE
                || block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE
                || block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE
                || block == Blocks.NETHER_QUARTZ_ORE;
    }

    @SubscribeEvent
    public static void parasiteKillerDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Parasite) || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        Holder<Enchantment> enchantment = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(PARASITE_KILLER).orElse(null);
        if (enchantment == null) {
            return;
        }
        int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment.value(), attacker.getMainHandItem());
        if (enchantmentLevel <= 0 || level.random.nextFloat() > 0.3F + 0.1F * enchantmentLevel) {
            return;
        }
        event.setAmount(event.getAmount() * 1.2F + 1.25F + 0.75F * enchantmentLevel);
        List<MobEffectInstance> effects = List.copyOf(event.getEntity().getActiveEffects());
        if (!effects.isEmpty()) {
            event.getEntity().removeEffect(effects.get(level.random.nextInt(effects.size())).getEffect());
        }
    }

    @SubscribeEvent
    public static void loginMessage(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SrpWorldData data = SrpWorldData.get(player.serverLevel());
        int phase = data.evolutionPhase();
        int minutePoints = 0;
        if (phase >= 3 && phase <= 7) {
            int span = EvolutionSystem.thresholdForPhase(phase + 1) - EvolutionSystem.thresholdForPhase(phase);
            minutePoints = (int) Math.floor((span / 4_000.0D) * Config.overlastNaturalEvolutionScale());
        }
        player.sendSystemMessage(Component.translatable("message.csrp.overlast.login", phase, minutePoints));
        if (phase >= 8) {
            player.sendSystemMessage(Component.translatable("message.csrp.overlast.phase_eight"));
        }
        syncHud(player);
    }

    private static void syncHud(ServerPlayer player) {
        SrpWorldData data = SrpWorldData.get(player.serverLevel());
        int phase = data.evolutionPhase();
        int current = EvolutionSystem.thresholdForPhase(phase);
        int next = phase >= 10 ? EvolutionSystem.thresholdForPhase(10)
                : EvolutionSystem.thresholdForPhase(phase + 1);
        boolean holdingClock = player.getMainHandItem().is(ModItems.EVCLOCK.get())
                || player.getOffhandItem().is(ModItems.EVCLOCK.get());
        CsrpNetwork.sendToPlayer(player, new EvolutionHudPayload(phase, data.evolutionPoints(), current, next,
                !Config.overlastHudRequiresClock() || holdingClock));
    }
}
