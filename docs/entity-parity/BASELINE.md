# 生物部分还原度基线

> 生成时间：2026-09-25T03:15:31.165Z
> 事实来源：`D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites`（SRParasites 1.10.9，生物部分与 1.10.8 一致）
> 本工程：`D:/code/MC模组/csrp`（MC 26.3 / NeoForge 26.3，分支 `port-26.3`）

## 方法与口径

- 审计单元 = `SRPEntities.CreateEntityMob` 的 127 个注册项，逐生物走完双方继承链（父类共用行为计入该生物）。
- 11 个审计面：注册、属性、AI、行为、伤害/效果、同步数据、动画、模型/贴图、音效、生成、掉落。
- 条款判定 `satisfied` / `partial` / `missing`，`na` 不计入分母；每条判定必须带原版与（满足时的）本工程证据行号。
- 完成度 = (satisfied + 0.5 × partial) / (satisfied + partial + missing)。
- 复跑：`node scripts/entity-parity/build-entity-parity-input.cjs && node scripts/entity-parity/verify-entity-parity.cjs`。

## 总体基线

- 覆盖：**13/127** 只生物已出条款级审计
- 条款：满足 **656**、部分 **380**、缺失 **286**（另有 57 条判定为不适用）
- **加权完成度：64%**

## 分面基线

| 面 | 满足 | 部分 | 缺失 | 完成度 |
| --- | ---: | ---: | ---: | ---: |
| 注册 | 61 | 40 | 30 | 61.8% |
| 属性 | 72 | 29 | 24 | 69.2% |
| AI | 79 | 75 | 35 | 61.6% |
| 行为 | 152 | 76 | 72 | 63.3% |
| 伤害/效果 | 102 | 16 | 20 | 79.7% |
| 同步数据 | 26 | 33 | 30 | 47.8% |
| 动画 | 53 | 13 | 7 | 81.5% |
| 模型/贴图 | 37 | 12 | 19 | 63.2% |
| 音效 | 33 | 21 | 18 | 60.4% |
| 生成 | 32 | 39 | 16 | 59.2% |
| 掉落 | 9 | 26 | 15 | 44% |

## 完成度最低的 20 只（补齐队列起点）

| id | 原版类 | 工程类 | 完成度 | 满足/部分/缺失 |
| --- | --- | --- | ---: | --- |
| `beckon_siii` | EntityVenkrolSIII | NexusParasiteEntity | 54% | 42/38/33 |
| `fer_villager` | EntityFerVillager | FeralParasiteEntity | 56.3% | 59/33/42 |
| `hi_skeleton` | EntityHiSkeleton | HiSkeletonEntity | 56.5% | 39/26/27 |
| `host` | EntityHost | HostEntity | 64.1% | 42/25/18 |
| `mar_cow` | EntitySpeCow | MarauderizedCowEntity | 64.4% | 54/26/24 |
| `sim_human` | EntityInfHuman | SimHumanEntity | 65.5% | 57/30/23 |
| `sim_cow` | EntityInfCow | AssimilatedParasiteEntity | 67% | 54/34/18 |
| `pri_longarms` | EntityShyco | LongarmsEntity | 67.1% | 56/29/20 |
| `sim_squid` | EntityInfSquid | AssimilatedParasiteEntity | 67.2% | 47/31/15 |
| `sim_wolf` | EntityInfWolf | AssimilatedParasiteEntity | 67.8% | 57/31/19 |
| `sim_bigspider` | EntityDorpa | AssimilatedVariantEntity | 67.9% | 51/31/16 |
| `buglin` | EntityLodo | BuglinEntity | 67.9% | 40/15/15 |
| `sim_sheep` | EntityInfSheep | AssimilatedParasiteEntity | 70% | 58/31/16 |

## 已知前提与风险

- 本基线的审计对象是 `csrp` 当前工作树（`port-26.3`）的源码与资源；
- 该分支的 26.3 迁移尚未完成，`gradlew build` 当前失败，因此本轮基线只覆盖「源码与资源层还原度」，不含运行时验证；
- 审计由代理逐条比对得出，`confidence` 字段标注了每只生物的把握程度；低把握条目应在补齐时复核。
