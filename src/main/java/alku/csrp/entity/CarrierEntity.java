package alku.csrp.entity;

import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

/** Shared detonation, residue, and toxic-cloud behavior of the original carrier parasites. */
public abstract class CarrierEntity extends PrimitiveParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private static final float LOW_HEALTH_FUSE_THRESHOLD = 0.05F;
    private static final String FUSE_TICKS_TAG = "carrier_fuse_ticks";

    private final int fuseTime;
    private final int residueRadius;
    private final double viralRadius;
    private final int viralAmplifier;
    private final int cloudDuration;
    private int fuseTicks = -1;
    private boolean detonated;

    protected CarrierEntity(EntityType<? extends CarrierEntity> type, Level level, int fuseTime, int residueRadius,
            double viralRadius, int viralAmplifier, int cloudDuration) {
        super(type, level);
        this.fuseTime = fuseTime;
        this.residueRadius = residueRadius;
        this.viralRadius = viralRadius;
        this.viralAmplifier = viralAmplifier;
        this.cloudDuration = cloudDuration;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new SwellGoal());
        if (usesMeleeAttack()) {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        }
    }

    protected boolean usesMeleeAttack() {
        return true;
    }

    protected abstract RawAnimation idleAnimation();

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(idleAnimation())));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || detonated || !isAlive()) {
            return;
        }

        if (getHealth() < getMaxHealth() * LOW_HEALTH_FUSE_THRESHOLD) {
            startFuse();
        }
        if (fuseTicks >= 0 && ++fuseTicks >= fuseTime) {
            detonate();
        }
    }

    protected final void startFuse() {
        if (fuseTicks < 0) {
            fuseTicks = 0;
            getNavigation().stop();
        }
    }

    public final boolean isDetonating() {
        return fuseTicks >= 0 && !detonated;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(FUSE_TICKS_TAG, fuseTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(FUSE_TICKS_TAG, Tag.TAG_INT)) {
            fuseTicks = tag.getInt(FUSE_TICKS_TAG);
        }
    }

    private void detonate() {
        if (!(level() instanceof ServerLevel) || detonated) {
            return;
        }

        detonated = true;
        Level.ExplosionInteraction interaction = level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        level().explode(this, getX(), getY(), getZ(), 4.0F, interaction);
        applyExplosionEffects();
        spreadResidue();
        spawnLingeringCloud();
        spawnGnats();
        super.die(damageSources().mobAttack(this));
        discard();
    }

    protected int gnatSpawnCount() {
        return 0;
    }

    private void spawnGnats() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int index = 0; index < gnatSpawnCount(); index++) {
            GnatEntity gnat = ModEntities.GNAT.get().create(serverLevel, null, blockPosition(),
                    MobSpawnType.MOB_SUMMONED, false, false);
            if (gnat == null) {
                continue;
            }
            gnat.moveTo(getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + 0.2D, getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    random.nextFloat() * 360.0F, 0.0F);
            gnat.setDeltaMovement((random.nextDouble() - 0.5D) * 0.35D,
                    0.2D + random.nextDouble() * 0.2D,
                    (random.nextDouble() - 0.5D) * 0.35D);
            gnat.setTarget(getTarget());
            serverLevel.addFreshEntity(gnat);
        }
    }

    private void applyExplosionEffects() {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(viralRadius), this::isValidParasiteTarget)) {
            target.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 400, viralAmplifier), this);
        }
    }

    private void spreadResidue() {
        if (residueRadius <= 0) {
            return;
        }

        BlockPos origin = blockPosition();
        for (int x = -residueRadius; x <= residueRadius; x++) {
            for (int z = -residueRadius; z <= residueRadius; z++) {
                if (random.nextBoolean()) {
                    placeResidueAtFloor(origin.offset(x, 3, z));
                }
            }
        }
    }

    private void placeResidueAtFloor(BlockPos start) {
        for (int y = 0; y <= 6; y++) {
            BlockPos candidate = start.below(y);
            if (level().isEmptyBlock(candidate) && !level().isEmptyBlock(candidate.below())) {
                level().setBlock(candidate, ModBlocks.INFESTED_REMAINS.get().defaultBlockState(), 3);
                return;
            }
        }
    }

    private void spawnLingeringCloud() {
        AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
        float radius = getBbWidth() * 3.5F;
        cloud.setOwner(this);
        cloud.setRadius(radius);
        cloud.setWaitTime(10);
        cloud.setDuration(cloudDuration);
        cloud.setRadiusPerTick(-radius / cloudDuration);
        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 0));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.COTH, 3600, 0, false, false));
        cloud.addEffect(new MobEffectInstance(ModMobEffects.VIRAL, 3600, 0, false, false));
        level().addFreshEntity(cloud);
    }

    private final class SwellGoal extends Goal {
        private SwellGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && distanceToSqr(target) < 9.0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && distanceToSqr(target) < 49.0 && !detonated;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            startFuse();
        }
    }
}
