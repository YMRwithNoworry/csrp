const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const source = fs.readFileSync(path.join(root, "src/main/java/alku/csrp/item/InfestedBonemealItem.java"), "utf8");
const gameTest = fs.readFileSync(path.join(root, "src/main/java/alku/csrp/gametest/InfestedBonemealGameTests.java"), "utf8");
const failures = [];

const requireFragment = (text, fragment, message) => {
  if (!text.includes(fragment)) failures.push(message);
};

requireFragment(source, "SPHERE_RADIUS = 3", "the original radius 3 is missing");
requireFragment(source, "SPHERE_RADIUS_SQ = 9", "the original squared-distance cutoff is missing");
requireFragment(source, "dx * dx + dy * dy + dz * dz > SPHERE_RADIUS_SQ", "conversion is not restricted to the original sphere");
requireFragment(source, "level.getBlockEntity(target) != null", "block entities are not excluded");
requireFragment(source, "level.getBlockTicks().hasScheduledTick(target, state.getBlock())", "scheduled-tick blocks are not excluded");
requireFragment(source, "return !level.getFluidState(pos).isEmpty();", "all non-empty fluids are not excluded");
requireFragment(source, "path.contains(\"cobblestone\")", "original cobblestone path matching is missing");
requireFragment(source, "path.contains(\"farmland\")", "original farmland path matching is missing");
requireFragment(source, "path.contains(\"flower\")", "original flower path matching is missing");
requireFragment(source, "path.contains(\"stonebrick\") || path.contains(\"stone_brick\")", "original stone-brick path matching is missing");
requireFragment(source, "path.endsWith(\"_wall\") || path.contains(\"wall\")", "original wall path matching is missing");
requireFragment(source, "path.endsWith(\"_fence\") || path.contains(\"fence\")", "original fence path matching is missing");
requireFragment(source, "path.contains(\"glass\") && !path.contains(\"pane\")", "original glass path matching is missing");
requireFragment(source, "path.contains(\"carpet\")", "original carpet path matching is missing");
requireFragment(source, "block == Blocks.SAND || block == Blocks.RED_SAND || path.equals(\"sand\")", "original red-sand mapping is missing");
requireFragment(source, "path.contains(\"ore\") ? ModBlocks.INFESTED_ORE", "the original generic ore fallback is missing");
requireFragment(source, "path.startsWith(\"infested\") && !path.equals(\"infestedsand\")", "original post-conversion bush growth is missing");
requireFragment(source, "if (!any)", "the original harlequin/lipoma fallback is missing");
requireFragment(source, "random.nextInt(5)", "the original five-way plant distribution is missing");
requireFragment(source, "random.nextBoolean()", "the original hair choice is missing");
requireFragment(source, "level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE", "original large-smoke particles are missing");
requireFragment(gameTest, "infested_bonemeal/sphere_and_guards", "the sphere GameTest is not registered");
requireFragment(gameTest, "Blocks.FARMLAND, 20", "the GameTest does not exercise scheduled-tick exclusion");
requireFragment(gameTest, "Blocks.WATER", "the GameTest does not exercise fluid exclusion");
requireFragment(gameTest, "ModBlocks.INFUSER_FURNACE.get()", "the GameTest does not exercise block-entity exclusion");

if (failures.length) {
  console.error(`Infested bonemeal verification failed (${failures.length} checks):`);
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Infested bonemeal verification passed (sphere, legacy mapping, decoration and runtime guards).");
