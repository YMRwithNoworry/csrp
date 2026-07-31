package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * The utility-stage Moving Flesh: it avoids non-parasites, merges up to four bodies, and
 * becomes a random available primitive parasite after maturing.
 */
public final class MovingFleshEntity extends CrudeParasiteEntity {
    private static final float BASE_WIDTH = 0.7F;
    private static final float BASE_HEIGHT = 0.5F;
    private static final int REQUIRED_MERGES = 4;
    private static final int EVOLUTION_DELAY_TICKS = 70;
    private static final int AUTO_EVOLUTION_AGE_TICKS = 800;
    private static final int MERGE_COOLDOWN_TICKS = 20;
    private static final float REGEN_PER_SECOND = 0.125F;
    private static final EntityDataAccessor<Integer> MERGE_COUNT = SynchedEntityData.defineId(
            MovingFleshEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RENDER_SCALE = SynchedEntityData.defineId(
            MovingFleshEntity.class, EntityDataSerializers.FLOAT);
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");

    private float targetScale = 1.0F;
    private int mergeCooldown;
    private int evolutionDelay;
    private int mergeContacts;

    public MovingFleshEntity(EntityType<? extends MovingFleshEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MERGE_COUNT, 1);
        builder.define(RENDER_SCALE, 1.0F);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MergeMovingFleshGoal());
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, LivingEntity.class, 8.0F, 1.0D, 1.3D,
                entity -> entity instanceof Player || entity instanceof Monster && !(entity instanceof Parasite)));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
            heal(REGEN_PER_SECOND);
        }
        if (isInWaterOrBubble() && tickCount % 10 == 0) {
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(movement.x * 1.1D, Math.max(movement.y, 0.15D), movement.z * 1.1D);
        }
        if (getRenderScale(1.0F) < targetScale) {
            entityData.set(RENDER_SCALE, Math.min(targetScale, getRenderScale(1.0F) + 0.01F));
        }
        if (evolutionDelay > 0) {
            if (--evolutionDelay == 0) {
                evolveToPrimitive();
            }
            return;
        }
        if (mergeCooldown > 0) {
            mergeCooldown--;
        } else if (tickCount > AUTO_EVOLUTION_AGE_TICKS && getMergeCount() > 1) {
            startEvolution();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof MovingFleshEntity other) || !canMergePartner(other)) {
            return false;
        }
        mergeContacts++;
        if (mergeContacts < 3) {
            return true;
        }
        mergeContacts = 0;
        if (getRenderScale(1.0F) >= other.getRenderScale(1.0F)) {
            absorb(other);
        } else {
            other.absorb(this);
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.LITE_FLESH_SLIDE.get(), 0.3F, getVoicePitch());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MOVING_FLESH_LIVING.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MOVING_FLESH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MOVING_FLESH_DEATH.get();
    }

    public int getMergeCount() {
        return entityData.get(MERGE_COUNT);
    }

    public float getRenderScale(float partialTick) {
        return entityData.get(RENDER_SCALE);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDefaultDimensions(pose);
        float growth = getRenderScale(1.0F) - 1.0F;
        return dimensions.scale((BASE_WIDTH + growth) / BASE_WIDTH, (BASE_HEIGHT + growth) / BASE_HEIGHT);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == RENDER_SCALE) {
            refreshDimensions();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("merge_count", getMergeCount());
        tag.putFloat("render_scale", getRenderScale(1.0F));
        tag.putFloat("target_scale", targetScale);
        tag.putInt("merge_cooldown", mergeCooldown);
        tag.putInt("evolution_delay", evolutionDelay);
        tag.putInt("merge_contacts", mergeContacts);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(MERGE_COUNT, Math.max(1, tag.getInt("merge_count")));
        entityData.set(RENDER_SCALE, Math.max(1.0F, tag.getFloat("render_scale")));
        targetScale = Math.max(entityData.get(RENDER_SCALE), tag.getFloat("target_scale"));
        mergeCooldown = tag.getInt("merge_cooldown");
        evolutionDelay = tag.getInt("evolution_delay");
        mergeContacts = tag.getInt("merge_contacts");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    private void absorb(MovingFleshEntity other) {
        int combined = getMergeCount() + other.getMergeCount();
        entityData.set(MERGE_COUNT, combined);
        targetScale += 0.3F;
        mergeCooldown = MERGE_COOLDOWN_TICKS;
        playSound(ModSounds.MOVING_FLESH_EAT.get(), 1.0F, 1.0F);
        playSound(ModSounds.MOVING_FLESH_GROW.get(), 1.0F, 1.0F);
        if (getCustomName() == null && other.getCustomName() != null) {
            setCustomName(other.getCustomName());
            setCustomNameVisible(other.isCustomNameVisible());
        }
        other.discard();
        if (combined >= REQUIRED_MERGES) {
            startEvolution();
        }
    }

    private void startEvolution() {
        if (evolutionDelay == 0) {
            evolutionDelay = EVOLUTION_DELAY_TICKS;
            navigation.stop();
        }
    }

    private void evolveToPrimitive() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Mob primitive = switch (random.nextInt(12)) {
            case 0 -> ModEntities.PRI_LONGARMS.get().create(serverLevel);
            case 1 -> ModEntities.PRI_SUMMONER.get().create(serverLevel);
            case 2 -> ModEntities.PRI_VERMIN.get().create(serverLevel);
            case 3 -> ModEntities.PRI_VISCERA.get().create(serverLevel);
            case 4 -> ModEntities.PRI_ARACHNIDA.get().create(serverLevel);
            case 5 -> ModEntities.PRI_BOLSTER.get().create(serverLevel);
            case 6 -> ModEntities.PRI_BURROWER.get().create(serverLevel);
            case 7 -> ModEntities.PRI_DEVOURER.get().create(serverLevel);
            case 8 -> ModEntities.PRI_MANDUCATER.get().create(serverLevel);
            case 9 -> ModEntities.PRI_REEKER.get().create(serverLevel);
            case 10 -> ModEntities.PRI_TOZOON.get().create(serverLevel);
            default -> ModEntities.PRI_YELLOWEYE.get().create(serverLevel);
        };
        if (primitive == null) {
            return;
        }
        primitive.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        primitive.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        primitive.setCustomName(getCustomName());
        primitive.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            primitive.setPersistenceRequired();
        }
        playSound(ModSounds.MOVING_FLESH_PRIMITIVE.get(), 1.0F, 1.0F);
        serverLevel.addFreshEntity(primitive);
        discard();
    }

    private final class MergeMovingFleshGoal extends Goal {
        private static final double SEARCH_RADIUS = 16.0D;
        private static final double ABSORB_DISTANCE_SQR = 2.25D;
        private MovingFleshEntity target;

        private MergeMovingFleshGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mergeCooldown > 0 || evolutionDelay > 0) {
                return false;
            }
            target = findMergeTarget();
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && canMergeWith(target) && distanceToSqr(target) <= SEARCH_RADIUS * SEARCH_RADIUS;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (distanceToSqr(target) <= ABSORB_DISTANCE_SQR) {
                doHurtTarget(target);
                if (!isAlive() || !target.isAlive()) {
                    target = null;
                }
                return;
            }
            navigation.moveTo(target, 1.1D);
        }

        @Override
        public void stop() {
            target = null;
            navigation.stop();
        }

        private MovingFleshEntity findMergeTarget() {
            List<MovingFleshEntity> candidates = level().getEntitiesOfClass(MovingFleshEntity.class,
                    getBoundingBox().inflate(SEARCH_RADIUS), this::canMergeWith);
            return candidates.stream().min(Comparator.comparingDouble(MovingFleshEntity.this::distanceToSqr))
                    .orElse(null);
        }

        private boolean canMergeWith(MovingFleshEntity other) {
            return canMergePartner(other);
        }
    }

    private boolean canMergePartner(MovingFleshEntity other) {
        return other != this && other.isAlive() && other.evolutionDelay == 0 && other.mergeCooldown == 0
                && other.getMergeCount() + getMergeCount() <= REQUIRED_MERGES;
    }
}
