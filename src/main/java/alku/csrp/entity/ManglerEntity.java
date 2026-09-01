package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.config.MobsConfig;
import alku.csrp.effect.EffectStacking;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelRawAnimation;

import java.util.EnumSet;

/** Original EntityNuuh, including its synchronized skin and combat-state animation routing. */
public final class ManglerEntity extends PrimitiveParasiteEntity implements ManualVariantProvider {
    private static final EntityDataAccessor<Byte> CLIMBING =
            SynchedEntityData.defineId(ManglerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(ManglerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> COMBAT_STATUS =
            SynchedEntityData.defineId(ManglerEntity.class, EntityDataSerializers.BYTE);

    public static final int NORMAL_VARIANT = 0;
    public static final int VIRAL_VARIANT = 5;
    public static final int BLEEDING_VARIANT = 6;
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_APPROACH = 1;
    private static final int STATUS_SPRINT = 2;
    private static final int STATUS_LEAP = 10;
    private static final int REGENERATION_TAG_DEFAULT = 1;
    private static final String REGENERATION_USES_TAG = "mangler_regeneration_uses";

    private final CitadelRawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final CitadelRawAnimation LIMB_SWING = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final CitadelRawAnimation AGE_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final CitadelRawAnimation LIMB_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final CitadelRawAnimation LIMB_STATUS_2 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final CitadelRawAnimation LEAP = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");

    private int regenerationUses = REGENERATION_TAG_DEFAULT;
    private boolean deathConversionHandled;

    public ManglerEntity(EntityType<? extends ManglerEntity> type, Level level) {
        super(type, level);
        xpReward = 75;
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return 8;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return 0.125F;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return 12;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return 0.95F;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return 0.30F;
    }

    @Override
    protected float damageAdaptationEffectiveness() {
        return 0.95F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 17.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.37)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, (byte) 0);
        builder.define(VARIANT, (byte) NORMAL_VARIANT);
        builder.define(COMBAT_STATUS, (byte) STATUS_IDLE);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new SwimmingDivingGoal());
        goalSelector.addGoal(1, new EvasiveDashGoal());
        goalSelector.addGoal(2, new SkillLeapGoal());
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(4, new FastMeleeAttackGoal());
    }

    @Override
    protected boolean isValidParasiteTarget(LivingEntity target) {
        return !(target instanceof WaterAnimal) && !(target instanceof Animal)
                && !(target instanceof Villager) && super.isValidParasiteTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            setClimbing(horizontalCollision && canClimbForTarget());
            tickRegeneration();
        }
    }

    private boolean canClimbForTarget() {
        LivingEntity target = getTarget();
        if (target == null) {
            return true;
        }
        if (!hasLineOfSight(target) && distanceToSqr(target) < 100.0D) {
            return false;
        }
        return target.getY() + 1.0D >= getY();
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level()) >= Config.alwaysVariantPhase()) {
            setVariant(random.nextBoolean() ? VIRAL_VARIANT : BLEEDING_VARIANT);
        }
        return data;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float healthBefore = target instanceof LivingEntity living
                ? living.getHealth() + living.getAbsorptionAmount() : 0.0F;
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            applyMinimumDamage(living, healthBefore);
        }
        return hurt;
    }

    @Override
    public void push(Entity entity) {
        if (!level().isClientSide && getVariant() == VIRAL_VARIANT && entity instanceof LivingEntity living
                && living != this && !(living instanceof Parasite)) {
            EffectStacking.apply(living, ModMobEffects.VIRAL, 100, 0);
        }
        super.push(entity);
    }

    @Override
    public boolean onClimbable() {
        return (entityData.get(CLIMBING) & 1) != 0;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return distance >= 200.0F && super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.get("small.step"), 0.3F, getVoicePitch());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getCombatStatus() == STATUS_IDLE
                ? ModSounds.get("nuuh.growl") : ModSounds.get("mob.silence");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("nuuh.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get("nuuh.death");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("variant", (byte) getVariant());
        tag.putInt(REGENERATION_USES_TAG, regenerationUses);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int storedVariant = tag.getInt("variant");
        if (storedVariant == 1) {
            storedVariant = VIRAL_VARIANT;
        } else if (storedVariant == 2) {
            storedVariant = BLEEDING_VARIANT;
        }
        setVariant(storedVariant == BLEEDING_VARIANT ? BLEEDING_VARIANT
                : storedVariant == VIRAL_VARIANT ? VIRAL_VARIANT : NORMAL_VARIANT);
        regenerationUses = Math.max(1, tag.contains(REGENERATION_USES_TAG)
                ? tag.getInt(REGENERATION_USES_TAG) : REGENERATION_TAG_DEFAULT);
    }

    @Override
    public void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers) {
        controllers.add(new CitadelAnimationController<>(this, "movement_controller", 4,
                state -> {
                    if (isSpecialLeapAnimating()) {
                        return state.setAndContinue(LEAP);
                    }
                    boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
                    return switch (getCombatStatus()) {
                        case STATUS_APPROACH -> state.setAndContinue(moving ? LIMB_STATUS_1 : AGE_STATUS_1);
                        case STATUS_SPRINT -> state.setAndContinue(moving ? LIMB_STATUS_2 : AGE_IN_TICKS);
                        case STATUS_LEAP -> state.setAndContinue(LEAP);
                        default -> state.setAndContinue(moving ? LIMB_SWING : AGE_IN_TICKS);
                    };
                }));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (!level().isClientSide && source.getEntity() instanceof Player player && player.isAlive()) {
            setLastHurtByMob(player);
            setTarget(player);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !deathConversionHandled && level() instanceof ServerLevel serverLevel
                && !SrpWorldData.get(serverLevel).colonies().isEmpty()) {
            deathConversionHandled = true;
            Mob rupter = ModEntities.RUPTER.get().create(serverLevel);
            if (rupter != null) {
                rupter.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                rupter.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                        MobSpawnType.MOB_SUMMONED, null);
                rupter.setCustomName(getCustomName());
                rupter.setCustomNameVisible(isCustomNameVisible());
                if (isPersistenceRequired()) {
                    rupter.setPersistenceRequired();
                }
                if (rupter instanceof PrimitiveParasiteEntity parasite) {
                    copyDamageAdaptationsTo(parasite);
                }
                if (isOnFire()) {
                    rupter.setHealth(rupter.getMaxHealth() * 0.5F);
                    rupter.setRemainingFireTicks(8 * 20);
                }
                if (serverLevel.addFreshEntity(rupter)) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + getBbHeight() * 0.5D,
                            getZ(), 10, 0.2D, 0.2D, 0.2D, 0.0D);
                    discard();
                    return;
                }
            }
        }
        super.die(source);
    }

    public int getVariant() {
        return entityData.get(VARIANT);
    }

    @Override
    public int getManualVariant() {
        return entityData.get(VARIANT);
    }

    @Override
    public void setManualVariant(int variant) {
        entityData.set(VARIANT, (byte) Math.clamp(variant, 0, getMaxManualVariants() - 1));
    }

    public int getCombatStatus() {
        return entityData.get(COMBAT_STATUS);
    }

    private void setVariant(int value) {
        entityData.set(VARIANT, (byte) value);
    }

    private void setCombatStatus(int status) {
        entityData.set(COMBAT_STATUS, (byte) status);
    }

    private void tickRegeneration() {
        if (tickCount % 21 != 10 || isOnFire() || !isAlive() || getParasiteKills() <= 1
                || getHealth() >= getMaxHealth()) {
            return;
        }
        heal(MobsConfig.manglerRegeneration());
        if (--regenerationUses <= 0) {
            consumeParasiteKill();
            regenerationUses = 3;
        }
    }

    private void applyMinimumDamage(LivingEntity target, float healthBefore) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel).minimumDamage()
                || target == this || !target.isAlive() || target instanceof Parasite
                || target instanceof Player player && player.getAbilities().invulnerable) {
            return;
        }
        float dealt = healthBefore - target.getHealth() - target.getAbsorptionAmount();
        float minimum = MobsConfig.manglerMinimumDamage();
        if (dealt >= minimum || minimum <= 0.0F) {
            return;
        }
        float remaining = minimum - Math.max(0.0F, dealt);
        float absorptionDamage = Math.min(target.getAbsorptionAmount(), remaining * 0.5F);
        if (absorptionDamage > 0.0F) {
            target.setAbsorptionAmount(target.getAbsorptionAmount() - absorptionDamage);
        }
        target.setHealth(Math.max(0.0F, target.getHealth() - (remaining - absorptionDamage)));
        serverLevel.broadcastEntityEvent(target, (byte) 2);
        if (target.getHealth() <= 0.0F) {
            target.die(damageSources().mobAttack(this));
        }
    }

    private void setClimbing(boolean climbing) {
        byte value = entityData.get(CLIMBING);
        entityData.set(CLIMBING, climbing ? (byte) (value | 1) : (byte) (value & -2));
    }

    private final class FastMeleeAttackGoal extends MeleeAttackGoal {
        private FastMeleeAttackGoal() {
            super(ManglerEntity.this, 1.3D, false);
        }

        @Override
        protected int getTicksUntilNextAttack() {
            return 6;
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity target = getTarget();
            if (target == null) {
                setCombatStatus(STATUS_IDLE);
                setSprinting(false);
                return;
            }
            boolean sprinting = level() instanceof ServerLevel serverLevel
                    && EvolutionSystem.generationProfile(serverLevel).sprinting();
            int status = sprinting && distanceToSqr(target) > 1.0D ? STATUS_SPRINT : STATUS_APPROACH;
            setCombatStatus(status);
            setSprinting(status == STATUS_SPRINT);
        }

        @Override
        public void stop() {
            super.stop();
            if (getCombatStatus() != STATUS_LEAP) {
                setCombatStatus(STATUS_IDLE);
                setSprinting(false);
            }
        }
    }

    private final class SkillLeapGoal extends Goal {
        private int chargeTicks;
        private int leapTicks;
        private boolean leaping;

        private SkillLeapGoal() {
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            return !leaping && canChargeLeap();
        }

        @Override
        public boolean canContinueToUse() {
            return leaping || canChargeLeap();
        }

        @Override
        public void tick() {
            if (leaping) {
                leapTicks++;
                if (leapTicks > 2 && onGround()) {
                    leaping = false;
                    chargeTicks = 0;
                    leapTicks = 0;
                    setCombatStatus(STATUS_IDLE);
                    setSprinting(false);
                }
                return;
            }
            LivingEntity target = getTarget();
            if (target != null && distanceToSqr(target) >= 25.0D && distanceToSqr(target) < 10_000.0D
                    && hasLineOfSight(target) && !hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                    && ++chargeTicks >= 20 && onGround()) {
                Vec3 direction = new Vec3(target.getX() - getX(), 0.0D, target.getZ() - getZ());
                if (direction.lengthSqr() <= 0.001D) {
                    return;
                }
                direction = direction.normalize();
                Vec3 movement = getDeltaMovement();
                setDeltaMovement(movement.x + direction.x * 2.0D * 0.9D + movement.x * 0.3D,
                        0.8D,
                        movement.z + direction.z * 2.0D * 0.9D + movement.z * 0.3D);
                hasImpulse = true;
                navigation.stop();
                setCombatStatus(STATUS_LEAP);
                setSprinting(false);
                startSpecialLeapAnimation(20);
                leaping = true;
                leapTicks = 0;
            }
        }

        @Override
        public void stop() {
            if (!leaping) {
                chargeTicks = 0;
            }
        }

        private boolean canChargeLeap() {
            LivingEntity target = getTarget();
            return target != null && getCombatStatus() > STATUS_IDLE && getCombatStatus() < STATUS_LEAP
                    && level() instanceof ServerLevel serverLevel
                    && EvolutionSystem.generationProfile(serverLevel).specialMoves();
        }
    }

    private final class SwimmingDivingGoal extends Goal {
        private SwimmingDivingGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
            getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            if (!isInWaterOrBubble()) {
                return false;
            }
            LivingEntity target = getTarget();
            if (target != null && target.isInWaterOrBubble()
                    && distanceToSqr(getX(), target.getY(), getZ()) < 25.0D
                    && target.getY() - getY() < -1.0D) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.12D, 0.0D));
                return false;
            }
            return true;
        }

        @Override
        public void tick() {
            if (random.nextFloat() < 0.8F) {
                getJumpControl().jump();
            }
        }
    }

    private final class EvasiveDashGoal extends Goal {
        private static final int DASH_COOLDOWN_TICKS = 10;
        private static final double MIN_DASH_DISTANCE_SQR = 1.0D;
        private static final double MAX_DASH_DISTANCE_SQR = 225.0D;
        private int cooldown;

        private EvasiveDashGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !onGround() || getCombatStatus() != STATUS_APPROACH
                    && getCombatStatus() != STATUS_SPRINT) {
                cooldown = 0;
                return false;
            }
            double distance = distanceToSqr(target);
            if (distance <= MIN_DASH_DISTANCE_SQR || distance >= MAX_DASH_DISTANCE_SQR
                    || !hasLineOfSight(target)) {
                return false;
            }
            return ++cooldown >= DASH_COOLDOWN_TICKS;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            cooldown = 0;
            if (target == null) {
                return;
            }
            Vec3 direction = new Vec3(target.getX() - getX(), 0.0D, target.getZ() - getZ());
            if (direction.lengthSqr() <= 0.001D) {
                return;
            }
            direction = direction.normalize();
            Vec3 movement = getDeltaMovement();
            double axisX = random.nextBoolean() ? 1.0D : 0.0D;
            double axisZ = axisX == 0.0D ? 1.0D : 0.0D;
            setDeltaMovement(movement.x * 0.2D + direction.x * 0.8D + axisX,
                    movement.y, movement.z * 0.2D + direction.z * 0.8D + axisZ);
            navigation.stop();
        }
    }
}
