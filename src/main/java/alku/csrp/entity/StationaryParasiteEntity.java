package alku.csrp.entity;

import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimatedEntity;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.gen.ParasiteFloorScan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Random;

/** Shared stationary, buried, target-restricted behavior for Nak and Unvo. */
public abstract class StationaryParasiteEntity extends PrimitiveParasiteEntity {
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            StationaryParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BURIED_DEPTH = SynchedEntityData.defineId(
            StationaryParasiteEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> RETREATING = SynchedEntityData.defineId(
            StationaryParasiteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PEEK_ONLY = SynchedEntityData.defineId(
            StationaryParasiteEntity.class, EntityDataSerializers.BOOLEAN);

    protected double buriedTarget;
    private double anchorX;
    private double anchorZ;
    private boolean anchorInitialized;
    private int peekingTicks;
    private int delayBuried;

    protected StationaryParasiteEntity(EntityType<? extends StationaryParasiteEntity> type, Level level) {
        super(type, level);
        xpReward = MobsConfig.turretXpValue();
    }

    public static AttributeSupplier.Builder createStationaryAttributes(double health, double armor,
                                                                        double damage, double followRange) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, followRange);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, 0);
        builder.define(BURIED_DEPTH, 0.0F);
        builder.define(RETREATING, false);
        builder.define(PEEK_ONLY, false);
    }

    @Override
    protected boolean usesDefaultMovementGoals() {
        return false;
    }

    @Override
    protected boolean usesDefaultFloatGoal() {
        return false;
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (getBuriedDepth() <= 0.0F && peekingTicks <= 50) {
            super.setTarget(target);
        }
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void push(Entity entity) {
        if (entity instanceof FishingHook) {
            entity.discard();
        }
    }

    @Override
    protected void doPush(Entity entity) {
        if (entity instanceof FishingHook) {
            entity.discard();
        }
    }

    @Override
    public void tick() {
        anchorStationaryPosition(true);
        tickBuriedState();
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (isPeekOnly() && tickCount > 100) {
            LivingEntity target = getTarget();
            if (target != null && !target.isAlive()) {
                setTarget(null);
            } else if (target != null) {
                peekingTicks = 0;
            } else if (++peekingTicks > 100) {
                beginRetreat();
            }
        }
        retreat(getTarget() == null);
        anchorStationaryPosition(false);
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth() && MobsConfig.turretRegeneration() > 0.0D) {
            heal((float) MobsConfig.turretRegeneration());
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            applyStationaryMinimumDamage(living, 1.0F);
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        amount = ParasiteCombatEffects.damageAfterKillingResistance(source, amount, ModMobEffects.NEXUS);
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return MobsConfig.turretPointReductionCap();
    }

    @Override
    protected float damageAdaptationPerHit() {
        return (float) MobsConfig.turretPointReductionValue();
    }

    @Override
    protected int maxLearnableDamageSources() {
        return MobsConfig.turretPointDamageCap();
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return (float) MobsConfig.turretChanceToLearn();
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return (float) MobsConfig.turretChanceToLearnFire();
    }

    @Override
    protected int incomingDamageCapDivisor() {
        return MobsConfig.turretDamageCap();
    }

    public boolean applyStationaryMinimumDamage(LivingEntity target, float multiplier) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !EvolutionSystem.generationProfile(serverLevel, this).minimumDamage()
                || target == this || target instanceof Parasite || !target.isAlive()
                || target instanceof Player player && player.getAbilities().instabuild) {
            return false;
        }
        float amount = (float) (MobsConfig.turretMinimumDamage() * Math.max(0.0F, multiplier));
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

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, status);
    }

    public float getBuriedDepth() {
        return entityData.get(BURIED_DEPTH);
    }

    public float getFloorTimer() {
        return getBuriedDepth();
    }

    public boolean isRetreating() {
        return entityData.get(RETREATING);
    }

    public boolean isPeekOnly() {
        return entityData.get(PEEK_ONLY);
    }

    public boolean isBuried() {
        return getBuriedDepth() > 0.0F;
    }

    public void setPeek(boolean peekOnly) {
        entityData.set(PEEK_ONLY, peekOnly);
    }

    public void setBuried() {
        setParasiteStatus(3);
        setBuriedDepth((float) buriedTarget);
    }

    public void setDelayBuried(int ticks) {
        delayBuried = Math.max(0, ticks);
    }

    public void beginRetreat() {
        entityData.set(RETREATING, true);
    }

    public void setAnchorPosition(double x, double z) {
        anchorX = x;
        anchorZ = z;
        anchorInitialized = true;
    }

    protected void setBuriedDepth(float depth) {
        entityData.set(BURIED_DEPTH, depth);
    }

    protected boolean buried() {
        if (!isBuried()) {
            if (getParasiteStatus() == 3) {
                setParasiteStatus(0);
            } else if (!isRetreating()) {
                setBuriedDepth(-0.1F);
            }
            return false;
        }
        if (delayBuried > 0) {
            delayBuried--;
            if (delayBuried > 0) {
                return true;
            }
        }
        if (!isRetreating()) {
            setBuriedDepth(Math.max(0.0F, getBuriedDepth() - getBuriedSpeed()));
        }
        return true;
    }

    protected double getBuriedSpeed() {
        return 0.08D;
    }

    protected void retreat(boolean discardWhenDead) {
        if (!isRetreating()) {
            return;
        }
        setBuriedDepth(getBuriedDepth() + (float) getBuriedSpeed());
        setParasiteStatus(3);
        if (getBuriedDepth() <= buriedTarget || level().isClientSide()) {
            return;
        }
        if (discardWhenDead) {
            discard();
        } else if (getTarget() != null && teleportDigging(getTarget().blockPosition())) {
            setPeek(true);
            entityData.set(RETREATING, false);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.broadcastEntityEvent(this, (byte) 52);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 50) {
            setBuried();
        } else if (id == 51) {
            beginRetreat();
        } else if (id == 52) {
            setPeek(true);
            entityData.set(RETREATING, false);
        } else {
            super.handleEntityEvent(id);
        }
    }

    private boolean tickBuriedState() {
        return buried();
    }

    private void anchorStationaryPosition(boolean applyFall) {
        if (!anchorInitialized) {
            setAnchorPosition(getX(), getZ());
        }
        Vec3 movement = getDeltaMovement();
        double vertical = movement.y - (applyFall && !onGround() ? 0.5D : 0.0D);
        setDeltaMovement(0.0D, vertical, 0.0D);
        setPos(anchorX, getY(), anchorZ);
    }

    private boolean teleportDigging(BlockPos targetPosition) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!hasEffect(MobEffects.MINING_FATIGUE)) {
            addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 4, false, false), this);
            return false;
        }

        Random relocationRandom = new Random();
        int offsetX = nextRelocationOffset(relocationRandom, 5, 2);
        int offsetZ = nextRelocationOffset(relocationRandom, 5, 2);
        for (int attempt = 0; attempt < 5; attempt++) {
            BlockPos floor = ParasiteFloorScan.getFloor(serverLevel,
                    BlockPos.containing(targetPosition.getX() + offsetX, targetPosition.getY(),
                            targetPosition.getZ() + offsetZ), 5);
            if (floor != null && serverLevel.getBlockState(floor.below())
                    .isFaceSturdy(serverLevel, floor.below(), Direction.UP)) {
                float hardness = 0.0F;
                for (int height = 1; height < 4; height++) {
                    BlockPos block = floor.above(height);
                    float blockHardness = serverLevel.getBlockState(block).getDestroySpeed(serverLevel, block);
                    if (blockHardness <= 0.0F) {
                        return false;
                    }
                    hardness += blockHardness;
                }
                if (hardness >= 10.0F) {
                    return false;
                }
                AABB search = new AABB(floor.getX(), floor.getY(), floor.getZ(),
                        floor.getX() + 1.0D, floor.getY() + 1.0D, floor.getZ() + 1.0D).inflate(1.0D);
                if (serverLevel.getEntitiesOfClass(NakEntity.class, search).isEmpty()) {
                    setPos(floor.getX() + 0.5D, floor.getY(), floor.getZ() + 0.5D);
                    setAnchorPosition(getX(), getZ());
                    return true;
                }
            }
            if (attempt < 4) {
                offsetX = nextRelocationOffset(relocationRandom, 5, 2);
                offsetZ = nextRelocationOffset(relocationRandom, 5, 2);
            }
        }
        return false;
    }

    private static int nextRelocationOffset(Random random, int range, int minimum) {
        int offset = random.nextInt(range) + minimum;
        return random.nextInt(2) == 0 ? -offset : offset;
    }

    private void setBuriedDepth(double depth) {
        setBuriedDepth((float) depth);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(PARASITE_STATUS, input.getIntOr("stationary_status", 0));
        entityData.set(BURIED_DEPTH, input.getFloatOr("stationary_buried", 0.0F));
        entityData.set(RETREATING, input.getBooleanOr("stationary_retreating", false));
        entityData.set(PEEK_ONLY, input.getBooleanOr("stationary_peek_only", false));
        peekingTicks = input.getIntOr("stationary_peeking_ticks", 0);
        delayBuried = input.getIntOr("stationary_buried_delay", 0);
        double savedAnchorX = input.getDoubleOr("stationary_anchor_x", Double.NaN);
        double savedAnchorZ = input.getDoubleOr("stationary_anchor_z", Double.NaN);
        if (Double.isFinite(savedAnchorX) && Double.isFinite(savedAnchorZ)) {
            setAnchorPosition(savedAnchorX, savedAnchorZ);
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("stationary_status", getParasiteStatus());
        output.putFloat("stationary_buried", getBuriedDepth());
        output.putBoolean("stationary_retreating", isRetreating());
        output.putBoolean("stationary_peek_only", isPeekOnly());
        output.putInt("stationary_peeking_ticks", peekingTicks);
        output.putInt("stationary_buried_delay", delayBuried);
        if (anchorInitialized) {
            output.putDouble("stationary_anchor_x", anchorX);
            output.putDouble("stationary_anchor_z", anchorZ);
        }
    }
}
