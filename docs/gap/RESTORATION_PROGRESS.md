# 原版功能还原进度（1.21.1 / `main`）

> 目标：把 `csrp`（MC 1.21.1 / NeoForge 21.1）补到与 **SRParasites 1.10.9** 功能一致。
> 基准事实源：`D:/code/MC模组/_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites`（原版反编译源码）。
> 缺口来源：`docs/entity-parity/raw/<id>.json`（条款级审计，逐条带原版与本工程证据）。
> 本轮（2026-09-25）：**放弃 26.3 移植线，只在 1.21.1 分支做内容/行为还原**。

## 工作流（每个批次固定四步）

```bash
node scripts/entity-parity/list-gaps.cjs --mob sim_cow --verdict missing   # 1) 选缺口（带原版证据行）
# 2) 查原版源码实现，按 1.21.1 架构落到共享系统（ParasiteCombatEffects / event / 实体基类）
mc_gradle(projectDir="D:/code/MC模组/csrp", task="build")                  # 3) 构建验证
node scripts/run-all-verifications.cjs                                     #    校验脚本无回归
node scripts/entity-parity/mark-restored-clauses.cjs --apply --batch <批次> # 4) 记账
node scripts/entity-parity/build-entity-parity-input.cjs && node scripts/entity-parity/verify-entity-parity.cjs
```

- 记账只对**本批次真正改到的类**生效（`projectClasses` 过滤），避免把其他科的同类条款误判为已完成。
- 审计覆盖 13/127 只生物；未审计生物的同类条款在实现共享系统时一并受益，但不记账。

## 批次 1：同化体通用战斗规则（2026-09-25）

原版出处：`EntityPInfected`（`entity/ai/misc/EntityPInfected.java`）+ `EntityParasiteBase`。
对应 1.21.1 落点：`entity/ParasiteCombatEffects.java`（共享实现）+ 三个同化体类 + 效果事件。

| 原版条款 | 原版行 | 1.21.1 实现 |
| --- | --- | --- |
| `damageCap = SRPConfig.infectedCap`（2）：超限削减并给 RAGE 200/1 | `EntityPInfected.java:87`、`EntityParasiteBase.java:778` | `ParasiteCombatEffects.damageAfterIncomingCap`（`maxHealth/cap + 余数*0.5`、RAGE 200/1、火焰/虚空不设限、`generationProfile.damageCap()` 基因门控） |
| `attackEntityAsMobMinimum(MiniDamage = 0.5)` 穿甲最小伤害 | `EntityParasiteBase.java:858` | `ParasiteCombatEffects.applyMinimumMeleeDamage`（吸收量对半、直接扣血、致死走 `die`） |
| VIRA 等级放大最小伤害 | `EntityParasiteBase.java:878` | 同上：`base * (VIRA 等级 + 2)` |
| `foodSteal`：命中玩家偷食物→`infected_drop` | `EntityParasiteBase.java:958`、`attackEntityAsMobFood` | `ParasiteCombatEffects.stealFoodFromPlayer`（`DataComponents.FOOD` 扫描 + `assimilated_flesh` 掉落） |
| `geneMobHealing`：击杀按 victim 生命回血 | `EntityParasiteBase.java:1079` | `ParasiteCombatEffects.healOnKill`（`generationProfile.mobHealing` × victim 最大生命 × `infectedKillHeal`） |
| 药水免疫：COTH/VIRA/CORRO/DLER 不可施加 | `EntityParasiteBase.java:1084` | `StatusEffectEvents.preventParasiteStatusApplication`（`MobEffectEvent.Applicable` → DO_NOT_APPLY） |

新增配置（`csrp-systems.toml`）：`infectedDamageCap`、`infectedMinimumDamage`、`infectedFoodSteal`、`infectedKillHeal`。
接线类：`AssimilatedParasiteEntity`、`AssimilatedVariantEntity`、`SimHumanEntity`（`hurt` 上限、`doHurtTarget` 最小伤害+偷食物、`killedEntity` 回血）。
校验：`scripts/verify-infected-combat-rules.cjs`；审计记账 30 条（13 只已审计生物中 6 只 `sim_*`，满足条款 493 → 523）。

## 后续批次（按缺口聚类排序）

1. **家族铺开**：把批次 1 的规则接到其余寄生体科（`FeralParasiteEntity`、`Marauderized*`、原始/适应/纯种基类、`AssimilatedEnderman/Dragon/Head` 等）——原版这些规则在 `EntityParasiteBase`，全科共享。
2. **死亡血肉**：`spawnGore`（`goreSim` 方块 + `RemainEntity` 200 tick）、`attackEntityFromEffects`（受击铺 gore）、`attackEntityFromCap`（3 个 `EntityGore` 炸弹）、`selfExplode`（`MOB_EXPLOSION` 音效 + 毒云 + 召唤表）。需要先把 `ModBlocks` 的 gore 变体暴露成公开放置 helper。
3. **同步数据**：`SELFE`（自爆引信）、`COLD_L`（寒冷变体）、`DISLO15`（脱落计数）等 `DataManager` 条目与客户端表现。
4. **AI 补全**：`EntityAISwimmingDiving`、`EntityAIWaterLeapAtTargetStatus`、`EntityAIInfectedSearch`（融化搜索）、Nidus 生成、`doLast` 的 SPOT/alertOthers；对照 `docs/entity-parity/ORIGINAL_AI_TASKS.md`（127 只 × 1254 条注册）。
5. **内容缺口**（`docs/gap/PORT_COMPLETION_PLAN.md`）：残骸/木系楼梯台阶墙、`colonyoutpost`/`relaycontroller`/`noderelay`/`parasitecanister`、`assimilated_blossom` 与花盆、灌木/藤蔓/叶子、gore 六块、`lurecomponent7-10`、客户端体积雾/黑天幕/粒子/GUI。
6. **覆盖审计**：对剩余 114 只生物按 `AUDIT_PROTOCOL.md` 出条款级审计，让缺口清单覆盖全 127 只。

## 批次 2：全科通用战斗规则事件化（2026-09-25）

原版这些规则写在 `EntityParasiteBase`（全科共享），逐科比对的数值取自 `SRPConfig`。
1.21.1 落点改为**事件驱动 + 分科参数表**，一次覆盖所有寄生体科，不再逐科复制：

| 规则 | 原版出处 | 实现 |
| --- | --- | --- |
| 分科伤害上限 + 触顶 RAGE 200/1 | `EntityParasiteBase:706-790`、`SRPConfig.<tier>Cap` | `ParasiteCombatRules.applyDefenseRules`（`maxHealth/cap + 余数*0.5`，火焰不设限，`parasiteCombatTable` 配置：infected 2 / feral 3 / hijacked 5 / assimara 5 / primitive 6 / adapted 9 / pure 13 / preeminent 18 / ancient 5 / derived 25 / nexus 4·8·14·20） |
| 分科穿甲最小伤害（VIRA 放大） | `EntityParasiteBase:858-1002` | `applyAttackRules` → `ParasiteCombatEffects.applyMinimumDamage`（`base × (VIRA等级+2)`） |
| 分科 foodSteal 饥饿 + foodRott 食物转掉落 | `EntityParasiteBase:958-1000` | `ParasiteCombatEffects.stealFood`（`causeFoodExhaustion` + `assimilated_flesh`） |
| 毒伤害（magic, amount==1, 自身中毒）转治疗 | `EntityParasiteBase:1084+`、`genePoisonHealing` | `ParasiteCombatRules.convertPoisonToHealing`（`parasitePoisonHealing` 2.5） |
| 命中 >8 伤害给玩家 FEAR 1..3 级 | `EntityPInfected:168-180` | `ParasiteCombatRules.applyFear` |
| 击杀回血 geneMobHealing | `EntityParasiteBase:1079` | `ParasiteCombatRules.applyKillHeal`（`generationProfile.mobHealing` 门控） |
| 火伤 × firemultyplier(4.0) + 20% RAGE | `EntityParasiteBase:67` | `PrimitiveParasiteEntity.hurt`（Primitive/Crude/Hijacked/Host 系继承）+ `BuglinEntity.hurt` |

批次 1 的类内接线已回退为事件化实现（避免双份结算）。分科解析按注册 id 前缀（`sim_`/`fer_`/`hi_`/`mar_`/`pri_`/`ada_`/`beckon_s*`…），与 `InfectionMechanics` 现有约定一致。
校验：`scripts/verify-parasite-combat-rules.cjs`；审计记账 45 条，满足条款 523 → **568**（缺失 402 → 360）。

**仍未做**（下一批）：gore 血迹方块 / `EntityGore` / `spawnGore`+`RemainEntity` / `selfExplode` 毒云与召唤表、
`setWait(10)`、PVOT 伤害转移、`placeNidus`、`liquidLeap`、`doLast` SPOT/alertOthers、
applyGene/阶段属性加成、同步数据（SELFE/COLD_L/DISLO15）、AI 任务补全与界面/客户端层。
