# 审计代理作业说明（AGENT BRIEF）

你是 MC 模组移植工程 csrp 的「生物还原审计代理」。目标：逐生物、逐条款比对原版 SRParasites 1.10.9
与本工程实现，产出可核查的差异清单。

## 事实来源

**本工程（Minecraft 26.3 / NeoForge 26.3，Java 25）：`D:/code/MC模组/csrp`**

| 用途 | 路径 |
| --- | --- |
| 实体实现 | `src/main/java/alku/csrp/entity/**` |
| 实体注册 | `src/main/java/alku/csrp/registry/ModEntities.java` |
| 渲染器 / 模型 | `src/main/java/alku/csrp/client/renderer/**`、`client/model/**` |
| 自然生成规则 | `src/main/java/alku/csrp/registry/CommonModEvents.java`（SpawnPlacements / SpawnRules） |
| 模型 / 动画 / 贴图 | `src/main/resources/assets/csrp/{geo,animations,tabla,textures}` |
| 语言键 | `src/main/resources/assets/csrp/lang/*.json` |
| 音效 | `src/main/resources/assets/csrp/sounds.json` |
| 实体掉落表 | `src/main/resources/data/csrp/loot_table/**` |

**原版事实来源（SRParasites 1.10.9，1.12.2 Forge 反编译）：`D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites`**

| 用途 | 路径 |
| --- | --- |
| 实体 | `entity/monster/**` |
| 共用基类 | `entity/ai/misc/**`（`EntityParasiteBase`、`EntityP*` 系列、`EntityCan*` 能力接口） |
| AI | `entity/ai/**` |
| 投射物 | `entity/projectile/**` |
| 注册与生成 | `init/SRPEntities.java`、`init/SRPSpawning.java` |
| 原版客户端（模型/动画/贴图/渲染） | `client/**` |
| 掉落 | 写在实体类内的 `dropFewItems` / `entityDropItem` |

## 必读

1. 审计协议：`D:/code/MC模组/csrp/docs/entity-parity/AUDIT_PROTOCOL.md`（11 个 facet、判定与计分、输出格式）
2. 审计输入：`D:/code/MC模组/csrp/docs/entity-parity/audit-input.json`
   （每只生物的 `originalFile` / `originalChain[]` / `projectFile` / `projectChain[]`）

## 工作步骤

1. 读协议，明确 facet 定义、判定标准与计分规则。
2. 从 `audit-input.json` 取本批生物的条目。
3. **原版侧**：读原版类，并沿 `originalChain` 读完所有父类（共用行为算在该生物名下）；按需读它引用的 AI 类、投射物、`init/SRPSpawning.java` 中的生成注册、原版 `client/**` 的模型/贴图/动画名与渲染设置、实体类内的掉落逻辑。
4. **本工程侧**：读实体类与父类链、`ModEntities.java` 条目、renderer/模型/动画、`data/csrp/loot_table` 掉落表、`lang` 键、`sounds.json`、`CommonModEvents.java` 的生成规则。
5. 逐条枚举原版条款（枚举单位见协议第 2 节表格），逐条判定 `satisfied` / `partial` / `missing` / `na`，
   每条给出证据：`original` 形如 `路径:行号`，`project` 同样（缺失时 `null`）。
6. 为每个 id 写一个 `D:/code/MC模组/csrp/docs/entity-parity/raw/<id>.json`，格式严格按协议第 4 节。

## 硬性要求

- **只新增 `docs/entity-parity/raw/<id>.json`，禁止修改任何其它文件**（包括源码、注册表、脚本）。
- 证据不足不许判 `satisfied`；不确定就 `partial` / `missing` 并在 `notes` 说明原因。
- 原版 `EntityMob` / `EntityParasiteBase` 系列的共用行为（近战、索敌、游荡、观察、伤害规则、COTH、适应等）都算该生物的条款。
- API 差异不算缺失：1.12.2 → 26.3 的等价实现记 `satisfied`，并在 `notes` 写明映射关系。
- 数值差异（血量/伤害/护甲/速度/跟随距离/碰撞箱/经验/掉落概率与数量）、AI 优先级、技能冷却、
  阶段门控（phase / COTH）、粒子与音效等伴随表现都算独立条款。
- JSON 必须能被 `JSON.parse` 解析（UTF-8、无注释）。

## 返回

只回简短摘要（不要贴审计明细全文）：

```json
{"ids":["..."],"written":["docs/entity-parity/raw/x.json"],"perCreature":[{"id":"...","satisfied":N,"partial":N,"missing":N,"na":N,"completion":0.00,"topGaps":["...","..."]}],"notes":"..."}
```
