package alku.csrp.entity;

import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.List;

/** Feral Enderman teleport combat and parasite relocation from the legacy implementation. */
public final class FeralEndermanEntity extends FeralParasiteEntity {
    private static final double TELEPORT_RADIUS = 32.0D;
    private static final double MIN_TARGET_DISTANCE_SQR = 49.0D;
    private static final int TARGET_GRACE_TICKS = 30;
    private static final int TELEPORT_COOLDOWN_TICKS = 20;
    private static final int ALLY_TELEPORT_COOLDOWN_TICKS = 10;
    private static final int DAMAGE_CAP_DIVISOR = 3;
    private static final float MINIMUM_DAMAGE = 0.75F;
    private static final float COTH_CHANCE = 0.70F;
    private static final float DEATH_BURST_CHANCE = 0.50F;
    private final RawAnimation ageInTicksAnimation = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation limbSwingAnimation = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation screamingAgeAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.is_screaming_1");
    private final RawAnimation screamingLimbAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.is_screaming_1");
    private final RawAnimation stillAgeAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_still_ani_1");
    private final RawAnimation screamingStillAgeAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_still_ani_1.is_screaming_1");
    private final RawAnimation status2LimbAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation screamingStatus2LimbAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2.is_screaming_1");
    private final RawAnimation status2StillAgeAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1");
    private final RawAnimation screamingStatus2StillAgeAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1.is_screaming_1");

    private int targetTicks;
    private int teleportCooldown;
    private int allyTeleportCooldown;
    private boolean deathBurstHandled;

    public FeralEndermanEntity(EntityType<? extends FeralEndermanEntity> type, Level level) {
        super(type, level, Kind.ENDERMAN);
        xpReward = 16;
    }

    public static boolean checkFeralEndermanSpawnRules(EntityType<? extends Monster> type,
            ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        ServerLevel endLevel = level.getLevel();
        if (endLevel.dimension() != Level.END) {
            return false;
        }
        SrpWorldData endData = SrpWorldData.get(endLevel);
        SrpWorldData overworldData = SrpWorldData.get(endLevel.getServer().overworld());
        return overworldData.evolutionPhase() >= 8
                && endData.evolutionPhase() >= 4
                && endData.assimilatedEndermen() >= 9
                && Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 7;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.7F));
    }

    @Override
    public void setTarget(LivingEntity target) {
        LivingEntity previous = getTarget();
        super.setTarget(target);
        setAggressive(target != null);
        if (target == null) {
            targetTicks = 0;
        } else if (target != previous) {
            targetTicks = 0;
            if (!level().isClientSide) {
                level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        ModSounds.INFECTED_ENDERMAN_PORTAL.get(), getSoundSource(), 0.3F, 1.0F);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnPortalParticles();
            return;
        }

        if (teleportCooldown > 0) {
            teleportCooldown--;
        }
        if (allyTeleportCooldown > 0) {
            allyTeleportCooldown--;
        }
        if (hasEffect(ModMobEffects.RAGE.get())) {
            allyTeleportCooldown = 0;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            targetTicks = 0;
            return;
        }
        targetTicks++;
        pursueInLiquid(target);
        if (specialMovesEnabled() && targetTicks >= TARGET_GRACE_TICKS && tickCount % 20 == 0
                && teleportCooldown <= 0 && distanceToSqr(target) > MIN_TARGET_DISTANCE_SQR) {
            Mob ally = findTeleportAlly();
            if (teleportNearTarget(target)) {
                teleportAllyToTarget(target, ally);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            Mob ally = level().isClientSide ? null : findTeleportAlly();
            if (!level().isClientSide && teleportAwayFromTarget(getTarget(), true)) {
                teleportAllyToTarget(getTarget(), ally);
            }
            return false;
        }
        boolean damaged = super.hurt(source, cappedDamage(source, amount));
        if (damaged && isAlive() && !level().isClientSide) {
            allyTeleportCooldown = 0;
            if (random.nextFloat() < 0.10F) {
                placeFeralRemains(blockPosition());
            }
            if (specialMovesEnabled() && teleportCooldown <= 0 && random.nextBoolean()
                    && teleportWithAllyAwayFromTarget(getTarget())) {
                return true;
            }
        }
        return damaged;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        LivingEntity livingTarget = target instanceof LivingEntity living ? living : null;
        float healthBefore = livingTarget == null ? 0.0F : ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean damaged = super.doHurtTarget(target);
        if (damaged && livingTarget != null) {
            applyMinimumDamage(livingTarget, healthBefore);
        }
        if (damaged && !level().isClientSide && specialMovesEnabled()
                && teleportCooldown <= 0 && random.nextBoolean()
                && teleportWithAllyAwayFromTarget(getTarget())) {
            return true;
        }
        return damaged;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!deathBurstHandled && level() instanceof ServerLevel && random.nextFloat() < DEATH_BURST_CHANCE) {
            deathBurstHandled = true;
            spawnDeathBurst();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("target_ticks", targetTicks);
        tag.putInt("teleport_cooldown", teleportCooldown);
        tag.putInt("ally_teleport_cooldown", allyTeleportCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        targetTicks = tag.getInt("target_ticks");
        teleportCooldown = tag.getInt("teleport_cooldown");
        allyTeleportCooldown = tag.getInt("ally_teleport_cooldown");
    }

    public static float cothChance() {
        return COTH_CHANCE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 0,
                state -> state.setAndContinue(ageAnimation())));
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                return PlayState.STOP;
            }
            return state.setAndContinue(limbAnimation());
        }));
    }

    private RawAnimation ageAnimation() {
        boolean screaming = isAggressive();
        boolean still = getStillAni();
        if (getParasiteStatus() == 2 && still) {
            return screaming ? screamingStatus2StillAgeAnimation : status2StillAgeAnimation;
        }
        if (still) {
            return screaming ? screamingStillAgeAnimation : stillAgeAnimation;
        }
        return screaming ? screamingAgeAnimation : ageInTicksAnimation;
    }

    private RawAnimation limbAnimation() {
        boolean screaming = isAggressive();
        if (getParasiteStatus() == 2) {
            return screaming ? screamingStatus2LimbAnimation : status2LimbAnimation;
        }
        return screaming ? screamingLimbAnimation : limbSwingAnimation;
    }

    private void spawnPortalParticles() {
        for (int i = 0; i < 2; i++) {
            level().addParticle(ParticleTypes.PORTAL,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight() - 0.25D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    (random.nextDouble() - 0.5D) * 2.0D, -random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 2.0D);
        }
    }

    private Mob findTeleportAlly() {
        if (allyTeleportCooldown > 0) {
            return null;
        }
        List<Mob> allies = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(64.0D),
                ally -> ally != this && ally.isAlive() && canTeleportAlly(ally));
        return allies.isEmpty() ? null : allies.get(0);
    }

    private boolean teleportAllyToTarget(LivingEntity target, Mob ally) {
        if (target == null || ally == null || !ally.isAlive() || allyTeleportCooldown > 0) {
            return false;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 destination = target.position().add((random.nextDouble() - 0.5D) * 8.0D,
                    random.nextInt(5) - 2, (random.nextDouble() - 0.5D) * 8.0D);
            if (teleportEntity(ally, destination)) {
                if (!isTeleportDamageImmune(ally)) {
                    ally.hurt(damageSources().magic(), 0.5F);
                }
                if (isOnFire() && random.nextFloat() < 0.75F) {
                    ally.setSecondsOnFire(1);;
                }
                ally.setTarget(target);
                allyTeleportCooldown = ALLY_TELEPORT_COOLDOWN_TICKS;
                return true;
            }
        }
        return false;
    }

    private boolean teleportWithAllyAwayFromTarget(LivingEntity target) {
        Mob ally = findTeleportAlly();
        if (!teleportAwayFromTarget(target, false)) {
            return false;
        }
        teleportAllyToTarget(target, ally);
        return true;
    }

    private boolean teleportNearTarget(LivingEntity target) {
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 destination = target.position().add((random.nextDouble() - 0.5D) * 12.0D,
                    random.nextInt(9) - 4, (random.nextDouble() - 0.5D) * 12.0D);
            if (target.distanceToSqr(destination) < 9.0D) {
                continue;
            }
            if (teleportEntity(this, destination)) {
                teleportCooldown = TELEPORT_COOLDOWN_TICKS;
                return true;
            }
        }
        return false;
    }

    private boolean teleportAwayFromTarget(LivingEntity target, boolean ignoreCooldown) {
        if (!ignoreCooldown && teleportCooldown > 0) {
            return false;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            Vec3 destination = position().add((random.nextDouble() - 0.5D) * TELEPORT_RADIUS * 2.0D,
                    random.nextInt(64) - 32, (random.nextDouble() - 0.5D) * TELEPORT_RADIUS * 2.0D);
            if (target != null && target.distanceToSqr(destination) < MIN_TARGET_DISTANCE_SQR) {
                continue;
            }
            if (teleportEntity(this, destination)) {
                teleportCooldown = TELEPORT_COOLDOWN_TICKS;
                return true;
            }
        }
        return false;
    }

    private boolean teleportEntity(Entity entity, Vec3 requestedPosition) {
        BlockPos position = BlockPos.containing(requestedPosition);
        while (position.getY() > level().getMinBuildHeight() && !level().getBlockState(position).blocksMotion()) {
            position = position.below();
        }
        if (!level().getBlockState(position).blocksMotion()) {
            return false;
        }

        Vec3 destination = new Vec3(requestedPosition.x, position.getY() + 1.0D, requestedPosition.z);
        AABB movedBox = entity.getBoundingBox().move(destination.subtract(entity.position()));
        if (!level().noCollision(entity, movedBox)) {
            return false;
        }

        entity.teleportTo(destination.x, destination.y, destination.z);
        entity.resetFallDistance();
        playSound(ModSounds.INFECTED_ENDERMAN_PORTAL.get(), 1.0F, 1.0F);
        return true;
    }

    private boolean specialMovesEnabled() {
        return level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).specialMoves();
    }

    private float cappedDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel).damageCap()
                || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return amount;
        }
        float cap = getMaxHealth() / DAMAGE_CAP_DIVISOR;
        if (amount >= cap && !hasEffect(ModMobEffects.RAGE.get())) {
            addEffect(new MobEffectInstance(ModMobEffects.RAGE.get(), 140, 1, false, false), this);
        }
        return Math.min(amount, cap);
    }

    private void applyMinimumDamage(LivingEntity target, float healthBefore) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel).minimumDamage() || !target.isAlive()) {
            return;
        }
        float dealt = Math.max(0.0F, healthBefore - ParasiteCombatEffects.healthWithAbsorption(target));
        float remaining = MINIMUM_DAMAGE - dealt;
        if (remaining <= 0.0F) {
            return;
        }
        float absorption = target.getAbsorptionAmount();
        float absorbed = Math.min(absorption, remaining);
        target.setAbsorptionAmount(absorption - absorbed);
        remaining -= absorbed;
        if (remaining > 0.0F) {
            target.setHealth(Math.max(0.0F, target.getHealth() - remaining));
        }
        level().broadcastEntityEvent(target, (byte) 2);
    }

    private void pursueInLiquid(LivingEntity target) {
        if (!isInWaterOrBubble()) {
            return;
        }
        Vec3 movement = getDeltaMovement();
        if (target.isInWaterOrBubble()) {
            Vec3 direction = target.getEyePosition().subtract(getEyePosition());
            if (direction.lengthSqr() > 0.001D) {
                Vec3 pursuit = direction.normalize().scale(0.14D);
                setDeltaMovement(movement.x * 0.8D + pursuit.x,
                        movement.y * 0.5D + pursuit.y, movement.z * 0.8D + pursuit.z);
            }
        } else if (tickCount % 20 == 0) {
            setDeltaMovement(movement.x, Math.max(0.28D, movement.y), movement.z);
        }
    }

    private boolean canTeleportAlly(Mob ally) {
        return ally instanceof RupterEntity
                || ally instanceof AssimilatedParasiteEntity
                || ally instanceof AssimilatedVariantEntity
                || ally instanceof AssimilatedEndermanEntity
                || ally instanceof AssimilatedHeadEntity
                || ally instanceof FeralParasiteEntity
                || ally instanceof PrimitiveParasiteEntity;
    }

    private boolean isTeleportDamageImmune(Mob ally) {
        return ally instanceof AssimilatedEndermanEntity
                || ally instanceof FeralEndermanEntity
                || ally instanceof AssimilatedHeadEntity head && head.getKind() == AssimilatedHeadEntity.Kind.ENDERMAN;
    }

    private void spawnDeathBurst() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), getBoundingBox().inflate(2.0D));
        level().explode(this, getX(), getY(), getZ(), 2.0F, Level.ExplosionInteraction.NONE);
        ToxicCloudEntity cloud = ToxicCloudEntity.create(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(3.5F);
        cloud.setDuration(200);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 240, 1, false, false));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH.get(), 600, 1, false, false, true));
        serverLevel.addFreshEntity(cloud);

        GnatEntity gnat = ModEntities.GNAT.get().create(serverLevel, null, entity -> {
                entity.moveTo(blockPosition(), getYRot(), getXRot());
            }, blockPosition(), MobSpawnType.MOB_SUMMONED, false, false);
        if (gnat != null) {
            gnat.moveTo(getX(), getY() + 0.25D, getZ(), random.nextFloat() * 360.0F, 0.0F);
            gnat.setDeltaMovement((random.nextDouble() - 0.5D) * 0.3D, 0.3D,
                    (random.nextDouble() - 0.5D) * 0.3D);
            serverLevel.addFreshEntity(gnat);
        }
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1.0D, getZ(),
                30, 1.2D, 1.5D, 1.2D, 0.12D);
        playSound(ModSounds.MOB_EXPLOSION.get(), 1.0F, 0.9F);
    }

    private void placeFeralRemains(BlockPos origin) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        for (int offset = 0; offset <= 4; offset++) {
            BlockPos candidate = origin.below(offset);
            if (level().isEmptyBlock(candidate) && level().getBlockState(candidate.below()).blocksMotion()) {
                level().setBlock(candidate, ModBlocks.INFESTED_REMAINS.get().defaultBlockState(), 3);
                return;
            }
        }
    }
}
