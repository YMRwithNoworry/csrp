# 尸块活体血肉系统（Corpse → Living Flesh）

> 行为改动：**被寄生体杀死的生物会在尸体处留下「活体肉块」（Living Flesh，`csrp:movingflesh`）；
> 两个活体肉块会主动互相接近、融合，随后融化成一个随机寄生体。**
> 默认配置下融合结果覆盖全部七个 tier：Crude / Feral / Assimara / Hijacked / Primitive / Adapted / Pure。

> 本文件同步自 `main`（1.21.1）提交 `-（功能）击杀留下活体肉块，两块融合融化出新寄生体`，
> 代码已按 MC 26.3 / NeoForge 26.3 的 API 名称落位（`snapTo`、`EntitySpawnReason`、
> `EntityType.create(Level, EntitySpawnReason)`），因此不会给 26.3 移植新增编译错误。

## 1. 尸体留下活体肉块

`alku.csrp.infection.InfectionEvents#leaveMovingFleshOnParasiteKill`（`LivingDeathEvent`，
`EventPriority.LOWEST`）：

- **击杀者必须是寄生体**（`attacker instanceof Parasite`，即任何 CSRP 寄生生物，含弹射物/间接伤害的归属者）。
- **不拦截死亡**：处理器不调用 `setCanceled`，因此被击杀生物照常结算死亡——
  **掉落物、经验、死亡音效/粒子全部保留**，活体肉块是额外生成的实体。
- **排除项**：寄生体自身（`corpse instanceof Parasite`）与玩家（`corpse instanceof Player`，玩家走同化路径）。
- **优先级 LOWEST 的意义**：`convertTerminalCothHost` 把 COTH 宿主转化为同化体/未成形体时会取消死亡事件，
  已被取消的死亡不会进入 LOWEST 处理器，所以**同化转化优先，其余击杀才留下活体肉块**。

生成逻辑在 `alku.csrp.entity.MovingFleshEntity#spawnFromCorpse(ServerLevel, LivingEntity)`：
在尸体坐标生成活体肉块，继承自定义名称与 `persistenceRequired`（命名生物留下的肉块不会自然消失），
死亡结算完成后尸体正常移除。

## 2. 两块融合 → 融化出新生物

| 项目 | 值 | 说明 |
| --- | --- | --- |
| `REQUIRED_MERGES` | `2` | **两个**活体肉块融合即可产生新生物（原版 SRParasites 为 4 块） |
| `MergeMovingFleshGoal.SEARCH_RADIUS` | `32.0D` | 主动索敌半径，让落单肉块也能找到同伴 |
| `EVOLUTION_DELAY_TICKS` / `EVOLUTION_FUSE_INCREMENT` | `70` / `2` | 融合后约 35 tick（1.75 秒）融化 |
| `mergeSystemMobHealth` | `0.5` | 新生物血量 = 最大血量 × 该系数 |

流程：两块肉块互相靠近 → 接触若干次后较大者吸收较小者（`absorb`，融合计数 `1+1=2` 达到
`REQUIRED_MERGES`）→ `startEvolution()` 冻结移动并播放闪烁 → 融化时播放 `moving_flesh.primitive`
音效并产生爆炸粒子，随后 `discard()` 自身、生成新的寄生体。

## 3. 融合结果池（七个 tier）

结果由 `MobsConfig.mergeSystemMobList`（`config/csrp-mobs.toml` 的 `[merge_System]` 段）决定，
默认值已扩展为**七 tier 全池**共 59 个条目，配合 `mergeSystemRandom=true`（默认）等概率随机：
Crude（10）、Feral（9）、Assimara（6）、Hijacked（3）、Primitive（12）、Adapted（12）、Pure（7）。
配置项语义不变：条目格式 `entity_id;merge_value`，`mergeSystemRandom=false` 时按 `merge_value`
精确匹配（所有条目默认 `0`，此时退化为随机）。

## 4. 与既有系统的关系

- **同化（Assimilation）**：COTH 击杀转化保持原样，优先于本系统。
- **同化体融化系统**（`AssimilatedMeltSystem`）：被同化生物融化仍生成活体肉块，与本系统共享同一个融合池。
- **未成形寄生体 / 未成形残体**：仅作为融合池中的 Crude tier 成员出现，自身行为不变。

## 5. 验证

- `node scripts/verify-corpse-moving-flesh.cjs`：断言死亡处理器（优先级、击杀者、排除项、不取消死亡、
  调用生成工厂）、融合阈值与融合流程、以及默认融合池**七 tier 完整且每个 id 都已注册**。
- `node scripts/verify-sim-adventurer-port.cjs` 的 `REQUIRED_MERGES` 断言已随本次刻意改动更新为 `2`；
  该脚本在本分支仍有既有的 26.3 移植期失败项（与本次改动无关）。
- **`gradlew build` 在本分支当前仍失败**（`PLAN.md` 记录 26.3 移植尚余约 3235 个编译错误），
  因此本分支的改动只做了静态断言与人工审阅；`main`（1.21.1）分支上的同一改动已通过
  `gradlew build` 与全部 93 个校验脚本（失败集合与改动前一致）。
