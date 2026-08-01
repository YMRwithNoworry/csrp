package alku.csrp.world;

import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.entity.DeterrentParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.SrpWorldData.DislodgmentCode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Original death-bound dislodgment codes and the Jugg/Worm completion chain. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class DislodgmentSystem {
    private static final int DEATH_RADIUS = 7;
    private static final int[] PHASE_COST_MULTIPLIER = {0, 1, 3, 6, 8, 10, 13, 17, 20, 25, 30};

    private DislodgmentSystem() {
    }

    @SubscribeEvent
    public static void onParasiteDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || !(dead instanceof Parasite)
                || !Config.useDislodgment()) {
            return;
        }

        SrpWorldData data = SrpWorldData.get(level);
        tryTriggerDeathCode(level, data);
        applySummonByDeath(dead, event.getSource().getEntity(), data);
        applyDeathAreaCodes(level, dead, data);
    }

    @SubscribeEvent
    public static void tickCodes(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Config.useDislodgment()) {
            return;
        }
        for (DislodgmentCode expired : SrpWorldData.get(level).expireDislodgmentCodes(level)) {
            if (expired.code() == 2 && Config.disloSummonByDeath()) {
                finishSummonByDeath(level, expired.value());
            }
        }
    }

    @SubscribeEvent
    public static void suppressDislodgmentDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Parasite && event.getEntity().level() instanceof ServerLevel level
                && Config.useDislodgment() && Config.disloLootXpCancel()
                && activeValue(SrpWorldData.get(level), 18) > 0) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public static void suppressDislodgmentExperience(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof Parasite && event.getEntity().level() instanceof ServerLevel level
                && Config.useDislodgment() && Config.disloLootXpCancel()
                && activeValue(SrpWorldData.get(level), 18) > 0) {
            event.setDroppedExperience(0);
        }
    }

    public static void clearCodes(ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        List<DislodgmentCode> active = data.activeDislodgmentCodes(level);
        data.clearDislodgmentCodes();
        for (DislodgmentCode code : active) {
            if (code.code() == 2 && Config.useDislodgment() && Config.disloSummonByDeath()) {
                finishSummonByDeath(level, code.value());
            }
        }
    }

    private static void tryTriggerDeathCode(ServerLevel level, SrpWorldData data) {
        int phase = data.evolutionPhase();
        if (!data.dislodgmentTriggerReady(level) || phase < 1 || phase > 10
                || EvolutionSystem.ubiquitousDevelopment(level.getServer()) < 1
                || level.getRandom().nextDouble() > Config.dislodgmentDeathTriggerChance()) {
            return;
        }

        List<ActivationRule> candidates = new ArrayList<>();
        if (Config.disloSummonByDeath()) {
            candidates.add(new ActivationRule(2, Config.disloSummonByDeathValue(),
                    Config.disloSummonByDeathDuration(), Config.disloSummonByDeathPointCost()));
        }
        if (Config.disloHealingDeath()) {
            candidates.add(new ActivationRule(7, Config.disloHealingDeathValue(),
                    Config.disloHealingDeathDuration(), Config.disloHealingDeathPointCost()));
        }
        if (Config.disloLootXpCancel()) {
            candidates.add(new ActivationRule(18, 1, Config.disloLootXpCancelDuration(),
                    Config.disloLootXpCancelPointCost()));
        }

        while (!candidates.isEmpty()) {
            ActivationRule rule = candidates.remove(level.getRandom().nextInt(candidates.size()));
            int value = saturatingMultiply(rule.value(), phase);
            int duration = saturatingMultiply(rule.durationSeconds(), phase);
            int cost = saturatingMultiply(rule.pointCost(), PHASE_COST_MULTIPLIER[phase]);
            if (data.startDislodgmentCode(level, rule.code(), value, duration, cost)) {
                data.setDislodgmentTriggerCooldown(level, Config.dislodgmentGlobalCooldown());
                return;
            }
        }
    }

    private static void applySummonByDeath(LivingEntity dead, Entity killer, SrpWorldData data) {
        DislodgmentCode code = data.dislodgmentCode(2);
        if (!Config.disloSummonByDeath() || code == null || code.value() < 1
                || !(killer instanceof LivingEntity livingKiller)) {
            return;
        }
        data.increaseDislodgmentValue(2, (int) dead.getMaxHealth());
        int remaining = (int) Math.min(Integer.MAX_VALUE,
                Math.max(0L, code.expiresAt() - dead.level().getGameTime()) + 50L);
        applyStackedJugg(livingKiller, remaining, dead);
    }

    private static void applyStackedJugg(LivingEntity target, int duration, LivingEntity source) {
        MobEffectInstance existing = target.getEffect(ModMobEffects.JUGG);
        if (existing == null) {
            target.addEffect(new MobEffectInstance(ModMobEffects.JUGG, duration, 1, false, false), source);
            return;
        }
        int newDuration = existing.getDuration() + 40 <= duration ? duration : existing.getDuration() + 10;
        int amplifier = Math.min(255, existing.getAmplifier() + 1);
        target.addEffect(new MobEffectInstance(ModMobEffects.JUGG, newDuration, amplifier, false, false), source);
    }

    private static void applyDeathAreaCodes(ServerLevel level, LivingEntity dead, SrpWorldData data) {
        int healing = Config.disloHealingDeath() ? activeValue(data, 7) : 0;
        if (healing > 0) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    dead.getBoundingBox().inflate(DEATH_RADIUS), entity -> entity instanceof Parasite)) {
                entity.heal(healing);
            }
            deathBurst(level, dead);
        }

        int damage = Config.disloDamageDeath() ? activeValue(data, 8) : 0;
        if (damage > 0) {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    dead.getBoundingBox().inflate(DEATH_RADIUS), entity -> !(entity instanceof Parasite))) {
                entity.hurt(level.damageSources().mobAttack(dead), damage);
            }
            deathBurst(level, dead);
        }

        int exhaustion = Config.disloFoodDeath() ? activeValue(data, 9) : 0;
        if (exhaustion > 0) {
            for (Player player : level.getEntitiesOfClass(Player.class,
                    dead.getBoundingBox().inflate(DEATH_RADIUS))) {
                player.causeFoodExhaustion(exhaustion);
            }
            deathBurst(level, dead);
        }
    }

    private static void deathBurst(ServerLevel level, LivingEntity dead) {
        level.playSound(null, dead.blockPosition(), ModSounds.RATHOL_BOOM.get(),
                SoundSource.HOSTILE, 0.2F, 0.7F);
        level.sendParticles(ParticleTypes.EXPLOSION, dead.getX(), dead.getY() + dead.getBbHeight() * 0.5D,
                dead.getZ(), 7, 0.6D, 0.6D, 0.6D, 0.05D);
    }

    private static void finishSummonByDeath(ServerLevel level, int accumulatedHealth) {
        LivingEntity target = strongestJuggHolder(level);
        if (target == null || target.getEffect(ModMobEffects.JUGG).getAmplifier()
                < Config.disloSummonByDeathKilling()) {
            return;
        }
        ResourceLocation payload = selectSummonPayload(accumulatedHealth);
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos position = findSpawnFloor(level, target.blockPosition().offset(
                    signedOffset(level), 0, signedOffset(level)));
            if (position != null && spawnPayloadWorm(level, target, position, payload)) {
                return;
            }
        }
    }

    private static LivingEntity strongestJuggHolder(ServerLevel level) {
        LivingEntity strongest = null;
        int amplifier = Integer.MIN_VALUE;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            MobEffectInstance effect = living.getEffect(ModMobEffects.JUGG);
            if (effect != null && effect.getAmplifier() >= amplifier) {
                amplifier = effect.getAmplifier();
                strongest = living;
            }
        }
        return strongest;
    }

    private static boolean spawnPayloadWorm(ServerLevel level, LivingEntity target, BlockPos position,
            ResourceLocation payload) {
        DeterrentParasiteEntity worm = ModEntities.WORM.get().create(level);
        if (worm == null) {
            return false;
        }
        worm.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D,
                level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(worm, worm.getBoundingBox().inflate(1.0D, 7.0D, 1.0D))) {
            return false;
        }
        worm.setWormPayload(1, 1);
        worm.setWormPayloadTypes(List.of(payload));
        worm.setTarget(target);
        worm.finalizeSpawn(level, level.getCurrentDifficultyAt(position), MobSpawnType.MOB_SUMMONED, null);
        return level.addFreshEntity(worm);
    }

    private static BlockPos findSpawnFloor(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.above(2).mutable();
        for (int depth = 0; depth <= 10; depth++) {
            BlockPos floor = cursor.below();
            BlockState state = level.getBlockState(cursor);
            if (state.getCollisionShape(level, cursor).isEmpty()
                    && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return cursor.immutable();
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private static ResourceLocation selectSummonPayload(int accumulatedHealth) {
        ResourceLocation fallback = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "warden");
        ResourceLocation selected = null;
        int selectedThreshold = Integer.MIN_VALUE;
        for (String entry : Config.disloSummonByDeathMobs()) {
            String[] parts = entry.split(";", -1);
            if (parts.length != 2) {
                continue;
            }
            try {
                int threshold = Integer.parseInt(parts[0]);
                ResourceLocation id = normalizeLegacyId(ResourceLocation.tryParse(parts[1]));
                if (id != null && selected == null) {
                    selected = id;
                }
                if (id != null && threshold < accumulatedHealth && threshold > selectedThreshold) {
                    selected = id;
                    selectedThreshold = threshold;
                }
            } catch (NumberFormatException ignored) {
                // Invalid user entries are skipped like missing legacy registry entries.
            }
        }
        return selected == null ? fallback : selected;
    }

    private static ResourceLocation normalizeLegacyId(ResourceLocation id) {
        if (id != null && "srparasites".equals(id.getNamespace())) {
            return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, id.getPath());
        }
        return id;
    }

    private static int signedOffset(ServerLevel level) {
        int value = level.getRandom().nextInt(4);
        return level.getRandom().nextBoolean() ? value : -value;
    }

    private static int activeValue(SrpWorldData data, int code) {
        DislodgmentCode entry = data.dislodgmentCode(code);
        return entry == null ? 0 : entry.value();
    }

    private static int saturatingMultiply(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) left * right));
    }

    private record ActivationRule(int code, int value, int durationSeconds, int pointCost) {
    }
}
