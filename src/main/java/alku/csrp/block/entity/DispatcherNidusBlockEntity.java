package alku.csrp.block.entity;

import alku.csrp.registry.ModBlockEntities;
import alku.csrp.registry.ModEntities;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Dispatcher Nidus block entity. Collects killcount from nearby parasites and
 * either spawns a Stage I Dispatcher once it reaches 40, or grants evolution
 * points when a Dispatcher is already nearby.
 */
public final class DispatcherNidusBlockEntity extends BlockEntity {
    public static final int SPAWN_KILLS = 40;
    private static final double NEARBY_DISPATCHER_RANGE = 24.0D;

    private int killCount;

    public DispatcherNidusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISPATCHER_NIDUS.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(blockEntity instanceof DispatcherNidusBlockEntity nidus) || level.isClientSide) {
            return;
        }
        nidus.tickNidus((ServerLevel) level);
    }

    private void tickNidus(ServerLevel level) {
        if (killCount < SPAWN_KILLS) {
            return;
        }
        if (hasNearbyDispatcher(level)) {
            killCount = 0;
            EvolutionSystem.addPoints(level, EvolutionSystem.VALUE_NIDUS_FAILURE,
                    EvolutionSystem.PointSource.NIDUS_FAILURE);
            setChanged();
            return;
        }
        var dispatcher = ModEntities.DISPATCHER_SI.get().create(level);
        if (dispatcher == null) {
            return;
        }
        BlockPos pos = getBlockPos();
        dispatcher.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                level.getRandom().nextFloat() * 360.0F, 0.0F);
        dispatcher.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null);
        level.addFreshEntity(dispatcher);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private boolean hasNearbyDispatcher(ServerLevel level) {
        return !level.getEntitiesOfClass(alku.csrp.entity.NexusParasiteEntity.class,
                new AABB(getBlockPos()).inflate(NEARBY_DISPATCHER_RANGE),
                entity -> entity.getKind() == alku.csrp.entity.NexusParasiteEntity.Kind.DISPATCHER_SI)
                .isEmpty();
    }

    public void addKill() {
        killCount++;
        setChanged();
    }

    public int killCount() {
        return killCount;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("KillCount", killCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        killCount = tag.getInt("KillCount");
    }
}
