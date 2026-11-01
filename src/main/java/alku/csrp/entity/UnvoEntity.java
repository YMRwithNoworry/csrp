package alku.csrp.entity;

import alku.csrp.animation.CitadelAnimationController;
import alku.csrp.animation.CitadelAnimationManager;
import alku.csrp.animation.CitadelAnimationState;
import alku.csrp.animation.CitadelPlayState;
import alku.csrp.animation.CitadelRawAnimation;
import alku.csrp.config.GeneralConfig;
import alku.csrp.config.MobsConfig;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Unvo (legacy Sentry), a stationary parasite with a three-shot spineball volley. */
public final class UnvoEntity extends StationaryParasiteEntity {
    private static final int SHOT_PREHEAT_TICKS = 20;
    private static final int SHOTS_PER_VOLLEY = 3;
    private static final EntityDataAccessor<Integer> SHOTS_FIRED = SynchedEntityData.defineId(
            UnvoEntity.class, EntityDataSerializers.INT);
    private static final CitadelRawAnimation IDLE = CitadelRawAnimation.begin()
            .thenLoop("animation.sentry.func_78087_a.age_in_ticks");
    private static final CitadelRawAnimation BURIED = CitadelRawAnimation.begin()
            .thenLoop("animation.sentry.get_floor_timer");
    private static final CitadelRawAnimation ATTACK = CitadelRawAnimation.begin()
            .thenLoop("animation.sentry.func_78087_a.age_in_ticks.get_parasite_status_1");
    private static final CitadelRawAnimation BURIED_ATTACK = CitadelRawAnimation.begin()
            .thenLoop("animation.sentry.get_floor_timer.get_parasite_status_1");
    private static final CitadelRawAnimation BURIED_STATUS = CitadelRawAnimation.begin()
            .thenLoop("animation.sentry.get_floor_timer.get_parasite_status_3");

    private int attackTimer;

    public UnvoEntity(EntityType<? extends UnvoEntity> type, Level level) {
        super(type, level);
        xpReward = 2 * GeneralConfig.adaptedExperienceValue();
        buriedTarget = 5.1D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createStationaryAttributes(30.0D, 10.0D, 5.0D, 32.0D);
    }

    public void applyConfiguredAttributes() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0D * MobsConfig.unvoHealthMultiplier());
        getAttribute(Attributes.ARMOR).setBaseValue(10.0D * MobsConfig.unvoArmorMultiplier());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0D * MobsConfig.unvoDamageMultiplier());
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(MobsConfig.turretFollowRange());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHOTS_FIRED, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || getParasiteStatus() >= 3 || isRetreating() || isBuried()) {
            attackTimer = 0;
            entityData.set(SHOTS_FIRED, 0);
            return;
        }
        if (getParasiteStatus() == 0) {
            setParasiteStatus(1);
        }
        if (distanceToSqr(target) < 4225.0D && hasLineOfSight(target)) {
            attackTimer++;
            if (hasEffect(alku.csrp.registry.ModMobEffects.RAGE)) {
                attackTimer++;
            }
            if (attackTimer == SHOT_PREHEAT_TICKS - 10) {
                playShotSound("unvo.shooting", 2.0F, 1.0F);
            }
            if (attackTimer > SHOT_PREHEAT_TICKS) {
                int shotsFired = entityData.get(SHOTS_FIRED);
                if (shotsFired < SHOTS_PER_VOLLEY) {
                    fireSpineball(target);
                    entityData.set(SHOTS_FIRED, shotsFired + 1);
                } else {
                    attackTimer = 0;
                    entityData.set(SHOTS_FIRED, 0);
                }
            }
        } else if (attackTimer > 0) {
            attackTimer--;
        }
    }

    public int getParasiteIDRegister() {
        return 30;
    }

    public int getShotsFired() {
        return entityData.get(SHOTS_FIRED);
    }

    @Override
    public void registerControllers(CitadelAnimationManager.ControllerRegistrar controllers) {
        controllers.add(new CitadelAnimationController<>(this, "movement_controller", 4,
                this::movementAnimation));
    }

    private CitadelPlayState movementAnimation(CitadelAnimationState<UnvoEntity> state) {
        if (getParasiteStatus() == 3) {
            return state.setAndContinue(BURIED_STATUS);
        }
        if (getBuriedDepth() > 0.0F) {
            return state.setAndContinue(BURIED);
        }
        if (getParasiteStatus() == 1) {
            return state.setAndContinue(getBuriedDepth() < 0.0F ? ATTACK : BURIED_ATTACK);
        }
        return state.setAndContinue(IDLE);
    }

    private void fireSpineball(LivingEntity target) {
        Vec3 look = getViewVector(1.0F);
        Vec3 start = new Vec3(getX() + look.x, getEyeY() - 0.2D, getZ() + look.z);
        double accelerationX = target.getX() - (getX() + look.x);
        double accelerationY = target.getBoundingBox().minY + target.getBbHeight() / 4.0D
                - (1.0D + getY() + getBbHeight() / 2.0D);
        double accelerationZ = target.getZ() - (getZ() + look.z);
        ParasiteProjectileEntity projectile = ModEntities.createProjectile(level(),
                ParasiteProjectileEntity.Mode.UNVO_SPINE);
        if (projectile == null) {
            return;
        }
        playShotSound("emana.shooting", 2.0F, 1.0F);
        projectile.configureAccelerating(this, ParasiteProjectileEntity.Mode.UNVO_SPINE,
                start, new Vec3(accelerationX, accelerationY, accelerationZ),
                (float) (25.0D * GeneralConfig.globalDamageMultiplier()
                        * MobsConfig.unvoRangeDamageMultiplier()), 0.0D);
        projectile.setUnvoSpineEffects(MobsConfig.unvoPoisonDurationTicks(),
                MobsConfig.unvoPoisonAmplifier(), MobsConfig.unvoGearDamage());
        if (!level().addFreshEntity(projectile)) {
            projectile.discard();
        }
    }

    private void playShotSound(String id, float volume, float pitch) {
        SoundEvent sound = ModSounds.get(id);
        if (sound != null) {
            playSound(sound, volume, pitch);
        }
    }
}
