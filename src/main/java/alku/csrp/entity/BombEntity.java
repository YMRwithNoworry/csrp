package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** Original SRP timed bomb used by Hosts, Iki, Omboo, Jinjo and stationary architects. */
public final class BombEntity extends Entity {
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(
            BombEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> SKIN = SynchedEntityData.defineId(
            BombEntity.class, EntityDataSerializers.BYTE);

    private UUID ownerId;
    private int fuseTicks = 80;
    private float strength = 4.0F;
    private float damage;
    private boolean canGrief;
    private int rangeRadius;

    public BombEntity(EntityType<? extends BombEntity> type, Level level) {
        super(type, level);
    }

    public void configure(PrimitiveParasiteEntity owner, int fuse, float strength, float damage,
                          int rangeRadius, int skin, boolean canGrief) {
        ownerId = owner == null ? null : owner.getUUID();
        setFuse(fuse);
        this.strength = strength;
        this.damage = damage;
        this.rangeRadius = Math.max(0, rangeRadius);
        this.canGrief = canGrief;
        setSkin(skin);
        if (owner != null) {
            moveTo(owner.getX(), owner.getY() + owner.getEyeHeight() - 0.1D, owner.getZ(),
                    owner.getYRot(), owner.getXRot());
        }
    }

    public void shoot(Vec3 direction, float velocity, float inaccuracy) {
        if (direction.lengthSqr() < 1.0E-7D) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 normalized = direction.normalize().add(
                random.nextGaussian() * 0.0075D * inaccuracy,
                random.nextGaussian() * 0.0075D * inaccuracy,
                random.nextGaussian() * 0.0075D * inaccuracy).scale(velocity);
        setDeltaMovement(normalized);
        double horizontal = normalized.horizontalDistance();
        setYRot((float) (Mth.atan2(normalized.x, normalized.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(normalized.y, horizontal) * Mth.RAD_TO_DEG));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FUSE, 80);
        entityData.define(SKIN, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 movement = getDeltaMovement();
        if (!isNoGravity()) {
            movement = movement.add(0.0D, -0.04D, 0.0D);
        }
        move(MoverType.SELF, movement);
        movement = movement.scale(0.98D);
        if (onGround()) {
            movement = new Vec3(movement.x * 0.7D, movement.y * -0.5D, movement.z * 0.7D);
        }
        setDeltaMovement(movement);
        collideWithNearbyEntities();

        if (!level().isClientSide) {
            setFuse(fuseTicks - 1);
            if (fuseTicks <= 0) {
                explode();
            }
        }
    }

    private void explode() {
        PrimitiveParasiteEntity owner = owner();
        if (strength > 0.0F) {
            boolean grief = canGrief && level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            level().explode(owner == null ? this : owner, getX(), getY(), getZ(), strength,
                    grief ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        }

        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS, 0.5F,
                (1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F) * 0.7F);
        if (owner != null) {
            AABB area = new AABB(getX(), getY(), getZ(), getX() + 1.0D, getY() + 1.0D, getZ() + 1.0D)
                    .inflate(rangeRadius);
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                    owner::isValidParasiteTarget)) {
                if (!target.hasLineOfSight(this)) {
                    continue;
                }
                target.hurt(damageSources().mobAttack(owner), damage);
                target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL.get(), 300, 0), owner);
                if (owner.isAlive()) {
                    owner.applyPrimitiveMinimumDamage(target, 3.0F);
                }
            }
        }

        if (getSkin() == 2) {
            spawnJinjoPayload(owner);
        }
        spawnToxicCloud(owner);
        discard();
    }

    private void spawnToxicCloud(PrimitiveParasiteEntity owner) {
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
        if (owner != null) {
            cloud.setOwner(owner);
        }
        cloud.setRadius(rangeRadius);
        cloud.setWaitTime(5);
        cloud.setDuration(60);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 0));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH.get(), 3600, 0, false, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL.get(), 3600, 0, false, false));
        level().addFreshEntity(cloud);
    }

    private void spawnJinjoPayload(PrimitiveParasiteEntity owner) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        List<? extends String> entries = MobsConfig.jinjoMobs();
        if (entries.isEmpty()) {
            return;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(entries.get(random.nextInt(entries.size())));
        if (parsed == null) {
            return;
        }
        if (parsed.getNamespace().equals("srparasites")) {
            parsed = new ResourceLocation("csrp", parsed.getPath());
        }
        EntityType<?> payloadType = BuiltInRegistries.ENTITY_TYPE.getOptional(parsed).orElse(null);
        Entity created = payloadType == null ? null : payloadType.create(serverLevel);
        if (!(created instanceof Mob payload)) {
            return;
        }
        payload.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        payload.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(payload.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null, null);
        if (owner != null) {
            payload.setTarget(owner.getTarget());
        }
        serverLevel.addFreshEntity(payload);
    }

    private PrimitiveParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof PrimitiveParasiteEntity parasite ? parasite : null;
    }

    private void collideWithNearbyEntities() {
        for (Entity entity : level().getEntities(this, getBoundingBox(), Entity::isPushable)) {
            entity.push(this);
        }
    }

    public void setFuse(int fuse) {
        fuseTicks = Math.max(0, fuse);
        entityData.set(FUSE, fuseTicks);
    }

    public int getFuse() {
        return level().isClientSide ? entityData.get(FUSE) : fuseTicks;
    }

    public void setSkin(int skin) {
        entityData.set(SKIN, (byte) Mth.clamp(skin, 0, 3));
    }

    public int getSkin() {
        return entityData.get(SKIN);
    }

    public float getStrength() {
        return strength;
    }

    public float getDamage() {
        return damage;
    }

    public int getRangeRadius() {
        return rangeRadius;
    }

    public boolean canGrief() {
        return canGrief;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        setFuse(tag.contains("Fuse") ? tag.getShort("Fuse") : 80);
        setSkin(tag.getInt("parasitetype"));
        strength = tag.contains("stren") ? tag.getFloat("stren") : 4.0F;
        damage = tag.getFloat("damage");
        rangeRadius = Math.max(0, tag.getInt("range_radius"));
        canGrief = tag.getBoolean("cangrief");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        tag.putShort("Fuse", (short) fuseTicks);
        tag.putInt("parasitetype", getSkin());
        tag.putFloat("stren", strength);
        tag.putFloat("damage", damage);
        tag.putInt("range_radius", rangeRadius);
        tag.putBoolean("cangrief", canGrief);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return true;
    }
}
