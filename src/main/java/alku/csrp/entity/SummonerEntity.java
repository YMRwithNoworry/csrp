package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class SummonerEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Boolean> SUMMONING = SynchedEntityData.defineId(
            SummonerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SUMMON_TICKS = SynchedEntityData.defineId(
            SummonerEntity.class, EntityDataSerializers.INT);

    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation RUN = ParasiteAnimations.loop(this, "run");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation SUMMON = ParasiteAnimations.loop(this, "summon");

    private int summonCooldown = 200;

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
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, false));
    }

    @Override public void tick() {
        super.tick();
        if (summonCooldown > 0) summonCooldown--;
    }

    private void summonMinions() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        for (int i = 0; i < 3; i++) {
            GnatEntity gnat = ModEntities.GNAT.get().create(serverLevel);
            if (gnat == null) continue;
            double angle = Math.PI * 2.0 * i / 3.0;
            gnat.moveTo(getX() + Math.cos(angle) * 2.0, getY() + 0.5, getZ() + Math.sin(angle) * 2.0,
                    getYRot(), 0.0F);
            gnat.setTarget(getTarget());
            serverLevel.addFreshEntity(gnat);
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            ScaryOrbEntity orb = new ScaryOrbEntity(ModEntities.SCARY_ORB.get(), serverLevel, this);
            Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.75D));
            orb.launch(start, target.getEyePosition(), target);
            serverLevel.addFreshEntity(orb);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SUMMONING, false);
        builder.define(SUMMON_TICKS, 0);
    }

    public boolean isSummoning() {
        return entityData.get(SUMMONING);
    }

    public int getSummonTicks() {
        return entityData.get(SUMMON_TICKS);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("attack_controller", "attack");
        }
        return hit;
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (isSummoning()) {
                return state.setAndContinue(SUMMON);
            }
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                return state.setAndContinue(IDLE);
            }
            return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() > 0.02 ? RUN : WALK);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    private final class SummonGoal extends Goal {
        private static final int SUMMON_DURATION = 80;
        private static final int SPAWN_INTERVAL = 20;
        private static final int MAX_SPAWNS = 4;
        private int castTicks;
        private int spawnCount;

        public SummonGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return summonCooldown == 0 && getTarget() != null && distanceToSqr(getTarget()) <= 256.0;
        }

        @Override
        public boolean canContinueToUse() {
            return castTicks < SUMMON_DURATION && spawnCount < MAX_SPAWNS;
        }

        @Override
        public void start() {
            castTicks = 0;
            spawnCount = 0;
            getNavigation().stop();
            entityData.set(SUMMONING, true);
            entityData.set(SUMMON_TICKS, 0);
        }

        @Override
        public void tick() {
            castTicks++;
            entityData.set(SUMMON_TICKS, castTicks);

            // 每20 ticks生成一波寄生体
            if (castTicks >= 40 && castTicks % SPAWN_INTERVAL == 0 && spawnCount < MAX_SPAWNS) {
                summonMinions();
                spawnCount++;
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
            summonCooldown = 200;
        }
    }
}
