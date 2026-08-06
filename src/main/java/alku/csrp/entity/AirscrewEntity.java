package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AirscrewEntity extends CrudeParasiteEntity implements PullingBallOwner {
    private static final float LEGACY_MOUTH_HEIGHT = 0.5F;
    private static final int MAX_PULL_TARGETS = 3;
    private static final int PULL_DURATION_TICKS = 600;
    private static final int VOLLEY_COOLDOWN_TICKS = 300;
    private static final double PULL_STRENGTH = 0.1;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private static final EntityDataAccessor<Integer> PULL_TARGET_0 = SynchedEntityData.defineId(
            AirscrewEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PULL_TARGET_1 = SynchedEntityData.defineId(
            AirscrewEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PULL_TARGET_2 = SynchedEntityData.defineId(
            AirscrewEntity.class, EntityDataSerializers.INT);
    private static final List<EntityDataAccessor<Integer>> PULL_TARGET_IDS = List.of(
            PULL_TARGET_0, PULL_TARGET_1, PULL_TARGET_2);

    private final Set<UUID> pullTargets = new LinkedHashSet<>();
    private int pullTicks;
    private int volleyCooldown = 40;

    public AirscrewEntity(EntityType<? extends AirscrewEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 36;
    }

    public float getTetherMouthHeight() {
        return LEGACY_MOUTH_HEIGHT;
    }

    public Vec3 getTetherMouthPosition(float partialTick) {
        return getPosition(partialTick).add(0.0D, LEGACY_MOUTH_HEIGHT, 0.0D);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ARMOR, 9.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.25).add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        for (EntityDataAccessor<Integer> targetId : PULL_TARGET_IDS) {
            builder.define(targetId, 0);
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        navigation.setCanOpenDoors(false);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new AirscrewFlightGoal());
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide) {
            return;
        }
        if (onGround()) getMoveControl().setWantedPosition(getX(), getY() + 5.0, getZ(), 0.5);

        if (!pullTargets.isEmpty()) {
            tickPullTargets();
        } else {
            pullTicks = 0;
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && --volleyCooldown <= 0 && hasLineOfSight(target)) {
                firePullingVolley(target);
                volleyCooldown = VOLLEY_COOLDOWN_TICKS;
            }
        }
    }

    public boolean captureTarget(LivingEntity target) {
        if (pullTargets.size() >= MAX_PULL_TARGETS || !isValidParasiteTarget(target)) return false;
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 3), this);
        boolean captured = pullTargets.add(target.getUUID());
        if (captured) {
            syncPullTargets();
        }
        return captured;
    }

    @Override
    public boolean isValidPullTarget(LivingEntity target) {
        return isValidParasiteTarget(target);
    }

    private void tickPullTargets() {
        if (++pullTicks > PULL_DURATION_TICKS) {
            pullTargets.clear();
            syncPullTargets();
            pullTicks = 0;
            return;
        }

        Iterator<UUID> iterator = pullTargets.iterator();
        while (iterator.hasNext()) {
            LivingEntity target = resolveTarget(iterator.next());
            if (target == null || !hasLineOfSight(target)) {
                iterator.remove();
                continue;
            }
            target.stopRiding();
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false), this);
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 3, false, false), this);
            Vec3 direction = position().subtract(target.position());
            if (direction.lengthSqr() > 0.001) {
                Vec3 pull = direction.normalize().scale(PULL_STRENGTH);
                target.push(pull.x, pull.y, pull.z);
            }
            if (distanceToSqr(target) < 4.0 && tickCount % 20 == 0) {
                float attackDamage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
                if (!(target instanceof Player) && (target.getHealth() <= attackDamage || pullTicks >= 100)) {
                    convertConsumedTarget(target);
                    iterator.remove();
                    continue;
                }
                doHurtTarget(target);
            }
        }
        syncPullTargets();
        if (level() instanceof ServerLevel serverLevel) {
            sendPullTetherParticles(serverLevel);
        }
    }

    private void convertConsumedTarget(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float consumedHealth = target.getHealth();
        target.discard();
        Mob incomplete = random.nextBoolean()
                ? ModEntities.INCOMPLETEFORM_SMALL.get().create(serverLevel)
                : ModEntities.INCOMPLETEFORM_MEDIUM.get().create(serverLevel);
        if (incomplete == null) {
            return;
        }
        incomplete.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        incomplete.setTarget(getTarget());
        serverLevel.addFreshEntity(incomplete);
        heal(Math.max(1.0F, consumedHealth * 0.2F));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("attack_controller", "attack");
        }
        return hit;
    }

    private LivingEntity resolveTarget(UUID id) {
        if (!(level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void syncPullTargets() {
        if (level().isClientSide) {
            return;
        }
        int slot = 0;
        for (UUID id : pullTargets) {
            LivingEntity target = resolveTarget(id);
            if (target == null) {
                continue;
            }
            entityData.set(PULL_TARGET_IDS.get(slot++), target.getId());
            if (slot >= PULL_TARGET_IDS.size()) {
                break;
            }
        }
        while (slot < PULL_TARGET_IDS.size()) {
            entityData.set(PULL_TARGET_IDS.get(slot++), 0);
        }
    }

    /**
     * Broadcast a tether trail so players tracking the target still see the pull when this
     * entity itself is outside their view frustum.
     */
    private void sendPullTetherParticles(ServerLevel serverLevel) {
        if (tickCount % 3 != 0) {
            return;
        }
        Vec3 start = getTetherMouthPosition(1.0F).add(getViewVector(1.0F).scale(0.25D));
        for (UUID targetId : pullTargets) {
            LivingEntity target = resolveTarget(targetId);
            if (target == null) {
                continue;
            }
            Vec3 end = target.getEyePosition();
            Vec3 delta = end.subtract(start);
            double length = delta.length();
            if (length < 0.01D) {
                continue;
            }
            int segments = Mth.clamp((int) Math.ceil(length * 3.0D), 4, 36);
            Vec3 motion = delta.scale(-0.015D / length);
            double offset = (tickCount / 3 % 2) * 0.5D;
            for (int segment = 0; segment < segments; segment++) {
                double progress = (segment + offset) / segments;
                if (progress >= 1.0D) {
                    continue;
                }
                Vec3 point = start.lerp(end, progress);
                serverLevel.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z,
                        0, motion.x, motion.y, motion.z, 1.0D);
            }
        }
    }

    /**
     * Returns the server-synchronised pull targets for the client tether renderer.
     * Entity IDs are used here because they resolve in the current client level without
     * requiring a custom packet for each pull-state update.
     */
    public List<LivingEntity> getPullTargetsForRendering() {
        List<LivingEntity> targets = new ArrayList<>(MAX_PULL_TARGETS);
        for (EntityDataAccessor<Integer> targetId : PULL_TARGET_IDS) {
            int entityId = entityData.get(targetId);
            Entity entity = entityId == 0 ? null : level().getEntity(entityId);
            if (entity instanceof LivingEntity target && target.isAlive()) {
                targets.add(target);
            }
        }
        return targets;
    }

    private void firePullingVolley(LivingEntity primary) {
        triggerAnim("attack_controller", "attack");
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(primary);
        for (LivingEntity candidate : level().getEntitiesOfClass(LivingEntity.class,
                primary.getBoundingBox().inflate(18.0, 8.0, 18.0), this::isValidParasiteTarget)) {
            if (targets.size() >= MAX_PULL_TARGETS) break;
            if (!targets.contains(candidate)) targets.add(candidate);
        }
        targets.forEach(this::shootPullingBall);
    }

    private void shootPullingBall(LivingEntity target) {
        PullingBallEntity ball = ModEntities.PULLING_BALL.get().create(level());
        if (ball == null) return;
        Vec3 start = getTetherMouthPosition(1.0F).add(getViewVector(1.0F).scale(0.5));
        Vec3 direction = target.getEyePosition().subtract(start).normalize();
        ball.setOwner(this);
        ball.setPos(start.x, start.y, start.z);
        ball.setDeltaMovement(direction.scale(0.35));
        level().addFreshEntity(ball);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag targets = new ListTag();
        pullTargets.forEach(id -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            targets.add(entry);
        });
        tag.put("pull_targets", targets);
        tag.putInt("pull_ticks", pullTicks);
        tag.putInt("volley_cooldown", volleyCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pullTargets.clear();
        for (Tag raw : tag.getList("pull_targets", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            if (entry.hasUUID("id") && pullTargets.size() < MAX_PULL_TARGETS) pullTargets.add(entry.getUUID("id"));
        }
        pullTicks = tag.getInt("pull_ticks");
        volleyCooldown = tag.getInt("volley_cooldown");
        syncPullTargets();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    private final class AirscrewFlightGoal extends Goal {
        private AirscrewFlightGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            getMoveControl().setWantedPosition(target.getX(), target.getY() + 5.0, target.getZ(), 1.0);
        }
    }
}
