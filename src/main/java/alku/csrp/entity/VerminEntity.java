package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumSet;

public final class VerminEntity extends PrimitiveParasiteEntity {
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private int bombCooldown = 160;

    public VerminEntity(EntityType<? extends VerminEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 45.0).add(Attributes.ARMOR, 9.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0).add(Attributes.FLYING_SPEED, 0.25)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new VerminCombatGoal());
    }

    @Override public void tick() {
        super.tick();
        setNoGravity(true);
        if (bombCooldown > 0) bombCooldown--;
    }

    private void dropGnatBomb() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        triggerAnim("attack_controller", "attack");
        GnatEntity gnat = ModEntities.GNAT.get().create(serverLevel, null, blockPosition(), MobSpawnType.MOB_SUMMONED, false, false);
        if (gnat != null) {
            gnat.moveTo(getX(), getY() - 0.5, getZ(), getYRot(), 0.0F);
            gnat.setTarget(getTarget());
            serverLevel.addFreshEntity(gnat);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("attack_controller", "attack");
        }
        return hit;
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state ->
                software.bernie.geckolib.animation.PlayState.STOP).triggerableAnim("attack", ATTACK));
    }

    private final class VerminCombatGoal extends Goal {
        VerminCombatGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return getTarget() != null; }
        @Override public void tick() {
            var target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = distanceToSqr(target);
            if (distance > 9.0) getNavigation().moveTo(target.getX(), target.getY() + 2.0, target.getZ(), 1.1);
            if (bombCooldown == 0 && distance < 256.0) { dropGnatBomb(); bombCooldown = 160; }
            if (distance < 6.25) {
                setDeltaMovement(target.position().subtract(position()).normalize().scale(0.65));
                if (tickCount % 12 == 0) doHurtTarget(target);
            }
        }
    }
}
