package alku.csrp.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

/**
 * Legacy Cruxa: a heavy crude parasite that sweeps groups, hurls nearby blocks, and grows stronger from kills.
 */
public final class CruxEntity extends CrudeParasiteEntity {
    private static final double BASE_ATTACK_DAMAGE = 20.0;
    private static final int DAMAGE_STACK_CAP = 10;
    private static final double DAMAGE_GAIN_PER_KILL = 0.12;
    private static final String DAMAGE_STACKS_TAG = "crux_damage_stacks";
    private static final RawAnimation ANIMATION = RawAnimation.begin().thenLoop("animation");

    private int attackCooldown;
    private int throwCooldown;
    private int damageStacks;

    public CruxEntity(EntityType<? extends CruxEntity> type, Level level) {
        super(type, level);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 70.0).add(Attributes.ARMOR, 9.0)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new BlockThrowGoal());
        goalSelector.addGoal(2, new CruxMeleeGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            if (throwCooldown > 0) {
                throwCooldown--;
            }
        }
    }

    @Override
    protected void onParasiteKill(net.minecraft.server.level.ServerLevel level, LivingEntity victim, int kills) {
        if (damageStacks >= DAMAGE_STACK_CAP) {
            return;
        }

        damageStacks++;
        AttributeInstance attackDamage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(BASE_ATTACK_DAMAGE * (1.0 + DAMAGE_GAIN_PER_KILL * damageStacks));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(DAMAGE_STACKS_TAG, damageStacks);
        tag.putInt("crux_throw_cooldown", throwCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(DAMAGE_STACKS_TAG, Tag.TAG_INT)) {
            damageStacks = Math.min(DAMAGE_STACK_CAP, Math.max(0, tag.getInt(DAMAGE_STACKS_TAG)));
            AttributeInstance attackDamage = getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                attackDamage.setBaseValue(BASE_ATTACK_DAMAGE * (1.0 + DAMAGE_GAIN_PER_KILL * damageStacks));
            }
        }
        if (tag.contains("crux_throw_cooldown", Tag.TAG_INT)) {
            throwCooldown = Math.max(0, tag.getInt("crux_throw_cooldown"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(ANIMATION)));
    }

    private boolean performAoeAttack(LivingEntity target) {
        AABB attackArea = target.getBoundingBox().inflate(1.0);
        boolean hit = false;
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, attackArea, this::isValidParasiteTarget)) {
            hit |= victim.hurt(damageSources().mobAttack(this), damage);
        }
        attackCooldown = 20;
        return hit;
    }

    private boolean throwBlockAt(LivingEntity target) {
        if (level().isClientSide || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }

        BlockPos source = findThrowableBlock();
        if (source == null) {
            return false;
        }

        BlockState state = level().getBlockState(source);
        float hardness = state.getDestroySpeed(level(), source);
        FallingBlockEntity block = FallingBlockEntity.fall(level(), source, state);
        block.disableDrop();
        block.setHurtsEntities(10.0F + Math.max(0.0F, hardness), 40);

        Vec3 toTarget = target.getEyePosition().subtract(block.position());
        double horizontalDistance = Math.max(0.001, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z));
        double verticalVelocity = Math.min(0.85, Math.max(-0.15,
                toTarget.y * 0.05 + 0.22 + horizontalDistance * 0.016));
        block.setDeltaMovement(toTarget.x / horizontalDistance * 0.75, verticalVelocity,
                toTarget.z / horizontalDistance * 0.75);
        return true;
    }

    private BlockPos findThrowableBlock() {
        BlockPos origin = blockPosition();
        for (int attempt = 0; attempt < 8; attempt++) {
            int offsetX = random.nextInt(5) - 2;
            int offsetZ = random.nextInt(5) - 2;
            if (offsetX == 0 && offsetZ == 0) {
                continue;
            }
            for (int offsetY = 2; offsetY >= -4; offsetY--) {
                BlockPos candidate = origin.offset(offsetX, offsetY, offsetZ);
                BlockState state = level().getBlockState(candidate);
                float hardness = state.getDestroySpeed(level(), candidate);
                if (!state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                        && hardness > 0.0F && hardness <= 50.0F) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private final class CruxMeleeGoal extends Goal {
        private CruxMeleeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            getNavigation().moveTo(target, 1.3);
            double reach = getBbWidth() + target.getBbWidth() + 1.0;
            if (attackCooldown == 0 && distanceToSqr(target) <= reach * reach) {
                performAoeAttack(target);
            }
        }
    }

    private final class BlockThrowGoal extends Goal {
        private int windupTicks;

        private BlockThrowGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || throwCooldown > 0) {
                return false;
            }
            double verticalOffset = target.getY() - getY();
            double distance = distanceToSqr(target);
            return distance >= 144.0 && distance <= 1024.0 && verticalOffset >= 0.0 && verticalOffset <= 3.0;
        }

        @Override
        public boolean canContinueToUse() {
            return windupTicks < 20 && getTarget() != null;
        }

        @Override
        public void start() {
            windupTicks = 0;
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (++windupTicks == 12) {
                throwBlockAt(target);
            }
        }

        @Override
        public void stop() {
            throwCooldown = 100;
        }
    }
}
