package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/** Legacy Ancient Drop Pod (EntityDropPod). */
public final class AncientPodEntity extends PrimitiveParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private static final int DEFAULT_FUSE = 80;

    private final RawAnimation groundedAnimation = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks");
    private final RawAnimation airborneAnimation = ParasiteAnimations.loop(
            this, "func_78087_a.age_in_ticks.get_parasite_status_1");
    private byte owner = 62;
    private int fuseTicks = DEFAULT_FUSE;
    private boolean fuseStarted;
    private boolean exploded;

    public AncientPodEntity(EntityType<? extends AncientPodEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 45.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (!fuseStarted && onGround()) {
            fuseStarted = true;
        }
        if (!fuseStarted || exploded) {
            return;
        }
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0D, getZ(),
                    4, 0.35D, 0.5D, 0.35D, 0.02D);
            if (--fuseTicks <= 0) {
                explodePod(serverLevel);
            }
        }
    }

    public void setOwner(byte owner) {
        this.owner = owner;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 2, this::movementAnimation));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("pod_owner", owner);
        tag.putInt("pod_fuse", fuseTicks);
        tag.putBoolean("pod_fuse_started", fuseStarted);
        tag.putBoolean("pod_exploded", exploded);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.contains("pod_owner") ? tag.getByte("pod_owner") : 62;
        fuseTicks = tag.contains("pod_fuse") ? tag.getInt("pod_fuse") : DEFAULT_FUSE;
        fuseStarted = tag.getBoolean("pod_fuse_started");
        exploded = tag.getBoolean("pod_exploded");
    }

    private PlayState movementAnimation(AnimationState<AncientPodEntity> state) {
        // The legacy pod uses status 1 while falling and returns to its normal pose on landing.
        return state.setAndContinue(onGround() ? groundedAnimation : airborneAnimation);
    }

    private void explodePod(ServerLevel level) {
        exploded = true;
        DragonEggAssimilationEntity.assimilateDragonEggs(level, getBoundingBox().inflate(4.0D));
        Level.ExplosionInteraction interaction = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level.explode(this, getX(), getY(), getZ(), 4.0F, interaction);
        spawnLingeringCloud(level);
        spawnContents(level);
        discard();
    }

    private void spawnLingeringCloud(ServerLevel level) {
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level, getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(getBbWidth() * 2.0F);
        cloud.setWaitTime(5);
        cloud.setDuration(600);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 0, false, false));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0, false, false));
        level.addFreshEntity(cloud);
    }

    private void spawnContents(ServerLevel level) {
        int count = owner == 62 ? 5 : owner == 63 ? 1 : 0;
        for (int index = 0; index < count; index++) {
            Mob mob = random.nextBoolean() ? ModEntities.BUGLIN.get().create(level) : ModEntities.RUPTER.get().create(level);
            if (mob == null) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2.0D;
            mob.moveTo(getX() + Math.cos(angle) * 1.5D, getY(), getZ() + Math.sin(angle) * 1.5D,
                    random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            mob.setTarget(getTarget());
            level.addFreshEntity(mob);
        }
    }
}
