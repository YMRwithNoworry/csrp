package alku.csrp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Shared legacy Feral behaviour: fire weakness and kill-fuelled recovery. */
public class FeralParasiteEntity extends Monster implements GeoEntity, Parasite {
    private static final float REGEN_AMOUNT = 3.0F;
    private static final int REGEN_KILL_INTERVAL = 10;
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            FeralParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STILL_ANI = SynchedEntityData.defineId(
            FeralParasiteEntity.class, EntityDataSerializers.BOOLEAN);

    private final RawAnimation ageInTicksAnimation = ParasiteAnimations.loop(this, "func_78087_a.age_in_ticks");
    private final RawAnimation limbSwingAnimation = ParasiteAnimations.loop(this, "func_78087_a.limb_swing");
    private final RawAnimation ageStillAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_still_ani_1");
    private final RawAnimation ageStatus1Animation = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation limbStatus1Animation = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation limbStatus2Animation = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation limbStatus3Animation = ParasiteAnimations.loop(this,
            "func_78087_a.limb_swing.get_parasite_status_3");
    private final RawAnimation ageStatus3StillAnimation = ParasiteAnimations.loop(this,
            "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Kind kind;
    private int parasiteKills;
    private int regenUse = REGEN_KILL_INTERVAL;
    private int stillTicks;

    public FeralParasiteEntity(EntityType<? extends FeralParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.experience;
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind.knockbackResistance)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.FOLLOW_RANGE, kind.followRange);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.5D, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(6, new ParasiteFollowGoal(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(PARASITE_STATUS, 0);
        entityData.define(STILL_ANI, false);
    }

    @Override
    public void tick() {
        super.tick();
        updateAnimationState();
        if (level().isClientSide || tickCount % 10 != 0 || isOnFire() || parasiteKills <= 1
                || getHealth() >= getMaxHealth()) {
            return;
        }

        heal(REGEN_AMOUNT);
        if (--regenUse <= 0) {
            parasiteKills--;
            regenUse = REGEN_KILL_INTERVAL;
        }
    }

    private void updateAnimationState() {
        if (level().isClientSide) {
            return;
        }

        double dx = getX() - xo;
        double dz = getZ() - zo;
        if (dx * dx + dz * dz <= 1.0E-6D) {
            stillTicks++;
        } else {
            stillTicks = 0;
        }
        entityData.set(STILL_ANI, stillTicks > 25);

        LivingEntity target = getTarget();
        int status = 0;
        if (target != null && target.isAlive()) {
            double attackReach = getBbWidth() * 2.0D;
            double attackReachSqr = attackReach * attackReach + target.getBbWidth();
            status = distanceToSqr(target) > attackReachSqr ? 2 : 1;
        }
        entityData.set(PARASITE_STATUS, status);
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, Math.max(0, Math.min(3, status)));
    }

    public boolean getStillAni() {
        return entityData.get(STILL_ANI);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        amount = ParasiteCombatEffects.damageAfterKillingResistance(source, amount, ModMobEffects.FERAL.get());
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
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
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return super.doHurtTarget(target);
        }
        float healthBefore = ParasiteCombatEffects.healthWithAbsorption(livingTarget);
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            ParasiteCombatEffects.applyFearFromDamage(livingTarget, healthBefore, this);
        }
        return hit;
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        parasiteKills++;
        return super.killedEntity(level, victim);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("parasite_kills", parasiteKills);
        tag.putInt("regen_use", regenUse);
        tag.putInt("still_ticks", stillTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        parasiteKills = tag.getInt("parasite_kills");
        regenUse = tag.contains("regen_use") ? tag.getInt("regen_use") : REGEN_KILL_INTERVAL;
        stillTicks = tag.getInt("still_ticks");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 0,
                state -> state.setAndContinue(ageAnimation())));
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                return PlayState.STOP;
            }
            return state.setAndContinue(limbAnimation());
        }));
    }

    private RawAnimation ageAnimation() {
        int status = getParasiteStatus();
        boolean still = getStillAni();
        if (status == 3 && kind == Kind.COW && still) {
            return ageStatus3StillAnimation;
        }
        if (status == 1 && (kind == Kind.BEAR || kind == Kind.HUMAN || kind == Kind.VILLAGER)) {
            return ageStatus1Animation;
        }
        if (still && (kind == Kind.HUMAN || kind == Kind.VILLAGER)) {
            return ageStillAnimation;
        }
        return ageInTicksAnimation;
    }

    private RawAnimation limbAnimation() {
        int status = getParasiteStatus();
        if (status == 3 && (kind == Kind.COW || kind == Kind.HORSE)) {
            return limbStatus3Animation;
        }
        if (status == 2) {
            return limbStatus2Animation;
        }
        if (status == 1 && (kind == Kind.BEAR || kind == Kind.HUMAN || kind == Kind.VILLAGER)) {
            return limbStatus1Animation;
        }
        return limbSwingAnimation;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isValidParasiteTarget(LivingEntity target) {
        return target != this && target.isAlive() && !(target instanceof Parasite);
    }

    public enum Kind {
        BEAR(38.0D, 15.0D, 8.0D, 0.8D, 0.28D, 32.0D, 12),
        COW(38.0D, 15.0D, 8.0D, 0.8D, 0.28D, 32.0D, 12),
        ENDERMAN(80.0D, 21.0D, 6.0D, 0.5D, 0.33D, 64.0D, 24),
        HORSE(37.0D, 16.0D, 3.0D, 0.6D, 0.2775D, 32.0D, 12),
        HUMAN(24.0D, 15.0D, 7.0D, 0.3D, 0.26D, 32.0D, 10),
        PIG(16.0D, 13.0D, 8.0D, 0.7D, 0.32D, 24.0D, 8),
        SHEEP(21.0D, 12.0D, 5.0D, 0.7D, 0.30D, 24.0D, 9),
        VILLAGER(27.0D, 17.0D, 8.0D, 0.9D, 0.26D, 32.0D, 10),
        WOLF(16.0D, 15.0D, 4.0D, 0.4D, 0.36D, 32.0D, 10);

        private final double maxHealth;
        private final double attackDamage;
        private final double armor;
        private final double knockbackResistance;
        private final double movementSpeed;
        private final double followRange;
        private final int experience;

        Kind(double maxHealth, double attackDamage, double armor, double knockbackResistance,
             double movementSpeed, double followRange, int experience) {
            this.maxHealth = maxHealth;
            this.attackDamage = attackDamage;
            this.armor = armor;
            this.knockbackResistance = knockbackResistance;
            this.movementSpeed = movementSpeed;
            this.followRange = followRange;
            this.experience = experience;
        }
    }
}
