package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BiomassEntity extends Monster implements GeoEntity, Parasite {
    public static final int HATCH_FUSE_TICKS = 80;
    private static final int DEFAULT_FUSE_TICKS = 777;
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(
            BiomassEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> STAGE = SynchedEntityData.defineId(
            BiomassEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(
            BiomassEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GROWTH_TICKS = SynchedEntityData.defineId(
            BiomassEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CAPACITY_COST = SynchedEntityData.defineId(
            BiomassEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> SPAWN_TYPE = SynchedEntityData.defineId(
            BiomassEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> PARENT = SynchedEntityData.defineId(
            BiomassEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> TARGET = SynchedEntityData.defineId(
            BiomassEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation idleAnimation = RawAnimation.begin().thenLoop("animation.biomass.idle");
    private boolean hatchHandled;

    public BiomassEntity(EntityType<? extends BiomassEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D);
    }

    public static boolean spawnFromVomit(Mob summoner, SummonCapacityOwner owner, int skin,
                                         List<SummonOption> options) {
        if (!(summoner.level() instanceof ServerLevel level) || options.isEmpty()) {
            return false;
        }
        Optional<SummonOption> selected = selectSummon(summoner, owner, options);
        if (selected.isEmpty()) {
            return false;
        }

        float angle = summoner.getYRot() * Mth.DEG_TO_RAD - summoner.yBodyRot * 0.01F;
        double distance = 3.0D * Mth.cos((float) Math.PI / 18.0F);
        Vec3 spawnPosition = summoner.position().add(-Mth.sin(angle) * distance,
                summoner.getEyeHeight(), Mth.cos(angle) * distance);
        if (!level.isEmptyBlock(net.minecraft.core.BlockPos.containing(spawnPosition))) {
            return false;
        }
        return spawnBiomass(level, summoner, owner, null, selected.get(), skin,
                summoner.getTarget(), spawnPosition, summoner.getYRot(), summoner.getXRot());
    }

    public static Optional<SummonOption> selectSummon(Mob summoner, SummonCapacityOwner owner,
                                                       List<SummonOption> options) {
        if (options.isEmpty()) {
            return Optional.empty();
        }
        int index = summoner.getRandom().nextInt(options.size());
        int wraps = 0;
        while (true) {
            if (index >= options.size()) {
                index = 0;
                wraps++;
            }
            if (wraps == 2) {
                return Optional.empty();
            }
            SummonOption option = options.get(index);
            if (summoner.getRandom().nextDouble() > option.chance() || !owner.canReserveSummon(option.cost())) {
                index++;
                continue;
            }
            return Optional.of(option);
        }
    }

    public static boolean spawnFromProjectile(Mob summoner, SummonCapacityOwner owner, UUID reservationId,
                                              SummonOption option, int skin, LivingEntity target,
                                              Vec3 position, float yaw, float pitch) {
        if (!(summoner.level() instanceof ServerLevel level)) {
            owner.releaseTrackedSummon(reservationId);
            return false;
        }
        return spawnBiomass(level, summoner, owner, reservationId, option, skin, target,
                position, yaw, pitch);
    }

    private static boolean spawnBiomass(ServerLevel level, Mob summoner, SummonCapacityOwner owner,
                                        UUID reservationId, SummonOption option, int skin,
                                        LivingEntity target, Vec3 position, float yaw, float pitch) {
        BiomassEntity biomass = ModEntities.BIOMASS.get().create(level);
        if (biomass == null) {
            if (reservationId != null) {
                owner.releaseTrackedSummon(reservationId);
            }
            return false;
        }
        biomass.configure(summoner, option.type(), option.cost(), skin, target);
        biomass.moveTo(position.x, position.y, position.z, yaw, pitch);
        if (summoner.isOnFire()) {
            biomass.igniteForSeconds(8.0F);
        }
        if (!level.addFreshEntity(biomass)) {
            if (reservationId != null) {
                owner.releaseTrackedSummon(reservationId);
            }
            return false;
        }
        if (reservationId == null) {
            owner.reserveTrackedSummon(biomass.getUUID(), option.cost());
        } else {
            owner.replaceTrackedSummon(reservationId, biomass.getUUID(), option.cost());
        }
        return true;
    }

    private void configure(Mob parent, EntityType<? extends Mob> spawnType, int cost, int skin,
                           LivingEntity target) {
        entityData.set(PARENT, Optional.of(parent.getUUID()));
        entityData.set(TARGET, target == null ? Optional.empty() : Optional.of(target.getUUID()));
        entityData.set(SPAWN_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(spawnType).toString());
        entityData.set(CAPACITY_COST, cost);
        entityData.set(SKIN, Mth.clamp(skin, 1, 6));
        entityData.set(STAGE, 1.0F);
        entityData.set(FUSE, HATCH_FUSE_TICKS);
        entityData.set(GROWTH_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnClientParticles();
            return;
        }
        if (getHealth() < getMaxHealth()) {
            setHealth(Math.min(getMaxHealth(), getHealth() + 0.1F));
        }
        if (tickCount % 10 == 0) {
            applyCothAura();
        }

        int decrements = 0;
        if (onGround()) {
            decrements++;
            entityData.set(GROWTH_TICKS, entityData.get(GROWTH_TICKS) + 1);
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        }
        if (tickCount >= 200) {
            decrements++;
        }
        if (decrements > 0) {
            int fuse = entityData.get(FUSE) - decrements;
            entityData.set(FUSE, fuse);
            if (fuse <= 0 && !hatchHandled && level() instanceof ServerLevel serverLevel) {
                hatch(serverLevel);
            }
        }
    }

    private void spawnClientParticles() {
        int stage = Mth.clamp((int) getStage(), 1, 6);
        int interval = stage == 3 ? 3 : stage == 2 || stage == 6 ? 5 : 10;
        int count = stage == 3 ? 3 : 2;
        if (tickCount % interval != 0) {
            return;
        }
        for (int index = 0; index < count; index++) {
            double x = getX() + (random.nextDouble() - 0.5D) * getBbWidth();
            double y = getY() + random.nextDouble() * getBbHeight();
            double z = getZ() + (random.nextDouble() - 0.5D) * getBbWidth();
            level().addParticle(ModParticles.BIOMASS.get(), x, y, z,
                    (random.nextDouble() - 0.5D) * 0.04D,
                    random.nextDouble() * 0.025D,
                    (random.nextDouble() - 0.5D) * 0.04D);
        }
    }

    private void applyCothAura() {
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(2.0D), living -> living != this && !(living instanceof Parasite))) {
            InfectionMechanics.applyCothEffect(living, this, 200, 1, false, false);
        }
    }

    private void hatch(ServerLevel level) {
        hatchHandled = true;
        applyCothAura();
        playSound(ModSounds.get("flesh.primitive"), 1.0F, 1.0F);
        level.sendParticles(ModParticles.BIOMASS.get(), getX(), getY() + 0.5D, getZ(),
                11, 0.5D, 0.5D, 0.5D, 0.15D);

        Mob parent = resolveMob(level, entityData.get(PARENT));
        ResourceLocation spawnTypeId = ResourceLocation.tryParse(entityData.get(SPAWN_TYPE));
        Entity created = spawnTypeId == null ? null
                : BuiltInRegistries.ENTITY_TYPE.getOptional(spawnTypeId).map(type -> type.create(level)).orElse(null);
        if (!(created instanceof Mob spawned)) {
            releaseReservation(parent);
            discard();
            return;
        }

        float yaw = parent == null ? getYRot() : parent.getYRot();
        float pitch = parent == null ? getXRot() : parent.getXRot();
        spawned.moveTo(getX(), getY(), getZ(), yaw, pitch);
        spawned.finalizeSpawn(level, level.getCurrentDifficultyAt(spawned.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        AttributeInstance followRange = spawned.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.setBaseValue(16.0D + (getStage() - 1.0F) * 8.0D);
        }
        spawned.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 1, false, false), this);
        spawned.addEffect(new MobEffectInstance(ModMobEffects.DEBAR, 120000, 1, false, false), this);
        if (isOnFire()) {
            spawned.setHealth(spawned.getMaxHealth() * 0.5F);
            spawned.igniteForSeconds(8.0F);
        }
        LivingEntity target = resolveLiving(level, entityData.get(TARGET));
        if (target != null && target.isAlive()) {
            spawned.setTarget(target);
        }
        if (!level.addFreshEntity(spawned)) {
            releaseReservation(parent);
            discard();
            return;
        }
        if (parent instanceof SummonCapacityOwner owner) {
            owner.replaceTrackedSummon(getUUID(), spawned.getUUID(), entityData.get(CAPACITY_COST));
        }
        discard();
    }

    private void releaseReservation(Mob parent) {
        if (parent instanceof SummonCapacityOwner owner) {
            owner.releaseTrackedSummon(getUUID());
        }
    }

    private static Mob resolveMob(ServerLevel level, Optional<UUID> id) {
        Entity entity = id.map(level::getEntity).orElse(null);
        return entity instanceof Mob mob ? mob : null;
    }

    private static LivingEntity resolveLiving(ServerLevel level, Optional<UUID> id) {
        Entity entity = id.map(level::getEntity).orElse(null);
        return entity instanceof LivingEntity living ? living : null;
    }

    public int getSkin() {
        return entityData.get(SKIN);
    }

    public float getStage() {
        return entityData.get(STAGE);
    }

    public int getFuse() {
        return entityData.get(FUSE);
    }

    public float getGrowthWidth(float partialTick) {
        return (entityData.get(GROWTH_TICKS) + partialTick) * growthPerTick(false);
    }

    public float getGrowthHeight(float partialTick) {
        return (entityData.get(GROWTH_TICKS) + partialTick) * growthPerTick(true);
    }

    private float growthPerTick(boolean height) {
        return switch (Mth.clamp((int) getStage(), 1, 6)) {
            case 2, 4, 6 -> height ? 0.012F : 0.010F;
            case 3 -> 0.001F;
            default -> height ? 0.006F : 0.005F;
        };
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if ((attacker instanceof Parasite && attacker != this)
                || (direct instanceof Parasite && direct != this)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return distance >= 18.0F && super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 1);
        builder.define(STAGE, 1.0F);
        builder.define(FUSE, DEFAULT_FUSE_TICKS);
        builder.define(GROWTH_TICKS, 0);
        builder.define(CAPACITY_COST, 0);
        builder.define(SPAWN_TYPE, "");
        builder.define(PARENT, Optional.empty());
        builder.define(TARGET, Optional.empty());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("biomass_skin", getSkin());
        tag.putFloat("biomass_stage", getStage());
        tag.putInt("biomass_fuse", getFuse());
        tag.putInt("biomass_growth_ticks", entityData.get(GROWTH_TICKS));
        tag.putInt("biomass_capacity_cost", entityData.get(CAPACITY_COST));
        tag.putString("biomass_spawn_type", entityData.get(SPAWN_TYPE));
        entityData.get(PARENT).ifPresent(id -> tag.putUUID("biomass_parent", id));
        entityData.get(TARGET).ifPresent(id -> tag.putUUID("biomass_target", id));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SKIN, Mth.clamp(tag.getInt("biomass_skin"), 1, 6));
        entityData.set(STAGE, tag.contains("biomass_stage") ? tag.getFloat("biomass_stage") : 1.0F);
        entityData.set(FUSE, tag.contains("biomass_fuse") ? tag.getInt("biomass_fuse") : DEFAULT_FUSE_TICKS);
        entityData.set(GROWTH_TICKS, Math.max(0, tag.getInt("biomass_growth_ticks")));
        entityData.set(CAPACITY_COST, Math.max(0, tag.getInt("biomass_capacity_cost")));
        entityData.set(SPAWN_TYPE, tag.getString("biomass_spawn_type"));
        entityData.set(PARENT, tag.hasUUID("biomass_parent")
                ? Optional.of(tag.getUUID("biomass_parent")) : Optional.empty());
        entityData.set(TARGET, tag.hasUUID("biomass_target")
                ? Optional.of(tag.getUUID("biomass_target")) : Optional.empty());
        hatchHandled = false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_controller", 0,
                state -> state.setAndContinue(idleAnimation)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public record SummonOption(EntityType<? extends Mob> type, double chance, int cost) {
    }
}
