package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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

import java.util.List;

/** Legacy assimilated Enderman teleports itself and idle parasite allies around its prey. */
public final class AssimilatedEndermanEntity extends Monster implements GeoEntity, Parasite {
    private static final int TARGET_GRACE_TICKS = 80;
    private static final int SELF_TELEPORT_COOLDOWN = 20;
    private static final int ALLY_TELEPORT_COOLDOWN = 40;
    private static final double MIN_TARGET_DISTANCE_SQR = 100.0D;
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int targetTicks;
    private int selfTeleportCooldown;
    private int allyTeleportCooldown;
    private int parasiteKills;

    public AssimilatedEndermanEntity(EntityType<? extends AssimilatedEndermanEntity> type, Level level) {
        super(type, level);
        xpReward = 24;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 55.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnPortalParticles();
            return;
        }
        if (selfTeleportCooldown > 0) selfTeleportCooldown--;
        if (allyTeleportCooldown > 0) allyTeleportCooldown--;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            targetTicks = 0;
            return;
        }
        targetTicks++;
        if (isInWaterRainOrBubble() && tickCount % 20 == 0) {
            hurt(damageSources().drown(), 2.0F);
        }
        if (targetTicks > TARGET_GRACE_TICKS && tickCount % 20 == 0 && selfTeleportCooldown <= 0
                && distanceToSqr(target) > MIN_TARGET_DISTANCE_SQR) {
            if (!teleportAllyToTarget(target)) {
                teleportAwayFromTarget(target);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        LivingEntity livingTarget = entity instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(entity);
        if (hit && livingTarget != null) {
            ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
            InfectionMechanics.applyCoth(livingTarget, this);
            if (random.nextFloat() < 0.2F) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0), this);
            }
        }
        return hit;
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        parasiteKills++;
        if (parasiteKills > AssimilatedParasiteEntity.FERAL_KILL_THRESHOLD) {
            FeralEndermanEntity feral = ModEntities.FER_ENDERMAN.get().create(level);
            if (feral != null) {
                feral.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
                feral.setTarget(getTarget());
                feral.setCustomName(getCustomName());
                feral.setCustomNameVisible(isCustomNameVisible());
                if (isPersistenceRequired()) {
                    feral.setPersistenceRequired();
                }
                level.addFreshEntity(feral);
                discard();
            }
        }
        return super.killedEntity(level, victim);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasite_kills", parasiteKills);
        tag.putInt("target_ticks", targetTicks);
        tag.putInt("self_teleport_cooldown", selfTeleportCooldown);
        tag.putInt("ally_teleport_cooldown", allyTeleportCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        parasiteKills = tag.getInt("parasite_kills");
        targetTicks = tag.getInt("target_ticks");
        selfTeleportCooldown = tag.getInt("self_teleport_cooldown");
        allyTeleportCooldown = tag.getInt("ally_teleport_cooldown");
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getDirectEntity() != null && source.getDirectEntity() != source.getEntity()) {
            for (int attempt = 0; attempt < 64; attempt++) {
                if (teleportAwayFromTarget(getTarget())) {
                    return true;
                }
            }
            return false;
        }
        boolean hurt = super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
        if (hurt && !level().isClientSide) {
            allyTeleportCooldown = 0;
            if (random.nextBoolean()) {
                teleportAwayFromTarget(getTarget());
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide || random.nextFloat() >= 0.5F || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AssimilatedHeadEntity head = ModEntities.SIM_ENDERMAN_HEAD.get().create(serverLevel);
        if (head == null) {
            return;
        }
        head.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        head.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        serverLevel.addFreshEntity(head);
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

    private boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    private boolean teleportAllyToTarget(LivingEntity target) {
        if (allyTeleportCooldown > 0) {
            return false;
        }
        List<Mob> allies = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(64.0D),
                ally -> ally != this && ally instanceof Parasite && ally.isAlive() && ally.getTarget() == null);
        for (Mob ally : allies) {
            for (int attempt = 0; attempt < 8; attempt++) {
                Vec3 destination = target.position().add((random.nextDouble() - 0.5D) * 8.0D,
                        random.nextInt(5) - 2, (random.nextDouble() - 0.5D) * 8.0D);
                if (teleportEntity(ally, destination)) {
                    if (ally != this) {
                        ally.hurt(damageSources().magic(), 2.0F);
                        if (isOnFire() && random.nextFloat() < 0.75F) {
                            ally.igniteForSeconds(8.0F);
                        }
                    }
                    ally.setTarget(target);
                    allyTeleportCooldown = ALLY_TELEPORT_COOLDOWN;
                    selfTeleportCooldown = SELF_TELEPORT_COOLDOWN;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean teleportAwayFromTarget(LivingEntity target) {
        if (selfTeleportCooldown > 0) {
            return false;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 destination = position().add((random.nextDouble() - 0.5D) * 64.0D,
                    random.nextInt(64) - 32, (random.nextDouble() - 0.5D) * 64.0D);
            if (target != null && target.distanceToSqr(destination) < MIN_TARGET_DISTANCE_SQR) {
                continue;
            }
            if (teleportEntity(this, destination)) {
                selfTeleportCooldown = SELF_TELEPORT_COOLDOWN;
                return true;
            }
        }
        return false;
    }

    private boolean teleportEntity(Entity entity, Vec3 requestedPosition) {
        BlockPos blockPos = BlockPos.containing(requestedPosition);
        while (blockPos.getY() > level().getMinBuildHeight() && !level().getBlockState(blockPos).blocksMotion()) {
            blockPos = blockPos.below();
        }
        if (!level().getBlockState(blockPos).blocksMotion()) {
            return false;
        }
        Vec3 destination = new Vec3(requestedPosition.x, blockPos.getY() + 1.0D, requestedPosition.z);
        AABB box = entity.getBoundingBox().move(destination.subtract(entity.position()));
        if (!level().noCollision(entity, box)) {
            return false;
        }
        entity.teleportTo(destination.x, destination.y, destination.z);
        entity.resetFallDistance();
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        return true;
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
}
