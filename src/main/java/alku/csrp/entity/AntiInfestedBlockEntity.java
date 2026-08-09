package alku.csrp.entity;

import alku.csrp.block.InfestedBlock;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class AntiInfestedBlockEntity extends Entity {
    private static final int HORIZONTAL_RANGE = 7;
    private static final int VERTICAL_RANGE = 5;
    private static final int MAX_LIFETIME = 100;

    public AntiInfestedBlockEntity(EntityType<? extends AntiInfestedBlockEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void launch(Vec3 start, Vec3 velocity) {
        setPos(start);
        setDeltaMovement(velocity);
        hasImpulse = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 start = position();
        Vec3 movement = getDeltaMovement();
        Vec3 end = start.add(movement);
        HitResult hit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        setPos(end);
        setDeltaMovement(movement.scale(0.99D).add(0.0D, -0.03D, 0.0D));
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            return;
        }
        if (hit.getType() != HitResult.Type.MISS || tickCount >= MAX_LIFETIME) {
            neutralizeInfestedBlocks();
            discard();
        }
    }

    private void neutralizeInfestedBlocks() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos center = blockPosition();
        for (BlockPos mutable : BlockPos.betweenClosed(
                center.offset(-HORIZONTAL_RANGE, -VERTICAL_RANGE, -HORIZONTAL_RANGE),
                center.offset(HORIZONTAL_RANGE, VERTICAL_RANGE, HORIZONTAL_RANGE))) {
            BlockPos pos = mutable.immutable();
            BlockState state = serverLevel.getBlockState(pos);
            if (state.is(ModBlocks.INFESTED_STAIN.get()) || state.is(ModBlocks.INFESTED_RUBBLE.get())) {
                serverLevel.setBlockAndUpdate(pos, state.setValue(InfestedBlock.STAGE, 3));
                serverLevel.scheduleTick(pos, state.getBlock(), 40);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
