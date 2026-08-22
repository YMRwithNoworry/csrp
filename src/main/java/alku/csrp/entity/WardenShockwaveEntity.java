package alku.csrp.entity;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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

/** Ground wave emitted by the Warden's original Ganro shockwave skill. */
public final class WardenShockwaveEntity extends Entity {
    private static final String OWNER_TAG = "Owner";
    private static final String TARGET_X_TAG = "TargetX";
    private static final String TARGET_Y_TAG = "TargetY";
    private static final String TARGET_Z_TAG = "TargetZ";
    private static final double MOVEMENT_SPEED = 0.6D;
    private static final int DURATION_TICKS = 20 * 60;

    private UUID ownerUuid;
    private double targetX;
    private double targetY;
    private double targetZ;

    public WardenShockwaveEntity(EntityType<? extends WardenShockwaveEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void configure(PureParasiteEntity owner, LivingEntity target) {
        ownerUuid = owner.getUUID();
        targetX = target.getX();
        targetY = owner.getY();
        targetZ = target.getZ();
        updateMovement();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnGroundDebris();
            return;
        }

        PureParasiteEntity owner = resolveOwner();
        if (owner == null || !owner.isAlive() || owner.getKind() != PureParasiteEntity.Kind.WARDEN
                || targetX == 0.0D || !level().getFluidState(blockPosition()).isEmpty()
                || tickCount > DURATION_TICKS) {
            discard();
            return;
        }
        if (tickCount > 20 && (getX() == xo || getZ() == zo)) {
            discard();
            return;
        }

        breakContactBlocks(owner);
        damageTargets(owner);
        if (distanceToSqr(targetX, targetY, targetZ) <= 2.0D) {
            discard();
            return;
        }
        updateMovement();
        Vec3 movement = getDeltaMovement();
        AABB nextBounds = getBoundingBox().move(movement);
        if (!level().noCollision(this, nextBounds)) {
            discard();
            return;
        }
        move(MoverType.SELF, movement);
    }

    private PureParasiteEntity resolveOwner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity owner = serverLevel.getEntity(ownerUuid);
        return owner instanceof PureParasiteEntity pure ? pure : null;
    }

    private void updateMovement() {
        Vec3 direction = new Vec3(targetX - getX(), 0.0D, targetZ - getZ());
        if (direction.horizontalDistanceSqr() <= 0.001D) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        setDeltaMovement(direction.normalize().scale(MOVEMENT_SPEED));
        hasImpulse = true;
    }

    private void damageTargets(PureParasiteEntity owner) {
        AABB area = getBoundingBox().inflate(1.5D, 0.2D, 1.5D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), area);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                target -> target != owner && target.isAlive() && !(target instanceof Parasite))) {
            if (owner.hurtWardenSkillTarget(target)) {
                Vec3 movement = target.getDeltaMovement();
                target.setDeltaMovement(movement.x, movement.y + 0.64645D, movement.z);
                target.hurtMarked = true;
            }
        }
    }

    private void breakContactBlocks(PureParasiteEntity owner) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        BlockPos center = blockPosition();
        float maximumHardness = owner.adjustBlockBreakHardness(5.0F);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 3; y++) {
                    BlockPos candidate = center.offset(x, y, z);
                    BlockState state = level().getBlockState(candidate);
                    float hardness = state.getDestroySpeed(level(), candidate);
                    if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                            || hardness < 0.0F || hardness > maximumHardness) {
                        continue;
                    }
                    ParasiteBlockInventory.collect(serverLevel, candidate, owner);
                }
            }
        }
    }

    private void spawnGroundDebris() {
        BlockState ground = level().getBlockState(blockPosition().below());
        if (ground.isAir()) {
            return;
        }
        for (int index = 0; index < 35; index++) {
            level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    getX() + random.nextDouble() * getBbWidth() * 2.4D - getBbWidth() * 1.2D,
                    getY(),
                    getZ() + random.nextDouble() * getBbWidth() * 2.4D - getBbWidth() * 1.2D,
                    random.nextGaussian() * 0.02D,
                    random.nextGaussian() + 140.0D,
                    random.nextGaussian() * 0.02D);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID(OWNER_TAG)) {
            ownerUuid = tag.getUUID(OWNER_TAG);
        }
        targetX = tag.getDouble(TARGET_X_TAG);
        targetY = tag.getDouble(TARGET_Y_TAG);
        targetZ = tag.getDouble(TARGET_Z_TAG);
        updateMovement();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID(OWNER_TAG, ownerUuid);
        }
        tag.putDouble(TARGET_X_TAG, targetX);
        tag.putDouble(TARGET_Y_TAG, targetY);
        tag.putDouble(TARGET_Z_TAG, targetZ);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
