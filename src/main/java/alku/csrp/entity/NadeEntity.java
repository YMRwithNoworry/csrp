package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public final class NadeEntity extends Entity {
    public enum Kind {
        ELVIA,
        ACID,
        YELLOWEYE
    }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(
            NadeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUSE_PROGRESS = SynchedEntityData.defineId(
            NadeEntity.class, EntityDataSerializers.INT);
    private UUID ownerId;
    private int startDelayTicks = 3;
    private int fuseTicks = 4;
    private int durationTicks = 60;
    private int activeTicks;
    private int damageTicks;

    public NadeEntity(EntityType<? extends NadeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void configure(PrimitiveParasiteEntity owner, Kind kind) {
        ownerId = owner.getUUID();
        entityData.set(KIND, kind.ordinal());
        switch (kind) {
            case ELVIA -> {
                startDelayTicks = 3;
                fuseTicks = 4;
            }
            case ACID -> {
                startDelayTicks = 0;
                fuseTicks = 3;
            }
            case YELLOWEYE -> {
                startDelayTicks = 3;
                fuseTicks = 3;
            }
        }
        durationTicks = 60;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, Kind.ELVIA.ordinal());
        entityData.define(FUSE_PROGRESS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (level().isClientSide) {
            spawnClientParticles();
            return;
        }
        activeTicks++;
        if (activeTicks == 2) {
            playSound(ModSounds.NADE_IGNITE.get(), 1.0F, 1.0F);
        }
        if (activeTicks <= startDelayTicks) {
            return;
        }
        int fuseProgress = activeTicks - startDelayTicks;
        entityData.set(FUSE_PROGRESS, Math.min(fuseProgress, fuseTicks));
        if (fuseProgress < fuseTicks) {
            return;
        }
        PrimitiveParasiteEntity owner = owner();
        if (owner != null && owner.isAlive()) {
            applyFrameDamage(owner);
        }
        if (++damageTicks > durationTicks) {
            discard();
        }
    }

    private void applyFrameDamage(PrimitiveParasiteEntity owner) {
        double halfWidth = getRenderWidth() * 0.5D;
        AABB area = new AABB(getX() - halfWidth, getY(), getZ() - halfWidth,
                getX() + halfWidth, getY() + getRenderHeight(), getZ() + halfWidth);
        float frameDamage = (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (getKind() == Kind.ELVIA) {
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), area);
        }
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                owner::isValidParasiteTarget)) {
            switch (getKind()) {
                case ELVIA -> target.hurt(damageSources().mobAttack(owner), frameDamage);
                case ACID -> {
                    target.invulnerableTime = 0;
                    target.hurt(damageSources().mobAttack(owner), frameDamage);
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.CORROSION.get(), 60, 0), owner);
                }
                case YELLOWEYE -> {
                    target.invulnerableTime = 0;
                    target.hurt(damageSources().magic(), Math.max(frameDamage,
                            (float) MobsConfig.yelloweyeNadeDamage()));
                    owner.applyPrimitiveMinimumDamage(target);
                }
            }
        }
    }

    private void spawnClientParticles() {
        int count = getKind() == Kind.ELVIA ? 7 : 4;
        for (int index = 0; index < count; index++) {
            level().addParticle(getKind() == Kind.ELVIA
                            ? (index < 5 ? net.minecraft.core.particles.ParticleTypes.SMOKE
                                    : net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE)
                            : net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,
                    getRandomX(getRenderWidth()), getY() + random.nextDouble() * getRenderHeight(),
                    getRandomZ(getRenderWidth()), 0.0D, 0.01D, 0.0D);
        }
    }

    private PrimitiveParasiteEntity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerId);
        return entity instanceof PrimitiveParasiteEntity parasite ? parasite : null;
    }

    public Kind getKind() {
        int value = entityData.get(KIND);
        return Kind.values()[value >= 0 && value < Kind.values().length ? value : 0];
    }

    public float getRenderWidth() {
        return 0.5F + entityData.get(FUSE_PROGRESS) * 0.8F;
    }

    public float getRenderHeight() {
        return 0.5F + entityData.get(FUSE_PROGRESS) * 0.32F;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        entityData.set(KIND, tag.getInt("kind"));
        entityData.set(FUSE_PROGRESS, tag.getInt("fuse_progress"));
        startDelayTicks = tag.getInt("start_delay_ticks");
        fuseTicks = Math.max(1, tag.getInt("fuse_ticks"));
        durationTicks = Math.max(1, tag.getInt("duration_ticks"));
        activeTicks = tag.getInt("active_ticks");
        damageTicks = tag.getInt("damage_ticks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) {
            tag.putUUID("owner", ownerId);
        }
        tag.putInt("kind", entityData.get(KIND));
        tag.putInt("fuse_progress", entityData.get(FUSE_PROGRESS));
        tag.putInt("start_delay_ticks", startDelayTicks);
        tag.putInt("fuse_ticks", fuseTicks);
        tag.putInt("duration_ticks", durationTicks);
        tag.putInt("active_ticks", activeTicks);
        tag.putInt("damage_ticks", damageTicks);
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
