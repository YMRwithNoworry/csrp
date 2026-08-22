package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.ResourceLocation;
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
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

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
    private static final int EVOLUTION_FUSE_INCREMENT = 2;
    private static final int AUTO_EVOLUTION_AGE_TICKS = 800;
    private static final int MERGE_COOLDOWN_TICKS = 20;
    private static final float REGEN_PER_TICK = 0.007F;
    private static final EntityDataAccessor<Integer> MERGE_COUNT = SynchedEntityData.defineId(
            MovingFleshEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MERGE_VALUE = SynchedEntityData.defineId(
            MovingFleshEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RENDER_SCALE = SynchedEntityData.defineId(
            MovingFleshEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> EVOLUTION_FUSE = SynchedEntityData.defineId(
            MovingFleshEntity.class, EntityDataSerializers.INT);
    private final RawAnimation AGE = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");

    private float targetScale = 1.0F;
    private int mergeCooldown;
    private int mergeContacts;
    private int mergeContactCooldown;
    private float evolutionFlashIntensity = 0.0F;

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
    protected void defineSynchedData() {
        super.defineSynchedData(builder);
        builder.define(MERGE_COUNT, 1);
        builder.define(MERGE_VALUE, (1 + random.nextInt(2)) * 2);
        builder.define(RENDER_SCALE, 1.0F);
        builder.define(EVOLUTION_FUSE, 0);
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
            // 客户端：更新进化闪烁效果
            if (getEvolutionFuse() > 0) {
                float progress = 1.0F - (getEvolutionFuse() / (float) EVOLUTION_DELAY_TICKS);
                evolutionFlashIntensity = progress;
            } else {
                evolutionFlashIntensity = 0.0F;
            }
            return;
        }
        if (getHealth() > 0.0F && getHealth() < getMaxHealth()) {
            heal(REGEN_PER_TICK);
        }
        if (isInWaterOrBubble() && tickCount % 10 == 0) {
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(movement.x * 1.1D, Math.max(movement.y, 0.15D), movement.z * 1.1D);
        }
        if (getRenderScale(1.0F) < targetScale) {
            entityData.set(RENDER_SCALE, Math.min(targetScale, getRenderScale(1.0F) + 0.01F));
        }
        if (mergeContactCooldown > 0) {
            mergeContactCooldown--;
        }
        if (getEvolutionFuse() > 0) {
            int remaining = Math.max(0, getEvolutionFuse() - EVOLUTION_FUSE_INCREMENT);
            entityData.set(EVOLUTION_FUSE, remaining);
            if (remaining == 0) {
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
        if (mergeContactCooldown > 0) {
            return true;
        }
        mergeContacts++;
        mergeContactCooldown = 20;
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

    public int getMergeValue() {
        return entityData.get(MERGE_VALUE);
    }

    public int getEvolutionFuse() {
        return entityData.get(EVOLUTION_FUSE);
    }

    public void setMergeValue(int value) {
        entityData.set(MERGE_VALUE, Math.max(0, value));
    }

    public float getRenderScale(float partialTick) {
        return entityData.get(RENDER_SCALE);
    }

    /**
     * 获取进化闪烁强度（用于渲染器中的爆炸前效果）
     * @param partialTick 部分tick时间
     * @return 0.0-1.0的闪烁强度
     */
    public float getEvolutionFlashIntensity(float partialTick) {
        return evolutionFlashIntensity;
    }

    @Override
    protected EntityDimensions getDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDimensions(pose);
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
        tag.putInt("merge_value", getMergeValue());
        tag.putFloat("render_scale", getRenderScale(1.0F));
        tag.putFloat("target_scale", targetScale);
        tag.putInt("merge_cooldown", mergeCooldown);
        tag.putInt("evolution_delay", getEvolutionFuse());
        tag.putInt("merge_contacts", mergeContacts);
        tag.putInt("merge_contact_cooldown", mergeContactCooldown);
        tag.putFloat("evolution_flash_intensity", evolutionFlashIntensity);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(MERGE_COUNT, Math.max(1, tag.getInt("merge_count")));
        if (tag.contains("merge_value")) {
            setMergeValue(tag.getInt("merge_value"));
        }
        entityData.set(RENDER_SCALE, Math.max(1.0F, tag.getFloat("render_scale")));
        targetScale = Math.max(entityData.get(RENDER_SCALE), tag.getFloat("target_scale"));
        mergeCooldown = tag.getInt("merge_cooldown");
        entityData.set(EVOLUTION_FUSE, Math.max(0, tag.getInt("evolution_delay")));
        mergeContacts = tag.getInt("merge_contacts");
        mergeContactCooldown = Math.max(0, tag.getInt("merge_contact_cooldown"));
        evolutionFlashIntensity = tag.getFloat("evolution_flash_intensity");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 0,
                state -> state.setAndContinue(AGE)));
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> ParasiteAnimations.isMoving(this, state.isMoving())
                        ? state.setAndContinue(LIMB)
                        : software.bernie.geckolib.core.object.PlayState.STOP));
    }

    private void absorb(MovingFleshEntity other) {
        int combined = getMergeCount() + other.getMergeCount();
        entityData.set(MERGE_COUNT, combined);
        setMergeValue(getMergeValue() + other.getMergeValue());
        targetScale += 0.3F;
        mergeCooldown = MERGE_COOLDOWN_TICKS;
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(Math.max(0.0D, speed.getValue() - 0.01D));
        }
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
        if (getEvolutionFuse() == 0) {
            entityData.set(EVOLUTION_FUSE, EVOLUTION_DELAY_TICKS);
            navigation.stop();
        }
    }

    private void evolveToPrimitive() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Mob primitive = createConfiguredPrimitive(serverLevel);
        if (primitive == null) {
            return;
        }
        primitive.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        primitive.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        primitive.setHealth(primitive.getMaxHealth() * (float) MobsConfig.mergeSystemMobHealth());
        primitive.setCustomName(getCustomName());
        primitive.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            primitive.setPersistenceRequired();
        }
        playSound(ModSounds.MOVING_FLESH_PRIMITIVE.get(), 1.0F, 1.0F);
        if (serverLevel.addFreshEntity(primitive)) {
            EvolutionSystem.addPoints(serverLevel, EvolutionSystem.VALUE_MERGE,
                    EvolutionSystem.PointSource.MERGE);
            discard();
        } else {
            primitive.discard();
        }
    }

    private Mob createConfiguredPrimitive(ServerLevel serverLevel) {
        List<? extends String> table = MobsConfig.mergeSystemMobList();
        if (table.isEmpty()) {
            return null;
        }
        String selected = null;
        if (!MobsConfig.mergeSystemRandom()) {
            for (String entry : table) {
                String[] parts = entry.split(";", -1);
                if (parts.length == 2) {
                    try {
                        if (Integer.parseInt(parts[1].trim()) == getMergeValue()) {
                            selected = parts[0].trim();
                            break;
                        }
                    } catch (NumberFormatException ignored) {
                        // Config validation normally rejects this; skip stale malformed entries safely.
                    }
                }
            }
        }
        if (selected == null) {
            selected = table.get(random.nextInt(table.size())).split(";", -1)[0].trim();
        }
        ResourceLocation location = ResourceLocation.tryParse(selected);
        if (location == null) {
            return null;
        }
        if (location.getNamespace().equals("srparasites")) {
            location = new ResourceLocation("csrp", location.getPath());
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(location).orElse(null);
        if (type == null || !(type.create(serverLevel) instanceof Mob primitive)) {
            return null;
        }
        return primitive;
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
            if (mergeCooldown > 0 || getEvolutionFuse() > 0) {
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
        return other != this && other.isAlive() && other.getEvolutionFuse() == 0 && other.mergeCooldown == 0
                && other.getMergeCount() + getMergeCount() <= REQUIRED_MERGES;
    }
}
