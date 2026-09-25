# 补齐阶段作业说明（FIX BRIEF）

本文件规定「按差异清单补齐」阶段每一批次的执行方式与验收标准。目标不是“看起来实现了”，
而是**每条条款都能指到原版源码、指到本工程实现、并有可复跑的验证证据**。

## 0. 前置条件（每批开始前确认）

1. 工作区在可编译基线上：`cd D:/code/MC模组/csrp && ./gradlew.bat build` 必须 **BUILD SUCCESSFUL**。
2. 参考源码就位：`D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites`（原版事实来源）。
3. 缺口清单就位：`docs/entity-parity/PARITY_MATRIX.md`（逐生物）、`GAP_CLUSTERS.md`（共享缺口）、
   `SYSTEM_GAPS.md`（系统层缺口）。
4. 基线可复跑：
   ```
   node scripts/entity-parity/build-entity-parity-input.cjs
   node scripts/entity-parity/verify-entity-parity.cjs
   node scripts/entity-parity/summarize-parity-gaps.cjs
   node scripts/entity-parity/verify-system-parity.cjs
   ```

## 1. 批次划分原则

- **先系统、后个体**：`GAP_CLUSTERS.md` 中被 ≥3 只生物共享的条款属于共用系统缺陷（例如原版 `EntityMob`
  基线目标、阶段属性加成、基因加成、最小伤害、COTH 传播、掉落体系）；这些先修，一次修好影响面大。
- **再按生物组**：同一继承链的生物（`pri_*`/`ada_*`/`sim_*`/`fer_*`/`mar_*`/bogus 组）一起修，避免重复改基类。
- **每个批次规模**：5–15 条条款，或 1 个系统 + 其最小可验证闭环；太大则无法定位回归。
- **批次顺序参考**：按 `SYSTEM_GAPS.md` 的 `impact=high` 数量降序，再按 `PARITY_MATRIX.md` 完成度升序。

## 2. 单批次执行流程

1. **取条款**：从缺口清单中摘出本批条款（每条含原版证据 `路径:行号`）。
2. **读原版**：读原版实现（含继承链上下文），把语义、数值、门控、冷却、伴随表现（音效/粒子/动画）都记下来；
   注意 1.12.2 → 26.3 的 API 映射（NBT → `ValueInput`/`ValueOutput`、`EntityType.create(level, reason)`、
   `SynchedEntityData.Builder`、`Identifier`、`HolderSet` 等）。
3. **实现**：在本工程落地，遵守 `AGENTS.md` 的代码规范（4 空格缩进、`PascalCase`/`camelCase`、
   注册 id 小写下划线、NeoForge `DeferredRegister`、客户端代码用 `Dist.CLIENT` 隔离）。
   - 数值必须来自原版常量或原版配置文件默认值，不许“凭感觉取整”。
   - 行为必须接线到可触发路径（goal/target/tick/事件），不许只加一个不会被调用的方法。
   - 不许留 `TODO`/空实现/占位返回值来“凑条款”。
4. **验证**（三件套，缺一不可）：
   - 新增或更新 `scripts/verify-<feature>.cjs` 契约脚本，断言本批条款（源码/资源断言要能失败）。
   - `node scripts/run-all-verifications.cjs` 全绿。
   - `./gradlew.bat build` BUILD SUCCESSFUL（编译 + 资源打包）。
5. **回写矩阵**：更新受影响生物的 `docs/entity-parity/raw/<id>.json` 中对应条款的 `verdict` 与 `evidence.project`
   （只改判定与证据，不许删条款或改口径），然后复跑第 0.4 节的四条命令。
6. **提交**：`-（还原）<系统/生物组>补齐 <要点>`，一次提交只含一个批次；提交信息里列出本批条款数与验证命令。

## 3. 验收标准（“100% 还原”的可判定定义）

- 对 127 只生物的每一条原版条款，`verdict` 为 `satisfied`（或原版本就不适用的 `na`）；
- `PARITY_MATRIX.md` 总体加权完成度 = **100%**，且 `verify-entity-parity.cjs` 零 `problems`；
- `SYSTEM_GAPS.md` 中所有 `impact=high` 的缺口关闭；
- `./gradlew.bat build` 通过 + `run-all-verifications.cjs` 全绿 + 至少一次 `runServer`/GameTest 冒烟（实体可创建、可 tick、NBT 往返）；
- 全部证据行号指向当前树（切换基线后需复核一次，见 `docs/entity-parity/BASELINE.md` 的“前提与风险”）。

## 4. 反模式（本阶段明确禁止）

- 用“别名注册 / 空 Renderer / 无行为实体”之类手段让校验脚本变绿（`docs/ENTITY_PORTING_MATRIX.md` 已记录过此类历史）。
- 为了让某条条款判 `satisfied` 而修改审计口径或删条款；口径只在 `AUDIT_PROTOCOL.md` 中统一演进。
- 一次性大改多个系统而不分批次，导致无法定位回归。
- 在切换基线前直接修改 `src/**`（会让分支切换失败、并让审计证据与树错位）。
