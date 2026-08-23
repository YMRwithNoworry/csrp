package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class SummonerEntity extends PrimitiveParasiteEntity implements SummonCapacityOwner {
    private static final byte VOMIT_EVENT = 100;
    private static final byte SUMMON_EVENT = 101;
    private static final int VOMIT_COOLDOWN_TICKS = 180;
    private static final int SUMMON_COOLDOWN_TICKS = 200;
    private static final int TOTAL_SUMMON_CAPACITY = 4;
    private static final int SUMMON_LIMIT = 2;
    private static final EntityDataAccessor<Boolean> SUMMONING = SynchedEntityData.defineId(
            SummonerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SUMMON_TICKS = SynchedEntityData.defineId(
            SummonerEntity.class, EntityDataSerializers.INT);

    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");
    private final RawAnimation COMBAT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation COMBAT_STILL = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation SPRINT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation SUMMON = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");

    private final SummonCapacityTracker summonTracker = new SummonCapacityTracker();
    private int summonCooldown = SUMMON_COOLDOWN_TICKS;
    private int vomitTicks;

    public SummonerEntity(EntityType<? extends SummonerEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new SummonGoal());
        goalSelector.addGoal(2, new VomitGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, false));
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (summonCooldown > 0) summonCooldown--;
            if (tickCount % 20 == 0 && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                summonTracker.prune(serverLevel);
            }
        }
        if (level().isClientSide && vomitTicks > 0) {
            vomitTicks--;
            spawnVomitParticles();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == VOMIT_EVENT) {
            vomitTicks = 40;
        } else if (id == SUMMON_EVENT) {
            spawnSummonParticles();
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void spawnVomitParticles() {
        Vec3 direction = getViewVector(1.0F);
        Vec3 start = getEyePosition().add(direction.scale(1.2D));
        for (int index = 0; index < 6; index++) {
            level().addParticle(ParticleTypes.WITCH, start.x, start.y - 0.2D, start.z,
                    direction.x * 0.2D + (random.nextDouble() - 0.5D) * 0.25D,
                    0.01D + random.nextDouble() * 0.1D,
                    direction.z * 0.2D + (random.nextDouble() - 0.5D) * 0.25D);
        }
    }

    private void spawnSummonParticles() {
        for (int index = 0; index < 11; index++) {
            level().addParticle(ModParticles.BIOMASS.get(),
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight(),
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    (random.nextDouble() - 0.5D) * 0.08D,
                    random.nextDouble() * 0.08D,
                    (random.nextDouble() - 0.5D) * 0.08D);
        }
    }

    private boolean summonBiomass() {
        return BiomassEntity.spawnFromVomit(this, this, 5, List.of(
                new BiomassEntity.SummonOption(ModEntities.RUPTER.get(), 1.0D, 1)));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SUMMONING, false);
        entityData.define(SUMMON_TICKS, 0);
    }

    public boolean isSummoning() {
        return entityData.get(SUMMONING);
    }

    public int getSummonTicks() {
        return entityData.get(SUMMON_TICKS);
    }

    @Override
    public int getSummonCapacity() {
        return TOTAL_SUMMON_CAPACITY;
    }

    @Override
    public int getUsedSummonCapacity() {
        return summonTracker.usedCapacity();
    }

    @Override
    public void reserveTrackedSummon(UUID entityId, int cost) {
        summonTracker.reserve(entityId, cost);
    }

    @Override
    public void replaceTrackedSummon(UUID previousId, UUID replacementId, int cost) {
        summonTracker.replace(previousId, replacementId, cost);
    }

    @Override
    public void releaseTrackedSummon(UUID entityId) {
        summonTracker.release(entityId);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("summoner_summon_cooldown", summonCooldown);
        summonTracker.save(tag, "summoner_tracked_summons");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        summonCooldown = tag.contains("summoner_summon_cooldown")
                ? tag.getInt("summoner_summon_cooldown") : SUMMON_COOLDOWN_TICKS;
        summonTracker.load(tag, "summoner_tracked_summons");
        entityData.set(SUMMONING, false);
        entityData.set(SUMMON_TICKS, 0);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (isSummoning()) {
                return state.setAndContinue(SUMMON);
            }
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                LivingEntity target = getTarget();
                return state.setAndContinue(target != null && target.isAlive() ? COMBAT_STILL : AGE_IN_TICKS);
            }
            return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02D
                    ? SPRINT_LIMB : getTarget() != null ? COMBAT_LIMB : LIMB_SWING);
        }));
    }

    private final class SummonGoal extends Goal {
        private static final int SPAWN_INTERVAL = 20;
        private int castTicks;
        private int successfulSummons;
        private int failedSummons;

        public SummonGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return summonCooldown <= 0 && getTarget() != null && distanceToSqr(getTarget()) <= 256.0D
                    && !isInWaterOrBubble();
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() != null && getTarget().isAlive() && !isInWaterOrBubble()
                    && successfulSummons < SUMMON_LIMIT && failedSummons <= 4;
        }

        @Override
        public void start() {
            castTicks = 0;
            successfulSummons = 0;
            failedSummons = 0;
            getNavigation().stop();
            entityData.set(SUMMONING, true);
            entityData.set(SUMMON_TICKS, 0);
        }

        @Override
        public void tick() {
            castTicks++;
            entityData.set(SUMMON_TICKS, castTicks);

            if (castTicks % SPAWN_INTERVAL == 0) {
                if (getUsedSummonCapacity() < getSummonCapacity()) {
                    playSound(ModSounds.get("canra.special"), 3.0F, 1.0F);
                }
                if (summonBiomass()) {
                    successfulSummons++;
                    level().broadcastEntityEvent(SummonerEntity.this, SUMMON_EVENT);
                } else {
                    failedSummons++;
                }
            }

            // 面向目标
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }

        @Override
        public void stop() {
            entityData.set(SUMMONING, false);
            entityData.set(SUMMON_TICKS, 0);
            summonCooldown = SUMMON_COOLDOWN_TICKS;
        }
    }

    private final class VomitGoal extends Goal {
        private int cooldown;

        private VomitGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return !isSummoning() && target != null && target.isAlive() && hasLineOfSight(target)
                    && distanceToSqr(target) >= 16.0D && distanceToSqr(target) <= 64.0D;
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
            ParasiteCombatEffects.spawnVomitCloud(SummonerEntity.this,
                    5.5D, 4.0F, 100, 300, 25);
            level().broadcastEntityEvent(SummonerEntity.this, VOMIT_EVENT);
            cooldown = VOMIT_COOLDOWN_TICKS;
        }
    }
}
