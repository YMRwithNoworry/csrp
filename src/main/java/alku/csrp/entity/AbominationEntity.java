package alku.csrp.entity;

import alku.csrp.event.StatusEffectEvents;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/** Legacy Many Bodies and Giant Head close-combat abominations. */
public final class AbominationEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            AbominationEntity.class, EntityDataSerializers.INT);

    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private final RawAnimation HEAD_IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation HEAD_WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation HEAD_ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation BODIES_AGE = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation BODIES_LIMB = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation BODIES_APPROACH_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation BODIES_SPRINT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");

    private final Kind kind;
    private int supportCooldown = 10;

    public AbominationEntity(EntityType<? extends AbominationEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.experience;
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 17.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new FastMeleeAttackGoal(this, 1.3D));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || activeKind() != Kind.BODIES) {
            return;
        }
        if (--supportCooldown > 0) {
            return;
        }
        applyBodiesSupport();
        supportCooldown = 240;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    protected int incomingDamageCapDivisor() {
        return activeKind() == Kind.BODIES && level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).damageCap() ? 4 : 1;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return activeKind() == Kind.BODIES && entityData.get(PARASITE_STATUS) != 0
                ? null : super.getAmbientSound();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return activeKind() == Kind.BODIES ? ModSounds.get("bodies.growl") : null;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(ModSounds.get("small.step"), getSoundVolume(), getVoicePitch());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && activeKind() == Kind.HEAD) {
            triggerAnim("attack_controller", "attack");
        }
        return hurt;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (activeKind() == Kind.BODIES) {
            controllers.add(new AnimationController<>(this, "age_controller", 0,
                    state -> state.setAndContinue(BODIES_AGE)));
            controllers.add(new AnimationController<>(this, "movement_controller", 4,
                    this::bodiesMovementAnimation));
            return;
        }
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::headMovementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", HEAD_ATTACK));
    }

    public Kind getKind() {
        return activeKind();
    }

    private Kind activeKind() {
        return kind == null ? Kind.HEAD : kind;
    }

    private PlayState headMovementAnimation(AnimationState<AbominationEntity> state) {
        return state.setAndContinue(ParasiteAnimations.isMoving(this, state.isMoving()) ? HEAD_WALK : HEAD_IDLE);
    }

    private PlayState bodiesMovementAnimation(AnimationState<AbominationEntity> state) {
        if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
            return PlayState.STOP;
        }
        return state.setAndContinue(switch (entityData.get(PARASITE_STATUS)) {
            case 1 -> BODIES_APPROACH_LIMB;
            case 2 -> BODIES_SPRINT_LIMB;
            default -> BODIES_LIMB;
        });
    }

    private void applyBodiesSupport() {
        for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(20.0D),
                entity -> entity != this && entity instanceof Parasite)) {
            if (ally instanceof NexusParasiteEntity nexus
                    && nexus.getKind().name().startsWith("ROOTER")) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(ModMobEffects.PIVOT, 300, 0, false, false), this);
            ally.addEffect(new MobEffectInstance(ModMobEffects.PARATE, 300, 0, false, false), this);
            StatusEffectEvents.linkToRooter(ally, this);
        }
    }

    public enum Kind {
        BODIES(0.211037D, 24),
        HEAD(0.272037D, 75);

        private final double movementSpeed;
        private final int experience;

        Kind(double movementSpeed, int experience) {
            this.movementSpeed = movementSpeed;
            this.experience = experience;
        }
    }

    private static final class FastMeleeAttackGoal extends MeleeAttackGoal {
        private final AbominationEntity parasite;

        private FastMeleeAttackGoal(PathfinderMob mob, double speedModifier) {
            super(mob, speedModifier, false);
            parasite = (AbominationEntity) mob;
        }

        @Override
        public void start() {
            super.start();
            updateParasiteStatus();
        }

        @Override
        public void stop() {
            parasite.entityData.set(PARASITE_STATUS, 0);
            super.stop();
        }

        @Override
        public void tick() {
            updateParasiteStatus();
            super.tick();
        }

        @Override
        protected int getTicksUntilNextAttack() {
            return 3;
        }

        private void updateParasiteStatus() {
            LivingEntity target = parasite.getTarget();
            if (target == null) {
                parasite.entityData.set(PARASITE_STATUS, 0);
                return;
            }
            double reach = parasite.getBbWidth() * 2.0D + target.getBbWidth();
            parasite.entityData.set(PARASITE_STATUS,
                    parasite.distanceToSqr(target) > reach * reach ? 2 : 1);
        }
    }
}
