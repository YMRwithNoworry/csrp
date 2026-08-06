package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Detached dragon head retains its fireball attack after the body has been decapitated. */
public final class AssimilatedDragonHeadEntity extends Monster implements GeoEntity, Parasite {
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int fireballCooldown;

    public AssimilatedDragonHeadEntity(EntityType<? extends AssimilatedDragonHeadEntity> type, Level level) {
        super(type, level);
        xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 78.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true,
                target -> target instanceof net.minecraft.world.entity.player.Player));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (fireballCooldown > 0) fireballCooldown--;
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && fireballCooldown <= 0 && hasLineOfSight(target)) {
            launchFireball(target);
            fireballCooldown = 30;
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        LivingEntity livingTarget = entity instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(entity);
        if (hit) {
            triggerAnim("attack_controller", "attack");
        }
        if (hit && livingTarget != null) {
            ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
            InfectionMechanics.applyCoth(livingTarget, this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return super.causeFallDamage(distance, damageMultiplier * 0.3F, source);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private void launchFireball(LivingEntity target) {
        triggerAnim("attack_controller", "attack");
        Vec3 source = getEyePosition().add(getLookAngle());
        Vec3 direction = target.getEyePosition().subtract(source);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        direction = direction.normalize();
        Vec3 impact = source.add(direction.scale(Math.min(32.0D, Math.sqrt(distanceToSqr(target)))));
        AreaEffectCloud cloud = new AreaEffectCloud(level(), impact.x, impact.y, impact.z);
        cloud.setOwner(this);
        cloud.setRadius(2.5F);
        cloud.setDuration(80);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 240, 0, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 120, 0, false, true));
        level().addFreshEntity(cloud);
        AABB breathArea = getBoundingBox().expandTowards(direction.scale(24.0D)).inflate(1.5D);
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), breathArea);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                breathArea,
                entity -> entity != this && entity.isAlive() && !(entity instanceof Parasite))) {
            if (hasLineOfSight(victim)) {
                victim.hurt(damageSources().indirectMagic(this, this), 12.0F);
                break;
            }
        }
    }
}
