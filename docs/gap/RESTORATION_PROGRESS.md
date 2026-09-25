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

## 批次 3：动画剪辑解析修复（行走动画缺失，2026-09-25）

**现象**：原始召唤兽（`pri_summoner`）等生物完全没有动画（含行走），呈现"贴地滑动"。

**根因**（两层，逐一定位）：
1. `ParasiteAnimations.usesShortAnimationKeys` 把 `pri_summoner` 当成"短键资源"，于是
   `func_78087_a.limb_swing` 被原样返回；但它的剪辑是全限定名
   （`animation.pri_summoner.func_78087_a.limb_swing`，见 `assets/csrp/tabula/pri_summoner.tbl`
   内嵌 `animations.json` 的 6 个键）→ `LegacyAnimationLibrary.findClip` 精确查不到、
   末段回退（`limb_swing`）也查不到 → 静默不播放任何剪辑。
   旁证：`scripts/verify-entity-animation-contracts.cjs` 里同一份解析镜像**本来就没有**
   pri_summoner，说明是 Java 与设计不一致（该脚本一直通过）。
2. `findClip` 只有"精确 + 最后一段"两种查法：既不能把裸名请求匹配到全限定键，
   也不能把未转写的状态剪辑（例如 `...limb_swing.get_parasite_status_3`）降级到基础剪辑，
   于是一旦转写不全就整只生物无动画。

**修复**：
- `ParasiteAnimations`：`pri_summoner` 移出短键集合（补注释说明原因）；短键资源新增
  legacy→短名翻译（`func_78087_a.limb_swing*`→`walk`、`func_78087_a.age_in_ticks*`→`idle`、
  `get_attack_timer*`→`attack`），修复 `abo_head`。
- `LegacyAnimationLibrary.findClip`：新增两趟兼容查找——①请求是资源键后缀时命中
  （裸名↔全限定名互通）；②逐级去掉请求尾段（`a.b.c`→`a.b`→`a`）直到命中已转写的基础剪辑。
  缺剪辑的状态不再"冻结"，而是回退到最近的基础剪辑。

**效果**（`scripts/audit-animation-clips.cjs`，257 条请求）：
- 未解析请求 **82 → 40**，受影响实体 **13 → 5**；
  `pri_summoner` 6/6 → **0**、`abo_head` 7 条 → **0**；
  `fer_wolf`/`worm`/`anc_overlord`/`warden`/`sim_wolfhead` 的状态剪辑缺失改为降级命中；
  `sim_dragonhead` 走 `sim_dragonehead` 资源名互通。
- 校验：新增 `scripts/verify-animation-clip-resolution.cjs`（把"pri_summoner 不得进入短键集合"
  写成回归断言）；`run-all-verifications` 失败集合仍为既有 20 个。

**剩余真实数据缺口**（转写不全，需要补资源而非改代码）：

| 实体 | 已有剪辑 | 缺口 |
| --- | ---: | --- |
| `ada_yelloweye` | 2 | 无 `func_78087_a.limb_swing`（行走）、`get_attack_timer`、`get_dig_model` 等 20 条 |
| `pri_yelloweye` | 1 | 只有 `age_in_ticks`，缺行走/攻击/掘地 12 条 |
| `rooterball` | 0 | 完全没有 `animations.json`（6 条请求全缺） |
| `sim_squid` | 1 | 只有 `age_in_ticks`，缺 `func_78087_a.limb_swing` |
| `wraith` | 2 | 缺 `func_78087_a.limb_swing` |

补法：从 `_srp-orig/decomp-1.10.9` 的对应模型类（`client/model/Model*.java` 的
`setRotationAngles` 肢体摆动公式）补转 `.tbl` 内嵌 `animations.json` 的缺失剪辑，
与现有转写管线（`scripts/modelrenderer-to-gecko.cjs` / `convert-geo-to-tabula.cjs`）保持一致。

### 批次 3 收尾：剩余 5 只经原版源码证实「原版就没有该动画」（2026-09-25 续）

用原版反编译模型类逐只核对 `func_78087_a`（`setRotationAngles`）方法体：

| 实体 | 原版模型 | 方法体事实 | 结论 |
| --- | --- | --- | --- |
| `pri_yelloweye` | `ModelEmana` | 只有 `ageInTicks` 驱动触须；`limbSwing` 仅出现在签名 | 原版无行走循环 |
| `ada_yelloweye` | `ModelEmanaAdapted` | 同上（触须 + 关节归零后按年龄摆动），无 attack/dig 姿态 | 原版无行走/攻击/掘地姿态 |
| `sim_squid` | `ModelInfSquid` | 只有 `ageInTicks` | 原版无行走循环 |
| `wraith` | `ModelElvia` | 只有 `ageInTicks` | 原版无行走循环 |
| `rooterball` | `ModelLeemB` | `func_78087_a` **空方法** | 原版完全无动画 |

因此不再补写剪辑（补写等于凭空捏造），改为让解析器按原版语义降级：
- `findClip`：行走请求缺剪辑时回退到同实体的 `func_78087_a.age_in_ticks`；
- 新增 `findAgePoseFallback`：`idle./walk./fly./run.` 这类别名请求回退到该实体的 age 姿态，并保留
  `get_parasite_status_N` / `is_screaming_N` / `get_still_ani_N` 后缀。

**动画章结项证据**：`scripts/audit-animation-clips.cjs` 扫描全部已注册实体 → 257 条请求
**0 条真实缺口**，其中 27 条经原版模型源码判定为「原版从未动画该姿态」。
审计脚本现在自带原版证据（`Model<内部名>.java` 的 `func_78087_a` 方法体 + 姿态输入判定），
不再把这类请求误报为缺失。

## 批次 4：死亡血肉与自爆（2026-09-25 续）

原版 `EntityParasiteBase.spawnGore` / `attackEntityFromEffects` / `attackEntityFromCap` / `selfExplode`
（受感染科在 `EntityPInfected` 覆写）：

| 条款 | 原版出处 | 1.21.1 实现 |
| --- | --- | --- |
| `spawnGore`：BIG 血迹方块 + `EntityRemain`（goal = 20×`infectedRemainValue`）+ 3 个血肉弹 | `EntityPInfected:319-339` | `ParasiteCombatRules.leaveGore`（死亡时） |
| `attackEntityFromEffects(range,count)`：按 `paraGore` 铺血迹方块 | `EntityPInfected:264-291` | `ModBlocks.placeGore`（按科选 goresim/gorepri/goreada/gorepur/gorefer/goremar）+ 受击 10% 铺 flat |
| `attackEntityFromCap(go)`：抛 go 个 type 1 `EntityGore` | `EntityPInfected:293-317` | `spawnGoreBombs`（触顶 30% 抛 1 个、死亡抛 3 个，带随机初速） |
| `selfExplode`：`MOB_EXPLOTION` + `EntityToxicCloud`（半径 width×1.5、waitTime 10、时长减半、中毒 300、COTH 3600） | `EntityParasiteBase.selfExplode` | `ParasiteCombatRules.selfExplode`（死亡 50% 触发） |

新增配置：`parasiteGore`(true)、`parasiteRemainValue`(10)、`parasiteSelfExplodeChance`(0.5)。
差异说明：①原版的 40 tick 引信（`dyingBurst`）未做，改为死亡即爆；②毒云 COTH 图标按本工程约定保持可见
（`verify-coth-visibility.cjs` 全局禁止 `visible=false`，原版是隐藏的）；③未接 `selfExplode` 的额外召唤表与
`EntityAta`/`worldMobCap` 逻辑（对应条款仍记为缺失）。
校验：`scripts/verify-parasite-gore.cjs`；审计记账 42 条，满足条款 568 → **611**（缺失 360 → 323）。
