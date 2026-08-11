package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;

import java.util.EnumSet;
import java.util.List;

/** Shared detonation and toxic-cloud behavior of the original carrier parasites. */
public abstract class CarrierEntity extends PrimitiveParasiteEntity {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> SKIN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(CarrierEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BYTE);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> FUSE_TICKS =
            net.minecraft.network.syncher.SynchedEntityData.defineId(CarrierEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);
    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private static final float LOW_HEALTH_FUSE_THRESHOLD = 0.05F;
    private static final String FUSE_TICKS_TAG = "carrier_fuse_ticks";
    private final int fuseTime;
    private final double viralRadius;
    private final int viralAmplifier;
    private final int cloudDuration;
    private final int vomitDuration;
    private boolean detonated;

    protected CarrierEntity(EntityType<? extends CarrierEntity> type, Level level, int fuseTime,
            double viralRadius, int viralAmplifier, int cloudDuration, int vomitDuration) {
        super(type, level);
        this.fuseTime = fuseTime;
        this.viralRadius = viralRadius;
        this.viralAmplifier = viralAmplifier;
        this.cloudDuration = cloudDuration;
        this.vomitDuration = vomitDuration;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, (byte) 0);
        builder.define(FUSE_TICKS, -1);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData);
        if (getSkin() == 0 && (random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase())) {
            setSkin(1);
        }
        return data;
    }

    public final int getSkin() {
        return entityData.get(SKIN);
    }

    public final void setSkin(int skin) {
        int normalized = skin == 1 ? 1 : 0;
        if (getSkin() == normalized) {
            return;
        }
        entityData.set(SKIN, (byte) normalized);
        if (normalized == 1) {
            onVariantActivated();
        }
    }

    protected void onVariantActivated() {
    }

    protected final boolean isVariant() {
        return getSkin() == 1;
    }

    protected boolean griefingEnabled() {
        return false;
    }

    protected List<? extends String> spawnTable() {
        return List.of();
    }

    protected float cloudRadiusMultiplier() {
        return 3.5F;
    }

    protected int variantViralAmplifier() {
        return viralAmplifier + 2;
    }

    protected double variantViralRadius() {
        return viralRadius + 4.0D;
    }

    protected int variantVomitDuration() {
        return vomitDuration;
    }

    protected int variantCloudDuration() {
        return cloudDuration;
    }

    protected int variantPoisonAmplifier() {
        return 2;
    }

    protected int normalPoisonAmplifier() {
        return 0;
    }

    protected int cloudWaitTime() {
        return 10;
    }

    protected int fuseIncrement() {
        return 2;
    }

    protected boolean startsFuseAtLowHealth() {
        return true;
    }

    protected boolean variantExplosionDamage() {
        return true;
    }

    protected SoundEvent explosionSound() {
        return ModSounds.RATHOL_BOOM.get();
    }

    protected float explosionVolume() {
        return 2.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new SwellGoal());
        if (usesMeleeAttack()) {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        }
    }

    protected boolean usesMeleeAttack() {
        return true;
    }

    protected abstract RawAnimation ageAnimation();

    protected RawAnimation limbSwingAnimation() {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 0,
                state -> state.setAndContinue(ageAnimation())));
        RawAnimation limbSwing = limbSwingAnimation();
        if (limbSwing != null) {
            controllers.add(new AnimationController<>(this, "movement_controller", 4, state ->
                    ParasiteAnimations.isMoving(this, state.isMoving())
                            ? state.setAndContinue(limbSwing) : PlayState.STOP));
        }

    }

    /**
     * 获取爆炸膨胀进度 (模拟原版苦力怕的 getCreeperFlashIntensity)
     * @param partialTick 部分刻
     * @return 0.0-1.0 的进度值
     */
    public float getSwellProgress(float partialTick) {
        int fuseTicks = getFuseTicks();
        if (fuseTicks < 0 || fuseTime <= 0) {
            return 0.0F;
        }
        return Mth.clamp((fuseTicks + partialTick) / (float) fuseTime, 0.0F, 1.0F);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return usesMeleeAttack() || super.doHurtTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || detonated || !isAlive()) {
            return;
        }

        if (startsFuseAtLowHealth() && getHealth() < getMaxHealth() * LOW_HEALTH_FUSE_THRESHOLD) {
            startFuse();
        }
        advanceFuse();
    }

    @Override
    protected void tickDeath() {
        if (isOnFire()) {
            super.tickDeath();
            return;
        }
        deathTime++;
        if (!level().isClientSide) {
            startFuse();
            advanceFuse();
        }
    }

    protected final void startFuse() {
        if (getFuseTicks() < 0) {
            setFuseTicks(0);
            getNavigation().stop();
        }
    }

    public final boolean isDetonating() {
        return getFuseTicks() >= 0 && !detonated;
    }

    private int getFuseTicks() {
        return entityData.get(FUSE_TICKS);
    }

    private void setFuseTicks(int fuseTicks) {
        entityData.set(FUSE_TICKS, fuseTicks);
    }

    private void advanceFuse() {
        int fuseTicks = getFuseTicks();
        if (fuseTicks < 0) {
            return;
        }
        int nextFuseTicks = Math.min(fuseTime, fuseTicks + fuseIncrement());
        setFuseTicks(nextFuseTicks);
        if (nextFuseTicks >= fuseTime) {
            detonate();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(FUSE_TICKS_TAG, getFuseTicks());
        tag.putByte("carrier_skin", (byte) getSkin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(FUSE_TICKS_TAG, Tag.TAG_INT)) {
            setFuseTicks(tag.getInt(FUSE_TICKS_TAG));
        }
        if (tag.contains("carrier_skin", Tag.TAG_BYTE)) {
            setSkin(tag.getByte("carrier_skin"));
        }
    }

    private void detonate() {
        if (!(level() instanceof ServerLevel) || detonated) {
            return;
        }

        detonated = true;
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(4.0D));
        Level.ExplosionInteraction interaction = level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                        && griefingEnabled() && EventHooks.canEntityGrief(level(), this)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), 4.0F, interaction);
        playSound(explosionSound(), explosionVolume(), 1.0F);
        applyExplosionEffects();
        spawnLingeringCloud();
        if (!isVariant()) {
            spawnConfiguredMobs();
        }
        if (isAlive()) {
            super.die(damageSources().mobAttack(this));
        }
        discard();
    }

    private void spawnConfiguredMobs() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (String raw : spawnTable()) {
            String[] parts = raw == null ? new String[0] : raw.split(";", -1);
            if (parts.length != 3) {
                continue;
            }
            int maximum;
            int minimum;
            try {
                maximum = Integer.parseInt(parts[1].trim());
                minimum = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (minimum < 0 || maximum < minimum) {
                continue;
            }
            ResourceLocation location = ResourceLocation.tryParse(parts[0].trim());
            if (location == null) {
                continue;
            }
            if (location.getNamespace().equals("srparasites")) {
                location = ResourceLocation.fromNamespaceAndPath("csrp", location.getPath());
            }
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(location).orElse(null);
            if (type == null) {
                continue;
            }
            int count = minimum == maximum ? minimum : minimum + random.nextInt(maximum - minimum + 1);
            for (int index = 0; index < count; index++) {
                Entity created = type.create(serverLevel);
                if (!(created instanceof Mob mob)) {
                    continue;
                }
                mob.moveTo(getX(), getY() + getBbHeight() * 0.5D + 0.5D, getZ(), getYRot(), getXRot());
                mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(mob.blockPosition()),
                        MobSpawnType.MOB_SUMMONED, null);
                mob.setTarget(getTarget());
                serverLevel.addFreshEntity(mob);
            }
        }
    }

    private void applyExplosionEffects() {
        double radius = isVariant() ? variantViralRadius() : viralRadius;
        int amplifier = isVariant() ? variantViralAmplifier() : viralAmplifier;
        int duration = isVariant() ? variantVomitDuration() : vomitDuration;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(radius), this::isValidParasiteTarget)) {
            if (isVariant() && variantExplosionDamage()) {
                target.hurt(damageSources().mobAttack(this),
                        (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
            }
            target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 400, amplifier), this);
            target.addEffect(new MobEffectInstance(ModMobEffects.VOMIT, duration, 0,
                    false, true), this);
        }
    }

    private void spawnLingeringCloud() {
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
        float radius = getBbWidth() * cloudRadiusMultiplier();
        cloud.setOwner(this);
        cloud.setRadius(radius);
        cloud.setWaitTime(cloudWaitTime());
        int duration = isVariant() ? variantCloudDuration() : cloudDuration;
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(-radius / duration);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 300,
                isVariant() ? variantPoisonAmplifier() : normalPoisonAmplifier()));
        int cloudAmplifier = isVariant() ? 2 : 0;
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, cloudAmplifier, false, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 3600, cloudAmplifier, false, false));
        level().addFreshEntity(cloud);
    }

    @Override
    protected boolean isValidParasiteTarget(LivingEntity target) {
        return super.isValidParasiteTarget(target) && !(target instanceof WaterAnimal)
                && !(target instanceof Animal);
    }

    private final class SwellGoal extends Goal {
        private SwellGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && distanceToSqr(target) < 9.0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && distanceToSqr(target) < 49.0 && !detonated;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            startFuse();
        }
    }
}
