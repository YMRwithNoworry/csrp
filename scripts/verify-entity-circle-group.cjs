const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

// ---------------------------------------------------------------------------
// EntityAICircleGroup (out109 entity/ai/misc/EntityAICircleGroup.java, 311 lines)
//
// Sole 1.10.9 user: EntityInfHuman:121
//   tasks.addTask(4, new EntityAICircleGroup(this, 1.15, 8, 4.0, 10.0, 16, e -> e instanceof EntityInfHuman));
//
// Facts:
//   canUse:   !isRiding() && getAttackTarget() == null && !isInWater();
//             snapshot the group within scanRadius x ±8 x scanRadius (self first, sorted by entity id),
//             require size >= minGroup, estimate centre/radius, assign the initial angle
//   canCont:  !isDead, no attack target, group size >= max(2, minGroup - 2)
//   start:    speedMul = 0.65 + 0.35 * hash01(entityId), effSpeed = speed * speedMul
//   tick:     re-estimate centre/radius every 10 ticks
//             smooth centre 0.15, smooth radius 0.2
//             dirSign = (entityId & 1) == 0 ? 1 : -1
//             angularVel = dirSign * (2*PI / 100) * speedMul  (+ per-entity angle jitter)
//             rEff = smoothRadius + 0.8 * sin((tickAge + seed) * 0.06)
//             side = 0.6 * sin((tickAge + seed*3) * 0.09)
//             target lerp 0.35, ground y clamped to ±0.4, waypoint refresh every 8 ticks or >2 blocks
//             yaw approaches atan2(tnz, tnx) - 90 with a 20 degree step
//             move control + look control (30, 30), tangent push 0.03
//             pushApartSlightly: min(0.035, 0.02 / d2) within a 0.6/0.2 box
// ---------------------------------------------------------------------------
const human = read("src/main/java/alku/csrp/entity/SimHumanEntity.java");
expect(human, /private final class CircleGroupGoal extends Goal/, "CircleGroupGoal is missing");
expect(human, /goalSelector\.addGoal\(4, new CircleGroupGoal\(\)\)/,
  "the circle group goal is not mounted at the original priority 4");
expect(human, /private static final double SPEED = 1\.15D;/, "the circle speed is not the original 1.15");
expect(human, /private static final int MIN_GROUP = 8;/, "the minimum group size is not the original 8");
expect(human, /private static final double MIN_RADIUS = 4\.0D;/, "the minimum radius is not the original 4.0");
expect(human, /private static final double MAX_RADIUS = 10\.0D;/, "the maximum radius is not the original 10.0");
expect(human, /private static final int SCAN_RADIUS = 16;/, "the scan radius is not the original 16");
expect(human, /private static final int RECALC_CENTER_EVERY = 10;/, "the centre is not re-estimated every 10 ticks");
expect(human, /private static final int RECALC_WAYPOINT_EVERY = 8;/, "the waypoint is not refreshed every 8 ticks");
expect(human, /private static final float LAP_TICKS_BASE = 100\.0F;/, "the lap base is not the original 100 ticks");
expect(human, /private static final float WOBBLE_AMPLITUDE = 0\.8F;/, "the wobble amplitude is not the original 0.8");
expect(human, /private static final float WANDER_AMPLITUDE = 0\.6F;/, "the wander amplitude is not the original 0.6");
expect(human, /private static final double TANGENT_PUSH = 0\.03D;/, "the tangent push is not the original 0.03");
expect(human, /isPassenger\(\) \|\| getTarget\(\) != null \|\| isInWater\(\)/,
  "the circle group conditions are not the original ones");
expect(human, /group\.size\(\) < MIN_GROUP/, "the minimum group gate is missing");
expect(human, /group\.size\(\) >= Math\.max\(2, MIN_GROUP - 2\)/,
  "the continue condition is not max(2, minGroup - 2)");
expect(human, /0\.65F \+ 0\.35F \* unit/, "the per-entity speed multiplier is not the original 0.65 + 0.35 * u");
expect(human, /smoothCenterX \+= \(centerX - smoothCenterX\) \* 0\.15D/,
  "the centre smoothing is not the original 0.15");
expect(human, /smoothRadius \+= \(radius - smoothRadius\) \* 0\.2D/,
  "the radius smoothing is not the original 0.2");
expect(human, /\(getId\(\) & 1\) == 0 \? 1 : -1/, "the ring direction is not taken from the id parity");
expect(human, /Math\.PI \* 2\.0D \/ LAP_TICKS_BASE/, "the angular velocity is not 2*PI/100");
expect(human, /targetX \+= \(rawX - targetX\) \* 0\.35D;/, "the target smoothing is not the original 0.35");
expect(human, /Mth\.clamp\(rawY - targetY, -0\.4D, 0\.4D\)/, "the vertical step is not clamped to ±0.4");
expect(human, /dx \* dx \+ dz \* dz > 4\.0D/, "the 2 block waypoint drift rule is missing");
expect(human, /approachAngle\(smoothYaw, targetYaw, 20\.0F\)/, "the yaw approach step is not the original 20 degrees");
expect(human, /getMoveControl\(\)\.setWantedPosition\(targetX, targetY, targetZ, effectiveSpeed\)/,
  "the move control is not driven");
expect(human, /getLookControl\(\)\.setLookAt\(targetX, targetY \+ getEyeHeight\(\), targetZ, 30\.0F, 30\.0F\)/,
  "the look control is not the original (30, 30)");
expect(human, /Math\.min\(0\.035D, 0\.02D \/ distanceSqr\)/, "the separation push is not min(0.035, 0.02/d2)");
expect(human, /getHeightmapPos\(Heightmap\.Types\.MOTION_BLOCKING/, "the ground height lookup is missing");
expect(human, /Math\.abs\(top\.getY\(\) - fallbackY\) > 6\.0D \? fallbackY : top\.getY\(\) \+ 0\.2D/,
  "the ground fallback rule is not the original 6 block rule");
expect(human, /group\.sort\(Comparator\.comparingInt\(SimHumanEntity::getId\)\)/,
  "the group is not ordered by entity id as in the original");

if (failures.length) {
  console.error(`verify-entity-circle-group: ${failures.length} failure(s)`);
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}
console.log("verify-entity-circle-group: ok");
