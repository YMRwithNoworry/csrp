package alku.csrp.world;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Original SRP evolution thresholds, generation timing, and point sources. */
public final class EvolutionSystem {
    public static final int VALUE_KILL = 1;
    public static final int VALUE_COTH = 6;
    public static final int VALUE_BLOCK = 6;
    public static final int VALUE_MERGE = 9;
    public static final int VALUE_EVOLUTION_DESPAWN = 100;
    public static final int MAX_EVOLUTION_POINTS = 2_100_000_000;

    private static final int[] PHASE_THRESHOLDS = {
            0, 800, 1_600, 5_000, 30_000, 200_000,
            5_000_000, 25_000_000, 500_000_000, 1_000_000_000, 1_800_000_000
    };
    private static final int[] PHASE_COOLDOWN_SECONDS = {
            0, 4_000, 4_800, 4_700, 4_500, 4_200, 3_800, 3_700, 3_700, 3_800, 6_000
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
        while (phase < 10 && points > PHASE_THRESHOLDS[phase + 1]) {
            phase++;
        }
        return phase;
    }

    public static int cooldownSecondsForPhase(int phase) {
        return phase < 1 || phase > 10 ? 0 : PHASE_COOLDOWN_SECONDS[phase];
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
        int generation = Math.max(0, Math.min(5, SrpWorldData.get(level).generation()));
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
        return SrpWorldData.get(level).addEvolutionPoints(level, points, false);
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
        String direction = current > previous ? "advanced" : "decreased";
        Component message = Component.literal("Parasite evolution phase " + direction + " to " + current);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(message);
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
        COMMAND
    }

    public record InitialProgress(int phase, int points) {
    }

    public record GenerationProfile(float cothChance, boolean sprinting, boolean adaptation,
            boolean specialMoves, boolean damageCap, boolean minimumDamage,
            boolean blockSearch, boolean ordinaryOrb,
            float poisonHealing, float mobHealing, float attackSpeedMultiplier) {
    }
}
