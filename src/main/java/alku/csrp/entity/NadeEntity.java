package alku.csrp.entity;

import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, Kind.ELVIA.ordinal());
        builder.define(FUSE_PROGRESS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (level().isClientSide()) {
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
                    target.setInvulnerableTime(0);
                    target.hurt(damageSources().mobAttack(owner), frameDamage);
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), owner);
                    target.addEffect(new MobEffectInstance(ModMobEffects.CORROSION, 60, 0), owner);
                }
                case YELLOWEYE -> {
                    target.setInvulnerableTime(0);
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
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("owner", UUIDUtil.CODEC).ifPresent(uuid -> ownerId = uuid);
        entityData.set(KIND, input.getIntOr("kind", 0));
        entityData.set(FUSE_PROGRESS, input.getIntOr("fuse_progress", 0));
        startDelayTicks = input.getIntOr("start_delay_ticks", 0);
        fuseTicks = Math.max(1, input.getIntOr("fuse_ticks", 0));
        durationTicks = Math.max(1, input.getIntOr("duration_ticks", 0));
        activeTicks = input.getIntOr("active_ticks", 0);
        damageTicks = input.getIntOr("damage_ticks", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerId != null) {
            output.store("owner", UUIDUtil.CODEC, ownerId);
        }
        output.putInt("kind", entityData.get(KIND));
        output.putInt("fuse_progress", entityData.get(FUSE_PROGRESS));
        output.putInt("start_delay_ticks", startDelayTicks);
        output.putInt("fuse_ticks", fuseTicks);
        output.putInt("duration_ticks", durationTicks);
        output.putInt("active_ticks", activeTicks);
        output.putInt("damage_ticks", damageTicks);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /**
     * The 1.10.8 source never overrode {@code Entity#hurt}, so a nade absorbed every hit without
     * reacting to it. 26.3 made {@code hurtServer} the required hook for that same decision.
     */
    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float damage) {
        return false;
    }
}
