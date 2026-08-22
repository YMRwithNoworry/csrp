package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * ColonicIII (LeemSIII) - Rooter Stage III Nexus entity.
 * A stationary root-based parasite that provides support to nearby parasites.
 */
public final class ColonicIIIEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Boolean> RTTS = SynchedEntityData.defineId(
            ColonicIIIEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> GROWTH_TIME = SynchedEntityData.defineId(
            ColonicIIIEntity.class, EntityDataSerializers.INT);

    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    private int supportCooldown;
    private int attackFlashTicks;

    public ColonicIIIEntity(EntityType<? extends ColonicIIIEntity> type, Level level) {
        super(type, level);
        xpReward = 64;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.ARMOR, 21.0D)
                .add(Attributes.ATTACK_DAMAGE, 13.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData(builder);
        builder.define(RTTS, false);
        builder.define(GROWTH_TIME, 0);
    }

    public boolean getRTTS() {
        return entityData.get(RTTS);
    }

    public void setRTTS(boolean value) {
        entityData.set(RTTS, value);
    }

    public int getGrowthTime() {
        return entityData.get(GROWTH_TIME);
    }

    public void setGrowthTime(int value) {
        entityData.set(GROWTH_TIME, value);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
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

        // Stop all movement - this is a stationary entity
        getNavigation().stop();

        // Decrement cooldowns
        if (supportCooldown > 0) supportCooldown--;
        if (attackFlashTicks > 0) attackFlashTicks--;

        // Update RTTS status based on ground detection
        if (tickCount % 20 == 0) {
            setRTTS(onGround() && tickCount > 100);
        }

        // Rooter support ability
        if (supportCooldown <= 0) {
            applyRooterSupport();
            triggerAttackAnimation();
            supportCooldown = 200;
        }
    }

    private void applyRooterSupport() {
        int stage = 3;
        double range = 16.0D + stage * 4.0D; // 28 blocks for stage 3

        for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(range),
                entity -> entity != this && entity instanceof Parasite)) {
            ally.addEffect(new MobEffectInstance(ModMobEffects.PIVOT.get(), 300,
                    Math.max(0, stage - 1), false, false), this);
            ally.addEffect(new MobEffectInstance(ModMobEffects.PARATE.get(), 300,
                    Math.max(0, stage - 1), false, false), this);
        }

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                    getX(), getY() + getBbHeight() * 0.55D, getZ(),
                    16, 1.5D, 1.0D, 1.5D, 0.02D);
        }
    }

    private void triggerAttackAnimation() {
        attackFlashTicks = 12;
        triggerAnim("attack_controller", "attack");
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return 6;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return 0.17F;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return 15;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return 0.90F;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return 0.30F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Main idle animation with procedural tentacle movements
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::idleAnimation));

        // Attack animation controller
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    /**
     * Idle animation with procedural tentacle and body swaying based on ageInTicks.
     * The procedural animations from the original mod (sine wave oscillations for tentacles,
     * decoration joints, and body parts) are baked into the animation JSON files.
     */
    private PlayState idleAnimation(AnimationState<ColonicIIIEntity> state) {
        return state.setAndContinue(IDLE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("colonic_rtts", getRTTS());
        tag.putInt("colonic_growth_time", getGrowthTime());
        tag.putInt("colonic_support_cooldown", supportCooldown);
        tag.putInt("colonic_attack_flash", attackFlashTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRTTS(tag.getBoolean("colonic_rtts"));
        setGrowthTime(tag.getInt("colonic_growth_time"));
        supportCooldown = tag.getInt("colonic_support_cooldown");
        attackFlashTicks = tag.getInt("colonic_attack_flash");
    }
}
