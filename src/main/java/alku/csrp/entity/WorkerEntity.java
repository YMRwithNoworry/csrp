package alku.csrp.entity;

import alku.csrp.block.SrpCoreBlock;
import alku.csrp.registry.ModBlocks;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

/** Legacy colony worker (EntityKol), including its 13/26-block construction grid. */
public final class WorkerEntity extends PrimitiveParasiteEntity {
    @Override
    public boolean supportsDamageAdaptation() {
        return false;
    }
    private static final int BUILD_INTERVAL = 200;
    private static final int SEARCH_RANGE = 25;
    private static final int DEFENCE_GRID = 13;
    private static final int BUILDING_GRID = 26;

    private final RawAnimation idleAnimation = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation walkAnimation = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation buildAnimation = ParasiteAnimations.play(this, "attack");

    private BlockPos colonyOrigin;
    private int colonyRadius;
    private int buildCooldown = BUILD_INTERVAL;

    public WorkerEntity(EntityType<? extends WorkerEntity> type, Level level) {
        super(type, level);
        xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new BuildColonyGoal());
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && colonyOrigin == null && tickCount % 10 == 0 && random.nextInt(7) == 0
                && level() instanceof ServerLevel serverLevel) {
            SrpWorldData.ColonyEntry colony = SrpWorldData.get(serverLevel)
                    .nearestColonyInConstructionRange(blockPosition());
            if (colony != null) {
                setColonyTask(colony.pos(), colonyRadius(colony));
            }
        }
    }

    public void setColonyTask(BlockPos origin, int radius) {
        colonyOrigin = origin.immutable();
        colonyRadius = Math.max(1, radius);
        buildCooldown = Math.min(buildCooldown, BUILD_INTERVAL);
    }

    public static int colonyRadius(SrpWorldData.ColonyEntry colony) {
        return SrpWorldData.colonyConstructionRadius(colony.points());
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        return distance > 50.0F && super.causeFallDamage(distance, damageMultiplier, source);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", buildAnimation));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (colonyOrigin != null) {
            tag.putLong("parasite_origin", colonyOrigin.asLong());
            tag.putInt("parasite_build_radius", colonyRadius);
        }
        tag.putInt("parasite_build_cooldown", buildCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("parasite_origin")) {
            colonyOrigin = BlockPos.of(tag.getLong("parasite_origin"));
            colonyRadius = Math.max(1, tag.getInt("parasite_build_radius"));
        }
        buildCooldown = tag.contains("parasite_build_cooldown")
                ? Math.max(0, tag.getInt("parasite_build_cooldown")) : BUILD_INTERVAL;
    }

    private PlayState movementAnimation(AnimationState<WorkerEntity> state) {
        return state.setAndContinue(getDeltaMovement().horizontalDistanceSqr() >= 0.001 ? walkAnimation : idleAnimation);
    }

    private boolean placeNextStructure() {
        if (colonyOrigin == null || !(level() instanceof ServerLevel serverLevel)
                || !serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }
        BlockPos current = blockPosition();
        for (int x = current.getX() - SEARCH_RANGE; x <= current.getX() + SEARCH_RANGE; x++) {
            for (int z = current.getZ() - SEARCH_RANGE; z <= current.getZ() + SEARCH_RANGE; z++) {
                int stage = structureStage(x, z);
                if (stage == 0 || colonyOrigin.distSqr(new BlockPos(x, colonyOrigin.getY(), z))
                        > (double) colonyRadius * colonyRadius) {
                    continue;
                }
                int surfaceY = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos placement = new BlockPos(x, surfaceY, z);
                if (!serverLevel.getBlockState(placement).canBeReplaced() || hasNearbyColonyStructure(placement)) {
                    continue;
                }
                serverLevel.setBlockAndUpdate(placement, ModBlocks.PARASITE_STRUCTURE.get().defaultBlockState()
                        .setValue(SrpCoreBlock.ACTIVE, stage));
                triggerAnim("attack_controller", "attack");
                return true;
            }
        }
        return false;
    }

    private boolean hasNearbyColonyStructure(BlockPos placement) {
        for (int y = placement.getY() - 30; y <= placement.getY() + 30; y++) {
            BlockPos check = new BlockPos(placement.getX(), y, placement.getZ());
            if (level().getBlockState(check).is(ModBlocks.PARASITE_STRUCTURE)
                    || level().getBlockState(check).is(ModBlocks.COLONYHEART)
                    || level().getBlockState(check).is(ModBlocks.BIOMEHEART)) {
                return true;
            }
        }
        return false;
    }

    private static int structureStage(int x, int z) {
        if (Math.floorMod(x, DEFENCE_GRID) == 0 && Math.floorMod(z, DEFENCE_GRID) == 0
                && (Math.floorMod(x, BUILDING_GRID) != 0 || Math.floorMod(z, BUILDING_GRID) != 0)) {
            return 2;
        }
        return Math.floorMod(x, BUILDING_GRID) == 0 && Math.floorMod(z, BUILDING_GRID) == 0 ? 1 : 0;
    }

    private final class BuildColonyGoal extends Goal {
        private BuildColonyGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (colonyOrigin == null || buildCooldown-- > 0) {
                return false;
            }
            buildCooldown = BUILD_INTERVAL;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            placeNextStructure();
        }
    }
}
