package alku.csrp.entity;

import alku.csrp.effect.EffectStacking;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public final class GnatEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Byte> CLIMBING =
            SynchedEntityData.defineId(GnatEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> COMBAT_STATUS =
            SynchedEntityData.defineId(GnatEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int MAX_LIFETIME_TICKS = 1_200;
    private static final float HIJACK_HEALTH_FRACTION = 0.5F;
    private static final int VIRAL_DURATION_TICKS = 120;
    private static final int VIRAL_AMPLIFIER = 2;

    private boolean consumed;

    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks");
    private final RawAnimation LIMB_SWING = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing");
    private final RawAnimation COMBAT_AGE = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation COMBAT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation SPRINT_LIMB = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_10");

    public GnatEntity(EntityType<? extends GnatEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ARMOR, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.MOVEMENT_SPEED, 0.34559)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, createAnimatedLeapGoal(0.4F, 20));
        goalSelector.addGoal(2, new FastMeleeAttackGoal());
        goalSelector.addGoal(0, new SwimmingDivingGoal());
    }

    @Override
    protected boolean isValidParasiteTarget(LivingEntity target) {
        return !(target instanceof WaterAnimal) && super.isValidParasiteTarget(target);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, (byte) 0);
        builder.define(COMBAT_STATUS, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || consumed) {
            return;
        }

        LivingEntity target = getTarget();
        entityData.set(COMBAT_STATUS, target != null && target.isAlive());
        if (tickCount > MAX_LIFETIME_TICKS) {
            expireAndDiscard();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public void push(Entity entity) {
        super.push(entity);
        if (entity == getTarget()) {
            performContactAttack(entity);
        }
    }

    private boolean performContactAttack(Entity entity) {
        if (level().isClientSide || consumed || entity != getTarget()
                || !(entity instanceof LivingEntity target)
                || target instanceof Parasite || !target.isAlive()) {
            return false;
        }

        boolean converted = false;
        if (target.getHealth() <= target.getMaxHealth() * HIJACK_HEALTH_FRACTION) {
            converted = InfectionMechanics.convertFeralEndermanHost(target)
                    || InfectionMechanics.convertGnatHost(target);
        }
        boolean damaged = false;
        if (!converted) {
            damaged = target.hurt(damageSources().mobAttack(this),
                    (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        EffectStacking.apply(target, ModMobEffects.VIRAL, VIRAL_DURATION_TICKS, VIRAL_AMPLIFIER);
        contactAndDiscard(converted);
        return converted || damaged;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return distance >= 60.0F && super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    public void die(DamageSource source) {
        if (consumed) {
            return;
        }
        super.die(source);
        expireAndDiscard();
    }

    private void expireAndDiscard() {
        if (consumed || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        consumed = true;
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                getX(), getY() + getBbHeight() * 0.5D, getZ(), 4,
                getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.08D);
        discard();
    }

    private void contactAndDiscard(boolean converted) {
        if (consumed || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                getX(), getY() + getBbHeight() * 0.5D, getZ(), 4,
                getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.08D);
        if (converted) {
            serverLevel.sendParticles(ModParticles.ASSIMILATION_SPLASH.get(),
                    getX(), getY() + getBbHeight() * 0.5D, getZ(), 2,
                    getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.02D);
        }
        playSound(ModSounds.get("buthol.boom"), 0.3F, 0.9F + random.nextFloat() * 0.2F);
        consumed = true;
        discard();
    }

    @Override
    public boolean onClimbable() {
        return (entityData.get(CLIMBING) & 1) != 0;
    }

    private final class FastMeleeAttackGoal extends MeleeAttackGoal {
        private FastMeleeAttackGoal() {
            super(GnatEntity.this, 1.3D, false);
        }

        @Override
        protected int getTicksUntilNextAttack() {
            return 6;
        }
    }

    private final class SwimmingDivingGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private SwimmingDivingGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE));
            getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            if (!isInWaterOrBubble()) {
                return false;
            }
            LivingEntity target = getTarget();
            if (target != null && target.isInWaterOrBubble()
                    && distanceToSqr(getX(), target.getY(), getZ()) < 25.0D
                    && target.getY() - getY() < -1.0D) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.12D, 0.0D));
                return false;
            }
            return true;
        }

        @Override
        public void tick() {
            if (random.nextFloat() < 0.8F) {
                getJumpControl().jump();
            }
        }
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 3,
                state -> {
                    if (isSpecialLeapAnimating()) {
                        return state.setAndContinue(LEAP);
                    }
                    boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
                    if (!moving) {
                        return state.setAndContinue(entityData.get(COMBAT_STATUS) ? COMBAT_AGE : AGE_IN_TICKS);
                    }
                    if (getDeltaMovement().horizontalDistanceSqr() > 0.02D) {
                        return state.setAndContinue(SPRINT_LIMB);
                    }
                    return state.setAndContinue(entityData.get(COMBAT_STATUS) ? COMBAT_LIMB : LIMB_SWING);
                }));
    }
}
