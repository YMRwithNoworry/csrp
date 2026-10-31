package alku.csrp.block;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.entity.WorkerEntity;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModBlocks;
import alku.csrp.world.ColonyStructureGenerator;
import alku.csrp.world.SrpWorldData;
import alku.csrp.world.SrpWorldData.ColonyEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.RandomSource;

/**
 * Port of the 1.12.2 {@code com.dhanantry.scapeandrunparasites.block.BlockColonyCore} as registered
 * for {@code srparasites:colonyoutpost} ({@code init/SRPBlocks.java:486}:
 * {@code new BlockColonyCore(Material.ROCK, "colonyoutpost", 30.0F, true, true, 1200.0F)}).
 *
 * <p>This is the separate legacy outpost, not the worker-placed construction marker. It random-ticks
 * only when its 3-chunk neighborhood is loaded and colonies are enabled, checks for a nearby colony,
 * spawns two workers when no worker is nearby, and follows the original stage-specific random branch
 * table and pillar-building path.</p>
 */
public final class ColonyOutpostBlock extends Block {
    /** Shared with {@link SrpCoreBlock} / {@link ColonyStructureBlock}. */
    public static final IntegerProperty ACTIVE = SrpCoreBlock.ACTIVE;

    public ColonyOutpostBlock(Properties properties) {
        super(properties.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.hasChunksAt(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))
                || !WorldConfig.coloniesActivated()
                || SrpWorldData.get(level).nearestColonyInConstructionRange(pos) == null) {
            return;
        }

        int stage = state.getValue(ACTIVE);
        if (stage <= 0) {
            return;
        }

        AABB workerSearch = new AABB(pos).inflate(20.0D, 5.0D, 20.0D);
        if (level.getEntitiesOfClass(WorkerEntity.class, workerSearch).isEmpty()) {
            spawnWorkerFromBlock(level, pos.above(), 5);
            spawnWorkerFromBlock(level, pos.above(), 5);
        }

        if (stage == 1) {
            if (!random.nextBoolean()) {
                return;
            }
            ColonyStructureGenerator.ColonyBuilding building;
            if (random.nextInt(4) == 0) {
                building = ColonyStructureGenerator.ColonyBuilding.B3;
            } else {
                building = random.nextBoolean()
                        ? ColonyStructureGenerator.ColonyBuilding.B2
                        : ColonyStructureGenerator.ColonyBuilding.B1;
            }
            if (makePillar(level, pos, 23, 4)) {
                ColonyStructureGenerator.generateBuilding(level, pos.above(), building, 2, RandomSource.create());
            }
        } else if (stage == 2 && random.nextBoolean()) {
            // Both arms of the original second boolean branch construct BS1; retain the RNG draw.
            random.nextBoolean();
            if (makePillar(level, pos, 13, 3)) {
                ColonyStructureGenerator.generateBuilding(level, pos.above(),
                        ColonyStructureGenerator.ColonyBuilding.BS1, 2, RandomSource.create());
            }
        }

    }

    /** Mirrors the single-entry {@code ParasiteEventEntity.spawnFromBlock(..., worker;1, 5, pos)} call. */
    private static boolean spawnWorkerFromBlock(ServerLevel level, BlockPos origin, int offsetRange) {
        int parasiteCount = 0;
        int architectCount = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof alku.csrp.entity.Parasite) {
                parasiteCount++;
            }
            if (entity instanceof alku.csrp.entity.ArchitectEntity) {
                architectCount++;
            }
            if (parasiteCount > WorldConfig.worldMobCap() || architectCount > 5) {
                return false;
            }
        }

        RandomSource spawnRandom = RandomSource.create();
        for (int attempt = 0; attempt < 2; attempt++) {
            int offsetX = spawnRandom.nextInt(offsetRange);
            int offsetZ = spawnRandom.nextInt(offsetRange);
            if (spawnRandom.nextInt(2) == 0) {
                offsetX = -offsetX;
            }
            if (spawnRandom.nextInt(2) == 0) {
                offsetZ = -offsetZ;
            }
            spawnRandom.nextInt(1); // The source chooses from the one-entry worker table.
            if (spawnRandom.nextDouble() > 1.0D) { // Original worker entry chance is exactly 1.0.
                continue;
            }

            BlockPos candidate = BlockPos.containing(origin.getX() + offsetX, origin.getY(),
                    origin.getZ() + offsetZ);
            BlockPos floor = findSpawnFloor(level, candidate, 3);
            if (floor == null) {
                continue;
            }

            WorkerEntity worker = ModEntities.WORKER.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (worker == null) {
                return false;
            }
            worker.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16.0D);
            worker.snapTo(floor.getX(), floor.getY(), floor.getZ(), 0.0F, 0.0F);
            worker.finalizeSpawn(level, level.getCurrentDifficultyAt(floor), EntitySpawnReason.MOB_SUMMONED, null);

            SrpWorldData data = SrpWorldData.get(level);
            ColonyEntry colony = data.nearestColonyInConstructionRange(floor);
            if (colony == null) {
                return false;
            }
            worker.setColonyTask(colony.pos(), WorkerEntity.colonyRadius(colony));
            level.addFreshEntity(worker);
            return true;
        }
        return false;
    }

    /** Exact recursive {@code ParasiteEventEntity#getFloor}: find vanilla air with non-air below. */
    private static BlockPos findSpawnFloor(ServerLevel level, BlockPos pos, int remaining) {
        if (remaining <= 0) {
            return null;
        }
        remaining--;
        if (level.getBlockState(pos).is(Blocks.AIR)) {
            if (!level.getBlockState(pos.below()).is(Blocks.AIR)) {
                return pos;
            }
            return findSpawnFloor(level, pos.below(), remaining);
        }
        return findSpawnFloor(level, pos.above(), remaining);
    }

    /** 1.10.9 BlockColonyOutpost#makePillar: consume open space until the pillar reaches its height. */
    private boolean makePillar(ServerLevel level, BlockPos outpost, int totalCheck, int blocks) {
        BlockPos pos = outpost.above();
        int checked = 0;
        while (blocks > 0) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.legacyBlock("parasitestain").get())) {
                checked++;
                pos = pos.above();
                if (checked >= totalCheck) {
                    return true;
                }
                continue;
            }

            checked++;
            blocks--;
            placePillarBlock(level, pos, blocks <= 0);
            pos = pos.above();
            if (checked >= totalCheck) {
                return true;
            }
        }
        return false;
    }

    /** Clear the original radius-10 construction layer, then cap it with flesh stain. */
    private void placePillarBlock(ServerLevel level, BlockPos pos, boolean finalLayer) {
        AABB itemSearch = new AABB(pos).inflate(32.0D, 16.0D, 32.0D);
        boolean placeActiveCanister = finalLayer
                && level.getEntitiesOfClass(ItemEntity.class, itemSearch).size() > 7;
        for (int x = pos.getX() - PILLAR_CLEAR_RADIUS; x <= pos.getX() + PILLAR_CLEAR_RADIUS; x++) {
            for (int z = pos.getZ() - PILLAR_CLEAR_RADIUS; z <= pos.getZ() + PILLAR_CLEAR_RADIUS; z++) {
                BlockPos target = new BlockPos(x, pos.getY(), z);
                BlockState state = level.getBlockState(target);
                float hardness = state.getDestroySpeed(level, target);
                if (!(hardness > PILLAR_MAX_CLEAR_HARDNESS) && !isSrpBlock(state)) {
                    level.destroyBlock(target, true);
                }
            }
        }

        level.setBlock(pos, ModBlocks.legacyBlock("parasitestain").get().defaultBlockState()
                .setValue(ParasiteStainBlock.VARIANT, ParasiteStainBlock.Variant.FLESH), Block.UPDATE_ALL);
        if (placeActiveCanister) {
            level.setBlock(pos.below(), ModBlocks.CANISTER_ACTIVE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static boolean isSrpBlock(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && Csrp.MODID.equals(id.getNamespace());
    }

    private static final int PILLAR_CLEAR_RADIUS = 10;
    private static final float PILLAR_MAX_CLEAR_HARDNESS = 7.0F;
}
