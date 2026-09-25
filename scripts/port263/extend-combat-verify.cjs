// One-off: extend the combat-rule verification with the geneBlockSearch / geneSprinting gates.
const fs = require("node:fs");
const path = require("node:path");

const file = path.join(__dirname, "..", "verify-parasite-combat-rules.cjs");
let source = fs.readFileSync(file, "utf8");

const lines = [
  "// legacy geneBlockSearch / geneSprinting: both gate behaviour that already existed",
  "for (const [pattern, message] of [",
  "  [/protected final boolean blockSearchEnabled\\(\\) \\{[\\s\\S]{0,200}?\\.blockSearch\\(\\)/,",
  "    \"PrimitiveParasiteEntity.blockSearchEnabled() is missing\"],",
  "  [/if \\(!canBreakBlocks\\(\\) \\|\\| !blockSearchEnabled\\(\\)\\) \\{/,",
  "    \"block breaking must be gated by the geneBlockSearch flag\"],",
  "  [/protected final boolean sprintingEnabled\\(\\) \\{[\\s\\S]{0,200}?\\.sprinting\\(\\)/,",
  "    \"PrimitiveParasiteEntity.sprintingEnabled() is missing\"]",
  "]) expect(primitive, pattern, message);",
  "",
  "const sprintGoal = read(\"src/main/java/alku/csrp/entity/GeneSprintGoal.java\");",
  "for (const [pattern, message] of [",
  "  [/public final class GeneSprintGoal extends Goal/, \"the sprint goal is missing\"],",
  "  [/SPRINT_MULTIPLIER = 1\\.3D/, \"the legacy 1.3 sprint multiplier is missing\"],",
  "  [/SPRINT_DISTANCE_SQR = 16\\.0D/, \"the sprint must only apply while the target is far\"],",
  "  [/setFlags\\(EnumSet\\.of\\(Flag\\.MOVE\\)\\)/, \"the sprint goal must claim the MOVE flag\"],",
  "  [/EvolutionSystem\\.generationProfile\\(serverLevel\\)\\.sprinting\\(\\)/,",
  "    \"the sprint goal must consult the geneSprinting flag\"]",
  "]) expect(sprintGoal, pattern, message);",
  "",
  "// the three families register the sprint goal just before their melee goal, same priority",
  "for (const [file, pattern, message] of [",
  "  [\"MarauderizedParasiteEntity.java\",",
  "    /addGoal\\(3, new GeneSprintGoal\\(this, meleeSpeed\\(\\)\\)\\);\\s*\\r?\\n\\s*goalSelector\\.addGoal\\(3, new MeleeAttackGoal/,",
  "    \"MarauderizedParasiteEntity must register the sprint goal before its melee goal\"],",
  "  [\"FeralParasiteEntity.java\",",
  "    /addGoal\\(2, new GeneSprintGoal\\(this, 1\\.5D\\)\\);\\s*\\r?\\n\\s*goalSelector\\.addGoal\\(2, new MeleeAttackGoal/,",
  "    \"FeralParasiteEntity must register the sprint goal before its melee goal\"],",
  "  [\"AssimilatedParasiteEntity.java\",",
  "    /addGoal\\(2, new GeneSprintGoal\\(this, meleeSpeed\\)\\);\\s*\\r?\\n\\s*goalSelector\\.addGoal\\(2, new MeleeAttackGoal/,",
  "    \"AssimilatedParasiteEntity must register the sprint goal before its melee goal\"]",
  "]) expect(read(\"src/main/java/alku/csrp/entity/\" + file), pattern, message);",
  "",
  "if (failures.length) {"
];

const marker = "if (failures.length) {";
if (!source.includes(marker)) {
  console.log("MARKER NOT FOUND");
  process.exit(1);
}
if (source.includes("geneBlockSearch / geneSprinting")) {
  console.log("already patched");
  process.exit(0);
}
source = source.replace(marker, lines.join("\n"));
fs.writeFileSync(file, source, "utf8");
console.log("verify script extended");
