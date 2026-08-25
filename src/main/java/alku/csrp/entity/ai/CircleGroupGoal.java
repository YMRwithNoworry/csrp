package alku.csrp.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/** Keeps an idle group moving around a shared, smoothly updated center. */
public final class CircleGroupGoal extends Goal {
    private static final int RECALCULATE_CENTER_INTERVAL = 10;
    private static final int RECALCULATE_WAYPOINT_INTERVAL = 8;
    private static final float LAP_TICKS = 100.0F;

    private final PathfinderMob mob;
    private final double speed;
    private final int minimumGroupSize;
    private final double minimumRadius;
    private final double maximumRadius;
    private final int scanRadius;
    private final Predicate<Entity> sameGroup;
    private final List<PathfinderMob> group = new ArrayList<>();

    private int age;
    private int centerTicker;
    private int waypointTicker;
    private int direction;
    private float seed;
    private float speedMultiplier;
    private float angle;
    private float smoothedYaw;
    private double effectiveSpeed;
    private double centerX;
    private double centerZ;
    private double radius;
    private double smoothedCenterX;
    private double smoothedCenterZ;
    private double smoothedRadius;
    private double targetX;
    private double targetY;
    private double targetZ;

    public CircleGroupGoal(PathfinderMob mob, double speed, int minimumGroupSize,
                           double minimumRadius, double maximumRadius, int scanRadius,
                           Predicate<Entity> sameGroup) {
        this.mob = mob;
        this.speed = speed;
        this.minimumGroupSize = minimumGroupSize;
        this.minimumRadius = minimumRadius;
        this.maximumRadius = maximumRadius;
        this.scanRadius = scanRadius;
        this.sameGroup = sameGroup;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (mob.isPassenger() || mob.getTarget() != null || mob.isInWaterOrBubble()) {
            return false;
        }
        snapshotGroup();
        if (group.size() < minimumGroupSize) {
            return false;
        }
        estimateCenterAndRadius();
        assignInitialAngle();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!mob.isAlive() || mob.getTarget() != null || mob.isInWaterOrBubble()) {
            return false;
        }
        snapshotGroup();
        return group.size() >= Math.max(2, minimumGroupSize - 2);
    }

    @Override
    public void start() {
        age = 0;
        seed = mob.getId() % 997 * 0.73F;
        int mixedId = mob.getId() * 1_103_515_245 + 12_345;
        float unit = ((mixedId ^ mixedId >>> 16) & Integer.MAX_VALUE) / (float) Integer.MAX_VALUE;
        speedMultiplier = 0.65F + 0.35F * unit;
        effectiveSpeed = speed * speedMultiplier;
        smoothedCenterX = centerX;
        smoothedCenterZ = centerZ;
        smoothedRadius = radius;
        targetX = mob.getX();
        targetY = mob.getY();
        targetZ = mob.getZ();
        smoothedYaw = mob.getYRot();
        centerTicker = 0;
        waypointTicker = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        age++;
        if (++centerTicker >= RECALCULATE_CENTER_INTERVAL) {
            centerTicker = 0;
            snapshotGroup();
            estimateCenterAndRadius();
        }

        smoothedCenterX += (centerX - smoothedCenterX) * 0.15D;
        smoothedCenterZ += (centerZ - smoothedCenterZ) * 0.15D;
        smoothedRadius += (radius - smoothedRadius) * 0.20D;
        direction = (mob.getId() & 1) == 0 ? 1 : -1;
        float angularVelocity = direction * (Mth.TWO_PI / LAP_TICKS) * speedMultiplier;
        float jitter = (float) Math.toRadians(0.6D)
                * (0.5F * Mth.sin((age + seed) * 0.07F)
                + 0.5F * Mth.cos((age * 0.73F + seed) * 0.049F));
        angle = normalizeRadians(angle + angularVelocity + jitter);

        double effectiveRadius = smoothedRadius + 0.8D * Mth.sin((age + seed) * 0.06F);
        double normalX = Mth.cos(angle);
        double normalZ = Mth.sin(angle);
        double tangentX = -Mth.sin(angle) * direction;
        double tangentZ = Mth.cos(angle) * direction;
        double sideOffset = 0.6D * Mth.sin((age + seed * 3.0F) * 0.09F);
        double rawX = smoothedCenterX + normalX * effectiveRadius + tangentX * sideOffset;
        double rawZ = smoothedCenterZ + normalZ * effectiveRadius + tangentZ * sideOffset;
        double rawY = findGroundY(rawX, rawZ, mob.getY());

        targetX += (rawX - targetX) * 0.35D;
        targetZ += (rawZ - targetZ) * 0.35D;
        targetY += Mth.clamp(rawY - targetY, -0.4D, 0.4D);
        double distanceX = targetX - mob.getX();
        double distanceZ = targetZ - mob.getZ();
        if (distanceX * distanceX + distanceZ * distanceZ > 4.0D
                || ++waypointTicker >= RECALCULATE_WAYPOINT_INTERVAL) {
            waypointTicker = 0;
            mob.getNavigation().moveTo(targetX, targetY, targetZ, effectiveSpeed);
        }

        float targetYaw = (float) (Mth.atan2(tangentZ, tangentX) * Mth.RAD_TO_DEG) - 90.0F;
        smoothedYaw = Mth.approachDegrees(smoothedYaw, targetYaw, 20.0F);
        mob.setYRot(smoothedYaw);
        mob.yHeadRot = smoothedYaw;
        mob.yBodyRot = smoothedYaw;
        mob.getMoveControl().setWantedPosition(targetX, targetY, targetZ, effectiveSpeed);
        mob.setDeltaMovement(mob.getDeltaMovement().add(tangentX * 0.03D, 0.0D, tangentZ * 0.03D));
        mob.getLookControl().setLookAt(targetX, targetY + mob.getEyeHeight(), targetZ, 30.0F, 30.0F);
        pushApart(tangentX, tangentZ);
    }

    private void snapshotGroup() {
        group.clear();
        group.add(mob);
        AABB area = mob.getBoundingBox().inflate(scanRadius, 8.0D, scanRadius);
        for (Entity entity : mob.level().getEntities(mob, area, sameGroup)) {
            if (entity instanceof PathfinderMob groupMember && groupMember.isAlive()) {
                group.add(groupMember);
            }
        }
        group.sort(Comparator.comparingInt(Entity::getId));
    }

    private void estimateCenterAndRadius() {
        if (group.isEmpty()) {
            centerX = mob.getX();
            centerZ = mob.getZ();
            radius = Mth.clamp(3.0D, minimumRadius, maximumRadius);
            return;
        }
        double totalX = 0.0D;
        double totalZ = 0.0D;
        for (PathfinderMob groupMember : group) {
            totalX += groupMember.getX();
            totalZ += groupMember.getZ();
        }
        centerX = totalX / group.size();
        centerZ = totalZ / group.size();
        double averageRadius = 0.0D;
        for (PathfinderMob groupMember : group) {
            double x = groupMember.getX() - centerX;
            double z = groupMember.getZ() - centerZ;
            averageRadius += Math.sqrt(x * x + z * z);
        }
        averageRadius /= group.size();
        if (averageRadius < minimumRadius * 0.6D) {
            averageRadius = Math.max(minimumRadius,
                    Math.min(maximumRadius, 1.2D * Math.sqrt(group.size())));
        }
        radius = Mth.clamp(averageRadius, minimumRadius, maximumRadius);
    }

    private void assignInitialAngle() {
        int index = Math.max(0, group.indexOf(mob));
        angle = normalizeRadians(index * Mth.TWO_PI / Math.max(1, group.size()));
    }

    private static float normalizeRadians(float value) {
        while (value < -Mth.PI) value += Mth.TWO_PI;
        while (value > Mth.PI) value -= Mth.TWO_PI;
        return value;
    }

    private double findGroundY(double x, double z, double fallbackY) {
        int groundY = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(x), Mth.floor(z));
        return Math.abs(groundY - fallbackY) > 6.0D ? fallbackY : groundY + 0.2D;
    }

    private void pushApart(double tangentX, double tangentZ) {
        for (Entity entity : mob.level().getEntities(mob, mob.getBoundingBox().inflate(0.6D, 0.2D, 0.6D))) {
            if (!sameGroup.test(entity)) {
                continue;
            }
            double x = mob.getX() - entity.getX();
            double z = mob.getZ() - entity.getZ();
            double distance = x * x + z * z + 0.001D;
            double strength = Math.min(0.035D, 0.02D / distance);
            mob.setDeltaMovement(mob.getDeltaMovement().add(
                    (x * 0.5D + tangentX * 0.5D) * strength,
                    0.0D,
                    (z * 0.5D + tangentZ * 0.5D) * strength));
        }
    }
}
