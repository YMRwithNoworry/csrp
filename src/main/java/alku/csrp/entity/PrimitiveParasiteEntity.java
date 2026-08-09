package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.config.MobsConfig;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.event.EventHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Shared 1.12 primitive-parasite state: hostile targeting, kills, and repeated-damage adaptation. */
public abstract class PrimitiveParasiteEntity extends Monster implements GeoEntity, Parasite {
    private static final Map<String, BlockBreakProfile> BLOCK_BREAK_PROFILES = createBlockBreakProfiles();
    private int blockBreakCooldown;
    private static final EntityDataAccessor<Byte> ADAPTATION_HIT_STATUS = SynchedEntityData.defineId(
            PrimitiveParasiteEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> SPECIAL_LEAP_TICKS = SynchedEntityData.defineId(
            PrimitiveParasiteEntity.class, EntityDataSerializers.INT);
    private static final String KILLS_TAG = "parasitekills";
    private static final String LEGACY_KILLCOUNT_TAG = "legacy_killcount";
    private static final String ADAPTATIONS_TAG = "damage_adaptations";
    private static final String COLONY_SPAWNED_TAG = "colony_spawned";
    private static final int MAX_ADAPTATION_HITS = 12;
    private static final float ADAPTATION_PER_HIT = 0.05F;
    private static final int DEFAULT_MAX_LEARNABLE_DAMAGE_SOURCES = 5;
    private static final int NEW_DAMAGE_COOLDOWN_TICKS = 20;
    private static final int FIRE_ADAPTATION_BLOCK_TICKS = 10;
    private static final TagKey<DamageType> TACZ_BULLET_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("tacz", "bullets"));
    private static final Map<Class<?>, Optional<Method>> TACZ_BULLET_GUN_ID_METHODS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Optional<Method>> TACZ_ITEM_GUN_ID_METHODS = new ConcurrentHashMap<>();

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, Integer> damageAdaptations = new LinkedHashMap<>();
    private boolean bypassArmorForDamageCap;
    private float lastDamageAdaptationReduction;
    private int parasiteKills;
    private double legacyKillCount;
    private boolean colonySpawned;
    private boolean adaptedFormSpawned;
    private int adaptationLearningCooldown;
    private int fireAdaptationBlockTicks;

    protected PrimitiveParasiteEntity(EntityType<? extends PrimitiveParasiteEntity> type, Level level) {
        super(type, level);
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ADAPTATION_HIT_STATUS, (byte) 0);
        builder.define(SPECIAL_LEAP_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            tickBlockBreaking();
            int leapTicks = entityData.get(SPECIAL_LEAP_TICKS);
            if (leapTicks > 0) {
                entityData.set(SPECIAL_LEAP_TICKS, leapTicks - 1);
            }
        }
        if (!level().isClientSide && tickCount % 20 == 0 && !Config.useEvolutionPhases()
                && level().getDifficulty() == Difficulty.HARD && Config.killcountPlus() > 0.0D) {
            double previous = legacyKillCount;
            legacyKillCount += Config.killcountPlus();
            int previousKills = (int) Math.floor(previous);
            int currentKills = (int) Math.floor(legacyKillCount);
            if (currentKills > previousKills) {
                parasiteKills = Math.max(parasiteKills, currentKills);
                int requiredKills = requiredAdaptationKills();
                if (previousKills < requiredKills && currentKills >= requiredKills
                        && level() instanceof ServerLevel serverLevel) {
                    onParasiteKill(serverLevel, this, parasiteKills);
                }
            }
        }
        if (!level().isClientSide && tickCount % 21 == 10 && hasEffect(ModMobEffects.ANTIMALL)) {
            reduceAllResistances(1);
        }
        if (!level().isClientSide) {
            if (adaptationLearningCooldown > 0) {
                adaptationLearningCooldown--;
            }
            if (fireAdaptationBlockTicks > 0) {
                fireAdaptationBlockTicks--;
            }
        }
    }

    private void tickBlockBreaking() {
        if (!canBreakBlocks()) {
            return;
        }
        if (blockBreakCooldown > 0) {
            blockBreakCooldown--;
        }
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
        BlockBreakProfile profile = BLOCK_BREAK_PROFILES.get(entityId);
        LivingEntity target = getTarget();
        if (profile == null || blockBreakCooldown > 0 || target == null || !target.isAlive()
                || distanceToSqr(target) > 4096.0D
                || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || !EventHooks.canEntityGrief(level(), this)) {
            return;
        }
        int baseX = Mth.floor(getX());
        int baseY = Mth.floor(getY() + 0.1D);
        int baseZ = Mth.floor(getZ());
        int height = Mth.ceil(getBbHeight());
        double targetDeltaY = target.getY() - getY();
        int verticalOffset = targetDeltaY < -1.0D ? -2 : targetDeltaY > 2.0D ? 1 : 0;
        boolean broke = false;
        for (int x = -profile.range(); x <= profile.range(); x++) {
            for (int z = -profile.range(); z <= profile.range(); z++) {
                for (int y = 1 + verticalOffset; y <= height + verticalOffset; y++) {
                    BlockPos pos = new BlockPos(baseX + x, baseY + y, baseZ + z);
                    BlockState state = level().getBlockState(pos);
                    float hardness = state.getDestroySpeed(level(), pos);
                    if (state.isAir() || hardness < 0.0F
                            || hardness > adjustBlockBreakHardness(profile.hardness())
                            || !state.canEntityDestroy(level(), pos, this)
                            || !EventHooks.onEntityDestroyBlock(this, pos, state)) {
                        continue;
                    }
                    if (level().destroyBlock(pos, true, this)) {
                        broke = true;
                        if (MobsConfig.devourerWaterPlacement()
                                && ("pri_devourer".equals(entityId) || "ada_devourer".equals(entityId))) {
                            level().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                        }
                    }
                }
            }
        }
        blockBreakCooldown = profile.cooldown();
        if (broke) {
            playSound(net.minecraft.sounds.SoundEvents.STONE_BREAK, 0.7F, 0.8F + random.nextFloat() * 0.3F);
        }
    }

    protected boolean canBreakBlocks() {
        return true;
    }

    private static Map<String, BlockBreakProfile> createBlockBreakProfiles() {
        Map<String, BlockBreakProfile> profiles = new HashMap<>();
        addBlockBreakProfiles(profiles, 3.0F, 60, 4, "sim_dragone");
        addBlockBreakProfiles(profiles, 3.0F, 40, 2, "hi_golem");
        addBlockBreakProfiles(profiles, 1.0F, 40, 1, "mar_cow", "mar_enderman", "mar_bear");
        addBlockBreakProfiles(profiles, 1.0F, 60, 1, "pri_longarms", "pri_reeker", "pri_summoner",
                "pri_manducater", "pri_yelloweye", "pri_arachnida", "pri_bolster", "pri_vermin",
                "carrier_worm");
        addBlockBreakProfiles(profiles, 1.0F, 900, 2, "pri_devourer");
        addBlockBreakProfiles(profiles, 3.0F, 40, 1, "ada_longarms", "ada_reeker", "ada_summoner");
        addBlockBreakProfiles(profiles, 3.0F, 40, 2, "ada_manducater", "ada_yelloweye", "ada_arachnida");
        addBlockBreakProfiles(profiles, 3.5F, 20, 2, "ada_bolster");
        addBlockBreakProfiles(profiles, 3.5F, 540, 3, "ada_devourer");
        addBlockBreakProfiles(profiles, 5.0F, 20, 2, "warden", "vigilante", "overseer", "bomber_light");
        addBlockBreakProfiles(profiles, 5.0F, 10, 3, "marauder");
        addBlockBreakProfiles(profiles, 5.0F, 20, 4, "monarch");
        addBlockBreakProfiles(profiles, 7.0F, 20, 4, "beckon_siii", "dispatcher_siii");
        addBlockBreakProfiles(profiles, 15.0F, 60, 5, "wraith", "bogle", "haunter", "carrier_colony", "bomber_heavy");
        addBlockBreakProfiles(profiles, 15.0F, 60, 2, "succor");
        addBlockBreakProfiles(profiles, 18.0F, 20, 5, "beckon_siv", "dispatcher_siv");
        addBlockBreakProfiles(profiles, 7.0F, 20, 3, "kyphosis", "sentry");
        addBlockBreakProfiles(profiles, 9.0F, 5, 4, "anc_dreadnaut", "anc_overlord");
        addBlockBreakProfiles(profiles, 27.0F, 80, 6, "draconite");
        addBlockBreakProfiles(profiles, 27.0F, 60, 3, "kirin");
        addBlockBreakProfiles(profiles, 4.0F, 30, 2, "crux");
        return profiles;
    }

    protected float adjustBlockBreakHardness(float baseHardness) {
        return baseHardness;
    }

    private static void addBlockBreakProfiles(Map<String, BlockBreakProfile> profiles, float hardness,
                                               int cooldown, int range, String... ids) {
        for (String id : ids) {
            profiles.put(id, new BlockBreakProfile(hardness, cooldown, range));
        }
    }

    private record BlockBreakProfile(float hardness, int cooldown, int range) {
    }

    protected final Goal createAnimatedLeapGoal(float verticalVelocity, int animationTicks) {
        return new LeapAtTargetGoal(this, verticalVelocity) {
            @Override
            public void start() {
                super.start();
                startSpecialLeapAnimation(animationTicks);
            }
        };
    }

    protected final void startSpecialLeapAnimation(int animationTicks) {
        entityData.set(SPECIAL_LEAP_TICKS, Math.max(1, animationTicks));
    }

    protected final boolean isSpecialLeapAnimating() {
        return entityData.get(SPECIAL_LEAP_TICKS) > 0;
    }

    @Override
    protected void registerGoals() {
        if (usesDefaultMovementGoals()) {
            if (!(this instanceof PreeminentParasiteEntity preeminent
                    && (preeminent.getKind() == PreeminentParasiteEntity.Kind.CARRIER_COLONY
                    || preeminent.getKind() == PreeminentParasiteEntity.Kind.HAUNTER))
                    && usesDefaultFloatGoal()) {
                goalSelector.addGoal(0, new FloatGoal(this));
            }
            if (!(this instanceof PreeminentParasiteEntity)) {
                goalSelector.addGoal(6, new ParasiteFollowGoal(this));
            }
            goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        }
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    protected boolean usesDefaultMovementGoals() {
        return true;
    }

    protected boolean usesDefaultFloatGoal() {
        return true;
    }

    protected boolean isValidParasiteTarget(LivingEntity target) {
        boolean totalSlaughter = level() instanceof ServerLevel serverLevel
                && SrpWorldData.get(serverLevel).evolutionPhase() >= 9;
        return target != this && target.isAlive() && !(target instanceof Parasite)
                && (totalSlaughter || !target.hasEffect(ModMobEffects.THE_SIGN));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        lastDamageAdaptationReduction = 0.0F;
        if (!level().isClientSide) {
            entityData.set(ADAPTATION_HIT_STATUS, (byte) 0);
        }
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if ((attacker instanceof Parasite && attacker != this)
                || (direct instanceof Parasite && direct != this)) {
            return false;
        }
        Holder<MobEffect> resistanceEffect = killingResistanceEffect();
        if (resistanceEffect != null) {
            amount = ParasiteCombatEffects.damageAfterKillingResistance(source, amount, resistanceEffect);
        }
        if (!usesDamageAdaptation()) {
            return hurtWithIncomingDamageCap(source, amount);
        }
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return hurtWithIncomingDamageCap(source, amount);
        }
        if (!level().isClientSide && (isOnFire() || source.is(DamageTypeTags.IS_FIRE))
                && random.nextFloat() < fireAdaptationSuppressionChance()) {
            fireAdaptationBlockTicks = FIRE_ADAPTATION_BLOCK_TICKS;
        }
        String damageId = damageTypeId(source);
        int adaptationHits = damageAdaptations.getOrDefault(damageId, 0);
        if (!level().isClientSide && adaptationLearningCooldown <= 0 && fireAdaptationBlockTicks <= 0
                && (damageAdaptations.containsKey(damageId)
                || damageAdaptations.size() < maxLearnableDamageSources())
                && shouldLearnDamageSource(source, damageId, adaptationHits)) {
            adaptationHits = adaptationHits == Integer.MAX_VALUE ? adaptationHits : adaptationHits + 1;
            damageAdaptations.put(damageId, adaptationHits);
            adaptationLearningCooldown = NEW_DAMAGE_COOLDOWN_TICKS;
        }
        byte hitStatus = 0;
        if (!level().isClientSide && adaptationHits > 0) {
            hitStatus = adaptationHits <= maxDamageAdaptationHits() ? (byte) 1 : (byte) 2;
            entityData.set(ADAPTATION_HIT_STATUS, hitStatus);
        }
        float reduction = Math.min(1.0F, Math.min(maxDamageAdaptationHits(), adaptationHits)
                * damageAdaptationPerHit() * damageAdaptationEffectiveness());
        float adaptedDamage = amount * (1.0F - reduction);
        lastDamageAdaptationReduction = Math.max(0.0F, amount - adaptedDamage);
        boolean hurt = hurtWithIncomingDamageCap(source, adaptedDamage);
        if (hurt && !level().isClientSide && hitStatus != 0) {
            playSound(hitStatus == 2 ? ModSounds.ADAPTATION_FULL.get() : ModSounds.ADAPTATION_PARTIAL.get(),
                    getSoundVolume(), getVoicePitch());
        }
        return hurt;
    }

    private Holder<MobEffect> killingResistanceEffect() {
        if (this instanceof CrudeParasiteEntity) {
            return ModMobEffects.CRUDE;
        }
        if (this instanceof AdaptedVariantEntity) {
            return ModMobEffects.ADAPTED;
        }
        if (this instanceof PureParasiteEntity || this instanceof MarauderEntity) {
            return ModMobEffects.PURE;
        }
        if (this instanceof NexusParasiteEntity || this instanceof DeterrentParasiteEntity) {
            return ModMobEffects.NEXUS;
        }
        if (this instanceof PrimitiveVariantEntity || this instanceof LongarmsEntity
                || this instanceof SummonerEntity || this instanceof VerminEntity
                || this instanceof VisceraEntity) {
            return ModMobEffects.PRIMITIVE;
        }
        return null;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = !(target instanceof Parasite) && super.doHurtTarget(target);
        if (hit) {
            if (!swinging) {
                swing(InteractionHand.MAIN_HAND);
            }
            spawnAttackParticles(target);
        }
        return hit;
    }

    protected final void spawnAttackParticles(Entity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55D,
                target.getZ(), 8, target.getBbWidth() * 0.35D, target.getBbHeight() * 0.25D,
                target.getBbWidth() * 0.35D, 0.08D);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    protected int incomingDamageCapDivisor() {
        return 1;
    }

    private boolean hurtWithIncomingDamageCap(DamageSource source, float amount) {
        int divisor = incomingDamageCapDivisor();
        if (divisor <= 1 || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(source, amount);
        }
        float maximumHealth = getMaxHealth();
        float cappedDamage = maximumHealth / divisor + maximumHealth % divisor * 0.5F;
        boolean reachedCap = amount >= cappedDamage;
        if (reachedCap) {
            if (!hasEffect(ModMobEffects.RAGE)) {
                addEffect(new MobEffectInstance(ModMobEffects.RAGE, 200, 1, false, false), this);
            }
            if (random.nextDouble() < 0.3D && getHealth() > 0.0F) {
                attackEntityFromEffects(1, 1);
                attackEntityFromCap(1);
            }
        }
        boolean previousBypass = bypassArmorForDamageCap;
        bypassArmorForDamageCap = reachedCap;
        try {
            return super.hurt(source, Math.min(amount, cappedDamage));
        } finally {
            bypassArmorForDamageCap = previousBypass;
        }
    }

    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        return bypassArmorForDamageCap ? amount : super.getDamageAfterArmorAbsorb(source, amount);
    }

    protected void attackEntityFromEffects(int range, int count) {
    }

    protected void attackEntityFromCap(int count) {
    }

    protected float lastDamageAdaptationReduction() {
        return lastDamageAdaptationReduction;
    }

    public boolean applyScaryOrbEffect(LivingEntity target, int nearbyEntities) {
        if (target == this || target instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        InfectionMechanics.applyCothEffect(target, this, 1200, 3, false, false);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false), this);
        return true;
    }

    public float scaryOrbMinimumDamage() {
        return 4.0F;
    }

    public boolean applyScaryOrbMinimumDamage(LivingEntity target, float multiplier) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel).minimumDamage()
                || target instanceof Parasite || target == this || !target.isAlive()
                || target instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        float amount = scaryOrbMinimumDamage() * multiplier;
        if (amount <= 0.0F) {
            return false;
        }
        float absorptionDamage = Math.min(target.getAbsorptionAmount(), amount * 0.5F);
        if (absorptionDamage > 0.0F) {
            target.setAbsorptionAmount(target.getAbsorptionAmount() - absorptionDamage);
        }
        float healthDamage = amount - absorptionDamage;
        target.setHealth(Math.max(0.0F, target.getHealth() - healthDamage));
        level().broadcastEntityEvent(target, (byte) 2);
        return true;
    }

    public boolean applyPrimitiveMinimumDamage(LivingEntity target) {
        return applyPrimitiveMinimumDamage(target, 1.0F);
    }

    public boolean applyPrimitiveMinimumDamage(LivingEntity target, float multiplier) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel).minimumDamage()
                || target instanceof Parasite || target == this || !target.isAlive()
                || target instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        float amount = Config.primitiveMinimumDamage() * Math.max(0.0F, multiplier);
        if (amount <= 0.0F) {
            return false;
        }
        float absorptionDamage = Math.min(target.getAbsorptionAmount(), amount * 0.5F);
        if (absorptionDamage > 0.0F) {
            target.setAbsorptionAmount(target.getAbsorptionAmount() - absorptionDamage);
        }
        target.setHealth(Math.max(0.0F, target.getHealth() - (amount - absorptionDamage)));
        level().broadcastEntityEvent(target, (byte) 2);
        if (target.getHealth() <= 0.0F) {
            target.die(damageSources().mobAttack(this));
        }
        return true;
    }

    protected boolean usesDamageAdaptation() {
        return supportsDamageAdaptation() && level() instanceof ServerLevel serverLevel
                && !hasEffect(ModMobEffects.ANTIMALL)
                && EvolutionSystem.generationProfile(serverLevel).adaptation();
    }

    /** Whether this legacy entity class owns the malleable damage-adaptation state. */
    public boolean supportsDamageAdaptation() {
        return true;
    }

    /** Number of learned hits required to reach a damage source's reduction cap. */
    protected int maxDamageAdaptationHits() {
        return MAX_ADAPTATION_HITS;
    }

    /** Fraction of a source's damage removed by each learned hit. */
    protected float damageAdaptationPerHit() {
        return ADAPTATION_PER_HIT;
    }

    /** Caps the number of independent damage sources an entity can learn. */
    protected int maxLearnableDamageSources() {
        return DEFAULT_MAX_LEARNABLE_DAMAGE_SOURCES;
    }

    protected float damageAdaptationLearningChance() {
        return 0.70F;
    }

    /** Chance for fire damage to block all adaptation learning for the next ten ticks. */
    protected float fireAdaptationSuppressionChance() {
        return 0.70F;
    }

    /** Multiplier used by exceptional forms such as Host II. */
    protected float damageAdaptationEffectiveness() {
        return 1.0F;
    }

    protected boolean shouldLearnDamageSource(DamageSource source, String damageId, int previousHits) {
        return random.nextFloat() < damageAdaptationLearningChance();
    }

    public static String damageTypeId(DamageSource source) {
        String taczGunDamageId = taczGunDamageId(source);
        if (taczGunDamageId != null) {
            return taczGunDamageId;
        }
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        LivingEntity livingSource = direct instanceof LivingEntity living ? living
                : attacker instanceof LivingEntity living ? living : null;
        if (livingSource instanceof Player player) {
            ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.isEmpty()) {
                return BuiltInRegistries.ITEM.getKey(heldItem.getItem()).toString();
            }
            return source.getMsgId();
        }
        if (livingSource != null) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(livingSource.getType()).toString();
        }
        return source.getMsgId();
    }

    private static String taczGunDamageId(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (!source.is(TACZ_BULLET_DAMAGE) && !isTaczBullet(direct)) {
            return null;
        }

        ResourceLocation gunId = gunIdFromTaczBullet(direct);
        if (gunId == null && source.getEntity() instanceof LivingEntity shooter) {
            gunId = gunIdFromTaczItem(shooter.getMainHandItem());
        }
        return gunId == null ? "tacz:bullet"
                : "tacz:gun/" + gunId.getNamespace() + "/" + gunId.getPath();
    }

    private static boolean isTaczBullet(Entity entity) {
        if (entity == null) {
            return false;
        }
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return entityId.getNamespace().equals("tacz") && entityId.getPath().equals("bullet");
    }

    private static ResourceLocation gunIdFromTaczBullet(Entity bullet) {
        if (!isTaczBullet(bullet)) {
            return null;
        }
        Optional<Method> method = TACZ_BULLET_GUN_ID_METHODS.computeIfAbsent(bullet.getClass(),
                type -> findPublicMethod(type, "getGunId"));
        return invokeGunId(method, bullet);
    }

    private static ResourceLocation gunIdFromTaczItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Object item = stack.getItem();
        Optional<Method> method = TACZ_ITEM_GUN_ID_METHODS.computeIfAbsent(item.getClass(),
                type -> findPublicMethod(type, "getGunId", ItemStack.class));
        return invokeGunId(method, item, stack);
    }

    private static Optional<Method> findPublicMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return Optional.of(type.getMethod(name, parameterTypes));
        } catch (NoSuchMethodException | SecurityException ignored) {
            return Optional.empty();
        }
    }

    private static ResourceLocation invokeGunId(Optional<Method> method, Object owner, Object... arguments) {
        if (method.isEmpty()) {
            return null;
        }
        try {
            Object result = method.get().invoke(owner, arguments);
            return result instanceof ResourceLocation id ? id : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    public byte getAdaptationHitStatus() {
        return entityData.get(ADAPTATION_HIT_STATUS);
    }

    public void seedGlobalAdaptation(String damageId, int points) {
        if (!supportsDamageAdaptation() || damageId == null || damageId.isBlank() || points <= 0
                || !damageAdaptations.containsKey(damageId)
                && damageAdaptations.size() >= maxLearnableDamageSources()) {
            return;
        }
        // The legacy spawn loop calls addResistance repeatedly, but its new-source cooldown
        // means only the first call is accepted during the same tick.
        damageAdaptations.put(damageId, 1);
        colonySpawned = true;
    }

    public void removeInheritedGlobalAdaptation(String damageId, int points) {
        if (!colonySpawned || damageId == null || points <= 0) {
            return;
        }
        int remaining = damageAdaptations.getOrDefault(damageId, 0) - points;
        if (remaining > 0) {
            damageAdaptations.put(damageId, remaining);
        } else {
            damageAdaptations.remove(damageId);
        }
    }

    public String mostCommonAdaptedDamage() {
        String damage = null;
        int points = 0;
        for (Map.Entry<String, Integer> entry : damageAdaptations.entrySet()) {
            if (entry.getValue() > points) {
                damage = entry.getKey();
                points = entry.getValue();
            }
        }
        return damage;
    }

    public void increaseAllResistances() {
        damageAdaptations.replaceAll((damage, points) -> points == Integer.MAX_VALUE
                ? points : points + 1);
    }

    public void reduceAllResistances(int points) {
        if (points <= 0) {
            return;
        }
        damageAdaptations.replaceAll((damage, current) -> current - points);
        damageAdaptations.values().removeIf(current -> current <= 0);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        parasiteKills++;
        legacyKillCount = Math.max(legacyKillCount, parasiteKills);
        onParasiteKill(level, victim, parasiteKills);
        return super.killedEntity(level, victim);
    }

    protected void onParasiteKill(ServerLevel level, LivingEntity victim, int kills) {
        int requiredKills = requiredAdaptationKills();
        if (kills < requiredKills || adaptedFormSpawned || isRemoved()) {
            return;
        }
        Mob adapted = createAdaptedForm(level);
        if (adapted == null) {
            return;
        }
        adaptedFormSpawned = true;
        adapted.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        adapted.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        adapted.setCustomName(getCustomName());
        adapted.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            adapted.setPersistenceRequired();
        }
        if (level.addFreshEntity(adapted)) {
            discard();
        } else {
            adaptedFormSpawned = false;
        }
    }

    private int requiredAdaptationKills() {
        return getType() == ModEntities.PRI_BOLSTER.get() ? 30 : 10;
    }

    private Mob createAdaptedForm(ServerLevel level) {
        EntityType<?> type = getType();
        if (type == ModEntities.PRI_LONGARMS.get()) return ModEntities.ADA_LONGARMS.get().create(level);
        if (type == ModEntities.PRI_SUMMONER.get()) return ModEntities.ADA_SUMMONER.get().create(level);
        if (type == ModEntities.PRI_VERMIN.get()) return ModEntities.ADA_VERMIN.get().create(level);
        if (type == ModEntities.PRI_VISCERA.get()) return ModEntities.ADA_VISCERA.get().create(level);
        if (type == ModEntities.PRI_ARACHNIDA.get()) return ModEntities.ADA_ARACHNIDA.get().create(level);
        if (type == ModEntities.PRI_BOLSTER.get()) return ModEntities.ADA_BOLSTER.get().create(level);
        if (type == ModEntities.PRI_BURROWER.get()) return ModEntities.ADA_BURROWER.get().create(level);
        if (type == ModEntities.PRI_DEVOURER.get()) return ModEntities.ADA_DEVOURER.get().create(level);
        if (type == ModEntities.PRI_MANDUCATER.get()) return ModEntities.ADA_MANDUCATER.get().create(level);
        if (type == ModEntities.PRI_REEKER.get()) return ModEntities.ADA_REEKER.get().create(level);
        if (type == ModEntities.PRI_TOZOON.get()) return ModEntities.ADA_TOZOON.get().create(level);
        if (type == ModEntities.PRI_YELLOWEYE.get()) return ModEntities.ADA_YELLOWEYE.get().create(level);
        return null;
    }

    protected void hurtNearby(Entity center, double radius, float damage, boolean launch) {
        DragonEggAssimilationEntity.assimilateDragonEggs(level(), center.getBoundingBox().inflate(radius));
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(radius), this::isValidParasiteTarget)) {
            if (target.hurt(damageSources().mobAttack(this), damage) && launch) {
                double x = target.getX() - getX();
                double z = target.getZ() - getZ();
                double length = Math.max(0.001, Math.sqrt(x * x + z * z));
                target.push(x / length * 0.4, target instanceof net.minecraft.world.entity.player.Player ? 0.525 : 1.05,
                        z / length * 0.4);
            }
        }
    }

    public int getParasiteKills() {
        return parasiteKills;
    }

    protected boolean consumeParasiteKill() {
        if (parasiteKills <= 0) {
            return false;
        }
        parasiteKills--;
        legacyKillCount = Math.max(0.0D, legacyKillCount - 1.0D);
        return true;
    }

    protected final void copyDamageAdaptationsTo(PrimitiveParasiteEntity target) {
        target.damageAdaptations.clear();
        target.damageAdaptations.putAll(damageAdaptations);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(KILLS_TAG, parasiteKills);
        tag.putDouble(LEGACY_KILLCOUNT_TAG, legacyKillCount);
        tag.putBoolean(COLONY_SPAWNED_TAG, colonySpawned);
        ListTag adaptations = new ListTag();
        damageAdaptations.forEach((id, hits) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            entry.putInt("hits", hits);
            adaptations.add(entry);
        });
        tag.put(ADAPTATIONS_TAG, adaptations);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        parasiteKills = tag.getInt(KILLS_TAG);
        legacyKillCount = tag.contains(LEGACY_KILLCOUNT_TAG) ? tag.getDouble(LEGACY_KILLCOUNT_TAG) : parasiteKills;
        colonySpawned = tag.getBoolean(COLONY_SPAWNED_TAG);
        damageAdaptations.clear();
        for (Tag raw : tag.getList(ADAPTATIONS_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            damageAdaptations.put(entry.getString("id"), entry.getInt("hits"));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
