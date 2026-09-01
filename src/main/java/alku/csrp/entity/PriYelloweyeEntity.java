package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModSounds;
import alku.csrp.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import alku.csrp.animation.CitadelAnimatedEntity;
import alku.csrp.animation.CitadelAnimationCache;
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelAnimationState;
import alku.csrp.animation.CitadelPlayState;
import alku.csrp.animation.CitadelRawAnimation;
import alku.csrp.animation.CitadelAnimationUtil;

import java.util.EnumSet;

public class PriYelloweyeEntity extends PrimitiveParasiteEntity {
    private final CitadelRawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final CitadelRawAnimation FLY = ParasiteAnimations.loop(this, "fly");
    private final CitadelRawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    private final CitadelAnimationCache animationCache = CitadelAnimationUtil.createInstanceCache(this);
    private int shootCooldown;
    private int shootCount;

    public PriYelloweyeEntity(EntityType<? extends PriYelloweyeEntity> entityType, Level level) {
        super(entityType, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public static boolean checkPriYelloweyeSpawnRules(EntityType<? extends Monster> type,
                                                       ServerLevelAccessor level,
                                                       MobSpawnType spawnType,
                                                       BlockPos pos,
                                                       RandomSource random) {
        int phase = Config.evolutionPhase(level.getLevel());
        return phase >= 0 && phase <= 3
                && Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        goalSelector.addGoal(1, new YelloweyeRangedGoal());
        goalSelector.addGoal(1, new FloatingIdleGoal());
        goalSelector.addGoal(2, new RandomFlyingGoal());
        goalSelector.addGoal(3, new ParasiteFollowGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            setNoGravity(true);

            // 触地自动上升
            if (onGround()) {
                getMoveControl().setWantedPosition(getX(), getY() + 5.0, getZ(), 0.5);
            }

            // 射击冷却
            if (shootCooldown > 0) {
                shootCooldown--;
            }
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.get("emana.growl");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("emana.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.get("emana.death");
    }

    @Override
    public void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers) {
        controllers.add(new CitadelAnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new CitadelAnimationController<>(this, "attack_controller", 0, state -> CitadelPlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    private <T extends PriYelloweyeEntity> CitadelPlayState movementAnimation(CitadelAnimationState<T> state) {
        if (getDeltaMovement().horizontalDistanceSqr() > 0.001 || !onGround()) {
            return state.setAndContinue(FLY);
        }
        return state.setAndContinue(IDLE);
    }

    @Override
    public CitadelAnimationCache getCitadelAnimationCache() {
        return animationCache;
    }

    private void fireProjectile(LivingEntity target, ParasiteProjectileEntity.Mode mode,
                                double speed, float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(), mode);
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.35D));
        projectile.configure(this, mode, start, target.getEyePosition(), speed, damage, radius, lifetime);
        level().addFreshEntity(projectile);
    }

    /**
     * 漂浮待机目标 - 保持在空中轻微晃动
     */
    private final class FloatingIdleGoal extends Goal {
        private FloatingIdleGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null && !getMoveControl().hasWanted();
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null;
        }

        @Override
        public void tick() {
            if (tickCount % 40 == 0 && random.nextInt(3) == 0) {
                double offsetX = (random.nextDouble() - 0.5) * 2.0;
                double offsetY = (random.nextDouble() - 0.5) * 1.0;
                double offsetZ = (random.nextDouble() - 0.5) * 2.0;
                getMoveControl().setWantedPosition(
                        getX() + offsetX,
                        getY() + offsetY,
                        getZ() + offsetZ,
                        0.3
                );
            }
        }
    }

    /**
     * 随机飞行目标 - 模拟原版的 AIMoveRandom
     */
    private final class RandomFlyingGoal extends Goal {
        private RandomFlyingGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !getMoveControl().hasWanted() && random.nextInt(7) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos targetPos = blockPosition();
            double speed = 0.5;
            int range = 15;
            int verticalRange = 11;

            LivingEntity target = getTarget();
            if (target != null) {
                double distSqr = distanceToSqr(target);
                if (distSqr > 100.0) {
                    // 距离太远 - 接近目标
                    targetPos = target.blockPosition();
                    range = 6;
                    verticalRange = 7;
                    speed = 0.75;
                } else if (distSqr < 36.0) {
                    // 距离太近 - 远离目标
                    targetPos = target.blockPosition();
                    range = 4;
                    verticalRange = 5;
                    speed = 0.75;
                    // 反向偏移
                    Vec3 awayDirection = position().subtract(target.position()).normalize();
                    targetPos = targetPos.offset(
                            (int) (awayDirection.x * 3),
                            (int) (awayDirection.y * 4),
                            (int) (awayDirection.z * 3)
                    );
                }
            }

            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos randomPos = targetPos.offset(
                        random.nextInt(range * 2 + 1) - range,
                        random.nextInt(verticalRange * 2 + 1) - verticalRange / 2,
                        random.nextInt(range * 2 + 1) - range
                );

                if (level().isLoaded(randomPos)) {
                    getMoveControl().setWantedPosition(
                            randomPos.getX() + 0.5,
                            randomPos.getY() + 0.5,
                            randomPos.getZ() + 0.5,
                            speed
                    );
                    break;
                }
            }
        }
    }

    private final class YelloweyeRangedGoal extends Goal {
        private YelloweyeRangedGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return shootCooldown <= 0 && target != null && target.isAlive() && hasLineOfSight(target);
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
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (++shootCount % 5 == 0) {
                fireProjectile(target, ParasiteProjectileEntity.Mode.ACID, 0.70D, 14.0F, 2.25D, 100);
                shootCooldown = 90;
                playSound(ModSounds.get("emana.shooting"), 1.0F, 1.5F);
            } else {
                fireProjectile(target, ParasiteProjectileEntity.Mode.SPINE, 1.15D, 7.0F, 0.85D, 70);
                fireProjectile(target, ParasiteProjectileEntity.Mode.SPINE, 1.05D, 7.0F, 0.85D, 70);
                shootCooldown = 36;
                playSound(ModSounds.get("emana.shooting"), 1.0F, 1.0F);
            }
            triggerAnim("attack_controller", "attack");
        }
    }
}
