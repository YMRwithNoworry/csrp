package alku.csrp.entity;

import alku.csrp.Config;
import alku.csrp.registry.ModEntities;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shared 1.12 primitive-parasite state: hostile targeting, kills, and repeated-damage adaptation. */
public abstract class PrimitiveParasiteEntity extends Monster implements GeoEntity, Parasite {
    private static final String KILLS_TAG = "parasitekills";
    private static final String LEGACY_KILLCOUNT_TAG = "legacy_killcount";
    private static final String ADAPTATIONS_TAG = "damage_adaptations";
    private static final String COLONY_SPAWNED_TAG = "colony_spawned";
    private static final int MAX_ADAPTATION_HITS = 8;
    private static final float ADAPTATION_PER_HIT = 0.1F;
    private static final int DEFAULT_MAX_LEARNABLE_DAMAGE_SOURCES = Integer.MAX_VALUE;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, Integer> damageAdaptations = new LinkedHashMap<>();
    private int parasiteKills;
    private double legacyKillCount;
    private boolean colonySpawned;

    protected PrimitiveParasiteEntity(EntityType<? extends PrimitiveParasiteEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 20 == 0 && !Config.useEvolutionPhases()
                && level().getDifficulty() == Difficulty.HARD && Config.killcountPlus() > 0.0D) {
            double previous = legacyKillCount;
            legacyKillCount += Config.killcountPlus();
            int previousKills = (int) Math.floor(previous);
            int currentKills = (int) Math.floor(legacyKillCount);
            if (currentKills > previousKills) {
                parasiteKills = Math.max(parasiteKills, currentKills);
                if (previousKills < 10 && currentKills >= 10 && level() instanceof ServerLevel serverLevel) {
                    onParasiteKill(serverLevel, this, parasiteKills);
                }
            }
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    protected boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if ((attacker instanceof Parasite && attacker != this)
                || (direct instanceof Parasite && direct != this)) {
            return false;
        }
        if (!usesDamageAdaptation()) {
            return super.hurt(source, amount);
        }
        String damageId = damageTypeId(source);
        int previousHits = damageAdaptations.getOrDefault(damageId, 0);
        float reduction = Math.min(maxDamageAdaptationHits(), previousHits) * damageAdaptationPerHit();
        boolean hurt = super.hurt(source, amount * (1.0F - reduction));
        if (hurt && !level().isClientSide && (damageAdaptations.containsKey(damageId)
                || damageAdaptations.size() < maxLearnableDamageSources())
                && shouldLearnDamageSource(source, damageId, previousHits)) {
            damageAdaptations.put(damageId, Math.min(maxDamageAdaptationHits(), previousHits + 1));
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !(target instanceof Parasite) && super.doHurtTarget(target);
    }

    protected boolean usesDamageAdaptation() {
        return level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).adaptation();
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

    /** Allows entity tiers with legacy learning odds to reject an adaptation hit. */
    protected boolean shouldLearnDamageSource(DamageSource source, String damageId, int previousHits) {
        return previousHits < maxDamageAdaptationHits();
    }

    public static String damageTypeId(DamageSource source) {
        return source.typeHolder().unwrapKey().map(ResourceKey<DamageType>::location)
                .map(Object::toString).orElse("direct");
    }

    public void seedGlobalAdaptation(String damageId, int points) {
        if (damageId == null || damageId.isBlank() || points <= 0) {
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

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        parasiteKills++;
        legacyKillCount = Math.max(legacyKillCount, parasiteKills);
        onParasiteKill(level, victim, parasiteKills);
        return super.killedEntity(level, victim);
    }

    protected void onParasiteKill(ServerLevel level, LivingEntity victim, int kills) {
        if (kills < 10) {
            return;
        }
        Mob adapted = createAdaptedForm(level);
        if (adapted == null) {
            return;
        }
        adapted.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        adapted.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        adapted.setCustomName(getCustomName());
        adapted.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            adapted.setPersistenceRequired();
        }
        level.addFreshEntity(adapted);
        discard();
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
