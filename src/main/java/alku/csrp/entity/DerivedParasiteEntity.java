package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModEntities;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private static final int SHADOW_CLONE_LIFETIME_TICKS = 220;
    private static final int SHADOW_CLONE_COOLDOWN_TICKS = 200;
    private static final byte SHADOW_HIT_EVENT = 41;
    private static final int SHADOW_HIT_FLASH_TICKS = 15;
    private static final int COSMIC_ORB_COUNT = 3;
    private static final int COSMIC_ORB_INTERVAL_TICKS = 20;
    private static final int COSMIC_ORB_COOLDOWN_TICKS = 160;
    private static final int NEURAL_LINK_DURATION_TICKS = 100;
    private static final int NEURAL_LINK_COOLDOWN_TICKS = 240;
    private static final int NEURAL_LINK_EFFECT_INTERVAL_TICKS = 20;
    private static final int NEURAL_NEGATIVE_EFFECT_DURATION_TICKS = 140;
    private static final float NEURAL_HEAL_PER_EFFECT = 0.01F;
    private static final List<Holder<MobEffect>> NEURAL_NEGATIVE_EFFECTS = List.of(
            MobEffects.DIG_SLOWDOWN,
            MobEffects.CONFUSION,
            MobEffects.BLINDNESS,
            MobEffects.HUNGER,
            MobEffects.WEAKNESS,
            ModMobEffects.BLEED,
            ModMobEffects.CORROSION,
            ModMobEffects.VIRAL);

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
    private int neuralLinkCooldown = 80;
    private int cosmicOrbCooldown = COSMIC_ORB_COOLDOWN_TICKS;
    private int cosmicOrbBurstsRemaining;
    private int cosmicOrbInterval;
    private int shadowHitFlashTicks;
    private UUID cloneParent;
    private UUID activeClone;

    protected DerivedParasiteEntity(EntityType<? extends DerivedParasiteEntity> type, Level level) {
        super(type, level);
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
            spawnShadowParticles();
            spawnShadowHitParticles();
            spawnNeuralLinkParticles();
            return;
        }

        tickShadowState();
        tickCosmicOrbs();
        tickNeuralLink();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(source, amount);
        }
        if (!isShadowed() && !isShadowClone()) {
            return super.hurt(source, amount);
        }

        if (!isShadowClone() && isShadowProtected()) {
            shadowDamageTimeout = SHADOW_DAMAGE_TIMEOUT_TICKS;
            level().broadcastEntityEvent(this, SHADOW_HIT_EVENT);
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
        return false;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == SHADOW_HIT_EVENT) {
            shadowHitFlashTicks = SHADOW_HIT_FLASH_TICKS;
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
        clone.setHealth(Math.max(1.0F, Math.min(clone.getMaxHealth(), getHealth() * 0.5F)));
        clone.setCustomName(getCustomName());
        clone.setCustomNameVisible(isCustomNameVisible());
        serverLevel.addFreshEntity(clone);
        activeClone = clone.getUUID();
        setShadowed(false);
        shadowCloneCooldown = SHADOW_CLONE_COOLDOWN_TICKS;
        return true;
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
        if (cosmicOrbCooldown > 0) {
            cosmicOrbCooldown--;
        }
        if (cosmicOrbBurstsRemaining > 0) {
            if (cosmicOrbInterval > 0) {
                cosmicOrbInterval--;
                return;
            }
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                spawnCosmicOrb(target);
            }
            cosmicOrbBurstsRemaining--;
            cosmicOrbInterval = COSMIC_ORB_INTERVAL_TICKS;
            return;
        }

        LivingEntity target = getTarget();
        if (cosmicOrbCooldown <= 0 && !isShadowClone() && target != null && target.isAlive()) {
            cosmicOrbBurstsRemaining = COSMIC_ORB_COUNT;
            cosmicOrbInterval = 0;
            cosmicOrbCooldown = COSMIC_ORB_COOLDOWN_TICKS;
        }
    }

    private void spawnCosmicOrb(LivingEntity target) {
        ScaryOrbEntity orb = new ScaryOrbEntity(ModEntities.SCARY_ORB.get(), level(), this);
        orb.setAnchor(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
        level().addFreshEntity(orb);
    }

    private void tickNeuralLink() {
        if (neuralLinkCooldown > 0) {
            neuralLinkCooldown--;
        }

        int remainingTicks = entityData.get(NEURAL_LINK_TICKS);
        if (remainingTicks > 0) {
            remainingTicks--;
            entityData.set(NEURAL_LINK_TICKS, remainingTicks);
            validateNeuralTargets();
            if (remainingTicks % NEURAL_LINK_EFFECT_INTERVAL_TICKS == 0) {
                applyNeuralLinkEffects();
            }
            if (remainingTicks == 0 || !hasNeuralTargets()) {
                clearNeuralLink();
            }
            return;
        }

        if (neuralLinkCooldown <= 0 && canStartNeuralLink()) {
            startNeuralLink();
        }
    }

    private boolean canStartNeuralLink() {
        LivingEntity target = getTarget();
        return !isShadowClone() && isShadowed() && target != null && isValidNeuralTarget(target);
    }

    private void startNeuralLink() {
        List<LivingEntity> targets = new ArrayList<>(level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(NEURAL_LINK_RANGE), this::isValidNeuralTarget));
        targets.sort(Comparator.comparingDouble(this::distanceToSqr));

        LivingEntity primaryTarget = getTarget();
        if (primaryTarget != null && isValidNeuralTarget(primaryTarget)) {
            targets.remove(primaryTarget);
            targets.add(0, primaryTarget);
        }

        int index = 0;
        for (LivingEntity target : targets) {
            if (index >= NEURAL_LINK_TARGET_LIMIT) {
                break;
            }
            entityData.set(NEURAL_TARGETS.get(index++), target.getId());
        }
        while (index < NEURAL_LINK_TARGET_LIMIT) {
            entityData.set(NEURAL_TARGETS.get(index++), 0);
        }

        if (!hasNeuralTargets()) {
            neuralLinkCooldown = 40;
            return;
        }

        entityData.set(NEURAL_LINK_ACTIVE, true);
        entityData.set(NEURAL_LINK_TICKS, NEURAL_LINK_DURATION_TICKS);
        neuralLinkCooldown = NEURAL_LINK_COOLDOWN_TICKS;
        applyNeuralLinkEffects();
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
            target.addEffect(new MobEffectInstance(negativeEffect, NEURAL_NEGATIVE_EFFECT_DURATION_TICKS,
                    1, false, true), this);
        }
        if (healed > 0.0F) {
            heal(healed);
        }
    }

    private void clearNeuralLink() {
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

    private void spawnNeuralLinkParticles() {
        if (!isNeuralLinkActive()) {
            return;
        }
        Vec3 source = getEyePosition();
        for (int entityId : getNeuralTargetIds()) {
            Entity target = level().getEntity(entityId);
            if (entityId == 0 || target == null) {
                continue;
            }
            Vec3 destination = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 particle = source.lerp(destination, random.nextDouble());
            level().addParticle(ParticleTypes.END_ROD, particle.x, particle.y, particle.z,
                    0.0D, 0.0D, 0.0D);
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
        tag.putInt("derived_neural_cooldown", neuralLinkCooldown);
        tag.putInt("derived_cosmic_orb_cooldown", cosmicOrbCooldown);
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
        neuralLinkCooldown = tag.contains("derived_neural_cooldown")
                ? tag.getInt("derived_neural_cooldown") : 80;
        cosmicOrbCooldown = tag.contains("derived_cosmic_orb_cooldown")
                ? tag.getInt("derived_cosmic_orb_cooldown") : COSMIC_ORB_COOLDOWN_TICKS;
        cosmicOrbBurstsRemaining = tag.getInt("derived_cosmic_orb_remaining");
        cosmicOrbInterval = tag.getInt("derived_cosmic_orb_interval");
        cloneParent = tag.hasUUID("derived_clone_parent") ? tag.getUUID("derived_clone_parent") : null;
        activeClone = tag.hasUUID("derived_active_clone") ? tag.getUUID("derived_active_clone") : null;
    }
}
