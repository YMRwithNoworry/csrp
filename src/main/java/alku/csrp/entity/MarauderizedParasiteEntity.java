package alku.csrp.entity;

import alku.csrp.registry.ModMobEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/** Common state and original model-function animation routing for Marauderized forms. */
public abstract class MarauderizedParasiteEntity extends HijackedParasiteEntity {
    private static final float BLEED_CHANCE = 0.15F;
    private static final int ATTACK_ANIMATION_TICKS = 12;
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            MarauderizedParasiteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STILL_ANI = SynchedEntityData.defineId(
            MarauderizedParasiteEntity.class, EntityDataSerializers.BOOLEAN);

    private final RawAnimation ageInTicksAnimation = animation("func_78087_a.age_in_ticks");
    private final RawAnimation limbSwingAnimation = animation("func_78087_a.limb_swing");
    private final RawAnimation ageStillAnimation = animation("func_78087_a.age_in_ticks.get_still_ani_1");
    private final RawAnimation ageStatus1Animation = animation("func_78087_a.age_in_ticks.get_parasite_status_1");
    private final RawAnimation limbStatus1Animation = animation("func_78087_a.limb_swing.get_parasite_status_1");
    private final RawAnimation ageStatus1StillAnimation = animation(
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1");
    private final RawAnimation ageStatus2Animation = animation("func_78087_a.age_in_ticks.get_parasite_status_2");
    private final RawAnimation limbStatus2Animation = animation("func_78087_a.limb_swing.get_parasite_status_2");
    private final RawAnimation ageStatus2StillAnimation = animation(
            "func_78087_a.age_in_ticks.get_parasite_status_2.get_still_ani_1");
    private final RawAnimation ageStatus3Animation = animation("func_78087_a.age_in_ticks.get_parasite_status_3");
    private final RawAnimation limbStatus3Animation = animation("func_78087_a.limb_swing.get_parasite_status_3");
    private final RawAnimation ageStatus3StillAnimation = animation(
            "func_78087_a.age_in_ticks.get_parasite_status_3.get_still_ani_1");
    private final RawAnimation ageScreamingAnimation = animation("func_78087_a.age_in_ticks.is_screaming_1");
    private final RawAnimation limbScreamingAnimation = animation("func_78087_a.limb_swing.is_screaming_1");
    private final RawAnimation ageStillScreamingAnimation = animation(
            "func_78087_a.age_in_ticks.get_still_ani_1.is_screaming_1");
    private final RawAnimation ageStatus1ScreamingAnimation = animation(
            "func_78087_a.age_in_ticks.get_parasite_status_1.is_screaming_1");
    private final RawAnimation limbStatus1ScreamingAnimation = animation(
            "func_78087_a.limb_swing.get_parasite_status_1.is_screaming_1");
    private final RawAnimation ageStatus1StillScreamingAnimation = animation(
            "func_78087_a.age_in_ticks.get_parasite_status_1.get_still_ani_1.is_screaming_1");

    private final AnimationProfile animationProfile;
    private int forcedStatus;
    private int forcedStatusTicks;
    private int stillTicks;

    protected MarauderizedParasiteEntity(EntityType<? extends MarauderizedParasiteEntity> type, Level level,
                                         int experience, AnimationProfile animationProfile) {
        super(type, level, experience);
        this.animationProfile = animationProfile;
    }

    protected static AttributeSupplier.Builder createMarauderizedAttributes(double health, double armor,
                                                                              double damage,
                                                                              double knockbackResistance,
                                                                              double movementSpeed,
                                                                              double followRange) {
        return createAttributes(health, armor, damage, knockbackResistance, movementSpeed, followRange);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new MeleeAttackGoal(this, meleeSpeed(), false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PARASITE_STATUS, 0);
        builder.define(STILL_ANI, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            updateAnimationState();
        }
    }

    protected double meleeSpeed() {
        return 1.2D;
    }

    protected int specialAnimationStatus() {
        return -1;
    }

    protected final void startAttackAnimation() {
        if (!level().isClientSide) {
            forcedStatus = 1;
            forcedStatusTicks = ATTACK_ANIMATION_TICKS;
            entityData.set(PARASITE_STATUS, forcedStatus);
        }
    }

    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public boolean getStillAni() {
        return entityData.get(STILL_ANI);
    }

    private void updateAnimationState() {
        double dx = getX() - xo;
        double dz = getZ() - zo;
        if (dx * dx + dz * dz <= 1.0E-6D) {
            stillTicks++;
        } else {
            stillTicks = 0;
        }
        entityData.set(STILL_ANI, stillTicks > 25);

        int specialStatus = specialAnimationStatus();
        if (specialStatus >= 0) {
            entityData.set(PARASITE_STATUS, specialStatus);
            return;
        }
        if (forcedStatusTicks > 0) {
            forcedStatusTicks--;
            entityData.set(PARASITE_STATUS, forcedStatus);
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            entityData.set(PARASITE_STATUS, 0);
            return;
        }
        double attackReach = getBbWidth() * 2.0D;
        double attackReachSqr = attackReach * attackReach + target.getBbWidth();
        int status = distanceToSqr(target) > attackReachSqr ? 2 : 1;
        entityData.set(PARASITE_STATUS, animationProfile == AnimationProfile.ENDERMAN ? 1 : status);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "age_controller", 0, state -> {
            RawAnimation animation = ageAnimation();
            return animation == null ? PlayState.STOP : state.setAndContinue(animation);
        }));
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (!ParasiteAnimations.isMoving(this, state.isMoving())) {
                return PlayState.STOP;
            }
            RawAnimation animation = limbAnimation();
            return animation == null ? PlayState.STOP : state.setAndContinue(animation);
        }));
    }

    private RawAnimation ageAnimation() {
        int status = getParasiteStatus();
        boolean still = getStillAni();
        return switch (animationProfile) {
            case BEAR -> switch (status) {
                case 1 -> ageStatus1Animation;
                case 2 -> ageStatus2Animation;
                case 3 -> ageStatus3Animation;
                default -> ageInTicksAnimation;
            };
            case COW -> switch (status) {
                case 1 -> ageStatus1Animation;
                case 3 -> still ? ageStatus3StillAnimation : null;
                case 2 -> null;
                default -> ageInTicksAnimation;
            };
            case ENDERMAN -> endermanAgeAnimation(status, still);
            case HUMAN -> switch (status) {
                case 1 -> still ? ageStatus1StillAnimation : ageStatus1Animation;
                case 2 -> ageStatus2Animation;
                case 3 -> ageStatus3Animation;
                default -> still ? ageStillAnimation : ageInTicksAnimation;
            };
            case SHEEP -> switch (status) {
                case 1 -> ageStatus1Animation;
                case 2 -> ageStatus2Animation;
                default -> ageInTicksAnimation;
            };
            case VILLAGER -> switch (status) {
                case 1 -> still ? ageStatus1StillAnimation : ageStatus1Animation;
                case 2 -> still ? ageStatus2StillAnimation : ageStatus2Animation;
                default -> still ? ageStillAnimation : ageInTicksAnimation;
            };
        };
    }

    private RawAnimation endermanAgeAnimation(int status, boolean still) {
        boolean screaming = isAggressive();
        if (status == 1) {
            if (still) {
                return screaming ? ageStatus1StillScreamingAnimation : ageStatus1StillAnimation;
            }
            return screaming ? ageStatus1ScreamingAnimation : ageStatus1Animation;
        }
        if (still) {
            return screaming ? ageStillScreamingAnimation : ageStillAnimation;
        }
        return screaming ? ageScreamingAnimation : ageInTicksAnimation;
    }

    private RawAnimation limbAnimation() {
        int status = getParasiteStatus();
        if (animationProfile == AnimationProfile.ENDERMAN) {
            if (status == 1) {
                return isAggressive() ? limbStatus1ScreamingAnimation : limbStatus1Animation;
            }
            return isAggressive() ? limbScreamingAnimation : limbSwingAnimation;
        }
        return switch (status) {
            case 1 -> limbStatus1Animation;
            case 2 -> limbStatus2Animation;
            case 3 -> animationProfile == AnimationProfile.BEAR || animationProfile == AnimationProfile.COW
                    ? limbStatus3Animation : null;
            default -> limbSwingAnimation;
        };
    }

    private RawAnimation animation(String action) {
        return ParasiteAnimations.loop(this, action);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hit = super.doHurtTarget(entity);
        if (hit) {
            startAttackAnimation();
        }
        if (hit && entity instanceof LivingEntity target && random.nextFloat() < BLEED_CHANCE) {
            target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 100, 0), this);
        }
        return hit;
    }

    protected enum AnimationProfile {
        BEAR,
        COW,
        ENDERMAN,
        HUMAN,
        SHEEP,
        VILLAGER
    }
}
