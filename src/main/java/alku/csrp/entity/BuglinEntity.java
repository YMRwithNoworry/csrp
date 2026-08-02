package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BuglinEntity extends Monster implements GeoEntity, Parasite {
    public static final String GROWTH_NBT_KEY = "ruptergrow";
    public static final int EMERGENCE_TICKS = 50;

    private static final String GROWTH_TARGET_NBT_KEY = "ruptergrow_target";
    private static final String EMERGENCE_NBT_KEY = "emergence_ticks";
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation SPAWN = ParasiteAnimations.play(this, "spawn");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int growthSeconds;
    private int growthTargetSeconds;
    private int emergenceTicks = EMERGENCE_TICKS;
    private boolean emergenceStarted;

    public BuglinEntity(EntityType<? extends BuglinEntity> entityType, Level level) {
        super(entityType, level);
        this.growthTargetSeconds = random.nextInt(60) + 60;
        this.xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 7.0)
                .add(Attributes.ARMOR, 1.5)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.05)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LivingEntity.class, 8.0F, 1.0, 1.2,
                this::shouldAvoid));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    private boolean shouldAvoid(LivingEntity entity) {
        return entity != this
                && !(entity instanceof WaterAnimal)
                && !(entity instanceof Creeper)
                && !(entity instanceof Parasite)
                && !(entity instanceof Animal);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (!emergenceStarted && emergenceTicks > 0) {
                emergenceStarted = true;
                triggerAnim("emergence_controller", "spawn");
                playSound(ModSounds.BUGLIN_EMERGE.get(), 1.0F, 1.0F);
            }

            if (tickCount % 20 == 0) {
                growthSeconds++;
                tryEvolve();
            }
        }

        if (emergenceTicks > 0) {
            emergenceTicks--;
            navigation.stop();
            setDeltaMovement(Vec3.ZERO);
            spawnEmergenceParticles();
        }
    }

    private void tryEvolve() {
        if (growthSeconds <= growthTargetSeconds || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BuglinEvolutionTarget.rupterType().ifPresent(type -> {
            Mob rupter = type.create(serverLevel);
            if (rupter == null) {
                return;
            }
            rupter.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            serverLevel.addFreshEntity(rupter);
            playSound(ModSounds.BUGLIN_GROW.get(), 1.0F, 1.0F);
            discard();
        });
    }

    private void spawnEmergenceParticles() {
        if (!(level() instanceof ServerLevel serverLevel) || tickCount % 2 != 0) {
            return;
        }

        BlockState state = level().getBlockState(BlockPos.containing(getX(), getY() - 0.1, getZ()));
        if (!state.isAir()) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    getX(), getY() + 0.1, getZ(), 2,
                    getBbWidth() * 0.5, 0.05, getBbWidth() * 0.5, 0.02);
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        super.travel(emergenceTicks > 0 ? Vec3.ZERO : travelVector);
    }

    @Override
    public void push(Entity entity) {
        if (!level().isClientSide && tickCount % 20 == 0 && entity instanceof LivingEntity living
                && !(living instanceof Parasite)) {
            living.addEffect(new MobEffectInstance(ModMobEffects.COTH, 100, 0));
        }
        super.push(entity);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(GROWTH_NBT_KEY, growthSeconds);
        tag.putInt(GROWTH_TARGET_NBT_KEY, growthTargetSeconds);
        tag.putInt(EMERGENCE_NBT_KEY, emergenceTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(GROWTH_NBT_KEY)) {
            growthSeconds = tag.getInt(GROWTH_NBT_KEY);
        }
        if (tag.contains(GROWTH_TARGET_NBT_KEY)) {
            growthTargetSeconds = tag.getInt(GROWTH_TARGET_NBT_KEY);
        }
        if (tag.contains(EMERGENCE_NBT_KEY)) {
            emergenceTicks = tag.getInt(EMERGENCE_NBT_KEY);
            emergenceStarted = true;
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BUGLIN_GROWL.get();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return ModSounds.BUGLIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BUGLIN_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "emergence_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("spawn", SPAWN));
    }

    private <T extends BuglinEntity> PlayState movementAnimation(AnimationState<T> state) {
        if (emergenceTicks > 0) {
            return PlayState.STOP;
        }
        if (!state.isMoving()) {
            return state.setAndContinue(IDLE);
        }
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.015 ? RUN : WALK);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
