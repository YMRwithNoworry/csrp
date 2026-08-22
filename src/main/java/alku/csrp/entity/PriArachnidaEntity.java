package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.Config;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PriArachnidaEntity extends Monster implements GeoEntity, Parasite, PullingBallOwner {
    private static final String PARASITE_STATUS_NBT_KEY = "parasite_status";
    private static final String PULL_COOLDOWN_NBT_KEY = "pull_cooldown";
    private static final String PULL_COUNT_NBT_KEY = "pull_count";

    private static final EntityDataAccessor<Integer> PARASITE_STATUS =
            SynchedEntityData.defineId(PriArachnidaEntity.class, EntityDataSerializers.INT);

    // 动画字段
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK_PREPARE = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation ATTACK = ParasiteAnimations.loop(this, "idle.get_parasite_status_2");
    private final RawAnimation PULL = ParasiteAnimations.loop(this, "idle.get_parasite_status_3");
    private final RawAnimation SKILL = ParasiteAnimations.loop(this, "idle.get_parasite_status_11");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int pullCooldown;
    private int pullCount;

    public PriArachnidaEntity(EntityType<? extends PriArachnidaEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    public static boolean checkPriArachnidaSpawnRules(EntityType<? extends Monster> type,
                                                       ServerLevelAccessor level,
                                                       MobSpawnType spawnType,
                                                       BlockPos pos,
                                                       RandomSource random) {
        int phase = Config.evolutionPhase(level.getLevel());
        return phase >= 2 && phase <= 6
                && Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false, this::canTargetEntity));
    }

    private boolean canTargetEntity(LivingEntity entity) {
        return entity != this && entity.isAlive() && !(entity instanceof Parasite);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            updateParasiteStatus();
            updatePullCooldown();
            handleSkillExecution();
        }
    }

    private void updateParasiteStatus() {
        LivingEntity target = getTarget();

        // Status 11: 技能释放
        if (getParasiteStatus() == 11) {
            return; // 在 handleSkillExecution 中处理
        }

        // Status 3: 拉扯技能状态
        if (canUsePullSkill(target)) {
            setParasiteStatus(3);
            return;
        }

        // Status 2: 近战攻击状态
        if (target != null && distanceToSqr(target) < 4.0 && hasLineOfSight(target)) {
            setParasiteStatus(2);
            return;
        }

        // Status 1: 攻击准备状态
        if (target != null && distanceToSqr(target) < 16.0 && hasLineOfSight(target)) {
            setParasiteStatus(1);
            return;
        }

        // Status 0: 默认移动状态
        setParasiteStatus(0);
    }

    private boolean canUsePullSkill(LivingEntity target) {
        return target != null
                && hasLineOfSight(target)
                && distanceToSqr(target) < 49.0 // 7格距离
                && pullCooldown <= 0
                && random.nextInt(100) < 5; // 5% 触发概率
    }

    private void updatePullCooldown() {
        if (pullCooldown > 0) {
            pullCooldown--;
        }
    }

    private void handleSkillExecution() {
        if (getParasiteStatus() != 3) {
            pullCount = 0;
            return;
        }

        if (tickCount % 20 == 0) { // 每秒执行一次
            LivingEntity target = getTarget();
            if (target != null && hasLineOfSight(target)) {
                pullCount++;
                executePullSkill(target);

                if (pullCount >= 5) {
                    setParasiteStatus(11);
                    pullCooldown = random.nextInt(260) + 40; // 40-300 tick 冷却
                    pullCount = 0;
                }
            } else {
                setParasiteStatus(0);
                pullCount = 0;
            }
        }
    }

    private void executePullSkill(LivingEntity target) {
        PullingBallEntity projectile = ModEntities.PULLING_BALL.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.45D));
        Vec3 direction = target.getEyePosition().subtract(start);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        projectile.moveTo(start.x, start.y, start.z, getYRot(), getXRot());
        projectile.setOwner(this);
        projectile.setDeltaMovement(direction.normalize().scale(0.8D));
        level().addFreshEntity(projectile);
    }

    @Override
    public boolean captureTarget(LivingEntity target) {
        if (!isValidPullTarget(target)) {
            return false;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), this);
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1), this);
        Vec3 pull = position().subtract(target.position());
        if (pull.lengthSqr() > 0.001D) {
            pull = pull.normalize().scale(0.55D);
            target.push(pull.x, 0.12D, pull.z);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    14, target.getBbWidth() * 0.4D, target.getBbHeight() * 0.35D,
                    target.getBbWidth() * 0.4D, 0.03D);
        }
        return true;
    }

    @Override
    public boolean isValidPullTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit && getParasiteStatus() == 2) {
            triggerAnim("attack_controller", "attack");
        }
        return hit;
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(PARASITE_STATUS_NBT_KEY, getParasiteStatus());
        tag.putInt(PULL_COOLDOWN_NBT_KEY, pullCooldown);
        tag.putInt(PULL_COUNT_NBT_KEY, pullCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(PARASITE_STATUS_NBT_KEY)) {
            setParasiteStatus(tag.getInt(PARASITE_STATUS_NBT_KEY));
        }
        if (tag.contains(PULL_COOLDOWN_NBT_KEY)) {
            pullCooldown = tag.getInt(PULL_COOLDOWN_NBT_KEY);
        }
        if (tag.contains(PULL_COUNT_NBT_KEY)) {
            pullCount = tag.getInt(PULL_COUNT_NBT_KEY);
        }
    }

    @Nullable
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
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.RUPTER_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ParasiteAnimations.play(this, "attack")));
    }

    private <T extends PriArachnidaEntity> PlayState movementAnimation(AnimationState<T> state) {
        int status = getParasiteStatus();

        // Status 11: 技能释放动画
        if (status == 11) {
            return state.setAndContinue(SKILL);
        }

        // Status 3: 拉扯技能状态
        if (status == 3) {
            return state.setAndContinue(PULL);
        }

        // Status 2: 近战攻击状态
        if (status == 2) {
            return state.setAndContinue(ATTACK);
        }

        // Status 1: 攻击准备状态
        if (status == 1) {
            return state.setAndContinue(ATTACK_PREPARE);
        }

        // Status 0: 默认移动状态
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return state.setAndContinue(IDLE);
        }
        return state.setAndContinue(WALK);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
