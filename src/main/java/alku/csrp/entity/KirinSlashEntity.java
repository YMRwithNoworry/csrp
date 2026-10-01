package alku.csrp.entity;

import alku.csrp.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Direct port of SRParasites 1.10.9's {@code EntityProjectileKirinSlash}.
 *
 * <p>The slice stays anchored where it was created, grows along its local +Z axis over
 * {@code growTicks} with a {@code 1-(1-t)^2} curve, lives for {@code life} ticks, hits every
 * victim at most once, and swaps to a short "hit pop" animation instead of disappearing. Players
 * take an exact 2.0 health cut that ignores armour and i-frames, every other living entity takes
 * the configured damage.</p>
 */
public final class KirinSlashEntity extends Entity {
    private static final EntityDataAccessor<Float> SYNC_YAW = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNC_PITCH = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNC_ROLL = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNC_LENGTH = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SYNC_DELAY = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SYNC_GROW_TICKS = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SYNC_LIFE = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SYNC_HIT_POP = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SYNC_HIT_POP_AGE = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.INT);

    private static final float DEFAULT_LENGTH = 80.0F;
    private static final float DEFAULT_DAMAGE = 12.0F;
    private static final int DEFAULT_GROW_TICKS = 5;
    private static final int DEFAULT_LIFE = 60;
    private static final float PLAYER_EXACT_DAMAGE = 2.0F;
    private static final int HIT_POP_TICKS = 6;

    private LivingEntity owner;
    private int age;
    private int delayTicks;
    private int growTicks = DEFAULT_GROW_TICKS;
    private int life = DEFAULT_LIFE;
    private float damage = DEFAULT_DAMAGE;
    private float maxLength = DEFAULT_LENGTH;
    private double startX;
    private double startY;
    private double startZ;
    private boolean hitPop;
    private int hitPopAge;
    private final Set<Integer> hitEntities = new HashSet<>();

    public KirinSlashEntity(EntityType<? extends KirinSlashEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SYNC_YAW, 0.0F);
        entityData.define(SYNC_PITCH, 0.0F);
        entityData.define(SYNC_ROLL, 0.0F);
        entityData.define(SYNC_LENGTH, DEFAULT_LENGTH);
        entityData.define(SYNC_DELAY, 0);
        entityData.define(SYNC_GROW_TICKS, DEFAULT_GROW_TICKS);
        entityData.define(SYNC_LIFE, DEFAULT_LIFE);
        entityData.define(SYNC_HIT_POP, false);
        entityData.define(SYNC_HIT_POP_AGE, 0);
    }

    /** Original 13-argument constructor used by the kirin's pending slash queue. */
    public void configure(LivingEntity owner, Vec3 start, float yaw, float pitch, float roll,
            float length, float damage, int delayTicks, int growTicks, int life) {
        this.owner = owner;
        this.damage = damage;
        this.maxLength = Math.max(1.0F, length);
        this.delayTicks = Math.max(0, delayTicks);
        this.growTicks = Math.max(1, growTicks);
        this.life = Math.max(10, life);
        this.startX = start.x;
        this.startY = start.y;
        this.startZ = start.z;
        setPos(start.x, start.y, start.z);
        entityData.set(SYNC_YAW, yaw);
        entityData.set(SYNC_PITCH, pitch);
        entityData.set(SYNC_ROLL, roll);
        entityData.set(SYNC_LENGTH, this.maxLength);
        entityData.set(SYNC_DELAY, this.delayTicks);
        entityData.set(SYNC_GROW_TICKS, this.growTicks);
        entityData.set(SYNC_LIFE, this.life);
    }

    public float getSlashYaw() {
        return entityData.get(SYNC_YAW);
    }

    public float getSlashPitch() {
        return entityData.get(SYNC_PITCH);
    }

    public float getSlashRoll() {
        return entityData.get(SYNC_ROLL);
    }

    public float getSlashLength() {
        return entityData.get(SYNC_LENGTH);
    }

    public int getDelayTicks() {
        return entityData.get(SYNC_DELAY);
    }

    public int getGrowTicks() {
        return Math.max(1, entityData.get(SYNC_GROW_TICKS));
    }

    public int getLife() {
        return Math.max(1, entityData.get(SYNC_LIFE));
    }

    public boolean isHitPopping() {
        return entityData.get(SYNC_HIT_POP);
    }

    public int getHitPopAge() {
        return entityData.get(SYNC_HIT_POP_AGE);
    }

    public int getHitPopTicks() {
        return HIT_POP_TICKS;
    }

    /** Original {@code getGrowth}: {@code 1-(1-t)^2} over the grow window. */
    public float getGrowth(float partialTick) {
        float visibleAge = tickCount + partialTick - getDelayTicks();
        if (visibleAge <= 0.0F) {
            return 0.0F;
        }
        float raw = Mth.clamp(visibleAge / getGrowTicks(), 0.0F, 1.0F);
        return 1.0F - (1.0F - raw) * (1.0F - raw);
    }

    private int getVisibleAge() {
        return age - delayTicks;
    }

    private float getServerGrowth() {
        int visibleAge = getVisibleAge();
        if (visibleAge <= 0) {
            return 0.0F;
        }
        float raw = Mth.clamp(visibleAge / (float) growTicks, 0.0F, 1.0F);
        return 1.0F - (1.0F - raw) * (1.0F - raw);
    }

    @Override
    public void tick() {
        xo = getX();
        yo = getY();
        zo = getZ();
        age++;
        setPos(startX, startY, startZ);

        if (hitPop) {
            hitPopAge++;
            if (!level().isClientSide) {
                entityData.set(SYNC_HIT_POP_AGE, hitPopAge);
            }
            if (hitPopAge >= HIT_POP_TICKS) {
                discard();
            }
            return;
        }

        if (level().isClientSide) {
            return;
        }
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (getVisibleAge() > life) {
            discard();
            return;
        }
        if (getVisibleAge() >= 0) {
            checkEntityImpact();
        }
    }

    /** Original {@code checkEntityImpact}: one victim per tick, each victim only once in total. */
    private void checkEntityImpact() {
        float growth = getServerGrowth();
        if (growth <= 0.0F) {
            return;
        }
        float currentLength = Math.max(1.0F, maxLength * growth);
        AABB searchBox = getBoundingBox().inflate(currentLength,
                Math.max(4.0D, currentLength * 0.25D), currentLength);
        List<LivingEntity> candidates = level().getEntitiesOfClass(LivingEntity.class, searchBox);
        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 end = start.add(getSlashDirection().scale(currentLength));

        for (LivingEntity living : candidates) {
            if (living == null || !living.isAlive() || living == owner
                    || hitEntities.contains(living.getId()) || living instanceof Parasite) {
                continue;
            }
            if (living instanceof Player player && player.isSpectator()) {
                continue;
            }
            AABB hitBox = living.getBoundingBox().inflate(0.35D, 0.25D, 0.35D);
            if (hitBox.clip(start, end).isPresent() || hitBox.contains(start) || hitBox.contains(end)) {
                onSliceTouched(living);
                return;
            }
        }
    }

    /** Original {@code onSliceTouched}. */
    private void onSliceTouched(LivingEntity living) {
        if (living == null || !living.isAlive() || hitEntities.contains(living.getId())) {
            return;
        }
        if (living instanceof Player player && player.isSpectator()) {
            return;
        }
        hitEntities.add(living.getId());
        spawnContactSmoke(living);
        level().playSound(null, living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                ModSounds.KIRIN_PROJECTILE_IMPACT.get(), SoundSource.HOSTILE, 0.45F,
                0.92F + random.nextFloat() * 0.16F);

        if (living instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                forceExactHealthDamage(player, PLAYER_EXACT_DAMAGE);
            }
        } else {
            living.hurt(damageSources().mobAttack(owner), damage);
        }
        beginHitPop();
    }

    /** Original {@code forceExactHealthDamage}: bypasses armour and i-frames entirely. */
    private void forceExactHealthDamage(LivingEntity living, float amount) {
        if (living == null || !living.isAlive() || amount <= 0.0F) {
            return;
        }
        float newHealth = Math.max(0.0F, living.getHealth() - amount);
        living.setHealth(newHealth);
        if (newHealth <= 0.0F) {
            living.hurt(damageSources().mobAttack(owner), Float.MAX_VALUE);
        }
    }

    /** Original {@code spawnContactSmoke}. */
    private void spawnContactSmoke(LivingEntity living) {
        if (level() instanceof ServerLevel serverLevel) {
            double x = living.getX();
            double y = living.getBoundingBox().minY + 0.05D;
            double z = living.getZ();
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 4, 0.25D, 0.05D, 0.25D, 0.012D);
            serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 2, 0.18D, 0.03D, 0.18D, 0.006D);
        }
    }

    /** Original {@code beginHitPop}. */
    private void beginHitPop() {
        if (!hitPop) {
            hitPop = true;
            hitPopAge = 0;
            if (!level().isClientSide) {
                entityData.set(SYNC_HIT_POP, true);
                entityData.set(SYNC_HIT_POP_AGE, 0);
            }
        }
    }

    private Vec3 getSlashDirection() {
        float yawRad = getSlashYaw() * Mth.DEG_TO_RAD;
        float pitchRad = getSlashPitch() * Mth.DEG_TO_RAD;
        Vec3 direction = new Vec3(Mth.sin(yawRad) * Mth.cos(pitchRad),
                -Mth.sin(pitchRad), Mth.cos(yawRad) * Mth.cos(pitchRad));
        return direction.length() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
        life = tag.getInt("Life");
        damage = tag.getFloat("Damage");
        maxLength = tag.getFloat("SlashLength");
        delayTicks = tag.getInt("Delay");
        growTicks = Math.max(1, tag.getInt("Grow"));
        startX = tag.getDouble("StartX");
        startY = tag.getDouble("StartY");
        startZ = tag.getDouble("StartZ");
        hitPop = tag.getBoolean("HitPop");
        hitPopAge = tag.getInt("HitPopAge");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putInt("Life", life);
        tag.putFloat("Damage", damage);
        tag.putFloat("SlashLength", maxLength);
        tag.putInt("Delay", delayTicks);
        tag.putInt("Grow", growTicks);
        tag.putDouble("StartX", startX);
        tag.putDouble("StartY", startY);
        tag.putDouble("StartZ", startZ);
        tag.putBoolean("HitPop", hitPop);
        tag.putInt("HitPopAge", hitPopAge);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(0.25F, 0.25F);
    }
}
