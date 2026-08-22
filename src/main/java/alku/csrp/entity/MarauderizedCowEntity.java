package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/** Legacy Marauderized cow: its ranged attack leaves a virulent, corrosive vomit cloud. */
public final class MarauderizedCowEntity extends MarauderizedParasiteEntity implements ManualVariantProvider {
    private static final EntityDataAccessor<Boolean> RAGE_VARIANT = SynchedEntityData.defineId(
            MarauderizedCowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final byte VOMIT_EVENT = 100;
    private static final int VOMIT_COOLDOWN_TICKS = 200;

    private int vomitTicks;
    private boolean appliedRageVariant;

    public MarauderizedCowEntity(EntityType<? extends MarauderizedCowEntity> type, Level level) {
        super(type, level, 12, AnimationProfile.COW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMarauderizedAttributes(38.0D, 8.0D, 15.0D, 0.8D, 0.20D, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData(builder);
        builder.define(RAGE_VARIANT, false);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        String biome = level.getBiome(blockPosition()).unwrapKey()
                .map(key -> key.location().getPath()).orElse("");
        boolean desert = biome.contains("desert") || biome.contains("badlands");
        setRageVariant(random.nextDouble() < (desert ? 0.8D : 0.01D));
        applyVariantAttributes();
        setHealth(getMaxHealth());
        return result;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new VomitGoal());
    }

    @Override
    protected double meleeSpeed() {
        return 1.5D;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            applyVariantAttributes();
            if (isRageVariant()) {
                var rage = getEffect(ModMobEffects.RAGE.get());
                if (rage == null || rage.getDuration() < 40) {
                    addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            ModMobEffects.RAGE.get(), 400, 0, false, false), this);
                }
            }
        }
        if (level().isClientSide && vomitTicks-- > 0) {
            spawnVomitParticles();
        }
    }

    public boolean isRageVariant() {
        return entityData.get(RAGE_VARIANT);
    }

    @Override
    public int getManualVariant() {
        return isRageVariant() ? 1 : 0;
    }

    @Override
    public void setManualVariant(int variant) {
        setRageVariant(variant == 1);
        applyVariantAttributes();
    }

    @Override
    public int getMaxManualVariants() {
        return 2;
    }

    private void setRageVariant(boolean rageVariant) {
        entityData.set(RAGE_VARIANT, rageVariant);
        appliedRageVariant = !rageVariant;
    }

    private void applyVariantAttributes() {
        boolean rageVariant = isRageVariant();
        if (appliedRageVariant == rageVariant) {
            return;
        }
        appliedRageVariant = rageVariant;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(rageVariant ? 28.5D : 38.0D);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(rageVariant ? 0.26D : 0.20D);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(rageVariant ? 18.75D : 15.0D);
        if (!rageVariant) {
            removeEffect(ModMobEffects.RAGE.get());
        }
        setHealth(Math.min(getHealth(), getMaxHealth()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("rage_variant", isRageVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRageVariant(tag.getBoolean("rage_variant"));
        applyVariantAttributes();
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            int count = 3 + random.nextInt(2);
            for (int index = 0; index < count; index++) {
                BuglinEntity buglin = ModEntities.BUGLIN.get().create(serverLevel);
                if (buglin == null) {
                    continue;
                }
                buglin.moveTo(getX() + (random.nextDouble() - 0.5D) * 1.5D,
                        getY() + getBbHeight() * 0.5D + 0.5D,
                        getZ() + (random.nextDouble() - 0.5D) * 1.5D, getYRot(), 0.0F);
                buglin.setTarget(getTarget());
                serverLevel.addFreshEntity(buglin);
            }
        }
        super.die(source);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == VOMIT_EVENT) {
            vomitTicks = 40;
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void spitVomit(LivingEntity target) {
        ParasiteCombatEffects.spawnVomitCloud(this, 4.5D, 3.0F, 100, 300, 20);
        startAttackAnimation();
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        level().broadcastEntityEvent(this, VOMIT_EVENT);
    }

    private void spawnVomitParticles() {
        Vec3 direction = getViewVector(1.0F);
        Vec3 start = getEyePosition().add(direction.scale(1.2D));
        for (int index = 0; index < 6; index++) {
            level().addParticle(ParticleTypes.SNEEZE, start.x, start.y - 0.2D, start.z,
                    direction.x * 0.2D + (random.nextDouble() - 0.5D) * 0.25D,
                    0.02D + random.nextDouble() * 0.1D,
                    direction.z * 0.2D + (random.nextDouble() - 0.5D) * 0.25D);
        }
    }

    private final class VomitGoal extends Goal {
        private int cooldown;
        private boolean fired;

        private VomitGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && hasLineOfSight(target)
                    && distanceToSqr(target) >= 9.0D && distanceToSqr(target) <= 49.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return !fired;
        }

        @Override
        public void start() {
            fired = false;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                spitVomit(target);
            }
            fired = true;
        }

        @Override
        public void stop() {
            cooldown = VOMIT_COOLDOWN_TICKS;
        }
    }
}
