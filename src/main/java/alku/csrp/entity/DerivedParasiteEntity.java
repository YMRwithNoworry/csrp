package alku.csrp.entity;

import alku.csrp.effect.EffectStacking;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared derived-tier behavior from the legacy cosmical parasite layer.
 *
 * <p>Derived parasites use a light-sensitive shadow state, create a temporary
 * decoy when exposed, and can establish a five-target neural connection.</p>
 */
public abstract class DerivedParasiteEntity extends PrimitiveParasiteEntity {
    public static final int NEURAL_LINK_TARGET_LIMIT = 5;
    public static final double NEURAL_LINK_RANGE = 24.0D;
    private static final int SHADOW_LIGHT_THRESHOLD = 7;
    private static final int SHADOW_DAMAGE_TIMEOUT_TICKS = 100;
    private static final int SHADOW_REVEAL_TIMEOUT_TICKS = 200;
    private static final int SHADOW_CLONE_LIFETIME_TICKS = 23 * 21;
    private static final int SHADOW_CLONE_COOLDOWN_TICKS = 10 * 21;
    private static final byte SHADOW_HIT_EVENT = 41;
    private static final int SHADOW_HIT_FLASH_TICKS = 15;
    private static final int COSMIC_ORB_COUNT = 3;
    private static final int COSMIC_ORB_INTERVAL_TICKS = 40;
    private static final int COSMIC_ORB_CHARGE_TICKS = 160;
    private static final int COSMIC_ORB_CAST_TICKS = 220;
    private static final double COSMIC_ORB_MIN_DISTANCE_SQR = 10.0D * 10.0D;
    private static final double SHARED_SKILL_MAX_DISTANCE_SQR = 100.0D * 100.0D;
    private static final int NEURAL_LINK_CHARGE_TICKS = 240;
    private static final int NEURAL_LINK_DURATION_TICKS = 140;
    private static final int NEURAL_LINK_LOCK_TICKS = 60;
    private static final int NEURAL_LINK_EFFECT_INTERVAL_TICKS = 10;
    private static final int NEURAL_LINK_EFFECT_OFFSET_TICKS = 5;
    private static final int NEURAL_NEGATIVE_EFFECT_DURATION_TICKS = 140;
    private static final float NEURAL_HEAL_PER_EFFECT = 0.01F;
    private static final int DERIVED_DAMAGE_CAP = 25;
    private static final float DERIVED_MINIMUM_DAMAGE = 14.0F;
    private static final float DERIVED_REGENERATION = 25.0F;
    private static final int DERIVED_REGENERATION_USES = 10;
    private static final int DERIVED_ORB_ITEM_COOLDOWN_TICKS = 20 * 20;
    private static final int DERIVED_ORB_EXPERIENCE_STEAL = 340;
    private static final List<Holder<MobEffect>> NEURAL_NEGATIVE_EFFECTS = List.of(
            MobEffects.DIG_SLOWDOWN,
            MobEffects.CONFUSION,
            MobEffects.BLINDNESS,
            MobEffects.HUNGER,
            MobEffects.WEAKNESS,
            ModMobEffects.BLEED,
            ModMobEffects.EFFECTNEG,
            ModMobEffects.EFFECTPOS,
            ModMobEffects.MUSCLEOUT,
            ModMobEffects.OVERHEATING,
            ModMobEffects.INDEAF,
            ModMobEffects.NOVISION,
            ModMobEffects.BRAINING);

    private static final EntityDataAccessor<Boolean> SHADOWED = SynchedEntityData.defineId(
            DerivedParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHADOW_CLONE = SynchedEntityData.defineId(
            DerivedParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NEURAL_LINK_ACTIVE = SynchedEntityData.defineId(
            DerivedParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> NEURAL_LINK_TICKS = SynchedEntityData.defineId(
            DerivedParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> NEURAL_TARGET_0 = SynchedEntityData.defineId(
            DerivedParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> NEURAL_TARGET_1 = SynchedEntityData.defineId(
            DerivedParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> NEURAL_TARGET_2 = SynchedEntityData.defineId(
            DerivedParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> NEURAL_TARGET_3 = SynchedEntityData.defineId(
            DerivedParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> NEURAL_TARGET_4 = SynchedEntityData.defineId(
            DerivedParasiteEntity.class, EntityDataSerializers.INT);
    private static final List<EntityDataAccessor<Integer>> NEURAL_TARGETS = List.of(
            NEURAL_TARGET_0, NEURAL_TARGET_1, NEURAL_TARGET_2, NEURAL_TARGET_3, NEURAL_TARGET_4);

    private float shadowDamage;
    private int shadowDamageTimeout;
    private int shadowCloneCooldown;
    private int cloneLifeTicks;
    private int regenerationUses = DERIVED_REGENERATION_USES;
    private int neuralLinkCharge;
    private int neuralLinkCastTicks;
    private int cosmicOrbCharge;
    private int cosmicOrbCastTicks;
    private int cosmicOrbBurstsRemaining;
    private int cosmicOrbInterval;
    private int shadowHitFlashTicks;
    private float previousShadowRenderAlpha;
    private float shadowRenderAlpha;
    private int shadowRenderAlphaCooldown;
    private UUID cloneParent;
    private UUID activeClone;

    protected DerivedParasiteEntity(EntityType<? extends DerivedParasiteEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return 5;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return 0.20F;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return 30;
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return 1.0F;
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return 0.10F;
    }

    @Override
    protected int incomingDamageCapDivisor() {
        return level() instanceof ServerLevel serverLevel
                && EvolutionSystem.generationProfile(serverLevel).damageCap()
                ? DERIVED_DAMAGE_CAP : 1;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHADOWED, true);
        builder.define(SHADOW_CLONE, false);
        builder.define(NEURAL_LINK_ACTIVE, false);
        builder.define(NEURAL_LINK_TICKS, 0);
        for (EntityDataAccessor<Integer> target : NEURAL_TARGETS) {
            builder.define(target, 0);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickShadowRenderAlpha();
            spawnShadowParticles();
            spawnShadowHitParticles();
            return;
        }

        tickRegeneration();
        tickShadowState();
        EvolutionSystem.GenerationProfile profile = EvolutionSystem.generationProfile((ServerLevel) level());
        if (!hasExclusiveSkill()) {
            if (profile.ordinaryOrb()) {
                tickCosmicOrbs();
            } else {
                disableCosmicOrbs();
            }
            if (profile.specialMoves()) {
                tickNeuralLink();
            } else {
                disableNeuralLink();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(source, amount);
        }
        if (!isShadowed() && !isShadowClone()) {
            boolean hurt = super.hurt(source, amount);
            if (hurt) {
                reflectAdaptedDamage(source);
            }
            return hurt;
        }

        if (!isShadowClone() && isShadowProtected()) {
            shadowDamageTimeout = SHADOW_DAMAGE_TIMEOUT_TICKS;
            level().broadcastEntityEvent(this, SHADOW_HIT_EVENT);
            super.hurt(source, 0.0F);
            return false;
        }

        shadowDamage += amount;
        shadowDamageTimeout = SHADOW_DAMAGE_TIMEOUT_TICKS;
        if (!isShadowClone() && shadowDamage >= getMaxHealth() * 0.10F) {
            setShadowed(false);
            if (spawnShadowClone()) {
                shadowDamageTimeout = SHADOW_REVEAL_TIMEOUT_TICKS;
            }
            shadowDamage = 0.0F;
        }
        level().broadcastEntityEvent(this, SHADOW_HIT_EVENT);
        super.hurt(source, 0.0F);
        return false;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof LivingEntity living && !(target instanceof Parasite)) {
            applyMinimumDamage(living, DERIVED_MINIMUM_DAMAGE);
        }
        return super.doHurtTarget(target);
    }

    @Override
    public boolean applyScaryOrbEffect(LivingEntity target, int nearbyEntities) {
        if (target == this || target instanceof Player player && player.getAbilities().instabuild
                || target instanceof DerivedParasiteEntity derived && derived.isShadowClone()) {
            return false;
        }
        if (target instanceof Player player) {
            player.causeFoodExhaustion(40.0F);
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 15 * 20, 4, false, true), this);
            int experience = Math.min(DERIVED_ORB_EXPERIENCE_STEAL, player.totalExperience);
            if (experience > 0) {
                player.giveExperiencePoints(-experience);
            }
            Set<Item> cooledItems = new HashSet<>();
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && cooledItems.add(stack.getItem())) {
                    player.getCooldowns().addCooldown(stack.getItem(), DERIVED_ORB_ITEM_COOLDOWN_TICKS);
                }
            }
            ItemStack offhand = player.getOffhandItem();
            if (!offhand.isEmpty() && cooledItems.add(offhand.getItem())) {
                player.getCooldowns().addCooldown(offhand.getItem(), DERIVED_ORB_ITEM_COOLDOWN_TICKS);
            }
        }
        target.addEffect(new MobEffectInstance(ModMobEffects.COTH, 1200, 3, false, false), this);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false), this);
        return true;
    }

    @Override
    public float scaryOrbMinimumDamage() {
        return DERIVED_MINIMUM_DAMAGE;
    }

    public boolean applyMinimumDamage(LivingEntity target, float amount) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel).minimumDamage()
                || amount <= 0.0F || target == this || !target.isAlive()
                || target instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        float absorption = target.getAbsorptionAmount();
        float healthDamage = absorption > 0.0F ? amount * 0.5F : amount;
        if (absorption > 0.0F) {
            target.setAbsorptionAmount(Math.max(0.0F, absorption - amount * 0.5F));
        }
        target.setHealth(Math.max(0.0F, target.getHealth() - healthDamage));
        level().broadcastEntityEvent(target, (byte) 2);
        return true;
    }

    protected boolean hasExclusiveSkill() {
        return false;
    }

    public boolean isUsingDerivedSkill() {
        return cosmicOrbCastTicks > 0 || neuralLinkCastTicks > 0;
    }

    private void reflectAdaptedDamage(DamageSource source) {
        float reflected = lastDamageAdaptationReduction() * 0.5F;
        if (reflected <= 0.0F || !(source.getEntity() instanceof LivingEntity attacker)
                || attacker == this || attacker instanceof Parasite || !attacker.isAlive()) {
            return;
        }
        attacker.hurt(damageSources().mobAttack(this), reflected);
        applyMinimumDamage(attacker, reflected);
    }

    private void tickRegeneration() {
        if (tickCount % 20 != 10 || isShadowClone() || isOnFire() || getParasiteKills() <= 1
                || getHealth() <= 0.0F || getHealth() >= getMaxHealth()) {
            return;
        }
        heal(DERIVED_REGENERATION);
        if (--regenerationUses <= 0) {
            consumeParasiteKill();
            regenerationUses = DERIVED_REGENERATION_USES;
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == SHADOW_HIT_EVENT) {
            shadowHitFlashTicks = SHADOW_HIT_FLASH_TICKS;
            previousShadowRenderAlpha = shadowRenderAlpha;
            shadowRenderAlpha = 0.60F;
            shadowRenderAlphaCooldown = 40;
            return;
        }
        super.handleEntityEvent(id);
    }

    private void tickShadowState() {
        if (isShadowClone()) {
            cloneLifeTicks++;
            if (cloneLifeTicks >= SHADOW_CLONE_LIFETIME_TICKS || !hasLiveParent()) {
                restoreParentShadow();
                discard();
            }
            return;
        }

        if (!isShadowed()) {
            if (hasLiveClone()) {
                syncCloneTarget();
                return;
            }
            if (shadowDamageTimeout > 0) {
                shadowDamageTimeout--;
            }
            if (shadowDamageTimeout <= 0) {
                setShadowed(true);
                shadowCloneCooldown = SHADOW_CLONE_COOLDOWN_TICKS;
            }
            return;
        }

        if (shadowCloneCooldown > 0) {
            shadowCloneCooldown--;
        }
        if (tickCount % 20 == 0 && shadowCloneCooldown <= 0 && random.nextInt(100) == 0) {
            spawnShadowClone();
        }
    }

    private boolean isShadowProtected() {
        return level().getMaxLocalRawBrightness(blockPosition()) <= SHADOW_LIGHT_THRESHOLD;
    }

    private boolean spawnShadowClone() {
        if (isShadowClone() || hasLiveClone() || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Entity entity = getType().create(serverLevel);
        if (!(entity instanceof DerivedParasiteEntity clone)) {
            return false;
        }

        clone.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        clone.markAsShadowClone(getUUID());
        clone.setTarget(getTarget());
        scaleCloneAttribute(clone, Attributes.MOVEMENT_SPEED, 1.33D);
        scaleCloneAttribute(clone, Attributes.ATTACK_DAMAGE, 0.50D);
        clone.setHealth(clone.getMaxHealth());
        clone.setCustomName(getCustomName());
        clone.setCustomNameVisible(isCustomNameVisible());
        serverLevel.addFreshEntity(clone);
        activeClone = clone.getUUID();
        setShadowed(false);
        shadowCloneCooldown = SHADOW_CLONE_COOLDOWN_TICKS;
        return true;
    }

    private static void scaleCloneAttribute(DerivedParasiteEntity clone,
            Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double multiplier) {
        AttributeInstance instance = clone.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(instance.getBaseValue() * multiplier);
        }
    }

    private void markAsShadowClone(UUID parent) {
        cloneParent = parent;
        cloneLifeTicks = 0;
        entityData.set(SHADOW_CLONE, true);
        entityData.set(SHADOWED, true);
    }

    private boolean hasLiveClone() {
        if (activeClone == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Entity clone = serverLevel.getEntity(activeClone);
        return clone instanceof DerivedParasiteEntity derived && derived.isAlive();
    }

    private boolean hasLiveParent() {
        if (cloneParent == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Entity parent = serverLevel.getEntity(cloneParent);
        return parent instanceof DerivedParasiteEntity derived && derived.isAlive();
    }

    private void syncCloneTarget() {
        if (activeClone == null || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity clone = serverLevel.getEntity(activeClone);
        if (clone instanceof DerivedParasiteEntity derived && getTarget() != null) {
            derived.setTarget(getTarget());
        }
    }

    private void restoreParentShadow() {
        if (cloneParent == null || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity parent = serverLevel.getEntity(cloneParent);
        if (parent instanceof DerivedParasiteEntity derived && derived.isAlive()) {
            derived.activeClone = null;
            derived.setShadowed(true);
            derived.shadowCloneCooldown = SHADOW_CLONE_COOLDOWN_TICKS;
        }
    }

    private void tickCosmicOrbs() {
        LivingEntity target = getTarget();
        if (cosmicOrbCastTicks > 0) {
            getNavigation().stop();
            if (target == null || !target.isAlive() || isShadowClone()) {
                finishCosmicOrbCast();
                return;
            }
            cosmicOrbCastTicks++;
            if (cosmicOrbInterval > 0) {
                cosmicOrbInterval--;
            }
            if (cosmicOrbBurstsRemaining > 0 && cosmicOrbInterval <= 0) {
                spawnCosmicOrb(target);
                cosmicOrbBurstsRemaining--;
                cosmicOrbInterval = COSMIC_ORB_INTERVAL_TICKS;
            }
            if (cosmicOrbCastTicks >= COSMIC_ORB_CAST_TICKS) {
                finishCosmicOrbCast();
            }
            return;
        }

        if (neuralLinkCastTicks > 0) {
            return;
        }
        if (target == null || !target.isAlive() || isShadowClone()) {
            return;
        }
        double distance = distanceToSqr(target);
        if (distance >= COSMIC_ORB_MIN_DISTANCE_SQR && distance < SHARED_SKILL_MAX_DISTANCE_SQR
                && ++cosmicOrbCharge >= COSMIC_ORB_CHARGE_TICKS) {
            cosmicOrbCharge = 0;
            cosmicOrbCastTicks = 1;
            cosmicOrbBurstsRemaining = COSMIC_ORB_COUNT;
            cosmicOrbInterval = 0;
        }
    }

    private void finishCosmicOrbCast() {
        cosmicOrbCastTicks = 0;
        cosmicOrbBurstsRemaining = 0;
        cosmicOrbInterval = 0;
    }

    private void disableCosmicOrbs() {
        cosmicOrbCharge = 0;
        finishCosmicOrbCast();
    }

    private void spawnCosmicOrb(LivingEntity target) {
        ScaryOrbEntity orb = new ScaryOrbEntity(ModEntities.SCARY_ORB.get(), level(), this);
        orb.setTimings(15, 13);
        orb.setAnchor(target.position());
        level().addFreshEntity(orb);
        playSound(ModSounds.ORB_START.get(), 1.0F, 1.0F);
    }

    private void tickNeuralLink() {
        LivingEntity target = getTarget();
        if (neuralLinkCastTicks > 0) {
            getNavigation().stop();
            neuralLinkCastTicks++;
            entityData.set(NEURAL_LINK_TICKS,
                    Math.max(0, NEURAL_LINK_DURATION_TICKS - neuralLinkCastTicks));
            if (neuralLinkCastTicks < NEURAL_LINK_LOCK_TICKS && neuralLinkCastTicks % 20 == 0) {
                level().broadcastEntityEvent(this, SHADOW_HIT_EVENT);
            }
            if (neuralLinkCastTicks >= NEURAL_LINK_LOCK_TICKS) {
                int elapsed = neuralLinkCastTicks - NEURAL_LINK_LOCK_TICKS;
                validateNeuralTargets();
                if (elapsed % 20 == 0 && !hasFullNeuralTargetList()) {
                    acquireNeuralTargets();
                }
                boolean hasTargets = hasNeuralTargets();
                entityData.set(NEURAL_LINK_ACTIVE, hasTargets);
                if (hasTargets
                        && elapsed % NEURAL_LINK_EFFECT_INTERVAL_TICKS == NEURAL_LINK_EFFECT_OFFSET_TICKS) {
                    applyNeuralLinkEffects();
                }
            }
            if (neuralLinkCastTicks >= NEURAL_LINK_DURATION_TICKS) {
                clearNeuralLink();
            }
            return;
        }

        if (cosmicOrbCastTicks > 0) {
            return;
        }
        if (target != null && target.isAlive() && distanceToSqr(target) < SHARED_SKILL_MAX_DISTANCE_SQR
                && ++neuralLinkCharge >= NEURAL_LINK_CHARGE_TICKS) {
            neuralLinkCharge = 0;
            if (canStartNeuralLink()) {
                neuralLinkCastTicks = 1;
                entityData.set(NEURAL_LINK_TICKS, NEURAL_LINK_DURATION_TICKS);
            }
        }
    }

    private boolean canStartNeuralLink() {
        LivingEntity target = getTarget();
        return !isShadowClone() && isShadowed() && onGround()
                && target != null && isValidNeuralTarget(target);
    }

    private void acquireNeuralTargets() {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(NEURAL_LINK_RANGE), this::isValidNeuralTarget)) {
            if (containsNeuralTarget(target.getId())) {
                continue;
            }
            for (EntityDataAccessor<Integer> targetId : NEURAL_TARGETS) {
                if (entityData.get(targetId) == 0) {
                    entityData.set(targetId, target.getId());
                    break;
                }
            }
            if (hasFullNeuralTargetList()) {
                return;
            }
        }
    }

    private boolean containsNeuralTarget(int entityId) {
        for (EntityDataAccessor<Integer> target : NEURAL_TARGETS) {
            if (entityData.get(target) == entityId) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidNeuralTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite)
                && !(target instanceof Player player && player.getAbilities().invulnerable);
    }

    private void validateNeuralTargets() {
        for (EntityDataAccessor<Integer> targetId : NEURAL_TARGETS) {
            int entityId = entityData.get(targetId);
            if (entityId == 0) {
                continue;
            }
            Entity entity = level().getEntity(entityId);
            if (!(entity instanceof LivingEntity target) || !isValidNeuralTarget(target)
                    || distanceToSqr(target) > NEURAL_LINK_RANGE * NEURAL_LINK_RANGE) {
                entityData.set(targetId, 0);
            }
        }
    }

    private boolean hasNeuralTargets() {
        for (EntityDataAccessor<Integer> target : NEURAL_TARGETS) {
            if (entityData.get(target) != 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFullNeuralTargetList() {
        for (EntityDataAccessor<Integer> target : NEURAL_TARGETS) {
            if (entityData.get(target) == 0) {
                return false;
            }
        }
        return true;
    }

    private void applyNeuralLinkEffects() {
        float healed = 0.0F;
        for (EntityDataAccessor<Integer> targetId : NEURAL_TARGETS) {
            Entity entity = level().getEntity(entityData.get(targetId));
            if (!(entity instanceof LivingEntity target) || !isValidNeuralTarget(target)) {
                continue;
            }

            int removedAmplifierSum = 0;
            for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL
                        && target.removeEffect(effect.getEffect())) {
                    removedAmplifierSum += effect.getAmplifier() + 1;
                }
            }
            healed += getMaxHealth() * NEURAL_HEAL_PER_EFFECT * removedAmplifierSum;
            Holder<MobEffect> negativeEffect = NEURAL_NEGATIVE_EFFECTS.get(
                    random.nextInt(NEURAL_NEGATIVE_EFFECTS.size()));
            EffectStacking.apply(target, negativeEffect, NEURAL_NEGATIVE_EFFECT_DURATION_TICKS, 1);
            level().broadcastEntityEvent(this, SHADOW_HIT_EVENT);
        }
        if (healed > 0.0F) {
            heal(healed);
        }
    }

    private void clearNeuralLink() {
        neuralLinkCastTicks = 0;
        entityData.set(NEURAL_LINK_ACTIVE, false);
        entityData.set(NEURAL_LINK_TICKS, 0);
        for (EntityDataAccessor<Integer> target : NEURAL_TARGETS) {
            entityData.set(target, 0);
        }
    }

    private void spawnShadowParticles() {
        if (!isShadowed() || tickCount % 4 != 0) {
            return;
        }
        level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                getY() + random.nextDouble() * getBbHeight(),
                getZ() + (random.nextDouble() - 0.5D) * getBbWidth(), 0.0D, 0.01D, 0.0D);
    }

    private void tickShadowRenderAlpha() {
        previousShadowRenderAlpha = shadowRenderAlpha;
        if (shadowRenderAlphaCooldown > 0) {
            shadowRenderAlphaCooldown--;
        } else if (shadowRenderAlpha > 0.0F) {
            shadowRenderAlpha = Math.max(0.0F, shadowRenderAlpha - 0.01F);
        }
        if (tickCount % 21 == 10 && random.nextInt(10) == 0) {
            previousShadowRenderAlpha = shadowRenderAlpha;
            shadowRenderAlpha = 0.60F;
            shadowRenderAlphaCooldown = 40;
        }
    }

    private void disableNeuralLink() {
        neuralLinkCharge = 0;
        clearNeuralLink();
    }

    private void spawnShadowHitParticles() {
        if (shadowHitFlashTicks <= 0) {
            return;
        }
        shadowHitFlashTicks--;
        for (int index = 0; index < 3; index++) {
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + random.nextDouble() * getBbHeight(),
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    (random.nextDouble() - 0.5D) * 0.04D,
                    random.nextDouble() * 0.04D,
                    (random.nextDouble() - 0.5D) * 0.04D);
        }
    }

    public boolean isShadowed() {
        return entityData.get(SHADOWED);
    }

    public void setShadowed(boolean shadowed) {
        boolean changed = entityData.get(SHADOWED) != shadowed;
        entityData.set(SHADOWED, shadowed);
        if (changed && shadowed && !level().isClientSide) {
            level().broadcastEntityEvent(this, SHADOW_HIT_EVENT);
        }
    }

    public boolean isShadowClone() {
        return entityData.get(SHADOW_CLONE);
    }

    public float getShadowRenderAlpha(float partialTick) {
        return Mth.lerp(partialTick, previousShadowRenderAlpha, shadowRenderAlpha);
    }

    public boolean isShadowHitFlashing() {
        return shadowHitFlashTicks > 0;
    }

    public boolean isNeuralLinkActive() {
        return entityData.get(NEURAL_LINK_ACTIVE);
    }

    public int[] getNeuralTargetIds() {
        int[] ids = new int[NEURAL_LINK_TARGET_LIMIT];
        for (int index = 0; index < NEURAL_LINK_TARGET_LIMIT; index++) {
            ids[index] = entityData.get(NEURAL_TARGETS.get(index));
        }
        return ids;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("derived_shadowed", isShadowed());
        tag.putBoolean("derived_shadow_clone", isShadowClone());
        tag.putFloat("derived_shadow_damage", shadowDamage);
        tag.putInt("derived_shadow_timeout", shadowDamageTimeout);
        tag.putInt("derived_shadow_cooldown", shadowCloneCooldown);
        tag.putInt("derived_clone_life", cloneLifeTicks);
        tag.putInt("derived_regeneration_uses", regenerationUses);
        tag.putInt("derived_neural_charge", neuralLinkCharge);
        tag.putInt("derived_neural_cast", neuralLinkCastTicks);
        tag.putInt("derived_cosmic_orb_charge", cosmicOrbCharge);
        tag.putInt("derived_cosmic_orb_cast", cosmicOrbCastTicks);
        tag.putInt("derived_cosmic_orb_remaining", cosmicOrbBurstsRemaining);
        tag.putInt("derived_cosmic_orb_interval", cosmicOrbInterval);
        if (cloneParent != null) {
            tag.putUUID("derived_clone_parent", cloneParent);
        }
        if (activeClone != null) {
            tag.putUUID("derived_active_clone", activeClone);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setShadowed(!tag.contains("derived_shadowed") || tag.getBoolean("derived_shadowed"));
        entityData.set(SHADOW_CLONE, tag.getBoolean("derived_shadow_clone"));
        shadowDamage = tag.getFloat("derived_shadow_damage");
        shadowDamageTimeout = tag.getInt("derived_shadow_timeout");
        shadowCloneCooldown = tag.getInt("derived_shadow_cooldown");
        cloneLifeTicks = tag.getInt("derived_clone_life");
        regenerationUses = tag.contains("derived_regeneration_uses")
                ? Math.max(1, tag.getInt("derived_regeneration_uses")) : DERIVED_REGENERATION_USES;
        neuralLinkCharge = tag.getInt("derived_neural_charge");
        neuralLinkCastTicks = tag.getInt("derived_neural_cast");
        cosmicOrbCharge = tag.getInt("derived_cosmic_orb_charge");
        cosmicOrbCastTicks = tag.getInt("derived_cosmic_orb_cast");
        cosmicOrbBurstsRemaining = tag.getInt("derived_cosmic_orb_remaining");
        cosmicOrbInterval = tag.getInt("derived_cosmic_orb_interval");
        cloneParent = tag.hasUUID("derived_clone_parent") ? tag.getUUID("derived_clone_parent") : null;
        activeClone = tag.hasUUID("derived_active_clone") ? tag.getUUID("derived_active_clone") : null;
        entityData.set(NEURAL_LINK_TICKS,
                Math.max(0, NEURAL_LINK_DURATION_TICKS - neuralLinkCastTicks));
        entityData.set(NEURAL_LINK_ACTIVE, false);
    }
}
