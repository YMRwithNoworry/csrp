# 复仇之书抓钩（Vengeance Grapple）保真差异报告

**状态：待修正。** 本文件记录 1.10.9 原件 `item/VengeanceGrappleHandler.java`（441 行）的完整规格，以及本工程 `event/BookOfVengeanceEvents.java`（285 行）当前的差异。规格已逐行核实，可直接据此重写。

## 1. 触发方式

| | 原件 `ItemBookOfVengeance` | 工程 `BookOfVengeanceItem` |
| --- | --- | --- |
| 触发 | 右键，`findLookTargetLiving(player, 32.0)` 取视线目标，需 `player.canEntityBeSeen(target)` | `useOnEntity` 传入目标 |
| 冷却 | **60 tick** | `SLAM_COOLDOWN_TICKS = 80` ❌ |
| 启动 | `VengeanceGrappleHandler.start(player, target)` | `BookOfVengeanceEvents.beginSlamChain(...)` |

## 2. 状态机规格（原件）

### 阶段 1：WINDUP
- `timer = 20` ❌（工程 `MARK_DELAY_TICKS = 10`）
- 每 tick：链粒子（`SPELL_WITCH`）
- `timer % 4 == 0`：FX 类型 0（SPARKS，`count 16`，位置 = 玩家 + 0.2Y，`a = 1.2F`）
- `timer % 6 == 0`：音效 `SoundEvents.field_190021_aL`（音量 0.25，音调 1.8）
- `timer` 递减至 0 → 在目标处播放 `VENGEANCE_CHAIN_IMPACT` → 进入 PULL，`ticks = 0`

### 阶段 2：PULL
- 每 tick：链粒子（`CRIT_MAGIC`）+ `pullPlayerToward(player, target, 1.8)`
- 接触条件：`player.getDistance(target) <= 2.2` ❌（工程 `CONTACT_DISTANCE_SQR = 6.25` 即 2.5）**或** `ticks > 40` ❌（工程 `FIRST_DASH_TICKS = 12`）
- 满足 → `impact1`

### impact1（首次冲击）
- 音效：`VENGEANCE_IMPACT`(1.0,1.0)、`VENGEANCE_ROCK`(0.9,0.9)
- FX 类型 1（IMPACT_DUST，`count 28`）+ `EXPLOSION_HUGE` ×1
- **AoE**：盒 ±5 X、-2..+4 Y、±5 Z；对每个距目标 `<= 5.0` 的存活生物（**排除玩家**）：
  - `applyVengeanceDebuffs(ent)`：`DOD_SMOKE_TRAIL` / `BLEED` / `DEBAR` 各 **100 tick、等级 0、ambient=false、visible=true**
  - `VENGEANCE_WHOOSH`(0.8,1.2)
  - 伤害 **14.0F**（来源：玩家）❌（工程为 **7.0F 且仅对目标**）
  - 沿**玩家视线**弹射：`forward = 1.9`、`upward = 0.65`；`vx = look.x*1.9`、`vz = look.z*1.9`、`vy = max(vy, look.y*1.9 + 0.65)`
  - 记入 `affected` 列表（供 BOUNCE 阶段播 FX）
- 玩家：`vx *= 0.15`、`vz *= 0.15`、`fallDistance = 0`
- → 进入 BOUNCE，`timer = 14` ❌（工程 `BOUNCE_TICKS = 8`）

### 阶段 3：BOUNCE
- `timer % 4 == 0`：对 `affected` 中仍存活的实体播 FX 类型 2（HEAVY_BLEED，`count 14`，位置 = 实体中心）
- 速度曲线（按 `timer = t`）：
  - `t >= 12`：`vy = max(vy, 0.55)`，`vx *= 0.4`，`vz *= 0.4`
  - `t >= 6`：`vy = max(vy, -0.02)`，`vx *= 0.65`，`vz *= 0.65`
  - 否则：`pullPlayerToward(player, target, 2.35)`
- 每 tick `fallDistance = 0`；`timer--`；到 0 → `impact2AndLightning`

### impact2AndLightning（二次冲击）
- 音效 `SoundEvents.field_187539_bB`(0.7,1.2)、FX 类型 3（LIGHTNING_EXPL）、`VENGEANCE_ROCK`(0.9,0.9)、`EXPLOSION_LARGE` ×3
- 目标：伤害 **6.0F**（来源：玩家）❌（工程为 **11.0F**）；`vy = max(vy, 0.35)`
- **AoE**：半径 **3.5**；对每个距目标 `<= 3.5` 的存活生物（排除玩家）：
  - `scale = 1 - dist/3.5`；伤害 `10.0F * scale`（来源：爆炸伤害，玩家为源）
  - 水平击退：沿背离目标方向 `0.85 * scale`；`vy = max(vy, 0.25 + 0.2 * scale)`
- 玩家 `fallDistance = 0` → `lightningStrike`（音效 + 落雷）

### `pullPlayerToward(player, target, speed)` 语义
**直接赋值**速度（不是加速）：
```
to = (target.x - player.x, target.y + target.eyeHeight*0.5 - (player.y + player.eyeHeight), target.z - player.z)
dir = normalize(to)
vx = clamp(dir.x * speed, -2.8, 2.8)
vy = clamp(dir.y * speed, -2.2, 2.2)
vz = clamp(dir.z * speed, -2.8, 2.8)
player.deltaMovement = (vx, vy, vz); player.fallDistance = 0
```
工程用的是 `dash(player, destination, speed)`（1.55 / 1.85），语义与数值均不同。

## 3. 差异汇总表

| 项 | 原件 | 工程现状 | 处理 |
| --- | --- | --- | --- |
| 物品冷却 | 60 tick | 80 | 改 60 |
| 蓄力时长 | 20 | 10 | 改 20 |
| 首次拉近最长 | 40 tick | 12 | 改 40 |
| 接触距离 | 2.2 | 2.5 | 改 2.2 |
| 首次冲击伤害 | **14.0F，半径 5 的 AoE** | 7.0F，仅目标 | 按原件改 |
| 首次冲击弹射 | 沿玩家视线 1.9 前 + 0.65 上 | 无 | 补 |
| 首次冲击 debuff | 对 AoE 内每个实体 100t | 仅目标 | 按原件改 |
| 弹跳时长 | 14 | 8 | 改 14 |
| 弹跳速度曲线 | 三段（0.55 / -0.02 / 2.35 拉近） | 单一 bounce | 按原件改 |
| 二次拉近最长 | 40 tick | 10 | 改 40 |
| 二次冲击目标伤害 | **6.0F** | 11.0F | 改 6.0F |
| 二次冲击 AoE | 半径 3.5，`10.0F*(1-d/3.5)` + 击退 | 无 | 补 |
| 拉近速度 | 1.8（首次）/ 2.35（弹跳末段） | 1.55 / 1.85 | 按原件改 |
| 落雷 | `lightningStrike`（音效 + 落雷） | 仅生成 `LIGHTNING_BOLT` | 按原件核对 |

## 4. 复现与验证建议

- 原件：`_scratch/vf/out109/com/dhanantry/scapeandrunparasites/item/VengeanceGrappleHandler.java`（阶段机在 `onServerTick`，行 150-345）
- 相关网络包：`network/PacketVengeanceFX.java`（4 种 FX：SPARKS / IMPACT_DUST / HEAVY_BLEED / LIGHTNING_EXPL，带 `x,y,z,a,count`）→ 工程需对应的粒子表现
- 验证：速度与 AoE 难以用 GameTest 断言，建议至少静态校验常量与伤害数值，并在真机 `runClient` 观察拉近与两次冲击的手感
