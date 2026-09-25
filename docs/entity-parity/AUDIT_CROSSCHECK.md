# 审计交叉校验（AI 条款 premise 校验）

> 生成时间：2026-09-24T13:07:08.057Z；模式：仅报告。
> 方法：对每条 `ai` 条款抽取被引用的 AI 类名，与该生物的原版任务地面真值（`original-ai-tasks.json`）比对，
> 并回落到继承链上的 `new <类名>(` 注册点；两者都找不到即为 premise 错误（原版根本没有该任务），
> `--apply` 会将其判为 `na` 并记录 `reviewNote`；同时报告「原版有、审计未提」的遗漏任务。
> 背景：`docs/entity-parity/original-ai-baseline.json`（110/127 只生物覆盖了 `initEntityAI` 且未调用 `super`）。

- 校验条款：162
- premise 成立：126
- premise 被推翻：36（已修正 0 条）
- 未被任何条款覆盖的原版任务：7

| 生物 | 被推翻条款 | 判定 | 未注册的 AI 类 |
| --- | --- | --- | --- |
| `buglin` | tasks.addTask(2, EntityAIAttackMelee(this, 1.0, false))（EntityMob 继承的近战任务） | na | EntityAIAttackMelee |
| `buglin` | targetTasks.addTask(2, EntityAINearestAttackableTarget(EntityPlayer))（EntityMob 继承） | na | EntityAINearestAttackableTarget |
| `buglin` | tasks.addTask(5, EntityAIWanderAvoidWater(1.0))（EntityMob 继承） | na | EntityAIWanderAvoidWater |
| `buglin` | tasks.addTask(6, EntityAIWatchClosest(EntityPlayer, 8.0F))（EntityMob 继承） | na | EntityAIWatchClosest |
| `buglin` | tasks.addTask(6, EntityAILookIdle)（EntityMob 继承） | na | EntityAILookIdle |
| `pri_longarms` | tasks.addTask(2, EntityAIAttackMelee(this, 1.0, false))（EntityMob 继承） | na | EntityAIAttackMelee |
| `sim_bigspider` | 继承 EntityMob：tasks.addTask(2, EntityAIAttackMelee) | na | EntityAIAttackMelee |
| `sim_bigspider` | 继承 EntityMob：tasks.addTask(7, EntityAIWander) | na | EntityAIWander |
| `sim_bigspider` | 继承 EntityMob：tasks.addTask(8, EntityAIWatchClosest(EntityPlayer, 8)) | na | EntityAIWatchClosest |
| `sim_bigspider` | 继承 EntityMob：targetTasks.addTask(2, EntityAINearestAttackableTarget(EntityPlayer)) | na | EntityAINearestAttackableTarget |
| `sim_cow` | 继承 EntityMob：tasks.addTask(0, EntityAISwimming) | na | EntityAISwimming |
| `sim_cow` | 继承 EntityMob：tasks.addTask(2, EntityAIAttackMelee) | na | EntityAIAttackMelee |
| `sim_cow` | 继承 EntityMob：tasks.addTask(7, EntityAIWander) | na | EntityAIWander |
| `sim_cow` | 继承 EntityMob：tasks.addTask(8, EntityAIWatchClosest(EntityPlayer, 8)) | na | EntityAIWatchClosest |
| `sim_cow` | 继承 EntityMob：targetTasks.addTask(2, EntityAINearestAttackableTarget(EntityPlayer)) | na | EntityAINearestAttackableTarget |
| `sim_human` | 继承 EntityMob：tasks.addTask(0, EntityAISwimming) | na | EntityAISwimming |
| `sim_human` | 继承 EntityMob：tasks.addTask(2, EntityAIAttackMelee) | na | EntityAIAttackMelee |
| `sim_human` | 继承 EntityMob：tasks.addTask(7, EntityAIWander) | na | EntityAIWander |
| `sim_human` | 继承 EntityMob：tasks.addTask(8, EntityAIWatchClosest(EntityPlayer, 8)) | na | EntityAIWatchClosest |
| `sim_human` | 继承 EntityMob：targetTasks.addTask(2, EntityAINearestAttackableTarget(EntityPlayer)) | na | EntityAINearestAttackableTarget |
| `sim_sheep` | 继承 EntityMob：tasks.addTask(0, EntityAISwimming) | na | EntityAISwimming |
| `sim_sheep` | 继承 EntityMob：tasks.addTask(2, EntityAIAttackMelee) | na | EntityAIAttackMelee |
| `sim_sheep` | 继承 EntityMob：tasks.addTask(7, EntityAIWander) | na | EntityAIWander |
| `sim_sheep` | 继承 EntityMob：tasks.addTask(8, EntityAIWatchClosest(EntityPlayer, 8)) | na | EntityAIWatchClosest |
| `sim_sheep` | 继承 EntityMob：targetTasks.addTask(2, EntityAINearestAttackableTarget(EntityPlayer)) | na | EntityAINearestAttackableTarget |
| `sim_squid` | 继承 EntityMob：tasks.addTask(0, EntityAISwimming) | na | EntityAISwimming |
| `sim_squid` | 继承 EntityMob：tasks.addTask(2, EntityAIAttackMelee) | na | EntityAIAttackMelee |
| `sim_squid` | 继承 EntityMob：tasks.addTask(7, EntityAIWander) | na | EntityAIWander |
| `sim_squid` | 继承 EntityMob：tasks.addTask(8, EntityAILookIdle) | na | EntityAILookIdle |
| `sim_squid` | 继承 EntityMob：tasks.addTask(8, EntityAIWatchClosest(EntityPlayer, 8)) | na | EntityAIWatchClosest |
| `sim_squid` | 继承 EntityMob：targetTasks.addTask(2, EntityAINearestAttackableTarget(EntityPlayer)) | na | EntityAINearestAttackableTarget |
| `sim_wolf` | 继承 EntityMob：tasks.addTask(0, EntityAISwimming) | na | EntityAISwimming |
| `sim_wolf` | 继承 EntityMob：tasks.addTask(2, EntityAIAttackMelee) | na | EntityAIAttackMelee |
| `sim_wolf` | 继承 EntityMob：tasks.addTask(7, EntityAIWander) | na | EntityAIWander |
| `sim_wolf` | 继承 EntityMob：tasks.addTask(8, EntityAIWatchClosest(EntityPlayer, 8)) | na | EntityAIWatchClosest |
| `sim_wolf` | 继承 EntityMob：targetTasks.addTask(2, EntityAINearestAttackableTarget(EntityPlayer)) | na | EntityAINearestAttackableTarget |

## 未被条款覆盖的原版任务（审计遗漏，补齐时必须补条款）

| 生物 | 原版任务 | 优先级 | 列表 | 来源类 | 位置 |
| --- | --- | ---: | --- | --- | --- |
| `buglin` | `EntityAINearestAttackableTargetStatus` | 5 | targetTasks | EntityParasiteBase | `EntityParasiteBase.java:243` |
| `sim_bigspider` | `EntityAIInfectedSearch` | 3 | tasks | EntityPInfected | `EntityPInfected.java:112` |
| `sim_cow` | `EntityAIInfectedSearch` | 3 | tasks | EntityPInfected | `EntityPInfected.java:112` |
| `sim_human` | `EntityAIInfectedSearch` | 3 | tasks | EntityPInfected | `EntityPInfected.java:112` |
| `sim_sheep` | `EntityAIInfectedSearch` | 3 | tasks | EntityPInfected | `EntityPInfected.java:112` |
| `sim_squid` | `EntityAIInfectedSearch` | 3 | tasks | EntityPInfected | `EntityPInfected.java:112` |
| `sim_wolf` | `EntityAIInfectedSearch` | 3 | tasks | EntityPInfected | `EntityPInfected.java:112` |
