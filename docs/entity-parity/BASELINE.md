# 生物部分还原度基线

> 生成时间：2026-09-25T05:38:32.704Z
> 事实来源：`D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites`（SRParasites 1.10.9，生物部分与 1.10.8 一致）
> 本工程：`D:/code/MC模组/csrp`（MC 26.3 / NeoForge 26.3，分支 `port-26.3`）

## 方法与口径

- 审计单元 = `SRPEntities.CreateEntityMob` 的 127 个注册项，逐生物走完双方继承链（父类共用行为计入该生物）。
- 11 个审计面：注册、属性、AI、行为、伤害/效果、同步数据、动画、模型/贴图、音效、生成、掉落。
- 条款判定 `satisfied` / `partial` / `missing`，`na` 不计入分母；每条判定必须带原版与（满足时的）本工程证据行号。
- 完成度 = (satisfied + 0.5 × partial) / (satisfied + partial + missing)。
- 复跑：`node scripts/entity-parity/build-entity-parity-input.cjs && node scripts/entity-parity/verify-entity-parity.cjs`。

## 总体基线

- 覆盖：**16/127** 只生物已出条款级审计
- 条款：满足 **853**、部分 **419**、缺失 **287**（另有 66 条判定为不适用）
- **加权完成度：68.2%**

## 分面基线

| 面 | 满足 | 部分 | 缺失 | 完成度 |
| --- | ---: | ---: | ---: | ---: |
| 注册 | 81 | 46 | 35 | 64.2% |
| 属性 | 119 | 33 | 4 | 86.9% |
| AI | 100 | 91 | 34 | 64.7% |
| 行为 | 189 | 78 | 71 | 67.5% |
| 伤害/效果 | 118 | 16 | 20 | 81.8% |
| 同步数据 | 31 | 37 | 37 | 47.1% |
| 动画 | 61 | 15 | 8 | 81.5% |
| 模型/贴图 | 52 | 6 | 20 | 70.5% |
| 音效 | 40 | 27 | 21 | 60.8% |
| 生成 | 51 | 40 | 19 | 64.5% |
| 掉落 | 11 | 30 | 18 | 44.1% |

## 完成度最低的 20 只（补齐队列起点）

| id | 原版类 | 工程类 | 完成度 | 满足/部分/缺失 |
| --- | --- | --- | ---: | --- |
| `beckon_siii` | EntityVenkrolSIII | NexusParasiteEntity | 54.9% | 43/38/32 |
| `hi_skeleton` | EntityHiSkeleton | HiSkeletonEntity | 61.4% | 44/25/23 |
| `fer_villager` | EntityFerVillager | FeralParasiteEntity | 61.6% | 67/31/36 |
| `host` | EntityHost | HostEntity | 64.1% | 42/25/18 |
| `sim_pig` | EntityInfPig | AssimilatedParasiteEntity | 67% | 28/19/9 |
| `mar_cow` | EntitySpeCow | MarauderizedCowEntity | 67.3% | 58/24/22 |
| `buglin` | EntityLodo | BuglinEntity | 68.6% | 41/14/15 |
| `pri_longarms` | EntityShyco | LongarmsEntity | 68.6% | 58/28/19 |
| `sim_human` | EntityInfHuman | SimHumanEntity | 70.9% | 65/26/19 |
| `sim_adventurer` | EntityInfPlayer | SimAdventurerEntity | 70.9% | 53/23/15 |
| `sim_squid` | EntityInfSquid | AssimilatedParasiteEntity | 71% | 52/28/13 |
| `sim_villager` | EntityInfVillager | AssimilatedVariantEntity | 71.7% | 52/25/13 |
| `sim_wolf` | EntityInfWolf | AssimilatedParasiteEntity | 72.4% | 63/29/15 |
| `sim_cow` | EntityInfCow | AssimilatedParasiteEntity | 73.1% | 62/31/13 |
| `sim_bigspider` | EntityDorpa | AssimilatedVariantEntity | 74% | 60/25/13 |
| `sim_sheep` | EntityInfSheep | AssimilatedParasiteEntity | 75.2% | 65/28/12 |

## 已知前提与风险

- 本基线的审计对象是 `csrp` 当前工作树（`port-26.3`）的源码与资源；
- 该分支的 26.3 迁移尚未完成，`gradlew build` 当前失败，因此本轮基线只覆盖「源码与资源层还原度」，不含运行时验证；
- 审计由代理逐条比对得出，`confidence` 字段标注了每只生物的把握程度；低把握条目应在补齐时复核。
