package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.entity.GoreEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.entity.ParasiteCombatEffects;
import alku.csrp.entity.RemainEntity;
import alku.csrp.entity.ToxicCloudEntity;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * The shared {@code EntityParasiteBase} combat rules of SRParasites 1.10.9, applied to every
 * parasite instead of being copied into each family class:
 *
 * <ul>
 *   <li>per-tier damage cap ({@code SRPConfig.<tier>Cap}) with RAGE II on reaching it,</li>
 *   <li>per-tier armor-bypassing minimum melee damage ({@code <tier>MinDamage}) scaled by VIRA,</li>
 *   <li>per-tier food stealing from players ({@code foodSteal}) plus the {@code foodRott} item
 *       conversion into assimilated flesh,</li>
 *   <li>poison-to-healing conversion ({@code genePoisonHealing}),</li>
 *   <li>fear on heavy hits ({@code fearPlayer}, damage above 8),</li>
 *   <li>kill healing ({@code geneMobHealing}).</li>
 * </ul>
 *
 * Tiers are resolved from the entity id prefix (the same convention {@code InfectionMechanics}
 * uses for COTH spread) so the rules follow data, not class hierarchy. Values live in the
 * {@code parasiteCombatTable} config entry and default to the original SRPConfig numbers.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteCombatRules {
    private static final float FEAR_DAMAGE_THRESHOLD = 8.0F;
    private static Map<String, Tier> tierCache;

    private ParasiteCombatRules() {
    }

    /** damageCap diverts damage above maxHealth/cap; minimumDamage and foodSteal are attack side. */
    private record Tier(int damageCap, float minimumDamage, float foodSteal) {
    }

    private static final Tier NEUTRAL = new Tier(1, 0.0F, 0.0F);
    /** Legacy attackEntityFrom roll that lays a single flat gore block. */
    private static final float GORE_ON_HURT_CHANCE = 0.1F;
    /** Legacy damage-cap roll for a gore block plus one gore bomb. */
    private static final double CAP_GORE_CHANCE = 0.3D;

    @SubscribeEvent
    public static void applyDefenseRules(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Parasite) || event.getEntity().level().isClientSide) {
            return;
        }
        LivingEntity parasite = event.getEntity();
        if (convertPoisonToHealing(parasite, event.getSource(), event.getAmount())) {
            event.setCanceled(true);
            return;
        }
        Tier tier = tierOf(parasite);
        // Legacy attackEntityFrom: a 10% roll on any hit lays one flat gore block.
        if (Config.parasiteGoreEnabled() && parasite.level() instanceof ServerLevel goreLevel
                && parasite.getRandom().nextFloat() < GORE_ON_HURT_CHANCE) {
            ModBlocks.placeGore(goreLevel, parasite.blockPosition(),
                    tierKey(BuiltInRegistries.ENTITY_TYPE.getKey(parasite.getType()).getPath()), false);
        }
        if (tier.damageCap() <= 1 || event.getSource().is(DamageTypeTags.IS_FIRE)) {
            return;
        }
        float maximumHealth = parasite.getMaxHealth();
        float capped = maximumHealth / tier.damageCap() + maximumHealth % tier.damageCap() * 0.5F;
        if (event.getAmount() >= capped) {
            if (!parasite.hasEffect(ModMobEffects.RAGE) && Config.rageEnabled()) {
                parasite.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 200, 1, false, false), parasite);
            }
            // Legacy damage-cap reaction: 30% roll for a gore block plus one gore bomb.
            if (parasite.level() instanceof ServerLevel capLevel && parasite.getHealth() > 0.0F
                    && parasite.getRandom().nextDouble() < CAP_GORE_CHANCE) {
                String tierName = tierKey(BuiltInRegistries.ENTITY_TYPE.getKey(parasite.getType()).getPath());
                if (Config.parasiteGoreEnabled()) {
                    ModBlocks.placeGore(capLevel, parasite.blockPosition(), tierName, false);
                    spawnGoreBombs(capLevel, parasite, 1);
                }
            }
        }
        if (event.getAmount() > capped) {
            event.setAmount(capped);
        }
    }

    @SubscribeEvent
    public static void applyAttackRules(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        LivingEntity victim = event.getEntity();
        if (!(attacker instanceof LivingEntity parasite) || !(attacker instanceof Parasite)
                || attacker.level().isClientSide || victim == attacker || victim instanceof Parasite) {
            return;
        }
        Tier tier = tierOf(parasite);
        applyFear(parasite, victim, event.getNewDamage());
        ParasiteCombatEffects.applyMinimumDamage(parasite, victim, tier.minimumDamage());
        ParasiteCombatEffects.stealFood(parasite, victim, tier.foodSteal(),
                Config.parasiteFoodTheftChance());
    }

    @SubscribeEvent
    public static void applyKillHeal(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity killer)
                || !(killer instanceof Parasite) || killer.level().isClientSide) {
            return;
        }
        ParasiteCombatEffects.healOnKill(killer, event.getEntity());
    }

    /**
     * Legacy spawnGore + dyingBurst: a dying parasite leaves gore and a Remain behind, and may
     * burst into the original toxic cloud (MOB_EXPLOTION sound, poison 300, COTH 3600).
     */
    @SubscribeEvent
    public static void applyDeathGore(LivingDeathEvent event) {
        LivingEntity parasite = event.getEntity();
        if (!(parasite instanceof Parasite) || parasite.level().isClientSide
                || !(parasite.level() instanceof ServerLevel level)) {
            return;
        }
        String tier = tierKey(BuiltInRegistries.ENTITY_TYPE.getKey(parasite.getType()).getPath());
        if (Config.parasiteGoreEnabled()) {
            leaveGore(level, parasite, tier);
        }
        if (parasite.getRandom().nextDouble() < Config.parasiteSelfExplodeChance()) {
            selfExplode(level, parasite);
        }
    }

    /** Legacy spawnGore: a big gore block, a Remain, a flat gore spread and three gore bombs. */
    private static void leaveGore(ServerLevel level, LivingEntity parasite, String tier) {
        BlockPos pos = parasite.blockPosition();
        ModBlocks.placeGore(level, pos, tier, true);
        RemainEntity remain = ModEntities.REMAIN.get().create(level);
        if (remain != null) {
            remain.moveTo(parasite.getX(), parasite.getY(), parasite.getZ(),
                    parasite.getYRot(), parasite.getXRot());
            remain.setParasite(BuiltInRegistries.ENTITY_TYPE.getKey(parasite.getType()).toString());
            remain.setGoal(20 * Config.parasiteRemainValue());
            level.addFreshEntity(remain);
        }
        spreadFlatGore(level, parasite, tier);
        spawnGoreBombs(level, parasite, 3);
    }

    /** Legacy attackEntityFromEffects(2, 100): flat gore blocks around the corpse. */
    private static void spreadFlatGore(ServerLevel level, LivingEntity parasite, String tier) {
        BlockPos origin = parasite.blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (level.getRandom().nextInt(4) != 0) {
                    continue;
                }
                BlockPos candidate = origin.offset(dx, 0, dz);
                if (candidate.equals(origin)) {
                    continue;
                }
                ModBlocks.placeGore(level, candidate, tier, false);
            }
        }
    }

    /** Legacy attackEntityFromCap(go): throws go gore projectiles away from the parasite. */
    private static void spawnGoreBombs(ServerLevel level, LivingEntity parasite, int count) {
        for (int i = 0; i < count; i++) {
            GoreEntity bomb = ModEntities.GORE.get().create(level);
            if (bomb == null) {
                return;
            }
            bomb.setType((byte) 1);
            bomb.moveTo(parasite.getX(), parasite.getY() + parasite.getBbHeight() * 0.5D,
                    parasite.getZ(), parasite.getYRot(), parasite.getXRot());
            double dx = level.getRandom().nextDouble() - 0.5D;
            double dz = level.getRandom().nextDouble() - 0.5D;
            double dy = level.getRandom().nextDouble() * 0.5D;
            double length = Math.max(0.1D, Math.sqrt(dx * dx + dy * dy + dz * dz));
            double speed = 0.8D / (length / 4.0D + 0.1D)
                    * (level.getRandom().nextDouble() * level.getRandom().nextDouble() + 0.3D);
            bomb.setMotion(dx / length * speed, dy / length * speed * 2.0D, dz / length * speed,
                    0.2D, 0.8D);
            level.addFreshEntity(bomb);
        }
    }

    /**
     * Legacy selfExplode: MOB_EXPLOTION plus a toxic cloud of width * 1.5 with poison 300 and
     * COTH 3600, halved duration and wait time 10.
     */
    private static void selfExplode(ServerLevel level, LivingEntity parasite) {
        level.playSound(null, parasite.getX(), parasite.getY(), parasite.getZ(),
                ModSounds.MOB_EXPLOSION.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level,
                parasite.getX(), parasite.getY(), parasite.getZ());
        cloud.setRadius(parasite.getBbWidth() * 1.5F);
        cloud.setWaitTime(10);
        cloud.setDuration(cloud.getDuration() / 2);
        cloud.setRadiusPerTick(-cloud.getRadius() / Math.max(1, cloud.getDuration()));
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 0));
        // The original hid the COTH icon on this cloud; the project keeps COTH visible everywhere
        // (enforced by scripts/verify-coth-visibility.cjs), so the icon stays on.
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0, false, true));
        level.addFreshEntity(cloud);
    }

    /**
     * Legacy fearPlayer: a parasite hit dealing more than 8 damage frightens the victim
     * (level 1..3, 300 + 40 * (level - 1) ticks clamped to 200..500).
     */
    private static void applyFear(LivingEntity parasite, LivingEntity victim, float dealt) {
        if (dealt <= FEAR_DAMAGE_THRESHOLD || !(victim instanceof Player)) {
            return;
        }
        ParasiteCombatEffects.applyFearFromDamage(victim, victim.getHealth() + dealt, parasite);
    }

    /**
     * Legacy func_70687_e follow-up: poison damage of exactly 1.0 while poisoned is converted into
     * genePoisonHealing instead of hurting the parasite.
     */
    private static boolean convertPoisonToHealing(LivingEntity parasite, net.minecraft.world.damagesource.DamageSource source,
                                                  float amount) {
        float healing = Config.parasitePoisonHealing();
        if (healing <= 0.0F || amount != 1.0F || !parasite.hasEffect(MobEffects.POISON)
                || !source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) {
            return false;
        }
        parasite.heal(healing);
        return true;
    }

    private static Tier tierOf(LivingEntity parasite) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(parasite.getType());
        String path = id.getPath();
        return tiers().getOrDefault(tierKey(path), NEUTRAL);
    }

    /** Maps an entity id onto the legacy tier names used by the combat table. */
    private static String tierKey(String path) {
        if (path.startsWith("sim_")) {
            return "infected";
        }
        if (path.startsWith("fer_")) {
            return "feral";
        }
        if (path.startsWith("hi_")) {
            return "hijacked";
        }
        if (path.startsWith("mar_")) {
            return "assimara";
        }
        if (path.startsWith("ada_")) {
            return "adapted";
        }
        if (path.startsWith("pri_")) {
            return "primitive";
        }
        if (path.startsWith("beckon_si")) {
            return "nexus_si";
        }
        if (path.startsWith("beckon_sii")) {
            return "nexus_sii";
        }
        if (path.startsWith("beckon_siii")) {
            return "nexus_siii";
        }
        if (path.startsWith("beckon_siv")) {
            return "nexus_siv";
        }
        if (path.startsWith("anc_")) {
            return "ancient";
        }
        return switch (path) {
            case "grunt", "bomber_light", "monarch", "overseer", "vigilante", "warden", "seeker" -> "pure";
            case "bogle", "carrier_colony", "haunter", "bomber_heavy", "wraith" -> "preeminent";
            case "marauder" -> "assimara";
            default -> "neutral";
        };
    }

    /** Parsed parasiteCombatTable, rebuilt only when the config value changes. */
    private static Map<String, Tier> tiers() {
        var configured = Config.parasiteCombatTable();
        Map<String, Tier> cache = tierCache;
        if (cache != null && cache.size() == configured.size() + 1) {
            return cache;
        }
        Map<String, Tier> parsed = new HashMap<>();
        for (String entry : configured) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 4) {
                continue;
            }
            try {
                parsed.put(parts[0].trim().toLowerCase(), new Tier(
                        Integer.parseInt(parts[1].trim()),
                        Float.parseFloat(parts[2].trim()),
                        Float.parseFloat(parts[3].trim())));
            } catch (NumberFormatException ignored) {
                // Config validation rejects malformed entries; skip stale ones safely.
            }
        }
        parsed.put("neutral", NEUTRAL);
        tierCache = Map.copyOf(parsed);
        return tierCache;
    }

    /** Exposed for the verification script: the raw tier a mob id resolves to. */
    public static String tierNameFor(String entityPath) {
        return tierKey(entityPath);
    }

    static ServerLevel serverLevelOf(LivingEntity entity) {
        return entity.level() instanceof ServerLevel serverLevel ? serverLevel : null;
    }
}
