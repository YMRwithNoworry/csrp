package alku.csrp.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class PullingBallEntity extends Entity {
    private UUID ownerId;

    public PullingBallEntity(EntityType<? extends PullingBallEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public void setOwner(Entity owner) {
        ownerId = owner.getUUID();
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        HitResult hit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (!level().isClientSide && hit.getType() == HitResult.Type.BLOCK) {
            placeWebs(BlockPos.containing(hit.getLocation()));
            discard();
            return;
        }

        setPos(end.x, end.y, end.z);
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.POOF, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            return;
        }

        PullingBallOwner owner = owner();
        if (owner == null || !owner.isAlive() || tickCount > 80) {
            discard();
            return;
        }
        if (tickCount == 5) setDeltaMovement(getDeltaMovement().scale(2.0));

        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.7),
                owner::isValidPullTarget)) {
            if (owner.captureTarget(target)) {
                discard();
                return;
            }
        }
    }

    private PullingBallOwner owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof PullingBallOwner owner ? owner : null;
    }

    private void placeWebs(BlockPos center) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        int total = random.nextInt(3) + 1;
        for (int i = 0; i < total; i++) {
            BlockPos pos = center.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (level().isEmptyBlock(pos)) level().setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("owner", ownerId);
    }
}
