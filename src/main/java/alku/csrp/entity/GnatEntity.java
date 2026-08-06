package alku.csrp.entity;

import alku.csrp.effect.EffectStacking;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public final class GnatEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Byte> CLIMBING =
            SynchedEntityData.defineId(GnatEntity.class, EntityDataSerializers.BYTE);
    private static final int MAX_LIFETIME_TICKS = 1_200;
    private static final float HIJACK_HEALTH_FRACTION = 0.5F;
    private static final int VIRAL_DURATION_TICKS = 120;
    private static final int VIRAL_AMPLIFIER = 2;

    private boolean consumed;

    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final RawAnimation LEAP = ParasiteAnimations.loop(this, "idle.get_parasite_status_10");

    public GnatEntity(EntityType<? extends GnatEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ARMOR, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.MOVEMENT_SPEED, 0.34559)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, createAnimatedLeapGoal(0.4F, 20));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || consumed) {
            return;
        }

        setClimbing(horizontalCollision);
        pursueInLiquid();
        if (tickCount > MAX_LIFETIME_TICKS) {
            burstAndDiscard(false);
        }
    }

    private void pursueInLiquid() {
        if (!isInWaterOrBubble() || tickCount % 6 != 0) {
            return;
        }

        Vec3 movement = getDeltaMovement();
        LivingEntity target = getTarget();
        if (target != null && target.isInWaterOrBubble()) {
            Vec3 pursuit = target.getEyePosition().subtract(getEyePosition());
            if (pursuit.lengthSqr() > 0.001D) {
                movement = movement.add(pursuit.normalize().scale(0.16D));
            }
        } else {
            movement = new Vec3(movement.x * 1.2D, Math.max(movement.y, 0.18D), movement.z * 1.2D);
        }
        setDeltaMovement(movement);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return performContactAttack(target);
    }

    @Override
    public void push(Entity entity) {
        super.push(entity);
        performContactAttack(entity);
    }

    private boolean performContactAttack(Entity entity) {
        if (level().isClientSide || consumed || !(entity instanceof LivingEntity target)
                || target instanceof Parasite || !target.isAlive()) {
            return false;
        }

        boolean converted = target.getHealth() <= target.getMaxHealth() * HIJACK_HEALTH_FRACTION
                && InfectionMechanics.convertGnatHost(target);
        boolean damaged = false;
        if (!converted) {
            damaged = target.hurt(damageSources().mobAttack(this),
                    (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            EffectStacking.apply(target, ModMobEffects.VIRAL, VIRAL_DURATION_TICKS, VIRAL_AMPLIFIER);
            if (!target.isAlive()) {
                InfectionMechanics.convertGnatHost(target);
            }
        }
        triggerAnim("attack_controller", "attack");
        burstAndDiscard(true);
        return converted || damaged;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public void die(DamageSource source) {
        if (consumed) {
            return;
        }
        super.die(source);
        burstAndDiscard(false);
    }

    private void burstAndDiscard(boolean playSound) {
        if (consumed || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        consumed = true;
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                getX(), getY() + getBbHeight() * 0.5D, getZ(), 18,
                getBbWidth() * 0.65D, getBbHeight() * 0.45D, getBbWidth() * 0.65D, 0.12D);
        serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                getX(), getY() + getBbHeight() * 0.5D, getZ(), 24,
                getBbWidth(), getBbHeight() * 0.5D, getBbWidth(), 0.08D);
        if (playSound) {
            playSound(ModSounds.MOB_EXPLOSION.get(), 0.3F, 0.9F + random.nextFloat() * 0.2F);
        }
        discard();
    }

    @Override
    public boolean onClimbable() {
        return (entityData.get(CLIMBING) & 1) != 0;
    }

    private void setClimbing(boolean climbing) {
        byte value = entityData.get(CLIMBING);
        entityData.set(CLIMBING, climbing ? (byte) (value | 1) : (byte) (value & -2));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 3,
                state -> state.setAndContinue(isSpecialLeapAnimating() ? LEAP : getDeltaMovement().horizontalDistanceSqr() >= 0.0001 ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }
}
