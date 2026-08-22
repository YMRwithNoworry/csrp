package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraftforge.entity.PartEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.EnumSet;
import java.util.List;

/** Legacy Ancient Dreadnaut and Ancient Overlord boss implementations. */
public final class AncientParasiteEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Integer> DREAD_DAMAGE_REACTION_TICKS = SynchedEntityData.defineId(
            AncientParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DREAD_ATTACK_ANIMATION_TICKS = SynchedEntityData.defineId(
            AncientParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DREAD_URTEN = SynchedEntityData.defineId(
            AncientParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DREAD_ULTEN = SynchedEntityData.defineId(
            AncientParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DREAD_RATEN = SynchedEntityData.defineId(
            AncientParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DREAD_LATEN = SynchedEntityData.defineId(
            AncientParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_ADAPTATION_HITS = 10;
    private static final int MAX_LEARNABLE_DAMAGE_SOURCES = 5;
    private static final float ADAPTATION_PER_HIT = 0.10F;
    private static final float ADAPTATION_LEARN_CHANCE = 0.90F;
    private static final float FIRE_SUPPRESSION_CHANCE = 0.10F;
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation DREAD_ATTACK = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation DREAD_DAMAGE_REACTION = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_77");

    private final Kind kind;
    private final ServerBossEvent bossEvent;
    private final AncientPart[] bodyParts;
    private int blockBreakCooldown;
    private boolean health80 = true;
    private boolean health60 = true;
    private boolean health40 = true;
    private boolean health20 = true;

    public AncientParasiteEntity(EntityType<? extends AncientParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = 5000;
        bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        if (kind == Kind.DREADNAUT) {
            moveControl = new DreadMoveControl();
            setNoGravity(true);
            bodyParts = new AncientPart[] {
                    AncientPart.dreadTendril(this, "urten", 1, true),
                    AncientPart.dreadTendril(this, "ulten", 2, false),
                    AncientPart.dreadTendril(this, "raten", 3, true),
                    AncientPart.dreadTendril(this, "laten", 4, false)
            };
        } else {
            bodyParts = new AncientPart[] {
                    AncientPart.overlord(this, "head", 1, -3.0F, 0.0F, -1, 2.4F, 7.5F),
                    AncientPart.overlord(this, "middle", 2, 0.0F, 3.0F, 1, 2.4F, 4.5F)
            };
        }
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        AttributeSupplier.Builder attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
        if (kind == Kind.DREADNAUT) {
            attributes.add(Attributes.FLYING_SPEED, 0.30D);
        }
        return attributes;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        switch (activeKind()) {
            case DREADNAUT -> {
                goalSelector.addGoal(2, new DreadRandomFlightGoal());
                goalSelector.addGoal(4, new DreadPodGoal());
                goalSelector.addGoal(5, new DreadVolleyGoal());
                goalSelector.addGoal(6, new DreadFlightGoal());
            }
            case OVERLORD -> {
                goalSelector.addGoal(2, new OverlordMeleeGoal());
                goalSelector.addGoal(4, new OverlordHomingGoal());
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData(builder);
        builder.define(DREAD_DAMAGE_REACTION_TICKS, 0);
        builder.define(DREAD_ATTACK_ANIMATION_TICKS, 0);
        builder.define(DREAD_URTEN, true);
        builder.define(DREAD_ULTEN, true);
        builder.define(DREAD_RATEN, true);
        builder.define(DREAD_LATEN, true);
    }

    @Override
    public void tick() {
        super.tick();
        updateBodyParts();
        if (activeKind() == Kind.DREADNAUT) {
            setNoGravity(true);
        }
        if (level().isClientSide) {
            return;
        }
        bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
        bossEvent.setName(getDisplayName());
        if (blockBreakCooldown > 0) {
            blockBreakCooldown--;
        }
        if (entityData.get(DREAD_ATTACK_ANIMATION_TICKS) > 0) {
            entityData.set(DREAD_ATTACK_ANIMATION_TICKS, entityData.get(DREAD_ATTACK_ANIMATION_TICKS) - 1);
        }
        if (entityData.get(DREAD_DAMAGE_REACTION_TICKS) > 0) {
            entityData.set(DREAD_DAMAGE_REACTION_TICKS, entityData.get(DREAD_DAMAGE_REACTION_TICKS) - 1);
        }
        if (activeKind() == Kind.DREADNAUT && onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 5.0D, getZ(), 0.50D);
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            breakBlocksTowardsTarget(target);
        }
        if (activeKind() == Kind.DREADNAUT && target != null && tickCount % 30 == 0) {
            damageNearbyDreadnautTargets();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(Math.max(0.0F, getHealth() / getMaxHealth()));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 4.0F;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof ServerPlayer player) {
            bossEvent.addPlayer(player);
        }
        if (hurt && activeKind() == Kind.DREADNAUT && !level().isClientSide
                && !source.is(DamageTypeTags.IS_FALL)) {
            detachTendrilAtHealthThreshold();
        }
        return hurt;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return (activeKind() != Kind.OVERLORD || !effect.is(MobEffects.POISON))
                && super.canBeAffected(effect);
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
    public boolean doHurtTarget(Entity entity) {
        if (activeKind() != Kind.OVERLORD || !(entity instanceof LivingEntity center)) {
            boolean hurt = super.doHurtTarget(entity);
            if (hurt) {
                entityData.set(DREAD_ATTACK_ANIMATION_TICKS, 8);
            }
            return hurt;
        }
        AABB initialArea = center.getBoundingBox().inflate(5.0D, 2.0D, 5.0D);
        List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class, initialArea,
                LivingEntity::isAlive);
        boolean crowded = nearby.size() > 4;
        if (crowded) {
            nearby = level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(5.0D, 3.0D, 5.0D), LivingEntity::isAlive);
        }
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), initialArea);
        boolean hit = false;
        for (LivingEntity target : nearby) {
            if (target == this || target instanceof Parasite) {
                continue;
            }
            boolean damaged = crowded
                    ? target.hurt(damageSources().mobAttack(this),
                    (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F)
                    : super.doHurtTarget(target);
            if (damaged) {
                hit = true;
                pushAway(target, crowded ? 2.0D : 1.10D, crowded ? 1.1D : 0.85D);
            }
        }
        if (hit) {
            entityData.set(DREAD_ATTACK_ANIMATION_TICKS, 10);
        }
        return hit;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("urten", entityData.get(DREAD_URTEN));
        tag.putBoolean("ulten", entityData.get(DREAD_ULTEN));
        tag.putBoolean("raten", entityData.get(DREAD_RATEN));
        tag.putBoolean("laten", entityData.get(DREAD_LATEN));
        tag.putBoolean("healtheight", health80);
        tag.putBoolean("healthsix", health60);
        tag.putBoolean("healthfour", health40);
        tag.putBoolean("healthtwo", health20);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("urten")) entityData.set(DREAD_URTEN, tag.getBoolean("urten"));
        if (tag.contains("ulten")) entityData.set(DREAD_ULTEN, tag.getBoolean("ulten"));
        if (tag.contains("raten")) entityData.set(DREAD_RATEN, tag.getBoolean("raten"));
        if (tag.contains("laten")) entityData.set(DREAD_LATEN, tag.getBoolean("laten"));
        health80 = !tag.contains("healtheight") || tag.getBoolean("healtheight");
        health60 = !tag.contains("healthsix") || tag.getBoolean("healthsix");
        health40 = !tag.contains("healthfour") || tag.getBoolean("healthfour");
        health20 = !tag.contains("healthtwo") || tag.getBoolean("healthtwo");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (activeKind() == Kind.DREADNAUT) {
            controllers.add(new AnimationController<>(this, "state_controller", 0, this::dreadnautAnimation));
            return;
        }
        controllers.add(new AnimationController<>(this, "age_controller", 0,
                state -> state.setAndContinue(AGE_IN_TICKS)));
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> ParasiteAnimations.isMoving(this, state.isMoving())
                        ? state.setAndContinue(LIMB_SWING) : PlayState.STOP));
    }

    public Kind getKind() {
        return activeKind();
    }

    @Override
    public boolean isMultipartEntity() {
        return bodyParts != null && bodyParts.length > 0;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (bodyParts == null) {
            return;
        }
        for (int index = 0; index < bodyParts.length; index++) {
            bodyParts[index].setId(id + index + 1);
        }
    }

    @Override
    public PartEntity<?>[] getParts() {
        return bodyParts == null ? new PartEntity<?>[0] : bodyParts;
    }

    public boolean isDreadnautTendrilAttached(int partId) {
        return switch (partId) {
            case 1 -> entityData.get(DREAD_URTEN);
            case 2 -> entityData.get(DREAD_ULTEN);
            case 3 -> entityData.get(DREAD_RATEN);
            case 4 -> entityData.get(DREAD_LATEN);
            default -> true;
        };
    }

    private PlayState dreadnautAnimation(AnimationState<AncientParasiteEntity> state) {
        if (entityData.get(DREAD_DAMAGE_REACTION_TICKS) > 0) {
            return state.setAndContinue(DREAD_DAMAGE_REACTION);
        }
        if (entityData.get(DREAD_ATTACK_ANIMATION_TICKS) > 0) {
            return state.setAndContinue(DREAD_ATTACK);
        }
        return state.setAndContinue(AGE_IN_TICKS);
    }

    private void pushAway(LivingEntity target, double horizontal, double vertical) {
        Vec3 direction = target.position().subtract(position());
        double length = Math.max(0.001D, direction.horizontalDistance());
        target.push(direction.x / length * horizontal, vertical, direction.z / length * horizontal);
    }

    private void breakBlocksTowardsTarget(LivingEntity target) {
        if (blockBreakCooldown > 0 || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() <= 0.001D) {
            return;
        }
        horizontal = horizontal.normalize();
        BlockPos origin = BlockPos.containing(getX() + horizontal.x * 4.0D,
                getY() + getBbHeight() * 0.5D, getZ() + horizontal.z * 4.0D);
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || hardness < 0.0F || hardness > 9.0F) {
                continue;
            }
            if (ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this)) {
                blockBreakCooldown = 5;
            }
            return;
        }
    }

    private void fireProjectile(LivingEntity target, ParasiteProjectileEntity.Mode mode,
                                double speed, float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(), mode);
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.75D));
        projectile.configure(this, mode, start, target.getEyePosition(), speed, damage, radius, lifetime, target);
        level().addFreshEntity(projectile);
    }

    private void fireAncientBall(LivingEntity target) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(),
                ParasiteProjectileEntity.Mode.ANCIENT_BALL);
        if (projectile == null) {
            return;
        }
        Vec3 view = getViewVector(1.0F);
        Vec3 start = new Vec3(getX() + view.x, getY() + getEyeHeight() - 0.2D, getZ() + view.z);
        projectile.configureLegacyFireball(this, ParasiteProjectileEntity.Mode.ANCIENT_BALL,
                start, target.getEyePosition().subtract(start), 15.0F, 0.3D, 200);
        level().addFreshEntity(projectile);
    }

    private void triggerAttackAnimation() {
        entityData.set(DREAD_ATTACK_ANIMATION_TICKS, 10);
    }

    private void updateBodyParts() {
        for (AncientPart part : bodyParts) {
            part.updatePosition();
        }
    }

    private void detachTendrilAtHealthThreshold() {
        float health = getHealth();
        float maximum = getMaxHealth();
        if (health <= maximum * 0.8F && health80) {
            health80 = false;
        } else if (health <= maximum * 0.6F && health60) {
            health60 = false;
        } else if (health <= maximum * 0.4F && health40) {
            health40 = false;
        } else if (health <= maximum * 0.2F && health20) {
            health20 = false;
        } else {
            return;
        }
        detachRandomTendril();
        entityData.set(DREAD_DAMAGE_REACTION_TICKS, 30);
    }

    private void detachRandomTendril() {
        int start = random.nextInt(4);
        for (int offset = 0; offset < 4; offset++) {
            int partId = (start + offset) % 4 + 1;
            if (!isDreadnautTendrilAttached(partId)) {
                continue;
            }
            setDreadnautTendrilAttached(partId, false);
            spawnDetachedTendril(partId);
            return;
        }
    }

    private void setDreadnautTendrilAttached(int partId, boolean attached) {
        switch (partId) {
            case 1 -> entityData.set(DREAD_URTEN, attached);
            case 2 -> entityData.set(DREAD_ULTEN, attached);
            case 3 -> entityData.set(DREAD_RATEN, attached);
            case 4 -> entityData.set(DREAD_LATEN, attached);
            default -> {
            }
        }
    }

    private void spawnDetachedTendril(int partId) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DreadnautTentacleEntity tendril = ModEntities.ANC_DREADNAUT_TEN.get().create(serverLevel);
        if (tendril == null) {
            return;
        }
        AncientPart part = bodyParts[Math.max(0, Math.min(bodyParts.length - 1, partId - 1))];
        tendril.moveTo(part.getX(), part.getY(), part.getZ(), getYRot(), 0.0F);
        tendril.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(tendril.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        tendril.setTarget(getTarget());
        tendril.setDeltaMovement(getDeltaMovement().scale(0.5D).add(0.0D, -0.1D, 0.0D));
        serverLevel.addFreshEntity(tendril);
    }

    private void damageNearbyDreadnautTargets() {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(3.0D), this::isValidParasiteTarget)) {
            if (super.doHurtTarget(target)) {
                pushAway(target, 2.5D, 0.4D);
            }
        }
    }

    private boolean spawnDreadPod(double x, double y, double z, LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        AncientPodEntity pod = ModEntities.ANC_POD.get().create(serverLevel);
        if (pod == null) {
            return false;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = random.nextDouble() * 10.0D;
        pod.moveTo(x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius,
                random.nextFloat() * 360.0F, 0.0F);
        pod.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pod.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        pod.setOwner((byte) 62);
        pod.setTarget(target);
        pod.setDeltaMovement(0.0D, -0.35D, 0.0D);
        return serverLevel.addFreshEntity(pod);
    }

    private int terrainHeight(int x, int z) {
        return level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private double clampFlightY(double desiredY, int x, int z) {
        double maximum = MobsConfig.ancientDreadnautMaxY();
        double minimum = Math.min(maximum, Math.max(MobsConfig.ancientDreadnautMinY(), terrainHeight(x, z)
                + MobsConfig.ancientDreadnautMinY()));
        return Mth.clamp(desiredY, minimum, maximum);
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        return getType() == ModEntities.ANC_OVERLORD.get() ? Kind.OVERLORD : Kind.DREADNAUT;
    }

    private final class DreadVolleyGoal extends Goal {
        private int attackTimer;
        private int shots;

        private DreadVolleyGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
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
            if (target == null || distanceToSqr(target) >= 4225.0D || !hasLineOfSight(target)) {
                attackTimer = Math.max(0, attackTimer - 1);
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            attackTimer++;
            if (hasEffect(ModMobEffects.RAGE.get())) {
                attackTimer++;
            }
            if (attackTimer == 50) {
                playSound(ModSounds.get("oronco.shooting"), 4.0F, 1.0F);
            }
            if (attackTimer > 60 && attackTimer % 20 == 0) {
                fireAncientBall(target);
                playSound(ModSounds.get("oronco.shootingreal"), 2.0F, 1.0F);
                triggerAttackAnimation();
                if (++shots >= 3) {
                    attackTimer = 0;
                    shots = 0;
                }
            }
        }

        @Override
        public void stop() {
            attackTimer = 0;
            shots = 0;
        }
    }

    private final class DreadPodGoal extends Goal {
        private int attackTimer;
        private int attackingTicks;
        private int spawnedPods;
        private double targetX;
        private double targetY;
        private double targetZ;

        private DreadPodGoal() {
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return attackingTicks > 0 || target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (attackingTicks > 0) {
                attackingTicks++;
                getNavigation().stop();
                if (attackingTicks == 2) {
                    playSound(ModSounds.get("ancient.pod"), 5.0F, 1.0F);
                    triggerAttackAnimation();
                }
                if (attackingTicks >= 40 && attackingTicks % 20 == 0
                        && spawnDreadPod(targetX, targetY, targetZ, target)) {
                    spawnedPods++;
                }
                if (spawnedPods >= MobsConfig.ancientDreadnautPodNumber()) {
                    attackingTicks = 0;
                    attackTimer = 0;
                    spawnedPods = 0;
                }
                return;
            }
            if (target == null || !target.isAlive()) {
                attackTimer = 0;
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (hasLineOfSight(target) && distanceToSqr(target) < 2500.0D
                    && (target.onGround() || random.nextBoolean())) {
                attackTimer++;
            } else {
                attackTimer = 0;
            }
            if (attackTimer >= MobsConfig.ancientDreadnautPodCooldownTicks()) {
                attackingTicks = 1;
                spawnedPods = 0;
                targetX = target.getX();
                targetY = getY() + 25.0D;
                targetZ = target.getZ();
            }
        }

        @Override
        public void stop() {
            if (attackingTicks == 0) {
                attackTimer = 0;
                spawnedPods = 0;
            }
        }
    }

    private final class DreadRandomFlightGoal extends Goal {
        private int nextMoveTick;

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return true;
        }

        @Override
        public void tick() {
            if (--nextMoveTick > 0) {
                return;
            }
            nextMoveTick = 10 + random.nextInt(20);
            LivingEntity target = getTarget();
            double centerX = target == null ? getX() : target.getX();
            double centerY = target == null ? getY() : target.getY();
            double centerZ = target == null ? getZ() : target.getZ();
            double distance = target == null ? 0.0D : distanceToSqr(target);
            int range = distance > 400.0D ? 10 : distance < 100.0D ? 7 : 14;
            double x = centerX + random.nextInt(range + 1) - range * 0.5D;
            double z = centerZ + random.nextInt(range + 1) - range * 0.5D;
            double desiredY = centerY + (distance < 100.0D ? 4.0D + random.nextInt(7)
                    : random.nextInt(11) - 5.0D);
            int blockX = Mth.floor(x);
            int blockZ = Mth.floor(z);
            double y = clampFlightY(desiredY, blockX, blockZ);
            if (level().getBlockState(BlockPos.containing(x, y, z)).isAir()) {
                getMoveControl().setWantedPosition(x, y, z, distance < 100.0D ? 0.45D : 0.30D);
            }
        }
    }

    private final class DreadFlightGoal extends Goal {
        private int unseenTicks;

        private DreadFlightGoal() {
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
            if (!hasLineOfSight(target) || distanceToSqr(target) >= 4096.0D) {
                unseenTicks++;
            } else {
                unseenTicks = 0;
            }
            if (unseenTicks >= 6) {
                setTarget(null);
                unseenTicks = 0;
            }
        }
    }

    private final class OverlordHomingGoal extends Goal {
        private int cooldown;

        private OverlordHomingGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && hasLineOfSight(target) && distanceToSqr(target) >= 100.0D
                    && distanceToSqr(target) <= 1600.0D;
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
                fireProjectile(target, ParasiteProjectileEntity.Mode.HOMING, 0.0D, 15.0F, 2.0D, 200);
                triggerAttackAnimation();
                cooldown = 80;
            }
        }
    }

    private final class OverlordMeleeGoal extends MeleeAttackGoal {
        private OverlordMeleeGoal() {
            super(AncientParasiteEntity.this, 1.0D, false);
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && distanceToSqr(target) < 100.0D && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && distanceToSqr(target) < 100.0D && super.canContinueToUse();
        }
    }

    private final class DreadMoveControl extends MoveControl {
        private DreadMoveControl() {
            super(AncientParasiteEntity.this);
        }

        @Override
        public void tick() {
            if (!hasWanted()) {
                return;
            }
            double x = getWantedX() - getX();
            double y = getWantedY() - getY();
            double z = getWantedZ() - getZ();
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance < getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                setDeltaMovement(getDeltaMovement().scale(0.5D));
                return;
            }
            Vec3 movement = getDeltaMovement().add(x / distance * 0.05D * getSpeedModifier(),
                    y / distance * 0.05D * getSpeedModifier(),
                    z / distance * 0.05D * getSpeedModifier());
            setDeltaMovement(movement);
            LivingEntity target = getTarget();
            double facingX = target == null ? movement.x : target.getX() - getX();
            double facingZ = target == null ? movement.z : target.getZ() - getZ();
            setYRot((float) (-Mth.atan2(facingX, facingZ) * Mth.RAD_TO_DEG));
            yBodyRot = getYRot();
        }
    }

    private static final class AncientPart extends PartEntity<AncientParasiteEntity> {
        private final String name;
        private final int partId;
        private final boolean dreadTendril;
        private final float sideOffset;
        private final float forwardOffset;
        private final float yOffset;
        private final float width;
        private final float height;

        private AncientPart(AncientParasiteEntity parent, String name, int partId, boolean dreadTendril,
                            float sideOffset, float forwardOffset, float yOffset, float width, float height) {
            super(parent);
            this.name = name;
            this.partId = partId;
            this.dreadTendril = dreadTendril;
            this.sideOffset = sideOffset;
            this.forwardOffset = forwardOffset;
            this.yOffset = yOffset;
            this.width = width;
            this.height = height;
        }

        private static AncientPart dreadTendril(AncientParasiteEntity parent, String name,
                                                 int partId, boolean left) {
            float side = left ? 2.17F : -2.17F;
            float forward = partId <= 2 ? 1.4F : -1.4F;
            return new AncientPart(parent, name, partId, true, side, forward,
                    1.4F, 0.6F, 2.0F);
        }

        private static AncientPart overlord(AncientParasiteEntity parent, String name, int partId,
                                             float offset, float yOffset, int inverted,
                                             float width, float height) {
            return new AncientPart(parent, name, partId, false, 0.0F,
                    offset * inverted, yOffset, width, height);
        }

        private void updatePosition() {
            AncientParasiteEntity parent = getParent();
            float yaw = parent.getYRot() * Mth.DEG_TO_RAD;
            double forwardX = -Mth.sin(yaw);
            double forwardZ = Mth.cos(yaw);
            double sideX = Mth.cos(yaw);
            double sideZ = Mth.sin(yaw);
            setPos(parent.getX() + sideX * sideOffset + forwardX * forwardOffset,
                    parent.getY() + yOffset,
                    parent.getZ() + sideZ * sideOffset + forwardZ * forwardOffset);
            setYRot(parent.getYRot());
        }

        @Override
        protected void defineSynchedData() {
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        public boolean isPickable() {
            AncientParasiteEntity parent = getParent();
            return parent.isAlive() && (!dreadTendril || parent.isDreadnautTendrilAttached(partId));
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            return isPickable() && getParent().hurt(source, amount);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(width, height);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public Component getName() {
            return Component.literal(name);
        }
    }

    public enum Kind {
        DREADNAUT(200.0D, 15.0D, 15.0D, 0.30D),
        OVERLORD(250.0D, 15.0D, 20.0D, 0.23D);

        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final double movementSpeed;

        Kind(double maxHealth, double armor, double attackDamage, double movementSpeed) {
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
        }
    }
}
