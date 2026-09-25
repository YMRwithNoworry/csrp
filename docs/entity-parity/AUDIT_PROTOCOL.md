# 生物部分 100% 还原 —— 审计协议（Audit Protocol）

本文件是「逐生物差异清单 + 完成度基线」的方法定义，也是审计代理必须遵守的判定标准。
所有结论必须可核查：每条判定都要给出**原版证据**与**本工程证据**（文件:行）。

- 事实来源（原版）：`D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites/**`
  （1.10.9 与 1.10.8 的生物部分源码一致；`decomp-1.10.9` 同时是版本增量的事实来源）
- 事实来源（本工程）：`D:/code/MC模组/csrp/src/main/java/alku/csrp/**`、`src/main/resources/**`
- 审计输入（127 只生物 → 原版类 / 本工程类 / 双方继承链）：`docs/entity-parity/audit-input.json`

## 1. 审计单元

一个审计单元 = 一个 `SRPEntities.CreateEntityMob` 注册项。**行为可能来自继承链**，因此必须走完
`originalChain`（例如 `EntityShyco → EntityPPrimitive → EntityPMalleable → EntityParasiteBase`）与
`projectChain`（例如 `LongarmsEntity → PrimitiveParasiteEntity`）。只比对最末级子类会漏掉全部共用行为。

## 2. 十一个审计面（facet）

| facet | 含义 | 原版条款的枚举单位 |
| --- | --- | --- |
| `registration` | 注册身份：注册 id、MobCategory、碰撞箱/眼高、tracking range、刷怪蛋（颜色/开关）、`SRPConfig*` 开关、语言键 | 每个注册字段/开关 1 条 |
| `attributes` | 属性：`applyEntityAttributes`/`getEntityAttribute` 的每一项，含阶段/夜行/适应加成 | 每个属性 1 条 |
| `ai` | GOAL 集合：`tasks.addTask(prio, ...)`、`targetTasks.addTask(...)` 全量 | 每条 addTask 1 条 |
| `behaviors` | 实体专属行为：攻击方式、技能与冷却、状态机、`onLivingUpdate`/`updateEntityActionState`、投射物、召唤、钻地、伪装、融化、殖民地、跃击、闪避等 | 每个可观察行为 1 条 |
| `damage_and_effects` | 伤害/免疫/击退、对自身与目标施加的效果（COTH、病毒、缓慢…）、击杀计数与进化、伤害适应接入 | 每种伤害/效果 1 条 |
| `sync_data` | `DataManager.register`/`setDataManager` 参数 ↔ `SynchedEntityData` 定义，语义与同步时机 | 每个数据参数 1 条 |
| `animation` | 动画名、controller/状态机、触发条件与阈值 | 每个动画/控制器 1 条 |
| `model_texture` | 模型文件、贴图、渲染器设置（缩放、层、发光、阴影、hitbox） | 每个资源/渲染设置 1 条 |
| `sounds` | `playSound`/`SoundEvents`/自定义 SoundType：环境、受伤、死亡、攻击、脚步等 | 每个音效事件 1 条 |
| `spawning` | 自然生成（`SRPSpawning`/生物群系刷怪表：weight、min/max group、阶段/维度门控、光照/高度规则） | 每条生成规则 1 条 |
| `loot` | 掉落：`dropFewItems`/`entityDropItem`/装备/经验 | 每个掉落项 1 条 |

## 3. 判定与计分

- 每条原版条款判定为 `satisfied` / `partial` / `missing`：
  - `satisfied`：本工程存在等价实现（允许 API 改名、GoaL 类名不同，但行为语义一致）。
  - `partial`：部分实现（如数值不同、只在部分形态生效、缺少冷却/音效/粒子等伴随表现）。
  - `missing`：不存在。
- `na`：该条款在原版对本生物不适用（例如原版客户端没有渲染器）；`na` 不计入分母。
- 完成度 = `Σ satisfied / Σ (satisfied + partial + missing)`，`partial` 计 0.5。
- 未读到证据的条款**不得**判 `satisfied`；不确定时给 `partial` 或 `missing` 并在 `confidence` 标注。

## 4. 单生物输出格式

```json
{
  "id": "pri_longarms",
  "originalClass": "EntityShyco",
  "projectClass": "LongarmsEntity",
  "facets": [
    {
      "name": "ai",
      "status": "partial",
      "clauses": [
        {"clause": "tasks.addTask(1, EntityAIAttackMeleeNotGround)", "verdict": "satisfied",
         "evidence": {"original": ".../monster/primitive/EntityShyco.java:88",
                      "project": "src/main/java/alku/csrp/entity/LongarmsEntity.java:64"}},
        {"clause": "tasks.addTask(4, EntityAICircleGroup)", "verdict": "missing",
         "evidence": {"original": ".../ai/misc/EntityAICircleGroup.java:1", "project": null},
         "note": "群体绕行缺失"}
      ],
      "confidence": "high"
    }
  ],
  "missingSummary": ["自然生成权重在 26.3 未接入", "CircleGroup 绕行 AI 缺失"],
  "confidence": "high"
}
```

- `clauses[].clause` 必须能回溯到原版源码的一行或一段（给出短引用，不要贴大段代码）。
- `evidence.project` 允许为 `null`，此时 `verdict` 必须是 `missing` 或 `partial`。
- 缺证据链的判定视为无效审计。

## 5. 汇总产物

| 产物 | 内容 |
| --- | --- |
| `docs/entity-parity/raw/<id>.json` | 单生物审计明细（审计代理写出） |
| `docs/entity-parity/system/<area>.json` | 系统层审计明细（8 个 area，见 `SYSTEM_AUDIT_BRIEF.md`） |
| `docs/entity-parity/parity-matrix.json` | 127 只 × 11 面 的合并矩阵 + 条款计数 |
| `docs/entity-parity/PARITY_MATRIX.md` | 人读矩阵与缺口清单 |
| `docs/entity-parity/BASELINE.md` | 方法与完成度基线（总体/分面/分批） |
| `docs/entity-parity/GAP_CLUSTERS.md` | 共享缺口聚类（补齐批次依据之一） |
| `docs/entity-parity/SYSTEM_GAPS.md` | 系统层缺口与影响面（补齐批次依据之二） |
| `docs/entity-parity/AUDIT_CROSSCHECK.md` | AI 条款 premise 交叉校验结果 |
| `docs/entity-parity/original-ai-baseline.json` | 「原版是否继承 EntityMob 基线任务」的地面真值 |
| `scripts/entity-parity/verify-entity-parity.cjs` | 结构完整性校验 + 矩阵/基线生成 |
| `scripts/entity-parity/verify-system-parity.cjs` | 系统层审计校验 + 缺口生成 |
| `scripts/entity-parity/run-parity-pipeline.cjs` | 一键复跑（输入 → 矩阵 → 缺口） |

## 6. 复审与地面真值（重要）

代理审计会出现 **premise 错误**（把原版根本没有的条款当成缺失）。已确认的系统性 premise 错误：

- **`EntityMob` 基线任务不是默认存在**：1.12.2 的 `EntityMob#func_184651_r`（即 `initEntityAI`）注册
  近战/游荡/观察/张望/反击/索敌玩家等基线任务，但**只有完全不覆盖它、或覆盖时调用 `super.func_184651_r()` 才会生效**。
  实测：127 只生物里 **110 只**在该链上覆盖了 `func_184651_r` 且未调用 `super`，因此这些基线任务**不存在**
  （地面真值见 `original-ai-baseline.json`，由 `check-original-ai-inheritance.cjs` 生成）。
  审计时**不得**凭空添加「继承 EntityMob：…」条款；被推翻的条款已判为 `na` 并写入 `reviewNote`。

复审工具（对全部 `ai` 条款做 premise 校验：抽取被引用的 AI 类名 → 在该生物原版继承链中查找 `new 类名(`
注册点，兼容内部类 `new EntityParasiteBase.EntityAIWait(` 写法；对「基线适用于该生物」的情况放行
vanilla `EntityMob` 任务）：

```
node scripts/entity-parity/check-original-ai-inheritance.cjs      # 生成地面真值
node scripts/entity-parity/cross-check-audit-claims.cjs           # 只报告
node scripts/entity-parity/cross-check-audit-claims.cjs --apply   # 推翻的条款判 na + 写 reviewNote
```

报告写入 `AUDIT_CROSSCHECK.md`。`reviewNote` 是复审痕迹，**不允许**在后续补齐中被删除。

## 7. 尚未覆盖的维度（补齐阶段必须补做）

- **反向校验**：本工程是否注册了原版**没有**的任务/行为（例如所有 primitive 都挂了
  `NearestAttackableTargetGoal<LivingEntity>` 会让它们攻击原版不会攻击的目标）。条款模型是
  原版驱动的，天然看不见「多出来的行为」，因此每只生物补齐后要单独比对一次 goal 集合的**超集/子集**关系。
- **数值来源**：仿制数值必须能指回原版常量或原版配置默认值；`partial` 判定不能只写“数值不同”而不给原版值。
- **运行时验证**：源码层 `satisfied` 不等于运行时正确（AI 优先级、门控、同步时机都会出错）。每个系统补齐后
  需要 `runServer`/GameTest 冒烟（实体可创建、可 tick、NBT 往返、行为可触发）。
