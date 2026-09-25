# 生物部分还原度基线

> 生成时间：2026-09-25T01:46:19.308Z
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
- 条款：满足 **610**、部分 **388**、缺失 **324**（另有 57 条判定为不适用）
- **加权完成度：60.8%**

## 分面基线

| 面 | 满足 | 部分 | 缺失 | 完成度 |
| --- | ---: | ---: | ---: | ---: |
| 注册 | 61 | 40 | 30 | 61.8% |
| 属性 | 66 | 29 | 30 | 64.4% |
| AI | 70 | 76 | 43 | 57.1% |
| 行为 | 150 | 77 | 73 | 62.8% |
| 伤害/效果 | 92 | 16 | 30 | 72.5% |
| 同步数据 | 16 | 33 | 40 | 36.5% |
| 动画 | 46 | 19 | 8 | 76% |
| 模型/贴图 | 35 | 12 | 21 | 60.3% |
| 音效 | 33 | 21 | 18 | 60.4% |
| 生成 | 32 | 39 | 16 | 59.2% |
| 掉落 | 9 | 26 | 15 | 44% |

## 完成度最低的 20 只（补齐队列起点）

| id | 原版类 | 工程类 | 完成度 | 满足/部分/缺失 |
| --- | --- | --- | ---: | --- |
| `beckon_siii` | EntityVenkrolSIII | NexusParasiteEntity | 52.2% | 40/38/35 |
| `fer_villager` | EntityFerVillager | FeralParasiteEntity | 52.6% | 54/33/47 |
| `hi_skeleton` | EntityHiSkeleton | HiSkeletonEntity | 55.4% | 38/26/28 |
| `pri_longarms` | EntityShyco | LongarmsEntity | 59.5% | 48/29/28 |
| `mar_cow` | EntitySpeCow | MarauderizedCowEntity | 60.6% | 50/26/28 |
| `sim_human` | EntityInfHuman | SimHumanEntity | 61.4% | 51/33/26 |
| `host` | EntityHost | HostEntity | 61.8% | 40/25/20 |
| `sim_squid` | EntityInfSquid | AssimilatedParasiteEntity | 63.4% | 43/32/18 |
| `sim_wolf` | EntityInfWolf | AssimilatedParasiteEntity | 64.5% | 53/32/22 |
| `sim_cow` | EntityInfCow | AssimilatedParasiteEntity | 64.6% | 51/35/20 |
| `sim_bigspider` | EntityDorpa | AssimilatedVariantEntity | 65.3% | 48/32/18 |
| `sim_sheep` | EntityInfSheep | AssimilatedParasiteEntity | 66.7% | 54/32/19 |
| `buglin` | EntityLodo | BuglinEntity | 67.9% | 40/15/15 |

## 已知前提与风险

- 本基线的审计对象是 `csrp` 当前工作树（`port-26.3`）的源码与资源；
- 该分支的 26.3 迁移尚未完成，`gradlew build` 当前失败，因此本轮基线只覆盖「源码与资源层还原度」，不含运行时验证；
- 审计由代理逐条比对得出，`confidence` 字段标注了每只生物的把握程度；低把握条目应在补齐时复核。
