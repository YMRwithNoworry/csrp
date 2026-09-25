// One-off codemod: gate the existing water-leap goals on the legacy geneWaterleap flag
// (EvolutionSystem.GenerationProfile.waterLeap) and add the shared helper to the parasite base.
const fs = require("node:fs");
const path = require("node:path");

const dir = path.join(__dirname, "..", "..", "src", "main", "java", "alku", "csrp", "entity");

/** goal class name -> owning file */
const goals = {
  "ArachnidaWaterLeapGoal": "AdaptedVariantEntity.java",
  "WaterLeapGoal": null, // two files: HeedEntity.java and VisceraEntity.java
  "CarrierWaterLeapGoal": "PreeminentParasiteEntity.java",
  "ManducaterWaterLeapGoal": "PrimitiveVariantEntity.java",
  "ReekerWaterLeapGoal": "PrimitiveVariantEntity.java",
  "GruntWaterLeapGoal": "PureParasiteEntity.java",
  "MonarchWaterLeapGoal": "PureParasiteEntity.java"
};

const GUARD = [
  "        public boolean canUse() {",
  "            // Legacy geneWaterleap: the generation decides whether water leaps exist.",
  "            if (!waterLeapEnabled()) {",
  "                return false;",
  "            }"
].join("\n");

const targets = [];
for (const [goal, file] of Object.entries(goals)) {
  if (file) targets.push({ goal, file });
}
targets.push({ goal: "WaterLeapGoal", file: "HeedEntity.java" });
targets.push({ goal: "WaterLeapGoal", file: "VisceraEntity.java" });

let patched = 0;
for (const target of targets) {
  const full = path.join(dir, target.file);
  const source = fs.readFileSync(full, "utf8");
  const classAt = source.indexOf(`class ${target.goal} extends Goal`);
  if (classAt < 0) {
    console.log(`CLASS NOT FOUND: ${target.goal} in ${target.file}`);
    continue;
  }
  const canUseAt = source.indexOf("public boolean canUse() {", classAt);
  if (canUseAt < 0 || canUseAt - classAt > 4000) {
    console.log(`canUse NOT FOUND: ${target.goal} in ${target.file}`);
    continue;
  }
  if (source.slice(canUseAt, canUseAt + 200).includes("waterLeapEnabled()")) {
    console.log(`already gated: ${target.goal}`);
    continue;
  }
  const updated = source.slice(0, canUseAt) + GUARD + source.slice(canUseAt + "        public boolean canUse() {".length);
  fs.writeFileSync(full, updated, "utf8");
  patched++;
  console.log(`gated ${target.goal} in ${target.file}`);
}

// shared helper on the primitive base
const base = path.join(dir, "PrimitiveParasiteEntity.java");
let baseSource = fs.readFileSync(base, "utf8");
if (!baseSource.includes("protected final boolean waterLeapEnabled()")) {
  const anchor = "    /** Legacy EntityAIWait: suspends AI for the given ticks (see {@link WaitGoal}). */";
  if (!baseSource.includes(anchor)) {
    console.log("HELPER ANCHOR NOT FOUND");
  } else {
    baseSource = baseSource.replace(anchor, [
      "    /** Legacy geneWaterleap (applyGene): the generation decides whether water leaps exist. */",
      "    protected final boolean waterLeapEnabled() {",
      "        return level() instanceof ServerLevel serverLevel",
      "                && EvolutionSystem.generationProfile(serverLevel).waterLeap();",
      "    }",
      "",
      anchor
    ].join("\n"));
    fs.writeFileSync(base, baseSource, "utf8");
    console.log("helper added to PrimitiveParasiteEntity");
  }
}
console.log(`patched ${patched} goal(s)`);
