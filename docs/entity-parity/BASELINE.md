# 生物部分还原度基线

> 生成时间：2026-09-25T06:30:01.478Z
> 事实来源：`D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites`（SRParasites 1.10.9，生物部分与 1.10.8 一致）
> 本工程：`D:/code/MC模组/csrp`（MC 26.3 / NeoForge 26.3，分支 `port-26.3`）

## 方法与口径

- 审计单元 = `SRPEntities.CreateEntityMob` 的 127 个注册项，逐生物走完双方继承链（父类共用行为计入该生物）。
- 11 个审计面：注册、属性、AI、行为、伤害/效果、同步数据、动画、模型/贴图、音效、生成、掉落。
- 条款判定 `satisfied` / `partial` / `missing`，`na` 不计入分母；每条判定必须带原版与（满足时的）本工程证据行号。
- 完成度 = (satisfied + 0.5 × partial) / (satisfied + partial + missing)。
- 复跑：`node scripts/entity-parity/build-entity-parity-input.cjs && node scripts/entity-parity/verify-entity-parity.cjs`。

## 总体基线

- 覆盖：**24/127** 只生物已出条款级审计
- 条款：满足 **1202**、部分 **578**、缺失 **348**（另有 100 条判定为不适用）
- **加权完成度：70.1%**

## 分面基线

| 面 | 满足 | 部分 | 缺失 | 完成度 |
| --- | ---: | ---: | ---: | ---: |
| 注册 | 127 | 66 | 53 | 65% |
| 属性 | 174 | 41 | 11 | 86.1% |
| AI | 140 | 134 | 35 | 67% |
| 行为 | 238 | 112 | 69 | 70.2% |
| 伤害/效果 | 159 | 20 | 21 | 84.5% |
| 同步数据 | 46 | 48 | 50 | 48.6% |
| 动画 | 93 | 21 | 9 | 84.1% |
| 模型/贴图 | 79 | 6 | 21 | 77.4% |
| 音效 | 56 | 40 | 30 | 60.3% |
| 生成 | 70 | 51 | 28 | 64.1% |
| 掉落 | 20 | 39 | 21 | 49.4% |

## 完成度最低的 20 只（补齐队列起点）

| id | 原版类 | 工程类 | 完成度 | 满足/部分/缺失 |
| --- | --- | --- | ---: | --- |
| `beckon_siii` | EntityVenkrolSIII | NexusParasiteEntity | 54.9% | 43/38/32 |
| `hi_skeleton` | EntityHiSkeleton | HiSkeletonEntity | 61.4% | 44/25/23 |
| `fer_villager` | EntityFerVillager | FeralParasiteEntity | 61.6% | 67/31/36 |
| `host` | EntityHost | HostEntity | 64.1% | 42/25/18 |
| `sim_sheephead` | EntityInfSheepHead | AssimilatedHeadEntity | 66.3% | 20/17/6 |
| `sim_dragone` | EntityInfDragonE | AssimilatedDragonEntity | 66.7% | 36/24/12 |
| `mar_cow` | EntitySpeCow | MarauderizedCowEntity | 67.3% | 58/24/22 |
| `buglin` | EntityLodo | BuglinEntity | 68.6% | 41/14/15 |
| `pri_longarms` | EntityShyco | LongarmsEntity | 68.6% | 58/28/19 |
| `sim_pig` | EntityInfPig | AssimilatedParasiteEntity | 68.8% | 29/19/8 |
| `sim_pighead` | EntityInfPigHead | AssimilatedHeadEntity | 70% | 32/20/8 |
| `sim_human` | EntityInfHuman | SimHumanEntity | 70.9% | 65/26/19 |
| `sim_squid` | EntityInfSquid | AssimilatedParasiteEntity | 71% | 52/28/13 |
| `sim_adventurer` | EntityInfPlayer | SimAdventurerEntity | 71.4% | 54/22/15 |
| `sim_wolfhead` | EntityInfWolfHead | AssimilatedHeadEntity | 71.7% | 34/18/8 |
| `sim_cowhead` | EntityInfCowHead | AssimilatedHeadEntity | 72.5% | 34/19/7 |
| `sim_wolf` | EntityInfWolf | AssimilatedParasiteEntity | 74.3% | 65/29/13 |
| `sim_bigspider` | EntityDorpa | AssimilatedVariantEntity | 75% | 61/25/12 |
| `sim_cow` | EntityInfCow | AssimilatedParasiteEntity | 75% | 64/31/11 |
| `sim_bear` | EntityInfBear | AssimilatedParasiteEntity | 75.3% | 56/19/12 |

## 已知前提与风险

- 本基线的审计对象是 `csrp` 当前工作树（`port-26.3`）的源码与资源；
- 该分支的 26.3 迁移尚未完成，`gradlew build` 当前失败，因此本轮基线只覆盖「源码与资源层还原度」，不含运行时验证；
- 审计由代理逐条比对得出，`confidence` 字段标注了每只生物的把握程度；低把握条目应在补齐时复核。
