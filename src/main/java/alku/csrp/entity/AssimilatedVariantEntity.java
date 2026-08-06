package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shared implementation for the remaining assimilated bodies.  Their body
 * forms retain the legacy fire weakness, COTH contact damage, head-on-death
 * transition, and explosive remains burst.
 */
public final class AssimilatedVariantEntity extends Monster implements GeoEntity, Parasite {
    private static final EntityDataAccessor<Boolean> SPIDER_AIMING = SynchedEntityData.defineId(
            AssimilatedVariantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final float HEAD_SPAWN_CHANCE = 0.5F;
    private static final float EXPLOSION_CHANCE = 0.25F;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation SPIDER_AIM_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation SPIDER_AIM_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_1");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Kind kind;
    private int parasiteKills;
    private int rangedCooldown;

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPIDER_AIMING, false);
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
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (rangedCooldown > 0) {
            rangedCooldown--;
        }
        if (kind == Kind.BIGSPIDER) {
            LivingEntity target = getTarget();
            entityData.set(SPIDER_AIMING, target != null && target.isAlive());
        }
        if (tickCount % 20 == 0) {
            infectNearby();
        }
        if (kind == Kind.BIGSPIDER && rangedCooldown <= 0 && getTarget() != null && hasLineOfSight(getTarget())) {
            fireWebBall(getTarget());
            rangedCooldown = 60;
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        LivingEntity livingTarget = entity instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(entity);
        if (hit && livingTarget != null) {
            triggerAnim("attack_controller", "attack");
            ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
            InfectionMechanics.applyCoth(livingTarget, this);
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
        parasiteKills++;
        if (parasiteKills > AssimilatedParasiteEntity.FERAL_KILL_THRESHOLD) {
            transformToFeral(level);
        }
        return super.killedEntity(level, victim);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasite_kills", parasiteKills);
        tag.putInt("ranged_cooldown", rangedCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        parasiteKills = tag.getInt("parasite_kills");
        rangedCooldown = tag.getInt("ranged_cooldown");
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
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (kind == Kind.BIGSPIDER && entityData.get(SPIDER_AIMING)) {
                return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.0001 ? SPIDER_AIM_WALK : SPIDER_AIM_IDLE);
            }
            if (getDeltaMovement().horizontalDistanceSqr() < 0.0001) {
                return state.setAndContinue(IDLE);
            }
            return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.055D ? RUN : WALK);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
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
                MobSpawnType.MOB_SUMMONED, null);
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
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(radius);
        cloud.setDuration(160);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, false));
        cloud.addEffect(new MobEffectInstance(alku.csrp.registry.ModMobEffects.COTH, 200, 0, false, false));
        serverLevel.addFreshEntity(cloud);
        serverLevel.addFreshEntity(new ItemEntity(serverLevel, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                new ItemStack(ModItems.ASSIMILATED_FLESH.get())));
    }

    public enum Kind {
        BIGSPIDER("sim_bigspider", 22.0D, 3.0D, 9.0D, 0.5D, 0.27D, 32.0D, 10),
        HORSE("sim_horse", 24.0D, 0.5D, 7.5D, 0.1D, 0.27D, 32.0D, 12),
        HUMAN("sim_human", 15.0D, 5.0D, 9.0D, 0.1D, 0.23D, 32.0D, 10),
        VILLAGER("sim_villager", 16.0D, 5.0D, 10.0D, 0.2D, 0.23D, 32.0D, 10);

        private final String id;
        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final double knockbackResistance;
        private final double movementSpeed;
        private final double followRange;
        private final int experience;

        Kind(String id, double maxHealth, double armor, double attackDamage, double knockbackResistance,
             double movementSpeed, double followRange, int experience) {
            this.id = id;
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.knockbackResistance = knockbackResistance;
            this.movementSpeed = movementSpeed;
            this.followRange = followRange;
            this.experience = experience;
        }
    }
}
