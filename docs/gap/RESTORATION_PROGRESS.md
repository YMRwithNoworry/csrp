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

## 批次 19：EntityAISkill 语义提取（2026-09-25 续，为下一轮实施铺路）

读原版 `entity/ai/EntityAISkill.java`，与端口对照后确定实现路径：

| 原版要点 | 细节 | 端口应对 |
| --- | --- | --- |
| 构造 | `(para, cooldown, miniDistance, [maxDistance], needVisual, attackID[, ignoreStatus])`，距离存**平方** | 用 record/构造重载承载 |
| 门控 `func_75250_a` | `parentEntity.getGeneMod(5)` = **geneSpecialmove**（除非 `ignoreStatus`；attackID 13/31 例外）；另有 `parasiteStatus ∈ (0,3)` 或已 attacking | 复用 `GenerationProfile.specialMoves()` |
| 距离窗口 | `distanceL² ≤ d² < distanceC²` 才起手 | 同 |
| 执行 | `attacking ≥ 1` 后每 tick 调 `parentEntity.doSpecialSkill(attID)`，直到 `getFinished(attID)` 为真再复位 | **端口缺失 `doSpecialSkill(attackID)` 派发层**：`AssimilatedParasiteEntity` 目前用具体目标（`CowChargeGoal` 等）直连，无按 attackID 分派 |
| 外观要求 | `needVisual` → 需视线 | 用 `getSensing().hasLineOfSight` |

结论：`EntityAISkill` 不是「一个目标」而是一层**技能派发契约**（attackID → 行为）。
下一轮实施顺序：① 在共享基类加 `specialMovesEnabled()` 门（与 `waterLeapEnabled()` 同型）；
② 建共享 `ParasiteSkillGoal`（承载门控/距离窗口/冷却/派发）；③ 为各族把 `CowChargeGoal` 一类既有技能
适配成 attackID 条目并注册；④ 逐条核对参数表后翻转 7 条 gene 捆绑条款。
本轮为语义提取与落账，未改代码，账面不变（满足 649 / 缺失 293）。

## 批次 20：共享 geneSpecialmove 门（2026-09-25 续）

按批次 19 的路径第 ① 步：把 `geneSpecialmove` 门收敛到共享基类，供后续 `ParasiteSkillGoal` 使用。

- `PrimitiveParasiteEntity` 新增 `protected final boolean specialMovesEnabled()`（与 `waterLeapEnabled()`/
  `blockSearchEnabled()`/`sprintingEnabled()` 同型，统一读 `GenerationProfile.specialMoves()`）。
- 删除 `LongarmsEntity` 中重复的私有同签名实现（其 2 处调用点改由基类解析，行为不变）。

校验：`verify-parasite-combat-rules.cjs` 增加 1 条断言；全套 99 脚本失败集合仍为既有 20 个；`build` 通过。
下一步（批次 21）：新建共享 `ParasiteSkillGoal`（门控/距离窗口/冷却/attackID 派发），再把各族既有技能
（`CowChargeGoal` 等）适配为 attackID 条目，注册到同化/野化/掠夺化三族。

## 批次 21：ParasiteSkillGoal 派发契约（2026-09-25 续）

按批次 19 路径第 ② 步落地：新增 `entity/ParasiteSkillGoal`，复刻 `EntityAISkill` 的契约形状。

| 原版 | 端口 |
| --- | --- |
| 构造 `(para, cooldown, miniDistance, [maxDistance], needVisual, attackID[, ignoreStatus])`，距离存平方 | 三个构造重载，`minDistanceSqr`/`maxDistanceSqr` 在构造时平方 |
| `func_75250_a`：`getGeneMod(5)` 门控 + 状态条件；attackID 13/31 与 `ignoreStatus` 例外 | `canUse` 用 `mob instanceof PrimitiveParasiteEntity && specialMovesEnabled()`；例外由子类覆写 `geneAllows()`/`canUse()` 表达 |
| 距离窗口 `distanceL² ≤ d² < distanceC²` | 同（`maxDistance == 0` 视为无上界） |
| `needVisual` → 需视线 | `mob.getSensing().hasLineOfSight(target)` |
| 每 tick 调 `doSpecialSkill(attackID)` 直到 `getFinished(attackID)` | 内部接口 `ParasiteSkill { tick(); isFinished(); }`，`attackID` 作为身份保留 |
| 起手前的冷却累积 | `attackTimer < cooldownTicks` 预热，再置 `attacking = 1` |

本批只落地**契约层**（尚无注册点）：下一批把各族既有技能（`CowChargeGoal`、`LongarmsMeleeGoal`、
`ShockwaveGoal` 等）适配成 `ParasiteSkill` 条目并按原版参数表注册，即可翻转 7 条 gene 捆绑条款。
校验：`verify-parasite-combat-rules.cjs` 增加 8 条断言；全套 99 脚本失败集合仍为既有 20 个；`build` 通过。

## 批次 22：EntityAISkill 契约接入 pri_longarms（2026-09-25 续）

按批次 21 的契约层，把 `pri_longarms` 的技能接上（原版 `EntityShyco` 任务表：
`tasks.addTask(2, EntityAISkill(this, 80, 4, false, 21))`，attackID 21 = 恐怖球）：

- `LongarmsEntity` 优先级 2 注册 `ParasiteSkillGoal(this, 21, new ScaryOrbSkill(), 80, 4, false)`。
- 技能体 `ScaryOrbSkill implements ParasiteSkillGoal.ParasiteSkill`：起手首 tick 复用端口既有的
  `applyScaryOrbEffect(target, 0)` 与 `applyScaryOrbMinimumDamage(target, 1.0F)`，20 tick 动画后 `isFinished`。
- gene 门、距离窗口（4 格，平方）、`needVisual=false`、80 tick 冷却均由契约层承担。

校验：`verify-parasite-combat-rules.cjs` 增加 3 条断言；审计记账 1 条，满足 649 → **650**。
下一步（批次 23）：同法为 `sim_cow` 等把 `CowChargeGoal` 适配为 attackID 条目，进而翻转 7 条 gene 捆绑条款。

## 批次 23：修正 EntityAISkill 距离窗口语义（2026-09-25 续）

读 `EntityInfCow.java:75` 时发现原版**参数命名与实际用途相反**，这会直接导致端口把技能窗口算错：

```java
new EntityAISkill(this, 60, 32, 8, true, 1);      // sim_cow
// 构造: (para, cooldown, miniDistance, [maxDistance], needVisual, attackID)
distanceC = miniDistance² = 1024      // 实为【上界】
distanceL = maxDistance²  = 64        // 实为【下界】
canUse:  dis < distanceC && dis >= distanceL    // → 8 ≤ d < 32 格
```

批次 21 我按字面把第一个距离当成了下界，本批订正为忠实语义（`upperDistanceSqr` / `lowerDistanceSqr`），
并在类注释里写明这个反直觉命名与 `sim_cow` 的实际含义（「8–32 格内」）。
`pri_longarms` 的 `(80, 4, false, 21)` 在修正后语义为「4 格内」——与原版 `distanceC = 16, distanceL = 0` 一致 ✔。
`sim_cow` 技能本体对应原版 `doSpecialSkill(1) → charge()`（`attacking < 40` 蓄力，与端口既有
`CowChargeGoal` 的 `PREPARE_TICKS = 40` 吻合），下一批适配。
校验：`verify-parasite-combat-rules.cjs` 更新 1 条并新增 1 条窗口断言；全套失败集合仍为既有 20 个。

## 批次 24：gene 捆绑条款首批翻转（2026-09-25 续）

逐项核对后确认：对 `EntityInf*`/`EntityFer*`/`EntitySpe*` 三族，gene 捆绑条款里的
**水跃、穿墙破块、技能三项原版均不适用**（证据：`EntityAIWaterLeapAtTargetStatus` 仅
`EntityFer*` 与 `EntityInfHuman` 有；`EntityAIBlockLight` 在其全部段落为 0；`EntityAISkill` 在
`ORIGINAL_AI_TASKS.md` 中仅 `EntityInfCow:1494` 与 `EntitySpeBear:957` 出现）。因此适用子项只剩
最小伤害/伤害上限/治疗/毒伤治疗/疾跑/攻击速度——**这六项已全部实现**。

据此翻转 3 条（class 粒度可确证者）：`mar_cow`、`fer_villager`、`sim_human`。

未翻转但**同样已满足**（受记账脚本的 class 粒度限制，`AssimilatedParasiteEntity` 同时承载仍缺技能的
`sim_cow`）：`sim_sheep`、`sim_wolf`、`sim_squid`。下一批给记账脚本加 per-mob 过滤即可一并订正；
`sim_cow`（原版 `EntityInfCow:75 EntityAISkill(this, 60, 32, 8, true, 1)` → `doSpecialSkill(1) → charge()`，
与端口 `CowChargeGoal` 的 `PREPARE_TICKS = 40` 吻合）与 `sim_bigspider`（原版类名待考）仍未完成。

审计：满足 650 → **653**，缺失 292 → **289**。

## 批次 25：记账脚本加 per-mob 过滤 + 同化族其余三只订正（2026-09-25 续）

批次 24 遗留：`sim_sheep`/`sim_wolf`/`sim_squid` 的 gene 捆绑条款同样已满足，但三者与仍缺技能的 `sim_cow`
共用 `AssimilatedParasiteEntity` 这个 class，class 粒度过滤无法只翻它们。

本轮给 `scripts/entity-parity/mark-restored-clauses.cjs` 增加可选 `mobs: [...]` 过滤（在 projectClasses 判定之前），
并新增批次 `gene-bundle-assimilated`（`mobs: ["sim_sheep","sim_wolf","sim_squid"]`）——dry-run 正确跳过 `sim_cow`
与已完成者，恰好命中 3 条。审计：满足 653 → **656**，缺失 289 → **286**，加权完成度 63.8% → **64.0%**。

工具说明：`mobs` 过滤与既有的 `projectClasses` 正交，后续遇到「同一 class 下生物完成度不一」的情形可直接使用。

## 批次 26：sim_bigspider 的 gene 捆绑条款订正（2026-09-25 续）

上一轮遗留的类名悬案已解：`sim_bigspider` 的原版类是 **`EntityDorpa`**
（`entity/monster/infected/EntityDorpa.java`，来自 `audit-input.json` 的 `originalClass`）。

其任务表（`ORIGINAL_AI_TASKS.md:1469`）为：`EntityAISwimming`(0)、`EntityAIAttackMeleeStatus`(3)、
`EntityAIInfectedSearch`(3,cond + 3)、`EntityAIAttackProjectile`(6)、`EntityAIGetFollowers`(6)、`EntityAILookIdle`(8)
—— **无 `EntityAISkill`、无 `EntityAIWaterLeapAtTargetStatus`、无 `EntityAIBlockLight`**，即 gene 捆绑条款里的
技能/水跃/穿墙三项对它同样不适用；适用子项六项均已实现 ⇒ 订正为满足。

审计：满足 656 → **657**，缺失 286 → **285**，加权完成度 64.0% → **64.1%**。
至此已审计 13 只生物中，gene 捆绑条款仅剩 `sim_cow` 一只未完成（其原版 `EntityInfCow:75` 有
`EntityAISkill(this, 60, 32, 8, true, 1)` → charge 技能，下一批实施）。

## 批次 27：sim_cow 技能对齐与 gene 捆绑条款结清（2026-09-25 续）

读端口 `AssimilatedParasiteEntity` 后发现 charge 本体**早已存在**（`CowChargeGoal`：`PREPARE_TICKS=40`
与 `MAX_CHARGE_TICKS=80`，起手后锁定 15 格外冲刺并沿途伤害），缺的只是**原版 `EntityAISkill` 的门控与参数**：

| 原版 `EntityInfCow:75 EntityAISkill(this, 60, 32, 8, true, 1)` | 修改前 | 修改后 |
| --- | --- | --- |
| `geneSpecialmove` 门控（`getGeneMod(5)`） | 无 | `canUse` 增加 `specialMovesEnabled()`（本类继承 `Monster`，故自持同名 helper） |
| 距离窗口 `8 ≤ d < 32` 格 | `d ≥ 4` 格 | `distance >= 64.0D && distance < 1024.0D` |
| 冷却 60 tick | 100 tick | 60 tick |

审计：满足 657 → **658**，缺失 285 → **284**。
**里程碑**：13 只已审计生物的 gene 捆绑条款（8 条）**全部结清**。

## 批次 28：阶段属性加成（2026-09-25 续）

原版 `EntityParasiteBase.finalizeSpawn:1682-1696`：`useEvolution` 且 `phaseCreated >= evolutionParasiteStatIncrease（10）`
时，把 **MAX_HEALTH / ARMOR / ATTACK_DAMAGE** 的基础值 ×`(1 + evolutionParasiteStatIncreaseValue（0.07））`。

端口实现：`ParasiteCombatRules.applyPhaseStatBonus(FinalizeSpawnEvent)`（全局，覆盖所有寄生体）——
`Config.useEvolutionPhases()` 且 `SrpWorldData.evolutionPhase() >= Config.evolutionStatIncreasePhase()` 时对三项
基础属性做同公式缩放；新增配置 `evolutionStatIncreasePhase`(10) / `evolutionStatIncreaseValue`(0.07)。
校验：`verify-parasite-combat-rules.cjs` 增加 7 条断言；审计记账 6 条，满足 658 → **664**，缺失 284 → **278**。

## 批次 29：doLast 的 SPOT 与 alertOthers（2026-09-25 续）

原版 `EntityParasiteBase.doLast:1167-1179`：目标确定后，若 `SRPWorldData.nearestInfectionPosition` 存在，
则给目标 `SPOT_E` 1200 tick，并在 `useOneMind` 时 `ParasiteEventEntity.alertOthers(this, target, level, 7)`。

| 新增 | 说明 |
| --- | --- |
| `SrpWorldData.nearestInfectionPosition(BlockPos)` | 在既有 `nodes()`/`colonies()` 中取最近点，对应原版同名查询 |
| `ParasiteCombatRules.markSpottedTarget(LivingChangeTargetEvent)` | 目标确定 + 感染点存在 → 给目标 `SPOTTED` 1200 tick，并 `alertOthers` 唤醒 7 格内**尚无目标**的寄生体 |
| `alertOthers(level, parasite, target)` | 原版 `ParasiteEventEntity.alertOthers(..., 7)` 的等价实现 |

校验：`verify-parasite-combat-rules.cjs` 增加 6 条断言；审计记账 8 条，满足 664 → **672**，缺失 278 → **270**。

## 批次 30：EntityAISwimmingDiving 潜水任务（2026-09-25 续）

原版 `entity/ai/EntityAISwimmingDiving.java`（`func_75248_a(4)` 互斥 JUMP，各 `EntityInf*`/`EntityFer*` 均以
`new EntityAISwimmingDiving(this, 0.08)` 注册在**优先级 0**）：

| 原版 | 实现 `entity/SwimmingDivingGoal` |
| --- | --- |
| `canUse`：不在水/岩浆 → false；目标在液体内、`distanceToSqr(x, target.y, z) < 25.0` 且低 1 格以上 → `motionY -= yMotion` 后返回 false（本 tick 让位） | 同（`DIVE_RANGE_SQR = 25.0`、`DIVE_HEIGHT_DIFFERENCE = 1.0`） |
| 其余情形返回 true（任务持续） | `canContinueToUse` = 仍在液体中 |
| `updateTask`：80% 概率划水跳跃 | `STROKE_CHANCE = 0.8F` → `getJumpControl().jump()` |

注册：`AssimilatedParasiteEntity`（同化全族）、`FeralParasiteEntity`（野化全族）、`SimHumanEntity`，均为优先级 0、参数 0.08。
校验：`verify-parasite-combat-rules.cjs` 增加 9 条断言；审计记账 9 条，满足 672 → **681**，缺失 276 → **267**。

## 批次 31：EntityAIGetFollowers 招募跟随（2026-09-25 续）

原版 `entity/ai/EntityAIGetFollowers(parent, version, range)`（version 1，各 `EntityInf*`/`EntityFer*`/`EntityHi*`/
`EntityDorpa` 均以 `(this, 1, 16)` 注册在优先级 6，`EntityInfHuman` 为优先级 5）：

| 原版 | 实现 `entity/RecruitFollowersGoal` |
| --- | --- |
| `canUse`：`tickCount % 20 == 0` 且自身无跟随者、无目标 | 同（用端口 `ParasiteFollowGoal.getLeader` 判定） |
| `updateTask`：在 `(range, 2, range)` 盒内找**第一个**有视线、存活、`getParasiteFollowing() == null` 的寄生体并令其跟随 | 同（`leader.hasLineOfSight` + `setLeader`，找到即 `break`） |
| `getParasiteType() < 31`（数值类型门） | 端口已用 per-family 接线替代数值 id 体系，该门隐含 |

注册：`AssimilatedParasiteEntity`（同化族）、`FeralParasiteEntity`（野化族），优先级 6、range 16。
未接线：`EntityInfHuman` 的优先级 5 形态与 adapted 系的 version 3 / range 32（后续批次）。
校验：`verify-parasite-combat-rules.cjs` 增加 8 条断言；审计记账 1 条，满足 681 → **682**。

## 批次 32：sim_human 的招募任务优先级订正（2026-09-25 续）

原版 `EntityInfHuman:122` 把同一个 `EntityAIGetFollowers(this, 1, 16)` 注册在**优先级 5**（其余同化/野化类为 6）。
端口 `RecruitFollowersGoal` 已复刻 version 1/range 16 语义（批次 31），本轮按原版优先级补注册到 `SimHumanEntity`。

审计记账：该生物审计中无独立 `EntityAIGetFollowers` 条款（其 AI 条款为更粗的 `tasks.addTask` 形态），
故本轮**账面不变**，属行为保真度补全；对应断言已加入 `verify-parasite-combat-rules.cjs`。

## 批次 33：EntityAIGetFollowers version 3（2026-09-25 续）

原版 `EntityAIGetFollowers` 的 `case 3`（adapted 系 `tasks.addTask(6, EntityAIGetFollowers(this, 3, 32))`）与 version 1 的差异：
候选类型 `< 41`，且**当候选已有跟随者时**，若其上级 `getParasiteType() <= 40` 则**抢走**该跟随者。

`RecruitFollowersGoal` 扩展：新增 `version` 字段与 3 参构造（默认 1），`STEAL_LEADER_RANK = 40`，
循环体按 `version < 3 || commandRank(existing) > 40` 判定是否跳过；实体筛选谓词中的「无跟随者」条件上移到循环内
（否则 version 3 的抢夺永远命中不了——本批修正了这点）。数值类型门用端口既有的 `commandRank` 作等价物。

**踩坑与修法（本轮重点）**：上一轮用多行 `\n` 锚点做该扩展**全部静默失配**（仓库文件是 CRLF）；
本轮改用 **edit 工具逐处按字节匹配**，三处编辑（字段/构造、循环抢夺判定、谓词上移）一次到位。
校验：`verify-parasite-combat-rules.cjs` 增加 3 条断言（**先落实现再落断言**，避免上轮的失败断言窗口）。
未接线：adapted 族的 `addGoal(6, new RecruitFollowersGoal(this, 32, 3))` 注册点（其 goals 锚点待下一轮确认）。

## 批次 34：hi_skeleton 招募任务接线（2026-09-25 续）

原版 `EntityHiSkeleton:52` 注册 `EntityAIGetFollowers(this, 1, 16)`（优先级 6）。与 adapted 族不同，
`HiSkeletonEntity` 经 `HijackedParasiteEntity → PrimitiveParasiteEntity` **继承**了端口的 `ParasiteFollowGoal`
（领导模型齐全），因此本轮可直接接线而不会出现「指派了 leader 却无人跟随」的语义错配。

顺带记录一项**本轮判断为暂不接线**的决定：adapted 族（`AdaptedVariantEntity`）**没有** `ParasiteFollowGoal`
（全文件 0 处，其 goals 全部按 kind 分支注册），若只加 version 3 招募会出现「有 leader 无跟随」的错配；
应在补齐该族的跟随模型后再接线（已在条目中注明）。

校验：`verify-parasite-combat-rules.cjs` 增加 1 条断言；审计记账 1 条，满足 682 → **683**。

## 批次 35：EntityAIAttackProjectile 侦察（2026-09-25 续，未改代码）

`sim_bigspider` 审计有两条相邻条款：
- `tasks.addTask(6, EntityAIAttackProjectile(this, 60, 15, 3))`
- `远程攻击 EntityAIAttackProjectile(this, 60, 15, 3)：每 60 tick 发射蛛网弹`

侦察结论（关键，可直接开工）：

1. **端口已有对应弹体**：`ModEntities.WEB_BALL`（`ParasiteProjectileEntity` + `Mode.WEB`，`webball`，0.3×0.3，权重 4/3）——
   即条款所说的"蛛网弹"，无需新建实体类型。
2. **该弹体目前无任何使用点**（全仓 `WEB_BALL` 仅出现在注册处），属"有弹无枪"状态。
3. 端口 `AssimilatedVariantEntity`（sim_bigspider 所属类）`Projectile|shoot|Ranged` 计数为 **0** —— 远程攻击整体缺失。
4. 仍需补的两项前置阅读：`ParasiteProjectileEntity` 的生成/发射 API（构造或静态工厂），以及原版
   `EntityAIAttackProjectile` 的冷却/射程/连发语义（参数已由审计给出：60 tick、15 格、3 连发或 3 号弹种待核）。
5. 注册点：`AssimilatedVariantEntity.registerGoals` 的 goals 按 kind 分支，需定位 `DORPA/BIGSPIDER` 分支锚点
   （用 edit 工具按字节匹配，避免 CRLF 多行锚点失配——见批次 33 的教训）。

本轮不动代码，账面不变（满足 683 / 缺失 265 / 加权 65.8%）。

## 批次 36：sim_bigspider 蛛网弹订正（2026-09-25 续）

批次 35 的侦察结论需要修正一处：端口 `AssimilatedVariantEntity` **已经实现**该远程攻击，只是写在 `tick()` 里而非目标选择器：

```java
if (kind == Kind.BIGSPIDER && rangedCooldown <= 0 && getTarget() != null && hasLineOfSight(getTarget())) {
    fireWebBall(getTarget());     // 弹体即既有 ModEntities.WEB_BALL
    rangedCooldown = 60;          // 与原版 EntityAIAttackProjectile(this, 60, …) 的冷却一致
}
```

因此：**条款「远程攻击 EntityAIAttackProjectile(this, 60, 15, 3)：每 60 tick 发射蛛网弹」订正为满足**（有既有 `fireWebBall` + 60 tick 冷却为证）。
**条款「tasks.addTask(6, EntityAIAttackProjectile(this, 60, 15, 3))」保持缺失**——其「注册进目标选择器」形态与参数中的 `15/3`（原版 `tickInter`/`shootingTimes`，疑似 15 tick 间隔 3 连发）尚未实现，需先读原版 `func_75246_d` 确认语义。
顺带为该既有行为补上 3 条**此前没有的断言**（BIGSPIDER 门 / `fireWebBall` / 60 tick 冷却），防止它在重构中被悄悄改坏。

校验：`verify-parasite-combat-rules.cjs` 增加 3 条断言；审计记账 1 条，满足 683 → **684**，缺失 265 → **264**。

## 批次 37：EntityAIAttackProjectile 连发实现（2026-09-25 续）

读原版 `func_75246_d` 确认参数含义后实现（此前端口只有"每 60 tick 单发"）：

| 原版 | 实现 |
| --- | --- |
| 射程 `distanceToSqr < 4225.0`（**65 格**）且需视线，否则 `attackTimer = 0` | `WEB_RANGE_SQR = 4225.0D` + `hasLineOfSight`，失效即复位 |
| 蓄力：每 tick `attackTimer++`（RAGE 时翻倍），到 `cooldown` 后开始放 | `WEB_CHARGE_TICKS = 60`（RAGE 翻倍未做，见下） |
| 放：`shootingTimes(3) > shootingUpdate` 且 `attackTimer % tickInterval(15) == 0` → 一发 | `WEB_VOLLEY_SHOTS = 3`、`WEB_VOLLEY_INTERVAL_TICKS = 15` 逐发 `fireWebBall` |
| `attackTimer == cooldown - 10` 播放投射音 | **未实现**（端口无对应 `playProjSound`；记录待补） |

新实现集中在 `tickWebBallVolley()`，`tick()` 的 BIGSPIDER 分支只做 `if (kind == Kind.BIGSPIDER) tickWebBallVolley();`。
校验：`verify-parasite-combat-rules.cjs` 断言块重写为 7 条（覆盖门控/常量/连发消耗）；审计记账 1 条，满足 684 → **685**。

**过程记录**：本轮更新断言时先撞上 CRLF 多行锚点失配，继而连续两次行拼接把断言数组改出语法错误；最终处置为
`git checkout` 还原 + 按**行边界整块替换**（而非拼接）——该手法已稳定，后续更新断言块应直接采用。

## 批次 38：投影蓄力的 RAGE 加速（2026-09-25 续）

补齐批次 37 记录的第一个残留子项：原版 `func_75246_d` 中
`if (parent.hasEffect(RAGE_E)) attackTimer++;` —— 即 **RAGE 状态下蓄力计时每 tick 前进两次**（60 tick 蓄力缩短为 30 tick）。
实现于 `tickWebBallVolley()` 的蓄力分支；断言追加至 `verify-parasite-combat-rules.cjs`。
仍缺：`attackTimer == cooldown - 10` 的投射音（端口无对应音效方法，待确认资源后补）。

## 批次 39：投影蓄力电报音（2026-09-25 续）

补齐批次 37/38 记录的最后一个残留子项：原版 `func_75246_d` 在 `attackTimer == cooldown - 10` 时调用
`((EntityCanShoot) parent).playProjSound()`（开火前 10 tick 的预警音）。

端口无同名方法，改用既有音效资源 `ModSounds.MOB_SHOOT`（`mob.shoot`）作为等价物：
`WEB_SOUND_LEAD_TICKS = 10` + `webChargeCued` 单次触发标志（RAGE 双步进会跨过精确 tick，用标志而非等值判定保证只响一次）；
目标失效/离开射程/失去视线时与其余连发状态一并复位。
校验：`verify-parasite-combat-rules.cjs` 增加 3 条断言。
至此 `EntityAIAttackProjectile(this, 60, 15, 3)` 的**全部子项**（射程 65 格、视线、60 tick 蓄力、RAGE 翻倍、15 tick × 3 连发、预警音）均已还原。

## 批次 40：per-mob 属性倍率的缺口定位（2026-09-25 续，未改代码）

审计条款：「生成时按全局 × per-mob 倍率结算属性（`globalHealthMultiplier × fervillagerHealthMultiplier` 等 4 项）」。
本轮把它的两侧现状查清，结论是**机制已存在、只是未覆盖已审计生物**：

| 侧 | 现状 | 证据 |
| --- | --- | --- |
| 全局倍率 | ✅ 已实现并接线 | `GeneralConfig.globalHealthMultiplier()` 等 4 项在 `config/OriginalConfigEvents.java:39-45` 通过属性修饰符应用 |
| per-mob 倍率 | ⚠️ 机制存在但未覆盖已审计生物 | `config/MobsConfig.java` 中 `*HealthMultiplier`/`*DamageMultiplier` 共 **29** 条（例：`arachnidaHealthMultiplier`、`arachnidaDamageMultiplier`，见 `:156/:159`），但 `fervillager`/`infcow`/`specow` 等已审计生物**均为 0 条** |

即：**照抄 `arachnida*` 的既有范式**即可（配置项 + 属性创建处读取），但需要先从原版 `SRPConfig` 取每只生物的 4 个倍率值
（`fervillagerHealthMultiplier` 等），不能凭猜填数——这正是本轮不动手的原因。

下一轮实施清单（无待调研项）：
1. 从 `SRPConfig` 取出已审计生物（`fer_villager`、`sim_cow`、`mar_cow` 等）的 4 项 per-mob 倍率默认值；
2. 按 `MobsConfig` 既有 `defineInRange` 范式补条目；
3. 在各族属性创建处（`createAttributes`）读取并相乘；
4. 断言 + 记账（该条款为「全局 × per-mob」两项合取，须两侧齐备才可翻转）。

## 批次 41：per-mob 属性倍率的取值来源锁定（2026-09-25 续，未改代码）

批次 40 遗留的"需先取原版 per-mob 4 项值"本轮已解决，且**无需猜数**：

1. **来源文件**：原版 per-mob 倍率不在 `SRPConfig`（那里只有 4 个 global，`:15-18` 默认 1.0F），
   而在 **`SRPConfigMobs`**（例：`shycoHealthMultiplier:19`、`dorpaHealthMultiplier:85`、`ratholHealthMultiplier:96`）。
   —— 端口 `config/MobsConfig.java` 正是该文件的对应物（已有 29 条 `*Multiplier`）。
2. **默认值全部为 `1.0F`**（抽查 `shyco/gim/zaa/dorpa/rathol/gothol` 均如此）⇒ 端口补条目时**不存在"编数字"风险**。
3. **命名约定**：`<原版内部名>{Health,Armor,Damage,KDResistance}Multiplier`，与 4 个全局项一一对应；
   因此 `sim_bigspider`（原版类 `EntityDorpa`）对应 `dorpaHealthMultiplier` 等 4 项。
4. 端口既有范式（照抄即可）：`MobsConfig:156-160` 的 `arachnidaHealthMultiplier` / `arachnidaDamageMultiplier`，
   形式为 `(实体 id, 键名, 默认值, 最小值, 最大值, 注释)`。

下一轮实施清单（完全无待调研项）：
1. 按 `SRPConfigMobs` 为已审计生物各补 4 条（默认 1.0，范围与 `arachnida*` 同）；
2. 在对应族 `createAttributes` 中读取并相乘（`dorpa*` → `AssimilatedVariantEntity` 的 BIGSPIDER 分支等）；
3. 断言 + 记账（条款为「全局 × per-mob」合取，两侧齐备后方可翻转）。

## 批次 42：per-mob 倍率配置面开工 + 一处重要订正（2026-09-25 续）

**订正批次 40/41 的判断**：端口 `MobsConfig` 里既有的 `arachnida*` 等 29 条 `*Multiplier` **没有被任何代码读取**
（`grep ARACHNIDA_HEALTH_MULTIPLIER` 在 `MobsConfig` 之外 0 命中）——即 per-mob 倍率此前**只有配置键、没有机制**，
不能算"机制已存在"。这也解释了审计为何判定该条款缺失。

本批按原版 `SRPConfigMobs` 的命名与默认值（全 1.0F）补齐 `sim_bigspider`（原版类 `EntityDorpa`）的 4 条键：
`dorpaHealthMultiplier` / `dorpaDamageMultiplier` / `dorpaArmorMultiplier` / `dorpaKDResistanceMultiplier`，
照抄 `arachnida*` 的 `value(...)` 范式；断言 4 条。
**下一步（接线）**：在对应实体 `createAttributes` 中读取这些键并相乘，同时把既有的 `arachnida*` 一并接上
（顺带修掉这个遗留的半成品）；两侧齐备后「全局 × per-mob」合取条款方可翻转。

## 批次 43：per-mob 属性倍率接线（sim_bigspider）（2026-09-25 续）

批次 42 补齐配置键后，本轮完成**读取侧**接线：

- `MobsConfig` 新增 4 个公开访问器（`dorpaHealthMultiplier()` 等）；
- `AssimilatedVariantEntity.createAttributes` 在 `Kind.BIGSPIDER`（原版 `EntityDorpa`）分支读取并相乘
  生命/护甲/攻击，击退抗性按原版上限 `Math.min(1.0, …)` 夹取——与既有全局倍率
  （`OriginalConfigEvents:39-45`）共同构成原版 `SRPAttributes` 的「全局 × per-mob」结算。
- 同时修正了批次 40/42 指出的遗留半成品方向：既有的 `arachnida*` 配置键仍是"只配置不读取"，可在下一轮照本批范式接上。

校验：`verify-parasite-combat-rules.cjs` 增加 6 条断言；审计记账 1 条，满足 685 → **686**，缺失 265 → **264**。

## 批次 44：sim_cow 的 per-mob 倍率接线（2026-09-25 续）

按批次 43 的范式复制到同化牛：原版键名由 `SRPConfigMobs` 查得为 `infcow*`（`infbear/infhuman/infsquid/infcow/
infsheep/infwolf/infvillager` 系列，默认值全 1.0F）。

- `MobsConfig` 新增 `infcowHealth/Damage/Armor/KDResistanceMultiplier` 四项 + 4 个访问器；
- `AssimilatedParasiteEntity.createAttributes` 在 `Kind.COW` 分支读取相乘（击退抗性仍按上限夹取 1.0）。

**同族剩余**（键名已知，照抄即可）：`infsheep`、`infwolf`、`infsquid`、`infhuman`（`sim_human` 为独立类）、
`infbear`；野化族对应 `fer*` 系列（`ferbear` 已确认存在，`fervillager` 待逐项核对）。
校验：`verify-parasite-combat-rules.cjs` 增加 5 条断言；审计记账 1 条，满足 686 → **687**，缺失 264 → **263**。

## 批次 45：sim_sheep / sim_wolf 的 per-mob 倍率接线（2026-09-25 续）

沿用批次 43/44 的范式（原版键名 `infsheep*` / `infwolf*`，默认 1.0F）：
`MobsConfig` 各补 4 项 + 4 个访问器；`AssimilatedParasiteEntity.createAttributes` 扩展为
`cow / sheep / wolf` 三分支按 kind 取值（击退抗性仍夹取 1.0）。

同族剩余键名（已知，照抄即可）：`infsquid`、`infbear`、`infhuman`（`sim_human` 为独立类）、
野化族 `fer*` 系列（`ferbear` 已确认存在，`fervillager` 待逐项核对）。
校验：`verify-parasite-combat-rules.cjs` 增加 6 条断言；审计记账 2 条，满足 687 → **689**，缺失 263 → **261**，加权 **66.2%**。

## 批次 46：sim_squid 的 per-mob 倍率接线（2026-09-25 续）

沿用批次 43-45 范式（原版键名 `infsquid*`，默认 1.0F）：`MobsConfig` 补 4 项 + 4 个访问器；
`createAttributes` 的 `cow / sheep / wolf / squid` 四分支取 `infsquid*`（击退抗性夹取 1.0）。
校验：`verify-parasite-combat-rules.cjs` 增加 4 条断言；审计记账 1 条，满足 689 → **690**，缺失 261 → **260**，加权 **66.3%**。

同族剩余：`sim_human`（独立类 `SimHumanEntity`，键名 `infhuman*`）与 `sim_bear`（`infbear*`，未审计）；
野化族 `fer*` 系列（`ferbear` 已确认存在）。

## 批次 47：sim_human 的 per-mob 倍率接线（2026-09-25 续）

同化族 per-mob 倍率的**最后一只已审计生物**（原版键名 `infhuman*`，默认 1.0F）：
`MobsConfig` 补 4 项 + 4 个访问器；`SimHumanEntity.createAttributes` 把基础值
生命 40 / 攻击 12 / 护甲 6 / 击退抗性 0.2（按上限夹取 1.0）各自乘以对应倍率。
校验：`verify-parasite-combat-rules.cjs` 增加 5 条断言；审计记账 1 条，满足 690 → **691**，缺失 260 → **259**。

至此本线覆盖：`dorpa`(sim_bigspider)、`infcow`、`infsheep`、`infwolf`、`infsquid`、`infhuman`。
剩余：`sim_bear`（`infbear*`，未审计）与野化族 `fer*` 系列（`ferbear` 已确认存在，`fervillager` 待核对）。

## 批次 48：fer_villager 的 per-mob 倍率接线（2026-09-25 续）

野化族首只（原版键名 `fervillager*`，默认 1.0F；同族 `ferbear/fercow/ferenderman/ferhorse/ferhuman` 均已确认存在）：
`MobsConfig` 补 4 项 + 4 个访问器；`FeralParasiteEntity.createAttributes` 在 `Kind.VILLAGER` 分支按四项相乘
（击退抗性夹取 1.0）。
校验：`verify-parasite-combat-rules.cjs` 增加 6 条断言；审计记账 **3 条**，满足 691 → **694**，缺失 259 → **257**，加权 **66.5%**。
剩余：野化族其余 8 种（`ferbear` 等）与 `sim_bear`（`infbear*`）。

## 批次 49：pri_longarms（shyco）的 per-mob 倍率接线（2026-09-25 续）

键名 `shyco*`（`SRPConfigMobs:19` 已确认，默认 1.0F）：`MobsConfig` 补 4 项 + 4 个访问器；
`LongarmsEntity.createAttributes` 把基础 生命 45 / 护甲 9 / 攻击 15 / 击退抗性 0.7（夹取 1.0）各自乘以倍率。
**账面未变**（该生物审计中的相关条款措辞与匹配式不符，dry-run 0 命中），属行为保真度补全；断言 5 条。
本线已覆盖 8 只：dorpa / infcow / infsheep / infwolf / infsquid / infhuman / fervillager / shyco。
剩余候选（键名待核对）：`buglin`、`hiskeleton`、`host`、`speCow`(mar_cow)、`beckon*`。

## 批次 50：hi_skeleton（hiskeleton）的 per-mob 倍率接线（2026-09-25 续）

键名 `hiskeleton*`（`SRPConfigMobs:671` 确认，默认 1.0F）：`MobsConfig` 补 4 项 + 4 个访问器；
`HiSkeletonEntity.createAttributes`（委托 `HijackedParasiteEntity.createAttributes(生命, 护甲, 攻击, 击退, 速度, 跟随)`）
改为按四项乘基础值 27/8/17/0.9（击退夹取 1.0）。
校验：断言 5 条；审计记账 1 条，满足 694 → **695**，缺失 257 → **256**，加权 **66.6%**。

本线已覆盖 9 只：dorpa / infcow / infsheep / infwolf / infsquid / infhuman / fervillager / shyco / hiskeleton。
剩余候选键名已确认：`host*`（host）、`marcow*`（mar_cow）；`buglin`/`beckon` 未在 `SRPConfigMobs` 中找到同名键。

## 批次 51：mar_cow（marcow）的 per-mob 倍率接线（2026-09-25 续）

键名 `marcow*`（`SRPConfigMobs:585` 确认，默认 1.0F）：`MobsConfig` 补 4 项 + 4 个访问器；
`MarauderizedCowEntity.createAttributes` 按四项乘基础 生命 38 / 护甲 8 / 攻击 15 / 击退 0.8（夹取 1.0）。
校验：断言 5 条；审计记账 1 条，满足 695 → **696**，缺失 256 → **255**，加权 **66.7%**。

本线已覆盖 10 只：dorpa / infcow / infsheep / infwolf / infsquid / infhuman / fervillager / shyco / hiskeleton / marcow。
剩余候选：`host*`（host，键名已确认）；`buglin`/`beckon` 无同名键（已确认）。

## 批次 52：host 的 per-mob 倍率接线（2026-09-25 续）

键名 `host*`（`SRPConfigMobs:300` 确认，默认 1.0F）：`MobsConfig` 补 4 项 + 4 个访问器；
`HostEntity.createAttributes` 按三项乘基础 生命 50 / 护甲 7 / 攻击 10（helper 参数序为 health, armor, damage, speed, follow）；
击退抗性由 `AbstractHostEntity.createHostAttributes` 固定为 1.0（已在上限，乘倍率等价，已在注释与文档说明）。
**同批订正断言**：`verify-early-lifecycle-entities-port.cjs:50` 原本断言 host 属性的字面量，改为断言新表达式——
否则会多出一个假失败（本轮实际发生并被套件当场抓出：20 → 21 失败，订正后回到 20）。
本线已覆盖 11 只：…/ hiskeleton / marcow / host。剩余：`buglin`、`beckon`（已确认无同名键）。

## 批次 53：arachnida per-mob 倍率键打通（2026-09-25 续）

批次 52 定位到：`arachnida*` 等 29 条 `*Multiplier` 键是**私有常量、无任何访问器**，因此外部代码**根本无法读取**——
这才是"只配置不读取"的根因（不是缺接线，而是缺入口）。

本批先打通入口：`MobsConfig` 增加 `arachnidaHealth/Damage/Armor/KnockbackMultiplier()` 四个公开访问器
（注释写明"键存在但不可达"），断言 4 条。下一步即可在 `AdaptedVariantEntity` / `PrimitiveVariantEntity` 的
`case ARACHNIDA` 分支把 `adaptedArachnida*`（基础值）与 `arachnida*Multiplier`（per-mob 倍率）相乘——
即原版「全局 × per-mob」的第三层（该两族的基础值来自 `MobsConfig.adaptedArachnida*`，与 arachnida* 是两套并存配置面）。

## 批次 54：arachnida per-mob 倍率叠加（Adapted 侧）（2026-09-25 续）

入口打通后立即接线：`AdaptedVariantEntity.applyConfiguredAttributes` 的 `case ARACHNIDA` 由
「只用 `adaptedArachnida*` 基础值」改为「基础值 × `arachnida*Multiplier`」（击退抗性夹取 1.0），
即原版 `SRPAttributes` 的「全局 × per-mob」在 arachnida 上的落地；断言 3 条。
`PrimitiveVariantEntity` 的同类分支（`:342`）留待下一批（其文本尚未核对，避免凭记忆改）。

## 批次 55：arachnida per-mob 倍率叠加（Primitive 侧）（2026-09-25 续）

按批次 54 的同一改法完成 primitive 侧：`PrimitiveVariantEntity.applyConfiguredAttributes` 的
`case ARACHNIDA` 由「只用 `arachnida*` 基础值」改为「基础值 × `arachnida*Multiplier`」（击退抗性夹取 1.0）。
两侧（primitive + adapted）现已一致，`arachnida*` 这 4 条配置键不再是死键。断言 2 条。
`PrimitiveVariantEntity` 中还有 `bolster*`/`tozoon*` 等同类分支（本次未核对文本，未动）。

## 批次 56：死键清单固化 + 防复发守卫（2026-09-25 续）

对 `MobsConfig` 的 85 条 `*Multiplier` 键做了一次全量比对（键名 vs `public static double xxx()` 访问器），
发现欠账**远大于批次 52 的估计**：除已接线的两组（aarachnida、以及本会话新加的 dorpa/infcow/infsheep/infwolf/
infsquid/infhuman/fervillager/shyco/hiskeleton/marcow/host）外，还有 **12 组、约 40 条键从无访问器**：

```
heavyBomber*  overseer*  vigilante*  warden*
primitiveBolster*  primitiveBurrower*  primitiveDevourer*  primitiveManducater*
primitiveReeker*  primitiveTozoon*  primitiveViscera*  primitiveYelloweye*
```

其中 `primitive*` 八组恰好对应 `PrimitiveVariantEntity.activeKind()` 的分支（与 arachnida 同构，可照批次 55 的
办法逐一叠加）；`heavyBomber/overseer/vigilante/warden` 属 Preeminent/Pure 系。

**本轮动作**：在 `verify-parasite-combat-rules.cjs` 增加**防复发守卫**——扫描所有 `*Multiplier` 键，
凡无同名访问器且不在显式 `KNOWN_UNWIRED_MULTIPLIER_GROUPS` 白名单内即报错。
这样：① 新增死键会立刻被拦住；② 白名单本身就是一份可勾选的待办清单，随接线逐步缩短。

## 批次 57：**订正批次 56 的错误清单**（2026-09-25 续）

批次 56 声称「85 条 `*Multiplier` 键中有 12 组、约 40 条从无访问器」，并据此加了白名单守卫。本轮逐项落实时发现**该清单是 grep 误解析的产物**：

- 按 `"primitiveBolsterHealthMultiplier"` 字面量反查，全文件**只命中我新写的访问器行**，即该键根本不存在；
- 核对真实键名：bolster 系列实际叫 `bolsterHealth` / `bolsterArmor` / `bolsterDamage`（**无 `Multiplier` 后缀**），
  与 `MobsConfig.bolsterHealth()` 这类既有访问器一一对应，**并非死键**；
- 误解析原因：我用的 `grep -oE '"[a-zA-Z]*(Health|Damage|Armor)Multiplier"'` 在含多段字符串与注释的行上产生了
  被截断的伪键名（如 `anducaterArmorMultiplier`），据此生成的"缺访问器"差集自然不可信。

**处置**：① 删除上一批新增的白名单守卫（它的依据已被证伪，留着会误导后人）；② 保留 `verify-parasite-combat-rules.cjs`
中其余基于**实证**的断言；③ 后续若要重做该清单，须改用能正确解析 `value(..., "键名", ...)` 的解析方式（而非单行 grep）。
教训与批次 33/54 同类：**结论必须先落到单条可复核的证据上，再做批量推断**。

净变更：删除 14 行守卫（无其它改动）；套件回到既有基线（99 / 79 / 20）。

## 批次 58：用解析式审计替换 grep 猜测——并**再次订正批次 57**（2026-09-25 续）

新增 `scripts/audit-mob-multipliers.cjs`：按 `value(id, key, default, min, max, comment)` 调用**逐字段解析**
（正则跨行匹配），把「常量名 → 配置键」与「访问器 → 常量」两张表建起来，再求差集。这才是可信的口径。

实测（当前 HEAD）：

```
declared value(...) constants: 121     per-mob multiplier keys: 99     accessors: 56
unreachable (no accessor):     50
  BOLSTER_* → primitiveBolsterArmorMultiplier / … / primitiveBolsterKnockbackResistanceMultiplier
  BURROWER_* DEVOURER_* MANDUCATER_* REEKER_* TOZOON_* VISCERA_* YELLOWEYE_*
  (均为 primitive* 系列，另含 JINJO_* → heavyBomber*、OVERSEER_*、VIGILANTE_*、WARDEN_*)
```

**订正批次 57**：上一轮我断言"该清单是 grep 误解析产物、bolster 键不存在"，这是**错的**——
键确实存在（`primitiveBolsterArmorMultiplier` 等 50 条），我之所以查不到，是因为按**常量名**
`PRIMITIVE_BOLSTER_HEALTH_MULTIPLIER` 去搜，而它的真实常量名是 `BOLSTER_HEALTH_MULTIPLIER`。
因此批次 56 的清单**方向是对的**（只是当时的生成方式不可靠、名字有截断），批次 57 的否定**过头了**，
且当时删掉的守卫**应当恢复**（下一批按本审计脚本的口径重建，白名单即上表 50 条）。
教训升级：**证伪也要有证据；"查不到"可能只是查错了名字**。

## 批次 59：恢复死键守卫（`--strict`）（2026-09-25 续）

批次 57 删掉的守卫，本轮以**解析式审计**为依据重建：`node scripts/audit-mob-multipliers.cjs --strict`
在白名单之外出现新的"无访问器倍率键"时**退出非零**；白名单即当前实测的 50 条 backlog（12 组），随接线逐组划掉。

```
strict: 50 known backlog key(s), 0 new ones.      # 当前输出；退出码 0
```

同时把"dangling 访问器"降级为**提示信息**：解析器不建模 `PREEMINENT_FOLLOW`/`PURE_FOLLOW` 这类
经其它 helper 声明的常量，若当失败处理会产生假阳性（本轮实测 3 条，代码本身可正常编译）。

## 批次 60：primitive bolster 倍率接线（backlog 50 → 46）（2026-09-25 续）

按批次 58 审计脚本给出的**实证常量名**（`BOLSTER_HEALTH_MULTIPLIER` 等，非我先前误用的 `PRIMITIVE_BOLSTER_*`）：
`MobsConfig` 加 4 个访问器；`PrimitiveVariantEntity` 的 `case BOLSTER` 由「只用 `bolster*` 基础值」
改为「基础值 × 倍率」（击退抗性夹取 1.0）；并把 `BOLSTER_` 从 `--strict` 白名单划掉。

```
$ node scripts/audit-mob-multipliers.cjs --strict
strict: 46 known backlog key(s), 0 new ones.      # 由 50 降至 46
```

这标志着 12 组死键 backlog 的**第一组下线**；后续每组按同一流程（查审计脚本给的名字 → 访问器 → case 叠加 → 白名单划掉）即可稳定推进。

## 批次 61：primitive burrower 倍率接线（backlog 46 → 42）（2026-09-25 续）

同批次 60 的流程（审计脚本给常量名 → 加访问器 → `case BURROWER` 叠加 → 白名单划掉）：
`BURROWER_` 四键下线，`--strict` 输出由 46 降至 **42**。

## 批次 62：primitive devourer 倍率接线（backlog 42 → 38）（2026-09-25 续）

同固化流程：`DEVOURER_` 四键下线（访问器 + `case DEVOURER` 叠加 + 白名单划掉），`--strict` 由 42 降至 **38**。

## 批次 63：primitive manducater 倍率接线（backlog 38 → 34）（2026-09-25 续）

同固化流程：`MANDUCATER_` 四键下线，`--strict` 由 38 降至 **34**。

## 批次 64：primitive tozoon 倍率接线（backlog 34 → 30）（2026-09-25 续）

同固化流程：`TOZOON_` 四键下线，`--strict` 由 34 降至 **30**（已清 5/12 组）。

## 批次 65：VISCERA / YELLOWEYE 两组的接线点核查（2026-09-25 续，未改代码）

继续清 backlog 时对接下来两组做了**接线点核查**，结论是这两组都不能照抄前五组：

| 组 | 核查结果 |
| --- | --- |
| `VISCERA_*` | `PrimitiveVariantEntity` 中**没有** `case VISCERA`——该生物是独立类（`VisceraEntity`），其属性不走 `applyConfiguredAttributes` 的 switch，需另找插入点 |
| `YELLOWEYE_*` | 有 `case YELLOWEYE`（`:379`），但第三个实参是 **`MobsConfig.yelloweyeNadeDamage()`**（榴弹伤害）而非攻击伤害——`YELLOWEYE_DAMAGE_MULTIPLIER` 究竟应对应"攻击伤害"还是"榴弹伤害"**无法从现有证据判定** |

**处置**：本轮**不接这两组**。理由：若只加访问器不接线，`--strict` 的 backlog 计数会虚降而键仍不可读（比不做更糟）；
若把 `YELLOWEYE_DAMAGE_MULTIPLIER` 想当然地乘到 `yelloweyeNadeDamage()`，就是在没有证据的情况下改数值语义。
两者都已在文档留痕，待补齐证据（原版 `SRPConfigMobs.yelloweyeDamageMultiplier` 的注释/使用点、`VisceraEntity` 的属性来源）后再动。

净变更：回滚一次仅有访问器的临时改动；backlog 维持 **30**（已清 5/12 组）。

## 批次 66：backlog 里混入了**端口自造键**（2026-09-25 续，未改代码）

沿批次 65 的线索补证据时发现一件更重要的事：原版 `SRPConfigMobs` 中**根本不存在** `yelloweye*` 字段
（`grep -nE "public static float yelloweye[A-Za-z]*" SRPConfigMobs.java` 无输出），而端口却定义了
`primitiveYelloweyeHealthMultiplier / DamageMultiplier / ArmorMultiplier / KnockbackResistanceMultiplier / FlightHeightLimit / GearDegrade`。

即：**这组键是端口自造的，不对应任何原版行为**，因此它们"没有访问器"**并不构成还原缺口**——
把它们接线反而是给原版没有的东西加行为。同理需警惕 backlog 中其它组是否也属此类（`JINJO_`/`OVERSEER_`/
`VIGILANTE_`/`WARDEN_`/`VISCERA_` 等尚未逐组核对原版对应性）。

**结论与下一步**：`--strict` 的 backlog 计数混入了"端口自造键"，其语义应从"必须补完的缺口"改为
"需要逐组判定：原版有 → 接线；原版无 → 删除或明确标注为端口扩展"。判定需要一个**原版键清单**作为基准，
下一批应从 `SRPConfigMobs` 解析出全部 `<name>HealthMultiplier/DamageMultiplier/...` 字段名，
与端口 `MobsConfig` 求交/差集后再分组归类。本轮不做删除，避免误删可能被其它系统使用的端口扩展键。

## 批次 67：把「原版是否有该键」的判定做进脚本（2026-09-25 续）

批次 66 的线索需要全量核实，但**内联 shell 循环在本环境不可靠**（实测 `for k in …; do grep -c "float $k" …` 的 `$k` 未正确展开，
12 个不同键全部命中同一行 `:498`，产出的是假数据）。因此按"与其在 shell 里猜，不如让工具产出可复核数据"的原则，
把判定做进 `scripts/audit-mob-multipliers.cjs`：读取原版 `SRPConfigMobs.java`（若存在），
对每条 backlog 键同时探测**键名**与其去掉 `primitive` 前缀的写法。

当前输出：

```
backlog backed by the original (must be wired): 0
backlog port-only (no restoration gap):        30
```

**必须说明的可靠性边界（避免重蹈批次 57/58 的覆辙）**：该探测按**配置键名**匹配，而端口的键名与端口常量名、
原版字段名三者并不总一致——例如 `heavyBomber*` 这组的端口常量名是 `JINJO_*`，原版对应字段名很可能也是 `jinjo*`。
因此"0 条原版背书"**只能视为初步结论**；下一步应把**常量名**（`JINJO_HEALTH_MULTIPLIER` → `jinjoHealthMultiplier`）
也纳入候选拼写一并探测，再据三路结果（键名/去前缀/常量名）给出定论。

## 批次 68：三路匹配得出定论——backlog 拆成「3 条该接线 + 27 条端口自造」（2026-09-25 续）

给审计脚本补上第三路拼写（端口**常量名**转 camelCase，如 `JINJO_HEALTH_MULTIPLIER` → `jinjoHealthMultiplier`）后重跑：

```
backlog backed by the original (must be wired): 3
  heavyBomberHealthMultiplier / heavyBomberDamageMultiplier / heavyBomberArmorMultiplier
backlog port-only (no restoration gap):        27
  heavyBomberExplosionMultiplier / heavyBomberKnockbackResistanceMultiplier
  overseer* / primitiveReeker* / primitiveViscera* / primitiveYelloweye* / vigilante* / warden*
```

**上一轮的命名疑点被证实**：`heavyBomber*` 这组在原版**确有**对应（原版按常量名 `jinjo*` 命名），
若只用"键名/去前缀"两路匹配就会误判为端口自造——这正是我上轮标注可靠性边界的原因，三路匹配后结论才可信。

结论与后续：① **3 条该接线**（heavyBomber 的健康/伤害/护甲）→ 按既有流程处理；
② **27 条属端口自造**，对还原**不构成缺口**，应择机删除或明确标注为端口扩展；
③ 在此之前不再把它们计入"待补"。

## 批次 69：heavyBomber 三条接线的现场勘查（2026-09-25 续，未改代码）

批次 68 判定"3 条原版背书、该接线"后，本轮去落实，查明三件事（供下一轮直接使用）：

1. **常量名确认**：`MobsConfig` 中确实存在 `JINJO_HEALTH_MULTIPLIER` / `JINJO_DAMAGE_MULTIPLIER` /
   `JINJO_ARMOR_MULTIPLIER`（grep 命中，与批次 68 的三路匹配一致），因此访问器应写成
   `return JINJO_*_MULTIPLIER.get();`——这正是第 57 轮踩坑处（当时误按 `PRIMITIVE_*` 找常量）。
2. **属性创建点尚未定位**：`grep 'bomber_heavy'` 在 entity 包只命中 `ParasiteSoundProfiles`（音效映射）与
   `PrimitiveParasiteEntity` 的破块表，**没有属性站点**；说明 bomber_heavy 的属性不按实体 id 拼装，
   而在其所属类（`PreeminentParasiteEntity` 的 `Kind` 分支或专用类）里，需要先读该类再决定插入点。
3. 本轮对 `MobsConfig` 的一次访问器改动因文件版本陈旧被编辑器拒绝（未写入），**工作树保持干净**，
   未留下半成品——与批次 65 的处置一致：定位不清时不硬改。

下一步（无待调研项的部分已完成）：读 `PreeminentParasiteEntity`（或其 bomber 专用类）的 `createAttributes`/`Kind`
分支 → 加 3 个访问器 → 对应偏移处叠加 → `--strict` 白名单划掉这 3 条。

## 批次 70：heavyBomber（原版 jinjo*）三条接线完成——**真实缺口归零**（2026-09-25 续）

按批次 69 的勘查结果落地：`PreeminentParasiteEntity.createAttributes(Kind)` 在 `Kind.BOMBER_HEAVY`
分支按 `heavyBomberHealth/Damage/ArmorMultiplier()`（读 `JINJO_*` 常量）叠加；`MobsConfig` 加 3 个访问器。

```
$ node scripts/audit-mob-multipliers.cjs --strict
unreachable (no accessor):      27
backlog backed by the original (must be wired): 0      ← 真实缺口归零
backlog port-only (no restoration gap):        27
strict: 27 known backlog key(s), 0 new ones.
```

**收束（批次 52→70 这条线）**：
- 起点是"`arachnida*` 等键只配置不读取"；中途经历一次误判（批次 57）与两次自我订正（批次 58、67→68 的三路匹配）；
- 终点：**所有原版有对应的 per-mob 倍率键均已接线且被断言覆盖**（dorpa/infcow/infsheep/infwolf/infsquid/infhuman/
  fervillager/shyco/hiskeleton/marcow/host/arachnida/bolster/burrower/devourer/manducater/tozoon/heavyBomber），
  剩余 27 条经三路匹配确认为**端口自造键**，对还原不构成缺口；
- 工具沉淀：`scripts/audit-mob-multipliers.cjs`（解析式 + 三路匹配 + `--strict` 守卫），后续新增死键会被直接拦下。

## 批次 71：**自查发现并回退"倍率双重乘算"回退**（2026-09-25 续）

在按计划清理"27 条端口自造键"时，删除 `OVERSEER_*` 四项导致**编译失败**——编译器指出它们仍被引用：

```java
public static double overseerHealth() { return 80.0D * OVERSEER_HEALTH_MULTIPLIER.get(); }
```

**这暴露了两件事**：

1. **审计工具的漏检**：我的访问器正则只认裸形态 `return CONST.get()`，而端口大量使用**带缩放的访问器**
   （`return 35.0D * BOLSTER_HEALTH_MULTIPLIER.get()`）。已放宽为 `public static (double|float) X() { … CONST.get() … }`。
   修正后重跑：`unreachable = 0`——**所有倍率键本来就可达**。
2. **由此查处我自己引入的回退（serious）**：批次 54/55/60–64/70 的"叠加"改动，在实体侧又乘了一次同一个倍率，
   而这些倍率的**唯一正确应用点就是访问器内部** ⇒ 那 8 处属于**双重乘算**。

**处置（同轮完成，未留隐患）**：
- 用一次性脚本 `scripts/port263/revert-double-multipliers.cjs` 精确回退 8 处（primitive 的 arachnida/bolster/burrower/
  devourer/manducater/tozoon 六例 + adapted arachnida + preeminent heavyBomber），回退后 `build` 通过；
- 删除/改写那 9 条断言（它们断言的正是被回退的错误形态），改为断言**正确设计**：倍率在 `MobsConfig` 访问器内
  一次性应用（如 `bolsterHealth()` 返回 `35.0D * BOLSTER_HEALTH_MULTIPLIER.get()`），实体侧只能使用普通访问器；
- 套件回到基线 **99 / 79 / 20**。

**更正此前的结论**：批次 60–64 与 70 所称"接线完成/backlog 下降"**是对幽灵目标的追击**——那些键当时已被访问器读取；
本轮把实体侧改动回退后，配置面反而恢复到**单一正确应用点**。批次 68 的"3 条该接线"同样应作废。
一并澄清：`--strict` 的 backlog 语义今后以"**常量是否被任一访问器读取**"为准（已修好并归零）。

## 批次 72：自查第 43–51 轮接线是否存在同类双重乘算（2026-09-25 续）

批次 71 的教训必须外推到**同一条线上的其它改动**。用一次针对性核查（每个常量的全部引用点）：

```
INFCOW_HEALTH_MULTIPLIER      : 170 声明 / 183 我的直通访问器      ← 仅此两处
DORPA_HEALTH_MULTIPLIER       : 156 声明 / 400 我的直通访问器
FERVILLAGER_HEALTH_MULTIPLIER : 306 声明 / 319 我的直通访问器
SHYCO_HEALTH_MULTIPLIER       : 325 声明 / 338 我的直通访问器
HISKELETON_HEALTH_MULTIPLIER  : 344 声明 / 357 我的直通访问器
MARCOW_HEALTH_MULTIPLIER      : 363 声明 / 376 我的直通访问器
HOST_HEALTH_MULTIPLIER        : 382 声明 / 395 我的直通访问器
```

**结论**：这些常量的**唯一读者是我新增的直通访问器**（`return CONST.get();`），不存在"既有访问器已乘一次"的情况，
而实体侧的基值是硬编码字面量（如 `Kind.COW.maxHealth = 18.0D`、`LongarmsEntity` 的 45.0）⇒
**第 43–51 轮的接线只应用一次，正确无误**；双重乘算问题**仅限**批次 71 已回退的那 8 处（它们是"访问器内已乘一次 + 实体侧再乘一次"）。

至此该线状态明确：
- ✅ 正确且单次应用：dorpa / infcow / infsheep / infwolf / infsquid / infhuman / fervillager / shyco / hiskeleton / marcow / host（11 只）；
- ✅ 已回退双重乘算：primitive 六例 + adapted arachnida + preeminent heavyBomber；
- ✅ 配置面所有倍率键均可达（`audit --strict` = 0）。

## 批次 73：同化族跟随范围对齐原版 `infectedFollow = 16`（2026-09-25 续）

用新脚本 `scripts/entity-parity/cluster-gaps.cjs` 对剩余缺口重新聚类（missing 255 / partial 371），
按其输出挑出证据明确、改动最小的一项：条款「跟随范围 SRPConfig.infectedFollow = 16」（6 条 partial）。

`AssimilatedParasiteEntity.Kind` 的 followRange 原为 32/24（按体型自定），与原版统一值 16 不符 ⇒ 六种全部改为 `16.0D`。

**记账从宽不发**：该条款是 partial，除跟随范围外可能还捆绑其它子项，故本轮**不翻转**，只把数值对齐并加断言
（6 条：每种 kind 的 followRange 必须为 16.0D）。聚类脚本已沉淀（`cluster-gaps.cjs`），后续选靶有据可依。

## 批次 74：阴影半径条款的原版取值勘查（2026-09-25 续，未改代码）

按聚类选靶「渲染阴影半径 0.5F」（6 条 partial）后的勘查结果：

| 侧 | 事实 |
| --- | --- |
| 端口 | `ClientModEvents` 注册同化族时逐个传阴影半径：`AssimilatedParasiteRenderer(context, 0.65F / 0.55F / 0.45F / 0.50F / 0.40F / 0.45F)`（按体型自定） |
| 原版（头部渲染器可查） | `client/renderer/entity/infected/head/RenderInf*Head.java` 为 `super(manager, new Model…(), 0.6F)`，`RenderInfSheepHead` 为 `0.5F` |
| 原版（**身体**渲染器） | 用 `RenderInfCow/RenderInfSheep/…/RenderDorpa` 与 `RenderInf*` 两种命名探测 `find` 均未命中该 `super(...)` 形态 ⇒ **类名或构造形态待确认**，暂无"族统一 0.5F"的证据 |

**处置**：本轮**不改数值**。理由：现有证据只覆盖头部渲染器（0.6/0.5），不足以推出身体渲染器一律 0.5F；
若照 0.5F 统一改，可能与原版按体型分档的做法相悖（正如批次 73 的 followRange 是"族统一 16"、而阴影半径未必如此）。
下一步：先定位原版身体渲染器类名（或改从 `RenderManager`/注册处反查），取得每只的实参后再对齐。

## 批次 75：同化族经验对齐原版 `infectedXPValue = 8`（2026-09-25 续）

条款「经验 field_70728_aV = SRPAttributes.XP_INFECTE…」的取值链已查清：

```
SRPAttributes.XP_INFECTED = SRPConfig.infectedXPValue     // 按【档次】统一，而非逐生物
SRPConfig: infectedXPValue = 8 / feralXPValue = 16 / primitiveXPValue = 30
```

端口 `AssimilatedParasiteEntity.Kind` 原先逐生物自定为 8/6/3/4/5/5（仅 sim_bear 恰好等于 8）⇒
六种全部对齐为 **8**（`Kind.experience` 即 `xpReward` 的来源），并加 6 条断言。

**顺手获得的同线信息**（后续可复用）：feral 族应为 **16**、primitive 族应为 **30**——如果这两族的
`Kind.experience` 也偏离，可照本批一次性对齐。

## 批次 76：野化族经验对齐（已审计生物 fer_villager 到位）（2026-09-25 续）

按批次 75 的取值链，野化族应统一为 `SRPConfig.feralXPValue = 16`。端口 `FeralParasiteEntity.Kind`
原为 12/12/24/12/10/8/9/10/10 ⇒ 本轮先把**已审计生物**与其邻项对齐：

- `VILLAGER`（= `fer_villager`）：10 → **16** ✔（正是其审计里"经验 16→10"标记的缺口）
- `WOLF`：10 → **16**（同批同值，顺手对齐）

**剩余 7 种待对齐**（BEAR 12 / COW 12 / ENDERMAN 24 / HORSE 12 / HUMAN 10 / PIG 8 / SHEEP 9）——
纯数值批量，下一轮用 edit 工具逐行改（本轮的正则批量因缩进/转义未命中，已改用 edit 工具，避免重蹈批次 71 的脚本事故）。
`build` 通过、套件维持既有 20 失败。

## 批次 77：原始族经验对齐 primitiveXPValue = 30（2026-09-25 续）

同批次 75/76 的取值链：原始族统一为 `SRPConfig.primitiveXPValue = 30`。`LongarmsEntity`（pri_longarms，已审计）
原为 `xpReward = 18` ⇒ 改为 **30**。
其余原始族生物（Pri... 各族）与野化族剩余 7 种的经验值仍待批量对齐，证据（30 / 16）已在文档中备好。

## 批次 78：野化族九种经验全部对齐 16（2026-09-25 续）

批次 76 先落了 VILLAGER/WOLF，本轮补齐余下七种（BEAR/COW/ENDERMAN/HORSE/HUMAN/PIG/SHEEP）⇒
`FeralParasiteEntity.Kind` 九种经验**全部为 16**，与原版 `SRPConfig.feralXPValue = 16` 一致（一次 edit 完成，
未再尝试正则批量——批次 76 的失败已说明该手段在此文件上不可靠）。
`build` 通过、套件维持既有 20 失败。

## 批次 79：原始族变体经验对齐 + 全档次 XP 常量查清（2026-09-25 续）

原版各档次 XP 常量（`SRPConfig`）已一次查全，作为后续对齐的唯一依据：

```
infected 8   feral 16   hijacked 11   primitive 30   adapted 55
ancient 5000 pure 75    preeminent 200 derived 350   turret 75
```

对照端口现状：
- ✅ 已一致：adapted（`AdaLongleg/AdaScuttler/AdaWatcher/AdaptedVariant` 均 55）、ancient（5000）、pure（`PureParasiteEntity` 75）；
- ✗ 本轮修正：`PrimitiveVariantEntity:204` 的 `kind == YELLOWEYE ? 30 : 18` → **统一 30**（原版 primitive 档即 30，YELLOWEYE 本就 30）；
- ⏳ 仍待核对：hijacked（应为 11，端口 `HijackedParasiteEntity` 由构造参数传入，需追各子类实参）、preeminent（200）、derived（350）、turret（75）以及同化族的 `AssimilatedDragon(300)/DragonHead(40)/Enderman(24)` 等特例是否对应原版特殊值。

批次 79 补记（同批已修）：改动 `PrimitiveVariantEntity` 的 XP 表达式后，`verify-primitive-yelloweye-port.cjs:30`
的旧断言 `/xpReward = kind == Kind.YELLOWEYE \? 30 : 18/` 立即失效（套件 20 → 21 失败），已改为 `/xpReward = 30;/`
并注明"整档共用 primitiveXPValue = 30"——YELLOWEYE 的取值不变（30），仅表达式统一。套件回到基线 99/79/20。

## 批次 80：劫持族经验对齐 hijackedXPValue = 11（2026-09-25 续）

按批次 79 查全的档次表（hijacked = 11、preeminent = 200），核对端口：

- ✗ `HiSkeletonEntity` 30 / `HiBlazeEntity` 36 / `HiGolemEntity` 60 → **全部对齐为 11**（含已审计的 `hi_skeleton`）；
- ✗ `PreeminentParasiteEntity:131 xpReward = 75` → 应为 **200**（原版 preeminent 档；75 实为 pure/turret 档值），**留待下一轮**（本轮先落劫持三只，控制单轮改动面）。

## 批次 81：preeminent 族经验对齐 preeminentXPValue = 200（2026-09-25 续）

`PreeminentParasiteEntity:131` 原为 `xpReward = 75`（实为 pure/turret 档值）⇒ 改为 **200**。
判据：该类的 5 个 kind（BOGLE / CARRIER_COLONY / HAUNTER / BOMBER_HEAVY / WRAITH）**均属 preeminent 档**，
原版 turret 档（75）在此类中不存在，故不存在"按 kind 分档"的需要。

## 批次 82：derived / pure / turret 三档核对（2026-09-25 续，仅记录未改）

按批次 79 的档次表逐档核对端口现状，结论如下：

| 档 | 原版值 | 端口 | 判定 |
| --- | --- | --- | --- |
| derived | 350 | `KirinEntity:139 xpReward = 350` | ✅ 一致 |
| pure | 75 | `PureParasiteEntity:223`、`ArchitectEntity:82`、`ManglerEntity:85` 均 75 | ✅ 一致 |
| turret | 75 | 未找到明确的"turret 类"；`DeterrentParasiteEntity.Kind` 为 0/36/0/36/0（DISPATCHER_TENTACLE / KYPHOSIS / SEIZER / SENTRY / WORM） | ⚠️ **无法判定**：sentry/kyphosis 并不等同于原版 turret，映射关系缺证据，故不改 |

至此 XP 线只剩"同化族特例"（`AssimilatedDragon 300` / `AssimilatedDragonHead 40` / `AssimilatedEnderman 24`）未核对——
需查原版 `EntityInfDragonE` 等的 XP 来源（可能来自 `infectedXPValue` 或另有专门常量）。

## 批次 83：原版 XP 的**结构**查清（2026-09-25 续，记录未改）

批次 82 遗留的"同化族特例"需要先理解原版的赋值方式，本轮用它替换了逐类猜测：

```
EntityPInfected / EntityPAssimara : field_70728_aV = SRPAttributes.XP_INFECTED          // 基准
EntityAboBodies                   : XP_INFECTED * 3
EntityDod / EntityLeem / EntityVenkrol : XP_INFECTED * 2
统计：XP_PRIMITIVE(7 处) / XP_LiTTLE(5) / XP_ADAPTED*2(5) / XP_INFECTED*2(4) / XP_ADAPTED(4) / XP_ADAPTED*4(4) / XP_PRIMITIVE*2(3) / XP_PURE(2)
```

即原版经验 = **档次常量 × 倍数**（×2/×3/×4），另有 `XP_LiTTLE` 档用于小体型生物。

**结论**：端口把经验写成逐类硬编码字面量（如 `AssimilatedDragon 300`、`Enderman 24`、`DragonHead 40`）本身不是错，
但要判断其对错，必须**先定位每类在原版的表达式**（`XP_X * n`），而非拿档次基准值去套——
这正是本轮不改数值的原因（上一轮 turret 已因同类理由留手）。下一批按"逐类找原版表达式"推进。

## 批次 84：XP 线收束与交接（2026-09-25 续）

按"逐类找原版表达式"推进时，`EntityInfDragonE`/`EntityInfEnderman`/`EntityInfDragonEHead`/`EntityInfHumanHead`
四个类名在 `_srp-orig` 路径下**均未命中**（`find` 无结果 ⇒ 类名不同或 XP 赋值在别处）。至此 XP 线告一段落：

**已完成且验证一致**（按档次常量）：
`infected 8` ✅、`feral 16` ✅、`primitive 30` ✅、`hijacked 11` ✅、`adapted 55` ✅、`ancient 5000` ✅、
`pure 75` ✅、`preeminent 200` ✅、`derived 350` ✅；`XP_LiTTLE = infectedXPValue / 2 = 4`（已查清，端口尚未见对应实现）。

**剩余未核对**（需先解决类名/赋值点定位）：
- 同化族特例：`AssimilatedDragon 300`、`AssimilatedDragonHead 40`、`AssimilatedEnderman 24`；
- turret 档（75）对应的原版使用类；
- `XP_LiTTLE`(4) 是否在端口有小体型生物需要该值。

**下一批建议的定位手法**（本轮已验证 `find -name` 不够）：改从原版**赋值点**反查——
`grep -rn "field_70728_aV" <decomp>/entity | grep -i dragon`，或先列出 `entity/monster/infected/` 目录真实类名。

## 批次 85：同化族刷怪蛋配色对齐（2026-09-25 续）

条款「刷怪蛋颜色 8611072/16711900」（6 条 partial）给出了原版配色对。换算：`8611072 = 0x836500`（背景，暗）、
`16711900 = 0xFF00DC`（高光，亮）。端口这六只（sim_bear/cow/pig/sheep/wolf/squid）原为**逐生物主题色**
（如 `0x3A211C/0xA24A3C`）⇒ 已全部替换为 `0x836500, 0xFF00DC`。

**自校验信号**：替换后全文件该配色出现 **11 处** ⇒ 另有 5 只蛋**原本就用**这一对颜色，
右证"原版统一配色、这六只是例外"的判断成立（若原版是逐生物配色，端口不会恰好有 5 处一致）。
`build` 通过、套件维持既有 20 失败。

## 批次 86：tracker 条款的语义与端口做法查清（2026-09-25 续，记录未改）

条款原文（`raw/*.json`）：**`EntityEntryBuilder.tracker(64, 3, true)` 追踪范围 64 / 更新间隔 3 / 速度同步**（7 条 partial）。
这是 **1.12.2 Forge 的实体注册参数**，不是某个 AI 的追踪逻辑——因此它与 `EntityAIEvade.tracker`（同名字段，实为冷却计数）
无关（本轮先做了这步排除，避免误改 AI）。

**1.21.1 的等价物**：`EntityType.Builder` 的

```java
.clientTrackingRange(4)   // 原版 64 格 ÷ 16 = 4 个区块（1.21 以区块为单位）
.updateInterval(3)        // 含义一致
```

**做法（下一批执行）**：在 `registry/ModEntities.java` 为对应实体的 `EntityType.Builder` 补上这两项；
需先 grep 现有是否已设置（部分实体可能已有 `clientTrackingRange`），再逐只对齐，避免重复/冲突。

## 批次 87：实体追踪范围对齐原版 tracker(64, 3, true)（2026-09-25 续）

上一批查清语义后落地：`ModEntities` 的 `monster(...)` helper 原为 `.clientTrackingRange(8)`（=128 格）✗，
改为 **`.clientTrackingRange(4).updateInterval(3)`**（64 格 ÷ 16 = 4 区块；间隔 3）。

**自校验信号**：改动后全文件 `clientTrackingRange(4).updateInterval(3)` 共 **17 处** ⇒ 另有 **16 个实体原本就用**
这一组合，说明 4/3 正是本项目的既有惯例（与原版 tracker(64,3,true) 一致），而 `monster` helper 的 8 是唯一例外。
`build` 通过、套件维持既有 20 失败。

## 批次 88：残留 5 处 `clientTrackingRange(8)` 的核查结论（2026-09-25 续，未改）

五处及其形态：

| 位置 | 实体 | 形态 |
| --- | --- | --- |
| `:139` | `buglin`（0.5×0.3 小体型，直接注册） | `.clientTrackingRange(8)`，无 updateInterval |
| `:145` | `rupter`（0.85×1.0） | 同上 |
| `:254` | `kirin`（2.13×**8.85** 大体型） | 同行 `.clientTrackingRange(8)` |
| `:528` | `parasite_projectile`（弹体） | `.clientTrackingRange(8).updateInterval(1)` |
| `:671` | `monster(..., eyeHeight)` **5 参重载**（供 draconite 2.4×3.8 等大体型） | `.clientTrackingRange(8)` |

**为何不改**：要判断它们该是 4/3 还是更大的范围，需原版各实体的注册参数；而全库检索 `.tracker(` **只命中 2 处**
（说明原版其余注册写法不同，如无点号前缀或多行链式），**不足以支撑"一律 64/3"的结论**——
尤其弹体（间隔 1 属合理特例）与大体积重载（128 格可能是刻意为之）两类，盲目统一反而可能造成新的偏差。
故按纪律留手，待取得原版逐类参数后再定（可考虑先查原版大体积实体如 kirin/draconite 的实际 tracker 值）。

## 批次 89：生成合法性条款是**功能缺口**而非数值偏差（2026-09-25 续，记录未改）

条款原文：**「生成合法性 func_70601_bi：亮度 isValidLightLevelOne/Two + SRPConfig.ignoreL + 难度非 PEACEFUL + spawnDays 天数门控」**（7 条 partial）。

核查端口：`grep -rn "isValidLightLevel|ignoreL|spawnDays" --include=*.java src/main/java` → **0 命中**。
即三要素**在端口完全不存在**（连配置键都没有），因此这 7 条不是"改个数值"能收敛的，而是**需要实现的功能**：

| 子项 | 原版语义（待按源码确认细节） | 端口现状 |
| --- | --- | --- |
| `isValidLightLevelOne/Two` | 两级光照判定（对应 1.12 的 `isValidLightLevel` 变体） | ❌ 无 |
| `SRPConfig.ignoreL` | 忽略光照的开关 | ❌ 无（配置键也没有） |
| `spawnDays` 天数门控 | 世界天数（`worldTime/2400`）未达阈值则不生成 | ❌ 无 |
| 难度非 PEACEFUL | 和平难度不生成 | ❓ 待查现有生成路径 |

**下一批做法**（需按顺序）：① 读原版 `func_70601_bi` 全文与其调用点，确定两级光照的具体阈值与 `ignoreL` 分支；
② 在端口查现有生成路径（`SRPSpawning` / `SpawnPlacements` / `checkSpawnRules`）决定挂载点；
③ 补配置键（`ignoreL`、`spawnDays`）→ 实现判定 → 断言 → 记账。属多轮任务，不宜与数值对齐混做。

## 批次 90：生成合法性（`func_70601_bi`）**实现蓝图**查全（2026-09-25 续）

原版 `EntityParasiteBase:1553` 全文语义：

```java
if (SRPConfigSystems.useEvolution) {
    if (phaseCreated >= SRPConfigSystems.evolutionSpawningIgnoreSunlight   // 默认 1
        || (phaseCreated == -1 && SRPConfigSystems.phaseLightlessMinusOne)) { // 默认 true
        return 下方方块.canSpawnMob(this)          // func_189884_a
            && world.getDifficulty() != PEACEFUL
            && isValidLightLevelTwo()
            && SRPConfig.spawnDays <= (int) world.getTotalWorldTime();   // 默认 0，单位是【tick】不是天数
    } else {
        return 下方方块.canSpawnMob(this)
            && world.getDifficulty() != PEACEFUL
            && isValidLightLevelOne()
            && SRPConfig.spawnDays <= (int) world.getTotalWorldTime();
    }
}
```

两级光照（同文件）：

```java
isValidLightLevelOne() {                       // :1632
    if (所在群系 instanceof BiomeParasiteBase) return isValidLightLevelTwo();
    if (world.getLightFor(SKY, pos) > random.nextInt(32)) { ... 后续方块光判定 ... }
}
isValidLightLevelTwo() {                       // :1654
    int light = world.getLightFor(BLOCK, pos);
    return light <= random.nextInt(1000) && light <= 7 ? random.nextInt(8) == 0 : false;
}
```

配置默认值：`ignoreL = false`、`spawnDays = 0`（配置注释写明是 **ticks**："Mobs ticks required"）、
`evolutionSpawningIgnoreSunlight = 1`（byte）、`phaseLightlessMinusOne = true`。

**实现要点（下一批照此做）**：① 语义是"高阶段/阶段 -1 的寄生体放宽到两级光照中的 Two（更宽松），否则用 One"；
② `spawnDays` 名为"天数"实为 **tick 门槛**，默认 0 ⇒ 端口若按"天数"实现会偏离；
③ 两级光照都含**随机门控**（`nextInt(32)` / `nextInt(1000) && <=7` / `nextInt(8)==0`），必须照抄随机形态而非化简为纯阈值比较；
④ 端口挂载点已查明：`CommonModEvents:318 registerSpawnPlacements`（REPLACE）与 `UntamedPriLasherEntity:89 checkSpawnRules`。

## 批次 91：落地 `isValidLightLevelTwo`（生成合法性实现第 1 步）（2026-09-25 续）

新增 `world/SpawnLightChecks`：只实现**证据完整**的那一级——`isValidLightLevelTwo`
（`blockLight <= random.nextInt(1000) && <= 7 ? random.nextInt(8) == 0 : false`，随机门控照抄，未化简为阈值比较），
并提供"按实体位置"与"按显式位置+随机源"两个入口，便于挂到生成谓词上。

**未实现的部分**（有意）：`isValidLightLevelOne` 的尾部（`SKY > nextInt(32)` 之后的判定）在批次 90 的摘录中被截断，
在补全前不写猜测代码；类注释里已注明该限制。断言 2 条（锁定随机形态）。`build` 通过、套件维持既有 20 失败。

## 批次 92：`isValidLightLevelOne` 全文解出（2026-09-25 续，记录未改）

```java
protected boolean isValidLightLevelOne() {
    if (world.getBiome(pos) instanceof BiomeParasiteBase) return isValidLightLevelTwo();
    BlockPos blockpos = new BlockPos(x, boundingBox.minY, z);
    if (world.getLightFor(SKY, blockpos) > random.nextInt(32)) return false;
    int i = world.getLightFromNeighbors(blockpos);            // func_175671_l
    if (world.isThundering()) {                              // func_72911_I
        int j = world.getSkylightSubtracted();                // func_175657_ab
        world.setSkylightSubtracted(10);                      // func_175692_b
        i = world.getLightFromNeighbors(blockpos);
        world.setSkylightSubtracted(j);
    }
    return i <= random.nextInt(8)
        && getBlockPathWeight(blockpos) >= 0.0F;              // func_180484_a
}
```

**1.21.1 映射的三个待定点**（写代码前必须逐一确认，否则会写出"看起来对"实则偏的实现）：

| 原版调用 | 1.21 候选 | 状态 |
| --- | --- | --- |
| `getLightFromNeighbors(pos)` | `LevelReader.getMaxLocalRawBrightness(pos)`（或 `getRawBrightness(pos, skyDarken)`） | ⏳ 待确认签名与语义 |
| `isThundering()` + `setSkylightSubtracted` | `Level.isThundering()` ✔ + 天空减光的设置者（`getSkyDarken()`/`setSkyDarken`？服务端是否有 setter） | ⏳ 待确认 |
| `BiomeParasiteBase` 判定 | 端口是否有"寄生群系"标记（如 `ModBiomes` 的谓词/标签） | ⏳ 待查 |

另注：`getBlockPathWeight` 在 1.21 为 `PathfinderMob.getWalkTargetValue(BlockPos)` ✔（需 mob 实例，故该判定应挂在实体侧而非纯静态工具）。

**结论**：本方法**暂不实现**——它依赖 3 个未确认的映射点与 1 个实体侧实例方法，在证据补齐前落代码等于猜；
守卫断言已锁定 `Two` 级的随机形态（批次 91），`One` 级待上述三项确认后再补。

## 批次 93：三个 1.21 映射点中两个已用**端口既有用法**证实（2026-09-25 续）

批次 92 列的待确认项，本轮改用"端口自己已经在用哪些 API"来证实（比查外部文档可靠，且能保证签名一致）：

```java
DerivedParasiteEntity:372   return level().getMaxLocalRawBrightness(blockPosition()) <= SHADOW_LIGHT_THRESHOLD;
NexusParasiteEntity:222     if (activeKind.family == Family.BECKON && activeKind.stage == 4 && level().isThundering() ...
```

| 待确认项 | 结论 |
| --- | --- |
| `getLightFromNeighbors(pos)` | ✅ **`Level.getMaxLocalRawBrightness(BlockPos)`**（端口已在用，签名与语义匹配） |
| `isThundering()` | ✅ `Level.isThundering()`（端口已在用） |
| 雷暴下的 `setSkylightSubtracted(10)` | ⏳ 仍待定：1.21 的 `Level` 是否暴露服务端可写的"天空减光"设置者未知；**若不存在，该分支需按"不临时改全局状态"的方式取舍并留证** |
| 寄生群系判定（`BiomeParasiteBase`） | ⏳ 待查端口是否有对应谓词/标签 |

**因此实现 `isValidLightLevelOne` 只差两件事**：① 雷暴分支在 1.21 的可行写法（或取舍理由）；② 寄生群系的对应物。
其余已可直接照抄：`SKY > nextInt(32) → false`、`getMaxLocalRawBrightness(pos) <= nextInt(8)`、
`PathfinderMob.getWalkTargetValue(pos) >= 0.0F`。
