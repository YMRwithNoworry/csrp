package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Legacy Assimilated Adventurer. Its transition path is intentionally separate from the
 * generic assimilated-animal class because the original creature melts into Moving Flesh.
 */
public final class SimAdventurerEntity extends Monster implements GeoEntity, Parasite {
    public static final int MELT_KILL_THRESHOLD = 10;
    public static final int THRALL_KILL_THRESHOLD = 15;
    public static final int MELT_DURATION_TICKS = 127;
    private static final float BASE_HEIGHT = 1.95F;
    private static final float MELT_MIN_HEIGHT = 0.7F;
    private static final float MELT_HEIGHT_PER_TICK = 0.01F;
    private static final float MELT_MIN_SCALE = 0.35F;
    private static final float MELT_SCALE_PER_TICK = 0.005F;
    private static final int COTH_AURA_INTERVAL_TICKS = 20;
    private static final double COTH_AURA_RADIUS = 3.0D;
    private static final float HEAD_SPAWN_CHANCE = 0.5F;
    private static final float EXPLOSION_CHANCE = 0.25F;
    private static final String[] PLAYER_IDENTITY_NAMES = {
            "nischhelm", "Crimson Gaming", "Nyx/Sharp", "Elsa", "yodxxx1", "Akirawav3",
            "Mega Mario 2000", "roguetictac", "Golden Breeze", "Jaktt", "Mu Yao Shuanglin",
            "GrandeMalum", "IanZeArtist", "Kaleido", "KirbyKawaii", "Steve", "Alex"
    };
    private static final EntityDataAccessor<Boolean> MELTING = SynchedEntityData.defineId(
            SimAdventurerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MELT_TICKS = SynchedEntityData.defineId(
            SimAdventurerEntity.class, EntityDataSerializers.INT);
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int parasiteKills;

    public SimAdventurerEntity(EntityType<? extends SimAdventurerEntity> type, Level level) {
        super(type, level);
        xpReward = 10;
        setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MELTING, false);
        builder.define(MELT_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WaterPursuitLeapGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        if (isMelting()) {
            freezeMelting();
        }
        super.tick();
        if (isMelting()) {
            freezeMelting();
            if (level().isClientSide) {
                return;
            }
            tickMelting();
            return;
        }
        if (level().isClientSide) {
            return;
        }

        if (tickCount % COTH_AURA_INTERVAL_TICKS == 0) {
            infectNearby();
            tryStartEvolution();
        }
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        parasiteKills++;
        tryStartEvolution();
        return super.killedEntity(level, victim);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    protected boolean canReplaceCurrentItem(ItemStack candidate, ItemStack existing) {
        return getEquipmentSlotForItem(candidate) != EquipmentSlot.CHEST
                && super.canReplaceCurrentItem(candidate, existing);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide || isMelting()) {
            return;
        }

        if (random.nextFloat() < EXPLOSION_CHANCE) {
            spawnDeathBurst();
        }
        if (random.nextFloat() < HEAD_SPAWN_CHANCE) {
            spawnWalkingHead();
        }
    }

    public void melt() {
        if (isMelting()) {
            return;
        }
        entityData.set(MELTING, true);
        entityData.set(MELT_TICKS, 0);
        freezeMelting();
    }

    public boolean isMelting() {
        return entityData.get(MELTING);
    }

    public int getMeltTicks() {
        return entityData.get(MELT_TICKS);
    }

    public float getRenderScale(float partialTick) {
        float progress = Math.min(MELT_DURATION_TICKS, getMeltTicks() + partialTick);
        return Math.max(MELT_MIN_SCALE, 1.0F - progress * MELT_SCALE_PER_TICK);
    }

    public float getMeltHeight() {
        if (!isMelting()) {
            return BASE_HEIGHT;
        }
        return Math.max(MELT_MIN_HEIGHT, BASE_HEIGHT - getMeltTicks() * MELT_HEIGHT_PER_TICK);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDefaultDimensions(pose);
        return isMelting() ? dimensions.scale(1.0F, getMeltHeight() / BASE_HEIGHT) : dimensions;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == MELTING || accessor == MELT_TICKS) {
            refreshDimensions();
        }
    }

    public int getParasiteKills() {
        return parasiteKills;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (!level.isClientSide() && getCustomName() == null) {
            setCustomName(Component.literal(PLAYER_IDENTITY_NAMES[random.nextInt(PLAYER_IDENTITY_NAMES.length)]));
            setCustomNameVisible(true);
        }
        return data;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasite_kills", parasiteKills);
        tag.putBoolean("melting", isMelting());
        tag.putInt("melt_ticks", getMeltTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        parasiteKills = tag.getInt("parasite_kills");
        entityData.set(MELTING, tag.getBoolean("melting"));
        entityData.set(MELT_TICKS, tag.getInt("melt_ticks"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private void tryStartEvolution() {
        if (isMelting() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (parasiteKills > MELT_KILL_THRESHOLD && getTarget() == null && startMeltGroup()) {
            return;
        }
        if (parasiteKills > THRALL_KILL_THRESHOLD) {
            ThrallEntity thrall = ModEntities.THRALL.get().create(serverLevel);
            if (thrall == null) {
                return;
            }
            thrall.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            thrall.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            copyIdentity(thrall);
            thrall.setTarget(getTarget());
            serverLevel.addFreshEntity(thrall);
            discard();
        }
    }

    private boolean startMeltGroup() {
        double followRange = getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB searchBox = getBoundingBox().inflate(followRange);
        int movingFleshCount = 0;
        for (MovingFleshEntity flesh : level().getEntitiesOfClass(MovingFleshEntity.class, searchBox,
                Entity::isAlive)) {
            if (hasLineOfSight(flesh)) {
                movingFleshCount += flesh.getMergeCount();
            }
        }
        if (movingFleshCount >= 1 && movingFleshCount <= 3) {
            melt();
            return true;
        }

        List<Mob> candidates = level().getEntitiesOfClass(Mob.class, searchBox,
                this::isMeltCandidate);
        if (candidates.size() < 3) {
            return false;
        }
        int selected = 0;
        for (int index = 0; index < candidates.size() && selected < 3; index++) {
            startMelt(candidates.get(index));
            selected++;
        }
        melt();
        return true;
    }

    private boolean isMeltCandidate(Mob entity) {
        if (entity == this || !entity.isAlive() || !hasLineOfSight(entity) || entity.getTarget() != null) {
            return false;
        }
        if (entity instanceof SimAdventurerEntity adventurer) {
            return !adventurer.isMelting();
        }
        return entity instanceof AssimilatedParasiteEntity animal && animal.canMelt();
    }

    private void startMelt(Mob entity) {
        if (entity instanceof SimAdventurerEntity adventurer) {
            adventurer.melt();
        } else if (entity instanceof AssimilatedParasiteEntity animal) {
            animal.melt();
        }
    }

    private void tickMelting() {
        int ticks = getMeltTicks() + 1;
        entityData.set(MELT_TICKS, ticks);
        if (ticks % 20 == 0) {
            playSound(ModSounds.SIM_ADVENTURER_MELT.get(), 1.0F, 1.0F);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    4, 0.18D, 0.22D, 0.18D, 0.01D);
        }
        if (ticks < MELT_DURATION_TICKS || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        MovingFleshEntity flesh = ModEntities.MOVINGFLESH.get().create(serverLevel);
        if (flesh != null) {
            flesh.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            flesh.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            copyIdentity(flesh);
            serverLevel.addFreshEntity(flesh);
        }
        discard();
    }

    private void infectNearby() {
        for (LivingEntity nearby : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(COTH_AURA_RADIUS), this::isValidParasiteTarget)) {
            if (hasLineOfSight(nearby)) {
                InfectionMechanics.applyCoth(nearby, this);
            }
        }
    }

    private boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    private void spawnWalkingHead() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        SimAdventurerHeadEntity head = ModEntities.SIM_ADVENTURER_HEAD.get().create(serverLevel);
        if (head == null) {
            return;
        }
        head.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        head.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        serverLevel.addFreshEntity(head);
    }

    private void spawnDeathBurst() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        playSound(ModSounds.SIM_ADVENTURER_EXPLODE.get(), 1.0F, 1.0F);
        serverLevel.addFreshEntity(new ItemEntity(serverLevel, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                new ItemStack(ModItems.ASSIMILATED_FLESH.get())));
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(3.0F);
        cloud.setDuration(200);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, false));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 200, 0, false, false));
        serverLevel.addFreshEntity(cloud);

        int count = 3 + random.nextInt(2);
        for (int index = 0; index < count; index++) {
            BuglinEntity buglin = ModEntities.BUGLIN.get().create(serverLevel);
            if (buglin == null) {
                continue;
            }
            buglin.moveTo(getX() + (random.nextDouble() - 0.5D), getY(), getZ() + (random.nextDouble() - 0.5D),
                    getYRot(), 0.0F);
            buglin.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            serverLevel.addFreshEntity(buglin);
        }
    }

    private void freezeMelting() {
        getNavigation().stop();
        setTarget(null);
        setDeltaMovement(Vec3.ZERO);
    }

    private void copyIdentity(Mob target) {
        target.setCustomName(getCustomName());
        target.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            target.setPersistenceRequired();
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.SIM_ADVENTURER_LIVING.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SIM_ADVENTURER_HURT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return ModSounds.SIM_ADVENTURER_DEATH.get();
    }

    private final class WaterPursuitLeapGoal extends Goal {
        private WaterPursuitLeapGoal() {
            setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isInWaterOrBubble() && getTarget() != null && random.nextInt(12) == 0;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            Vec3 direction = target.position().subtract(position());
            if (direction.lengthSqr() > 0.001D) {
                direction = direction.normalize();
                setDeltaMovement(getDeltaMovement().add(direction.x * 0.35D, 0.18D, direction.z * 0.35D));
            }
        }
    }
}
