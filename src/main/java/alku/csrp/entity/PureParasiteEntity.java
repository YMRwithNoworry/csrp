package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

/**
 * Shared port of the original Pure-tier combatants. They retain the legacy
 * fire weakness and adaptive resistance while their enum branches implement
 * the individual melee, flying, summoning, and ranged roles.
 */
public final class PureParasiteEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Boolean> WARDEN_CHARGING = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> VIGILANTE_STATUS = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VIGILANTE_LEFT_TENDRIL = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VIGILANTE_RIGHT_TENDRIL = SynchedEntityData.defineId(
            PureParasiteEntity.class, EntityDataSerializers.FLOAT);
    private static final int MAX_ADAPTATION_HITS = 8;
    private static final int MAX_LEARNABLE_DAMAGE_SOURCES = 12;
    private static final float ADAPTATION_PER_HIT = 0.125F;
    private static final float ADAPTATION_LEARN_CHANCE = 0.95F;
    private static final float FIRE_SUPPRESSION_CHANCE = 0.30F;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation RUN = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation FLY = IDLE;
    private final RawAnimation WARDEN_ATTACK = ParasiteAnimations.play(this, "get_attack_timer");
    private final RawAnimation WARDEN_AGE_STILL = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_still_ani_1");
    private final RawAnimation WARDEN_ATTACK_STILL = ParasiteAnimations.play(this,
            "get_attack_timer.get_still_ani_1");
    private final RawAnimation WARDEN_AGE_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation WARDEN_LIMB_STATUS_1 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation WARDEN_ATTACK_STATUS_1 = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_1");
    private final RawAnimation WARDEN_AGE_STATUS_1_STILL = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation WARDEN_ATTACK_STATUS_1_STILL = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation WARDEN_LIMB_STATUS_2 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation WARDEN_AGE_STATUS_3 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3");
    private final RawAnimation WARDEN_LIMB_STATUS_3 = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_3");
    private final RawAnimation WARDEN_ATTACK_STATUS_3 = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_3");
    private final RawAnimation WARDEN_AGE_STATUS_3_STILL = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1");
    private final RawAnimation WARDEN_ATTACK_STATUS_3_STILL = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_3.get_still_ani_1");
    private final RawAnimation WARDEN_AGE_STATUS_10 = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");
    private final RawAnimation WARDEN_ATTACK_STATUS_10 = ParasiteAnimations.play(this,
            "get_attack_timer.get_parasite_status_10");
    private final RawAnimation WARDEN_CHARGE_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3");
    private final RawAnimation WARDEN_CHARGE_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_3");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");
    private final RawAnimation VIGILANTE_ATTACK_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation VIGILANTE_ATTACK_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation VIGILANTE_ATTACK2_IDLE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation VIGILANTE_ATTACK2_WALK = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation VIGILANTE_UNDERGROUND = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_25");

    private final Kind kind;
    private final VigilanteTendrilPart leftTendrilPart;
    private final VigilanteTendrilPart rightTendrilPart;
    private final PartEntity<?>[] bodyParts;
    private int blockBreakCooldown;
    private int supportCooldown;
    private int attackAnimationTicks;
    private int scentCooldown = 800;
    private int seekerCreationPhase = -1;
    private boolean deathBurstFired;

    public PureParasiteEntity(EntityType<? extends PureParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        if (kind == Kind.VIGILANTE) {
            leftTendrilPart = new VigilanteTendrilPart(this, true);
            rightTendrilPart = new VigilanteTendrilPart(this, false);
            bodyParts = new PartEntity<?>[]{leftTendrilPart, rightTendrilPart};
        } else {
            leftTendrilPart = null;
            rightTendrilPart = null;
            bodyParts = new PartEntity<?>[0];
        }
        xpReward = 75;
        if (kind.flying) {
            moveControl = new FlyingMoveControl(this, 16, true);
            setNoGravity(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        AttributeSupplier.Builder attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind.knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, kind.followRange);
        if (kind.flying) {
            attributes.add(Attributes.FLYING_SPEED, kind.movementSpeed);
        }
        return attributes;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        switch (activeKind()) {
            case GRUNT -> {
                goalSelector.addGoal(1, createAnimatedLeapGoal(0.65F, 24));
                goalSelector.addGoal(2, new EvasiveDashGoal(80, 0.70D));
                goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25D, false));
            }
            case BOMBER_LIGHT -> {
                goalSelector.addGoal(1, new LightBomberBombGoal());
                goalSelector.addGoal(3, new FlightPursuitGoal(1.00D));
            }
            case MONARCH -> {
                goalSelector.addGoal(1, new MonarchWebGoal());
                goalSelector.addGoal(2, new MonarchLeapGoal());
                goalSelector.addGoal(3, new MonarchChargeGoal());
                goalSelector.addGoal(4, new EvasiveDashGoal(100, 0.75D));
                goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.20D, false));
            }
            case OVERSEER -> {
                goalSelector.addGoal(1, new OverseerVolleyGoal());
                goalSelector.addGoal(2, new OverseerSummonGoal());
                goalSelector.addGoal(3, new FlightPursuitGoal(0.95D));
            }
            case SEEKER -> {
                goalSelector.addGoal(3, new FlightPursuitGoal(0.50D));
                goalSelector.addGoal(6, new SeekerRandomFlightGoal());
            }
            case VIGILANTE -> {
                goalSelector.addGoal(1, new VigilanteRangedGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.90D, false));
            }
            case WARDEN -> {
                goalSelector.addGoal(1, new WardenShockwaveGoal());
                goalSelector.addGoal(2, new WardenChargeGoal());
                goalSelector.addGoal(3, createAnimatedLeapGoal(0.75F, 30));
                goalSelector.addGoal(4, new EvasiveDashGoal(100, 0.70D));
                goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.10D, false));
            }
        }
    }

    @Override
    protected boolean usesDefaultMovementGoals() {
        return !activeKind().flying;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WARDEN_CHARGING, false);
        builder.define(VIGILANTE_STATUS, 0);
        builder.define(VIGILANTE_LEFT_TENDRIL, -1.0F);
        builder.define(VIGILANTE_RIGHT_TENDRIL, -1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        Kind activeKind = activeKind();
        if (activeKind.flying) {
            setNoGravity(true);
        }
        updateVigilanteParts();
        if (level().isClientSide) {
            return;
        }
        if (activeKind == Kind.VIGILANTE) {
            initializeVigilanteTendrils();
        }
        if (blockBreakCooldown > 0) {
            blockBreakCooldown--;
        }
        if (supportCooldown > 0) {
            supportCooldown--;
        }
        if (attackAnimationTicks > 0) {
            attackAnimationTicks--;
        }
        if (activeKind.flying && onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 4.0D, getZ(), 0.55D);
        }
        if (activeKind == Kind.SEEKER) {
            tickSeekerScent();
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        breakBlocksTowardsTarget(target, activeKind);
        if (activeKind != Kind.SEEKER && supportCooldown <= 0 && tickCount % 40 == 0) {
            trySummonSupport(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 4.0F;
        }
        boolean hurt = super.hurt(source, amount);
        return hurt;
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return MAX_ADAPTATION_HITS;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return ADAPTATION_PER_HIT;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return MAX_LEARNABLE_DAMAGE_SOURCES;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return ADAPTATION_LEARN_CHANCE;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return FIRE_SUPPRESSION_CHANCE;
    }

    @Override
    protected float damageAdaptationEffectiveness() {
        return switch (activeKind()) {
            case GRUNT, BOMBER_LIGHT, OVERSEER, WARDEN -> 0.95F;
            default -> 1.0F;
        };
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (!(entity instanceof LivingEntity target)) {
            return super.doHurtTarget(entity);
        }
        return switch (activeKind()) {
            case GRUNT, MONARCH, WARDEN -> performAreaMelee(target);
            default -> {
                boolean hurt = super.doHurtTarget(target);
                if (hurt) {
                    attackAnimationTicks = 8;
                    triggerAttackAnimation();
                    applyMeleeEffects(target, activeKind());
                }
                yield hurt;
            }
        };
    }

    @Override
    public boolean onClimbable() {
        return activeKind().climbs && horizontalCollision || super.onClimbable();
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !deathBurstFired && random.nextFloat() < 0.25F) {
            deathBurstFired = true;
            triggerPureDeathBurst();
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (activeKind() == Kind.VIGILANTE) {
            tag.putInt("VigilanteStatus", entityData.get(VIGILANTE_STATUS));
            tag.putFloat("VigilanteLeftTendril", entityData.get(VIGILANTE_LEFT_TENDRIL));
            tag.putFloat("VigilanteRightTendril", entityData.get(VIGILANTE_RIGHT_TENDRIL));
        }
        if (activeKind() == Kind.SEEKER) {
            tag.putInt("SeekerCreationPhase", seekerCreationPhase);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (activeKind() == Kind.VIGILANTE && tag.contains("VigilanteStatus")) {
            entityData.set(VIGILANTE_STATUS, tag.getInt("VigilanteStatus"));
            entityData.set(VIGILANTE_LEFT_TENDRIL, tag.contains("VigilanteLeftTendril")
                    ? tag.getFloat("VigilanteLeftTendril") : -1.0F);
            entityData.set(VIGILANTE_RIGHT_TENDRIL, tag.contains("VigilanteRightTendril")
                    ? tag.getFloat("VigilanteRightTendril") : -1.0F);
        }
        if (activeKind() == Kind.SEEKER) {
            seekerCreationPhase = tag.contains("SeekerCreationPhase")
                    ? tag.getInt("SeekerCreationPhase") : -1;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        if (activeKind() == Kind.WARDEN) {
            controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                    .triggerableAnim("get_attack_timer", WARDEN_ATTACK));
        }
    }

    public Kind getKind() {
        return activeKind();
    }

    @Override
    public boolean isMultipartEntity() {
        return bodyParts.length > 0;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int index = 0; index < bodyParts.length; index++) {
            bodyParts[index].setId(id + index + 1);
        }
    }

    @Override
    public PartEntity<?>[] getParts() {
        return bodyParts;
    }

    public boolean isLeftVigilanteTendrilAttached() {
        return activeKind() != Kind.VIGILANTE || entityData.get(VIGILANTE_LEFT_TENDRIL) != 0.0F;
    }

    public boolean isRightVigilanteTendrilAttached() {
        return activeKind() != Kind.VIGILANTE || entityData.get(VIGILANTE_RIGHT_TENDRIL) != 0.0F;
    }

    private void initializeVigilanteTendrils() {
        float health = getMaxHealth() * 0.4F;
        if (entityData.get(VIGILANTE_LEFT_TENDRIL) < 0.0F) {
            entityData.set(VIGILANTE_LEFT_TENDRIL, health);
        }
        if (entityData.get(VIGILANTE_RIGHT_TENDRIL) < 0.0F) {
            entityData.set(VIGILANTE_RIGHT_TENDRIL, health);
        }
    }

    private boolean hurtVigilanteTendril(boolean left, DamageSource source, float amount) {
        if (!hurt(source, amount)) {
            return false;
        }
        EntityDataAccessor<Float> data = left ? VIGILANTE_LEFT_TENDRIL : VIGILANTE_RIGHT_TENDRIL;
        float previous = entityData.get(data);
        if (previous <= 0.0F) {
            return false;
        }
        float remaining = Math.max(0.0F, previous - amount);
        entityData.set(data, remaining);
        if (remaining == 0.0F) {
            spawnVigilanteTendril(left);
            reduceAllResistances(Math.max(1, maxDamageAdaptationHits() / 2));
            playSound(ModSounds.get("mob.tendril"), 2.0F, 0.8F);
        }
        return true;
    }

    private void spawnVigilanteTendril(boolean left) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        TendrilEntity tendril = ModEntities.TENDRIL.get().create(serverLevel);
        if (tendril == null) {
            return;
        }
        double side = left ? 1.0D : -1.0D;
        double yaw = Math.toRadians(getYRot());
        tendril.setSkin(TendrilEntity.ANGED);
        tendril.moveTo(getX() + side * Math.cos(yaw) * 1.1D,
                getY() + 2.3D,
                getZ() + side * Math.sin(yaw) * 1.1D,
                getYRot(), 0.0F);
        serverLevel.addFreshEntity(tendril);
    }

    private void updateVigilanteParts() {
        if (leftTendrilPart != null) {
            leftTendrilPart.updatePosition();
        }
        if (rightTendrilPart != null) {
            rightTendrilPart.updatePosition();
        }
    }

    public int getVigilanteStatus() {
        return entityData.get(VIGILANTE_STATUS);
    }

    public void setVigilanteStatus(int status) {
        entityData.set(VIGILANTE_STATUS, status);
    }

    private PlayState movementAnimation(AnimationState<PureParasiteEntity> state) {
        if (isSpecialLeapAnimating()
                && (activeKind() == Kind.GRUNT || activeKind() == Kind.MONARCH || activeKind() == Kind.WARDEN)) {
            return state.setAndContinue(LEAP);
        }
        if (activeKind() == Kind.WARDEN && entityData.get(WARDEN_CHARGING)) {
            return state.setAndContinue(ParasiteAnimations.isMoving(this, state.isMoving()) ? WARDEN_CHARGE_WALK : WARDEN_CHARGE_IDLE);
        }
        if (activeKind() == Kind.VIGILANTE) {
            int status = entityData.get(VIGILANTE_STATUS);
            boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
            return switch (status) {
                case 1 -> state.setAndContinue(moving ? VIGILANTE_ATTACK_WALK : VIGILANTE_ATTACK_IDLE);
                case 2 -> state.setAndContinue(moving ? VIGILANTE_ATTACK2_WALK : VIGILANTE_ATTACK2_IDLE);
                case 25 -> state.setAndContinue(VIGILANTE_UNDERGROUND);
                default -> state.setAndContinue(moving ? (getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK) : IDLE);
            };
        }
        if (activeKind() == Kind.BOMBER_LIGHT || activeKind() == Kind.OVERSEER
                || activeKind() == Kind.SEEKER) {
            return state.setAndContinue(FLY);
        }
        if (activeKind() == Kind.GRUNT) {
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) return state.setAndContinue(IDLE);
            return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : VIGILANTE_ATTACK_WALK);
        }
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(IDLE);
        }
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK);
    }

    private boolean performAreaMelee(LivingEntity center) {
        double radius = activeKind() == Kind.WARDEN ? 2.6D : 2.0D;
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), center.getBoundingBox().inflate(radius));
        boolean hit = false;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(radius), this::isValidParasiteTarget)) {
            if (!super.doHurtTarget(target)) {
                continue;
            }
            hit = true;
            applyMeleeEffects(target, activeKind());
        }
        if (hit) {
            attackAnimationTicks = 10;
            triggerAttackAnimation();
        }
        return hit;
    }

    private void triggerAttackAnimation() {
        attackAnimationTicks = 10;
        if (activeKind() == Kind.WARDEN) {
            triggerAnim("attack_controller", "get_attack_timer");
        }
    }

    private void applyMeleeEffects(LivingEntity target, Kind activeKind) {
        if (random.nextFloat() < 0.40F) {
            target.addEffect(new MobEffectInstance(ModMobEffects.COTH, 180, 0, false, false), this);
        }
        switch (activeKind) {
            case GRUNT -> {
                // Grunt variants add bleeding or viral effects in the legacy mod; the base form keeps the shared COTH hit.
            }
            case MONARCH -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, false), this);
            case VIGILANTE -> pushAway(target, 0.45D, 0.20D);
            case WARDEN -> pushAway(target, 0.70D, 0.55D);
            default -> {
            }
        }
    }

    private void pushAway(LivingEntity target, double horizontal, double vertical) {
        Vec3 direction = target.position().subtract(position());
        double length = Math.max(0.001D, direction.horizontalDistance());
        target.push(direction.x / length * horizontal, vertical, direction.z / length * horizontal);
    }

    private void breakBlocksTowardsTarget(LivingEntity target, Kind activeKind) {
        if (activeKind.blockHardness <= 0.0F || blockBreakCooldown > 0
                || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.001D) {
            return;
        }
        horizontal = horizontal.normalize();
        BlockPos origin = BlockPos.containing(getX() + horizontal.x * activeKind.blockRange,
                getY() + getBbHeight() * 0.5D, getZ() + horizontal.z * activeKind.blockRange);
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || hardness < 0.0F || hardness > activeKind.blockHardness) {
                continue;
            }
            if (ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this)) {
                blockBreakCooldown = 20;
            }
            return;
        }
    }

    private void trySummonSupport(LivingEntity target) {
        supportCooldown = 160;
        if (!(level() instanceof ServerLevel serverLevel) || random.nextInt(4) != 0) {
            return;
        }
        int seizers = level().getEntitiesOfClass(DeterrentParasiteEntity.class, getBoundingBox().inflate(32.0D),
                        entity -> entity.getKind() == DeterrentParasiteEntity.Kind.SEIZER)
                .size();
        if (seizers < 3 && random.nextBoolean()) {
            DeterrentParasiteEntity seizer = ModEntities.SEIZER.get().create(serverLevel);
            if (seizer == null) {
                return;
            }
            double angle = random.nextDouble() * Math.PI * 2.0D;
            seizer.moveTo(target.getX() + Math.cos(angle) * 3.0D, target.getY(),
                    target.getZ() + Math.sin(angle) * 3.0D, getYRot(), 0.0F);
            seizer.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(seizer.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            seizer.setTarget(target);
            serverLevel.addFreshEntity(seizer);
            return;
        }
        if (!hasLineOfSight(target) && distanceToSqr(target) > 64.0D) {
            DeterrentParasiteEntity dispatcher = ModEntities.DISPATCHERTEN.get().create(serverLevel);
            if (dispatcher == null) {
                return;
            }
            dispatcher.moveTo(target.getX(), target.getY(), target.getZ(), getYRot(), 0.0F);
            dispatcher.setDispatchTarget(this);
            dispatcher.setLifetimeTicks(0);
            serverLevel.addFreshEntity(dispatcher);
        }
    }

    private void tickSeekerScent() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (seekerCreationPhase < 0) {
            seekerCreationPhase = Config.evolutionPhase(serverLevel);
        }
        if (--scentCooldown >= 0 || tickCount % 21 != 10 || !Config.scentEnabled()
                || seekerCreationPhase < Config.scentDevelopmentLevel()
                || serverLevel.getEntities(ModEntities.SCENT.get(), scent -> true).size()
                        > Config.scentCap()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        ParasiticScentEntity scent = ModEntities.SCENT.get().create(serverLevel);
        if (scent == null) {
            return;
        }
        scent.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        scent.setTargetToKill(target, false);
        scent.setDieAfterKilling(true);
        scent.setCanFollow(true);
        serverLevel.addFreshEntity(scent);
        scentCooldown = 800;
    }

    private final class SeekerRandomFlightGoal extends Goal {
        private SeekerRandomFlightGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!getMoveControl().hasWanted()) {
                return true;
            }
            double x = getMoveControl().getWantedX() - getX();
            double y = getMoveControl().getWantedY() - getY();
            double z = getMoveControl().getWantedZ() - getZ();
            double distance = x * x + y * y + z * z;
            return distance < 1.0D || distance > 3600.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                getMoveControl().setWantedPosition(
                        getX() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F,
                        getY() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F,
                        getZ() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F, 0.5D);
                return;
            }

            BlockPos center = blockPosition();
            int mode = 1;
            double speed = 0.11D;
            double distance = distanceToSqr(target);
            if (distance > 400.0D) {
                center = target.blockPosition();
                mode = 2;
                speed += 0.11D;
            } else if (distance < 100.0D) {
                center = target.blockPosition();
                mode = 3;
                speed += 0.11D;
            }

            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos destination = switch (mode) {
                    case 2 -> center.offset(random.nextInt(6) - 2,
                            random.nextInt(7) - 2, random.nextInt(6) - 2);
                    case 3 -> center.offset(random.nextInt(4) + 3,
                            random.nextInt(5) + 4, random.nextInt(4) + 3);
                    default -> center.offset(random.nextInt(15) - 7,
                            random.nextInt(9) - 5, random.nextInt(15) - 7);
                };
                if (level().isEmptyBlock(destination)) {
                    getMoveControl().setWantedPosition(destination.getX() + 0.5D,
                            destination.getY() + 0.5D, destination.getZ() + 0.5D, speed);
                    return;
                }
            }
        }
    }

    private void triggerPureDeathBurst() {
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(2.0D));
        Level.ExplosionInteraction interaction = level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY() + getBbHeight() * 0.5D, getZ(), 2.0F, interaction);
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(3.0F);
        cloud.setDuration(80);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 220, 0, false, true));
        level().addFreshEntity(cloud);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    12, 0.75D, 0.75D, 0.75D, 0.02D);
        }
    }

    private void fireProjectile(LivingEntity target, ParasiteProjectileEntity.Mode mode, double speed,
                                float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(), mode);
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.55D));
        projectile.configure(this, mode, start, target.getEyePosition(), speed, damage, radius, lifetime, target);
        level().addFreshEntity(projectile);
    }

    private void fireWebProjectile(LivingEntity target, int webKind) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(), ParasiteProjectileEntity.Mode.WEB);
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.55D));
        projectile.configure(this, ParasiteProjectileEntity.Mode.WEB, start,
                target.getEyePosition(), 0.95D, 8.0F, 1.0D, 80, target);
        projectile.setWebKind(webKind);
        level().addFreshEntity(projectile);
    }

    private void fireBomb(LivingEntity target) {
        BombEntity bomb = ModEntities.BOMB.get().create(level());
        if (bomb == null) {
            return;
        }
        bomb.configure(this, 80, 1.0F, MobsConfig.ombooBombDamage(), 4, 0,
                MobsConfig.ombooGriefing());
        bomb.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        level().addFreshEntity(bomb);
    }

    private void spawnBuglins(LivingEntity target, int count) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int index = 0; index < count; index++) {
            BuglinEntity buglin = ModEntities.BUGLIN.get().create(serverLevel);
            if (buglin == null) {
                continue;
            }
            double angle = Math.PI * 2.0D * index / Math.max(1, count);
            buglin.moveTo(getX() + Math.cos(angle) * 1.5D, getY() + 0.2D,
                    getZ() + Math.sin(angle) * 1.5D, getYRot(), 0.0F);
            buglin.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(buglin.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            buglin.setTarget(target);
            serverLevel.addFreshEntity(buglin);
        }
    }

    private void spawnOverseerMinion(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Mob minion = random.nextFloat() < 0.66F
                ? ModEntities.GRUNT.get().create(serverLevel)
                : ModEntities.RUPTER.get().create(serverLevel);
        if (minion == null) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        minion.moveTo(target.getX() + Math.cos(angle) * 2.0D, target.getY(),
                target.getZ() + Math.sin(angle) * 2.0D, getYRot(), 0.0F);
        minion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(minion.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        minion.setTarget(target);
        minion.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 1, false, false), this);
        serverLevel.addFreshEntity(minion);
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        EntityType<?> type = getType();
        if (type == ModEntities.BOMBER_LIGHT.get()) return Kind.BOMBER_LIGHT;
        if (type == ModEntities.MONARCH.get()) return Kind.MONARCH;
        if (type == ModEntities.OVERSEER.get()) return Kind.OVERSEER;
        if (type == ModEntities.SEEKER.get()) return Kind.SEEKER;
        if (type == ModEntities.VIGILANTE.get()) return Kind.VIGILANTE;
        if (type == ModEntities.WARDEN.get()) return Kind.WARDEN;
        return Kind.GRUNT;
    }

    private final class EvasiveDashGoal extends Goal {
        private final int interval;
        private final double speed;
        private int cooldown;

        private EvasiveDashGoal(int interval, double speed) {
            this.interval = interval;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && distanceToSqr(target) >= 9.0D
                    && distanceToSqr(target) <= 196.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            Vec3 toTarget = target.position().subtract(position());
            Vec3 strafe = new Vec3(-toTarget.z, 0.0D, toTarget.x);
            if (strafe.lengthSqr() > 0.001D) {
                strafe = strafe.normalize().scale(random.nextBoolean() ? speed : -speed);
                setDeltaMovement(strafe.x, 0.25D, strafe.z);
            }
            cooldown = interval;
        }
    }

    private final class MonarchWebGoal extends Goal {
        private int cooldown;

        private MonarchWebGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && hasLineOfSight(target) && distanceToSqr(target) >= 16.0D
                    && distanceToSqr(target) <= 400.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                fireWebProjectile(target, 1);
                triggerAttackAnimation();
                cooldown = 70;
            }
        }
    }

    private final class MonarchLeapGoal extends Goal {
        private int cooldown;

        private MonarchLeapGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && distanceToSqr(target) >= 25.0D
                    && distanceToSqr(target) <= 196.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            leapTowards(target, 0.75D, 0.62D);
            startSpecialLeapAnimation(30);
            spawnBuglins(target, 5);
            triggerAttackAnimation();
            cooldown = 220;
        }
    }

    private final class MonarchChargeGoal extends Goal {
        private int cooldown;

        private MonarchChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && distanceToSqr(target) >= 9.0D
                    && distanceToSqr(target) <= 100.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            leapTowards(target, 1.05D, 0.18D);
            spawnBuglins(target, 3);
            triggerAttackAnimation();
            cooldown = 180;
        }
    }

    private void leapTowards(LivingEntity target, double horizontalSpeed, double verticalSpeed) {
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() <= 0.001D) {
            return;
        }
        horizontal = horizontal.normalize().scale(horizontalSpeed);
        setDeltaMovement(horizontal.x, verticalSpeed, horizontal.z);
    }

    private final class FlightPursuitGoal extends Goal {
        private final double speed;
        private int contactCooldown;

        private FlightPursuitGoal(double speed) {
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double heightOffset = activeKind() == Kind.OVERSEER ? 2.5D : 4.0D;
            getMoveControl().setWantedPosition(target.getX(), target.getY() + heightOffset, target.getZ(), speed);
            if (contactCooldown > 0) {
                contactCooldown--;
            } else if (distanceToSqr(target) <= 9.0D) {
                doHurtTarget(target);
                contactCooldown = 20;
            }
        }
    }

    private final class LightBomberBombGoal extends Goal {
        private int cooldown;

        private LightBomberBombGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.onGround() && hasLineOfSight(target)
                    && distanceToSqr(target) <= 625.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                fireBomb(target);
                triggerAttackAnimation();
                cooldown = 80;
            }
        }
    }

    private final class OverseerVolleyGoal extends Goal {
        private int cooldown;
        private int warmup;
        private int shots;
        private int shotDelay;

        private OverseerVolleyGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && hasLineOfSight(target) && distanceToSqr(target) <= 1024.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && shots < 6;
        }

        @Override
        public void start() {
            warmup = 10;
            shots = 0;
            shotDelay = 0;
            getNavigation().stop();
            triggerAttackAnimation();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (warmup > 0) {
                warmup--;
                return;
            }
            if (shotDelay > 0) {
                shotDelay--;
                return;
            }
            fireProjectile(target, ParasiteProjectileEntity.Mode.NEEDLE, 0.90D, 30.0F, 1.6D, 70);
            shots++;
            shotDelay = 4;
        }

        @Override
        public void stop() {
            cooldown = 160;
        }
    }

    private final class OverseerSummonGoal extends Goal {
        private int cooldown;
        private int chargeTicks;

        private OverseerSummonGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.onGround() && distanceToSqr(target) <= 1024.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return chargeTicks < 60 && getTarget() != null;
        }

        @Override
        public void start() {
            chargeTicks = 0;
            getNavigation().stop();
            triggerAttackAnimation();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            chargeTicks++;
            if (chargeTicks % 20 == 0) {
                spawnOverseerMinion(target);
                level().addParticle(ParticleTypes.WITCH, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        0.0D, 0.05D, 0.0D);
            }
        }

        @Override
        public void stop() {
            cooldown = 200;
        }
    }

    private final class VigilanteRangedGoal extends Goal {
        private int cooldown;
        private int shots;
        private int shotDelay;
        private int warmupTicks;

        private VigilanteRangedGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && hasLineOfSight(target) && distanceToSqr(target) >= 36.0D
                    && distanceToSqr(target) <= 1024.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && shots < 3;
        }

        @Override
        public void start() {
            shots = 0;
            shotDelay = 0;
            warmupTicks = 0;
            getNavigation().stop();
            triggerAttackAnimation();
            entityData.set(VIGILANTE_STATUS, 1);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (warmupTicks < 10) {
                warmupTicks++;
                return;
            }
            if (shotDelay > 0) {
                shotDelay--;
                return;
            }
            fireProjectile(target, ParasiteProjectileEntity.Mode.ACID, 0.80D, 27.0F, 2.25D, 90);
            shots++;
            shotDelay = 8;
            if (shots >= 3) {
                entityData.set(VIGILANTE_STATUS, 2);
            }
        }

        @Override
        public void stop() {
            cooldown = 80;
            entityData.set(VIGILANTE_STATUS, 0);
        }
    }

    private final class WardenChargeGoal extends Goal {
        private int cooldown;
        private int chargeTicks;
        private int dashTicks;

        private WardenChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && distanceToSqr(target) >= 25.0D
                    && distanceToSqr(target) <= 225.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && dashTicks < 18;
        }

        @Override
        public void start() {
            chargeTicks = 0;
            dashTicks = 0;
            getNavigation().stop();
            entityData.set(WARDEN_CHARGING, true);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (chargeTicks < 20) {
                chargeTicks++;
                getNavigation().stop();
                level().addParticle(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        0.0D, 0.03D, 0.0D);
                return;
            }
            Vec3 direction = target.position().subtract(position());
            Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
            if (horizontal.lengthSqr() > 0.001D) {
                horizontal = horizontal.normalize().scale(1.10D);
                setDeltaMovement(horizontal.x, 0.12D, horizontal.z);
            }
            dashTicks++;
            DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(1.5D));
            for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(1.5D), PureParasiteEntity.this::isValidParasiteTarget)) {
                if (victim.hurt(damageSources().mobAttack(PureParasiteEntity.this),
                        (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25F)) {
                    pushAway(victim, 1.10D, 0.85D);
                }
            }
        }

        @Override
        public void stop() {
            cooldown = 220;
            entityData.set(WARDEN_CHARGING, false);
        }
    }

    private final class WardenShockwaveGoal extends Goal {
        private int cooldown;
        private int chargeTicks;

        private WardenShockwaveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && hasLineOfSight(target) && distanceToSqr(target) >= 36.0D
                    && distanceToSqr(target) <= 400.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return chargeTicks < 40 && getTarget() != null;
        }

        @Override
        public void start() {
            chargeTicks = 0;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            getNavigation().stop();
            chargeTicks++;
            if (chargeTicks < 20) {
                level().addParticle(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        0.0D, 0.04D, 0.0D);
        } else if (chargeTicks == 20) {
                fireShockwave(target);
                triggerAttackAnimation();
            }
        }

        @Override
        public void stop() {
            cooldown = 240;
        }
    }

    private void fireShockwave(LivingEntity target) {
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() <= 0.001D) {
            return;
        }
        horizontal = horizontal.normalize();
        AABB shockwave = getBoundingBox().expandTowards(horizontal.scale(14.0D)).inflate(1.35D, 1.5D, 1.35D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), shockwave);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, shockwave,
                this::isValidParasiteTarget)) {
            if (victim.hurt(damageSources().mobAttack(this),
                    (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.80F)) {
                pushAway(victim, 0.80D, 1.15D);
            }
        }
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        for (int step = 1; step <= 12; step++) {
            BlockPos position = BlockPos.containing(getX() + horizontal.x * step, getY(), getZ() + horizontal.z * step);
            BlockState state = level().getBlockState(position);
            float hardness = state.getDestroySpeed(level(), position);
            if (!state.isAir() && !state.hasBlockEntity() && hardness >= 0.0F && hardness <= 5.0F) {
                ParasiteBlockInventory.collect((ServerLevel) level(), position, this);
            }
        }
    }

    private static final class VigilanteTendrilPart extends PartEntity<PureParasiteEntity> {
        private final boolean left;

        private VigilanteTendrilPart(PureParasiteEntity parent, boolean left) {
            super(parent);
            this.left = left;
        }

        private void updatePosition() {
            PureParasiteEntity parent = getParent();
            float yaw = parent.getYRot() * (float) Math.PI / 180.0F;
            float side = left ? 1.0F : -1.0F;
            setPos(parent.getX() + side * (float) Math.cos(yaw) * 1.1F,
                    parent.getY() + 2.3D,
                    parent.getZ() + side * (float) Math.sin(yaw) * 1.1F);
            setYRot(parent.getYRot());
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder builder) {
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        public boolean isPickable() {
            PureParasiteEntity parent = getParent();
            return parent.isAlive() && (left ? parent.isLeftVigilanteTendrilAttached()
                    : parent.isRightVigilanteTendrilAttached());
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            return getParent().hurtVigilanteTendril(left, source, amount);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(0.7F, 0.9F);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public Component getName() {
            return Component.literal(left ? "left_tendril" : "right_tendril");
        }
    }

    public enum Kind {
        GRUNT(false, true, 20.0D, 7.0D, 13.0D, 0.274172325D, 0.40D, 32.0D, 3.0F, 1.0D),
        BOMBER_LIGHT(true, false, 75.0D, 20.0D, 25.0D, 0.27D, 0.15D, 32.0D, 5.0F, 2.0D),
        MONARCH(false, true, 75.0D, 10.0D, 25.0D, 0.2775D, 1.0D, 32.0D, 5.0F, 4.0D),
        OVERSEER(true, false, 80.0D, 20.0D, 45.0D, 0.27D, 0.40D, 32.0D, 5.0F, 2.0D),
        SEEKER(true, false, 80.0D, 20.0D, 22.0D, 0.27D, 0.40D, 32.0D, 5.0F, 2.0D),
        VIGILANTE(false, false, 70.0D, 25.0D, 23.0D, 0.20D, 1.0D, 32.0D, 5.0F, 2.0D),
        WARDEN(false, true, 80.0D, 15.0D, 25.0D, 0.27D, 1.0D, 32.0D, 5.0F, 2.0D);

        private final boolean flying;
        private final boolean climbs;
        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final double movementSpeed;
        private final double knockbackResistance;
        private final double followRange;
        private final float blockHardness;
        private final double blockRange;

        Kind(boolean flying, boolean climbs, double maxHealth, double armor, double attackDamage,
             double movementSpeed, double knockbackResistance, double followRange,
             float blockHardness, double blockRange) {
            this.flying = flying;
            this.climbs = climbs;
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.knockbackResistance = knockbackResistance;
            this.followRange = followRange;
            this.blockHardness = blockHardness;
            this.blockRange = blockRange;
        }
    }
}
