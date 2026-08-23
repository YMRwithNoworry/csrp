package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shared implementation for the remaining assimilated bodies.  Their body
 * forms retain the legacy fire weakness, COTH contact damage, head-on-death
 * transition, and explosive remains burst.
 */
public final class AssimilatedVariantEntity extends Monster implements GeoEntity, Parasite, MeltableAssimilated {
    private static final EntityDataAccessor<Integer> ANIMATION_STATUS = SynchedEntityData.defineId(
            AssimilatedVariantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MELTING = SynchedEntityData.defineId(
            AssimilatedVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MELT_TICKS = SynchedEntityData.defineId(
            AssimilatedVariantEntity.class, EntityDataSerializers.INT);
    private static final float HEAD_SPAWN_CHANCE = 0.5F;
    private static final float EXPLOSION_CHANCE = 0.25F;
    private static final float BLEED_CHANCE = 0.2F;
    private static final int HOST_SKELETON_KILLS = 5;
    private static final int STILL_ANIMATION_DELAY_TICKS = 25;
    private final RawAnimation AGE = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation AGE_STILL = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_still_ani_1");
    private final RawAnimation AGE_STATUS_1 = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation LIMB_STATUS_1 = ParasiteAnimations.loop(
            this, "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation AGE_STATUS_1_STILL = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation AGE_STATUS_2 = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_2");
    private final RawAnimation LIMB_STATUS_2 = ParasiteAnimations.loop(
            this, "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation AGE_STATUS_2_STILL = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1");
    private final RawAnimation LIMB_STATUS_3 = ParasiteAnimations.loop(
            this, "func_78087_a.limb_swing.get_parasite_status_3");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Kind kind;
    private int parasiteKills;
    private int rangedCooldown;
    private int stillAnimationTicks;
    private int skeletonKills;

    public AssimilatedVariantEntity(EntityType<? extends AssimilatedVariantEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.experience;
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind.knockbackResistance)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.FOLLOW_RANGE, kind.followRange);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIMATION_STATUS, 0);
        entityData.define(MELTING, false);
        entityData.define(MELT_TICKS, 0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ParasiteSoundProfiles.ambient(this);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ParasiteSoundProfiles.hurt(this);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ParasiteSoundProfiles.death(this);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, kind == Kind.HORSE ? 1.5D : 1.2D, false));
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
            AssimilatedMeltSystem.freeze(this);
        }
        super.tick();
        if (ParasiteAnimations.isMoving(this, true)) {
            stillAnimationTicks = 0;
        } else {
            stillAnimationTicks++;
        }
        if (level().isClientSide) {
            return;
        }
        if (isMelting()) {
            AssimilatedMeltSystem.freeze(this);
            tickMelting();
            return;
        }
        if (rangedCooldown > 0) {
            rangedCooldown--;
        }
        updateAnimationStatus();
        if (tickCount % 20 == 0) {
            infectNearby();
            if (AssimilatedMeltSystem.tryStartGroup(this, parasiteKills)) {
                parasiteKills = 0;
            }
        }
        if (kind == Kind.BIGSPIDER && rangedCooldown <= 0 && getTarget() != null && hasLineOfSight(getTarget())) {
            fireWebBall(getTarget());
            rangedCooldown = 60;
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (!isAlive() || (kind == Kind.HUMAN && isDeadOrDying())) {
            return false;
        }
        LivingEntity livingTarget = entity instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(entity);
        if (hit && livingTarget != null) {
            ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
            InfectionMechanics.applyCoth(livingTarget, this);
            if ((kind == Kind.HUMAN || kind == Kind.VILLAGER) && random.nextFloat() < BLEED_CHANCE) {
                livingTarget.addEffect(new MobEffectInstance(ModMobEffects.BLEED.get(), 100, 0), this);
            }
            if (kind == Kind.BIGSPIDER && random.nextInt(3) == 0) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), this);
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        if ((kind == Kind.HUMAN || kind == Kind.VILLAGER) && victim instanceof AbstractSkeleton
                && ++skeletonKills >= HOST_SKELETON_KILLS) {
            transformToHost(level);
            return super.killedEntity(level, victim);
        }
        parasiteKills++;
        if (AssimilatedMeltSystem.tryStartGroup(this, parasiteKills)) {
            parasiteKills = 0;
        } else if (parasiteKills > AssimilatedParasiteEntity.FERAL_KILL_THRESHOLD) {
            transformToFeral(level);
        }
        return super.killedEntity(level, victim);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasite_kills", parasiteKills);
        tag.putInt("ranged_cooldown", rangedCooldown);
        tag.putInt("skeleton_kills", skeletonKills);
        tag.putBoolean("melting", isMelting());
        tag.putInt("melt_ticks", entityData.get(MELT_TICKS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        parasiteKills = tag.getInt("parasite_kills");
        rangedCooldown = tag.getInt("ranged_cooldown");
        skeletonKills = tag.getInt("skeleton_kills");
        entityData.set(MELTING, tag.getBoolean("melting"));
        entityData.set(MELT_TICKS, tag.getInt("melt_ticks"));
    }

    @Override
    public boolean canMelt() {
        return kind != Kind.BIGSPIDER && !isMelting();
    }

    @Override
    public boolean isMelting() {
        return entityData.get(MELTING);
    }

    @Override
    public void melt() {
        if (!canMelt()) {
            return;
        }
        entityData.set(MELTING, true);
        entityData.set(MELT_TICKS, 0);
        AssimilatedMeltSystem.freeze(this);
        refreshDimensions();
    }

    @Override
    public float getMeltRenderScale(float partialTick) {
        if (!isMelting()) {
            return 1.0F;
        }
        return Math.max(0.01F, 1.0F - (entityData.get(MELT_TICKS) + partialTick) * 0.005F);
    }

    public float getMeltHeight() {
        if (!isMelting()) {
            return kind.baseHeight;
        }
        return Math.max(0.7F, kind.meltStartHeight - entityData.get(MELT_TICKS) * 0.01F);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDimensions(pose);
        return isMelting() ? dimensions.scale(1.0F, getMeltHeight() / kind.baseHeight) : dimensions;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == MELTING || accessor == MELT_TICKS) {
            refreshDimensions();
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) {
            return;
        }
        if (kind != Kind.BIGSPIDER && random.nextFloat() < HEAD_SPAWN_CHANCE) {
            spawnWalkingHead();
        }
        if (random.nextFloat() < EXPLOSION_CHANCE) {
            spawnDeathBurst();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 0,
                state -> state.setAndContinue(ageAnimation())));
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                return PlayState.STOP;
            }
            return state.setAndContinue(limbAnimation());
        }));
    }

    private void updateAnimationStatus() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            entityData.set(ANIMATION_STATUS, 0);
            return;
        }
        if (kind == Kind.BIGSPIDER) {
            entityData.set(ANIMATION_STATUS, 1);
            return;
        }
        double reach = getBbWidth() * 2.0D;
        entityData.set(ANIMATION_STATUS,
                distanceToSqr(target) <= reach * reach + target.getBbWidth() ? 1 : 2);
    }

    private RawAnimation ageAnimation() {
        int status = entityData.get(ANIMATION_STATUS);
        boolean still = stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS;
        return switch (kind) {
            case BIGSPIDER -> status == 1
                    ? (still ? AGE_STATUS_1_STILL : AGE_STATUS_1)
                    : (still ? AGE_STILL : AGE);
            case HORSE -> status == 2 ? AGE_STATUS_2 : AGE;
            case VILLAGER -> switch (status) {
                case 1 -> still ? AGE_STATUS_1_STILL : AGE_STATUS_1;
                case 2 -> still ? AGE_STATUS_2_STILL : AGE_STATUS_2;
                default -> still ? AGE_STILL : AGE;
            };
            case HUMAN -> switch (status) {
                case 1 -> still ? AGE_STATUS_1_STILL : AGE_STATUS_1;
                case 2 -> AGE_STATUS_2;
                default -> still ? AGE_STILL : AGE;
            };
        };
    }

    private RawAnimation limbAnimation() {
        int status = entityData.get(ANIMATION_STATUS);
        return switch (status) {
            case 1 -> LIMB_STATUS_1;
            case 2 -> LIMB_STATUS_2;
            case 3 -> LIMB_STATUS_3;
            default -> LIMB;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    private void infectNearby() {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(3.0D), this::isValidParasiteTarget)) {
            if (hasLineOfSight(target)) {
                InfectionMechanics.applyCoth(target, this);
            }
        }
    }

    private void fireWebBall(LivingEntity target) {
        Vec3 start = getEyePosition();
        Vec3 direction = target.getEyePosition().subtract(start);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().expandTowards(direction.normalize().scale(12.0D)).inflate(1.0D),
                this::isValidParasiteTarget)) {
            if (hasLineOfSight(victim)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1), this);
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), this);
                InfectionMechanics.applyCoth(victim, this);
                break;
            }
        }
    }

    private void spawnWalkingHead() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AssimilatedHeadEntity head = switch (kind) {
            case HORSE -> ModEntities.SIM_HORSE_HEAD.get().create(serverLevel);
            case HUMAN -> ModEntities.SIM_HUMAN_HEAD.get().create(serverLevel);
            case VILLAGER -> ModEntities.SIM_VILLAGER_HEAD.get().create(serverLevel);
            case BIGSPIDER -> null;
        };
        if (head == null) {
            return;
        }
        head.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        head.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null, null);
        head.setCustomName(getCustomName());
        head.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            head.setPersistenceRequired();
        }
        serverLevel.addFreshEntity(head);
    }

    private void transformToFeral(ServerLevel level) {
        FeralParasiteEntity feral = switch (kind) {
            case HORSE -> ModEntities.FER_HORSE.get().create(level);
            case HUMAN -> ModEntities.FER_HUMAN.get().create(level);
            case VILLAGER -> ModEntities.FER_VILLAGER.get().create(level);
            case BIGSPIDER -> null;
        };
        if (feral == null) {
            return;
        }
        feral.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        feral.setTarget(getTarget());
        feral.setCustomName(getCustomName());
        feral.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            feral.setPersistenceRequired();
        }
        level.addFreshEntity(feral);
        discard();
    }

    private void transformToHost(ServerLevel level) {
        HostEntity host = ModEntities.HOST.get().create(level);
        if (host == null) {
            return;
        }
        host.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        host.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()),
                MobSpawnType.MOB_SUMMONED, null, null);
        host.setCustomName(getCustomName());
        host.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            host.setPersistenceRequired();
        }
        if (level.addFreshEntity(host)) {
            AssimilatedMeltSystem.sendMeltParticles(level, host);
            discard();
        }
    }

    private void tickMelting() {
        int ticks = entityData.get(MELT_TICKS) + 1;
        entityData.set(MELT_TICKS, ticks);
        if (ticks % 20 == 0) {
            playSound(ModSounds.SIM_ADVENTURER_MELT.get(), 1.0F, 1.0F);
        }
        if (level() instanceof ServerLevel serverLevel) {
            AssimilatedMeltSystem.sendMeltParticles(serverLevel, this);
        }
        if (getMeltHeight() > 0.7F && ticks < kind.meltDuration) {
            return;
        }
        AssimilatedMeltSystem.spawnMovingFlesh(this, kind.mergeValue);
    }

    private void spawnDeathBurst() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float radius = kind == Kind.HORSE ? 3.5F : 2.25F;
        float damage = kind == Kind.HORSE ? 15.0F : 5.0F;
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(radius));
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius), this::isValidParasiteTarget)) {
            target.hurt(damageSources().mobAttack(this), damage);
        }
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(radius);
        cloud.setDuration(160);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, false));
        cloud.addEffect(new MobEffectInstance(alku.csrp.registry.ModMobEffects.COTH.get(),
                200, 0, false, false, true));
        serverLevel.addFreshEntity(cloud);
        serverLevel.addFreshEntity(new ItemEntity(serverLevel, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                new ItemStack(ModItems.ASSIMILATED_FLESH.get())));
    }

    public enum Kind {
        BIGSPIDER("sim_bigspider", 22.0D, 3.0D, 9.0D, 0.5D, 0.27D, 32.0D, 10,
                1.0F, 0.0F, 0, 0),
        HORSE("sim_horse", 24.0D, 0.5D, 7.5D, 0.1D, 0.27D, 32.0D, 12,
                1.75F, 1.6F, 73, 1),
        HUMAN("sim_human", 15.0D, 5.0D, 9.0D, 0.1D, 0.23D, 32.0D, 10,
                1.95F, 1.95F, 127, 1),
        VILLAGER("sim_villager", 16.0D, 5.0D, 10.0D, 0.2D, 0.23D, 32.0D, 10,
                1.95F, 1.95F, 127, 1);

        private final String id;
        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final double knockbackResistance;
        private final double movementSpeed;
        private final double followRange;
        private final int experience;
        private final float baseHeight;
        private final float meltStartHeight;
        private final int meltDuration;
        private final int mergeValue;

        Kind(String id, double maxHealth, double armor, double attackDamage, double knockbackResistance,
             double movementSpeed, double followRange, int experience, float baseHeight,
             float meltStartHeight, int meltDuration, int mergeValue) {
            this.id = id;
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.knockbackResistance = knockbackResistance;
            this.movementSpeed = movementSpeed;
            this.followRange = followRange;
            this.experience = experience;
            this.baseHeight = baseHeight;
            this.meltStartHeight = meltStartHeight;
            this.meltDuration = meltDuration;
            this.mergeValue = mergeValue;
        }
    }
}
