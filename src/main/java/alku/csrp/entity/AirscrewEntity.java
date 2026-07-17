package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
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

public final class AirscrewEntity extends CrudeParasiteEntity {
    private static final int MAX_PULL_TARGETS = 3;
    private static final int PULL_DURATION_TICKS = 600;
    private static final int VOLLEY_COOLDOWN_TICKS = 300;
    private static final double PULL_STRENGTH = 0.1;
    private static final RawAnimation ANIMATION = RawAnimation.begin().thenLoop("animation");

    private final Set<UUID> pullTargets = new LinkedHashSet<>();
    private int pullTicks;
    private int volleyCooldown = 40;

    public AirscrewEntity(EntityType<? extends AirscrewEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 36;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 120.0).add(Attributes.ARMOR, 19.0)
                .add(Attributes.ATTACK_DAMAGE, 30.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.25).add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.FOLLOW_RANGE, 48.0);
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
        if (level().isClientSide) return;
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
        return pullTargets.add(target.getUUID());
    }

    private void tickPullTargets() {
        if (++pullTicks > PULL_DURATION_TICKS) {
            pullTargets.clear();
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

    private LivingEntity resolveTarget(UUID id) {
        if (!(level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void firePullingVolley(LivingEntity primary) {
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
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.5));
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
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(ANIMATION)));
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
