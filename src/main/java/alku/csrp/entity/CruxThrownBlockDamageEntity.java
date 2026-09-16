package alku.csrp.entity;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Server-side hitbox that follows a Crux-thrown falling block. */
public final class CruxThrownBlockDamageEntity extends Entity {
    private static final int MAX_LIFETIME_TICKS = 80;
    private UUID ownerId;
    private UUID followerId;
    private float damage;

    public CruxThrownBlockDamageEntity(EntityType<? extends CruxThrownBlockDamageEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(CruxEntity owner, FallingBlockEntity follower, float damage) {
        ownerId = owner.getUUID();
        followerId = follower.getUUID();
        this.damage = damage;
        setPos(follower.position());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }

        CruxEntity owner = owner();
        FallingBlockEntity follower = follower();
        if (owner == null || !owner.isAlive() || follower == null || follower.isRemoved()
                || tickCount > MAX_LIFETIME_TICKS) {
            discard();
            return;
        }

        setPos(follower.position());
        AABB impactArea = follower.getBoundingBox().inflate(0.75, 0.5, 0.75);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), impactArea);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, impactArea,
                owner::isValidParasiteTarget)) {
            if (hasClearPath(target)) {
                target.hurt(damageSources().mobAttack(owner), damage);
                discard();
                return;
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!this.isInvulnerableToBase(source)) {
            this.markHurt();
        }
        return false;
    }

    private boolean hasClearPath(LivingEntity target) {
        Vec3 source = position().add(0.0, 0.5, 0.0);
        return level().clip(new ClipContext(source, target.getEyePosition(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private CruxEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof CruxEntity crux ? crux : null;
    }

    private FallingBlockEntity follower() {
        if (followerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(followerId);
        return entity instanceof FallingBlockEntity block ? block : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        ownerId = tag.read("owner", UUIDUtil.CODEC).orElse(null);
        followerId = tag.read("follower", UUIDUtil.CODEC).orElse(null);
        damage = tag.getFloatOr("damage", 0.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        if (ownerId != null) {
            tag.store("owner", UUIDUtil.CODEC, ownerId);
        }
        if (followerId != null) {
            tag.store("follower", UUIDUtil.CODEC, followerId);
        }
        tag.putFloat("damage", damage);
    }
}
