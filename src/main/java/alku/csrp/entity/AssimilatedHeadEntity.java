package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Shared walking-head behavior: infect targets and rebuild a body with a medium incomplete form. */
public final class AssimilatedHeadEntity extends Monster implements GeoEntity, Parasite {
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Kind kind;

    public AssimilatedHeadEntity(EntityType<? extends AssimilatedHeadEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.experience;
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.FOLLOW_RANGE, kind.followRange);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (kind == Kind.ENDERMAN) {
                spawnPortalParticles();
            }
            return;
        }
        if (kind == Kind.ENDERMAN && tickCount % 20 == 0 && getTarget() != null
                && distanceToSqr(getTarget()) > 4.0D && random.nextInt(3) == 0) {
            teleportAwayFromTarget(getTarget());
        }
        if (tickCount % 20 == 0) {
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(3.0D), this::isValidParasiteTarget)) {
                InfectionMechanics.applyCoth(target, this);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof IncompleteFormMediumEntity && level() instanceof ServerLevel serverLevel) {
            Mob body = switch (kind) {
                case COW -> ModEntities.SIM_COW.get().create(serverLevel);
                case ENDERMAN -> ModEntities.SIM_ENDERMAN.get().create(serverLevel);
                case HORSE -> ModEntities.SIM_HORSE.get().create(serverLevel);
                case HUMAN -> ModEntities.SIM_HUMAN.get().create(serverLevel);
                case PIG -> ModEntities.SIM_PIG.get().create(serverLevel);
                case SHEEP -> ModEntities.SIM_SHEEP.get().create(serverLevel);
                case VILLAGER -> ModEntities.SIM_VILLAGER.get().create(serverLevel);
                case WOLF -> ModEntities.SIM_WOLF.get().create(serverLevel);
            };
            if (body != null) {
                body.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                body.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()),
                        MobSpawnType.MOB_SUMMONED, null);
                body.setCustomName(getCustomName());
                body.setCustomNameVisible(isCustomNameVisible());
                if (isPersistenceRequired()) {
                    body.setPersistenceRequired();
                }
                serverLevel.addFreshEntity(body);
            }
            target.discard();
            discard();
            return true;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            InfectionMechanics.applyCoth(living, this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (kind == Kind.ENDERMAN && !level().isClientSide && source.getDirectEntity() != null
                && source.getDirectEntity() != source.getEntity()) {
            for (int attempt = 0; attempt < 16; attempt++) {
                if (teleportAwayFromTarget(getTarget())) {
                    return true;
                }
            }
            return false;
        }
        boolean hurt = super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
        if (hurt && kind == Kind.ENDERMAN && !level().isClientSide && random.nextBoolean()) {
            teleportAwayFromTarget(getTarget());
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return super.causeFallDamage(distance, damageMultiplier * 0.3F, source);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public Kind getKind() {
        return kind;
    }

    private boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    private boolean teleportAwayFromTarget(LivingEntity target) {
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 destination = position().add((random.nextDouble() - 0.5D) * 32.0D,
                    random.nextInt(16) - 8, (random.nextDouble() - 0.5D) * 32.0D);
            if (target != null && target.distanceToSqr(destination) < 4.0D) {
                continue;
            }
            net.minecraft.core.BlockPos blockPos = net.minecraft.core.BlockPos.containing(destination);
            while (blockPos.getY() > level().getMinBuildHeight() && !level().getBlockState(blockPos).blocksMotion()) {
                blockPos = blockPos.below();
            }
            if (!level().getBlockState(blockPos).blocksMotion()) {
                continue;
            }
            Vec3 safeDestination = new Vec3(destination.x, blockPos.getY() + 1.0D, destination.z);
            if (!level().noCollision(this, getBoundingBox().move(safeDestination.subtract(position())))) {
                continue;
            }
            teleportTo(safeDestination.x, safeDestination.y, safeDestination.z);
            resetFallDistance();
            return true;
        }
        return false;
    }

    private void spawnPortalParticles() {
        for (int index = 0; index < 2; index++) {
            level().addParticle(ParticleTypes.PORTAL,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight() - 0.25D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    (random.nextDouble() - 0.5D) * 2.0D, -random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 2.0D);
        }
    }

    public enum Kind {
        COW("sim_cowhead", 5.4D, 2.1D, 0.30D, 16.0D),
        ENDERMAN("sim_endermanhead", 16.5D, 3.3D, 0.30D, 32.0D),
        HORSE("sim_horsehead", 7.2D, 2.25D, 0.30D, 16.0D),
        HUMAN("sim_humanhead", 4.5D, 2.7D, 0.30D, 16.0D),
        PIG("sim_pighead", 2.7D, 1.05D, 0.30D, 16.0D),
        SHEEP("sim_sheephead", 3.9D, 1.8D, 0.30D, 16.0D),
        VILLAGER("sim_villagerhead", 4.8D, 3.0D, 0.30D, 16.0D),
        WOLF("sim_wolfhead", 3.0D, 3.15D, 0.34D, 16.0D);

        private final String id;
        private final double maxHealth;
        private final double attackDamage;
        private final double movementSpeed;
        private final double followRange;
        private final int experience;

        Kind(String id, double maxHealth, double attackDamage, double movementSpeed, double followRange) {
            this.id = id;
            this.maxHealth = maxHealth;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.followRange = followRange;
            this.experience = 4;
        }
    }
}
