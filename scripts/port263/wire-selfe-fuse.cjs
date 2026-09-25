// One-off codemod: wire the shared SELFE fuse (ParasiteFuseState / SelfeFuseOwner) into the
// parasite families that extend Monster instead of PrimitiveParasiteEntity.
const fs = require("node:fs");
const path = require("node:path");

const dir = path.join(__dirname, "..", "..", "src", "main", "java", "alku", "csrp", "entity");
const targets = [
  {
    file: "AssimilatedParasiteEntity.java",
    declaration: "public final class AssimilatedParasiteEntity extends Monster\n"
      + "        implements CitadelAnimatedEntity, Parasite, MeltableAssimilated, ManualVariantProvider {"
  },
  {
    file: "AssimilatedVariantEntity.java",
    declaration: "public final class AssimilatedVariantEntity extends Monster "
      + "implements CitadelAnimatedEntity, Parasite, MeltableAssimilated {"
  },
  {
    file: "SimHumanEntity.java",
    declaration: "public final class SimHumanEntity extends Monster "
      + "implements CitadelAnimatedEntity, Parasite, MeltableAssimilated {"
  },
  {
    file: "FeralParasiteEntity.java",
    declaration: "public class FeralParasiteEntity extends Monster "
      + "implements CitadelAnimatedEntity, Parasite {"
  }
];

const METHODS = [
  "",
  "    /** Legacy SELFE self-destruct fuse, shared with every other parasite family. */",
  "    private final ParasiteFuseState selfeFuse = new ParasiteFuseState();",
  "",
  "    @Override",
  "    public boolean willExplodeOnDeath() {",
  "        return selfeFuse.willExplodeOnDeath(this);",
  "    }",
  "",
  "    @Override",
  "    public void startDyingFuse() {",
  "        selfeFuse.start(this);",
  "    }",
  "",
  "    @Override",
  "    public boolean isDyingFuseActive() {",
  "        return selfeFuse.isActive(this);",
  "    }",
  "",
  "    @Override",
  "    public float getSelfeFlashIntensity(float partialTick) {",
  "        return selfeFuse.flashIntensity(this, partialTick);",
  "    }",
  "",
  "    /** Legacy onDeathUpdate: hold the corpse while the fuse burns, then burst. */",
  "    @Override",
  "    protected void tickDeath() {",
  "        if (!selfeFuse.isActive(this)) {",
  "            super.tickDeath();",
  "            return;",
  "        }",
  "        if (deathTime < ParasiteFuseState.DEATH_ANIMATION_TICKS) {",
  "            deathTime++;",
  "        }",
  "        if (level().isClientSide) {",
  "            return;",
  "        }",
  "        if (selfeFuse.advance(this)) {",
  "            if (level() instanceof ServerLevel serverLevel) {",
  "                ParasiteCombatRules.selfExplode(serverLevel, this);",
  "            }",
  "            selfeFuse.clear(this);",
  "            super.tickDeath();",
  "        }",
  "    }",
  ""
].join("\n");

for (const target of targets) {
  const file = path.join(dir, target.file);
  let source = fs.readFileSync(file, "utf8");
  if (!source.includes(target.declaration)) {
    console.log(`DECLARATION NOT FOUND: ${target.file}`);
    continue;
  }
  source = source.replace(target.declaration, target.declaration.replace(" {", ", SelfeFuseOwner {"));
  source = source.replace(
    /(protected void defineSynchedData\(SynchedEntityData\.Builder builder\) \{\r?\n        super\.defineSynchedData\(builder\);)/,
    "$1\n        builder.define(ParasiteFuseState.SELFE, -1);");
  source = source.replace(
    /(public boolean hurt\(DamageSource source, float amount\) \{\r?\n)/,
    "$1        if (!level().isClientSide) {\n            selfeFuse.willExplodeOnDeath(this);\n        }\n");
  source = source.replace(
    /(\r?\n    @Override\r?\n    protected void defineSynchedData\(SynchedEntityData\.Builder builder\) \{)/,
    `${METHODS}$1`);
  if (!source.includes("import alku.csrp.event.ParasiteCombatRules;")) {
    source = source.replace(/^import alku\.csrp\./m, "import alku.csrp.event.ParasiteCombatRules;\nimport alku.csrp.");
  }
  fs.writeFileSync(file, source, "utf8");
  console.log(`patched ${target.file}`);
}
