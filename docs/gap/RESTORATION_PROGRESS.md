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

## 批次 5：受击反击 RAGE、AI 定格与击杀再生（2026-09-25 续）

| 条款 | 原版出处 | 1.21.1 实现 |
| --- | --- | --- |
| 受击 20% 概率获得 RAGE 200/1 | `EntityParasiteBase.attackEntityFrom:796`（`rand.nextInt(5)==0`） | `ParasiteCombatRules.applyDefenseRules` 增加 20% 反击 RAGE（全科共享） |
| `EntityAIWait` 等待状态机 + 击杀后 `setWait(10)` | `EntityParasiteBase:155/2573`、击杀钩子 `:1080` | `PrimitiveParasiteEntity` 新增 `waitTicks`/`setWait`/`getWait` 与优先级 0 的 `WaitGoal`（互斥 MOVE/LOOK/JUMP）；击杀钩子在 `applyKillHeal` 中调用 `setWait(10)` |
| `primitiveRegen` 再生（killcount 门控） | `EntityPPrimitive:97-99`、`EntityPFeral:92-107` | `PrimitiveParasiteEntity.tickRegeneration`：每 20 tick 一次，`killcount>1`、非着火、受伤时 `heal(parasiteRegen)`，每 5 次消耗 1 killcount |

新增配置：`parasiteRegen`(4.0)。
覆盖范围：`PrimitiveParasiteEntity` 链（primitive/crude/hijacked/host/pure/preeminent/ancient/derived/deterrent/nexus）；
`FeralParasiteEntity`、`Marauderized*`、`Assimilated*` 各自继承 `Monster`，需要后续单独接线（已在缺口清单中保留）。
校验：`scripts/verify-parasite-wait-regen.cjs`；审计记账 7 条，满足条款 610 → **617**（缺失 324 → 317）。

### 台账修正：PARATE 击杀强化（无新代码）

审计（2026-09-23）把「击杀后用 PARATE 强化生命/护甲/伤害」记为缺失，逐条核对原版
`EntityParasiteBase:1046-1074` 与工程实现后确认**早已实现且语义一致**：
`StatusEffectEvents.absorbParateAttributes` 在击杀时按 `0.5 × (amp + 1)`（原版 `parateMuch = 0.5` × `bonuss`）
把受害者的基础最大生命/护甲/攻击加到击杀者身上。属审计陈旧，已按证据订正 7 条。

## 批次 6：SELFE 自爆引信与闪烁缩放（2026-09-25 续）

原版 `EntityParasiteBase.dyingBurst` / `madeRng` / `getSelfeFlashIntensity`（`fuseTime = 40`）：

| 条款 | 原版出处 | 1.21.1 实现 |
| --- | --- | --- |
| `madeRng`：首次受击掷骰（50%），命中则广播 byte 40 | `EntityParasiteBase:628-632` | `PrimitiveParasiteEntity.willExplodeOnDeath()`（`random.nextInt(2)`，首次受击时掷骰并广播 40） |
| `SELFE` 同步引信状态（默认 -1） | `:135/283/1186-1190` | 新增 `SELFE` 同步数据 + `getSelfeState()`/`setSelfeState()` |
| `dyingBurst(true,1)`：`onDeathUpdate` 期间引信 +1/tick，满 `fuseTime` 后 `selfExplode` | `:1430-1440`、`:1492-1505` | `tickDeath()` 覆写：引信期间持尸（`deathTime` 停在 20），满 40 tick 调 `ParasiteCombatRules.selfExplode` 再走正常死亡 |
| `getSelfeFlashIntensity` 驱动 `preRenderCallback` 膨胀缩放 | `:2177-2179`；`RenderEmanaAdapted:20-28` | `getSelfeFlashIntensity(partial)`（`/(fuseTime-2)` 夹取 0..1）+ `PrimitiveParasiteRenderer.scale` 按原版 `f1/f2/f3` 公式缩放 |

上一批的「死亡即爆」已升级为原版引信流程（非 primitive 链暂仍即时爆炸，待接线）。
校验：`scripts/verify-parasite-selfe-fuse.cjs`；审计记账 3 条，满足条款 624 → **627**（缺失 310 → 307）。

## 批次 7：SELFE 引信铺开到同化/野化系（2026-09-25 续）

原版 `EntityPInfected` 全家族共享同一套引信；上一批只在 `PrimitiveParasiteEntity` 链实现了。本批把引信抽成共享组件并铺开：

| 新增 | 作用 |
| --- | --- |
| `entity/ParasiteFuseState` | 共享引信状态：`SELFE` 同步 accessor（默认 -1）、`madeRng` 首次受击掷骰（`nextInt(2)`，广播 byte 40）、`FUSE_TICKS = 40`、`advance()`、`flashIntensity()`（`/(fuseTime-2)`） |
| `entity/SelfeFuseOwner` | 族类实现的接口：`willExplodeOnDeath` / `startDyingFuse` / `isDyingFuseActive` / `getSelfeFlashIntensity` |
| `client/renderer/SelfeFuseRender` | 共享 `applySwelling`：原版 `preRenderCallback` 的 `f1/f2/f3` 公式，供任意实现类复用 |

接线（各族 `defineSynchedData` 注册同一 accessor + `tickDeath` 持尸引信 + `hurt` 掷骰）：
`PrimitiveParasiteEntity`（重构复用，覆盖 primitive/crude/hijacked/host/pure/preeminent/ancient/derived/deterrent/nexus）、
`AssimilatedParasiteEntity`、`AssimilatedVariantEntity`、`SimHumanEntity`、`FeralParasiteEntity`。
渲染：`PrimitiveParasiteRenderer`、`AssimilatedParasiteRenderer`、`SimHumanRenderer` 接入膨胀缩放。
`ParasiteCombatRules.applyDeathGore` 改为对任意 `SelfeFuseOwner` 交付引信（不再只认 primitive 链）。

未接线：`Marauderized*`（`MarauderRenderer`）与 `TetheredMarauderizedEntity`，对应条款仍记缺失。
校验：`scripts/verify-parasite-selfe-fuse.cjs`（重写为组件 + 五族接线断言）；审计记账 14 条，
满足条款 627 → **641**（缺失 307 → 299，部分 388 → 382）。

## 批次 8：掠夺化族引信订正 + 基础缩放（2026-09-25 续）

上一批记录的「`Marauderized*` 未接线」经核实是**误判**：`MarauderizedParasiteEntity extends
HijackedParasiteEntity extends PrimitiveParasiteEntity`（`entity/HijackedParasiteEntity.java:10`），
该族本就继承 `ParasiteFuseState` 引信与 `SELFE` 同步，其 `hurt` 也经 `super.hurt` 触发首次受击掷骰。
真正缺的只有渲染端，本批补齐并顺带还原基础缩放：

| 项 | 原版出处 | 实现 |
| --- | --- | --- |
| 掠夺化族基础缩放 | `client/renderer/entity/infected/special/RenderSpe*.java`（`f2 * 1.1F`，`RenderSpeBear` 为 `1.3F`） | `PrimitiveParasiteRenderer` / `TetheredMarauderizedRenderer` 新增 `baseScale` 参数；注册处 `mar_cow/mar_human/mar_sheep/mar_villager/mar_enderman` 传 1.1F、`mar_bear` 传 1.3F |
| 束缚型渲染器引信膨胀 | 同族 `preRenderCallback` | `TetheredMarauderizedRenderer.scale` 接入 `SelfeFuseRender.applySwelling` |

注意：原版的 1.1/1.3 基础缩放**每帧都生效**（无引信时 `f=0` → 缩放即 1.1），因此端口按「常驻基础缩放 +
引信膨胀叠加」实现，与原版 `f2 * 1.1F` 等价。hijacked（`RenderHi*`）族原版无缩放调用，未加缩放。
校验：`scripts/verify-parasite-selfe-fuse.cjs` 扩充注册与参数断言；审计记账 2 条，满足 641 → **643**。

## 批次 9（部分）：基因门控接入伤害规则（2026-09-25 续）

原版 `applyGene(boolean[] kool, float[] goon)`（`EntityParasiteBase:266`）与端口的
`EvolutionSystem.GenerationProfile` **字段一一对应**（`kool[0..5]` = mindam/damcap/lookwall/sprinting/
waterleap/specialmove；`goon[0..2]` = poisonHealing/mobHealing/attackSpeed）。本批把其中**伤害相关**的两条接上：

| 原版语义 | 出处 | 端口实现 |
| --- | --- | --- |
| `flagCap = damageCap > 1 && geneDamcap` | `EntityParasiteBase:706` | `ParasiteCombatRules.applyDefenseRules` 的层级伤害上限增加 `GenerationProfile::damageCap` 门控（新增 `generationAllows(...)` helper） |
| `if (!geneMindam) return false;` | `EntityParasiteBase:858-865` | `applyAttackRules` 的最小伤害改为 `GenerationProfile::minimumDamage` 门控后才施加 |
| `geneSpecialmove` 门控技能 | `applyGene` + `getGeneMod(5)` | `SimHumanEntity` 的 `LeapAtTargetGoal` 由无条件注册改为 `canUse` 内查 `specialMoves()` |

**为何本轮不翻转「基因加成 applyGene」条款**：该条款是**捆绑条款**（最小伤害/伤害上限/穿墙/疾跑/水跃/技能/治疗/攻速），
其中 `waterleap`/`specialmove`/`blockSearch` 对应的**能力本体**在同化/野化/掠夺化族里尚不存在（实测 `FeralParasiteEntity`、
`AssimilatedParasiteEntity`、`AssimilatedVariantEntity`、`MarauderizedParasiteEntity` 均无水跃/疾跑/穿墙实现，
仅 `FeralEndermanEntity`/`LongarmsEntity` 等少数子类有）。按记账诚实原则不翻转，待能力本体补齐后一并核对。
校验：`scripts/verify-parasite-combat-rules.cjs` 增加 4 条门控断言（并修正 1 条因改写条件而失配的既有断言）。

## 批次 10：水跃能力本体与 geneWaterleap 门控（2026-09-25 续）

侦察发现端口生成表**缺两行**（原版 `getGeneModi` 返回 10 个布尔：MiniDamage/DamageCap/LookWalls/
Sprinting/**WaterLeap**/SpecialM/Adaptation/BlockSearch/**Residue**/Orbbox，端口只有其中 7 个）：

| 项 | 原版出处 | 实现 |
| --- | --- | --- |
| `generationWaterLeap0..5 = {false,false,false,true,true,true}` | `SRPConfigSystems:1452-1532` | `EvolutionSystem.GENERATION_WATER_LEAP` + `GenerationProfile.waterLeap` |
| `generationResidue0..5 = {false,false,false,false,true,true}` | 同文件 | `GENERATION_RESIDUE` + `GenerationProfile.residue` |
| `EntityAIWaterLeapAtTargetStatus(leaper, leapMotionY, speed, distance, cooldown, jumpDamageRange)` | `entity/ai/EntityAIWaterLeapAtTargetStatus.java` | 新增参数化目标 `entity/WaterLeapAtTargetGoal`：水中/岩浆中或跳跃中触发 → 瞄准 `cooldown` tick 记录目标位置与 `max(0, dy*0.07)` 高度补偿 → 起跳（`speed*0.9 + 现速*0.3`，垂直 `leapMotionY`）→ 落地按 `damageRange` 击退 2.5 并攻击 → 由 `waterLeapEnabled()` 门控 |
| 端口既有 8 个水跃目标未受 gene 门控 | — | 逐个补 `waterLeapEnabled()` 前置检查（AdaptedVariant/Heed/Preeminent/PrimitiveVariant×2/Pure×2/Viscera） |
| pri_longarms 缺失的 `tasks.addTask(2, EntityAIWaterLeapAtTargetStatus(this, 0.7F, 1.5, 3, 20, 0))` | `EntityShyco` 任务表 | `LongarmsEntity.registerGoals` 优先级 2 注册同参数目标 |

说明：原版用 `setParasiteStatus(10/2)` 表示起跳/落地，端口的 `LongarmsEntity` 状态码含义不同（10=冲击波），
故改用共享的 `startSpecialLeapAnimation(...)`（`SPECIAL_LEAP_TICKS` 同步位），避免串味。
校验：新增 `scripts/verify-water-leap-gene.cjs`；审计记账 1 条，满足 643 → **644**。

## 批次 11：疾跑与穿墙 gene 门控 + 远征冲刺目标（2026-09-25 续）

| 项 | 原版语义 | 端口实现 |
| --- | --- | --- |
| `geneSprinting`（kool[3]） | 疾跑加速（端口既有范式 `HeedEntity:374` / `DredgeEntity:433` 用 1.3×） | 新增 `entity/GeneSprintGoal`：目标距离 > 4 格且生成允许时按 1.3× 追近，近了交回近战目标；**同优先级、注册在近战目标之前**（`MeleeAttackGoal.speedModifier` 在 1.21.1 是 private，无法直接改） |
| `generationSprinting` 门控 | `applyGene` | 新增 `PrimitiveParasiteEntity.sprintingEnabled()` |
| `geneLookwall` / `generationBlockSearch` | 破块/找墙 | `tickBlockBreaking` 增加 `blockSearchEnabled()` 门控（此前只受 `canBreakBlocks()` 管，无生成门控）；新增 `blockSearchEnabled()` |
| 三族接线 | — | `MarauderizedParasiteEntity`(3)、`FeralParasiteEntity`(2)、`AssimilatedParasiteEntity`(2) 在近战目标前注册 `GeneSprintGoal` |

**仍未翻转「基因加成 applyGene」捆绑条款**（诚实说明）：本轮补了 `sprinting`/`blockSearch` 两项，但该捆绑条款还缺
**`attackSpeed`（全局未接：`GenerationProfile.attackSpeedMultiplier` 目前 0 个消费点）**、`waterleap`（野化/同化/掠夺化族无对应目标）、
`specialmove`（同上）三项。按记账诚实原则不翻转。
校验：`scripts/verify-parasite-combat-rules.cjs` 增加 10 条断言（两个 helper、破块门控、冲刺目标语义与三族注册顺序）。

## 批次 12：野化族与 sim_human 的水跃任务（2026-09-25 续）

侦察（按原版构造表逐类核对）得到关键结论：**已审计生物里只有 `fer_villager`（`EntityFerVillager:53`）
与 `sim_human`（`EntityInfHuman:119`）原版有水跃**，`sim_cow/sheep/wolf/squid/bear`（0 处）与
`mar_cow`（0 处）原版本就没有——之前把它们的水跃缺口当成待补能力是误判。野化族则是**全族**都有
（`EntityFerBear/Cow/Enderman/Horse/Human/Pig/Sheep/Villager/Wolf` 均注册）。

实现：把 `WaterLeapAtTargetGoal` 从「仅限 primitive 链」推广为任意 `Mob` + 显式 gene 门（新增 6 参构造，
跳跃动画钩子用 `instanceof PrimitiveParasiteEntity` 兜底），然后在 `FeralParasiteEntity`（全族）与
`SimHumanEntity` 按原版参数 `(0.7F, 1.5, 3, 20, 0)` 于优先级 2 注册。
未做：同一只的 `handleWater`/`liquidLeap` 液体命中累积突进（另一套机制），该条款仍记缺失。
校验：`scripts/verify-water-leap-gene.cjs` 更新为通用构造 + 两族注册断言；审计记账 3 条，满足 644 → **647**。

## 批次 13：handleWater 液体命中突进（2026-09-25 续）

原版 `EntityParasiteBase.handleWater(boolean)`（`:462-491`，`liquidLeap` 字段 `:167`，调用点 `:380/:435`）：

| 原版语义 | 实现 |
| --- | --- |
| 在液体中且锁定目标 → `liquidLeap++`（上限 4），按 `srpTicks` 周期判定一次 | `LiquidLeap.accumulate(mob)`，每 20 tick 判定一次 |
| `liquidLeap >= 1` 时逐枚消耗：`geneWaterleap` 开启则停导航、取目标方向突进（潜没时 h=0.1/str=0.5，出水时 h=0.3/str=1.0，水平 `str*0.8 + 现速*0.2`）并 `lookAt` 目标；未开启则只消耗 | `LiquidLeap.spend(mob, gene)`（组件化，任意 `Mob` 可用） |

接线：`PrimitiveParasiteEntity`（primitive/劫持/掠夺化链）与 `FeralParasiteEntity`（野化族）各持一份并在 `tick` 中驱动；
gene 门分别用 `waterLeapEnabled()` 与 `generationProfile(...).waterLeap()`。
校验：`scripts/verify-water-leap-gene.cjs` 增加 8 条断言；审计记账 1 条，满足 647 → **648**。

## 批次 14：geneAttackSpeed + geneSprinting 合并的近战目标（2026-09-25 续）

原版 `getAttackSpeed() = (int)(attackSpeedT * geneAttackSpeed)`（`EntityParasiteBase:233`）。端口的
`GENERATION_ATTACK_SPEED = {1.0,1.0,1.0,0.9,0.7,0.5}` 此前**零消费点**；而这三族的近战走原版
`MeleeAttackGoal`，其 `speedModifier` 与攻击计时字段在 1.21.1 **均为 private**，无注入点。

实现 `entity/GeneMeleeGoal`：把「追击 + 到范围攻击 + 间隔」自持一份——基础间隔 20 tick × 生成倍率
（`Math.max(1, round(20 * multiplier))`），并把疾跑 gene（>4 格时 1.3×）收编进同一个目标，
因此野化族的 `GeneSprintGoal` + `MeleeAttackGoal` 两个注册被它一个取代。
接线：`FeralParasiteEntity`（野化族，含已审计的 `fer_villager`）。
待接线：`MarauderizedParasiteEntity`、`AssimilatedParasiteEntity`（仍用 `GeneSprintGoal` + 原版近战）。
校验：`scripts/verify-parasite-combat-rules.cjs` 增 7 条断言（间隔缩放、旗标、攻击门槛、疾跑并入）。

## 批次 15：GeneMeleeGoal 铺开到掠夺化与同化族（2026-09-25 续）

把批次 14 的 `GeneMeleeGoal` 接到剩余两族，三族近战现已统一走 gene 感知循环：
`FeralParasiteEntity`(2)、`MarauderizedParasiteEntity`(3)、`AssimilatedParasiteEntity`(2) —— 各自的
`GeneSprintGoal` + 原版 `MeleeAttackGoal` 双注册被单个目标取代（疾跑与攻击间隔同处一门）。
顺带清掉三处因替换而失效的 `MeleeAttackGoal` import，并订正 `verify-marauderized-port.cjs` 的清单断言
（"MeleeAttackGoal" → "GeneMeleeGoal"）——这类「改代码不带改断言」正是静态守卫抓出来的。
`GeneSprintGoal` 保留（仅疾跑的场景仍可复用），当前无调用点。
校验：`verify-parasite-combat-rules.cjs` 两族断言改为 gene 目标；全套 99 脚本失败集合回到既有 20 个。

## 批次 16：EntityAIJumping 跳跃 AI（2026-09-25 续）

原版 `EntityParasiteBase.EntityAIJumping`（`:2525-2562`，`func_75248_a(4)` 互斥 JUMP）：

| 原版语义 | 实现 |
| --- | --- |
| 每 10 tick 检查一次（`secs`） | `CHECK_INTERVAL_TICKS = 10` |
| 目标平方距离 < 4.0 且 `target.y - (mob.y + eyeHeight) > 1.0` 且在地面 | 同门限照搬（含原版「用目标自身 Y 参与距离」的写法） |
| 停导航 + 起跳：垂直 `0.2 + 高*0.15`，水平 `dx/f * 0.5*0.8 + 现速*0.2` | `entity/JumpAtHigherTargetGoal`，沿原版在 `canUse` 内执行并返回 false 的形态（周期性触发器而非持续任务） |

接线：`LongarmsEntity` 优先级 5（原版 `tasks.addTask(5, this.jumpT)`）。
校验：`verify-parasite-combat-rules.cjs` 增 6 条断言；审计记账 1 条，满足 648 → **649**。

## 批次 17：清理死代码 + gene 覆盖证据表（2026-09-25 续）

`GeneSprintGoal`（批次 11 引入）在三族陆续改用 `GeneMeleeGoal`（批次 14/15）后**已无任何调用点**，
本批删除该类与其 5 条断言，并删除已消费的一次性 codemod `scripts/port263/extend-combat-verify.cjs`。

### applyGene 子项覆盖证据表（供下一轮据此翻转 8 条捆绑条款）

| 子项 | 原版出处 | fer_* | sim_* | mar_* | 说明 |
| --- | --- | --- | --- | --- | --- |
| 最小伤害 geneMindam | `EntityParasiteBase:858` | ✔ | ✔ | ✔ | `ParasiteCombatRules` 全局门控（批次 9） |
| 伤害上限 geneDamcap | `:706` | ✔ | ✔ | ✔ | 同上 |
| 击杀治疗 geneMobHealing | `:1046+` | ✔ | ✔ | ✔ | `healOnKill` 用 `GenerationProfile.mobHealing` |
| 毒伤治疗 genePoisonHealing | `applyGene` | ✔ | ✔ | ✔ | `convertPoisonToHealing` |
| 疾跑 geneSprinting | `kool[3]` | ✔ | ✔ | ✔ | `GeneMeleeGoal`（批次 12-15） |
| 攻击速度 geneAttackSpeed | `:233` | ✔ | ✔ | ✔ | 同上（间隔 = 20 × 生成倍率） |
| 水跃 geneWaterleap | `kool[4]` | ✔ | ✔(sim_human) | ✗ | 原版 sim_cow 等与 mar_cow **本就无水跃任务**，非缺口 |
| 穿墙/破块 geneLookwall+blockSearch | `kool[2]` | ✗ | ✗ | ✔(mar_cow 有破块表) | 野化/同化族继承 `Monster`，需自持破块逻辑 |
| 技能 geneSpecialmove | `kool[5]` | ✗ | 部分(sim_human 跳跃) | ✗ | 缺技能本体 |

结论：`sim_cow/sheep/wolf/squid/bigspider` 一族当前只差 `blockSearch` 与 `specialmove`；
补齐这两项即可一次翻转 7 条（`sim_*`×6 + `fer_villager`；`mar_cow` 另需 specialmove）。
校验：全套 99 脚本失败集合仍为既有 20 个；`build` 通过。

## 批次 18：gene 捆绑条款的真实阻塞项定位（2026-09-25 续，侦察+记账无代码）

按原版逐类核对 `EntityAIBlockLight` / `canLookWall` / `geneLookwall` 的出现次数：

```
EntityInfCow: 0    EntityInfHuman: 0    EntitySpeCow: 0    EntityFerVillager: 0
```

即**「穿墙/破块」子项对这些族不适用**（原版就没这个任务），与之前水跃的结论同类——属于此前审计把通用 gene
描述当成了逐项义务。据此，gene 覆盖表的实际阻塞项收敛为：

| 生物 | 原版任务表（`ORIGINAL_AI_TASKS.md`） | 唯一缺口 |
| --- | --- | --- |
| `sim_cow` | `EntityAISwimmingDiving`(0)、**`EntityAISkill`(2)**、`EntityAIAttackMeleeStatus`(3)、`EntityAIInfectedSearch`(3,cond)、`EntityAIGetFollowers`(6)、`EntityAILookIdle`(8) | `EntityAISkill`（技能本体） |
| `sim_human` | 同上结构 | `EntityAISkill` |
| `fer_villager` / `mar_cow` | 同族结构 | `EntityAISkill` |

结论：**补齐 `EntityAISkill` 即可一次翻转 7 条 gene 捆绑条款**（`sim_*`×6 + `fer_villager`）。
下一轮实施顺序建议：先读原版 `EntityAISkill`（`entity/ai/EntityAISkill.java`）与其在各 `EntityInf*` 的构造参数，
再决定是复用端口既有的技能/冲锋实现（如 `AssimilatedParasiteEntity` 的 `CowChargeGoal`）还是新建共享技能目标。
本轮为侦察与证据落账，未改代码，账面不变（满足 649 / 缺失 293）。
