package alku.csrp.entity;

import alku.csrp.effect.EffectStacking;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class LiceEntity extends PrimitiveParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    public static final int MAX_LIFESPAN_TICKS = 1200;
    public static final int VIRAL_DURATION_TICKS = 120;
    public static final int VIRAL_AMPLIFIER = 2;
    private final RawAnimation AGE_IN_TICKS = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");

    private int lifespan;
    private boolean charging;
    private boolean consumed;

    public LiceEntity(EntityType<? extends LiceEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.ARMOR, 5.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34559)
                .add(Attributes.FLYING_SPEED, 0.34559)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ChargeAttackGoal());
        goalSelector.addGoal(6, new RandomFlyGoal());
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    protected boolean isValidParasiteTarget(LivingEntity target) {
        return !(target instanceof WaterAnimal) && super.isValidParasiteTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide) {
            return;
        }
        if (++lifespan > MAX_LIFESPAN_TICKS) {
            expireAndDiscard();
            return;
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
        if (target.getHealth() <= target.getMaxHealth() * 0.5F) {
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
        charging = false;
        return converted || damaged;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
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
        if (consumed || !(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        consumed = true;
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                getX(), getY() + getBbHeight() * 0.5D, getZ(), 4,
                getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.08D);
        discard();
    }

    private void contactAndDiscard(boolean converted) {
        if (consumed || !(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        consumed = true;
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                getX(), getY() + getBbHeight() * 0.5D, getZ(), 4,
                getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.08D);
        if (converted) {
            serverLevel.sendParticles(ModParticles.ASSIMILATION_SPLASH.get(),
                    getX(), getY() + getBbHeight() * 0.5D, getZ(), 2,
                    getBbWidth() * 0.5D, getBbHeight() * 0.5D, getBbWidth() * 0.5D, 0.02D);
        }
        playSound(ModSounds.get("buthol.boom"), 0.4F, 1.0F);
        discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(AGE_IN_TICKS)));
    }

    private final class ChargeAttackGoal extends Goal {
        private int chargeTicks;

        private ChargeAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return !charging && !getMoveControl().hasWanted() && target != null && target.isAlive()
                    && distanceToSqr(target) > 1.0
                    && random.nextInt(7) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return charging && getMoveControl().hasWanted() && target != null && target.isAlive();
        }

        @Override
        public void start() {
            chargeTicks = 0;
            charging = true;
        }

        @Override
        public void stop() {
            charging = false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            chargeTicks++;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            Vec3 eye = target.getEyePosition();
            getMoveControl().setWantedPosition(eye.x, eye.y, eye.z, 2.0);
            if (getBoundingBox().inflate(0.15D).intersects(target.getBoundingBox())) {
                performContactAttack(target);
            }
        }
    }

    private final class RandomFlyGoal extends Goal {
        @Override
        public boolean canUse() {
            return getTarget() == null && !getMoveControl().hasWanted() && random.nextInt(7) == 0;
        }

        @Override
        public void start() {
            BlockPos origin = blockPosition();
            for (int attempt = 0; attempt < 3; attempt++) {
                BlockPos destination = origin.offset(random.nextInt(15) - 7,
                        random.nextInt(11) - 5, random.nextInt(15) - 7);
                if (level().isEmptyBlock(destination)) {
                    getMoveControl().setWantedPosition(destination.getX() + 0.5D,
                            destination.getY() + 0.5D, destination.getZ() + 0.5D, 0.25D);
                    if (getTarget() == null) {
                        getLookControl().setLookAt(destination.getX() + 0.5D,
                                destination.getY() + 0.5D, destination.getZ() + 0.5D, 180.0F, 20.0F);
                    }
                    break;
                }
            }
        }
    }
}
