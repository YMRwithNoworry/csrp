package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ParasiteProjectileEntity extends Entity {
    private static final int ELVIA_NADE_START_DELAY_TICKS = 3;
    private static final int ELVIA_NADE_FUSE_TICKS = 4;
    private static final int ELVIA_NADE_DURATION_TICKS = 60;

    public enum Mode {
        BOMB,
        SPINE,
        METEOR,
        LIGHT,
        ACID,
        VOMIT,
        NEEDLE,
        WITHER,
        LENCIA_BALL,
        ELVIA_BALL,
        ELVIA_NADE
    }

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HOMING_TARGET = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NADE_ARMED = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> NADE_FUSE_PROGRESS = SynchedEntityData.defineId(
            ParasiteProjectileEntity.class, EntityDataSerializers.INT);

    private UUID ownerId;
    private float damage = 4.0F;
    private double radius = 1.0;
    private int maximumLifetime = 80;
    private Vec3 acceleration = Vec3.ZERO;
    private boolean accelerating;
    private int nadeIgnitionTicks;
    private int nadeFuseTicks;
    private int nadeDamageTicks;

    public ParasiteProjectileEntity(EntityType<? extends ParasiteProjectileEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(PrimitiveParasiteEntity owner, Mode mode, Vec3 start, Vec3 target,
                          double speed, float damage, double radius, int maximumLifetime) {
        configure(owner, mode, start, target, speed, damage, radius, maximumLifetime, null);
    }

    public void configure(PrimitiveParasiteEntity owner, Mode mode, Vec3 start, Vec3 target,
                          double speed, float damage, double radius, int maximumLifetime,
                          LivingEntity homingTarget) {
        ownerId = owner.getUUID();
        entityData.set(MODE, mode.ordinal());
        entityData.set(HOMING_TARGET, homingTarget == null ? 0 : homingTarget.getId());
        this.damage = damage;
        this.radius = radius;
        this.maximumLifetime = maximumLifetime;
        setPos(start);
        Vec3 direction = target.subtract(start);
        if (direction.lengthSqr() > 0.001) {
            setDeltaMovement(direction.normalize().scale(speed));
        }
    }

    public void configureAccelerating(PrimitiveParasiteEntity owner, Mode mode, Vec3 start, Vec3 accelerationDirection,
                                      float damage, double radius) {
        ownerId = owner.getUUID();
        entityData.set(MODE, mode.ordinal());
        entityData.set(HOMING_TARGET, 0);
        entityData.set(NADE_ARMED, false);
        entityData.set(NADE_FUSE_PROGRESS, 0);
        this.damage = damage;
        this.radius = radius;
        maximumLifetime = Integer.MAX_VALUE;
        setPos(start);
        acceleration = accelerationDirection.lengthSqr() > 0.001D
                ? accelerationDirection.normalize().scale(0.1D) : Vec3.ZERO;
        accelerating = true;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        super.tick();
        Mode mode = getMode();
        PrimitiveParasiteEntity owner = owner();
        boolean armedNade = mode == Mode.ELVIA_NADE && entityData.get(NADE_ARMED);
        if (!level().isClientSide && (owner == null || !owner.isAlive()) && !armedNade) {
            discard();
            return;
        }
        if (armedNade) {
            tickElviaNade(owner);
            return;
        }

        Vec3 start = position();
        Vec3 movement = steerTowardsHomingTarget(owner, getDeltaMovement(), mode);
        Vec3 end = start.add(movement);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        setPos(end.x, end.y, end.z);

        if (level().isClientSide) {
            ParticleOptions particle = switch (mode) {
                case BOMB, METEOR -> ParticleTypes.FLAME;
                case LIGHT, WITHER -> ParticleTypes.SOUL_FIRE_FLAME;
                case SPINE, NEEDLE -> ParticleTypes.CRIT;
                case ACID, VOMIT -> ParticleTypes.WITCH;
                case LENCIA_BALL, ELVIA_BALL -> ParticleTypes.EXPLOSION;
                case ELVIA_NADE -> ParticleTypes.ITEM_SLIME;
            };
            level().addParticle(particle, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            return;
        }

        if (mode == Mode.BOMB || mode == Mode.METEOR || mode == Mode.ACID || mode == Mode.VOMIT) {
            setDeltaMovement(movement.add(0.0, -0.025, 0.0));
        } else if (accelerating) {
            setDeltaMovement(movement.add(acceleration).scale(0.95D));
        }

        LivingEntity hit = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.65),
                        target -> canCollideWith(owner, mode, target))
                .stream().findFirst().orElse(null);
        if (blockHit.getType() != HitResult.Type.MISS || hit != null || tickCount >= maximumLifetime) {
            impact(owner, mode, hit);
        }
    }

    private boolean canCollideWith(PrimitiveParasiteEntity owner, Mode mode, LivingEntity target) {
        if (owner == null || target == owner || !target.isAlive()) {
            return false;
        }
        return isLegacyProjectile(mode) || owner.isValidParasiteTarget(target);
    }

    private static boolean isLegacyProjectile(Mode mode) {
        return mode == Mode.LENCIA_BALL || mode == Mode.ELVIA_BALL || mode == Mode.ELVIA_NADE;
    }

    private Vec3 steerTowardsHomingTarget(PrimitiveParasiteEntity owner, Vec3 movement, Mode mode) {
        if (level().isClientSide || mode != Mode.LIGHT || tickCount < 10 || owner == null) {
            return movement;
        }
        Entity entity = level().getEntity(entityData.get(HOMING_TARGET));
        if (!(entity instanceof LivingEntity target) || !owner.isValidParasiteTarget(target)) {
            return movement;
        }
        Vec3 direction = target.getEyePosition().subtract(position());
        if (direction.lengthSqr() < 0.001D) {
            return movement;
        }
        return movement.scale(0.78D).add(direction.normalize().scale(0.42D));
    }

    private void impact(PrimitiveParasiteEntity owner, Mode mode, LivingEntity directHit) {
        if (owner == null) {
            discard();
            return;
        }
        if (isLegacyProjectile(mode)) {
            impactLegacyProjectile(owner, mode, directHit);
            return;
        }
        boolean launch = mode == Mode.BOMB || mode == Mode.METEOR || mode == Mode.ACID;
        owner.hurtNearby(this, radius, damage, launch);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius), owner::isValidParasiteTarget)) {
            switch (mode) {
                case SPINE -> {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), owner);
                    if (owner instanceof DeterrentParasiteEntity deterrent
                            && deterrent.getKind() == DeterrentParasiteEntity.Kind.SENTRY) {
                        deterrent.applySentrySpineEffects(target);
                    }
                }
                case NEEDLE -> target.addEffect(new MobEffectInstance(ModMobEffects.NEEDLER, 180, 0), owner);
                case WITHER -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1), owner);
                case LIGHT -> {
                    target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 100, 0), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 0), owner);
                }
                case ACID -> {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 180, 0), owner);
                }
                case VOMIT -> {
                    target.addEffect(new MobEffectInstance(ModMobEffects.COTH, 160, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 160, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 160, 0), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1), owner);
                    target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 1), owner);
                }
                case BOMB -> {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 160, 0), owner);
                    target.igniteForSeconds(4.0F);
                }
                case METEOR -> target.igniteForSeconds(4.0F);
            }
        }
        if (mode == Mode.BOMB || mode == Mode.METEOR) {
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(radius));
            spawnLingeringCothCloud(owner);
            if (mode == Mode.BOMB && level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                level().explode(owner, getX(), getY(), getZ(), (float) Math.max(1.5D, radius),
                        Level.ExplosionInteraction.MOB);
            }
        } else if (mode == Mode.ACID && radius >= 1.25D) {
            spawnLingeringAcidCloud(owner);
        } else if (mode == Mode.VOMIT) {
            spawnLingeringVomitCloud(owner);
        } else if (mode == Mode.WITHER) {
            spawnLingeringWitherCloud(owner);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(mode == Mode.LIGHT || mode == Mode.WITHER ? ParticleTypes.SOUL_FIRE_FLAME
                            : mode == Mode.ACID || mode == Mode.VOMIT ? ParticleTypes.WITCH : ParticleTypes.EXPLOSION,
                    getX(), getY(), getZ(), 12, radius * 0.25, radius * 0.25, radius * 0.25, 0.02);
        }
        discard();
    }

    private void impactLegacyProjectile(PrimitiveParasiteEntity owner, Mode mode, LivingEntity directHit) {
        if ((mode == Mode.LENCIA_BALL || mode == Mode.ELVIA_BALL) && isProtectedParasite(directHit)) {
            discard();
            return;
        }
        if (mode == Mode.ELVIA_NADE) {
            accelerating = false;
            acceleration = Vec3.ZERO;
            setDeltaMovement(Vec3.ZERO);
            entityData.set(NADE_ARMED, true);
            entityData.set(NADE_FUSE_PROGRESS, 0);
            return;
        }
        if (directHit != null) {
            directHit.hurt(damageSources().mobProjectile(this, owner), damage);
        }
        if (mode == Mode.LENCIA_BALL) {
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(10.0D));
            level().explode(owner, getX(), getY(), getZ(), 10.0F, Level.ExplosionInteraction.MOB);
        }
        discard();
    }

    private static boolean isProtectedParasite(LivingEntity target) {
        return target instanceof Parasite
                && (!(target instanceof DeterrentParasiteEntity deterrent)
                || deterrent.getKind() != DeterrentParasiteEntity.Kind.SEIZER);
    }

    private void tickElviaNade(PrimitiveParasiteEntity owner) {
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) {
            for (int index = 0; index < 5; index++) {
                level().addParticle(ParticleTypes.SMOKE, getRandomX(1.0D), getRandomY(), getRandomZ(1.0D),
                        0.0D, 0.0D, 0.0D);
            }
            for (int index = 0; index < 2; index++) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(1.0D), getRandomY(), getRandomZ(1.0D),
                        0.0D, 0.0D, 0.0D);
            }
            return;
        }
        nadeIgnitionTicks++;
        if (nadeIgnitionTicks == 2) {
            playSound(ModSounds.NADE_IGNITE.get(), 1.0F, 1.0F);
        }
        if (nadeIgnitionTicks <= ELVIA_NADE_START_DELAY_TICKS) {
            return;
        }
        nadeFuseTicks++;
        entityData.set(NADE_FUSE_PROGRESS, Math.min(nadeFuseTicks, ELVIA_NADE_FUSE_TICKS - 1));
        if (nadeFuseTicks < ELVIA_NADE_FUSE_TICKS) {
            return;
        }
        nadeDamageTicks++;
        if (owner != null && owner.isAlive()) {
            AABB damageArea = new AABB(getX() - 1.45D, getY(), getZ() - 1.45D,
                    getX() + 1.45D, getY() + 1.46D, getZ() + 1.45D);
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), damageArea);
            float attackDamage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, damageArea,
                    owner::isValidParasiteTarget)) {
                target.hurt(damageSources().mobAttack(owner), attackDamage);
            }
        }
        if (nadeDamageTicks > ELVIA_NADE_DURATION_TICKS) {
            discard();
        }
    }

    private void spawnLingeringCothCloud(PrimitiveParasiteEntity owner) {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius((float) Math.max(2.0D, radius + 1.0D));
        cloud.setDuration(60);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 300, 0, false, true));
        level().addFreshEntity(cloud);
    }

    private void spawnLingeringAcidCloud(PrimitiveParasiteEntity owner) {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius((float) Math.max(2.0D, radius + 0.5D));
        cloud.setDuration(60);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 180, 0, false, true));
        level().addFreshEntity(cloud);
    }

    private void spawnLingeringVomitCloud(PrimitiveParasiteEntity owner) {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius((float) Math.max(2.0D, radius));
        cloud.setDuration(70);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 160, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 160, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 160, 0, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 1, false, true));
        level().addFreshEntity(cloud);
    }

    private void spawnLingeringWitherCloud(PrimitiveParasiteEntity owner) {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(owner);
        cloud.setRadius((float) Math.max(2.0D, radius + 0.75D));
        cloud.setDuration(100);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1, false, true));
        level().addFreshEntity(cloud);
    }

    private PrimitiveParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof PrimitiveParasiteEntity parasite ? parasite : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, Mode.SPINE.ordinal());
        builder.define(HOMING_TARGET, 0);
        builder.define(NADE_ARMED, false);
        builder.define(NADE_FUSE_PROGRESS, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        entityData.set(MODE, sanitizeMode(tag.getInt("mode")));
        entityData.set(HOMING_TARGET, tag.getInt("homing_target"));
        entityData.set(NADE_ARMED, tag.getBoolean("nade_armed"));
        entityData.set(NADE_FUSE_PROGRESS, tag.getInt("nade_fuse_progress"));
        damage = tag.getFloat("damage");
        radius = tag.getDouble("radius");
        maximumLifetime = tag.getInt("maximum_lifetime");
        acceleration = new Vec3(tag.getDouble("acceleration_x"), tag.getDouble("acceleration_y"),
                tag.getDouble("acceleration_z"));
        accelerating = tag.getBoolean("accelerating");
        nadeIgnitionTicks = tag.getInt("nade_ignition_ticks");
        nadeFuseTicks = tag.getInt("nade_fuse_ticks");
        nadeDamageTicks = tag.getInt("nade_damage_ticks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        tag.putInt("mode", entityData.get(MODE));
        tag.putInt("homing_target", entityData.get(HOMING_TARGET));
        tag.putBoolean("nade_armed", entityData.get(NADE_ARMED));
        tag.putInt("nade_fuse_progress", entityData.get(NADE_FUSE_PROGRESS));
        tag.putFloat("damage", damage);
        tag.putDouble("radius", radius);
        tag.putInt("maximum_lifetime", maximumLifetime);
        tag.putDouble("acceleration_x", acceleration.x);
        tag.putDouble("acceleration_y", acceleration.y);
        tag.putDouble("acceleration_z", acceleration.z);
        tag.putBoolean("accelerating", accelerating);
        tag.putInt("nade_ignition_ticks", nadeIgnitionTicks);
        tag.putInt("nade_fuse_ticks", nadeFuseTicks);
        tag.putInt("nade_damage_ticks", nadeDamageTicks);
    }

    public Mode getMode() {
        return Mode.values()[sanitizeMode(entityData.get(MODE))];
    }

    public boolean isLegacyProjectileMode() {
        return isLegacyProjectile(getMode());
    }

    public float getRenderWidth() {
        if (getMode() != Mode.ELVIA_NADE || !entityData.get(NADE_ARMED)) {
            return 0.3F;
        }
        return 0.5F + entityData.get(NADE_FUSE_PROGRESS) * 0.8F;
    }

    public float getRenderHeight() {
        if (getMode() != Mode.ELVIA_NADE || !entityData.get(NADE_ARMED)) {
            return 0.3F;
        }
        return 0.5F + entityData.get(NADE_FUSE_PROGRESS) * 0.32F;
    }

    private static int sanitizeMode(int modeIndex) {
        return modeIndex >= 0 && modeIndex < Mode.values().length ? modeIndex : Mode.SPINE.ordinal();
    }
}
