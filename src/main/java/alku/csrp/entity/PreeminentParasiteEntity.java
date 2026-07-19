package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Shared implementation for the six legacy preeminent parasites. This tier
 * uses stronger adaptation and delegates its battlefield support to Succors.
 */
public final class PreeminentParasiteEntity extends PrimitiveParasiteEntity {
    private static final int MAX_ADAPTATION_HITS = 5;
    private static final int MAX_LEARNABLE_DAMAGE_SOURCES = 20;
    private static final float ADAPTATION_PER_HIT = 0.20F;
    private static final float ADAPTATION_LEARN_CHANCE = 1.0F;
    private static final float BURNING_LEARN_CHANCE = 0.70F;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");

    private final Kind kind;
    private UUID summonerId;
    private int blockBreakCooldown;
    private int supportCooldown;
    private int stealthCooldown;
    private int attackAnimationTicks;
    private boolean succorActionConsumed;

    public PreeminentParasiteEntity(EntityType<? extends PreeminentParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = 75;
        if (kind.flying) {
            moveControl = new FlyingMoveControl(this, 18, true);
            setNoGravity(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        AttributeSupplier.Builder attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, kind.movementSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind.knockbackResistance)
                .add(Attributes.FOLLOW_RANGE, kind.followRange);
        if (kind.flying) {
            attributes.add(Attributes.FLYING_SPEED, kind.movementSpeed);
        }
        return attributes;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        switch (activeKind()) {
            case BOGLE -> {
                goalSelector.addGoal(1, new BogleBombGoal());
                goalSelector.addGoal(2, new FlightPursuitGoal(1.0D));
            }
            case CARRIER_COLONY -> {
                goalSelector.addGoal(1, new ColonySupportGoal());
                goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, false));
            }
            case HAUNTER -> {
                goalSelector.addGoal(1, new HaunterHomingBurstGoal());
                goalSelector.addGoal(2, new EvasiveDashGoal(100, 1.0D));
                goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.05D, false));
            }
            case BOMBER_HEAVY -> {
                goalSelector.addGoal(1, new HeavyBomberBombGoal());
                goalSelector.addGoal(2, new FlightPursuitGoal(0.85D));
            }
            case WRAITH -> {
                goalSelector.addGoal(1, new WraithNadeBurstGoal());
                goalSelector.addGoal(2, new FlightPursuitGoal(1.0D));
            }
            case SUCCOR -> goalSelector.addGoal(1, new SuccorActionGoal());
        }
    }

    @Override
    public void tick() {
        super.tick();
        Kind activeKind = activeKind();
        if (activeKind.flying) {
            setNoGravity(true);
        }
        if (level().isClientSide) {
            return;
        }
        if (blockBreakCooldown > 0) {
            blockBreakCooldown--;
        }
        if (supportCooldown > 0) {
            supportCooldown--;
        }
        if (stealthCooldown > 0) {
            stealthCooldown--;
        }
        if (attackAnimationTicks > 0) {
            attackAnimationTicks--;
        }
        if (activeKind.flying && onGround()) {
            getMoveControl().setWantedPosition(getX(), getY() + 4.0D, getZ(), 0.60D);
        }
        if (isStealthKind() && stealthCooldown <= 0 && getHealth() >= getMaxHealth() * 0.40F) {
            setInvisible(true);
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        breakBlocksTowardsTarget(target, activeKind);
        if (activeKind != Kind.SUCCOR && supportCooldown <= 0 && tickCount % 40 == 0) {
            trySummonSuccor(target);
        }
        if ((activeKind == Kind.BOGLE || activeKind == Kind.WRAITH) && tickCount % 20 == 0) {
            applyFlyingAura();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 4.0F;
        }
        if (!level().isClientSide && isStealthKind()) {
            revealStealth();
        }
        return super.hurt(source, amount);
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return MAX_ADAPTATION_HITS;
    }

    @Override
    protected float damageAdaptationPerHit() {
        return ADAPTATION_PER_HIT;
    }

    @Override
    protected int maxLearnableDamageSources() {
        return MAX_LEARNABLE_DAMAGE_SOURCES;
    }

    @Override
    protected boolean shouldLearnDamageSource(DamageSource source, String damageId, int previousHits) {
        float chance = isOnFire() ? BURNING_LEARN_CHANCE : ADAPTATION_LEARN_CHANCE;
        return previousHits < MAX_ADAPTATION_HITS && random.nextFloat() < chance;
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hurt = super.doHurtTarget(entity);
        if (hurt) {
            attackAnimationTicks = 8;
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("preeminent_support_cooldown", supportCooldown);
        tag.putInt("preeminent_stealth_cooldown", stealthCooldown);
        tag.putBoolean("preeminent_succor_action", succorActionConsumed);
        if (summonerId != null) {
            tag.putUUID("preeminent_summoner", summonerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        supportCooldown = tag.getInt("preeminent_support_cooldown");
        stealthCooldown = tag.getInt("preeminent_stealth_cooldown");
        succorActionConsumed = tag.getBoolean("preeminent_succor_action");
        summonerId = tag.hasUUID("preeminent_summoner") ? tag.getUUID("preeminent_summoner") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    public Kind getKind() {
        return activeKind();
    }

    public void setSummoner(PreeminentParasiteEntity summoner) {
        summonerId = summoner == null ? null : summoner.getUUID();
    }

    public boolean isSummonedBy(PreeminentParasiteEntity summoner) {
        return summoner != null && summonerId != null && summonerId.equals(summoner.getUUID());
    }

    private PlayState movementAnimation(AnimationState<PreeminentParasiteEntity> state) {
        if (activeKind().flying) {
            return state.setAndContinue(FLY);
        }
        if (attackAnimationTicks > 0) {
            return state.setAndContinue(ATTACK);
        }
        return state.setAndContinue(state.isMoving() ? WALK : IDLE);
    }

    private boolean isStealthKind() {
        return activeKind() == Kind.BOGLE || activeKind() == Kind.WRAITH;
    }

    private void revealStealth() {
        setInvisible(false);
        stealthCooldown = 100;
    }

    private void applyFlyingAura() {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.0D),
                this::isValidParasiteTarget)) {
            target.hurt(damageSources().mobAttack(this),
                    (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.25F);
        }
    }

    private void breakBlocksTowardsTarget(LivingEntity target, Kind activeKind) {
        if (blockBreakCooldown > 0 || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() <= 0.001D) {
            return;
        }
        horizontal = horizontal.normalize();
        BlockPos origin = BlockPos.containing(getX() + horizontal.x * activeKind.blockRange,
                getY() + getBbHeight() * 0.5D, getZ() + horizontal.z * activeKind.blockRange);
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || hardness < 0.0F || hardness > 15.0F) {
                continue;
            }
            if (level().destroyBlock(candidate, true, this)) {
                blockBreakCooldown = 20;
            }
            return;
        }
    }

    private void trySummonSuccor(LivingEntity target) {
        supportCooldown = 200;
        if (!(level() instanceof ServerLevel serverLevel) || random.nextInt(3) != 0) {
            return;
        }
        int existingSuccors = level().getEntitiesOfClass(PreeminentParasiteEntity.class,
                getBoundingBox().inflate(48.0D), entity -> entity.getKind() == Kind.SUCCOR && entity.isSummonedBy(this))
                .size();
        if (existingSuccors > 0) {
            return;
        }
        PreeminentParasiteEntity succor = ModEntities.SUCCOR.get().create(serverLevel);
        if (succor == null) {
            return;
        }
        succor.moveTo(getX(), getY() + getBbHeight() * 0.5D, getZ(), getYRot(), 0.0F);
        succor.setSummoner(this);
        succor.setTarget(target);
        if (isInvisible()) {
            succor.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false), this);
        }
        serverLevel.addFreshEntity(succor);
    }

    private void fireProjectile(LivingEntity target, ParasiteProjectileEntity.Mode mode, double speed,
                                float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.65D));
        projectile.configure(this, mode, start, target.getEyePosition(), speed, damage, radius, lifetime, target);
        level().addFreshEntity(projectile);
    }

    private void spawnHeavyPayload(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Mob payload = switch (random.nextInt(4)) {
            case 0 -> ModEntities.OVERSEER.get().create(serverLevel);
            case 1 -> ModEntities.VIGILANTE.get().create(serverLevel);
            case 2 -> ModEntities.MARAUDER.get().create(serverLevel);
            default -> ModEntities.MONARCH.get().create(serverLevel);
        };
        if (payload == null) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        payload.moveTo(target.getX() + Math.cos(angle) * 2.5D, target.getY(),
                target.getZ() + Math.sin(angle) * 2.5D, getYRot(), 0.0F);
        payload.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(payload.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        payload.setTarget(target);
        serverLevel.addFreshEntity(payload);
    }

    private PreeminentParasiteEntity resolveSummoner() {
        if (summonerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(summonerId);
        return entity instanceof PreeminentParasiteEntity preeminent && preeminent.isAlive() ? preeminent : null;
    }

    private void completeSuccorAction(LivingEntity target) {
        if (succorActionConsumed || level().isClientSide) {
            return;
        }
        succorActionConsumed = true;
        int action = random.nextInt(3);
        PreeminentParasiteEntity summoner = resolveSummoner();
        if (action == 2 && summoner != null) {
            summoner.teleportTo(getX(), getY(), getZ());
            summoner.setDeltaMovement(Vec3.ZERO);
            discard();
            return;
        }
        if (action == 1) {
            AreaEffectCloud orb = new AreaEffectCloud(level(), getX(), getY(), getZ());
            orb.setOwner(this);
            orb.setRadius(4.0F);
            orb.setDuration(80);
            orb.setWaitTime(0);
            orb.setRadiusPerTick(-orb.getRadius() / orb.getDuration());
            orb.addEffect(new MobEffectInstance(MobEffects.HUNGER, 180, 2, false, true));
            orb.addEffect(new MobEffectInstance(ModMobEffects.NEEDLER, 180, 1, false, true));
            level().addFreshEntity(orb);
            discard();
            return;
        }
        Level.ExplosionInteraction interaction = level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), 3.0F, interaction);
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(this);
        cloud.setRadius(4.0F);
        cloud.setDuration(90);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1, false, true));
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1, false, true));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 300, 1, false, true));
        level().addFreshEntity(cloud);
        discard();
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        EntityType<?> type = getType();
        if (type == ModEntities.CARRIER_COLONY.get()) return Kind.CARRIER_COLONY;
        if (type == ModEntities.HAUNTER.get()) return Kind.HAUNTER;
        if (type == ModEntities.BOMBER_HEAVY.get()) return Kind.BOMBER_HEAVY;
        if (type == ModEntities.WRAITH.get()) return Kind.WRAITH;
        if (type == ModEntities.SUCCOR.get()) return Kind.SUCCOR;
        return Kind.BOGLE;
    }

    private final class FlightPursuitGoal extends Goal {
        private final double speed;
        private int contactCooldown;

        private FlightPursuitGoal(double speed) {
            this.speed = speed;
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
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            getMoveControl().setWantedPosition(target.getX(), target.getY() + 3.5D, target.getZ(), speed);
            if (contactCooldown > 0) {
                contactCooldown--;
            } else if (distanceToSqr(target) <= 9.0D) {
                doHurtTarget(target);
                contactCooldown = 20;
            }
        }
    }

    private final class BogleBombGoal extends Goal {
        private int cooldown;

        private BogleBombGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.onGround() && hasLineOfSight(target) && distanceToSqr(target) <= 1600.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                revealStealth();
                fireProjectile(target, ParasiteProjectileEntity.Mode.BOMB, 0.72D, 70.0F, 4.0D, 100);
                cooldown = 100;
            }
        }
    }

    private final class ColonySupportGoal extends Goal {
        private int cooldown;

        private ColonySupportGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            return getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16.0D),
                    entity -> entity instanceof Parasite && entity.isAlive())) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2, false, false),
                        PreeminentParasiteEntity.this);
                ally.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 1200, 1, false, false),
                        PreeminentParasiteEntity.this);
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, false),
                        PreeminentParasiteEntity.this);
            }
            cooldown = 200;
        }
    }

    private final class HaunterHomingBurstGoal extends Goal {
        private int cooldown;
        private int shots;
        private int delay;

        private HaunterHomingBurstGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && hasLineOfSight(target) && distanceToSqr(target) >= 25.0D
                    && distanceToSqr(target) <= 1600.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && shots < 3;
        }

        @Override
        public void start() {
            shots = 0;
            delay = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (delay > 0) {
                delay--;
                return;
            }
            fireProjectile(target, ParasiteProjectileEntity.Mode.LIGHT, 1.30D, 45.0F, 1.25D, 90);
            shots++;
            delay = 6;
        }

        @Override
        public void stop() {
            cooldown = 90;
        }
    }

    private final class HeavyBomberBombGoal extends Goal {
        private int cooldown;

        private HeavyBomberBombGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.onGround() && hasLineOfSight(target) && distanceToSqr(target) <= 2304.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            fireProjectile(target, ParasiteProjectileEntity.Mode.BOMB, 0.62D, 55.0F, 5.0D, 120);
            spawnHeavyPayload(target);
            cooldown = 160;
        }
    }

    private final class WraithNadeBurstGoal extends Goal {
        private int cooldown;
        private int shots;
        private int delay;

        private WraithNadeBurstGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && target.onGround() && hasLineOfSight(target) && distanceToSqr(target) <= 1600.0D;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && shots < 3;
        }

        @Override
        public void start() {
            shots = 0;
            delay = 0;
            revealStealth();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (delay > 0) {
                delay--;
                return;
            }
            fireProjectile(target, ParasiteProjectileEntity.Mode.BOMB, 0.82D, 35.0F, 2.5D, 90);
            shots++;
            delay = 5;
        }

        @Override
        public void stop() {
            cooldown = 100;
        }
    }

    private final class EvasiveDashGoal extends Goal {
        private final int interval;
        private final double speed;
        private int cooldown;

        private EvasiveDashGoal(int interval, double speed) {
            this.interval = interval;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return target != null && onGround() && distanceToSqr(target) >= 9.0D
                    && distanceToSqr(target) <= 625.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            Vec3 toTarget = target.position().subtract(position());
            Vec3 strafe = new Vec3(-toTarget.z, 0.0D, toTarget.x);
            if (strafe.lengthSqr() > 0.001D) {
                strafe = strafe.normalize().scale(random.nextBoolean() ? speed : -speed);
                setDeltaMovement(strafe.x, 0.35D, strafe.z);
            }
            cooldown = interval;
        }
    }

    private final class SuccorActionGoal extends Goal {
        private SuccorActionGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return !succorActionConsumed && target != null && target.isAlive();
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
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            getMoveControl().setWantedPosition(target.getX(), target.getY() + 0.5D, target.getZ(), 1.20D);
            if (distanceToSqr(target) <= 9.0D) {
                completeSuccorAction(target);
            }
        }
    }

    public enum Kind {
        BOGLE(true, 310.0D, 15.5D, 70.0D, 0.28D, 2.0D, 80.0D, 5.0D),
        CARRIER_COLONY(false, 390.0D, 15.5D, 45.0D, 0.242D, 2.0D, 80.0D, 5.0D),
        HAUNTER(false, 360.0D, 15.5D, 110.0D, 0.283D, 2.0D, 80.0D, 5.0D),
        BOMBER_HEAVY(true, 420.0D, 15.5D, 33.0D, 0.25D, 0.15D, 80.0D, 5.0D),
        WRAITH(true, 310.0D, 15.5D, 70.0D, 0.28D, 2.0D, 80.0D, 5.0D),
        SUCCOR(true, 85.0D, 2.0D, 1.0D, 0.32D, 1.0D, 80.0D, 2.0D);

        private final boolean flying;
        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final double movementSpeed;
        private final double knockbackResistance;
        private final double followRange;
        private final double blockRange;

        Kind(boolean flying, double maxHealth, double armor, double attackDamage, double movementSpeed,
             double knockbackResistance, double followRange, double blockRange) {
            this.flying = flying;
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.movementSpeed = movementSpeed;
            this.knockbackResistance = knockbackResistance;
            this.followRange = followRange;
            this.blockRange = blockRange;
        }
    }
}
