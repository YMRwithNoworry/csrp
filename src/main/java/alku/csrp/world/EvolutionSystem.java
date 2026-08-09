package alku.csrp.world;

import alku.csrp.Config;
import alku.csrp.entity.AbominationEntity;
import alku.csrp.entity.DerivedParasiteEntity;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Set;

/** Original SRP evolution thresholds, generation timing, and point sources. */
public final class EvolutionSystem {
    public static final int VALUE_KILL = 1;
    public static final int VALUE_CYST = 2;
    public static final int VALUE_COTH = 6;
    public static final int VALUE_BLOCK = 6;
    public static final int VALUE_MERGE = 9;
    public static final int VALUE_EVOLUTION_DESPAWN = 100;
    public static final int VALUE_NIDUS_FAILURE = 120;
    public static final int MAX_EVOLUTION_POINTS = 2_100_000_000;

    private static final int[] PHASE_THRESHOLDS = {
            0, 800, 1_600, 5_000, 30_000, 200_000,
            5_000_000, 25_000_000, 500_000_000, 1_000_000_000, 1_800_000_000
    };
    private static final int[] PHASE_DELAY_SECONDS = {
            0, 4_000, 4_800, 4_700, 4_500, 4_200, 3_800, 3_700, 3_700, 3_800, 6_000
    };
    private static final int[] SLEEP_POINTS = {3, 10, 25, 50, 100, 2_500, 8_500, 12_500, 15_000, 18_000, 1};
    private static final double[] PASSIVE_POINTS_PER_SECOND = {
            0.0D, 0.0D, 0.0D, 0.05D, 0.075D, 0.1D, 0.15D, 0.25D, 0.35D, 0.45D, 0.55D
    };
    private static final float[] PHASE_COTH_CHANCE = {
            0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.20F, 0.40F, 0.80F, 0.90F, 1.0F
    };
    private static final float[] CROP_BLOCK_CHANCE = {
            0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.10F, 0.30F, 0.60F, 1.0F, 1.0F
    };
    private static final int[] GENERATION_TIME_TICKS = {25_000, 45_000, 72_000, 72_000, 72_000};
    private static final int[][] GENERATION_PHASES = {
            {1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
            {3, 4, 5, 6, 7, 8, 9, 10},
            {5, 6, 7, 8, 9, 10},
            {7, 8, 9, 10},
            {9, 10}
    };
    private static final float[] GENERATION_COTH_CHANCE = {0.2F, 0.3F, 0.65F, 1.0F, 1.0F, 1.0F};
    private static final boolean[] GENERATION_SPRINTING = {false, false, true, true, true, true};
    private static final boolean[] GENERATION_ADAPTATION = {false, false, false, true, true, true};
    private static final boolean[] GENERATION_SPECIAL_MOVES = {false, false, false, false, true, true};
    private static final boolean[] GENERATION_DAMAGE_CAP = {false, false, false, true, true, true};
    private static final boolean[] GENERATION_MINIMUM_DAMAGE = {false, false, true, true, true, true};
    private static final boolean[] GENERATION_BLOCK_SEARCH = {false, false, false, false, false, true};
    private static final boolean[] GENERATION_ORDINARY_ORB = {false, false, false, false, false, true};
    private static final float[] GENERATION_POISON_HEALING = {0.0F, 0.3F, 1.0F, 1.5F, 2.0F, 2.5F};
    private static final float[] GENERATION_MOB_HEALING = {0.0F, 0.0F, 0.5F, 1.0F, 2.0F, 3.0F};
    private static final float[] GENERATION_ATTACK_SPEED = {1.0F, 1.0F, 1.0F, 0.9F, 0.7F, 0.5F};
    private static final Set<String> PHASE_MINUS_ONE_SPAWNS = Set.of(
            "rupter", "sim_squid", "sim_bigspider", "sim_human", "sim_cow", "sim_sheep",
            "sim_wolf", "sim_pig", "sim_villager", "sim_adventurer", "sim_horse", "sim_bear",
            "sim_enderman", "host", "heed", "pri_devourer", "pri_longarms", "pri_manducater",
            "pri_reeker", "pri_yelloweye", "pri_summoner", "pri_bolster", "pri_arachnida", "thrall");
    private static final Set<String> NATURAL_ASSIMILATED = Set.of(
            "sim_squid", "sim_bigspider", "sim_human", "sim_cow", "sim_sheep", "sim_wolf",
            "sim_pig", "sim_villager", "sim_adventurer", "sim_horse", "sim_bear", "sim_enderman");
    private static final Set<String> NATURAL_FERAL = Set.of(
            "fer_human", "fer_cow", "fer_sheep", "fer_wolf", "fer_pig", "fer_villager",
            "fer_horse", "fer_enderman");

    private EvolutionSystem() {
    }

    public static InitialProgress initialProgress(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) {
            return new InitialProgress(0, 0);
        }
        if (level.dimension() == Level.NETHER) {
            return new InitialProgress(-1, -50);
        }
        if (level.dimension() == Level.END) {
            return new InitialProgress(-1, -100);
        }
        return new InitialProgress(-1, -300);
    }

    public static int thresholdForPhase(int phase) {
        if (phase == -2) {
            return -200;
        }
        if (phase == -1) {
            return -100;
        }
        return PHASE_THRESHOLDS[Math.max(0, Math.min(10, phase))];
    }

    public static int phaseForPoints(int points) {
        if (points < 0) {
            return -1;
        }
        int phase = 0;
        while (phase < 10 && points >= PHASE_THRESHOLDS[phase + 1]) {
            phase++;
        }
        return phase;
    }

    public static int phaseDelaySeconds(int phase) {
        return phase < 0 ? 0 : PHASE_DELAY_SECONDS[Math.min(10, phase)];
    }

    public static int generationNeededTicks(int generation, int phase) {
        if (generation < 0 || generation >= GENERATION_TIME_TICKS.length) {
            return 0;
        }
        int needed = GENERATION_TIME_TICKS[generation];
        if (!contains(GENERATION_PHASES[generation], phase)) {
            needed = Math.round(needed * 1.5F);
        }
        return needed;
    }

    public static GenerationProfile generationProfile(ServerLevel level) {
        int generation = Config.generationEnabled() ? SrpWorldData.get(level).generation() : 5;
        return generationProfile(generation);
    }

    static GenerationProfile generationProfile(int requestedGeneration) {
        int generation = Math.max(0, Math.min(5, requestedGeneration));
        return new GenerationProfile(
                GENERATION_COTH_CHANCE[generation],
                GENERATION_SPRINTING[generation],
                GENERATION_ADAPTATION[generation],
                GENERATION_SPECIAL_MOVES[generation],
                GENERATION_DAMAGE_CAP[generation],
                GENERATION_MINIMUM_DAMAGE[generation],
                GENERATION_BLOCK_SEARCH[generation],
                GENERATION_ORDINARY_ORB[generation],
                GENERATION_POISON_HEALING[generation],
                GENERATION_MOB_HEALING[generation],
                GENERATION_ATTACK_SPEED[generation]);
    }

    public static boolean addPoints(ServerLevel level, int points, PointSource source) {
        SrpWorldData data = SrpWorldData.get(level);
        return data.addDifficultyScaledEvolutionPoints(level, points);
    }

    public static int sleepPoints(int phase) {
        if (phase < 0) {
            return 0;
        }
        int points = SLEEP_POINTS[Math.min(10, phase)];
        return phase >= 6 ? points * 5 : points;
    }

    public static double passivePointsPerSecond(int phase) {
        return phase < 0 ? 0.0D : PASSIVE_POINTS_PER_SECOND[Math.min(10, phase)];
    }

    public static float phaseCothChance(int phase) {
        return phase < 0 ? 0.0F : PHASE_COTH_CHANCE[Math.min(10, phase)];
    }

    public static float cropGrowthBlockChance(int phase) {
        return phase < 0 ? 0.0F : CROP_BLOCK_CHANCE[Math.min(10, phase)];
    }

    public static boolean canNaturallySpawn(String path, int phase) {
        if (phase == -1) {
            return PHASE_MINUS_ONE_SPAWNS.contains(path);
        }
        if (phase < 0) {
            return false;
        }
        NaturalPhase range = naturalPhase(path);
        return range != null && phase >= range.minimum() && phase <= range.maximum();
    }

    public static boolean crossDimensionUnlocked(ServerLevel level, String path) {
        net.minecraft.resources.ResourceKey<Level> requiredDimension;
        int requiredPhase;
        if (path.equals("sim_enderman") || path.equals("sim_dragone")) {
            requiredDimension = null;
            requiredPhase = 3;
        } else if (path.equals("fer_enderman")) {
            requiredDimension = null;
            requiredPhase = 4;
        } else if (path.equals("draconite")) {
            requiredDimension = Level.NETHER;
            requiredPhase = 7;
        } else {
            return true;
        }
        ServerLevel source = level.getServer().getLevel(requiredDimension == null ? Level.END : requiredDimension);
        return source != null && SrpWorldData.get(source).evolutionPhase() >= requiredPhase;
    }

    private static NaturalPhase naturalPhase(String path) {
        if (path.equals("buglin")) return new NaturalPhase(0, 2);
        if (path.equals("rupter")) return new NaturalPhase(1, 7);
        if (path.equals("carrier_light")) return new NaturalPhase(1, 4);
        if (path.equals("carrier_heavy")) return new NaturalPhase(2, 4);
        if (path.equals("carrier_flying")) return new NaturalPhase(3, 4);
        if (path.equals("sim_dragone")) return new NaturalPhase(9, 10);
        if (NATURAL_ASSIMILATED.contains(path)) return new NaturalPhase(2, 7);
        if (path.equals("host")) return new NaturalPhase(3, 6);
        if (path.equals("hostii")) return new NaturalPhase(7, 10);
        if (path.equals("lice")) return new NaturalPhase(3, 10);
        if (path.equals("heed") || path.startsWith("mar_")) return new NaturalPhase(4, 10);
        if (path.equals("crux") || path.equals("dredge")) return new NaturalPhase(5, 10);
        if (path.equals("mangler") || path.equals("abo_bodies")) return new NaturalPhase(6, 10);
        if (path.equals("airscrew") || path.equals("thrall")) return new NaturalPhase(7, 10);
        if (NATURAL_FERAL.contains(path) || path.equals("bomber_light")) return new NaturalPhase(8, 10);
        if (path.equals("worker") || path.equals("architect") || path.equals("bomber_heavy")
                || path.equals("wraith") || path.equals("bogle") || path.equals("haunter")
                || path.equals("carrier_colony") || path.equals("draconite") || path.equals("kirin")) {
            return new NaturalPhase(1, 10);
        }
        return null;
    }

    public static int parasiteDeathPenalty(LivingEntity entity) {
        if (!(entity instanceof Parasite) || entity.hasEffect(ModMobEffects.DEBAR)
                || entity instanceof DerivedParasiteEntity derived && derived.isShadowClone()) {
            return 0;
        }
        if (entity instanceof AbominationEntity abomination
                && abomination.getKind() == AbominationEntity.Kind.BODIES) {
            return 3;
        }
        if (entity instanceof NexusParasiteEntity nexus) {
            return switch (nexus.getKind().stage()) {
                case 1 -> 3;
                case 2 -> 15;
                case 3 -> 150;
                case 4 -> 20_000;
                default -> 0;
            };
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return switch (EvolutionTierCatalog.tier(id).toUpperCase(java.util.Locale.ROOT)) {
            case "ASSIMILATED" -> 1;
            case "ASSIMARA", "FERAL" -> 5;
            case "PRIMITIVE" -> 10;
            case "ADAPTED" -> 200;
            case "DETERRENT", "PURE" -> 1_000;
            case "PREEMINENT" -> 30_000;
            case "DERIVED" -> 45_000;
            case "ANCIENT" -> 1;
            default -> 0;
        };
    }

    public static int ubiquitousDevelopment(MinecraftServer server) {
        int phasePoints = 0;
        int dimensions = 0;
        int override = SrpWorldData.get(server.overworld()).ubiquitousDevelopmentOverride();
        for (ServerLevel level : server.getAllLevels()) {
            SrpWorldData data = SrpWorldData.get(level);
            if (data.evolutionPhase() > 0) {
                phasePoints += data.evolutionPhase();
                dimensions++;
            }
        }
        if (override > 0) {
            return override;
        }
        if (phasePoints >= 14 && dimensions >= 2) return 4;
        if (phasePoints >= 10 && dimensions >= 2) return 3;
        if (phasePoints >= 7 && dimensions >= 2) return 2;
        return phasePoints >= 4 && dimensions >= 1 ? 1 : 0;
    }

    public static void setUbiquitousDevelopmentOverride(MinecraftServer server, int level) {
        SrpWorldData.get(server.overworld()).setUbiquitousDevelopment(level);
    }

    public static void announcePhaseChange(ServerLevel level, int previous, int current) {
        boolean advanced = current > previous;
        Component message = Component.translatable(advanced
                ? "message.csrp.evolution.phase_advanced" : "message.csrp.evolution.phase_decreased", current);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
            if (advanced && current >= 1) {
                player.playNotifySound(ModSounds.evolutionPhase(current), SoundSource.MASTER, 1.0F, 1.0F);
            }
        }
        if (!advanced) {
            for (var rawEntity : level.getAllEntities()) {
                if (rawEntity instanceof LivingEntity entity && entity instanceof Parasite) {
                    entity.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1_200, 1, false, false));
                }
            }
        }
    }

    private static boolean contains(int[] values, int target) {
        for (int value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }

    public enum PointSource {
        KILL,
        COTH,
        BLOCK_CONVERSION,
        MERGE,
        EVOLUTION_DESPAWN,
        CYST,
        NIDUS_FAILURE,
        VECTOR_DAILY,
        SLEEP,
        PASSIVE,
        PARASITE_DEATH,
        BLOCK_BREAK,
        COMMAND

        ;
    }

    public record InitialProgress(int phase, int points) {
    }

    private record NaturalPhase(int minimum, int maximum) {
    }

    public record GenerationProfile(float cothChance, boolean sprinting, boolean adaptation,
            boolean specialMoves, boolean damageCap, boolean minimumDamage,
            boolean blockSearch, boolean ordinaryOrb,
            float poisonHealing, float mobHealing, float attackSpeedMultiplier) {
    }
}
