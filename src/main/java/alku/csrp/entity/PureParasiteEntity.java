package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    private static final int MAX_ADAPTATION_HITS = 8;
    private static final int MAX_LEARNABLE_DAMAGE_SOURCES = 12;
    private static final float ADAPTATION_PER_HIT = 0.125F;
    private static final float ADAPTATION_LEARN_CHANCE = 0.95F;
    private static final float FIRE_SUPPRESSION_CHANCE = 0.30F;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation FLY = ParasiteAnimations.loop(this, "fly");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation WARDEN_CHARGE_IDLE = ParasiteAnimations.loop(this,
            "idle.get_parasite_status_3");
    private final RawAnimation WARDEN_CHARGE_WALK = ParasiteAnimations.loop(this,
            "walk.get_parasite_status_3");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this, "idle.get_parasite_status_10");
    private final RawAnimation VIGILANTE_ATTACK_IDLE = ParasiteAnimations.loop(this,
            "idle.get_parasite_status_1");
    private final RawAnimation VIGILANTE_ATTACK_WALK = ParasiteAnimations.loop(this,
            "walk.get_parasite_status_1");
    private final RawAnimation VIGILANTE_ATTACK2_IDLE = ParasiteAnimations.loop(this,
            "idle.get_parasite_status_2");
    private final RawAnimation VIGILANTE_ATTACK2_WALK = ParasiteAnimations.loop(this,
            "walk.get_parasite_status_2");
    private final RawAnimation VIGILANTE_UNDERGROUND = ParasiteAnimations.loop(this,
            "idle.get_parasite_status_25");

    private final Kind kind;
    private int blockBreakCooldown;
    private int supportCooldown;
    private int attackAnimationTicks;
    private boolean deathBurstFired;

    public PureParasiteEntity(EntityType<? extends PureParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WARDEN_CHARGING, false);
        builder.define(VIGILANTE_STATUS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        Kind activeKind = activeKind();
        if (activeKind.flying) {
            setNoGravity(true);
        }
        if (level().isClientSide) {
            return;
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

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        breakBlocksTowardsTarget(target, activeKind);
        if (supportCooldown <= 0 && tickCount % 40 == 0) {
            trySummonSupport(target);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 4.0F;
        }
        return super.hurt(source, amount);
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
                    triggerAnim("attack_controller", "attack");
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
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (activeKind() == Kind.VIGILANTE && tag.contains("VigilanteStatus")) {
            entityData.set(VIGILANTE_STATUS, tag.getInt("VigilanteStatus"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    public Kind getKind() {
        return activeKind();
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
            return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.0001 ? WARDEN_CHARGE_WALK : WARDEN_CHARGE_IDLE);
        }
        if (activeKind() == Kind.VIGILANTE) {
            int status = entityData.get(VIGILANTE_STATUS);
            boolean moving = getDeltaMovement().horizontalDistanceSqr() >= 0.0001;
            return switch (status) {
                case 1 -> state.setAndContinue(moving ? VIGILANTE_ATTACK_WALK : VIGILANTE_ATTACK_IDLE);
                case 2 -> state.setAndContinue(moving ? VIGILANTE_ATTACK2_WALK : VIGILANTE_ATTACK2_IDLE);
                case 25 -> state.setAndContinue(VIGILANTE_UNDERGROUND);
                default -> state.setAndContinue(moving ? (getDeltaMovement().horizontalDistanceSqr() > 0.02D ? RUN : WALK) : IDLE);
            };
        }
        if (activeKind().flying) {
            return state.setAndContinue(FLY);
        }
        if (getDeltaMovement().horizontalDistanceSqr() < 0.0001) {
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
            triggerAnim("attack_controller", "attack");
        }
        return hit;
    }

    private void triggerAttackAnimation() {
        attackAnimationTicks = 10;
        triggerAnim("attack_controller", "attack");
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

    private void triggerPureDeathBurst() {
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(2.0D));
        Level.ExplosionInteraction interaction = level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY() + getBbHeight() * 0.5D, getZ(), 2.0F, interaction);
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
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
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.55D));
        projectile.configure(this, mode, start, target.getEyePosition(), speed, damage, radius, lifetime, target);
        level().addFreshEntity(projectile);
    }

    private void fireWebProjectile(LivingEntity target, int webKind) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
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
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = position().add(0.0D, 0.4D, 0.0D);
        projectile.configure(this, ParasiteProjectileEntity.Mode.BOMB, start, target.position(),
                0.50D, 20.0F, 2.5D, 100);
        level().addFreshEntity(projectile);
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

    public enum Kind {
        GRUNT(false, true, 20.0D, 7.0D, 13.0D, 0.274172325D, 0.40D, 32.0D, 3.0F, 1.0D),
        BOMBER_LIGHT(true, false, 75.0D, 20.0D, 25.0D, 0.27D, 0.15D, 32.0D, 5.0F, 2.0D),
        MONARCH(false, true, 75.0D, 10.0D, 25.0D, 0.2775D, 1.0D, 32.0D, 5.0F, 4.0D),
        OVERSEER(true, false, 80.0D, 20.0D, 45.0D, 0.27D, 0.40D, 32.0D, 5.0F, 2.0D),
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
