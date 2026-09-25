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
    /**
     * Original SRP divisor applied to the needed generation time for each SRP difficulty
     * ({@code SRPSaveData#getGenerationNeededTime(byte)}, switch over {@code choice}).
     * Easy stretches a generation, Impossible shortens it tenfold.
     */
    public static double generationDifficultyBonus(SrpDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 0.5D;
            case HARD -> 3.0D;
            case IMPOSSIBLE -> 10.0D;
            default -> 1.0D;
        };
    }

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

    /**
     * Point gain lock in seconds applied after a dimension enters {@code phase}, from the configurable
     * "phaseDelaySeconds" list (original SRP {@code SRPConfigSystems} "Phase # Delay"). The port ships
     * every phase at 0 seconds, so a phase change never blocks point gain unless a value is configured.
     */
    public static int phaseDelaySeconds(int phase) {
        return Config.phaseDelaySeconds(phase);
    }

    /** True when the current evolution phase needs no extra generation time (original {@code generationPhaseNeeded}). */
    public static boolean generationPhaseAllowed(int generation, int phase) {
        if (generation < 0 || generation > 4) {
            return true;
        }
        for (Integer allowed : Config.generationPhases(generation)) {
            if (allowed != null && allowed == phase) {
                return true;
            }
        }
        return false;
    }

    /**
     * Original {@code SRPSaveData#getGenerationNeededTime(World, int)}: the difficulty scaled base time,
     * multiplied by the phase penalty when the evolution phase is outside the generation's phase list.
     */
    public static int generationNeededTicks(int generation, int phase, SrpDifficulty difficulty) {
        return generationNeededTicks(generation, phase, generationDifficultyBonus(difficulty));
    }

    static int generationNeededTicks(int generation, int phase, double difficultyBonus) {
        if (generation < 0 || generation > 4) {
            return 0;
        }
        double bonus = difficultyBonus <= 0.0D ? 1.0D : difficultyBonus;
        int needed = (int) Math.max(1L, Math.round(Config.generationTime(generation) / bonus));
        if (!generationPhaseAllowed(generation, phase)) {
            needed = (int) (needed * Config.generationPhasePenalty());
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
                geneMinimumDamage(generation),
                geneDamageCap(generation),
                geneLookWalls(generation),
                geneSprinting(generation),
                geneWaterLeap(generation),
                geneSpecialMoves(generation),
                geneAdaptation(generation),
                geneBlockSearch(generation),
                geneResidue(generation),
                geneOrb(generation),
                geneCoth(generation),
                genePoisonHealing(generation),
                geneMobHealing(generation),
                geneAttackSpeed(generation));
    }

    private static boolean geneMinimumDamage(int g) {
        return switch (g) {
            case 0 -> Config.generation0MinimumDamage();
            case 1 -> Config.generation1MinimumDamage();
            case 2 -> Config.generation2MinimumDamage();
            case 3 -> Config.generation3MinimumDamage();
            case 4 -> Config.generation4MinimumDamage();
            default -> Config.generation5MinimumDamage();
        };
    }

    private static boolean geneDamageCap(int g) {
        return switch (g) {
            case 0 -> Config.generation0DamageCap();
            case 1 -> Config.generation1DamageCap();
            case 2 -> Config.generation2DamageCap();
            case 3 -> Config.generation3DamageCap();
            case 4 -> Config.generation4DamageCap();
            default -> Config.generation5DamageCap();
        };
    }

    private static boolean geneLookWalls(int g) {
        return switch (g) {
            case 0 -> Config.generation0LookWalls();
            case 1 -> Config.generation1LookWalls();
            case 2 -> Config.generation2LookWalls();
            case 3 -> Config.generation3LookWalls();
            case 4 -> Config.generation4LookWalls();
            default -> Config.generation5LookWalls();
        };
    }

    private static boolean geneSprinting(int g) {
        return switch (g) {
            case 0 -> Config.generation0Sprinting();
            case 1 -> Config.generation1Sprinting();
            case 2 -> Config.generation2Sprinting();
            case 3 -> Config.generation3Sprinting();
            case 4 -> Config.generation4Sprinting();
            default -> Config.generation5Sprinting();
        };
    }

    private static boolean geneWaterLeap(int g) {
        return switch (g) {
            case 0 -> Config.generation0WaterLeap();
            case 1 -> Config.generation1WaterLeap();
            case 2 -> Config.generation2WaterLeap();
            case 3 -> Config.generation3WaterLeap();
            case 4 -> Config.generation4WaterLeap();
            default -> Config.generation5WaterLeap();
        };
    }

    private static boolean geneSpecialMoves(int g) {
        return switch (g) {
            case 0 -> Config.generation0SpecialMoves();
            case 1 -> Config.generation1SpecialMoves();
            case 2 -> Config.generation2SpecialMoves();
            case 3 -> Config.generation3SpecialMoves();
            case 4 -> Config.generation4SpecialMoves();
            default -> Config.generation5SpecialMoves();
        };
    }

    private static boolean geneAdaptation(int g) {
        return switch (g) {
            case 0 -> Config.generation0Adaptation();
            case 1 -> Config.generation1Adaptation();
            case 2 -> Config.generation2Adaptation();
            case 3 -> Config.generation3Adaptation();
            case 4 -> Config.generation4Adaptation();
            default -> Config.generation5Adaptation();
        };
    }

    private static boolean geneBlockSearch(int g) {
        return switch (g) {
            case 0 -> Config.generation0BlockSearch();
            case 1 -> Config.generation1BlockSearch();
            case 2 -> Config.generation2BlockSearch();
            case 3 -> Config.generation3BlockSearch();
            case 4 -> Config.generation4BlockSearch();
            default -> Config.generation5BlockSearch();
        };
    }

    private static boolean geneResidue(int g) {
        return switch (g) {
            case 0 -> Config.generation0Residue();
            case 1 -> Config.generation1Residue();
            case 2 -> Config.generation2Residue();
            case 3 -> Config.generation3Residue();
            case 4 -> Config.generation4Residue();
            default -> Config.generation5Residue();
        };
    }

    private static boolean geneOrb(int g) {
        return switch (g) {
            case 0 -> Config.generation0Orb();
            case 1 -> Config.generation1Orb();
            case 2 -> Config.generation2Orb();
            case 3 -> Config.generation3Orb();
            case 4 -> Config.generation4Orb();
            default -> Config.generation5Orb();
        };
    }

    private static float geneCoth(int g) {
        return (float) switch (g) {
            case 0 -> Config.generation0Coth();
            case 1 -> Config.generation1Coth();
            case 2 -> Config.generation2Coth();
            case 3 -> Config.generation3Coth();
            case 4 -> Config.generation4Coth();
            default -> Config.generation5Coth();
        };
    }

    private static float genePoisonHealing(int g) {
        return (float) switch (g) {
            case 0 -> Config.generation0PoisonHeal();
            case 1 -> Config.generation1PoisonHeal();
            case 2 -> Config.generation2PoisonHeal();
            case 3 -> Config.generation3PoisonHeal();
            case 4 -> Config.generation4PoisonHeal();
            default -> Config.generation5PoisonHeal();
        };
    }

    private static float geneMobHealing(int g) {
        return (float) switch (g) {
            case 0 -> Config.generation0MobHealing();
            case 1 -> Config.generation1MobHealing();
            case 2 -> Config.generation2MobHealing();
            case 3 -> Config.generation3MobHealing();
            case 4 -> Config.generation4MobHealing();
            default -> Config.generation5MobHealing();
        };
    }

    private static float geneAttackSpeed(int g) {
        return (float) switch (g) {
            case 0 -> Config.generation0AttackSpeed();
            case 1 -> Config.generation1AttackSpeed();
            case 2 -> Config.generation2AttackSpeed();
            case 3 -> Config.generation3AttackSpeed();
            case 4 -> Config.generation4AttackSpeed();
            default -> Config.generation5AttackSpeed();
        };
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
        return NaturalSpawnTables.canSpawnAtPhase(path, phase);
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
        } else if (path.equals("kirin")) {
            requiredDimension = Level.END;
            requiredPhase = 7;
        } else if (path.equals("draconite")) {
            requiredDimension = Level.NETHER;
            requiredPhase = 7;
        } else {
            return true;
        }
        ServerLevel source = level.getServer().getLevel(requiredDimension == null ? Level.END : requiredDimension);
        return source != null && SrpWorldData.get(source).evolutionPhase() >= requiredPhase;
    }

    public static int parasiteDeathPenalty(LivingEntity entity) {
        if (!(entity instanceof Parasite) || entity.hasEffect(ModMobEffects.DEBAR.get())
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
                    entity.addEffect(new MobEffectInstance(ModMobEffects.RAGE.get(), 1_200, 1, false, false));
                }
            }
        }
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

    /**
     * Per-generation gene flags and multipliers, in the exact order of the original
     * {@code SRPSaveData#getGeneModi(int)} / {@code getGeneModi2(int)} arrays.
     */
    public record GenerationProfile(
            boolean minimumDamage,
            boolean damageCap,
            boolean lookWalls,
            boolean sprinting,
            boolean waterLeap,
            boolean specialMoves,
            boolean adaptation,
            boolean blockSearch,
            boolean residue,
            boolean ordinaryOrb,
            float cothChance,
            float poisonHealing,
            float mobHealing,
            float attackSpeedMultiplier) {
    }
}
