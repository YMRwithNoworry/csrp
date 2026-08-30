package alku.csrp.entity;

import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.joml.Vector3f;

public final class KirinEntity extends DerivedParasiteEntity {
    public static final int BLINK_CHARGE_TICKS = 60;
    public static final int BLINK_COOLDOWN_TICKS = 200;
    public static final double BLINK_LIFE_STEAL_RADIUS = 5.0D;
    public static final double BLINK_HEALTH_DRAIN_FRACTION = 0.5D;

    private static final int FLOAT_GROUND_SCAN = 24;
    private static final double FLOAT_HOVER_HEIGHT = 0.35D;
    private static final double FLOAT_BOB_AMPLITUDE = 0.06D;
    private static final double FLOAT_UP_MAX = 0.16D;
    private static final double FLOAT_DOWN_MAX = -0.16D;
    private static final int FLOAT_RECOVERY_DELAY_TICKS = 40;
    private static final int FLOAT_RECOVERY_HORIZONTAL_RANGE = 48;
    private static final int FLOAT_RECOVERY_VERTICAL_RANGE = 20;

    private static final int VOID_SKILL_CHARGE_TICKS = 80;
    private static final int VOID_SKILL_RANGE = 42;
    private static final int VOID_SKILL_STAGE_TICKS = 20;
    private static final int VOID_SKILL_END_STAGE = 12;
    private static final int VOID_ORB_FUSE_TICKS = 8;
    private static final int VOID_ORB_START_TICKS = 80;
    private static final double VOID_ORB_OFFSET = 10.0D;

    private static final int LASER_CHARGE_TICKS = 40;
    private static final int LASER_FIRE_TICKS = 20;
    private static final int LASER_COOLDOWN_TICKS = 160;
    private static final double LASER_RANGE = 48.0D;
    private static final int LASER_EFFECT_DURATION_TICKS = 7 * 20;

    /** Original Kirin judgement-cut ("spatial slash") cadence and volley shape. */
    private static final int JUDGEMENT_CUT_CHARGE_TICKS = 80;
    private static final int JUDGEMENT_CUT_CAST_TICKS = 80;
    private static final int JUDGEMENT_CUT_AURA_TICKS = 60;
    private static final int JUDGEMENT_CUT_AURA_END_TICKS = 24;
    private static final int JUDGEMENT_CUT_COUNT = 42;
    private static final double JUDGEMENT_CUT_RANGE = 35.0D;

    private static final float BLOCK_BREAK_MAX_HARDNESS = 27.0F;
    private static final int BLOCK_BREAK_COOLDOWN_TICKS = 60;
    private static final int BLOCK_BREAK_RANGE = 3;

    private static final EntityDataAccessor<BlockPos> BLINK_POS = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> BLINK_TICKS = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> VOID_CASTING = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LASER_TICKS = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LASER_TARGET_ID = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> JUDGEMENT_CUT_CHARGE = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> JUDGEMENT_CUT_AURA_END = SynchedEntityData.defineId(
            KirinEntity.class, EntityDataSerializers.INT);

    private final RawAnimation idleAnimation = RawAnimation.begin()
            .thenLoop("animation.kirin.func_78087_a.age_in_ticks");
    private final RawAnimation cloneAnimation = RawAnimation.begin()
            .thenLoop("animation.kirin.func_78087_a.age_in_ticks.get_clone_c_1");
    private final RawAnimation cloneShakingAnimation = RawAnimation.begin()
            .thenLoop("animation.kirin.func_78087_a.age_in_ticks.get_clone_c_1.shaking_c_1");
    private final RawAnimation shakingAnimation = RawAnimation.begin()
            .thenLoop("animation.kirin.func_78087_a.age_in_ticks.shaking_c_1");

    private int blinkCooldown;
    private int blinkCharge;
    private BlockPos blinkDestination = BlockPos.ZERO;
    private int voidSkillCharge;
    private int voidSkillCastTicks;
    private int voidSkillStage;
    private int laserCooldown;
    private int floatBob;
    private int noGroundTicks;
    private int blockBreakCooldown;
    private int judgementCutCharge;
    private int judgementCutSkillTicks;
    private boolean judgementCutQueued;
    private final List<PendingJudgementCut> pendingJudgementCuts = new ArrayList<>();

    public KirinEntity(EntityType<? extends KirinEntity> type, Level level) {
        super(type, level);
        setNoGravity(false);
        xpReward = 350;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 410.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 155.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.STEP_HEIGHT, 1.0D)
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
        goalSelector.addGoal(1, new KirinBlinkGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BLINK_POS, BlockPos.ZERO);
        builder.define(BLINK_TICKS, 0);
        builder.define(VOID_CASTING, false);
        builder.define(LASER_TICKS, 0);
        builder.define(LASER_TARGET_ID, 0);
        builder.define(JUDGEMENT_CUT_CHARGE, 0);
        builder.define(JUDGEMENT_CUT_AURA_END, 0);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(false);
        if (level().isClientSide) {
            spawnAmbientPortalParticles();
            spawnBlinkWarningParticles();
            if (isChargingJudgementCut()) {
                spawnJudgementCutChargeParticles();
            }
            return;
        }

        if (blinkCooldown > 0) {
            blinkCooldown--;
        }
        if (blockBreakCooldown > 0) {
            blockBreakCooldown--;
        }

        EvolutionSystem.GenerationProfile profile = EvolutionSystem.generationProfile((ServerLevel) level());
        updatePendingJudgementCuts();
        boolean judgementWasCasting = isJudgementCutCasting();
        tickJudgementCut();
        if (!judgementWasCasting && !isJudgementCutCasting()) {
            tickLaserSkill();
        }
        if (!judgementWasCasting && !isJudgementCutCasting() && !isLaserCasting()) {
            if (isVoidCasting()) {
                tickVoidSkill();
            } else {
                chargeVoidSkill();
            }
        } else {
            voidSkillCharge = 0;
            if (isVoidCasting()) {
                finishVoidSkill();
            }
        }
        if (blinkCharge <= 0 && !isVoidCasting() && !isLaserCasting()) {
            updateFloating();
        }
        if (profile.blockSearch()) {
            tryBreakBlocks();
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

    private void spawnBlinkWarningParticles() {
        int remainingTicks = entityData.get(BLINK_TICKS);
        BlockPos destination = entityData.get(BLINK_POS);
        if (remainingTicks <= 0 || destination.equals(BlockPos.ZERO)) {
            return;
        }

        float progress = 1.0F - remainingTicks / (float) BLINK_CHARGE_TICKS;
        double x = destination.getX() + 0.5D;
        double y = destination.getY() + 0.05D + progress * 0.15D;
        double z = destination.getZ() + 0.5D;
        double rotationSpeed = 0.35D + progress * 1.25D;
        float clockwise = (float) ((tickCount * rotationSpeed) % (Math.PI * 2.0D));
        float counterClockwise = (float) ((-tickCount * rotationSpeed) % (Math.PI * 2.0D));

        level().addParticle(ModParticles.KIRIN_WARNING.get(), x, y, z, 5.5D, clockwise, 1.0D);
        level().addParticle(ModParticles.KIRIN_WARNING.get(), x, y, z, 6.0D, counterClockwise, 1.0D);
    }

    /**
     * Charges and releases Kirin's original judgement-cut volley. The old AI
     * charged for 80 ticks once a target was within 35 blocks, then held the
     * casting pose for another 80 ticks while 42 cuts appeared around the
     * target over the following second.
     */
    private void tickJudgementCut() {
        if (isShadowClone()) {
            judgementCutCharge = 0;
            judgementCutSkillTicks = 0;
            judgementCutQueued = false;
            pendingJudgementCuts.clear();
            setJudgementCutChargeTicks(0);
            setJudgementCutAuraEndTicks(0);
            return;
        }

        if (judgementCutSkillTicks > 0) {
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                judgementCutSkillTicks = 0;
                judgementCutQueued = false;
                pendingJudgementCuts.clear();
                setJudgementCutChargeTicks(0);
                setJudgementCutAuraEndTicks(0);
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            judgementCutSkillTicks++;
            if (judgementCutSkillTicks > JUDGEMENT_CUT_CAST_TICKS) {
                judgementCutSkillTicks = 0;
                judgementCutQueued = false;
                setJudgementCutChargeTicks(0);
                setJudgementCutAuraEndTicks(0);
            }
            tickJudgementCutAura();
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || distanceToSqr(target) > JUDGEMENT_CUT_RANGE * JUDGEMENT_CUT_RANGE
                || !getSensing().hasLineOfSight(target) || isVoidCasting() || isLaserCasting()
                || blinkCharge > 0 || super.isUsingDerivedSkill()) {
            judgementCutCharge = 0;
            tickJudgementCutAura();
            return;
        }

        if (++judgementCutCharge >= JUDGEMENT_CUT_CHARGE_TICKS) {
            judgementCutCharge = 0;
            judgementCutSkillTicks = 1;
            judgementCutQueued = true;
            spawnJudgementCuts(target);
        }
        tickJudgementCutAura();
    }

    private void tickJudgementCutAura() {
        int chargeTicks = getJudgementCutChargeTicks();
        if (chargeTicks > 0) {
            setJudgementCutChargeTicks(chargeTicks - 1);
            if (chargeTicks - 1 <= 0) {
                setJudgementCutAuraEndTicks(JUDGEMENT_CUT_AURA_END_TICKS);
            }
        }
        int auraTicks = getJudgementCutAuraEndTicks();
        if (auraTicks > 0) {
            setJudgementCutAuraEndTicks(auraTicks - 1);
        }
    }

    public void spawnJudgementCuts(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel) || target == null || !target.isAlive()) {
            return;
        }
        playSound(ModSounds.KIRIN_PROJECTILE_CHARGE.get(), 4.0F, 0.95F + random.nextFloat() * 0.08F);
        setJudgementCutChargeTicks(JUDGEMENT_CUT_AURA_TICKS);
        setJudgementCutAuraEndTicks(0);

        boolean targetIsPlayer = target instanceof net.minecraft.world.entity.player.Player;
        float damage = targetIsPlayer ? 8.0F : 10.0F;
        for (int index = 0; index < JUDGEMENT_CUT_COUNT; index++) {
            double passAngle = random.nextDouble() * Math.PI * 2.0D;
            double dirX = Math.cos(passAngle);
            double dirZ = Math.sin(passAngle);
            double beforeTarget = 22.0D + random.nextDouble() * 16.0D;
            double sideOffset;
            double verticalOffset;
            if (targetIsPlayer) {
                float closeRoll = random.nextFloat();
                sideOffset = closeRoll < 0.55F ? getRandomSignedRange(2.4D, 5.2D)
                        : closeRoll < 0.88F ? getRandomSignedRange(5.5D, 10.0D)
                        : getRandomSignedRange(10.0D, 22.0D);
                float heightRoll = random.nextFloat();
                verticalOffset = heightRoll < 0.62F ? (random.nextDouble() - 0.5D) * 2.2D
                        : heightRoll < 0.9F ? (random.nextDouble() - 0.5D) * 5.0D
                        : (random.nextDouble() - 0.5D) * 9.0D;
            } else {
                float closeRoll = random.nextFloat();
                sideOffset = closeRoll < 0.7F ? (random.nextDouble() - 0.5D) * 1.2D
                        : closeRoll < 0.92F ? (random.nextDouble() - 0.5D) * 3.0D
                        : getRandomSignedRange(4.0D, 8.0D);
                float heightRoll = random.nextFloat();
                verticalOffset = heightRoll < 0.75F
                        ? (random.nextDouble() - 0.5D) * Math.max(1.0D, target.getBbHeight() * 0.45D)
                        : heightRoll < 0.94F
                        ? (random.nextDouble() - 0.5D) * Math.max(2.0D, target.getBbHeight() * 0.8D)
                        : (random.nextDouble() - 0.5D) * Math.max(3.0D, target.getBbHeight() * 1.2D);
            }

            float yaw = (float) Math.toDegrees(Math.atan2(dirX, dirZ));
            float pitch;
            if (targetIsPlayer) {
                pitch = random.nextFloat() < 0.1F ? -60.0F + random.nextFloat() * 120.0F
                        : -14.0F + random.nextFloat() * 28.0F;
            } else {
                pitch = random.nextFloat() < 0.08F ? -35.0F + random.nextFloat() * 70.0F
                        : -8.0F + random.nextFloat() * 16.0F;
            }
            float roll = random.nextFloat() * 360.0F;
            float length = 110.0F + random.nextFloat() * 75.0F;
            int delay = JUDGEMENT_CUT_AURA_TICKS + index + random.nextInt(5);
            int grow = 4 + random.nextInt(8);
            int life = 55 + random.nextInt(18);
            pendingJudgementCuts.add(new PendingJudgementCut(target.getId(), delay, dirX, dirZ,
                    beforeTarget, sideOffset, verticalOffset, yaw, pitch, roll, length, damage, grow, life));
        }
    }

    private void updatePendingJudgementCuts() {
        if (pendingJudgementCuts.isEmpty() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Iterator<PendingJudgementCut> iterator = pendingJudgementCuts.iterator();
        while (iterator.hasNext()) {
            PendingJudgementCut pending = iterator.next();
            pending.delay--;
            if (pending.delay > 0) {
                continue;
            }
            Entity entity = serverLevel.getEntity(pending.targetId);
            if (entity instanceof LivingEntity target && target.isAlive()) {
                double cx = target.getX();
                double cy = target.getBoundingBox().minY + target.getBbHeight() * 0.55D;
                double cz = target.getZ();
                double sideX = -pending.dirZ;
                double sideZ = pending.dirX;
                Vec3 start = new Vec3(
                        cx - pending.dirX * pending.beforeTarget + sideX * pending.sideOffset,
                        cy + pending.verticalOffset,
                        cz - pending.dirZ * pending.beforeTarget + sideZ * pending.sideOffset);
                KirinSlashEntity slash = KirinSlashEntity.create(serverLevel, this, start,
                        pending.yaw, pending.pitch, pending.roll, pending.length, pending.damage,
                        0, pending.growTicks, pending.lifeTicks);
                if (slash != null) {
                    serverLevel.addFreshEntity(slash);
                }
            }
            iterator.remove();
        }
    }

    private double getRandomSignedRange(double min, double max) {
        double value = min + random.nextDouble() * (max - min);
        return random.nextBoolean() ? value : -value;
    }

    public void setJudgementCutChargeTicks(int ticks) {
        entityData.set(JUDGEMENT_CUT_CHARGE, Math.max(0, ticks));
    }

    public int getJudgementCutChargeTicks() {
        return entityData.get(JUDGEMENT_CUT_CHARGE);
    }

    public void setJudgementCutAuraEndTicks(int ticks) {
        entityData.set(JUDGEMENT_CUT_AURA_END, Math.max(0, ticks));
    }

    public int getJudgementCutAuraEndTicks() {
        return entityData.get(JUDGEMENT_CUT_AURA_END);
    }

    public boolean isChargingJudgementCut() {
        return getJudgementCutChargeTicks() > 0 || getJudgementCutAuraEndTicks() > 0;
    }

    private boolean isJudgementCutCasting() {
        return judgementCutSkillTicks > 0;
    }

    private void spawnJudgementCutChargeParticles() {
        float progress = 1.0F - Mth.clamp(getJudgementCutChargeTicks() / (float) JUDGEMENT_CUT_AURA_TICKS,
                0.0F, 1.0F);
        int count = 4 + (int) (progress * 6.0F);
        DustParticleOptions auraDust = new DustParticleOptions(new Vector3f(0.92F, 0.05F, 0.65F), 1.0F);
        for (int index = 0; index < count; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double outerRadius = 12.0D + random.nextDouble() * 8.0D;
            double innerRadius = 2.0D + random.nextDouble() * 2.0D;
            double radius = outerRadius + (innerRadius - outerRadius) * progress;
            level().addParticle(auraDust,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.5D + random.nextDouble() * (getBbHeight() + 3.0D),
                    getZ() + Math.sin(angle) * radius, 0.92D, 0.05D, 0.65D);
        }
        if (random.nextBoolean()) {
            level().addParticle(auraDust,
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.6D,
                    getY() + getBbHeight() * 0.58D + (random.nextDouble() - 0.5D) * 1.4D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 1.6D,
                    1.0D, 0.08D, 0.75D);
        }
    }

    private void chargeVoidSkill() {
        LivingEntity target = getTarget();
        if (isShadowClone() || blinkCharge > 0 || isLaserCasting() || isUsingDerivedSkill()
                || target == null || !target.isAlive()
                || distanceToSqr(target) >= VOID_SKILL_RANGE * VOID_SKILL_RANGE
                || !getSensing().hasLineOfSight(target)) {
            return;
        }
        if (++voidSkillCharge >= VOID_SKILL_CHARGE_TICKS) {
            voidSkillCharge = 0;
            voidSkillCastTicks = 0;
            voidSkillStage = 0;
            entityData.set(VOID_CASTING, true);
            getNavigation().stop();
            summonVoidOrb();
        }
    }

    private void tickVoidSkill() {
        getNavigation().stop();
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x * 0.5D, 0.0D, motion.z * 0.5D);
        LivingEntity target = getTarget();
        if (target != null) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (++voidSkillCastTicks % VOID_SKILL_STAGE_TICKS != 0) {
            return;
        }

        voidSkillStage++;
        if (isShadowClone() || target == null || !target.isAlive()) {
            finishVoidSkill();
            return;
        }
        if (voidSkillStage > VOID_SKILL_END_STAGE) {
            finishVoidSkill();
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
        playSound(ModSounds.KIRIN_BLACK_HOLE.get(), getSoundVolume() * 2.0F,
                (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
    }

    private void finishVoidSkill() {
        voidSkillCastTicks = 0;
        voidSkillStage = 0;
        entityData.set(VOID_CASTING, false);
    }

    private boolean isVoidCasting() {
        return entityData.get(VOID_CASTING);
    }

    private void tickLaserSkill() {
        if (laserCooldown > 0) {
            laserCooldown--;
        }
        int ticks = entityData.get(LASER_TICKS);
        if (ticks <= 0) {
            tryStartLaserSkill();
            return;
        }

        Entity entity = level().getEntity(entityData.get(LASER_TARGET_ID));
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            finishLaserSkill();
            return;
        }
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().scale(0.35D));
        getLookControl().setLookAt(target, 45.0F, 45.0F);

        if (ticks == LASER_FIRE_TICKS) {
            applyLaserDebuffs(target);
            playSound(SoundEvents.GUARDIAN_ATTACK, 2.0F, 0.75F);
        }
        if (ticks <= LASER_FIRE_TICKS && getSensing().hasLineOfSight(target)) {
            target.invulnerableTime = 0;
            target.hurt(damageSources().mobAttack(this), 2.0F);
            target.invulnerableTime = 0;
            target.igniteForSeconds(5.0F);
        }

        entityData.set(LASER_TICKS, ticks - 1);
        if (ticks <= 1) {
            finishLaserSkill();
        }
    }

    private void tryStartLaserSkill() {
        LivingEntity target = getTarget();
        if (laserCooldown > 0 || isShadowClone() || blinkCharge > 0 || isVoidCasting()
                || isUsingDerivedSkill() || target == null || !target.isAlive()
                || distanceToSqr(target) > LASER_RANGE * LASER_RANGE
                || !getSensing().hasLineOfSight(target)) {
            return;
        }
        entityData.set(LASER_TICKS, LASER_CHARGE_TICKS + LASER_FIRE_TICKS);
        entityData.set(LASER_TARGET_ID, target.getId());
        getNavigation().stop();
        playSound(ModSounds.KIRIN_LIVING.get(), 2.0F, 0.75F);
    }

    private void applyLaserDebuffs(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, LASER_EFFECT_DURATION_TICKS, 1), this);
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, LASER_EFFECT_DURATION_TICKS, 0), this);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, LASER_EFFECT_DURATION_TICKS, 0), this);
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, LASER_EFFECT_DURATION_TICKS, 1), this);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, LASER_EFFECT_DURATION_TICKS, 1), this);
        target.addEffect(new MobEffectInstance(ModMobEffects.BLEED, LASER_EFFECT_DURATION_TICKS, 1), this);
        target.addEffect(new MobEffectInstance(ModMobEffects.EFFECTPOS, LASER_EFFECT_DURATION_TICKS, 0), this);
        target.addEffect(new MobEffectInstance(ModMobEffects.EFFECTNEG, LASER_EFFECT_DURATION_TICKS, 0), this);
        target.addEffect(new MobEffectInstance(ModMobEffects.INDEAF, LASER_EFFECT_DURATION_TICKS, 0), this);
        target.addEffect(new MobEffectInstance(ModMobEffects.OVERHEATING, LASER_EFFECT_DURATION_TICKS, 0), this);
        target.addEffect(new MobEffectInstance(ModMobEffects.NOVISION, LASER_EFFECT_DURATION_TICKS, 0), this);
        target.addEffect(new MobEffectInstance(ModMobEffects.BRAINING, LASER_EFFECT_DURATION_TICKS, 0), this);
        target.addEffect(new MobEffectInstance(ModMobEffects.MUSCLEOUT, LASER_EFFECT_DURATION_TICKS, 0), this);
    }

    private void finishLaserSkill() {
        entityData.set(LASER_TICKS, 0);
        entityData.set(LASER_TARGET_ID, 0);
        laserCooldown = LASER_COOLDOWN_TICKS;
    }

    public boolean isLaserCharging() {
        return entityData.get(LASER_TICKS) > LASER_FIRE_TICKS;
    }

    public boolean isLaserFiring() {
        int ticks = entityData.get(LASER_TICKS);
        return ticks > 0 && ticks <= LASER_FIRE_TICKS;
    }

    public int getLaserTargetId() {
        return entityData.get(LASER_TARGET_ID);
    }

    private boolean isLaserCasting() {
        return entityData.get(LASER_TICKS) > 0;
    }

    @Override
    public boolean isUsingDerivedSkill() {
        return super.isUsingDerivedSkill() || isJudgementCutCasting();
    }

    @Override
    protected boolean hasExclusiveSkill() {
        return blinkCharge > 0 || isVoidCasting() || isLaserCasting() || isJudgementCutCasting();
    }

    private void updateFloating() {
        fallDistance = 0.0F;
        floatBob++;
        double bob = Math.sin((tickCount + floatBob) * 0.12D) * FLOAT_BOB_AMPLITUDE;
        BlockPos base = BlockPos.containing(getX(), getY() + 0.1D, getZ());
        BlockPos ground = null;
        for (int offset = 0; offset <= FLOAT_GROUND_SCAN; offset++) {
            BlockPos candidate = base.below(offset);
            if (level().getBlockState(candidate).isSolidRender(level(), candidate)) {
                ground = candidate;
                break;
            }
        }

        if (ground == null) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, 0.0D, motion.z);
            if (++noGroundTicks >= FLOAT_RECOVERY_DELAY_TICKS && tryBlinkToNearbyLand()) {
                noGroundTicks = 0;
            }
            return;
        }

        noGroundTicks = 0;
        double targetY = ground.getY() + 1.0D + FLOAT_HOVER_HEIGHT + bob;
        double difference = targetY - getY();
        double acceleration = difference > 0.0D ? difference * 0.12D : difference * 0.06D;
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x,
                Mth.clamp(motion.y + acceleration, FLOAT_DOWN_MAX, FLOAT_UP_MAX), motion.z);
    }

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
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
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

    private void performBlink() {
        LivingEntity slashTarget = getTarget();
        playSound(alku.csrp.registry.ModSounds.KIRIN_PROJECTILE_CHARGE.get(), 1.0F, 1.0F);
        Vec3 origin = position();
        teleportTo(blinkDestination.getX() + 0.5D, blinkDestination.getY(), blinkDestination.getZ() + 0.5D);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        if (level() instanceof ServerLevel serverLevel && slashTarget != null) {
            Vec3 start = origin.add(0.0D, getBbHeight() * 0.6D, 0.0D);
            Vec3 toTarget = slashTarget.position().add(0.0D, slashTarget.getBbHeight() * 0.5D, 0.0D)
                    .subtract(start);
            float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.x, toTarget.z)));
            float pitch = (float) (Math.toDegrees(-Math.atan2(toTarget.y,
                    Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z))));
            KirinSlashEntity slash = KirinSlashEntity.create(serverLevel, this, start,
                    yaw, pitch, 14.0F, 6.0F, 2, 3, 30);
            if (slash != null) {
                serverLevel.addFreshEntity(slash);
            }
        }
        DragonEggAssimilationEntity.assimilateDragonEggs(level(),
                getBoundingBox().inflate(BLINK_LIFE_STEAL_RADIUS));
        List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(BLINK_LIFE_STEAL_RADIUS),
                entity -> entity != this && entity.isAlive() && !(entity instanceof Parasite));
        if (nearby.isEmpty()) {
            playSound(SoundEvents.ENDERMAN_HURT, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            return;
        }

        LivingEntity victim = nearby.getFirst();
        float currentHealth = victim.getHealth();
        if (currentHealth <= 0.0F) {
            return;
        }
        float stolen = currentHealth * (float) BLINK_HEALTH_DRAIN_FRACTION;
        victim.setHealth(Math.max(0.0F, currentHealth - stolen));
        level().broadcastEntityEvent(victim, (byte) 2);
        victim.playSound(SoundEvents.GENERIC_HURT, 1.0F, 0.8F + random.nextFloat() * 0.4F);
        heal(stolen);
    }

    private BlockPos findBlinkDestination(LivingEntity target) {
        BlockPos targetPosition = target.blockPosition();
        int[] verticalOffsets = {0, 1, -1, 2, -2, 3, -3, 4, -4, 6, -6, 8, -8};
        for (int attempt = 0; attempt < 64; attempt++) {
            double radius = 1.5D + random.nextDouble() * 22.5D;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int x = Mth.floor(targetPosition.getX() + 0.5D + radius * Math.cos(angle));
            int z = Mth.floor(targetPosition.getZ() + 0.5D + radius * Math.sin(angle));
            for (int verticalOffset : verticalOffsets) {
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

    private boolean isOutdoors(LivingEntity target) {
        BlockPos head = BlockPos.containing(target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
        for (int offset = 0; offset < 3; offset++) {
            if (level().canSeeSky(head.above(offset))) {
                return true;
            }
        }
        return false;
    }

    private void tryBreakBlocks() {
        LivingEntity target = getTarget();
        if (blockBreakCooldown > 0 || target == null || !target.isAlive()
                || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        int verticalOffset = 0;
        if (target.distanceToSqr(getX(), target.getY(), getZ()) < 9.0D) {
            if (target.getY() - getY() < -1.0D) {
                verticalOffset = onGround() ? -2 : -3;
            } else if (target.getY() - getY() > 2.0D) {
                verticalOffset = 1;
            }
        }

        int height = Math.max(1, Mth.ceil(getBbHeight()));
        boolean brokeAny = false;
        BlockPos origin = BlockPos.containing(getX(), getY() + 0.1D, getZ());
        int activeRange = verticalOffset > 0 ? 0 : BLOCK_BREAK_RANGE;
        for (int xOffset = -activeRange; xOffset <= activeRange; xOffset++) {
            for (int zOffset = -activeRange; zOffset <= activeRange; zOffset++) {
                for (int yOffset = 1 + verticalOffset; yOffset <= height + verticalOffset; yOffset++) {
                    BlockPos candidate = origin.offset(xOffset, yOffset, zOffset);
                    BlockState state = level().getBlockState(candidate);
                    float hardness = state.getDestroySpeed(level(), candidate);
                    if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                            || hardness < 0.0F || hardness > BLOCK_BREAK_MAX_HARDNESS
                            || state.is(ModBlocks.BIOMEHEART.get()) || state.is(ModBlocks.COLONYHEART.get())
                            || state.is(ModBlocks.PARASITE_STRUCTURE.get())) {
                        continue;
                    }
                    brokeAny |= ParasiteBlockInventory.collect((ServerLevel) level(), candidate, this);
                }
            }
        }
        blockBreakCooldown = brokeAny ? BLOCK_BREAK_COOLDOWN_TICKS : 10;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return super.doHurtTarget(target);
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
        return ModSounds.KIRIN_LIVING.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.KIRIN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KIRIN_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("kirin_blink_cooldown", blinkCooldown);
        tag.putInt("kirin_blink_charge", blinkCharge);
        tag.putLong("kirin_blink_destination", blinkDestination.asLong());
        tag.putInt("kirin_void_charge", voidSkillCharge);
        tag.putInt("kirin_void_cast_ticks", voidSkillCastTicks);
        tag.putInt("kirin_void_stage", voidSkillStage);
        tag.putInt("kirin_laser_cooldown", laserCooldown);
        tag.putInt("kirin_float_bob", floatBob);
        tag.putInt("kirin_no_ground_ticks", noGroundTicks);
        tag.putInt("kirin_block_break_cooldown", blockBreakCooldown);
        tag.putInt("kirin_judgement_charge", judgementCutCharge);
        tag.putInt("kirin_judgement_skill_ticks", judgementCutSkillTicks);
        tag.putInt("kirin_judgement_aura_ticks", getJudgementCutChargeTicks());
        tag.putInt("kirin_judgement_aura_end_ticks", getJudgementCutAuraEndTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        blinkCooldown = tag.contains("kirin_blink_cooldown")
                ? tag.getInt("kirin_blink_cooldown") : tag.getInt("blink_cooldown");
        blinkCharge = tag.contains("kirin_blink_charge")
                ? tag.getInt("kirin_blink_charge") : tag.getInt("blink_charge");
        blinkDestination = tag.contains("kirin_blink_destination")
                ? BlockPos.of(tag.getLong("kirin_blink_destination"))
                : new BlockPos(tag.getInt("blink_x"), tag.getInt("blink_y"), tag.getInt("blink_z"));
        voidSkillCharge = tag.contains("kirin_void_charge")
                ? tag.getInt("kirin_void_charge") : Math.max(0, 80 - tag.getInt("void_orb_cooldown"));
        voidSkillCastTicks = tag.getInt("kirin_void_cast_ticks");
        voidSkillStage = tag.getInt("kirin_void_stage");
        laserCooldown = tag.getInt("kirin_laser_cooldown");
        floatBob = tag.getInt("kirin_float_bob");
        noGroundTicks = tag.getInt("kirin_no_ground_ticks");
        blockBreakCooldown = tag.getInt("kirin_block_break_cooldown");
        judgementCutCharge = tag.getInt("kirin_judgement_charge");
        judgementCutSkillTicks = tag.getInt("kirin_judgement_skill_ticks");
        judgementCutQueued = judgementCutSkillTicks > 0;
        entityData.set(JUDGEMENT_CUT_CHARGE, tag.getInt("kirin_judgement_aura_ticks"));
        entityData.set(JUDGEMENT_CUT_AURA_END, tag.getInt("kirin_judgement_aura_end_ticks"));
        entityData.set(BLINK_POS, blinkCharge > 0 ? blinkDestination : BlockPos.ZERO);
        entityData.set(BLINK_TICKS, blinkCharge);
        entityData.set(VOID_CASTING, voidSkillCastTicks > 0);
        entityData.set(LASER_TICKS, 0);
        entityData.set(LASER_TARGET_ID, 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, state -> {
            if (isShadowClone()) {
                return state.setAndContinue(isShadowHitFlashing() ? cloneShakingAnimation : cloneAnimation);
            }
            if (isShadowHitFlashing()) {
                return state.setAndContinue(shakingAnimation);
            }
            if (isLaserCharging()) {
                return state.setAndContinue(shakingAnimation);
            }
            if (isShadowed() && getShadowRenderAlpha(0.0F) > 0.0F) {
                return state.setAndContinue(idleAnimation);
            }
            return state.setAndContinue(idleAnimation);
        }));
    }

    private final class KirinBlinkGoal extends Goal {
        private KirinBlinkGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (blinkCooldown > 0 || blinkCharge > 0 || isVoidCasting() || isUsingDerivedSkill()
                    || target == null || !target.isAlive() || distanceToSqr(target) <= 256.0D
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
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.9F);
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
            if (level() instanceof ServerLevel serverLevel && blinkCharge % 10 == 0) {
                Vec3 destination = Vec3.atCenterOf(blinkDestination);
                serverLevel.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y, destination.z,
                        8, 0.8D, 1.2D, 0.8D, 0.05D);
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
            if (blinkCharge > 0) {
                clearBlinkCharge();
                blinkCooldown = 40;
            }
        }
    }

    private static final class PendingJudgementCut {
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

        private PendingJudgementCut(int targetId, int delay, double dirX, double dirZ,
                double beforeTarget, double sideOffset, double verticalOffset,
                float yaw, float pitch, float roll, float length, float damage,
                int growTicks, int lifeTicks) {
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
