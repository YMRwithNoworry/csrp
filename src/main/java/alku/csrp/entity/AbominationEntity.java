package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private final Kind kind;
    private int supportCooldown;
    private int attackAnimationTicks;

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
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new FastMeleeAttackGoal(this, 1.3D));
    }

    @Override
    public void tick() {
        super.tick();
        if (attackAnimationTicks > 0) {
            attackAnimationTicks--;
        }
        if (level().isClientSide || activeKind() != Kind.BODIES) {
            return;
        }
        if (supportCooldown > 0) {
            supportCooldown--;
            return;
        }
        applyBodiesSupport();
        supportCooldown = 200;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            attackAnimationTicks = 8;
        }
        return hurt;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    public Kind getKind() {
        return activeKind();
    }

    private Kind activeKind() {
        return kind == null ? Kind.HEAD : kind;
    }

    private PlayState movementAnimation(AnimationState<AbominationEntity> state) {
        if (attackAnimationTicks > 0) {
            return state.setAndContinue(ATTACK);
        }
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    private void applyBodiesSupport() {
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3, false, false), this);
        for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16.0D),
                entity -> entity != this && entity instanceof Parasite)) {
            if (ally instanceof NexusParasiteEntity nexus
                    && nexus.getKind().name().startsWith("ROOTER")) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 300, 0, false, false), this);
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0, false, false), this);
        }
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    16, 1.5D, 1.0D, 1.5D, 0.02D);
        }
    }

    public enum Kind {
        BODIES(0.211037D, 30),
        HEAD(0.272037D, 10);

        private final double movementSpeed;
        private final int experience;

        Kind(double movementSpeed, int experience) {
            this.movementSpeed = movementSpeed;
            this.experience = experience;
        }
    }

    private static final class FastMeleeAttackGoal extends MeleeAttackGoal {
        private FastMeleeAttackGoal(PathfinderMob mob, double speedModifier) {
            super(mob, speedModifier, false);
        }

        @Override
        protected int getTicksUntilNextAttack() {
            return 4;
        }
    }
}
