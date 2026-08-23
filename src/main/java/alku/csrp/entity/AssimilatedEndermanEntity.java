package alku.csrp.entity;

import net.minecraft.util.Mth;
import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.Config;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Legacy assimilated Enderman teleports itself and idle parasite allies around its prey. */
public final class AssimilatedEndermanEntity extends Monster
        implements GeoEntity, Parasite, ManualVariantProvider {
    private static final EntityDataAccessor<Boolean> SHRIMP_FED = SynchedEntityData.defineId(
            AssimilatedEndermanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TEXTURE_VARIANT = SynchedEntityData.defineId(
            AssimilatedEndermanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SCREAMING = SynchedEntityData.defineId(
            AssimilatedEndermanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CRAWLING = SynchedEntityData.defineId(
            AssimilatedEndermanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            AssimilatedEndermanEntity.class, EntityDataSerializers.INT);
    private static final int TARGET_GRACE_TICKS = 80;
    private static final int SELF_TELEPORT_COOLDOWN = 20;
    private static final int ALLY_TELEPORT_COOLDOWN = 40;
    private static final int STILL_ANIMATION_DELAY_TICKS = 25;
    private static final double MIN_TARGET_DISTANCE_SQR = 100.0D;
    private static final List<String> ORIGINAL_ANIMATION_FUNCTIONS = List.of(
            "func_78087_a.age_in_ticks", "func_78087_a.limb_swing",
            "func_78087_a.age_in_ticks.is_screaming_1",
            "func_78087_a.limb_swing.is_screaming_1",
            "func_78087_a.age_in_ticks.is_crawling_1",
            "func_78087_a.limb_swing.is_crawling_1",
            "func_78087_a.age_in_ticks.is_crawling_1.is_screaming_1",
            "func_78087_a.limb_swing.is_crawling_1.is_screaming_1",
            "func_78087_a.age_in_ticks.get_still_ani_1",
            "func_78087_a.age_in_ticks.get_still_ani_1.is_screaming_1",
            "func_78087_a.age_in_ticks.get_parasite_status_1",
            "func_78087_a.limb_swing.get_parasite_status_1",
            "func_78087_a.age_in_ticks.get_parasite_status_1.is_screaming_1",
            "func_78087_a.limb_swing.get_parasite_status_1.is_screaming_1",
            "func_78087_a.age_in_ticks.get_parasite_status_1.is_crawling_1",
            "func_78087_a.limb_swing.get_parasite_status_1.is_crawling_1",
            "func_78087_a.age_in_ticks.get_parasite_status_1.is_crawling_1.is_screaming_1",
            "func_78087_a.limb_swing.get_parasite_status_1.is_crawling_1.is_screaming_1",
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1",
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1.is_screaming_1",
            "func_78087_a.age_in_ticks.get_parasite_status_2.is_crawling_1",
            "func_78087_a.limb_swing.get_parasite_status_2.is_crawling_1",
            "func_78087_a.age_in_ticks.get_parasite_status_2.is_crawling_1.is_screaming_1",
            "func_78087_a.limb_swing.get_parasite_status_2.is_crawling_1.is_screaming_1");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, RawAnimation> originalAnimations = createOriginalAnimations();
    private int targetTicks;
    private int selfTeleportCooldown;
    private int allyTeleportCooldown;
    private int parasiteKills;
    private int spotCooldown;
    private int stillAnimationTicks;

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
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SHRIMP_FED, false);
        entityData.define(TEXTURE_VARIANT, 0);
        entityData.define(SCREAMING, false);
        entityData.define(CRAWLING, false);
        entityData.define(PARASITE_STATUS, 0);
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        boolean hasTarget = target != null;
        entityData.set(SCREAMING, hasTarget);
        setAggressive(hasTarget);

        if (!hasTarget) {
            setParasiteStatus(0);
        }

        if (hasTarget && spotCooldown <= 0) {
            // 发现新目标时播放传送音效
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            spotCooldown = 40;
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData spawnGroupData, net.minecraft.nbt.CompoundTag spawnTag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, spawnTag);
        setCrawling(random.nextDouble() < Config.variantSpawnChance()
                || Config.evolutionPhase(level.getLevel()) >= Config.alwaysVariantPhase());
        return data;
    }

    public boolean isShrimpFed() {
        return entityData.get(SHRIMP_FED);
    }

    public int getTextureVariant() {
        return entityData.get(TEXTURE_VARIANT);
    }

    @Override
    public int getManualVariant() {
        return isShrimpFed() ? 2 : getTextureVariant();
    }

    @Override
    public void setManualVariant(int variant) {
        int skin = Mth.clamp(variant, 0, getMaxManualVariants() - 1);
        setShrimpFed(skin == 2);
        entityData.set(TEXTURE_VARIANT, skin == 1 ? 1 : 0);
    }

    @Override
    public int getMaxManualVariants() {
        return 3;
    }

    private void setShrimpFed(boolean fed) {
        entityData.set(SHRIMP_FED, fed);
    }

    public boolean isCrawling() {
        return entityData.get(CRAWLING);
    }

    private void setCrawling(boolean crawling) {
        entityData.set(CRAWLING, crawling);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return isCrawling() ? EntityDimensions.scalable(0.95F, 1.25F) : super.getDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (accessor == CRAWLING) {
            refreshDimensions();
        }
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
        goalSelector.addGoal(2, new EndermanMeleeGoal());
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
        if (ParasiteAnimations.isMoving(this, true)) {
            stillAnimationTicks = 0;
        } else {
            stillAnimationTicks++;
        }
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
            setParasiteStatus(0);
            return;
        }

        targetTicks++;

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
        tag.putInt("texture_variant", getTextureVariant());
        tag.putBoolean("crawling", isCrawling());
        tag.putInt("spot_cooldown", spotCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        parasiteKills = tag.getInt("parasite_kills");
        targetTicks = tag.getInt("target_ticks");
        selfTeleportCooldown = tag.getInt("self_teleport_cooldown");
        allyTeleportCooldown = tag.getInt("ally_teleport_cooldown");
        setShrimpFed(tag.getBoolean("shrimp_fed"));
        entityData.set(TEXTURE_VARIANT, Mth.clamp(tag.getInt("texture_variant"), 0, 1));
        setCrawling(tag.getBoolean("crawling"));
        spotCooldown = tag.getInt("spot_cooldown");
        setParasiteStatus(0);
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
        head.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        serverLevel.addFreshEntity(head);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Crawling rotates the original root model by roughly 90 degrees on X and 180 on Z.
        // The legacy model switched this pose immediately; blending from standing deforms the spawn pose.
        controllers.add(new AnimationController<>(this, "movement_controller", 0, state ->
                state.setAndContinue(originalAnimations.get(animationFunction(
                        ParasiteAnimations.isMoving(this, state.isMoving()))))));
    }

    private Map<String, RawAnimation> createOriginalAnimations() {
        Map<String, RawAnimation> animations = new HashMap<>();
        for (String functionName : ORIGINAL_ANIMATION_FUNCTIONS) {
            animations.put(functionName, ParasiteAnimations.loop(this, functionName));
        }
        return Map.copyOf(animations);
    }

    private String animationFunction(boolean moving) {
        int status = entityData.get(PARASITE_STATUS);
        boolean crawling = isCrawling();
        boolean screaming = entityData.get(SCREAMING);
        if (status < 0 || status > 2) {
            status = 0;
        } else if (!crawling && status == 2) {
            status = 1;
        }
        String functionName = moving
                ? "func_78087_a.limb_swing" : "func_78087_a.age_in_ticks";
        if (status == 1 || status == 2) {
            functionName += ".get_parasite_status_" + status;
        }
        if (!moving && !crawling && stillAnimationTicks > STILL_ANIMATION_DELAY_TICKS) {
            functionName += ".get_still_ani_1";
        }
        if (crawling) {
            functionName += ".is_crawling_1";
        }
        if (screaming) {
            functionName += ".is_screaming_1";
        }
        return functionName;
    }

    private void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private final class EndermanMeleeGoal extends MeleeAttackGoal {
        private EndermanMeleeGoal() {
            super(AssimilatedEndermanEntity.this, 1.2D, false);
        }

        @Override
        public void start() {
            super.start();
            setParasiteStatus(2);
        }

        @Override
        public void stop() {
            super.stop();
            setParasiteStatus(0);
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            double reach = getBbWidth() * 2.0D;
            setParasiteStatus(distanceToSqr(target) <= reach * reach + target.getBbWidth() ? 1 : 2);
        }
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
                            ally.setSecondsOnFire(1);;
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
