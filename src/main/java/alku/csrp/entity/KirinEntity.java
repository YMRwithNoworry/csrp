package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModParticles;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * Direct port of SRParasites 1.10.9's {@code EntityKirin} together with {@code EntityAIKirinBlink}.
 *
 * <p>All kirin-specific behaviour is held at the original's values: the 80-tick skill wind-up while
 * the target stays visible inside 35 blocks, the summon (black hole orb and a judgement cut volley
 * on the 3rd 20-tick stage, skill ends past the 12th), the 42-slash judgement cut with its charge
 * aura, the 60/200 tick blink with its indoor and 16-block gates plus the 50% life steal, and the
 * hovering movement with its 40-tick recovery blink.</p>
 *
 * <p>Note that the original runs the summon and the judgement cut as two concurrent skill goals, so
 * a full skill cycle fires two slash volleys: one when the judgement goal starts and one when the
 * summon reaches its 3rd stage. That duplication is reproduced here on purpose.</p>
 */
public final class KirinEntity extends DerivedParasiteEntity {
    // ---- EntityAIKirinBlink ----
    public static final int BLINK_CHARGE_TICKS = 60;
    public static final int BLINK_COOLDOWN_TICKS = 200;
    public static final double BLINK_LIFE_STEAL_RADIUS = 5.0D;
    public static final double BLINK_HEALTH_DRAIN_FRACTION = 0.5D;
    private static final double BLINK_MIN_FAR_DIST_SQ = 256.0D;
    private static final int BLINK_MAX_TRIES = 64;
    private static final double BLINK_RADIUS_MIN = 1.5D;
    private static final double BLINK_RADIUS_MAX = 22.5D;
    private static final int BLINK_SKY_CHECKS = 3;
    private static final int FORCED_HURT_ANIM_TICKS = 10;
    private static final int[] BLINK_VERTICAL_OFFSETS = {0, 1, -1, 2, -2, 3, -3, 4, -4, 6, -6, 8, -8};

    // ---- EntityKirin#updateFloating ----
    private static final int FLOAT_GROUND_SCAN = 24;
    private static final double FLOAT_HOVER_HEIGHT = 0.35D;
    private static final double FLOAT_BOB_AMPLITUDE = 0.06D;
    private static final double FLOAT_BOB_SPEED = 0.12D;
    private static final double FLOAT_UP_ACCELERATION = 0.12D;
    private static final double FLOAT_DOWN_ACCELERATION = 0.06D;
    private static final double FLOAT_UP_MAX = 0.16D;
    private static final double FLOAT_DOWN_MAX = -0.16D;
    private static final int FLOAT_RECOVERY_DELAY_TICKS = 40;
    private static final int FLOAT_RECOVERY_HORIZONTAL_RANGE = 48;
    private static final int FLOAT_RECOVERY_VERTICAL_RANGE = 20;

    // ---- EntityAISkill(this, 80, 35, true, id) ----
    private static final int SKILL_TRACK_TICKS = 80;
    private static final double SKILL_TRACK_RANGE = 35.0D;
    /** EntityPCosmical freezes the shadow/clone machine while {@code parasiteStatus >= 3}. */
    private static final int SKILL_PARASITE_STATUS = 30;

    // ---- EntityKirin#summon ----
    private static final int SUMMON_STAGE_TICKS = 20;
    private static final int SUMMON_ORB_STAGE = 3;
    private static final int SUMMON_END_STAGE = 12;
    private static final int VOID_ORB_FUSE_TICKS = 8;
    private static final int VOID_ORB_START_TICKS = 80;
    private static final double VOID_ORB_OFFSET = 10.0D;

    // ---- EntityKirin#spawnJudgementCuts / #summonJudgementCutsOnly ----
    private static final int JUDGEMENT_SLASH_COUNT = 42;
    private static final float JUDGEMENT_PLAYER_DAMAGE = 8.0F;
    private static final float JUDGEMENT_MOB_DAMAGE = 10.0F;
    private static final int JUDGEMENT_CHARGE_DELAY_TICKS = 60;
    private static final int JUDGEMENT_AURA_END_TICKS = 24;
    private static final int JUDGEMENT_SKILL_TICKS = 80;

    // ---- EntityPCosmical show/shake state (read by ModelTabula_kirin) ----
    private static final byte SHADOW_HIT_EVENT = 41;
    private static final int SHOW_C_EVENT_TICKS = 15;
    private static final int SHOW_C_FLOOR = -10;
    private static final int SHAKE_EVENT_TICKS = 15;

    public static final float KIRIN_EYE_HEIGHT = 5.7F;
    public static final float KIRIN_SOUND_VOLUME = 5.0F;

    private static final EntityDataAccessor<BlockPos> BLINK_POS = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> BLINK_TICKS = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PARASITE_STATUS = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> JUDGEMENT_CUT_CHARGE_TICKS = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> JUDGEMENT_CUT_AURA_END_TICKS = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.INT);

    private final List<PendingJudgementSlash> pendingJudgementCuts = new ArrayList<>();

    private int blinkCooldown;
    private int blinkCharge;
    private BlockPos blinkDestination = BlockPos.ZERO;
    private int skillTrackTicks;
    private int summonStage;
    private int judgementCutSkillTicks;
    private boolean summoning;
    private boolean judgementCutQueued;
    private int floatBob;
    private int noGroundTicks;
    private double lockedY;
    private int shakeee;
    private int showC;

    public KirinEntity(EntityType<? extends KirinEntity> type, Level level) {
        super(type, level);
        xpReward = 350;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 410.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 155.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(ForgeMod.STEP_HEIGHT_ADDITION.get(), 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 80.0D);
    }

    public static boolean checkKirinSpawnRules(EntityType<? extends Monster> type,
            ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        ServerLevel currentLevel = level.getLevel();
        ServerLevel endLevel = currentLevel.getServer().getLevel(Level.END);
        return endLevel != null
                && SrpWorldData.get(endLevel).evolutionPhase() >= 7
                && SrpWorldData.get(currentLevel).evolutionPhase() >= 1
                && Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new KirinGroundNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Original priorities: blink 2, melee 3; both skill goals share priority 2.
        goalSelector.addGoal(2, new KirinBlinkGoal());
        goalSelector.addGoal(3, new KirinMeleeGoal());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(BLINK_POS, BlockPos.ZERO);
        entityData.define(BLINK_TICKS, 0);
        entityData.define(PARASITE_STATUS, 0);
        entityData.define(JUDGEMENT_CUT_CHARGE_TICKS, 0);
        entityData.define(JUDGEMENT_CUT_AURA_END_TICKS, 0);
    }

    @Override
    public int getParasiteStatus() {
        return entityData.get(PARASITE_STATUS);
    }

    public void setParasiteStatus(int status) {
        entityData.set(PARASITE_STATUS, Math.max(0, status));
    }

    public int getJudgementCutChargeTicks() {
        return entityData.get(JUDGEMENT_CUT_CHARGE_TICKS);
    }

    public void setJudgementCutChargeTicks(int ticks) {
        entityData.set(JUDGEMENT_CUT_CHARGE_TICKS, Math.max(0, ticks));
    }

    public int getJudgementCutAuraEndTicks() {
        return entityData.get(JUDGEMENT_CUT_AURA_END_TICKS);
    }

    public void setJudgementCutAuraEndTicks(int ticks) {
        entityData.set(JUDGEMENT_CUT_AURA_END_TICKS, Math.max(0, ticks));
    }

    public boolean isChargingJudgementCut() {
        return getJudgementCutChargeTicks() > 0 || getJudgementCutAuraEndTicks() > 0;
    }

    public boolean isSummoning() {
        return summoning;
    }

    @Override
    public float getEyeHeight(Pose pose) {
        return KIRIN_EYE_HEIGHT;
    }

    /** Model bridge: {@code ModelTabula_kirin} reads these through {@code TabulaAnimationAccess}. */
    @Override
    public int shakingC() {
        return shakeee;
    }

    @Override
    public float showC() {
        return showC;
    }

    /**
     * GeckoLib never drives this entity: the kirin renders through {@code ModelTabula_kirin}, whose
     * clone/shake state comes from {@link #getCloneC()}, {@link #shakingC()} and {@link #showC()}.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public boolean getCloneC() {
        return isShadowClone();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == SHADOW_HIT_EVENT) {
            // EntityPCosmical: shakeee = 15, showC = 15, plus the translucent shadow flash.
            shakeee = SHAKE_EVENT_TICKS;
            showC = SHOW_C_EVENT_TICKS;
        }
        super.handleEntityEvent(id);
    }

    /** Mirrors the original's per-tick {@code motionY = 0; posY = prevPosY} while a skill runs. */
    private void holdPosition(boolean lockVertical) {
        getNavigation().stop();
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x, 0.0D, motion.z);
        if (lockVertical) {
            setPos(getX(), lockedY, getZ());
        }
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(false);

        if (level().isClientSide) {
            tickShadowModelState();
            spawnAmbientPortalParticles();
            spawnBlinkWarningParticles();
            spawnJudgementCutChargeParticles();
            return;
        }

        if (blinkCooldown > 0) {
            blinkCooldown--;
        }

        updatePendingJudgementCuts();
        tickJudgementCutTimers();
        if (summoning) {
            tickSummon();
        } else if (judgementCutQueued) {
            tickJudgementCutSkill();
        } else {
            tickSkillWindUp();
        }
        if (summoning || judgementCutQueued) {
            holdPosition(true);
            return;
        }
        updateFloating();
    }

    /** EntityPCosmical: shakeee/showC decay plus the random showC reset. */
    private void tickShadowModelState() {
        if (shakeee > 0) {
            shakeee--;
        }
        if (showC > SHOW_C_FLOOR) {
            showC--;
        }
        if (random.nextInt(10) == 0 && showC <= SHOW_C_FLOOR) {
            showC = random.nextInt(1) * 60;
        }
    }

    private void spawnAmbientPortalParticles() {
        for (int index = 0; index < 4; index++) {
            level().addParticle(ParticleTypes.PORTAL,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 3.0D,
                    getY() + random.nextDouble() * getBbHeight() - 0.25D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 3.0D,
                    (random.nextDouble() - 0.5D) * 2.0D,
                    -random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 2.0D);
        }
    }

    /** Original client tick: two counter-rotating warning rings above the blink destination. */
    private void spawnBlinkWarningParticles() {
        int remainingTicks = entityData.get(BLINK_TICKS);
        BlockPos destination = entityData.get(BLINK_POS);
        if (remainingTicks <= 0 || destination.equals(BlockPos.ZERO)) {
            return;
        }

        float progress = 1.0F - remainingTicks / (float) BLINK_CHARGE_TICKS;
        double x = destination.getX() + 0.5D;
        double y = destination.getY() + 1.5D + progress;
        double z = destination.getZ() + 0.5D;
        double speed = 0.35D + progress * 1.25D;
        float clockwise = (float) ((tickCount * speed) % (Math.PI * 2.0D));
        float counterClockwise = (float) ((-tickCount * speed) % (Math.PI * 2.0D));

        level().addParticle(ModParticles.KIRIN_WARNING.get(), x, y, z, 5.5D, clockwise, 1.0D);
        level().addParticle(ModParticles.KIRIN_WARNING.get(), x, y, z, 6.0D, counterClockwise, 1.0D);
    }

    /** Original {@code spawnJudgementCutChargeParticles}: dust spiralling inward while charging. */
    private void spawnJudgementCutChargeParticles() {
        if (!isChargingJudgementCut()) {
            return;
        }
        int chargeTicks = getJudgementCutChargeTicks();
        float progress = 1.0F
                - Math.max(0.0F, Math.min(chargeTicks / (float) JUDGEMENT_CHARGE_DELAY_TICKS, 1.0F));
        int count = 4 + (int) (progress * 6.0F);
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F);

        for (int index = 0; index < count; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double outerRadius = 12.0D + random.nextDouble() * 8.0D;
            double innerRadius = 2.0D + random.nextDouble() * 2.0D;
            double radius = outerRadius + (innerRadius - outerRadius) * progress;
            level().addParticle(dust,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.5D + random.nextDouble() * (getBbHeight() + 3.0D),
                    getZ() + Math.sin(angle) * radius,
                    0.0D, 0.0D, 0.0D);
        }
        if (random.nextInt(2) == 0) {
            level().addParticle(dust,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.6D,
                    getY() + getBbHeight() * 0.58D + (random.nextDouble() - 0.5D) * 1.4D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.6D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    // ------------------------------------------------------------------
    // Skill wind-up: EntityAISkill(this, 80, 35, true, id)
    // ------------------------------------------------------------------

    private void tickSkillWindUp() {
        if (isShadowClone() || getParasiteStatus() >= 3 || blinkCharge > 0
                || !EvolutionSystem.generationProfile((ServerLevel) level()).specialMoves()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (distanceToSqr(target) < SKILL_TRACK_RANGE * SKILL_TRACK_RANGE
                && getSensing().hasLineOfSight(target)) {
            skillTrackTicks++;
        }
        if (skillTrackTicks >= SKILL_TRACK_TICKS) {
            // The original starts both skill goals on the same tick.
            skillTrackTicks = 0;
            startSummon();
            startJudgementCut();
        }
    }

    // ------------------------------------------------------------------
    // Skill 1: summon() - black hole orb, and a judgement cut volley at stage 3
    // ------------------------------------------------------------------

    private void startSummon() {
        summoning = true;
        summonStage = 0;
        lockedY = getY();
        setParasiteStatus(SKILL_PARASITE_STATUS);
        holdPosition(true);
    }

    private void tickSummon() {
        setParasiteStatus(SKILL_PARASITE_STATUS);
        holdPosition(true);
        LivingEntity target = getTarget();
        if (target != null) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (++summonStage % SUMMON_STAGE_TICKS != 0) {
            return;
        }

        int stage = summonStage / SUMMON_STAGE_TICKS;
        if (isShadowClone() || getTarget() == null) {
            finishSummon();
            return;
        }
        if (stage == SUMMON_ORB_STAGE) {
            summonVoidOrb();
            // The original spawns this volley unconditionally, on top of the judgement goal's.
            spawnJudgementCuts(getTarget());
        }
        if (stage > SUMMON_END_STAGE) {
            finishSummon();
        }
    }

    private void finishSummon() {
        summoning = false;
        summonStage = 0;
        if (!judgementCutQueued) {
            setParasiteStatus(0);
        }
    }

    private void summonVoidOrb() {
        VoidOrbEntity orb = ModEntities.VOID_ORB.get().create(level());
        if (orb == null) {
            return;
        }
        orb.configure(this, VOID_ORB_FUSE_TICKS, VOID_ORB_START_TICKS, true, VOID_ORB_OFFSET);
        orb.moveTo(getX(), getY() + getBbHeight() + VOID_ORB_OFFSET, getZ());
        level().addFreshEntity(orb);
        playSound(ModSounds.KIRIN_SHOOT.get(), getSoundVolume() * 2.0F,
                (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
    }

    // ------------------------------------------------------------------
    // Skill 2: judgement cut - 42 delayed slashes around the target
    // ------------------------------------------------------------------

    private void startJudgementCut() {
        lockedY = getY();
        judgementCutSkillTicks = 0;
        judgementCutQueued = false;
        LivingEntity target = getTarget();
        if (!isShadowClone() && target != null && target.isAlive()) {
            spawnJudgementCuts(target);
        } else {
            judgementCutQueued = false;
            if (!summoning) {
                setParasiteStatus(0);
            }
        }
    }

    private void tickJudgementCutSkill() {
        setParasiteStatus(SKILL_PARASITE_STATUS);
        holdPosition(true);
        LivingEntity target = getTarget();
        if (isShadowClone() || target == null || !target.isAlive()) {
            finishJudgementCut();
            return;
        }
        judgementCutSkillTicks++;
        if (judgementCutSkillTicks > JUDGEMENT_SKILL_TICKS) {
            finishJudgementCut();
        }
    }

    private void finishJudgementCut() {
        judgementCutQueued = false;
        judgementCutSkillTicks = 0;
        if (!summoning) {
            setParasiteStatus(0);
        }
    }

    /** Original {@code spawnJudgementCuts}. */
    public void spawnJudgementCuts(LivingEntity target) {
        if (level().isClientSide || target == null || !target.isAlive()) {
            return;
        }
        level().playSound(null, getX(), getY() + getBbHeight() * 0.55D, getZ(),
                ModSounds.KIRIN_PROJECTILE_CHARGE.get(), SoundSource.HOSTILE, 4.0F,
                0.95F + random.nextFloat() * 0.08F);
        setJudgementCutChargeTicks(JUDGEMENT_CHARGE_DELAY_TICKS);
        setJudgementCutAuraEndTicks(0);
        judgementCutQueued = true;
        lockedY = getY();

        boolean targetIsPlayer = target instanceof Player;
        float damage = targetIsPlayer ? JUDGEMENT_PLAYER_DAMAGE : JUDGEMENT_MOB_DAMAGE;
        for (int index = 0; index < JUDGEMENT_SLASH_COUNT; index++) {
            double passAngle = random.nextDouble() * Math.PI * 2.0D;
            double dirX = Math.cos(passAngle);
            double dirZ = Math.sin(passAngle);
            double beforeTarget = 22.0D + random.nextDouble() * 16.0D;

            double sideOffset;
            double verticalOffset;
            if (targetIsPlayer) {
                float closeRoll = random.nextFloat();
                if (closeRoll < 0.55F) {
                    sideOffset = randomSignedRange(2.4D, 5.2D);
                } else if (closeRoll < 0.88F) {
                    sideOffset = randomSignedRange(5.5D, 10.0D);
                } else {
                    sideOffset = randomSignedRange(10.0D, 22.0D);
                }

                float heightRoll = random.nextFloat();
                if (heightRoll < 0.62F) {
                    verticalOffset = (random.nextDouble() - 0.5D) * 2.2D;
                } else if (heightRoll < 0.9F) {
                    verticalOffset = (random.nextDouble() - 0.5D) * 5.0D;
                } else {
                    verticalOffset = (random.nextDouble() - 0.5D) * 9.0D;
                }
            } else {
                float closeRoll = random.nextFloat();
                if (closeRoll < 0.7F) {
                    sideOffset = (random.nextDouble() - 0.5D) * 1.2D;
                } else if (closeRoll < 0.92F) {
                    sideOffset = (random.nextDouble() - 0.5D) * 3.0D;
                } else {
                    sideOffset = randomSignedRange(4.0D, 8.0D);
                }

                float heightRoll = random.nextFloat();
                if (heightRoll < 0.75F) {
                    verticalOffset = (random.nextDouble() - 0.5D) * Math.max(1.0D, target.getBbHeight() * 0.45D);
                } else if (heightRoll < 0.94F) {
                    verticalOffset = (random.nextDouble() - 0.5D) * Math.max(2.0D, target.getBbHeight() * 0.8D);
                } else {
                    verticalOffset = (random.nextDouble() - 0.5D) * Math.max(3.0D, target.getBbHeight() * 1.2D);
                }
            }

            float yaw = (float) (Math.atan2(dirX, dirZ) * 180.0D / Math.PI);
            float pitch;
            if (targetIsPlayer) {
                if (random.nextFloat() < 0.1F) {
                    pitch = -60.0F + random.nextFloat() * 120.0F;
                } else {
                    pitch = -14.0F + random.nextFloat() * 28.0F;
                }
            } else if (random.nextFloat() < 0.08F) {
                pitch = -35.0F + random.nextFloat() * 70.0F;
            } else {
                pitch = -8.0F + random.nextFloat() * 16.0F;
            }

            pendingJudgementCuts.add(new PendingJudgementSlash(
                    target.getId(),
                    JUDGEMENT_CHARGE_DELAY_TICKS + index + random.nextInt(5),
                    dirX, dirZ, beforeTarget, sideOffset, verticalOffset, yaw, pitch,
                    (float) (random.nextDouble() * 360.0D),
                    110.0F + random.nextFloat() * 75.0F,
                    damage,
                    4 + random.nextInt(8),
                    55 + random.nextInt(18)));
        }
    }

    private void updatePendingJudgementCuts() {
        if (level().isClientSide || pendingJudgementCuts.isEmpty()) {
            return;
        }
        Iterator<PendingJudgementSlash> iterator = pendingJudgementCuts.iterator();
        while (iterator.hasNext()) {
            PendingJudgementSlash slash = iterator.next();
            if (--slash.delay > 0) {
                continue;
            }
            Entity entity = level().getEntity(slash.targetId);
            if (entity instanceof LivingEntity target && target.isAlive()) {
                double centerY = target.getBoundingBox().minY + target.getBbHeight() * 0.55D;
                double sideX = -slash.dirZ;
                double sideZ = slash.dirX;
                double x = target.getX() - slash.dirX * slash.beforeTarget + sideX * slash.sideOffset;
                double y = centerY + slash.verticalOffset;
                double z = target.getZ() - slash.dirZ * slash.beforeTarget + sideZ * slash.sideOffset;
                KirinSlashEntity slashEntity = ModEntities.KIRIN_SLASH.get().create(level());
                if (slashEntity != null) {
                    slashEntity.configure(this, new Vec3(x, y, z), slash.yaw, slash.pitch, slash.roll,
                            slash.length, slash.damage, 0, slash.growTicks, slash.lifeTicks);
                    level().addFreshEntity(slashEntity);
                    level().playSound(null, x, y, z, ModSounds.KIRIN_PROJECTILE_SUMMON.get(),
                            SoundSource.HOSTILE, 0.85F, 0.9F + random.nextFloat() * 0.25F);
                }
            }
            iterator.remove();
        }
    }

    /** Original countdown of the judgement cut charge and its trailing aura. */
    private void tickJudgementCutTimers() {
        int chargeTicks = getJudgementCutChargeTicks();
        if (chargeTicks > 0) {
            setJudgementCutChargeTicks(chargeTicks - 1);
            if (chargeTicks - 1 <= 0) {
                setJudgementCutAuraEndTicks(JUDGEMENT_AURA_END_TICKS);
            }
        }
        int auraEndTicks = getJudgementCutAuraEndTicks();
        if (auraEndTicks > 0) {
            setJudgementCutAuraEndTicks(auraEndTicks - 1);
        }
    }

    private double randomSignedRange(double minimum, double maximum) {
        double value = minimum + random.nextDouble() * (maximum - minimum);
        return random.nextBoolean() ? value : -value;
    }

    /** Original {@code KirinPendingSlash}; {@code delay} is mutated while pending. */
    private static final class PendingJudgementSlash {
        private final int targetId;
        private int delay;
        private final double dirX;
        private final double dirZ;
        private final double beforeTarget;
        private final double sideOffset;
        private final double verticalOffset;
        private final float yaw;
        private final float pitch;
        private final float roll;
        private final float length;
        private final float damage;
        private final int growTicks;
        private final int lifeTicks;

        private PendingJudgementSlash(int targetId, int delay, double dirX, double dirZ,
                double beforeTarget, double sideOffset, double verticalOffset, float yaw, float pitch,
                float roll, float length, float damage, int growTicks, int lifeTicks) {
            this.targetId = targetId;
            this.delay = delay;
            this.dirX = dirX;
            this.dirZ = dirZ;
            this.beforeTarget = beforeTarget;
            this.sideOffset = sideOffset;
            this.verticalOffset = verticalOffset;
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
            this.length = length;
            this.damage = damage;
            this.growTicks = growTicks;
            this.lifeTicks = lifeTicks;
        }
    }

    // ------------------------------------------------------------------
    // Floating (EntityKirin#updateFloating)
    // ------------------------------------------------------------------

    private void updateFloating() {
        fallDistance = 0.0F;
        floatBob++;
        double bob = Math.sin((tickCount + floatBob) * FLOAT_BOB_SPEED) * FLOAT_BOB_AMPLITUDE;
        BlockPos base = BlockPos.containing(getX(), getY() + 0.1D, getZ());
        BlockPos ground = null;
        for (int offset = 0; offset <= FLOAT_GROUND_SCAN; offset++) {
            BlockPos candidate = base.below(offset);
            if (level().getBlockState(candidate).isSolidRender(level(), candidate)) {
                ground = candidate;
                break;
            }
        }

        Vec3 motion = getDeltaMovement();
        if (ground == null) {
            setDeltaMovement(motion.x, 0.0D, motion.z);
            if (++noGroundTicks >= FLOAT_RECOVERY_DELAY_TICKS && tryBlinkToNearbyLand()) {
                noGroundTicks = 0;
            }
            return;
        }

        noGroundTicks = 0;
        double targetY = ground.getY() + 1.0D + FLOAT_HOVER_HEIGHT + bob;
        double difference = targetY - getY();
        double acceleration = difference > 0.0D
                ? difference * FLOAT_UP_ACCELERATION : difference * FLOAT_DOWN_ACCELERATION;
        setDeltaMovement(motion.x, Mth.clamp(motion.y + acceleration, FLOAT_DOWN_MAX, FLOAT_UP_MAX), motion.z);
    }

    /** EntityAIKirinBlink#tryBlinkToNearbyLand. */
    private boolean tryBlinkToNearbyLand() {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int xOffset = -FLOAT_RECOVERY_HORIZONTAL_RANGE;
                xOffset <= FLOAT_RECOVERY_HORIZONTAL_RANGE; xOffset++) {
            for (int zOffset = -FLOAT_RECOVERY_HORIZONTAL_RANGE;
                    zOffset <= FLOAT_RECOVERY_HORIZONTAL_RANGE; zOffset++) {
                for (int yOffset = FLOAT_RECOVERY_VERTICAL_RANGE;
                        yOffset >= -FLOAT_RECOVERY_VERTICAL_RANGE; yOffset--) {
                    BlockPos candidate = origin.offset(xOffset, yOffset, zOffset);
                    if (!isRecoverySpotValid(candidate)) {
                        continue;
                    }
                    double distance = candidate.distSqr(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = candidate;
                    }
                    break;
                }
            }
        }
        if (best == null) {
            return false;
        }
        teleportTo(best.getX() + 0.5D, best.getY(), best.getZ() + 0.5D);
        playSound(ModSounds.INFECTED_ENDERMAN_PORTAL.get(), 1.0F, 1.0F);
        return true;
    }

    private boolean isRecoverySpotValid(BlockPos position) {
        if (!level().hasChunkAt(position)) {
            return false;
        }
        BlockPos below = position.below();
        return level().getBlockState(below).isSolidRender(level(), below)
                && level().getBlockState(position).getCollisionShape(level(), position).isEmpty()
                && level().getBlockState(position.above()).getCollisionShape(level(), position.above()).isEmpty()
                && level().canSeeSky(position.above());
    }

    // ------------------------------------------------------------------
    // Blink (EntityAIKirinBlink)
    // ------------------------------------------------------------------

    private void setBlinkCharge(BlockPos destination, int ticks) {
        blinkDestination = destination == null ? BlockPos.ZERO : destination.immutable();
        blinkCharge = Math.max(0, ticks);
        entityData.set(BLINK_POS, blinkDestination);
        entityData.set(BLINK_TICKS, blinkCharge);
    }

    private void clearBlinkCharge() {
        blinkCharge = 0;
        blinkDestination = BlockPos.ZERO;
        entityData.set(BLINK_POS, BlockPos.ZERO);
        entityData.set(BLINK_TICKS, 0);
    }

    /** Original {@code doBlinkLifeSteal}: steal half of the first non-parasite's health. */
    private void performBlink() {
        teleportTo(blinkDestination.getX() + 0.5D, blinkDestination.getY(), blinkDestination.getZ() + 0.5D);
        playSound(ModSounds.INFECTED_ENDERMAN_PORTAL.get(), 1.0F, 1.0F);

        List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(BLINK_LIFE_STEAL_RADIUS),
                entity -> entity != this && entity.isAlive() && !(entity instanceof Parasite));
        if (nearby.isEmpty()) {
            playSound(ModSounds.ALAFHA_HURT.get(), 0.7F, 0.9F + random.nextFloat() * 0.2F);
            return;
        }

        LivingEntity victim = nearby.get(0);
        float currentHealth = victim.getHealth();
        if (currentHealth <= 0.0F) {
            return;
        }
        float stolen = currentHealth * (float) BLINK_HEALTH_DRAIN_FRACTION;
        victim.setHealth(Math.max(0.0F, currentHealth - stolen));
        forceHurtAnim(victim);
        level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                ModSounds.CRUX_HURT.get(), SoundSource.HOSTILE, 1.0F, 0.8F + random.nextFloat() * 0.4F);
        if (stolen > 0.0F) {
            heal(stolen);
        }
    }

    /** Original {@code EntityAIKirinBlink#forceHurtAnim}. */
    private static void forceHurtAnim(LivingEntity target) {
        target.hurtDuration = FORCED_HURT_ANIM_TICKS;
        target.hurtTime = FORCED_HURT_ANIM_TICKS;
        target.level().broadcastEntityEvent(target, (byte) 2);
    }

    private BlockPos findBlinkDestination(LivingEntity target) {
        BlockPos targetPosition = target.blockPosition();
        for (int attempt = 0; attempt < BLINK_MAX_TRIES; attempt++) {
            double radius = BLINK_RADIUS_MIN + random.nextDouble() * BLINK_RADIUS_MAX;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int x = Mth.floor(targetPosition.getX() + 0.5D + radius * Math.cos(angle));
            int z = Mth.floor(targetPosition.getZ() + 0.5D + radius * Math.sin(angle));
            for (int verticalOffset : BLINK_VERTICAL_OFFSETS) {
                BlockPos candidate = new BlockPos(x, targetPosition.getY() + verticalOffset, z);
                if (isBlinkSpotValid(candidate) && level().canSeeSky(candidate.above())
                        && hasBlinkLineOfSight(target, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isBlinkSpotValid(BlockPos position) {
        if (!level().hasChunkAt(position)) {
            return false;
        }
        AABB collisionBox = new AABB(position).deflate(0.05D);
        BlockPos below = position.below();
        return level().noCollision(this, collisionBox)
                && level().getBlockState(below).isSolidRender(level(), below);
    }

    private boolean hasBlinkLineOfSight(LivingEntity target, BlockPos destination) {
        HitResult result = level().clip(new ClipContext(getEyePosition(), Vec3.atCenterOf(destination),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS && getSensing().hasLineOfSight(target);
    }

    /** Original {@code isIndoors}, inverted: the target must have sky access at head level. */
    private boolean isOutdoors(LivingEntity target) {
        BlockPos head = BlockPos.containing(target.getX(),
                target.getY() + target.getEyeHeight(), target.getZ());
        for (int offset = 0; offset < BLINK_SKY_CHECKS; offset++) {
            if (level().canSeeSky(head.above(offset))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 4.0F : amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getParasiteStatus() != 0 ? ModSounds.MOBSILENCE.get() : ModSounds.KIRIN_GROWL.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return random.nextBoolean() && getAdaptationHitStatus() > 0
                ? ModSounds.MOBSILENCE.get() : ModSounds.KIRIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KIRIN_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return KIRIN_SOUND_VOLUME;
    }

    @Override
    protected boolean hasExclusiveSkill() {
        return blinkCharge > 0 || summoning || judgementCutQueued;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("kirin_blink_cooldown", blinkCooldown);
        tag.putInt("kirin_blink_charge", blinkCharge);
        tag.putLong("kirin_blink_destination", blinkDestination.asLong());
        tag.putInt("kirin_summon_stage", summonStage);
        tag.putBoolean("kirin_summoning", summoning);
        tag.putInt("kirin_judgement_ticks", judgementCutSkillTicks);
        tag.putBoolean("kirin_judgement_queued", judgementCutQueued);
        tag.putInt("kirin_skill_track", skillTrackTicks);
        tag.putInt("kirin_float_bob", floatBob);
        tag.putInt("kirin_no_ground_ticks", noGroundTicks);
        tag.putInt("kirin_status", getParasiteStatus());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        blinkCooldown = tag.getInt("kirin_blink_cooldown");
        blinkCharge = tag.getInt("kirin_blink_charge");
        blinkDestination = BlockPos.of(tag.getLong("kirin_blink_destination"));
        summonStage = tag.getInt("kirin_summon_stage");
        summoning = tag.getBoolean("kirin_summoning");
        judgementCutSkillTicks = tag.getInt("kirin_judgement_ticks");
        judgementCutQueued = tag.getBoolean("kirin_judgement_queued");
        skillTrackTicks = tag.getInt("kirin_skill_track");
        floatBob = tag.getInt("kirin_float_bob");
        noGroundTicks = tag.getInt("kirin_no_ground_ticks");
        setParasiteStatus(tag.getInt("kirin_status"));
        lockedY = getY();
        entityData.set(BLINK_POS, blinkCharge > 0 ? blinkDestination : BlockPos.ZERO);
        entityData.set(BLINK_TICKS, blinkCharge);
    }

    /** Original {@code EntityAIAttackMeleeStatus}: melee is suspended during a skill. */
    private final class KirinMeleeGoal extends MeleeAttackGoal {
        private KirinMeleeGoal() {
            super(KirinEntity.this, 1.0D, true);
        }

        @Override
        public boolean canUse() {
            return getParasiteStatus() == 0 && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return getParasiteStatus() == 0 && super.canContinueToUse();
        }
    }

    private final class KirinBlinkGoal extends Goal {
        private KirinBlinkGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (blinkCooldown > 0 || blinkCharge > 0 || getParasiteStatus() >= 3
                    || summoning || judgementCutQueued
                    || target == null || !target.isAlive() || distanceToSqr(target) <= BLINK_MIN_FAR_DIST_SQ
                    || !getSensing().hasLineOfSight(target) || !isOutdoors(target)) {
                return false;
            }
            BlockPos destination = findBlinkDestination(target);
            if (destination == null) {
                return false;
            }
            blinkDestination = destination;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return blinkCharge > 0 && target != null && target.isAlive();
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public void start() {
            setBlinkCharge(blinkDestination, BLINK_CHARGE_TICKS);
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            playSound(ModSounds.OMBOO_DEATH.get(), 1.0F, 0.9F);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (blinkCharge % 10 == 0) {
                // The original played SoundEvents.field_187685_dH here; both ports map that to the
                // ambient portal sound at the same volume and pitch.
                playSound(SoundEvents.PORTAL_AMBIENT, 0.9F, 1.25F);
            }
            blinkCharge--;
            entityData.set(BLINK_TICKS, Math.max(0, blinkCharge));
            if (blinkCharge <= 0) {
                performBlink();
                clearBlinkCharge();
                blinkCooldown = BLINK_COOLDOWN_TICKS;
            }
        }

        @Override
        public void stop() {
            clearBlinkCharge();
        }
    }

    private static final class KirinGroundNavigation extends GroundPathNavigation {
        private KirinGroundNavigation(Mob mob, Level level) {
            super(mob, level);
        }

        @Override
        protected boolean canUpdatePath() {
            return true;
        }
    }
}
