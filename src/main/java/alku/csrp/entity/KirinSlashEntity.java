package alku.csrp.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import alku.csrp.registry.ModSounds;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A delayed, growing judgement cut emitted by Kirin. */
public final class KirinSlashEntity extends Entity {
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(KirinSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GROW = SynchedEntityData.defineId(KirinSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(KirinSlashEntity.class, EntityDataSerializers.INT);

    private UUID ownerId;
    private float damage = 10.0F;
    private int age;
    private final Set<Integer> hitEntities = new HashSet<>();

    public KirinSlashEntity(EntityType<? extends KirinSlashEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(PrimitiveParasiteEntity owner, Vec3 start, float yaw, float pitch, float roll,
                          float length, float damage, int delay, int grow, int life) {
        ownerId = owner == null ? null : owner.getUUID();
        this.damage = damage;
        setPos(start);
        entityData.set(YAW, yaw);
        entityData.set(PITCH, pitch);
        entityData.set(ROLL, roll);
        entityData.set(LENGTH, Math.max(1.0F, length));
        entityData.set(DELAY, Math.max(0, delay));
        entityData.set(GROW, Math.max(1, grow));
        entityData.set(LIFE, Math.max(10, life));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(YAW, 0.0F);
        entityData.define(PITCH, 0.0F);
        entityData.define(ROLL, 0.0F);
        entityData.define(LENGTH, 80.0F);
        entityData.define(DELAY, 0);
        entityData.define(GROW, 5);
        entityData.define(LIFE, 60);
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (level().isClientSide) return;
        PrimitiveParasiteEntity owner = owner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        int visibleAge = age - entityData.get(DELAY);
        if (visibleAge < 0) return;
        if (visibleAge > entityData.get(LIFE)) {
            discard();
            return;
        }
        float raw = Math.min(1.0F, visibleAge / (float) entityData.get(GROW));
        float length = Math.max(1.0F, entityData.get(LENGTH) * (1.0F - (1.0F - raw) * (1.0F - raw)));
        Vec3 direction = direction();
        Vec3 end = position().add(direction.scale(length));
        AABB search = getBoundingBox().inflate(length, Math.max(4.0D, length * 0.25D), length);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, search)) {
            if (target == owner || !target.isAlive() || hitEntities.contains(target.getId())
                    || target instanceof PrimitiveParasiteEntity || target instanceof Player player && player.isSpectator()) {
                continue;
            }
            AABB box = target.getBoundingBox().inflate(0.35D, 0.25D, 0.35D);
            if (box.clip(position(), end).isPresent() || box.contains(position()) || box.contains(end)) {
                hitEntities.add(target.getId());
                target.hurt(damageSources().mobAttack(owner), target instanceof Player ? 2.0F : damage);
                level().playSound(null, target.blockPosition(), ModSounds.KIRIN_LIVING.get(),
                        SoundSource.HOSTILE, 0.45F, 0.92F + random.nextFloat() * 0.16F);
                discard();
                return;
            }
        }
    }

    private PrimitiveParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel server)) return null;
        Entity entity = server.getEntity(ownerId);
        return entity instanceof PrimitiveParasiteEntity parasite ? parasite : null;
    }

    private Vec3 direction() {
        double yaw = Math.toRadians(entityData.get(YAW));
        double pitch = Math.toRadians(entityData.get(PITCH));
        Vec3 direction = new Vec3(Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch));
        return direction.lengthSqr() < 1.0E-4D ? Vec3.ZERO : direction.normalize();
    }

    public float getSlashYaw() { return entityData.get(YAW); }
    public float getSlashPitch() { return entityData.get(PITCH); }
    public float getSlashRoll() { return entityData.get(ROLL); }
    public float getSlashLength() { return entityData.get(LENGTH); }
    public float getGrowth(float partialTick) {
        float visible = tickCount + partialTick - entityData.get(DELAY);
        if (visible <= 0) return 0.0F;
        float raw = Math.min(1.0F, visible / entityData.get(GROW));
        return 1.0F - (1.0F - raw) * (1.0F - raw);
    }
    public int getLife() { return entityData.get(LIFE); }

    @Override public EntityDimensions getDimensions(Pose pose) { return EntityDimensions.fixed(0.25F, 0.25F); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) ownerId = tag.getUUID("owner");
        age = tag.getInt("age");
        damage = tag.getFloat("damage");
        entityData.set(YAW, tag.getFloat("yaw"));
        entityData.set(PITCH, tag.getFloat("pitch"));
        entityData.set(ROLL, tag.getFloat("roll"));
        entityData.set(LENGTH, Math.max(1.0F, tag.getFloat("length")));
        entityData.set(DELAY, Math.max(0, tag.getInt("delay")));
        entityData.set(GROW, Math.max(1, tag.getInt("grow")));
        entityData.set(LIFE, Math.max(10, tag.getInt("life")));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("owner", ownerId);
        tag.putInt("age", age);
        tag.putFloat("damage", damage);
        tag.putFloat("yaw", getSlashYaw());
        tag.putFloat("pitch", getSlashPitch());
        tag.putFloat("roll", getSlashRoll());
        tag.putFloat("length", getSlashLength());
        tag.putInt("delay", entityData.get(DELAY));
        tag.putInt("grow", entityData.get(GROW));
        tag.putInt("life", entityData.get(LIFE));
    }
    @Override public boolean isPickable() { return false; }
}
