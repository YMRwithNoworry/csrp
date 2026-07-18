package alku.csrp.entity;

import alku.csrp.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Legacy Marauderized bear: fires a pullball volley, then reels its captured victim in. */
public final class MarauderizedBearEntity extends TetheredMarauderizedEntity {
    private static final int VOLLEY_SHOTS = 7;
    private static final int VOLLEY_INTERVAL_TICKS = 20;
    private static final int VOLLEY_COOLDOWN_TICKS = 300;

    private int volleyShots;
    private int volleyDelay;

    public MarauderizedBearEntity(EntityType<? extends MarauderizedBearEntity> type, Level level) {
        super(type, level, 12);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMarauderizedAttributes(38.0D, 8.0D, 15.0D, 0.8D, 0.25D, 64.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new PullVolleyGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            tickTether();
        }
    }

    private void shootPullingBall(LivingEntity target) {
        PullingBallEntity ball = ModEntities.PULLING_BALL.get().create(level());
        if (ball == null) {
            return;
        }
        Vec3 start = getEyePosition().add(getViewVector(1.0F).scale(0.35D));
        Vec3 direction = target.getEyePosition().subtract(start);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }
        ball.setOwner(this);
        ball.setPos(start);
        ball.setDeltaMovement(direction.normalize().scale(0.35D));
        level().addFreshEntity(ball);
    }

    private final class PullVolleyGoal extends Goal {
        private int cooldown;

        private PullVolleyGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            LivingEntity target = getTarget();
            return !hasPullTarget() && target != null && target.isAlive() && hasLineOfSight(target)
                    && distanceToSqr(target) >= 9.0D && distanceToSqr(target) <= 64.0D * 64.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return volleyShots > 0 && getTarget() != null && getTarget().isAlive() && !hasPullTarget();
        }

        @Override
        public void start() {
            volleyShots = VOLLEY_SHOTS;
            volleyDelay = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || volleyDelay-- > 0) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            shootPullingBall(target);
            volleyShots--;
            volleyDelay = VOLLEY_INTERVAL_TICKS;
        }

        @Override
        public void stop() {
            volleyShots = 0;
            cooldown = VOLLEY_COOLDOWN_TICKS;
        }
    }
}
