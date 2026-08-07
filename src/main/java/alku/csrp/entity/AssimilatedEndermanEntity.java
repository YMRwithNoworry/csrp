package alku.csrp.entity;

import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
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
    private static final EntityDataAccessor<Boolean> SHRIMP_FED = SynchedEntityData.defineId(
            AssimilatedEndermanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SCREAMING = SynchedEntityData.defineId(
            AssimilatedEndermanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CRAWLING = SynchedEntityData.defineId(
            AssimilatedEndermanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            AssimilatedEndermanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PULLING = SynchedEntityData.defineId(
            AssimilatedEndermanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int TARGET_GRACE_TICKS = 80;
    private static final int SELF_TELEPORT_COOLDOWN = 20;
    private static final int ALLY_TELEPORT_COOLDOWN = 40;
    private static final double MIN_TARGET_DISTANCE_SQR = 100.0D;
    // 基础动画
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");

    // 尖叫状态动画
    private final RawAnimation SCREAM_IDLE = ParasiteAnimations.loop(this, "idle.is_screaming_1");
    private final RawAnimation SCREAM_WALK = ParasiteAnimations.loop(this, "walk.is_screaming_1");

    // 爬行状态动画
    private final RawAnimation CRAWL_IDLE = ParasiteAnimations.loop(this, "idle.is_crawling_1");
    private final RawAnimation CRAWL_WALK = ParasiteAnimations.loop(this, "walk.is_crawling_1");

    // 爬行+尖叫组合动画
    private final RawAnimation CRAWL_SCREAM_IDLE = ParasiteAnimations.loop(
            this, "idle.is_crawling_1.is_screaming_1");
    private final RawAnimation CRAWL_SCREAM_WALK = ParasiteAnimations.loop(
            this, "walk.is_crawling_1.is_screaming_1");

    // 拉拽状态动画
    private final RawAnimation PULLING_IDLE = ParasiteAnimations.loop(this, "idle.get_parasite_status_1");
    private final RawAnimation PULLING_WALK = ParasiteAnimations.loop(this, "walk.get_parasite_status_1");

    // 传送动画
    private final RawAnimation TELEPORT = ParasiteAnimations.play(this, "idle.get_parasite_status_3");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int targetTicks;
    private int selfTeleportCooldown;
    private int allyTeleportCooldown;
    private int parasiteKills;
    private int pullingCounter;
    private int spotCooldown;

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHRIMP_FED, false);
        builder.define(SCREAMING, false);
        builder.define(CRAWLING, false);
        builder.define(PARASITE_STATUS, 0);
        builder.define(PULLING, false);
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        boolean hasTarget = target != null;
        entityData.set(SCREAMING, hasTarget);
        setAggressive(hasTarget);

        // 设置parasiteStatus: 有目标时为状态3(锁定)，否则为状态0(正常)
        entityData.set(PARASITE_STATUS, hasTarget ? 3 : 0);

        if (hasTarget && spotCooldown <= 0) {
            // 发现新目标时播放传送音效
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            spotCooldown = 40;
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        // The crawling clip is a special pose and must not be selected randomly at spawn.
        entityData.set(CRAWLING, false);
        return data;
    }

    public boolean isShrimpFed() {
        return entityData.get(SHRIMP_FED);
    }

    private void setShrimpFed(boolean fed) {
        entityData.set(SHRIMP_FED, fed);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ParasiteSoundProfiles.ambient(this);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ParasiteSoundProfiles.hurt(this);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ParasiteSoundProfiles.death(this);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ModItems.SHRIMP.get()) || isShrimpFed()) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide) {
            setShrimpFed(true);
            playSound(ModSounds.get("shrimp.eat"), 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnPortalParticles();
            return;
        }

        // 更新冷却计时器
        if (selfTeleportCooldown > 0) selfTeleportCooldown--;
        if (allyTeleportCooldown > 0) allyTeleportCooldown--;
        if (spotCooldown > 0) spotCooldown--;

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            targetTicks = 0;
            entityData.set(PARASITE_STATUS, 0);
            entityData.set(PULLING, false);
            pullingCounter = 0;
            return;
        }

        targetTicks++;

        // 更新拉拽状态 (pulling counter 0-200)
        double distToTarget = distanceTo(target);
        if (distToTarget < 10.0D && distToTarget > 3.0D) {
            if (pullingCounter < 200) {
                pullingCounter++;
            }
            entityData.set(PULLING, true);
            entityData.set(PARASITE_STATUS, 2); // 状态2: 拉拽

            // 拉拽效果：拉近目标
            if (tickCount % 5 == 0) {
                Vec3 direction = position().subtract(target.position()).normalize().scale(0.15D);
                target.setDeltaMovement(target.getDeltaMovement().add(direction));
            }
        } else {
            if (pullingCounter > 0) {
                pullingCounter--;
            }
            if (pullingCounter == 0) {
                entityData.set(PULLING, false);
                entityData.set(PARASITE_STATUS, 3); // 状态3: 锁定目标
            }
        }

        // 水伤害
        if (isInWaterRainOrBubble() && tickCount % 20 == 0) {
            hurt(damageSources().drown(), 2.0F);
        }

        // 传送逻辑
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
        if (hit) {
            // 设置攻击状态
            entityData.set(PARASITE_STATUS, 1); // 状态1: 攻击
            triggerAnim("attack_controller", "attack");
        }
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
        if (parasiteKills >= AssimilatedParasiteEntity.FERAL_KILL_THRESHOLD) {
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
        tag.putBoolean("shrimp_fed", isShrimpFed());
        tag.putBoolean("crawling", entityData.get(CRAWLING));
        tag.putInt("pulling_counter", pullingCounter);
        tag.putInt("spot_cooldown", spotCooldown);
        tag.putInt("parasite_status", entityData.get(PARASITE_STATUS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        parasiteKills = tag.getInt("parasite_kills");
        targetTicks = tag.getInt("target_ticks");
        selfTeleportCooldown = tag.getInt("self_teleport_cooldown");
        allyTeleportCooldown = tag.getInt("ally_teleport_cooldown");
        setShrimpFed(tag.getBoolean("shrimp_fed"));
        // Older saves may contain the legacy random crawling flag; normalize it on load.
        entityData.set(CRAWLING, false);
        pullingCounter = tag.getInt("pulling_counter");
        spotCooldown = tag.getInt("spot_cooldown");
        entityData.set(PARASITE_STATUS, tag.getInt("parasite_status"));
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
        // 主移动动画控制器 - 根据多种状态组合选择动画
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
            boolean screaming = entityData.get(SCREAMING);
            boolean crawling = entityData.get(CRAWLING);
            boolean pulling = entityData.get(PULLING);
            int parasiteStatus = entityData.get(PARASITE_STATUS);

            // 优先级: 拉拽 > 爬行+尖叫 > 爬行 > 尖叫 > 正常
            if (pulling) {
                return state.setAndContinue(moving ? PULLING_WALK : PULLING_IDLE);
            }

            if (crawling) {
                if (screaming) {
                    return state.setAndContinue(moving ? CRAWL_SCREAM_WALK : CRAWL_SCREAM_IDLE);
                }
                return state.setAndContinue(moving ? CRAWL_WALK : CRAWL_IDLE);
            }

            if (screaming) {
                return state.setAndContinue(moving ? SCREAM_WALK : SCREAM_IDLE);
            }

            return state.setAndContinue(moving ? WALK : IDLE);
        }));

        // 攻击动画控制器 - 可触发的单次播放动画
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP)
                .triggerableAnim("attack", ATTACK));

        // 传送动画控制器 - 可触发的传送效果动画
        controllers.add(new AnimationController<>(this, "teleport_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP)
                .triggerableAnim("teleport", TELEPORT));
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

        // 触发传送动画
        if (entity == this) {
            triggerAnim("teleport_controller", "teleport");
        }

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
