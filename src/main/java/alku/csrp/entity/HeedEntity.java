package alku.csrp.entity;

import net.minecraftforge.common.ForgeMod;
import net.minecraft.network.syncher.SynchedEntityData;
import alku.csrp.Config;
import alku.csrp.effect.EffectStacking;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.EnumSet;

/** Original crude Heed behavior, including its support skill, scent, and vulnerable head. */
public final class HeedEntity extends CrudeParasiteEntity {
    private static final EntityDataAccessor<Boolean> COMBAT_STATUS = SynchedEntityData.defineId(
            HeedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int SCENT_COOLDOWN_TICKS = 1_000;
    private static final int RAGE_SKILL_COOLDOWN_TICKS = 200;
    private static final int RAGE_DURATION_TICKS = 1_200;
    private static final int MELEE_ATTACK_INTERVAL_TICKS = 10;
    private static final int WATER_LEAP_CHARGE_TICKS = 20;
    private static final double MELEE_SPRINT_DISTANCE_SQR = 8.0D * 8.0D;
    private static final double RAGE_TARGET_RANGE_SQR = 20.0D * 20.0D;
    private static final double RAGE_EFFECT_RANGE = 1.5D;
    private static final double RECRUIT_RANGE = 16.0D;
    private static final RawAnimation AGE_IN_TICKS = RawAnimation.begin().thenLoop(
            "animation.heed.func_78087_a.age_in_ticks");
    private static final RawAnimation LIMB_SWING = RawAnimation.begin().thenLoop(
            "animation.heed.func_78087_a.limb_swing");
    private static final RawAnimation COMBAT_AGE = RawAnimation.begin().thenLoop(
            "animation.heed.func_78087_a.age_in_ticks.get_parasite_status_1");
    private static final RawAnimation COMBAT_LIMB = RawAnimation.begin().thenLoop(
            "animation.heed.func_78087_a.limb_swing.get_parasite_status_1");

    private final HeedHeadPart headPart;
    private final PartEntity<?>[] parts;
    private int scentCooldown = SCENT_COOLDOWN_TICKS;
    private int creationDevelopment = -1;
    private boolean waterLeaping;

    public HeedEntity(EntityType<? extends HeedEntity> type, Level level) {
        super(type, level);
        headPart = new HeedHeadPart(this);
        parts = new PartEntity<?>[]{headPart};
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.ARMOR, 9.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(ForgeMod.STEP_HEIGHT_ADDITION.get(), 1.0D);
    }

    @Override
    protected boolean usesDefaultFloatGoal() {
        return false;
    }

    @Override
    protected boolean usesDefaultTargetGoals() {
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new SwimmingDivingGoal());
        goalSelector.addGoal(2, new WaterLeapGoal());
        goalSelector.addGoal(2, new RageSkillGoal());
        goalSelector.addGoal(3, new HeedMeleeGoal());
        goalSelector.addGoal(6, new RecruitFollowersGoal());

        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 0,
                false, false, this::isValidParasiteTarget));
        if (Config.mobAttackingEnabled()) {
            targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 0,
                    !Config.collectiveConsciousnessEnabled(), false, this::isValidHeedMobTarget));
        }
    }

    private boolean isValidHeedMobTarget(LivingEntity target) {
        if (!isValidParasiteTarget(target) || target instanceof WaterAnimal
                || target instanceof Animal || target instanceof Villager) {
            return false;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace();
        boolean listed = Config.mobAttackingBlacklist().stream()
                .anyMatch(entry -> entry.indexOf(':') >= 0 ? entry.equals(id) : entry.equals(namespace));
        return Config.mobAttackingBlacklistInverted() ? listed : !listed;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(COMBAT_STATUS, false);
    }

    @Override
    public void tick() {
        super.tick();
        updateHeadPart();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity target = getTarget();
        boolean inCombat = target != null && target.isAlive();
        entityData.set(COMBAT_STATUS, inCombat);
        if (creationDevelopment < 0) {
            creationDevelopment = EvolutionSystem.ubiquitousDevelopment(serverLevel.getServer());
        }
        tickScent(serverLevel);
    }

    private void tickScent(ServerLevel level) {
        if (--scentCooldown >= 0 || tickCount % 21 != 10 || !Config.scentEnabled()
                || creationDevelopment < Config.scentDevelopmentLevel()
                || level.getEntities(ModEntities.SCENT.get(), scent -> true).size() > Config.scentCap()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        ParasiticScentEntity scent = ModEntities.SCENT.get().create(level);
        if (scent == null) {
            return;
        }
        scent.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        scent.setTargetToKill(target, false);
        scent.setDieAfterKilling(true);
        scent.setCanFollow(true);
        level.addFreshEntity(scent);
        scentCooldown = SCENT_COOLDOWN_TICKS;
    }

    private void updateHeadPart() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float forward = 2.1F * Mth.cos((float) Math.PI / 18.0F);
        headPart.setPos(getX() + Mth.sin(yaw) * forward, getY() + 0.3D,
                getZ() - Mth.cos(yaw) * forward);
        headPart.setYRot(getYRot());
    }

    private boolean hurtHead(DamageSource source, float amount) {
        if (!level().isClientSide && random.nextBoolean()) {
            EffectStacking.apply(this, ModMobEffects.BLEED.get(), 80, 0);
        }
        return hurt(source, amount * 3.0F);
    }

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose pose) {
        return 1.5F;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null) {
            for (int index = 0; index < parts.length; index++) {
                parts[index].setId(id + index + 1);
            }
        }
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    @Override
    public void remove(RemovalReason reason) {
        for (PartEntity<?> part : parts) {
            if (!part.isRemoved()) {
                part.remove(reason);
            }
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("heed_creation_development", creationDevelopment);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        creationDevelopment = tag.contains("heed_creation_development")
                ? tag.getInt("heed_creation_development") : -1;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> {
                    boolean moving = ParasiteAnimations.isMoving(this, state.isMoving());
                    return state.setAndContinue(entityData.get(COMBAT_STATUS)
                            ? moving ? COMBAT_LIMB : COMBAT_AGE
                            : moving ? LIMB_SWING : AGE_IN_TICKS);
                }));
    }

    private final class SwimmingDivingGoal extends Goal {
        private SwimmingDivingGoal() {
            setFlags(EnumSet.of(Flag.JUMP));
            if (getNavigation() instanceof GroundPathNavigation navigation) {
                navigation.setCanFloat(true);
            }
        }

        @Override
        public boolean canUse() {
            if (!isInWaterOrBubble() && !isInLava()) {
                return false;
            }
            LivingEntity target = getTarget();
            if (target != null && (target.isInWaterOrBubble() || target.isInLava())
                    && distanceToSqr(getX(), target.getY(), getZ()) < 25.0D
                    && target.getY() - getY() < -1.0D) {
                setDeltaMovement(getDeltaMovement().add(0.0D, -0.095D, 0.0D));
                return false;
            }
            return true;
        }

        @Override
        public void tick() {
            if (random.nextFloat() < 0.8F) {
                getJumpControl().jump();
            }
        }
    }

    private final class WaterLeapGoal extends Goal {
        private int attackTimer;
        private int attacking;
        private double targetX;
        private double targetY;
        private double targetZ;

        @Override
        public boolean canUse() {
            return isInWaterOrBubble() || isInLava() || attacking >= 1;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                attackTimer++;
                if (attackTimer >= WATER_LEAP_CHARGE_TICKS && attacking == 0) {
                    attacking = 1;
                    targetX = target.getX();
                    targetZ = target.getZ();
                    targetY = Math.max(0.0D, (target.getY() - getY()) * 0.07D);
                }
            } else if (attackTimer > 0) {
                attackTimer--;
            }

            if (attacking < 1) {
                return;
            }
            attacking++;
            if (attacking == 2 && onGround()) {
                waterLeaping = true;
                getNavigation().stop();
                double dx = targetX - getX();
                double dz = targetZ - getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                Vec3 movement = getDeltaMovement();
                if (distance > 0.0D) {
                    setDeltaMovement(movement.x + dx / distance * 1.5D * 0.9D + movement.x * 0.3D,
                            0.7D + targetY,
                            movement.z + dz / distance * 1.5D * 0.9D + movement.z * 0.3D);
                } else {
                    setDeltaMovement(movement.x, 0.7D + targetY, movement.z);
                }
            }
            if (attacking >= 3 && onGround()) {
                attacking = 0;
                attackTimer = 0;
                waterLeaping = false;
            }
        }
    }

    private final class HeedMeleeGoal extends Goal {
        private int attackTick;

        private HeedMeleeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (attackTick > 0) {
                attackTick--;
            }
            double distance = distanceToSqr(target);
            double speed = distance > MELEE_SPRINT_DISTANCE_SQR && level() instanceof ServerLevel serverLevel
                    && EvolutionSystem.generationProfile(serverLevel).sprinting() ? 1.3D : 1.0D;
            getNavigation().moveTo(target, speed);
            if (isWithinMeleeAttackRange(target) && attackTick <= 0 && getSensing().hasLineOfSight(target)) {
                attackTick = MELEE_ATTACK_INTERVAL_TICKS;
                doHurtTarget(target);
            }
        }
    }

    private final class RageSkillGoal extends Goal {
        private int attackTimer;

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && level() instanceof ServerLevel serverLevel
                    && !waterLeaping && EvolutionSystem.generationProfile(serverLevel).specialMoves();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            if (distanceToSqr(target) < RAGE_TARGET_RANGE_SQR) {
                attackTimer++;
            }
            if (hasEffect(ModMobEffects.RAGE.get())) {
                attackTimer++;
            }
            if (attackTimer < RAGE_SKILL_COOLDOWN_TICKS) {
                return;
            }
            if (Config.rageEnabled()) {
                for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox().inflate(RAGE_EFFECT_RANGE), candidate ->
                                candidate != HeedEntity.this && candidate instanceof Parasite
                                        && candidate.isAlive())) {
                    ally.addEffect(new MobEffectInstance(ModMobEffects.RAGE.get(),
                            RAGE_DURATION_TICKS, 0, false, false), HeedEntity.this);
                }
            }
            attackTimer = 0;
        }
    }

    private final class RecruitFollowersGoal extends Goal {
        @Override
        public boolean canUse() {
            return tickCount % 20 == 0 && getTarget() == null
                    && ParasiteFollowGoal.getLeader(HeedEntity.this) == null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            for (Mob follower : level().getEntitiesOfClass(Mob.class,
                    getBoundingBox().inflate(RECRUIT_RANGE, 2.0D, RECRUIT_RANGE), candidate ->
                            candidate != HeedEntity.this && candidate instanceof Parasite
                                    && candidate.isAlive() && ParasiteFollowGoal.commandRank(candidate) < 41)) {
                if (!hasLineOfSight(follower)) {
                    continue;
                }
                Mob leader = ParasiteFollowGoal.getLeader(follower);
                if (leader == null || ParasiteFollowGoal.commandRank(leader) <= 30) {
                    ParasiteFollowGoal.setLeader(follower, HeedEntity.this);
                    break;
                }
            }
        }
    }

    private static final class HeedHeadPart extends PartEntity<HeedEntity> {
        private HeedHeadPart(HeedEntity parent) {
            super(parent);
        }

        @Override
        protected void defineSynchedData() {
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        public boolean isPickable() {
            return getParent().isAlive();
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            return getParent().hurtHead(source, amount);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(1.8F, 1.8F);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }

        @Override
        public Component getName() {
            return Component.literal("heed_head");
        }
    }
}
