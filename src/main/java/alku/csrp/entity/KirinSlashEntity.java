package alku.csrp.entity;

import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 麒麟闪烁斩击（原版 EntityProjectileKirinSlash）：从起点沿固定方向展开的线状斩击，
 * 有延迟/生长/淡出三段表现，命中行进路线上的非寄生生物。渲染为程序化光刃，无贴图。
 */
public class KirinSlashEntity extends Entity {
    private static final EntityDataAccessor<Float> SYNC_YAW = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNC_PITCH = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNC_LENGTH = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SYNC_DELAY = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SYNC_GROW_TICKS = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SYNC_LIFE = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SYNC_FADING = SynchedEntityData.defineId(
            KirinSlashEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int FADE_TICKS = 10;

    private LivingEntity owner;
    private UUID ownerUuid;
    private float damage = 12.0F;
    private int age;
    private int fadeAge;
    private final Set<Integer> hitEntities = new HashSet<>();

    public KirinSlashEntity(EntityType<? extends KirinSlashEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static KirinSlashEntity create(ServerLevel level, LivingEntity owner, Vec3 start,
            float yaw, float pitch, float length, float damage, int delayTicks, int growTicks, int life) {
        KirinSlashEntity slash = alku.csrp.registry.ModEntities.KIRIN_SLASH.get().create(level);
        if (slash == null) {
            return null;
        }
        slash.owner = owner;
        slash.ownerUuid = owner.getUUID();
        slash.damage = damage;
        slash.setPos(start.x, start.y, start.z);
        slash.entityData.set(SYNC_YAW, yaw);
        slash.entityData.set(SYNC_PITCH, pitch);
        slash.entityData.set(SYNC_LENGTH, length);
        slash.entityData.set(SYNC_DELAY, Math.max(0, delayTicks));
        slash.entityData.set(SYNC_GROW_TICKS, Math.max(1, growTicks));
        slash.entityData.set(SYNC_LIFE, Math.max(10, life));
        slash.entityData.set(SYNC_FADING, false);
        slash.playSound(ModSounds.KIRIN_PROJECTILE_SUMMON.get(), 0.75F, 1.0F);
        return slash;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SYNC_YAW, 0.0F);
        builder.define(SYNC_PITCH, 0.0F);
        builder.define(SYNC_LENGTH, 80.0F);
        builder.define(SYNC_DELAY, 0);
        builder.define(SYNC_GROW_TICKS, 5);
        builder.define(SYNC_LIFE, 60);
        builder.define(SYNC_FADING, false);
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (isFading()) {
            fadeAge++;
            if (fadeAge >= FADE_TICKS) {
                discard();
            }
            return;
        }

        if (level().isClientSide) {
            return;
        }

        if (owner == null && ownerUuid != null && level() instanceof ServerLevel serverLevel) {
            owner = serverLevel.getEntity(ownerUuid) instanceof LivingEntity living ? living : null;
        }
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        if (getVisibleAge() > getLife()) {
            entityData.set(SYNC_FADING, true);
            return;
        }
        if (getVisibleAge() >= 0) {
            checkEntityImpact();
        }
    }

    private void checkEntityImpact() {
        float growth = getGrowth(0.0F);
        if (growth <= 0.0F) {
            return;
        }
        float currentLength = Math.max(1.0F, getLength() * growth);
        AABB searchBox = getBoundingBox().inflate(currentLength,
                Math.max(4.0D, currentLength * 0.25D), currentLength);
        List<LivingEntity> victims = level().getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity.isAlive() && entity != owner && !(entity instanceof Parasite));

        Vec3 start = position();
        Vec3 end = start.add(getSlashDirection().scale(currentLength));
        DamageSource source = owner != null
                ? damageSources().mobAttack(owner)
                : damageSources().magic();
        for (LivingEntity victim : victims) {
            if (hitEntities.contains(victim.getId())) {
                continue;
            }
            if (victim.getBoundingBox().clip(start, end).isEmpty()
                    && !victim.getBoundingBox().inflate(0.5D).contains(start)) {
                continue;
            }
            hitEntities.add(victim.getId());
            victim.hurt(source, damage);
            victim.playSound(ModSounds.KIRIN_PROJECTILE_IMPACT.get(), 0.9F,
                    0.9F + random.nextFloat() * 0.2F);
            victim.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.1F);
        }
    }

    public Vec3 getSlashDirection() {
        float yawRad = getYaw() * ((float) Math.PI / 180.0F);
        float pitchRad = getPitch() * ((float) Math.PI / 180.0F);
        float cosPitch = Mth.cos(pitchRad);
        return new Vec3(-Mth.sin(yawRad) * cosPitch, -Mth.sin(pitchRad), Mth.cos(yawRad) * cosPitch);
    }

    public float getGrowth(float partialTicks) {
        float visibleAge = age + partialTicks - getDelayTicks();
        if (visibleAge <= 0.0F) {
            return 0.0F;
        }
        return Math.min(1.0F, visibleAge / Math.max(1, getGrowTicks()));
    }

    private int getVisibleAge() {
        return age - getDelayTicks();
    }

    public float getYaw() {
        return entityData.get(SYNC_YAW);
    }

    public float getPitch() {
        return entityData.get(SYNC_PITCH);
    }

    public float getLength() {
        return entityData.get(SYNC_LENGTH);
    }

    public int getDelayTicks() {
        return entityData.get(SYNC_DELAY);
    }

    public int getGrowTicks() {
        return entityData.get(SYNC_GROW_TICKS);
    }

    public int getLife() {
        return entityData.get(SYNC_LIFE);
    }

    public boolean isFading() {
        return entityData.get(SYNC_FADING);
    }

    /** 渲染用透明度：淡出期随剩余时间衰减。 */
    public float getRenderAlpha(float partialTicks) {
        if (!isFading()) {
            return 1.0F;
        }
        return Math.max(0.0F, 1.0F - (fadeAge + partialTicks) / FADE_TICKS);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        damage = tag.contains("Damage") ? tag.getFloat("Damage") : 12.0F;
        age = tag.getInt("Age");
        fadeAge = tag.getInt("FadeAge");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putFloat("Damage", damage);
        tag.putInt("Age", age);
        tag.putInt("FadeAge", fadeAge);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
