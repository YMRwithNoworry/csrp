# 审计证据校验（工程侧行号）

> 生成时间：2026-09-24T13:08:58.063Z；窗口 ±12 行。
> 方法：对每条带 `evidence.project` 的条款，检查该文件是否存在、并确认 `文件:行号±窗口` 中至少出现一个条款里提到的标识符。
> 用途：抓「凭空行号」的证据造假，以及在切换基线后量化行号漂移。

- 校验条数：703
- 命中：185
- 未命中（可疑/疑似漂移）：518
- 文件不存在：0
- 缺 project 证据（判定为 satisfied 时不允许）：0

| 类型 | 生物/领域 | 面 | 条款 | 证据 | 状态 | 期望标识符 |
| --- | --- | --- | --- | --- | --- | --- |
| creature | `beckon_siii` | registration | CreateEntityMob 的 active 形参为常量 true（本生物无 per-mob 启用开关） | `src/main/java/alku/csrp/registry/ModEntities.java:629` | unmatched | CreateEntityMob、active |
| creature | `beckon_siii` | registration | func_70105_a(0.7F, 5.1F) 碰撞箱 | `src/main/java/alku/csrp/registry/ModEntities.java:451` | unmatched | func_70105_a |
| creature | `beckon_siii` | registration | func_70047_e() 返回 4.9F 眼高 | `src/main/java/alku/csrp/registry/ModEntities.java:450` | unmatched | func_70047_e |
| creature | `beckon_siii` | registration | EntityEntryBuilder.tracker(64, 3, true) 追踪范围 64 / 更新间隔 3 / 速度同步 | `src/main/java/alku/csrp/registry/ModEntities.java:630` | unmatched | EntityEntryBuilder、tracker |
| creature | `beckon_siii` | registration | SRPConfig.vanillaEggs 开关与刷怪蛋颜色 3224855/3224855（0x313737） | `src/main/java/alku/csrp/registry/ModItems.java:300` | unmatched | SRPConfig、vanillaEggs、x313737 |
| creature | `beckon_siii` | registration | 注册为怪物类别（EntityMob 系；EntityParasiteBase.isCreatureType 参与刷怪计数） | `src/main/java/alku/csrp/registry/ModEntities.java:629` | unmatched | EntityMob、EntityParasiteBase、isCreatureType |
| creature | `beckon_siii` | attributes | MAX_HEALTH = SRPAttributes.VENKROLSIII_HEALTH = 110.0 | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:1042` | unmatched | MAX_HEALTH、SRPAttributes、VENKROLSIII_HEALTH |
| creature | `beckon_siii` | attributes | ARMOR = VENKROLSIII_ARMOR = 16.0 | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:1042` | unmatched | ARMOR、VENKROLSIII_ARMOR |
| creature | `beckon_siii` | attributes | ATTACK_DAMAGE = VENKROLSIII_ATTACK_DAMAGE = 13.0 | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:1042` | unmatched | ATTACK_DAMAGE、VENKROLSIII_ATTACK_DAMAGE |
| creature | `beckon_siii` | ai | tasks.addTask(5, this.aiWander) 被 EntityPStationary 构造器移除 | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:152` | unmatched | aiWander、EntityPStationary |
| creature | `beckon_siii` | ai | tasks.addTask(6, this.folow) 被移除（不跟随同类） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:152` | unmatched | folow |
| creature | `beckon_siii` | ai | tasks.addTask(5, this.jumpT) EntityAIJumping 被移除 | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:152` | unmatched | jumpT、EntityAIJumping |
| creature | `beckon_siii` | ai | targetTasks(4) EntityAINearestAttackableTargetStatus(EntityPlayer, 0,true,true,null,1.0,1.0F) | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:158` | unmatched | EntityAINearestAttackableTargetStatus、EntityPlayer |
| creature | `beckon_siii` | ai | targetTasks(4) EntityAINearestAttackableTargetStatus(EntityLiving 非水生/非动物/非村民, mobattacking) | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:158` | unmatched | EntityAINearestAttackableTargetStatus、EntityLiving、mobattacking |
| creature | `beckon_siii` | ai | targetTasks(1) EntityAIHurtByTarget(this, true, new Class[0]) | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:157` | unmatched | EntityAIHurtByTarget、Class |
| creature | `beckon_siii` | ai | tasks.addTask(4, EntityAINexusGrow(this, 3)) 成长目标（type 1 = beckon 家族） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:189` | unmatched | EntityAINexusGrow、type、beckon |
| creature | `beckon_siii` | ai | tasks.addTask(7, EntityAIBlockLight(this, 20, 5))（SRPConfig.canTargetBlock 开关） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:237` | unmatched | EntityAIBlockLight、SRPConfig、canTargetBlock |
| creature | `beckon_siii` | behaviors | func_70653_a 空实现（免疫击退）+ func_96092_aw=false（不被水推动） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:329` | unmatched | func_70653_a、func_96092_aw |
| creature | `beckon_siii` | behaviors | 召唤配额体系：totalP=venkrolsiiiTotalActiveMobs(12)+venkrolsiiilimit(2)、mobID/mobPT 数组、addID/IDable/checkID/setActualParasites | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:607` | unmatched | totalP、venkrolsiiiTotalActiveMobs、venkrolsiiilimit、mobID、mobPT、addID |
| creature | `beckon_siii` | behaviors | 召唤表 VENKROLSIII_MOBTABLEG/A（venkrolsiiimoblist 26 条 ground/air 权重表，含 sim_/carrier/vigilante 等） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:631` | unmatched | VENKROLSIII_MOBTABLEG、venkrolsiiimoblist、ground、sim_、carrier、vigilante |
| creature | `beckon_siii` | behaviors | 生成时 SRPConfigMobs.venkrolsiiiRange=32 / venkrolsiiiRangeY=8 决定召唤半径（经 spawnBiomassFromBeckon） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:641` | unmatched | SRPConfigMobs、venkrolsiiiRange、venkrolsiiiRangeY、spawnBiomassFromBeckon |
| creature | `beckon_siii` | behaviors | 被召唤的 beckon 由父柱 setCanGrowTo(false)+setLifeB(300) 转为限时临时柱，300t 后自伤 5000000 死亡 | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:950` | unmatched | beckon、setCanGrowTo、setLifeB |
| creature | `beckon_siii` | behaviors | func_70074_a 击杀后处理链（killcount、进化分值、PARATE 加成、治疗）在柱体上的表现 | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:705` | unmatched | func_70074_a、killcount、PARATE |
| creature | `beckon_siii` | behaviors | onDeathDislo：setDisloWorldPhase(EVENTPARANEXUSIIID, chanceEventParaNexusIIID) | `src/main/java/alku/csrp/world/DislodgmentSystem.java:574` | unmatched | onDeathDislo、setDisloWorldPhase、EVENTPARANEXUSIIID、chanceEventParaNexusIIID |
| creature | `beckon_siii` | damage_and_effects | KILLNEX_E 减伤：parasiteKillingReduction*(amp+1)，上限 0.95 | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:333` | unmatched | KILLNEX_E、parasiteKillingReduction |
| creature | `beckon_siii` | damage_and_effects | 伤害适应：pointReduction 0.17 / pointCap 6 / DamageTypeCap(nexussiiiPointDamCap) 15 / chanceLearn 0.9 / 火焰失败率 0.3 | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:334` | unmatched | pointReduction、pointCap、DamageTypeCap、nexussiiiPointDamCap、chanceLearn |
| creature | `beckon_siii` | damage_and_effects | newDamageCooldown = SRPConfig.adaptationNewDamageCooldon（新伤害类型学习冷却） | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:82` | unmatched | newDamageCooldown、SRPConfig、adaptationNewDamageCooldon |
| creature | `beckon_siii` | damage_and_effects | RES_E 效果每 srpTicks==10 削减 1 点适应（removeAllResistance(1)） | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:151` | unmatched | RES_E、srpTicks、removeAllResistance |
| creature | `beckon_siii` | damage_and_effects | 火焰伤害乘数 firemultyplier（skin 120 减半） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:317` | unmatched | firemultyplier、skin |
| creature | `beckon_siii` | damage_and_effects | 近战命中按 cothSpread 概率传播 COTH（本生物继承 EntityParasiteBase 默认 cothSpread=0 → 不传播） | `src/main/java/alku/csrp/infection/InfectionMechanics.java:160` | unmatched | cothSpread、COTH、EntityParasiteBase |
| creature | `beckon_siii` | damage_and_effects | 最低伤害 MiniDamage=0（attackEntityAsMobMinimum 因 0 直接返回） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:1042` | unmatched | MiniDamage、attackEntityAsMobMinimum |
| creature | `beckon_siii` | sync_data | SPECIAL(byte) 寄生虫状态同步（0/1/2/3/9/10 等驱动动画层） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:60` | unmatched | SPECIAL、byte |
| creature | `beckon_siii` | sync_data | HIT(byte) 适应命中档位同步（getHitStatus，1=未到上限 2=到上限 3=吞噬） | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:71` | unmatched | byte、getHitStatus |
| creature | `beckon_siii` | sync_data | NBT 持久化：parasitetype(SKIN)/parasitekills/phasecreat/levelcreat/paracolony/beckonlifeleft/tickse/neededtime/cangrowto/par | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:401` | unmatched | parasitetype、SKIN、parasitekills、phasecreat、levelcreat、paracolony |
| creature | `beckon_siii` | sync_data | EntityPStationaryArchitect 另有 setGT 初始 neededTime 与 actualTime 同步（成长进度） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:404` | unmatched | EntityPStationaryArchitect、setGT、neededTime、actualTime |
| creature | `beckon_siii` | animation | ModelVenkrolSIII 待机动画（ageInTicks 驱动的触须/躯体摆动） | `src/main/resources/assets/csrp/animations/beckon_siii.animation.json:1` | unmatched | ModelVenkrolSIII、ageInTicks |
| creature | `beckon_siii` | animation | ModelVenkrolSIII 依 getBODY() 的躯体伸缩动画 | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:463` | unmatched | ModelVenkrolSIII、getBODY |
| creature | `beckon_siii` | animation | ModelVenkrolSIII 依 getFloorTimer() 的底部高度动画 | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:467` | unmatched | ModelVenkrolSIII、getFloorTimer |
| creature | `beckon_siii` | animation | 动画切换无过渡插值（原版每帧直接按状态计算旋转） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:393` | unmatched | — |
| creature | `beckon_siii` | model_texture | 模型 ModelVenkrolSIII（client/model/entity/deterrent/nexus/ModelVenkrolSIII.java） | `src/main/resources/assets/csrp/geo/beckon_siii.geo.json:1` | unmatched | ModelVenkrolSIII、client、model、entity、deterrent、nexus |
| creature | `beckon_siii` | model_texture | 贴图 srparasites:textures/entity/monster/venkrolsiii.png | `src/main/resources/assets/csrp/textures/entity/beckon_siii.png:1` | unmatched | srparasites、textures、entity、monster、venkrolsiii |
| creature | `beckon_siii` | model_texture | 阴影半径 0.6F | `src/main/java/alku/csrp/client/ClientModEvents.java:326` | unmatched | — |
| creature | `beckon_siii` | model_texture | 渲染器按 skin 切换贴图的 getEntityTexture 分支 | `src/main/java/alku/csrp/client/renderer/NexusParasiteRenderer.java:8` | unmatched | skin、getEntityTexture |
| creature | `beckon_siii` | sounds | getHurtSound = (rand.nextBoolean() && getHitStatus()>0) ? MOBSILENCE : VENKROLSIII_HURT | `src/main/java/alku/csrp/entity/ParasiteSoundProfiles.java:99` | unmatched | getHurtSound、rand、nextBoolean、getHitStatus、MOBSILENCE、VENKROLSIII_HURT |
| creature | `beckon_siii` | sounds | getDeathSound = SRPSounds.VENKROLSIII_DEATH | `src/main/java/alku/csrp/entity/ParasiteSoundProfiles.java:105` | unmatched | getDeathSound、SRPSounds、VENKROLSIII_DEATH |
| creature | `beckon_siii` | sounds | func_70599_aP() = 1.0F（受伤音效音量） | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:368` | unmatched | func_70599_aP |
| creature | `beckon_siii` | spawning | SRPSpawning 中不存在 beckon_siii 的自然生成条目（本生物只由结构/增援系统/AI 召唤产生） | `src/main/java/alku/csrp/world/NaturalSpawnTables.java:1` | unmatched | SRPSpawning、beckon_siii |
| creature | `beckon_siii` | spawning | 增援系统（RS）按殖民地分值选择 beckon 阶：bonus>2.0 → beckon_siii | `src/main/java/alku/csrp/world/ReinforcementSystem.java:90` | unmatched | beckon、bonus、beckon_siii |
| creature | `beckon_siii` | spawning | RS 触发条件：寄生体死亡时 spawnBeckon（本生物尺寸 >1 判定、nexusVenkrolCap 上限、nexusVenkrolDis 距离、阶段概率、rsCooldown） | `src/main/java/alku/csrp/world/ReinforcementSystem.java:100` | unmatched | spawnBeckon、nexusVenkrolCap、nexusVenkrolDis、rsCooldown |
| creature | `beckon_siii` | loot | 掉落由 setLoot 事件按 getParasiteIDRegister()==19 分派，受 doMobLoot 与 DEBAR_E 门控 | `src/main/resources/data/csrp/loot_table/entities/beckon_siii.json:1` | unmatched | setLoot、getParasiteIDRegister、doMobLoot、DEBAR_E |
| creature | `beckon_siii` | loot | 经验 = SRPAttributes.XP_ADAPTED * 2（55*2 = 110） | `src/main/java/alku/csrp/entity/NexusParasiteEntity.java:1042` | unmatched | SRPAttributes、XP_ADAPTED |
| creature | `buglin` | registration | func_70105_a(0.5F, 0.3F) 碰撞箱 | `src/main/java/alku/csrp/registry/ModEntities.java:138` | unmatched | func_70105_a |
| creature | `buglin` | registration | func_70047_e() 返回 0.3F 眼高 | `src/main/java/alku/csrp/registry/ModEntities.java:137` | unmatched | func_70047_e |
| creature | `buglin` | registration | EntityEntryBuilder.tracker(64, 3, true) 追踪范围 64 / 更新间隔 3 / 速度同步 | `src/main/java/alku/csrp/registry/ModEntities.java:139` | unmatched | EntityEntryBuilder、tracker |
| creature | `buglin` | registration | vanillaEggs 开关与刷怪蛋颜色 3224855/3224855 | `src/main/java/alku/csrp/registry/ModItems.java:68` | unmatched | vanillaEggs |
| creature | `buglin` | attributes | lodoHealthMultiplier/lodoArmorMultiplier/lodoDamageMultiplier/lodoKDResistanceMultiplier 与全局乘数相乘 | `src/main/java/alku/csrp/config/OriginalConfigEvents.java:38` | unmatched | lodoHealthMultiplier、lodoArmorMultiplier、lodoDamageMultiplier、lodoKDResistanceMultiplier |
| creature | `buglin` | ai | targetTasks.addTask(1, EntityAIHurtByTarget(this, true, new Class[0])) | `src/main/java/alku/csrp/entity/BuglinEntity.java:97` | unmatched | EntityAIHurtByTarget、Class |
| creature | `buglin` | ai | tasks.addTask(0, EntityAISwimming)（EntityMob 继承） | `src/main/java/alku/csrp/entity/BuglinEntity.java:91` | unmatched | EntityAISwimming、EntityMob |
| creature | `buglin` | ai | tasks.addTask(5, EntityAIWanderStatus(1.0, 120, 0.001, true)) | `src/main/java/alku/csrp/entity/BuglinEntity.java:95` | unmatched | EntityAIWanderStatus |
| creature | `buglin` | ai | tasks.addTask(6, EntityAIParasiteFollow(1.3, 16.0, 6.0, true)) | `src/main/java/alku/csrp/entity/BuglinEntity.java:94` | unmatched | EntityAIParasiteFollow |
| creature | `buglin` | behaviors | growStage 触发条件 actualGrowtime > totalGrowtime && ParasiteEventEntity.canSpawnNext \|\| killcount > 1000 | `src/main/java/alku/csrp/entity/BuglinEntity.java:134` | unmatched | growStage、actualGrowtime、totalGrowtime、ParasiteEventEntity、canSpawnNext、killcount |
| creature | `buglin` | behaviors | 进化时播放 LODO_MUDO 并 spawnNext(new EntityMudo)（继承自定义名/持久性） | `src/main/java/alku/csrp/entity/BuglinEntity.java:152` | unmatched | LODO_MUDO、spawnNext、EntityMudo |
| creature | `buglin` | behaviors | type = 1、buried 初始 -1.0 | `src/main/java/alku/csrp/entity/BuglinEntity.java:54` | unmatched | type、buried |
| creature | `buglin` | behaviors | setFloorTimer() 置 buried = 1.0，由虫灵方块 BlockBuglin 生成实体时调用 | `src/main/java/alku/csrp/entity/BuglinEntity.java:84` | unmatched | setFloorTimer、buried、BlockBuglin |
| creature | `buglin` | behaviors | buried == 1.0 时播放 SRPSounds.LODO_EMERGE（SoundCategory.BLOCKS） | `src/main/java/alku/csrp/entity/BuglinEntity.java:116` | unmatched | buried、SRPSounds、LODO_EMERGE、SoundCategory、BLOCKS |
| creature | `buglin` | behaviors | NBT "ruptergrow" 保存/读取 actualGrowtime | `src/main/java/alku/csrp/entity/BuglinEntity.java:187` | unmatched | ruptergrow、actualGrowtime |
| creature | `buglin` | behaviors | func_184645_a 无特殊交互（仅转发 super） | `src/main/java/alku/csrp/entity/BuglinEntity.java:42` | unmatched | func_184645_a、super |
| creature | `buglin` | sync_data | 埋地状态同步到客户端（func_70103_a(50) 设置 buried = 1.0） | `src/main/java/alku/csrp/entity/BuglinEntity.java:115` | unmatched | func_70103_a、buried |
| creature | `buglin` | sync_data | NBT 持久化 parasitetype/parasitekills/phasecreat 等（func_70014_b） | `src/main/java/alku/csrp/entity/BuglinEntity.java:185` | unmatched | parasitetype、parasitekills、phasecreat、func_70014_b |
| creature | `buglin` | animation | 常驻摆动动画（ModelLodo 中 ageInTicks*0.643219 的 idle 分支） | `src/main/java/alku/csrp/entity/BuglinEntity.java:47` | unmatched | ModelLodo、ageInTicks、idle |
| creature | `buglin` | animation | 行走动画（getFloorTimer() < 0 分支，用 limbSwing/limbSwingAmount 摆动 joint1..5） | `src/main/java/alku/csrp/entity/BuglinEntity.java:47` | unmatched | getFloorTimer、limbSwing、limbSwingAmount、joint1 |
| creature | `buglin` | animation | 埋地/出土动画（getFloorTimer() >= 0 分支，mainbody.offsetZ = buried） | `src/main/java/alku/csrp/entity/BuglinEntity.java:49` | unmatched | getFloorTimer、mainbody、offsetZ、buried |
| creature | `buglin` | model_texture | 模型 ModelLodo（client/model/entity/inborn/ModelLodo.java） | `src/main/resources/assets/csrp/geo/buglin.geo.json:1` | unmatched | ModelLodo、client、model、entity、inborn、java |
| creature | `buglin` | model_texture | 阴影半径 0.2F | `src/main/java/alku/csrp/client/renderer/BuglinRenderer.java:10` | unmatched | — |
| creature | `buglin` | model_texture | 模型尺寸/可见边界与 0.5x0.3 碰撞箱匹配 | `src/main/resources/assets/csrp/geo/buglin.geo.json:7` | unmatched | — |
| creature | `buglin` | sounds | 进化播放 SRPSounds.LODO_MUDO（音量/音高 1.0） | `src/main/java/alku/csrp/entity/BuglinEntity.java:152` | unmatched | SRPSounds、LODO_MUDO |
| creature | `buglin` | sounds | 出土播放 SRPSounds.LODO_EMERGE，原版 SoundCategory.BLOCKS | `src/main/java/alku/csrp/entity/BuglinEntity.java:116` | unmatched | SRPSounds、LODO_EMERGE、SoundCategory、BLOCKS |
| creature | `buglin` | spawning | 非阶段路径：所有生物群系 MONSTER 表加入 Lodo（weight lodoSpawnRate=30, group 2-5） | `src/main/java/alku/csrp/world/NaturalSpawnTables.java:288` | unmatched | MONSTER、Lodo、weight、lodoSpawnRate、group |
| creature | `buglin` | spawning | 光照规则 isValidLightLevelOne/Two（天空光随机阈值 + 方块光 <=7 时 1/8 概率） | `src/main/java/alku/csrp/entity/BuglinEntity.java:76` | unmatched | isValidLightLevelOne |
| creature | `buglin` | spawning | 虫灵方块 BlockBuglin 在条件满足时生成 Lodo 并置埋地计时 | `src/main/java/alku/csrp/block/TunnelBlock.java:93` | unmatched | BlockBuglin、Lodo |
| creature | `buglin` | loot | 未覆写 dropFewItems/entityDropItem → 无物品掉落 | `src/main/resources/data/csrp/loot_table/entities/buglin.json:1` | unmatched | dropFewItems、entityDropItem |
| creature | `buglin` | loot | 经验值 = SRPAttributes.XP_LiTTLE（= infectedXPValue/2 = 4） | `src/main/java/alku/csrp/entity/BuglinEntity.java:60` | unmatched | SRPAttributes、XP_LiTTLE、infectedXPValue |
| creature | `fer_villager` | registration | 碰撞箱 func_70105_a(0.6F, 1.95F) | `src/main/java/alku/csrp/registry/ModEntities.java:348` | unmatched | func_70105_a |
| creature | `fer_villager` | registration | 眼高 func_70047_e() 返回 1.73F | `src/main/java/alku/csrp/registry/ModEntities.java:348` | unmatched | func_70047_e |
| creature | `fer_villager` | registration | EntityEntryBuilder.tracker(64, 3, true)：追踪 64 格 / 更新间隔 3 / 速度同步 | `src/main/java/alku/csrp/registry/ModEntities.java:627` | unmatched | EntityEntryBuilder、tracker |
| creature | `fer_villager` | registration | 刷怪蛋颜色 8611072/16711900 | `src/main/java/alku/csrp/registry/ModItems.java:223` | unmatched | — |
| creature | `fer_villager` | registration | getParasiteIDRegister() 数值 id 99 用于掉落/群体派发 | `src/main/resources/data/csrp/loot_table/entities/fer_villager.json:1` | unmatched | getParasiteIDRegister |
| creature | `fer_villager` | attributes | 生命 SRPAttributes.FERVILLAGER_HEALTH = 27.0 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:268` | unmatched | SRPAttributes、FERVILLAGER_HEALTH |
| creature | `fer_villager` | attributes | 护甲 FERVILLAGER_ARMOR = 8.0 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:268` | unmatched | FERVILLAGER_ARMOR |
| creature | `fer_villager` | attributes | 攻击力 FERVILLAGER_ATTACK_DAMAGE = 17.0 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:268` | unmatched | FERVILLAGER_ATTACK_DAMAGE |
| creature | `fer_villager` | attributes | 击退抗性 FERVILLAGER_KD_RESISTANCE = 0.9 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:268` | unmatched | FERVILLAGER_KD_RESISTANCE |
| creature | `fer_villager` | attributes | 移动速度 0.260000004172325 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:268` | unmatched | — |
| creature | `fer_villager` | attributes | 跟随范围 SRPConfig.feralFollow = 20.0 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:268` | unmatched | SRPConfig、feralFollow |
| creature | `fer_villager` | attributes | 经验 field_70728_aV = SRPAttributes.XP_FERAL = SRPConfig.feralXPValue = 16 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:268` | unmatched | field_70728_aV、SRPAttributes、XP_FERAL、SRPConfig、feralXPValue |
| creature | `fer_villager` | attributes | setScentHPMultiplier(1.5F)：Fer 在嗅觉宿主血量计算中的倍率 | `src/main/java/alku/csrp/entity/ParasiticScentEntity.java:197` | unmatched | setScentHPMultiplier |
| creature | `fer_villager` | ai | EntityMob 默认 tasks（游泳/近战/游荡/注视/环视）被子类 func_184651_r 覆写且未调用 super | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:80` | unmatched | EntityMob、func_184651_r、super |
| creature | `fer_villager` | ai | EntityParasiteBase 构造：tasks.addTask(0, EntityAIWait) | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:81` | unmatched | EntityParasiteBase、EntityAIWait |
| creature | `fer_villager` | ai | EntityParasiteBase 构造：tasks.addTask(5, EntityAIWanderStatus(1.0, 120, 0.001F, true)) | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:83` | unmatched | EntityParasiteBase、EntityAIWanderStatus |
| creature | `fer_villager` | ai | EntityParasiteBase 构造：tasks.addTask(6, EntityAIParasiteFollow(1.3, 16, 6, true)) | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:84` | unmatched | EntityParasiteBase、EntityAIParasiteFollow |
| creature | `fer_villager` | ai | EntityPFeral 构造：移除 EntityAIParasiteFollow（func_85156_a(folow)），Fer 不追随主人 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:84` | unmatched | EntityPFeral、EntityAIParasiteFollow、func_85156_a、folow |
| creature | `fer_villager` | ai | EntityPFeral 构造：targetTasks.addTask(4, EntityAINearestAttackableTargetStatus(EntityPlayer, sneakPen = feralSneakPen 1.0, | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:87` | unmatched | EntityPFeral、EntityAINearestAttackableTargetStatus、EntityPlayer、sneakPen、feralSneakPen、inviPen |
| creature | `fer_villager` | ai | EntityPFeral 构造：SRPConfig.mobattacking 时 targetTasks.addTask(4, EntityLiving 目标，排除水栖/动物/黑名单) | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:87` | unmatched | EntityPFeral、SRPConfig、mobattacking、EntityLiving |
| creature | `fer_villager` | ai | EntityFerVillager 构造：targetTasks.addTask(1, EntityAIHurtByTarget(this, true, new Class[0])) | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:86` | unmatched | EntityFerVillager、EntityAIHurtByTarget、Class |
| creature | `fer_villager` | ai | EntityFerVillager 构造：tasks.addTask(0, EntityAISwimmingDiving(this, 0.08)) | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:81` | unmatched | EntityFerVillager、EntityAISwimmingDiving |
| creature | `fer_villager` | ai | EntityFerVillager 构造：tasks.addTask(3, EntityAIAttackMeleeStatus(this, 1.5, false, 0.0)) | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:82` | unmatched | EntityFerVillager、EntityAIAttackMeleeStatus |
| creature | `fer_villager` | ai | EntityFerVillager 构造：tasks.addTask(8, EntityAILookIdle(this)) | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:85` | unmatched | EntityFerVillager、EntityAILookIdle |
| creature | `fer_villager` | ai | EntityAIWanderStatus 门控：parasiteStatus != 0 或 mustUpdate 时不游荡 + 1/120 概率 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:83` | unmatched | EntityAIWanderStatus、parasiteStatus、mustUpdate |
| creature | `fer_villager` | ai | EntityAINearestAttackableTargetStatus 仅在 getParasiteStatus()==0 时索敌，并按潜行/隐身折减距离 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:87` | unmatched | EntityAINearestAttackableTargetStatus、getParasiteStatus |
| creature | `fer_villager` | ai | applyBonuses：phaseCreated ≥ evolutionAssimilatedDehiding(9) 时追加 targetTasks.addTask(5, EntityVillager 目标) | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:87` | unmatched | applyBonuses、phaseCreated、evolutionAssimilatedDehiding、EntityVillager |
| creature | `fer_villager` | ai | applyBonuses：phaseCreated ≥ evolutionTotalKill(9) 时追加 targetTasks.addTask(4, 非玩家 EntityLivingBase 目标) | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:87` | unmatched | applyBonuses、phaseCreated、evolutionTotalKill、EntityLivingBase |
| creature | `fer_villager` | behaviors | func_70652_k 覆写：按实际伤害（含吸收）触发 fearPlayer | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:179` | unmatched | func_70652_k、fearPlayer |
| creature | `fer_villager` | behaviors | 击杀计数 killcount++（skin 120 时仅 1/3 概率） | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:185` | unmatched | killcount、skin |
| creature | `fer_villager` | behaviors | 击杀写入进化点 valueKill = 1（PIVOT 时按倍率放大） | `src/main/java/alku/csrp/world/EvolutionEvents.java:104` | unmatched | valueKill、PIVOT |
| creature | `fer_villager` | behaviors | 死亡扣除进化点 -feralLoosingEPValue = -5（DEBAR 时豁免） | `src/main/java/alku/csrp/world/EvolutionSystem.java:212` | unmatched | feralLoosingEPValue、DEBAR |
| creature | `fer_villager` | behaviors | 超距/闲置消失时 spawnCyst + storeBefDes（canD = SRPConfig.feraldespawn） | `src/main/java/alku/csrp/mixin/MobDespawnMixin.java:16` | unmatched | spawnCyst、storeBefDes、canD、SRPConfig、feraldespawn |
| creature | `fer_villager` | behaviors | 攻击状态机：parasiteStatus 0/1/2 由近战 goal 按距离与冷却切换 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:128` | unmatched | parasiteStatus、goal |
| creature | `fer_villager` | damage_and_effects | 火焰伤害倍率 SRPConfig.firemultyplier = 4.0 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:153` | unmatched | SRPConfig、firemultyplier |
| creature | `fer_villager` | damage_and_effects | KILLFER 效果减伤：parasiteKillingReduction 0.15 ×(amp+1)，上限 0.95 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:152` | unmatched | KILLFER、parasiteKillingReduction |
| creature | `fer_villager` | sync_data | DataManager.register SPECIAL (byte)：parasiteStatus 状态机（0..10） | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:37` | unmatched | DataManager、register、SPECIAL、byte、parasiteStatus |
| creature | `fer_villager` | sync_data | 注册入口 func_70088_a 中一并注册上述 5 个数据参数 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:92` | unmatched | func_70088_a |
| creature | `fer_villager` | sync_data | NBT 持久化 killcount/skin/wait/phaseCreated/levelCreated/madeRng/dislo 等状态 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:191` | unmatched | killcount、skin、wait、phaseCreated、levelCreated、madeRng |
| creature | `fer_villager` | animation | ModelSRP.underground(parasite, ageInTicks, mainbody) 埋地姿态钩子 | `src/main/java/alku/csrp/client/model/CitadelParasiteModel.java:31` | unmatched | ModelSRP、underground、parasite、ageInTicks、mainbody |
| creature | `fer_villager` | model_texture | 模型 ModelFerVillager（1.12 Java 模型，128×64 贴图坐标） | `src/main/resources/assets/csrp/geo/fer_villager.geo.json:1` | unmatched | ModelFerVillager、Java |
| creature | `fer_villager` | model_texture | 贴图 srparasites:textures/entity/monster/fervillager.png | `src/main/java/alku/csrp/client/ClientModEvents.java:249` | unmatched | srparasites、textures、entity、monster、fervillager |
| creature | `fer_villager` | model_texture | 渲染器阴影半径 0.5F | `src/main/java/alku/csrp/client/ClientModEvents.java:249` | unmatched | — |
| creature | `fer_villager` | model_texture | geo 贴图尺寸 128×64 与原版模型一致 | `src/main/resources/assets/csrp/geo/fer_villager.geo.json:1` | unmatched | — |
| creature | `fer_villager` | sounds | 环境音 SRPSounds.INFECTEDHUMAN_GROWL（getParasiteStatus() != 0 时退化为 MOBSILENCE） | `src/main/java/alku/csrp/entity/ParasiteSoundProfiles.java:17` | unmatched | SRPSounds、INFECTEDHUMAN_GROWL、getParasiteStatus、MOBSILENCE |
| creature | `fer_villager` | sounds | 受伤音 SRPSounds.INFECTEDHUMAN_HURT | `src/main/resources/assets/csrp/sounds.json:459` | unmatched | SRPSounds、INFECTEDHUMAN_HURT |
| creature | `fer_villager` | sounds | 死亡音 SRPSounds.INFECTEDHUMAN_DEATH | `src/main/java/alku/csrp/entity/ParasiteSoundProfiles.java:17` | unmatched | SRPSounds、INFECTEDHUMAN_DEATH |
| creature | `fer_villager` | sounds | 自爆音 SRPSounds.MOBEXPLOTION | `src/main/java/alku/csrp/registry/ModSounds.java:110` | unmatched | SRPSounds、MOBEXPLOTION |
| creature | `fer_villager` | spawning | 生成权重 20（非进化） | `src/main/resources/data/csrp/neoforge/biome_modifier/fer_villager_spawns.json:1` | unmatched | — |
| creature | `fer_villager` | spawning | 生成群组大小 3-6（非进化模式） | `src/main/resources/data/csrp/neoforge/biome_modifier/fer_villager_spawns.json:1` | unmatched | — |
| creature | `fer_villager` | spawning | 阶段门控 canSpawninPhase(type=11, phaseMax/Cancel ID 阈值) 决定何时允许生成 | `src/main/java/alku/csrp/world/NaturalSpawnTables.java:325` | unmatched | canSpawninPhase、type、phaseMax、Cancel |
| creature | `fer_villager` | spawning | 生成合法性 func_70601_bi：亮度 isValidLightLevelOne/Two + SRPConfig.ignoreL + spawnDays 天数门控 | `src/main/java/alku/csrp/registry/CommonModEvents.java:374` | unmatched | func_70601_bi、isValidLightLevelOne、SRPConfig、ignoreL、spawnDays |
| creature | `fer_villager` | spawning | 非和平难度才生成（func_175659_aa() != PEACEFUL） | `src/main/java/alku/csrp/registry/CommonModEvents.java:374` | unmatched | func_175659_aa、PEACEFUL |
| creature | `fer_villager` | spawning | 演化/殖民地锁 checkEvoLock(99) / checkColoLock(99) 拒绝生成 | `src/main/java/alku/csrp/world/EvolutionSystem.java:166` | unmatched | checkEvoLock、checkColoLock |
| creature | `fer_villager` | spawning | 宿主被同化后的映射由 COTHVictimParasite 决定（useEvolution 阶段影响 Fer 档位） | `src/main/java/alku/csrp/infection/InfectionMechanics.java:58` | unmatched | COTHVictimParasite、useEvolution |
| creature | `fer_villager` | loot | 掉落派发：getParasiteIDRegister()==99 → 使用 SRPConfigMobs.fervillagerLoot 字符串表 | `src/main/resources/data/csrp/loot_table/entities/fer_villager.json:1` | unmatched | getParasiteIDRegister、SRPConfigMobs、fervillagerLoot |
| creature | `fer_villager` | loot | 默认掉落为空（fervillagerLoot = new String[0]，原版默认不掉任何物品） | `src/main/resources/data/csrp/loot_table/entities/fer_villager.json:1` | unmatched | fervillagerLoot、String |
| creature | `fer_villager` | loot | 经验 XP_FERAL = SRPConfig.feralXPValue = 16 | `src/main/java/alku/csrp/entity/FeralParasiteEntity.java:268` | unmatched | XP_FERAL、SRPConfig、feralXPValue |
| creature | `pri_longarms` | registration | func_70105_a(0.6F, 3.2F) 碰撞箱 | `src/main/java/alku/csrp/registry/ModEntities.java:149` | unmatched | func_70105_a |
| creature | `pri_longarms` | registration | func_70047_e() 返回 2.7F 眼高 | `src/main/java/alku/csrp/registry/ModEntities.java:630` | unmatched | func_70047_e |
| creature | `pri_longarms` | registration | EntityEntryBuilder.tracker(64, 3, true) 追踪范围 64 / 更新间隔 3 / 速度同步 | `src/main/java/alku/csrp/registry/ModEntities.java:630` | unmatched | EntityEntryBuilder、tracker |
| creature | `pri_longarms` | registration | vanillaEggs 开关与刷怪蛋颜色 8350208/4210752 | `src/main/java/alku/csrp/registry/ModItems.java:74` | unmatched | vanillaEggs |
| creature | `pri_longarms` | attributes | extraDamage/currentDamage 使用 SRPAttributes.SHYCO_I_DAMAGE（受伤增伤系数） | `src/main/java/alku/csrp/entity/LongarmsEntity.java:201` | unmatched | extraDamage、currentDamage、SRPAttributes、SHYCO_I_DAMAGE |
| creature | `pri_longarms` | attributes | shycoHealthMultiplier/shycoArmorMultiplier/shycoDamageMultiplier/shycoKDResistanceMultiplier 与全局乘数相乘 | `src/main/java/alku/csrp/config/OriginalConfigEvents.java:38` | unmatched | shycoHealthMultiplier、shycoArmorMultiplier、shycoDamageMultiplier、shycoKDResistanceMultiplier |
| creature | `pri_longarms` | ai | targetTasks.addTask(1, EntityAIHurtByTarget(this, true, new Class[0])) | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:296` | unmatched | EntityAIHurtByTarget、Class |
| creature | `pri_longarms` | ai | tasks.addTask(0, EntityAISwimmingDiving(this, 0.095)) | `src/main/java/alku/csrp/entity/LongarmsEntity.java:115` | unmatched | EntityAISwimmingDiving |
| creature | `pri_longarms` | ai | tasks.addTask(2, EntityAISkill(this, 100, (int)(primitiveFollow*0.7), 2, false, 1)) 冲击波技能 | `src/main/java/alku/csrp/entity/LongarmsEntity.java:384` | unmatched | EntityAISkill、primitiveFollow |
| creature | `pri_longarms` | ai | tasks.addTask(3, EntityAIAttackMeleeStatusAOE(this, 1.3, false, 8.0, 2.5)) | `src/main/java/alku/csrp/entity/LongarmsEntity.java:327` | unmatched | EntityAIAttackMeleeStatusAOE |
| creature | `pri_longarms` | ai | tasks.addTask(8, EntityAILookIdle(this)) | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:294` | unmatched | EntityAILookIdle |
| creature | `pri_longarms` | ai | targetTasks.addTask(4, EntityAINearestAttackableTargetStatus(EntityPlayer, primitiveWalls, primitiveSneakPen, primitiveI | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:297` | unmatched | EntityAINearestAttackableTargetStatus、EntityPlayer、primitiveWalls、primitiveSneakPen、primitiveInviPen |
| creature | `pri_longarms` | ai | targetTasks.addTask(4, EntityAINearestAttackableTargetStatus(EntityLiving 非水生/非动物/非村民, mobattacking)) | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:297` | unmatched | EntityAINearestAttackableTargetStatus、EntityLiving、mobattacking |
| creature | `pri_longarms` | ai | tasks.addTask(5, this.aiWander) EntityAIWanderStatus(1.0, 120, 0.001, true) | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:292` | unmatched | aiWander、EntityAIWanderStatus |
| creature | `pri_longarms` | ai | tasks.addTask(6, this.folow) EntityAIParasiteFollow(1.3, 16.0, 6.0, true) | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:290` | unmatched | folow、EntityAIParasiteFollow |
| creature | `pri_longarms` | behaviors | attackTimer 上冲 0.3/回落 0.15（挥击动画计时，>2.0 结束） | `src/main/java/alku/csrp/entity/LongarmsEntity.java:119` | unmatched | attackTimer |
| creature | `pri_longarms` | behaviors | attackEntityAsMobAOE：以目标为中心的 1.5 膨胀 AABB，对非寄生虫且可见的活体命中，命中寄生虫则清除自身目标 | `src/main/java/alku/csrp/entity/LongarmsEntity.java:182` | unmatched | attackEntityAsMobAOE、AABB |
| creature | `pri_longarms` | behaviors | 近战命中后额外造成 currentDamage（随已损生命提升） | `src/main/java/alku/csrp/entity/LongarmsEntity.java:214` | unmatched | currentDamage |
| creature | `pri_longarms` | behaviors | 冲击波命中时把目标上抬 0.64645（EntityWaveShock.func_70652_k） | `src/main/java/alku/csrp/entity/LongarmsEntity.java:233` | unmatched | EntityWaveShock、func_70652_k |
| creature | `pri_longarms` | behaviors | 冲击波波体：宽 3.1 高 0.2、速度 0.6、持续 duration=12 秒、沿途破坏硬度<=caster.getBlockH() 的方块 | `src/main/java/alku/csrp/entity/ShockwaveEntity.java:27` | unmatched | duration、caster、getBlockH |
| creature | `pri_longarms` | behaviors | killcount > SRPConfig.adaptedKills(30) 时进化：func_70074_a / onLivingUpdate 中 spawnNext(EntityShycoAdapted, thunder=true) | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:712` | unmatched | killcount、SRPConfig、adaptedKills、func_70074_a、onLivingUpdate、spawnNext |
| creature | `pri_longarms` | behaviors | func_70074_a 击杀回调：killcount++、粒子 particleStatus(5)、进化分值写入 | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:705` | unmatched | func_70074_a、killcount、particleStatus |
| creature | `pri_longarms` | behaviors | 被弹射物命中（source == arrow）触发 skillBreakBlocks | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:164` | unmatched | source、arrow、skillBreakBlocks |
| creature | `pri_longarms` | damage_and_effects | 火焰伤害乘数 SRPConfig.firemultyplier（默认 4.0） | `src/main/java/alku/csrp/entity/LongarmsEntity.java:287` | unmatched | SRPConfig、firemultyplier |
| creature | `pri_longarms` | damage_and_effects | 单次伤害上限 damageCap（primitiveCap=6）：伤害 >= maxHealth/6 时削减并触发 RAGE 与血块表现 | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:418` | unmatched | damageCap、primitiveCap、maxHealth、RAGE |
| creature | `pri_longarms` | damage_and_effects | 伤害适应：每点 0.05 减免、上限 12 点、最多 5 种来源、学习概率 0.7、新类型冷却 20 tick、火焰抑制 0.7 | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:79` | unmatched | tick |
| creature | `pri_longarms` | damage_and_effects | 最低伤害 primitiveMinDamage = 2.0（每次命中额外扣血） | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:501` | unmatched | primitiveMinDamage |
| creature | `pri_longarms` | damage_and_effects | KILLPRI_E 击杀抗性降低受到的伤害（最高 95%） | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:387` | unmatched | KILLPRI_E |
| creature | `pri_longarms` | damage_and_effects | adaptations 新来源冷却期间不重复学习（newDamageCooldown） | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:349` | unmatched | adaptations、newDamageCooldown |
| creature | `pri_longarms` | sync_data | SPECIAL(byte) 寄生虫状态（0/1/2/10）同步驱动动画层 | `src/main/java/alku/csrp/entity/LongarmsEntity.java:53` | unmatched | SPECIAL、byte |
| creature | `pri_longarms` | sync_data | HIT(byte) 适应命中状态同步（getHitStatus） | `src/main/java/alku/csrp/entity/PrimitiveParasiteEntity.java:71` | unmatched | byte、getHitStatus |
| creature | `pri_longarms` | animation | 攻击姿态动画（ModelShyco 中 getAttackTimer() 驱动的挥击分支） | `src/main/java/alku/csrp/entity/LongarmsEntity.java:58` | unmatched | ModelShyco、getAttackTimer |
| creature | `pri_longarms` | animation | 静止待机动画（getStillAni()==true 分支，stillTicks>25 判定） | `src/main/java/alku/csrp/entity/LongarmsEntity.java:259` | unmatched | getStillAni、stillTicks |
| creature | `pri_longarms` | animation | status 10（冲击波/技能）姿态 | `src/main/java/alku/csrp/entity/LongarmsEntity.java:322` | unmatched | status |
| creature | `pri_longarms` | animation | 动画控制器过渡时间（原版每帧即时计算，无过渡） | `src/main/java/alku/csrp/entity/LongarmsEntity.java:307` | unmatched | — |
| creature | `pri_longarms` | model_texture | 模型 ModelShyco（client/model/entity/primitive/ModelShyco.java） | `src/main/resources/assets/csrp/geo/pri_longarms.geo.json:1` | unmatched | ModelShyco、client、model、entity、primitive、java |
| creature | `pri_longarms` | model_texture | 变体贴图 shycov/shycob/shycoh（skin 5/6/7） | `src/main/java/alku/csrp/client/model/PrimitiveParasiteModel.java:117` | unmatched | shycov、shycob、shycoh、skin |
| creature | `pri_longarms` | model_texture | 阴影半径 0.7F | `src/main/java/alku/csrp/client/ClientModEvents.java:100` | unmatched | — |
| creature | `pri_longarms` | model_texture | RenderMalleable 基类附加渲染（适应/受击染色等） | `src/main/java/alku/csrp/client/renderer/ParasiteGeoRenderer.java:22` | unmatched | RenderMalleable |
| creature | `pri_longarms` | sounds | getAmbientSound = getParasiteStatus() != 0 ? MOBSILENCE : SHYCO_GROWL | `src/main/java/alku/csrp/entity/ParasiteSoundProfiles.java:48` | unmatched | getAmbientSound、getParasiteStatus、MOBSILENCE、SHYCO_GROWL |
| creature | `pri_longarms` | sounds | getHurtSound = (随机 && getHitStatus() > 0) ? MOBSILENCE : SHYCO_HURT | `src/main/java/alku/csrp/entity/ParasiteSoundProfiles.java:48` | unmatched | getHurtSound、getHitStatus、MOBSILENCE、SHYCO_HURT |
| creature | `pri_longarms` | sounds | getDeathSound = SRPSounds.SHYCO_DEATH | `src/main/java/alku/csrp/entity/ParasiteSoundProfiles.java:48` | unmatched | getDeathSound、SRPSounds、SHYCO_DEATH |
| creature | `pri_longarms` | sounds | AOE 攻击播放 SRPSounds.SWIPE（音量 2.0） | `src/main/java/alku/csrp/entity/LongarmsEntity.java:184` | unmatched | SRPSounds、SWIPE |
| creature | `pri_longarms` | sounds | 冲击波蓄力播放 getHurtSound(generic) 音量 4.0、音高 1.6..2.4 | `src/main/java/alku/csrp/entity/LongarmsEntity.java:394` | unmatched | getHurtSound、generic |
| creature | `pri_longarms` | spawning | 所有生物群系 MONSTER 表：weight shycoSpawnRate=15，group 1-1 | `src/main/java/alku/csrp/world/NaturalSpawnTables.java:35` | unmatched | MONSTER、weight、shycoSpawnRate、group |
| creature | `pri_longarms` | spawning | 阶段生成表条目（phase -1 起引入 primitive 群） | `src/main/java/alku/csrp/world/NaturalSpawnTables.java:35` | unmatched | phase、primitive |
| creature | `pri_longarms` | spawning | 光照与生成条件：isValidLightLevelOne/Two + 难度非 PEACEFUL + spawnDays 天数门控 | `src/main/java/alku/csrp/registry/CommonModEvents.java:373` | unmatched | isValidLightLevelOne、PEACEFUL、spawnDays |
| creature | `pri_longarms` | loot | 未覆写 dropFewItems/entityDropItem → 无物品掉落 | `src/main/resources/data/csrp/loot_table/entities/pri_longarms.json:1` | unmatched | dropFewItems、entityDropItem |
| creature | `pri_longarms` | loot | 经验值 = SRPAttributes.XP_PRIMITIVE（SRPConfig.primitiveXPValue 默认 30） | `src/main/java/alku/csrp/entity/LongarmsEntity.java:80` | unmatched | SRPAttributes、XP_PRIMITIVE、SRPConfig、primitiveXPValue |
| creature | `sim_bigspider` | registration | 注册用数值 id 2（getParasiteIDRegister）驱动掉落/生成派发 | `src/main/resources/data/csrp/loot_table/entities/sim_bigspider.json:1` | unmatched | getParasiteIDRegister |
| creature | `sim_bigspider` | registration | 碰撞箱 func_70105_a(1.9F, 2.1F) | `src/main/java/alku/csrp/registry/ModEntities.java:281` | unmatched | func_70105_a |
| creature | `sim_bigspider` | registration | tracker(64, 3, true)：追踪距离 64 格 / 更新间隔 3 tick | `src/main/java/alku/csrp/registry/ModEntities.java:630` | unmatched | tracker、tick |
| creature | `sim_bigspider` | attributes | 生命 SRPAttributes.DORPA_HEALTH = 22 | `src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java:505` | unmatched | SRPAttributes、DORPA_HEALTH |
| creature | `sim_bigspider` | attributes | 护甲 SRPAttributes.DORPA_ARMOR = 3 | `src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java:505` | unmatched | SRPAttributes、DORPA_ARMOR |
| creature | `sim_bigspider` | attributes | 攻击力 SRPAttributes.DORPA_ATTACK_DAMAGE = 9 | `src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java:505` | unmatched | SRPAttributes、DORPA_ATTACK_DAMAGE |
| creature | `sim_bigspider` | attributes | 移动速度 0.27 | `src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java:505` | unmatched | — |
| creature | `sim_bigspider` | attributes | 击退抗性 SRPAttributes.DORPA_KD_RESISTANCE = 0.5 | `src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java:505` | unmatched | SRPAttributes、DORPA_KD_RESISTANCE |
| creature | `sim_bigspider` | attributes | 跟随范围 SRPConfig.infectedFollow = 16 | `src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java:505` | unmatched | SRPConfig、infectedFollow |
| creature | `sim_bigspider` | attributes | 经验 field_70728_aV = SRPAttributes.XP_INFECTED = 8 | `src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java:94` | unmatched | field_70728_aV、SRPAttributes、XP_INFECTED |
| creature | `sim_bigspider` | ai | 继承 EntityMob：tasks.addTask(0, EntityAISwimming) | `src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java:131` | unmatched | EntityMob、EntityAISwimming |

（另有 318 条未列出）
