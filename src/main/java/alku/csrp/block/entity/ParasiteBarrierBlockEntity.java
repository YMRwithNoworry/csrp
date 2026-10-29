package alku.csrp.block.entity;

import alku.csrp.client.ParasiteBarrierClientTick;
import alku.csrp.registry.ModBlockEntities;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** 原件 TileEntityParasiteBarrier：服务器驱离、目标清理与客户端边界粒子。 */
public final class ParasiteBarrierBlockEntity extends BlockEntity {
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 10;
    private static final int TICK_INTERVAL = 2;
    private static final int TELEPORT_AFTER_TICKS = 100;
    private static final double SEARCH_MARGIN = 48.0D;
    private static final double OUTWARD_OFFSET = 0.6D;
    private int centerChunkX;
    private int centerChunkZ;
    private int radiusChunks = MIN_RADIUS;
    private boolean centerInitialized;
    private final Map<Integer, Integer> insideTicks = new HashMap<>();

    public ParasiteBarrierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARASITE_BARRIER.get(), pos, state);
    }

    public void initializeCenter(BlockPos pos) {
        centerChunkX = pos.getX() >> 4;
        centerChunkZ = pos.getZ() >> 4;
        centerInitialized = true;
        setChanged();
        syncToClients();
    }

    public int radiusChunks() {
        return radiusChunks;
    }

    public void cycleRadius(ServerLevel level) {
        setRadius(level, radiusChunks % MAX_RADIUS + 1);
    }

    public void setRadius(ServerLevel level, int radius) {
        int next = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
        if (radiusChunks == next) {
            return;
        }
        radiusChunks = next;
        setChanged();
        syncToClients();
        scrubMobsAroundField(level);
    }

    public int[] currentWorldBounds() {
        int minChunkX = centerChunkX - radiusChunks;
        int maxChunkX = centerChunkX + radiusChunks;
        int minChunkZ = centerChunkZ - radiusChunks;
        int maxChunkZ = centerChunkZ + radiusChunks;
        return new int[] {minChunkX * 16, (maxChunkX + 1) * 16 - 1,
                minChunkZ * 16, (maxChunkZ + 1) * 16 - 1};
    }

    public void tick() {
        if (level == null) {
            return;
        }
        if (level.isClientSide()) {
            clientTick(level);
        } else if (level instanceof ServerLevel serverLevel && level.getGameTime() % TICK_INTERVAL == 0L) {
            serverTick(serverLevel);
        }
    }

    private void serverTick(ServerLevel level) {
        int[] bounds = currentWorldBounds();
        AABB search = searchBox(level, bounds);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, search)) {
            clearCrossFactionAggroIfTargetInside(mob, bounds);
        }
        List<Entity> parasites = level.getEntities((Entity) null, search,
                ParasiteBarrierBlockEntity::isParasiteEntity);
        Set<Integer> observedIds = new HashSet<>();
        for (Entity entity : parasites) {
            observedIds.add(entity.getId());
            clearAggroIfTargetInside(entity, bounds);
            if (entity instanceof Mob mob) {
                clearPathIfTouchesInside(mob, bounds);
            }
            if (!isInside(bounds, entity)) {
                insideTicks.remove(entity.getId());
                continue;
            }
            pushOrTeleportOutside(level, bounds, entity);
            if (isInside(bounds, entity)) {
                int ticks = insideTicks.getOrDefault(entity.getId(), 0) + TICK_INTERVAL;
                if (ticks >= TELEPORT_AFTER_TICKS) {
                    pushOrTeleportOutside(level, bounds, entity);
                    pushOrTeleportOutside(level, bounds, entity);
                    ticks = 0;
                }
                insideTicks.put(entity.getId(), ticks);
            } else {
                insideTicks.remove(entity.getId());
            }
        }
        insideTicks.keySet().removeIf(id -> !observedIds.contains(id));
    }

    private static AABB searchBox(ServerLevel level, int[] bounds) {
        return new AABB(bounds[0] - SEARCH_MARGIN, level.getMinY(), bounds[2] - SEARCH_MARGIN,
                bounds[1] + SEARCH_MARGIN, level.getMaxY() + 1.0D, bounds[3] + SEARCH_MARGIN);
    }

    public void scrubMobsAroundField(ServerLevel level) {
        int[] bounds = currentWorldBounds();
        AABB search = searchBox(level, bounds);
        for (Entity entity : level.getEntities((Entity) null, search,
                ParasiteBarrierBlockEntity::isParasiteEntity)) {
            clearAggroIfTargetInside(entity, bounds);
            if (entity instanceof Mob mob) {
                clearPathIfTouchesInside(mob, bounds);
            }
        }
    }

    private static boolean isParasiteEntity(Entity entity) {
        return entity.getType().builtInRegistryHolder().unwrapKey()
                .map(key -> alku.csrp.Csrp.MODID.equals(key.identifier().getNamespace()))
                .orElse(false);
    }

    private static boolean isInside(int[] bounds, Entity entity) {
        return entity.getX() >= bounds[0] && entity.getX() <= bounds[1]
                && entity.getZ() >= bounds[2] && entity.getZ() <= bounds[3];
    }

    private static void clearCrossFactionAggroIfTargetInside(Mob mob, int[] bounds) {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            target = mob.getLastHurtByMob();
        }
        if (target != null && isInside(bounds, target)
                && (isParasiteEntity(mob) || isParasiteEntity(target))) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            mob.getNavigation().stop();
        }
    }

    private static void clearAggroIfTargetInside(Entity entity, int[] bounds) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target != null && isInside(bounds, target)) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            mob.getNavigation().stop();
            return;
        }
        LivingEntity attacker = mob.getLastHurtByMob();
        if (attacker != null && isInside(bounds, attacker)) {
            mob.setLastHurtByMob(null);
            mob.getNavigation().stop();
        }
    }

    private static void clearPathIfTouchesInside(Mob mob, int[] bounds) {
        Path path = mob.getNavigation().getPath();
        if (path == null) {
            return;
        }
        for (int i = 0; i < path.getNodeCount(); i++) {
            BlockPos node = path.getNode(i).asBlockPos();
            if (node.getX() >= bounds[0] && node.getX() <= bounds[1]
                    && node.getZ() >= bounds[2] && node.getZ() <= bounds[3]) {
                mob.getNavigation().stop();
                LivingEntity target = mob.getTarget();
                if (target != null && isInside(bounds, target)) {
                    mob.setTarget(null);
                    mob.setLastHurtByMob(null);
                }
                return;
            }
        }
    }

    private static void pushOrTeleportOutside(ServerLevel level, int[] bounds, Entity entity) {
        double x = entity.getX();
        double z = entity.getZ();
        double[] distances = {Math.abs(x - bounds[0]), Math.abs(bounds[1] - x),
                Math.abs(z - bounds[2]), Math.abs(bounds[3] - z)};
        int edge = 0;
        for (int index = 1; index < distances.length; index++) {
            if (distances[index] < distances[edge]) {
                edge = index;
            }
        }
        double targetX = x;
        double targetZ = z;
        if (edge == 0) targetX = bounds[0] - OUTWARD_OFFSET;
        else if (edge == 1) targetX = bounds[1] + OUTWARD_OFFSET;
        else if (edge == 2) targetZ = bounds[2] - OUTWARD_OFFSET;
        else targetZ = bounds[3] + OUTWARD_OFFSET;

        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,
                BlockPos.containing(targetX, 0.0D, targetZ));
        entity.teleportTo(targetX, surface.getY() + 0.1D, targetZ);
        Vec3 movement = entity.getDeltaMovement();
        entity.setDeltaMovement(movement.x * 0.1D, Math.min(movement.y, 0.0D), movement.z * 0.1D);
        entity.resetFallDistance();
    }

    private void clientTick(Level level) {
        alku.csrp.client.ParasiteBarrierClientTick.tick(this, level);
    }

    private void syncToClients() {
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("centerChunkX", centerChunkX);
        output.putInt("centerChunkZ", centerChunkZ);
        output.putInt("radiusChunks", radiusChunks);
        output.putBoolean("CenterInitialized", centerInitialized);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        centerChunkX = readInt(input, "centerChunkX", "CenterChunkX", 0);
        centerChunkZ = readInt(input, "centerChunkZ", "CenterChunkZ", 0);
        int storedRadius = readInt(input, "radiusChunks", "RadiusChunks", MIN_RADIUS);
        radiusChunks = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, storedRadius));
        boolean hasStoredCenter = hasInt(input, "centerChunkX") && hasInt(input, "centerChunkZ")
                || hasInt(input, "CenterChunkX") && hasInt(input, "CenterChunkZ");
        centerInitialized = input.getBooleanOr("CenterInitialized", hasStoredCenter);
        if (!centerInitialized) {
            centerChunkX = worldPosition.getX() >> 4;
            centerChunkZ = worldPosition.getZ() >> 4;
            centerInitialized = true;
        }
    }

    private static int readInt(ValueInput input, String legacyKey, String temporaryKey, int fallback) {
        return input.getIntOr(legacyKey, input.getIntOr(temporaryKey, fallback));
    }

    private static boolean hasInt(ValueInput input, String key) {
        return input.getIntOr(key, Integer.MIN_VALUE) != Integer.MIN_VALUE;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
