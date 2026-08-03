package alku.csrp.world;

import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Phase, cooldown, cap, and colony rules shared by all reinforcement triggers. */
public final class ReinforcementSystem {
    private static final int MAX_BECKONS = 5;
    private static final int COOLDOWN_TICKS = 40;
    private static final double MINIMUM_BECKON_DISTANCE = 32.0D;
    private static final float INFESTED_BLOCK_CHANCE = 0.05F;
    private static final int[] RESIDUE_INTERVAL = {
            0, 0, 0, 5_500, 4_000, 1_000, 500, 400, 300, 200, 150
    };
    private static final float[] DEATH_CHANCE = {
            0.0F, 0.0F, 0.0F, 0.04F, 0.06F, 0.08F, 0.10F, 0.14F, 0.16F, 0.18F, 0.20F
    };

    private ReinforcementSystem() {
    }

    public static void tryFromResidue(ServerLevel level, BlockPos pos, RandomSource random) {
        int phase = phase(level);
        if (phase >= 3 && random.nextInt(RESIDUE_INTERVAL[phase]) == 0) {
            trySpawn(level, pos, random);
        }
    }

    public static void tryFromParasiteDeath(ServerLevel level, BlockPos pos, float width, float height,
                                             RandomSource random) {
        int phase = phase(level);
        if ((width > 1.0F || height > 1.0F) && phase >= 3 && random.nextFloat() < DEATH_CHANCE[phase]) {
            trySpawn(level, pos, random);
        }
    }

    public static void tryFromInfestedBlock(ServerLevel level, BlockPos pos, RandomSource random) {
        if (phase(level) >= 3 && random.nextFloat() < INFESTED_BLOCK_CHANCE && !hasNearbyBeckon(level, pos)) {
            trySpawn(level, pos, random);
        }
    }

    private static int phase(ServerLevel level) {
        return Math.max(0, Math.min(10, SrpWorldData.get(level).evolutionPhase()));
    }

    private static boolean trySpawn(ServerLevel level, BlockPos origin, RandomSource random) {
        SrpWorldData data = SrpWorldData.get(level);
        if (!data.reinforcementReady(level) || countBeckons(level) >= MAX_BECKONS || hasNearbyBeckon(level, origin)) {
            return false;
        }
        BlockPos spawnPos = resolveSpawnPos(level, origin);
        EntityType<NexusParasiteEntity> type = reinforcementType(data.totalColonyPoints());
        NexusParasiteEntity beckon = type.create(level);
        if (beckon == null) {
            return false;
        }
        beckon.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                random.nextFloat() * 360.0F, 0.0F);
        beckon.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null);
        if (!level.noCollision(beckon)) {
            beckon.discard();
            return false;
        }
        level.addFreshEntity(beckon);
        data.startReinforcementCooldown(level, COOLDOWN_TICKS);
        return true;
    }

    private static BlockPos resolveSpawnPos(ServerLevel level, BlockPos origin) {
        if (!level.getBlockState(origin).isAir()) {
            return origin.above();
        }
        if (Block.canSupportCenter(level, origin.below(), Direction.UP)) {
            return origin;
        }
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin);
    }

    private static EntityType<NexusParasiteEntity> reinforcementType(int colonyPoints) {
        if (colonyPoints > 40) {
            return ModEntities.BECKON_SIII.get();
        }
        if (colonyPoints > 20) {
            return ModEntities.BECKON_SII.get();
        }
        return ModEntities.BECKON_SI.get();
    }

    private static int countBeckons(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof NexusParasiteEntity nexus && isBeckon(nexus)) {
                count++;
                if (count >= MAX_BECKONS) {
                    break;
                }
            }
        }
        return count;
    }

    private static boolean hasNearbyBeckon(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(NexusParasiteEntity.class,
                new AABB(pos).inflate(MINIMUM_BECKON_DISTANCE), ReinforcementSystem::isBeckon).isEmpty();
    }

    private static boolean isBeckon(NexusParasiteEntity nexus) {
        EntityType<?> type = nexus.getType();
        return type == ModEntities.BECKON_SI.get() || type == ModEntities.BECKON_SII.get()
                || type == ModEntities.BECKON_SIII.get() || type == ModEntities.BECKON_SIV.get();
    }
}
