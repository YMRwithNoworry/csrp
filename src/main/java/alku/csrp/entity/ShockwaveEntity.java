package alku.csrp.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Moving ground wave created by the Primitive Longarms special attack. */
public final class ShockwaveEntity extends Entity {
    private static final String OWNER_TAG = "Owner";
    private static final String REMAINING_DISTANCE_TAG = "RemainingDistance";
    private static final int MAX_LIFETIME_TICKS = 240;
    private static final double MOVEMENT_SPEED = 0.6D;
    private static final float BLOCK_BREAK_HARDNESS = 1.0F;

    private final Set<UUID> hitEntities = new HashSet<>();
    private UUID ownerUuid;
    private double remainingDistance;

    public ShockwaveEntity(EntityType<? extends ShockwaveEntity> type, Level level) {
        super(type, level);
    }

    public void configure(LongarmsEntity owner, LivingEntity target) {
        ownerUuid = owner.getUUID();
        Vec3 horizontal = target.position().subtract(owner.position()).multiply(1.0D, 0.0D, 1.0D);
        remainingDistance = horizontal.length();
        if (remainingDistance > 0.001D) {
            setDeltaMovement(horizontal.normalize().scale(MOVEMENT_SPEED));
            hasImpulse = true;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        LongarmsEntity owner = resolveOwner();
        Vec3 movement = getDeltaMovement();
        if (owner == null || !owner.isAlive() || tickCount > MAX_LIFETIME_TICKS
                || remainingDistance <= 0.0D || movement.horizontalDistanceSqr() < 0.001D
                || !level().getFluidState(blockPosition()).isEmpty()) {
            discard();
            return;
        }

        breakContactBlocks(owner, movement);
        AABB nextBounds = getBoundingBox().move(movement);
        if (!level().noCollision(this, nextBounds)) {
            discard();
            return;
        }
        move(MoverType.SELF, movement);
        remainingDistance -= movement.horizontalDistance();
        spawnGroundDebris();
        damageTargets(owner);
        if (horizontalCollision || remainingDistance <= 0.0D) {
            discard();
        }
    }

    private LongarmsEntity resolveOwner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity owner = serverLevel.getEntity(ownerUuid);
        return owner instanceof LongarmsEntity longarms ? longarms : null;
    }

    private void damageTargets(LongarmsEntity owner) {
        AABB impactArea = getBoundingBox().inflate(1.5D, 0.2D, 1.5D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), impactArea);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, impactArea,
                owner::isValidParasiteTarget)) {
            if (hitEntities.add(target.getUUID())) {
                owner.hitWithShockwave(target);
            }
        }
    }

    private void breakContactBlocks(LongarmsEntity owner, Vec3 movement) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 next = position().add(movement);
        BlockPos center = BlockPos.containing(next.x, next.y, next.z);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 3; y++) {
                    BlockPos candidate = center.offset(x, y, z);
                    BlockState state = level().getBlockState(candidate);
                    float hardness = state.getDestroySpeed(level(), candidate);
                    if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                            || hardness < 0.0F || hardness > BLOCK_BREAK_HARDNESS) {
                        continue;
                    }
                    level().destroyBlock(candidate, true, owner);
                }
            }
        }
    }

    private void spawnGroundDebris() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState ground = level().getBlockState(blockPosition().below());
        if (ground.isAir()) {
            return;
        }
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                getX(), getY() + 0.1D, getZ(), 35,
                1.6D, 0.12D, 1.6D, 0.08D);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID(OWNER_TAG)) {
            ownerUuid = tag.getUUID(OWNER_TAG);
        }
        remainingDistance = tag.getDouble(REMAINING_DISTANCE_TAG);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID(OWNER_TAG, ownerUuid);
        }
        tag.putDouble(REMAINING_DISTANCE_TAG, remainingDistance);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
