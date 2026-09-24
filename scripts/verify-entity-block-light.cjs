const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const read = (relative) => fs.readFileSync(path.join(root, relative), "utf8");
const failures = [];
const expect = (text, pattern, message) => {
  if (!pattern.test(text)) failures.push(message);
};

// ---------------------------------------------------------------------------
// EntityAIBlockLight (out109 entity/ai/EntityAIBlockLight.java, 211 lines)
//
// Mount point: out109 entity/ai/misc/EntityPMalleable.java:92
//   this.field_70714_bg.func_75776_a(7, new EntityAIBlockLight(this, 20, 5));
// Gate: EntityPMalleable.getGeneMod(7) == geneBlocksearch (EntityPMalleable:68, default true).
//
// Facts copied into the port:
//   canUse:   ticks++ ; require getGeneMod(7) ; ticks < 40 -> false ; then reset ticks,
//             require mobGriefing and parent.getTarget() == null, then findSource() != null
//   canCont:  target != null && same block && parent.getTarget() == null
//   start:    neededTime = (int)(state.getBlockHardness * max(0, 10.0F))
//   tick:     r = 5.0 ; distance > r -> navigation.moveTo(target, 1.1)
//             idle == 120 -> parent.skillBreakBlocks()
//             idle >= 240 -> blacklist the block, stop, ticks += 30
//             distance <= r -> moveTo(target, 0.0), progressB++,
//                              world.sendBlockBreakProgress(parent.id, target, progressB/neededTime)
//                              progressB >= neededTime -> world.destroyBlock(target, true), ticks += 30
//   findSource: skip when block light < 5 and random.nextInt(3) != 0;
//             scan x,z in [-20,20], y in [-4, parent.height];
//             keep blocks with getLightValue() >= 5, skipping liquids, portals, end gateways,
//             end portal frames, mod base blocks and fire
// ---------------------------------------------------------------------------
const base = read("src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java");
expect(base, /protected final class LightSourceBreakingGoal extends Goal/, "LightSourceBreakingGoal is missing");
expect(base, /private static final int SCAN_RANGE = 20;/, "the light scan range is not the original 20");
expect(base, /private static final int LIGHT_TRIGGER = 5;/, "the light trigger is not the original 5");
expect(base, /private static final int RESCAN_TICKS = 40;/, "the rescan gate is not the original 40 ticks");
expect(base, /private static final double REACH = 5\.0D;/, "the reach is not the original 5 blocks");
expect(base, /private static final int UNREACHABLE_TICKS = 120;/, "the 120 tick skill trigger is missing");
expect(base, /private static final int GIVE_UP_TICKS = 240;/, "the 240 tick give-up is missing");
expect(base, /private static final float HARDNESS_MULTIPLIER = 10\.0F;/, "the hardness multiplier is not the original 10");
expect(base, /serverLevel\.getGameRules\(\)\.get\(GameRules\.MOB_GRIEFING\)/,
  "the light breaking is not gated on mobGriefing");
expect(base, /getTarget\(\) != null \|\| !\(level\(\) instanceof ServerLevel/,
  "the light breaking is not gated on having no attack target");
expect(base, /getMaxLocalRawBrightness\(origin\) < LIGHT_TRIGGER && random\.nextInt\(3\) != 0/,
  "the original block-light / 1-in-3 early-out is missing");
expect(base, /for \(int y = -4; y <= maxY; y\+\+\)/, "the vertical scan bound (-4 .. height) is missing");
expect(base, /state\.getLightEmission\(\) < LIGHT_TRIGGER/, "blocks below the light trigger are not skipped");
expect(base, /unreachable\.contains\(candidate\)/, "blacklisted blocks are not skipped");
expect(base, /getNavigation\(\)\.moveTo\(target\.getX\(\), target\.getY\(\), target\.getZ\(\), 1\.1D\)/,
  "the approach speed is not the original 1.1");
expect(base, /serverLevel\.destroyBlockProgress\(getId\(\), target, \(int\) \(\(float\) progress \/ neededTime \* 10\.0F\)\)/,
  "the break progress is not synced to the client");
expect(base, /progress >= neededTime[\s\S]{0,300}?serverLevel\.destroyBlock\(target, RuntimeToggles\.parasiteBlockDrops\(\), PrimitiveParasiteEntity\.this\)/,
  "the final break does not honour SRPConfig.doTileDrops");
expect(base, /idle >= GIVE_UP_TICKS[\s\S]{0,200}?unreachable\.add\(target\.immutable\(\)\)/,
  "unreachable light sources are not blacklisted");
expect(base, /goalSelector\.addGoal\(7, new LightSourceBreakingGoal\(\)\)/,
  "the goal is not mounted at the original priority 7");
expect(base, /protected boolean supportsLightSourceBreaking\(\) \{\s*return false;\s*\}/,
  "the gene 7 substitute hook is missing");

// EntityPMalleable gene 7 (geneBlocksearch, default true) covers the malleable families.
for (const file of ["PureParasiteEntity", "AdaptedVariantEntity", "PrimitiveVariantEntity", "PreeminentParasiteEntity"]) {
  const text = read(`src/main/java/alku/csrp/entity/${file}.java`);
  expect(text, /protected boolean supportsLightSourceBreaking\(\) \{\s*return true;\s*\}/,
    `${file} does not enable the malleable light-source breaking behaviour`);
}

if (failures.length) {
  console.error(`verify-entity-block-light: ${failures.length} failure(s)`);
  for (const failure of failures) console.error(` - ${failure}`);
  process.exit(1);
}
console.log("verify-entity-block-light: ok");
