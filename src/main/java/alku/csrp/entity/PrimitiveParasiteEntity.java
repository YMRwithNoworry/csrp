package alku.csrp.entity;

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

import java.util.HashMap;
import java.util.Map;

/** Shared 1.12 primitive-parasite state: hostile targeting, kills, and repeated-damage adaptation. */
public abstract class PrimitiveParasiteEntity extends Monster implements GeoEntity, Parasite {
    private static final String KILLS_TAG = "parasitekills";
    private static final String ADAPTATIONS_TAG = "damage_adaptations";
    private static final int MAX_ADAPTATION_HITS = 8;
    private static final float ADAPTATION_PER_HIT = 0.1F;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, Integer> damageAdaptations = new HashMap<>();
    private int parasiteKills;

    protected PrimitiveParasiteEntity(EntityType<? extends PrimitiveParasiteEntity> type, Level level) {
        super(type, level);
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
        String damageId = damageTypeId(source);
        int previousHits = damageAdaptations.getOrDefault(damageId, 0);
        float reduction = Math.min(MAX_ADAPTATION_HITS, previousHits) * ADAPTATION_PER_HIT;
        boolean hurt = super.hurt(source, amount * (1.0F - reduction));
        if (hurt && !level().isClientSide) {
            damageAdaptations.put(damageId, Math.min(MAX_ADAPTATION_HITS, previousHits + 1));
        }
        return hurt;
    }

    private static String damageTypeId(DamageSource source) {
        return source.typeHolder().unwrapKey().map(ResourceKey<DamageType>::location)
                .map(Object::toString).orElse("direct");
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        parasiteKills++;
        onParasiteKill(level, victim, parasiteKills);
        return super.killedEntity(level, victim);
    }

    protected void onParasiteKill(ServerLevel level, LivingEntity victim, int kills) {
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
