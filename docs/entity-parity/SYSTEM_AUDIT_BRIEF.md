# 系统层审计作业说明（SYSTEM AUDIT BRIEF）

逐生物审计回答「这只生物差什么」，系统层审计回答「**哪些共用系统整体没搬过来**」。
后者是补齐阶段的主要依据，因为大量缺口是继承链上的共用逻辑（基类、伤害规则、AI、属性、同步数据、生成、掉落）。

## 事实来源

- 原版（SRParasites 1.10.9，1.12.2 Forge 反编译）：`D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites`
- 本工程（MC 26.3 / NeoForge 26.3）：`D:/code/MC模组/csrp`
- 口径参考：`docs/entity-parity/AUDIT_PROTOCOL.md`（判定与证据要求）、`docs/entity-parity/audit-input.json`（127 只生物的类映射与继承链）

## 审计领域（area）

| area | 原版侧入口 | 本工程侧入口 |
| --- | --- | --- |
| `base_classes` | `entity/ai/misc/EntityParasiteBase.java`、`entity/IHitboxedEntity.java`、`entity/ai/misc/EntityBodyParts.java`、`entity/EntityBody.java` | `entity/Parasite.java`、`entity/PrimitiveParasiteEntity.java`、`entity/AdaptedVariantEntity.java`、`entity/CrudeParasiteEntity.java`、`entity/DerivedParasiteEntity.java`、`entity/PureParasiteEntity.java`、`entity/FeralParasiteEntity.java`、`entity/HijackedParasiteEntity.java`、`animation/**` |
| `damage_effects` | `util/SRPAttributes.java`、`entity/ai/misc/EntityCutomAttack.java`、`util/handlers/**`（伤害/免疫/适应）、`potion/**` | `entity/ParasiteCombatEffects.java`、`entity/AssimilatedMeltSystem.java`、`infection/**`、`effect/**`、`event/**` |
| `ai_inventory` | `entity/ai/**`（73 个类）、`EntityMob` 基线任务 | `entity/**` 内的 `Goal` 内部类、`entity/ParasiteFollowGoal.java` |
| `attributes_genes` | `util/SRPAttributes.java`、`applyGene`、`evolutionParasiteStatIncrease`、`SRPConfigMobs` 逐生物倍率 | `entity/*ParasiteEntity.java` 的 `createAttributes()`、`world/EvolutionSystem.java`、`config/**` |
| `sync_data` | 各生物 `DataManager.register`/`setDataManager` | 各实体 `defineSynchedData` + `EntityDataAccessor` |
| `spawning` | `init/SRPSpawning.java`、`world/**`、`SRPConfigWorld` | `registry/CommonModEvents.java`、`src/main/resources/data/csrp/worldgen/**` |
| `loot_drops` | 各生物 `dropFewItems` / `entityDropItem` | `src/main/resources/data/csrp/loot_table/**` |
| `animation_model` | `client/**`（ModelSRP、Tabula 动画控制器、渲染器） | `src/main/java/alku/csrp/client/**`、`src/main/resources/assets/csrp/{geo,animations,tabla}` |

## 输出格式

每个 area 一个文件：`D:/code/MC模组/csrp/docs/entity-parity/system/<area>.json`

```json
{
  "area": "damage_effects",
  "title": "伤害/免疫/适应系统对照",
  "originalRefs": [".../util/SRPAttributes.java"],
  "projectRefs": ["src/main/java/alku/csrp/entity/ParasiteCombatEffects.java"],
  "clauses": [
    {
      "clause": "火焰伤害 ×4（fireDamageMultiplier）",
      "verdict": "satisfied",
      "evidence": {"original": ".../entity/ai/misc/EntityParasiteBase.java:120", "project": ".../entity/PrimitiveParasiteEntity.java:210"},
      "impact": "high",
      "affectedGroups": ["primitive", "adapted", "inborn"],
      "note": "数值一致；仅 API 名称不同"
    }
  ],
  "summary": "…（≤5 行，指出最关键的 3 个缺口与影响面）"
}
```

- `verdict`：`satisfied` / `partial` / `missing` / `na`（判定口径同审计协议）。
- `impact`：`high` / `medium` / `low`（该条款缺失对“生物还原度”的影响）。
- `affectedGroups`：受影响的生物组（如 `inborn`/`crude`/`primitive`/`adapted`/`assimilated`/`feral`/`hijacked`/`marauderized`/`pure`/`preeminent`/`ancient`/`nexus`/`all`）。
- 证据必须给 `路径:行号`；`satisfied` 必须有 `project` 证据。

## 硬性要求

- **只新增 `docs/entity-parity/system/<area>.json`，禁止修改任何其它文件**。
- 条款要覆盖该系统的**全部可观察行为**，宁细勿粗：数值、门控、冷却、免疫、伴随音效/粒子都算独立条款。
- 明确区分「本工程完全没有」(`missing`) 与「有但不完整/数值不符」(`partial`)。
- 结论要可执行：`summary` 里给出补什么、影响哪些生物组。
