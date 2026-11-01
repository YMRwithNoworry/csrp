package alku.csrp.entity;

import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimationState;
import alku.csrp.animation.CitadelPlayState;
import alku.csrp.animation.CitadelRawAnimation;
import alku.csrp.config.MobsConfig;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Nak (legacy Seizer), a stationary parasite that holds and pulls its current target. */
public final class NakEntity extends StationaryParasiteEntity {
    private static final EntityDataAccessor<Integer> TARGET_ENTITY_ID = SynchedEntityData.defineId(
            NakEntity.class, EntityDataSerializers.INT);
    private static final CitadelRawAnimation IDLE = CitadelRawAnimation.begin()
            .thenLoop("animation.seizer.func_78087_a.age_in_ticks");
    private static final CitadelRawAnimation BURIED = CitadelRawAnimation.begin()
            .thenLoop("animation.seizer.get_floor_timer");
    private static final CitadelRawAnimation HOLD = CitadelRawAnimation.begin()
            .thenLoop("animation.seizer.get_floor_timer.get_targeted_entity_1");

    private int noTargetTicks;

    public NakEntity(EntityType<? extends NakEntity> type, Level level) {
        super(type, level);
        buriedTarget = 4.0D;
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createStationaryAttributes(15.0D, 10.0D, 6.0D, 6.0D);
    }

    public void applyConfiguredAttributes() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(15.0D * MobsConfig.nakHealthMultiplier());
        getAttribute(Attributes.ARMOR).setBaseValue(10.0D * MobsConfig.nakArmorMultiplier());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6.0D * MobsConfig.nakDamageMultiplier());
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(6.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_ENTITY_ID, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isRetreating() || isBuried()) {
            return;
        }

        LivingEntity target = getTarget();
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            setTarget(null);
            setTargetedEntity(0);
            setParasiteStatus(0);
            return;
        }
        if (target != null && !target.isAlive()) {
            setTarget(null);
            target = null;
        }
        if (target == null) {
            noTargetTicks++;
            if (noTargetTicks >= 60) {
                beginRetreat();
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.broadcastEntityEvent(this, (byte) 51);
                }
            }
            setParasiteStatus(0);
            setTargetedEntity(0);
            return;
        }

        noTargetTicks = 0;
        if (hasLineOfSight(target) && distanceToSqr(target) < 25.0D) {
            setParasiteStatus(3);
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 2, false, false), this);
            setTargetedEntity(target.getId());
            getNavigation().moveTo(target, 0.0D);
            lookAt(target, 30.0F, 30.0F);
        } else {
            setParasiteStatus(2);
            setTargetedEntity(0);
        }

        LivingEntity heldTarget = getTargetedEntity();
        if (heldTarget != null) {
            double distance = distanceToSqr(heldTarget);
            if (distance < 25.0D && distance > 1.0D) {
                heldTarget.stopRiding();
                var movement = heldTarget.getDeltaMovement();
                double desiredX = Math.signum(getX() - heldTarget.getX()) * 0.5D;
                double desiredZ = Math.signum(getZ() - heldTarget.getZ()) * 0.5D;
                heldTarget.setDeltaMovement(
                        movement.x + (desiredX - movement.x) * 0.5D,
                        movement.y,
                        movement.z + (desiredZ - movement.z) * 0.5D);
                heldTarget.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 20, 2, false, false), this);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof ParasiteProjectileEntity) {
            LivingEntity heldTarget = getTargetedEntity();
            if (heldTarget != null) {
                heldTarget.hurt(source, amount * 2.0F);
            }
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    public int getParasiteIDRegister() {
        return 72;
    }

    public boolean hasTargetedEntity() {
        return entityData.get(TARGET_ENTITY_ID) != 0;
    }

    public LivingEntity getTargetedEntity() {
        if (level().isClientSide()) {
            Entity target = level().getEntity(entityData.get(TARGET_ENTITY_ID));
            return target instanceof LivingEntity living && living.isAlive() ? living : null;
        }
        LivingEntity target = getTarget();
        return target != null && target.isAlive() ? target : null;
    }

    private void setTargetedEntity(int entityId) {
        entityData.set(TARGET_ENTITY_ID, entityId);
    }

    @Override
    public void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers) {
        controllers.add(new CitadelAnimationController<>(this, "movement_controller", 4,
                this::movementAnimation));
    }

    private CitadelPlayState movementAnimation(CitadelAnimationState<NakEntity> state) {
        if (getTargetedEntity() != null) {
            return state.setAndContinue(HOLD);
        }
        if (getBuriedDepth() > 0.0F) {
            return state.setAndContinue(BURIED);
        }
        return state.setAndContinue(IDLE);
    }
}
