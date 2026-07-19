package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
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

/** Legacy Ancient Dreadnaut and Ancient Overlord boss implementations. */
public final class AncientParasiteEntity extends PrimitiveParasiteEntity {
    private static final int MAX_ADAPTATION_HITS = 10;
    private static final int MAX_LEARNABLE_DAMAGE_SOURCES = 5;
    private static final float ADAPTATION_PER_HIT = 0.10F;
    private static final float ADAPTATION_LEARN_CHANCE = 0.90F;
    private static final float BURNING_LEARN_CHANCE = 0.80F;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private final Kind kind;
    private final ServerBossEvent bossEvent;
    private int blockBreakCooldown;
    private int attackAnimationTicks;
    private int nextTendrilThreshold = 4;
    private boolean deathBurstFired;

    public AncientParasiteEntity(EntityType<? extends AncientParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = 5000;
        bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        if (kind == Kind.DREADNAUT) {
            moveControl = new FlyingMoveControl(this, 18, true);
            setNoGravity(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        AttributeSupplier.Builder attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
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
                goalSelector.addGoal(1, new DreadVolleyGoal());
                goalSelector.addGoal(2, new DreadPodGoal());
                goalSelector.addGoal(3, new DreadFlightGoal());
            }
            case OVERLORD -> {
                goalSelector.addGoal(1, new OverlordHomingGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
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
        if (attackAnimationTicks > 0) {
            attackAnimationTicks--;
        }
        if (activeKind() == Kind.DREADNAUT && onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 8.0D, getZ(), 0.60D);
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            breakBlocksTowardsTarget(target);
        }
        if (activeKind() == Kind.DREADNAUT) {
            deployTendrilReinforcements();
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
        bossEvent.addPlayer(player);
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
    protected boolean shouldLearnDamageSource(DamageSource source, String damageId, int previousHits) {
        float chance = isOnFire() ? BURNING_LEARN_CHANCE : ADAPTATION_LEARN_CHANCE;
        return previousHits < MAX_ADAPTATION_HITS && random.nextFloat() < chance;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        if (activeKind() != Kind.OVERLORD || !(entity instanceof LivingEntity center)) {
            boolean hurt = super.doHurtTarget(entity);
            if (hurt) {
                attackAnimationTicks = 8;
            }
            return hurt;
        }
        boolean hit = false;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(4.0D, 2.0D, 4.0D), this::isValidParasiteTarget)) {
            if (!super.doHurtTarget(target)) {
                continue;
            }
            hit = true;
            pushAway(target, 1.10D, 0.85D);
        }
        if (hit) {
            attackAnimationTicks = 10;
        }
        return hit;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !deathBurstFired && random.nextFloat() < 0.35F) {
            deathBurstFired = true;
            triggerAncientDeathBurst();
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ancient_tendril_threshold", nextTendrilThreshold);
        tag.putBoolean("ancient_death_burst", deathBurstFired);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nextTendrilThreshold = tag.contains("ancient_tendril_threshold")
                ? tag.getInt("ancient_tendril_threshold") : 4;
        deathBurstFired = tag.getBoolean("ancient_death_burst");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    public Kind getKind() {
        return activeKind();
    }

    private PlayState movementAnimation(AnimationState<AncientParasiteEntity> state) {
        if (activeKind() == Kind.DREADNAUT) {
            return state.setAndContinue(FLY);
        }
        if (attackAnimationTicks > 0) {
            return state.setAndContinue(ATTACK);
        }
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
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
            if (level().destroyBlock(candidate, true, this)) {
                blockBreakCooldown = 5;
            }
            return;
        }
    }

    private void fireProjectile(LivingEntity target, ParasiteProjectileEntity.Mode mode,
                                double speed, float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.75D));
        projectile.configure(this, mode, start, target.getEyePosition(), speed, damage, radius, lifetime, target);
        level().addFreshEntity(projectile);
    }

    private void deployTendrilReinforcements() {
        float healthFraction = getHealth() / getMaxHealth();
        while (nextTendrilThreshold > 0 && healthFraction <= nextTendrilThreshold / 5.0F) {
            nextTendrilThreshold--;
            spawnDreadnautReinforcements();
        }
    }

    private void spawnDreadnautReinforcements() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int index = 0; index < 2; index++) {
            BuglinEntity buglin = ModEntities.BUGLIN.get().create(serverLevel);
            if (buglin == null) {
                continue;
            }
            double angle = Math.PI * (index + random.nextDouble());
            buglin.moveTo(getX() + Math.cos(angle) * 3.0D, getY(), getZ() + Math.sin(angle) * 3.0D,
                    getYRot(), 0.0F);
            buglin.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(buglin.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            buglin.setTarget(getTarget());
            serverLevel.addFreshEntity(buglin);
            serverLevel.sendParticles(ParticleTypes.SMOKE, buglin.getX(), buglin.getY() + 0.4D, buglin.getZ(),
                    6, 0.3D, 0.3D, 0.3D, 0.02D);
        }
    }

    private void spawnDreadPods(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int index = 0; index < 5; index++) {
            float roll = random.nextFloat();
            Mob minion = roll < 0.666F ? ModEntities.RUPTER.get().create(serverLevel)
                    : roll < 0.782F ? ModEntities.GRUNT.get().create(serverLevel) : null;
            if (minion == null) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2.0D;
            minion.moveTo(target.getX() + Math.cos(angle) * 4.0D, target.getY() + 6.0D,
                    target.getZ() + Math.sin(angle) * 4.0D, getYRot(), 0.0F);
            minion.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(minion.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            minion.setTarget(target);
            minion.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 1, false, false), this);
            minion.setDeltaMovement(0.0D, -0.35D, 0.0D);
            serverLevel.addFreshEntity(minion);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, minion.getX(), minion.getY(), minion.getZ(),
                    8, 0.4D, 0.4D, 0.4D, 0.03D);
        }
    }

    private void triggerAncientDeathBurst() {
        Level.ExplosionInteraction interaction = level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY() + getBbHeight() * 0.5D, getZ(), 5.0F, interaction);
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(7.0F);
        cloud.setDuration(160);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 260, 1, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 320, 1, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 240, 1, false, true));
        level().addFreshEntity(cloud);
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        return getType() == ModEntities.ANC_OVERLORD.get() ? Kind.OVERLORD : Kind.DREADNAUT;
    }

    private final class DreadVolleyGoal extends Goal {
        private int cooldown;
        private int shots;
        private int delay;

        private DreadVolleyGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && hasLineOfSight(target) && distanceToSqr(target) <= 4096.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && shots < 3;
        }

        @Override
        public void start() {
            shots = 0;
            delay = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (delay > 0) {
                delay--;
                return;
            }
            fireProjectile(target, ParasiteProjectileEntity.Mode.WITHER, 1.0D, 15.0F, 2.5D, 100);
            shots++;
            delay = 8;
        }

        @Override
        public void stop() {
            cooldown = 100;
        }
    }

    private final class DreadPodGoal extends Goal {
        private int cooldown;
        private int chargeTicks;

        private DreadPodGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.onGround() && distanceToSqr(target) <= 4096.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return chargeTicks < 30 && getTarget() != null;
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
            if (++chargeTicks == 20) {
                spawnDreadPods(target);
            }
        }

        @Override
        public void stop() {
            cooldown = 240;
        }
    }

    private final class DreadFlightGoal extends Goal {
        private int swoopCooldown;
        private int swoopTicks;

        private DreadFlightGoal() {
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
            if (swoopTicks > 0) {
                swoopTicks--;
                getMoveControl().setWantedPosition(target.getX(), target.getY() + 0.5D, target.getZ(), 1.25D);
            } else {
                if (swoopCooldown > 0) {
                    swoopCooldown--;
                } else {
                    swoopTicks = 28;
                }
                getMoveControl().setWantedPosition(target.getX(), target.getY() + 8.0D, target.getZ(), 0.85D);
            }
            if (swoopTicks > 0 && distanceToSqr(target) <= 16.0D) {
                doHurtTarget(target);
                pushAway(target, 1.5D, 1.0D);
                swoopTicks = 0;
                swoopCooldown = 80;
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
            return target != null && hasLineOfSight(target) && distanceToSqr(target) >= 36.0D
                    && distanceToSqr(target) <= 4096.0D;
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
                fireProjectile(target, ParasiteProjectileEntity.Mode.LIGHT, 1.10D, 20.0F, 2.0D, 100);
                cooldown = 60;
            }
        }
    }

    public enum Kind {
        DREADNAUT(200.0D, 15.0D, 15.0D, 0.30D),
        OVERLORD(250.0D, 15.0D, 20.0D, 0.25D);

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
