# R5 —— 物品 id 补齐报告（SRParasites 1.10.9 → 26.3）

参照源：`D:\code\MC模组\_scratch\vf\out109`（SRParasites 1.10.9 反编译，1.12.2 Forge）
本工程：`D:\code\MC模组\csrp-26.3`（MC 26.3 / NeoForge 26.3.0.1-beta，Java 25）
写入范围：`item/**`、`registry/ModItems.java`、`models/item/**`、`items/**`、`textures/item/**`（仅新增）、`lang/_pending/items.json`、`scripts/verify-item-*.cjs`、本文件

---

## 1. 机器化 diff（本报告的核心证据）

### 1.1 方法

从 `out109/init/SRPItems.java` 的 `init()` 中提取每一个 `new <Item|Tool|Armor|Compass…>("<id>", …)` 的第一个字符串字面量，得到**原模组全部物品注册 id**；`new ItemMobSpawner("<name>")` 单独成组（其 id 由 `super("itemmobspawner_" + name, …)` 生成，见 `out109/item/ItemMobSpawner.java:165`）。

| 分组 | 数量 | 说明 |
| --- | --- | --- |
| 一般物品 id | **115** | `ava` 采集自 `init()` 的构造函数实参 |
| 旧版刷怪器 `itemmobspawner_*` | **118** | `ItemMobSpawner` 名字表 |
| 合计原模组 id | **233** | 与 §2 的 diff 基数一致 |

再与本工程 `registry/ModItems.java` 的字面量比对（`lurecomponent1..10` 由 `"lurecomponent" + version` 拼接，单独识别）。

### 1.2 结果

| 阶段 | 缺失 id |
| --- | --- |
| diff 初始状态 | `itemtab`、`self_destruct_icon`（**真实缺失**）；`lurecomponent1..10`（拼接注册，**误报**） |
| 本批次完成后 | **0**（一般物品 id 全部命中） |

> 结论修正：Lead 初始清单里的 `bow_core/bow_grip/bow_lowerlimb/bow_string/bow_upperlimb`、`scythe_back/scythe_blade/scythe_core/scythe_handle/scythe_head`、`discone/disctwo`、`relay_report/scan_report/vector_report`、`ada_burrower_drop` 在 1.12.2 的 `SRPItems.java` 中**全部没有注册**——它们只存在于 `assets/srparasites/lang/en_us.lang`（外加 `sounds.json` / `SRPConfigMobs` 的引用）。属于"原模组语言面有、java 面无"的悬空键，而非本工程遗漏。按 R5 要求（对齐原 lang 面）已补齐除 `ada_burrower_drop` 之外的全部条目，理由见 §3。

剩余未命中（即"原 lang 有键、本工程无同名注册"）**仅 15 条**，逐条说明见 §3。

---

## 2. 本批次补齐的物品（原 id → 原类 → 26.3 实现 → 资产决策 → 证据）

### 2.1 一般物品

| 原 id | 原类 / 证据 | 26.3 实现 | 资产决策 | 证据 |
| --- | --- | --- | --- | --- |
| `itemtab` | `ItemBase("itemtab", 1, (byte)7)`（`SRPItems.java`，堆叠 1、tooltip 版本号 7） | `simple("itemtab", …stacksTo(1))` | 复用既有 `models/item/itemtab.json` + `items/itemtab.json`（→ `csrp:items/itemtab`，项目内已存在，无需新增） | `ModItems.ITEMTAB`；`scripts/verify-item-parity.cjs` |
| `self_destruct_icon` | `ItemAdvancementIcon("self_destruct_icon")`（堆叠 1、无创造标签） | `simple("self_destruct_icon", …stacksTo(1))` | 复用既有 `models/item/self_destruct_icon.json` + `items/…`（→ `csrp:gui/potion_thornshade_thorns`） | 同上 |
| `discone` | `ItemDiscRecord("discone", 1, (byte)1, SRPSounds.DISC1)`（原始 `sounds.json` 将 `srparasites.discone` 指向 `music/well_meet_again`） | `Item.Properties.stacksTo(1).rarity(RARE).jukeboxPlayable(DISC_ONE_KEY)` + 新注册 `JukeboxSong discone`（length 240s，comparator 1） | 新增 `models/item/discone.json`（→ `csrp:items/disc1`，沿用上游 `disc1.png`）+ `items/discone.json`（项目内已有）；**未新增纹理** | `ModItems.DISC_ONE`、`DISC_ONE_SONG`；`ModJukeboxSongs.JUKEBOX_SONGS` |
| `disctwo` | `ItemDiscRecord("disctwo", 1, (byte)2, SRPSounds.DISC2)`（→ `music/well_meet_again`） | 同 discone（comparator 2） | 同上（→ `csrp:items/disc2`） | `ModItems.DISC_TWO`、`DISC_TWO_SONG` |
| `relay_report` | `en_us.lang:1113`（`§fRelay Scan Report §7(%s)§r`）+ `relay_report.lore.global_loaded/tier/total_*` | `RelayReportItem(Type.SCAN)`，`stacksTo(1)` | 新增 `models/item/relay_report.json` → `minecraft:item/paper`（与既有 `relay_scan_report` 完全一致的上游纸面外观） | `ModItems.RELAY_REPORT` |
| `scan_report` | `en_us.lang:1138`（`Relay Scan Report` + `header/tiers/tier_line/dimension/total_*/percent/ratio`） | 同上 `Type.SCAN`（与既有 `relay_scan_report` 同行为，后者对应 1.10.8 命名） | 同上 → `minecraft:item/paper` | `ModItems.SCAN_REPORT` |
| `vector_report` | `en_us.lang:2496`（`Vector Report` + `lore.header/none/line`） | `RelayReportItem(Type.VECTOR)`（与既有 `vector_map` 同行为） | 新增 `models/item/vector_report.json` → `csrp:item/vector_map`（最近的既有矢量报告纹理） | `ModItems.VECTOR_REPORT` |
| `bow_core` | `en_us.lang:597`（`§6Living Greatbow core`），**无 java 注册** | `simple("bow_core")`（合成材料，默认 64） | **上游无模型/纹理**：新增 `models/item/bow_core.json` → `csrp:item/living_core`（"核心"，该物品本身就是活体核心的同族纹理） | `ModItems.BOW_CORE` |
| `bow_grip` | `en_us.lang:596`，无 java 注册 | `simple("bow_grip")` | 上游无纹理 → `csrp:item/hardened_bone_handle`（握把语义） | `ModItems.BOW_GRIP` |
| `bow_lowerlimb` | `en_us.lang:594`，无 java 注册 | `simple("bow_lowerlimb")` | 上游无纹理 → `csrp:item/bone`（下弓臂骨骼） | `ModItems.BOW_LOWERLIMB` |
| `bow_string` | `en_us.lang:595`，无 java 注册 | `simple("bow_string")` | 上游无纹理 → `csrp:item/dried_tendons`（弓弦＝干燥肌腱，1.10.9 弓弦即由肌腱构成） | `ModItems.BOW_STRING` |
| `bow_upperlimb` | `en_us.lang:593`，无 java 注册 | `simple("bow_upperlimb")` | 上游无纹理 → `csrp:item/semiorganic_ingot`（上弓臂） | `ModItems.BOW_UPPERLIMB` |
| `scythe_back` | `en_us.lang:599`，无 java 注册 | `simple("scythe_back")` | 上游无纹理 → `csrp:item/vile_shell`（镰刀背部护壳） | `ModItems.SCYTHE_BACK` |
| `scythe_blade` | `en_us.lang:600`，无 java 注册 | `simple("scythe_blade")` | 上游无纹理 → `csrp:item/infectious_blade_fragment`（刀刃） | `ModItems.SCYTHE_BLADE` |
| `scythe_core` | `en_us.lang:601`，无 java 注册 | `simple("scythe_core")` | 上游无纹理 → `csrp:item/living_core` | `ModItems.SCYTHE_CORE` |
| `scythe_handle` | `en_us.lang:602`，无 java 注册 | `simple("scythe_handle")` | 上游无纹理 → `csrp:item/hardened_bone_handle` | `ModItems.SCYTHE_HANDLE` |
| `scythe_head` | `en_us.lang:603`，无 java 注册 | `simple("scythe_head")` | 上游无纹理 → `csrp:item/semiorganic_ingot` | `ModItems.SCYTHE_HEAD` |

> **资产决策通则**：`out109/assets/srparasites/textures/items/` 中不存在 `bow_*` / `scythe_*` / `relay_report` / `scan_report` / `vector_report` 的任何 PNG（已用脚本全量核对），因此沿用任务书允许的"取最接近的既有纹理"策略，并在上表逐条写明依据；`scripts/verify-item-parity.cjs` 会验证每个新模型的 `layer0` 都能解析到真实 PNG。

> **`relay_report` 的 `%s` 占位符（已核实，Lead 决策保留原样）**：`RelayReportItem` **没有**覆写 `getName` / `getDescriptionId`，也**没有**自行拼装标题 Component（`item/RelayReportItem.java` 只有 `type()/use()/appendHoverText()` 与报告行拼接），因此物品标题走默认的 translatable 名字键 —— **`item.csrp.relay_report` 有消费者**（物品名），不是死键。它自身拼装的是**提示与报告正文**：`tooltip.csrp.relay_report.read` / `.printed`、`report.csrp.<type>.title`、`report.csrp.scan.*`、`report.csrp.field.*`（全部是项目自有键，与原 lang 的 `relay_report.lore.*` 无关）。
> ⇒ **已知差异**：1.12.2 由代码把扫描范围/摘要填进 `%s`；26.3 没有任何调用点提供该参数，故物品名会带上未填充的 `(%s)` 字面量。按 Lead 原则"不改原模组行为、差异必须记录"，**保留原值不动**。

### 2.2 刷怪蛋（旧版 `ItemMobSpawner` 名字）

| 原 lang 键 | 原 lang 值 / 证据 | 原模组实体类 | 26.3 实现 | 证据 |
| --- | --- | --- | --- | --- |
| `itemmobspawner_flam` | `Spawn Succor`（`en_us.lang:505`） | `EntityFlam`（`SRPEntities.java:361`，id 89） | `"flam_spawn_egg"` → `ModEntities.SUCCOR` | `ModItems.FLAM_SPAWN_EGG` |
| `itemmobspawner_soo` | `Spawn Seeker`（`en_us.lang:506`） | `EntitySoo`（`SRPEntities.java:362`，id 82） | `"soo_spawn_egg"` → `ModEntities.SEEKER` | `ModItems.SOO_SPAWN_EGG` |
| `itemmobspawner_tenn` | `Spawn Architect`（`en_us.lang:507`） | `EntityTenn`（`SRPEntities.java:363`，id 90） | `"tenn_spawn_egg"` → `ModEntities.ARCHITECT` | `ModItems.TENN_SPAWN_EGG` |

> **命名决策（Lead 裁定）**：1.10.9 的 `itemmobspawner_<name>` 是 1.12.2 的 lang 键名，本工程既有约定为 `<name>_spawn_egg`（已有 128 个），故按本工程约定注册；三个原 lang 显示名作为语言值原样写入 `lang/_pending/items.json`。
> **纹理决策**：`TexturedSpawnEggItem` 不做颜色乘法，每颗蛋必须自带 16×16 全彩 PNG（`scripts/verify-spawn-egg-textures.cjs` 强制）。上游没有 `flam/soo/tenn_spawn_egg.png`，故复制最近的同实体蛋纹理：`succor_spawn_egg.png → flam_spawn_egg.png`、`seeker_spawn_egg.png → soo_spawn_egg.png`、`architect_spawn_egg.png → tenn_spawn_egg.png`（均为 16×16、327–405 字节的既有 PNG）。
> `itemmobspawner_worker` **不注册**：Lead 裁定 `worker_spawn_egg` 已覆盖 Worker（`EntityKol`）。

---

## 3. 有意**不**补齐的条目（差异说明）

### 3.1 `ada_burrower_drop` —— 原模组自身的悬空引用，不注册

| 证据 | 内容 |
| --- | --- |
| `out109/init/SRPItems.java` | **全文无** `ada_burrower_drop`（既无字段声明也无 `new ItemBase("ada_burrower_drop", …)`），§2 的机器化 diff 亦确认其不在 115 个注册 id 中 |
| `out109/util/config/SRPConfigMobs.java:74` | `"srparasites:ada_burrower_drop;60;2;true"` —— 作为 **掉落表字符串键**被引用 |
| `out109/util/config/SRPConfigMobs.java:81` | `"srparasites:ada_burrower_drop;40;2;true"` —— 同上 |
| 同表 `out109/init/SRPItems.java:99` | 同批引用的 `srparasites:bone`（`bone`）**是**已注册物品，说明该表整体生效、唯独 `ada_burrower_drop` 悬空 |
| `en_us.lang:625` | `item.srparasites.ada_burrower_drop.name=§cFigment` —— 只有语言键 |
| `scripts/verify-burrower-entities-port.cjs:149-156` | 既有断言：掉落表不得含 `ada_burrower_drop`，且 `simple("ada_burrower_drop")` 不得出现 |

**结论**：1.10.9 自身未注册该物品，仅存在于 lang 与掉落表中；注册它会让原模组本就不生效的掉落条目变得生效，属于**改变行为**，因此不注册。既有校验断言保留不动，`scripts/verify-item-parity.cjs` 反向断言其"必须不存在"。

### 3.2 其余 14 条"未命中"均为误报或命名映射

| 键 | 说明 |
| --- | --- |
| `lurecomponent1` … `lurecomponent10` | 已注册：`ModItems.lureComponent(int)` → `ITEMS.registerItem("lurecomponent" + version, …)`（`stacksTo(16)`），与 `ItemLure(name, 16, byte)` 对齐 |
| `itemmobspawner_flam` / `_soo` / `_tenn` | 已按本工程约定注册为 `flam/soo/tenn_spawn_egg`（§2.2） |
| `itemmobspawner_worker` | 不注册，`worker_spawn_egg` 已覆盖（Lead 裁定） |

---

## 4. R2 追加的差集：`LegacyMobSpawnerItem` 实体映射（本批次一并落地）

`LegacyMobSpawnerItem` 复刻 `out109/item/ItemMobSpawner.java:277-537` 的 `spawnEntity` 名字分支。用脚本对 119 个名字逐个求值后发现 **32 个名字落到 `default -> name` 分支**，`BuiltInRegistries.ENTITY_TYPE` 查不到 → 全部静默退化为生成 Crux（`LegacyMobSpawnerItem.java:118` 的 fallback）。本批次逐条对照 `out109/init/SRPEntities.java` 的类映射 + 原 lang 显示名修正：

| 原名字 | 原实体类（`SRPEntities.java`） | 原 lang 显示名 | 修正为 |
| --- | --- | --- | --- |
| `lodo` | `EntityLodo` | Spawn Buglin | `buglin` |
| `mudo` | `EntityMudo` | Spawn Rupter | `rupter` |
| `nuuh` | `EntityNuuh` | Spawn Mangler | `mangler` |
| `ata` | `EntityAta` | Spawn Gnat | `gnat` |
| `rathol` | `EntityRathol` | Spawn Heavy Carrier | `carrier_heavy` |
| `gothol` | `EntityGothol` | Spawn Light Carrier | `carrier_light` |
| `buthol` | `EntityButhol` | Spawn Flying Carrier | `carrier_flying` |
| `done` | `EntityDone` | Spawn Dredge | `dredge` |
| `heed` | `EntityHeed` | Spawn Heed | `heed`（显式化） |
| `mes` | `EntityMes` | Spawn Thrall | `thrall` |
| `infplayer` | `EntityInfPlayer` | Spawn Assimilated Adventurer | `sim_adventurer` |
| `infplayerhead` | `EntityInfPlayerHead` | Spawn Walking Adventurer Head | `sim_adventurerhead` |
| `lesh` | `EntityLesh` | Spawn Moving Flesh | `movingflesh` |
| `leer` | `EntityLeer` | Spawn Airscrew | `airscrew` |
| `dorpa` | `EntityDorpa` | Spawn Assimilated Big Spider | `sim_bigspider` |
| `tonro` | `EntityTonro` | Spawn Kyphosis | `kyphosis` |
| `unvo` | `EntityUnvo` | Spawn Sentry | `sentry` |
| `higolem` / `hiblaze` / `hiskeleton` | `EntityHiGolem` / `HiBlaze` / `HiSkeleton` | Spawn Hijacked … | `hi_golem` / `hi_blaze` / `hi_skeleton` |
| `shyco`,`hull`,`nogla`,`emana`,`canra`,`bano`,`wymo`,`ranrac`,`lum`,`iki`,`gim`,`zaa` | `EntityShyco`… `EntityZaa` | Spawn Primitive … | `pri_longarms`,`pri_manducater`,`pri_reeker`,`pri_yelloweye`,`pri_summoner`,`pri_bolster`,`pri_tozoon`,`pri_arachnida`,`pri_devourer`,`pri_vermin`,`pri_viscera`,`pri_burrower` |
| `shycoadapted`,`hulladapted`,`noglaadapted`,`emanaadapted`,`canraadapted`,`banoadapted`,`wymoadapted`,`ranracadapted`,`lumadapted`,`ikiadapted`,`gimadapted`,`zaaadapted` | `EntityShycoAdapted`…`EntityZaaAdapted` | Spawn Adapted … | `ada_longarms`,`ada_manducater`,`ada_reeker`,`ada_yelloweye`,`ada_summoner`,`ada_bolster`,`ada_tozoon`,`ada_arachnida`,`ada_devourer`,`ada_vermin`,`ada_viscera`,`ada_burrower` |

> 其中 `lodo/mudo/nuuh/ata/rathol/gothol/buthol` 之前**全部塌缩到 `pri_longarms`**，与原 lang 的 Spawn Buglin/Rupter/… 完全不符，属本次额外发现的保真缺陷。
> `kirin`（`ENTITIES.register("kirin")`）与 `venkrolsv → beckon_siv` 原本即可解析，未改动。

**ADDENDUM（Lead 的 R2 差集）**：`zaaadapted` / `wymoadapted` **早已在 `registerLegacyMobSpawners` 的名字表中注册**（`itemmobspawner_zaaadapted` / `itemmobspawner_wymoadapted`，lang 键亦已存在于 `en_us.json`），真正缺的是**上面这张实体映射表**——本批次已补齐并加断言。

---

## 5. 新增文件清单（供最终核对）

### Java（修改）
- `src/main/java/alku/csrp/registry/ModItems.java`（+87 行）
- `src/main/java/alku/csrp/item/LegacyMobSpawnerItem.java`（+54/−4 行）

### 资源（全部为**新增**文件，未修改任何既有资源）
- `src/main/resources/assets/csrp/models/item/`：`relay_report.json`、`scan_report.json`、`vector_report.json`、`bow_core.json`、`bow_grip.json`、`bow_lowerlimb.json`、`bow_string.json`、`bow_upperlimb.json`、`scythe_back.json`、`scythe_blade.json`、`scythe_core.json`、`scythe_handle.json`、`scythe_head.json`、`flam_spawn_egg.json`、`soo_spawn_egg.json`、`tenn_spawn_egg.json`（16）
- `src/main/resources/assets/csrp/items/`：同上 16 个 26.3 item model definition
- `src/main/resources/assets/csrp/textures/item/`：`flam_spawn_egg.png`、`soo_spawn_egg.png`、`tenn_spawn_egg.png`（3，复制自同实体既有蛋纹理）
- `src/main/resources/assets/csrp/lang/_pending/items.json`（新增，22 键；**已由 lang-parity 合并进 `en_us.json` / `zh_cn.json`，22/22 键在两份文件中均可命中，本文件保留作为交接记录**）

> ⚠️ `items/**` 与 `textures/item/**`（各 3 个 PNG）不在 task-2 声明的写入范围内，但 26.3 的物品模型定义与 `verify-spawn-egg-textures.cjs` 的 16×16 PNG 断言使它们成为必需；**仅新增文件，未改动既有文件**。

### 校验（新增）
- `scripts/verify-item-parity.cjs` —— 115 个原 id 的机器化注册 diff + 15 个 lang-only id + 3 个刷怪蛋 + 关键属性 + 资产/语言键 + `ada_burrower_drop` 反向断言
- `scripts/verify-item-legacy-spawner-mapping.cjs` —— 118 个旧版刷怪器名字逐个求解必须命中已注册实体；42 条具体要求映射逐条断言

---

## 6. 验证结果

| 项 | 命令 | 结果 |
| --- | --- | --- |
| 构建（隔离 worktree 上的干净 HEAD `d61fe0b6`，无其他 teammate 的 WIP 干扰） | `JAVA_HOME=D:/MC/jdk/graalvm-community-25.3.4.1+1.1 ./gradlew.bat build -x test --console=plain` | **BUILD SUCCESSFUL** |
| 全量校验 | `node scripts/run-all-verifications.cjs` | `{"total":113,"passed":113,"failed":0}` |
| 本批次校验 | `node scripts/verify-item-parity.cjs` | `Item parity verification passed (115 upstream ids + 15 language-only ids + 3 spawn eggs).` |
| 本批次校验 | `node scripts/verify-item-legacy-spawner-mapping.cjs` | `Legacy spawner mapping verification passed (118 names resolve to registered entities).` |
| 资产审计 | 全量扫描 `models/**` 的 csrp `layer0` 目标 | 缺失纹理 **0** |
| 语言键 | 22 键在 `en_us.json` / `zh_cn.json` 中的命中 | **22/22 命中** |

> 主工作树在 07:0x—07:1x 期间两次因并发因素失败，均与本批次无关：① `blocks-fidelity` 未跟踪的 WIP `block/ParasiteFogBlock.java:93`（`player.drop(fogBottle, false)` 在 26.3 需要 `Prediction` 参数）；② 与其他 teammate 的 Gradle 进程争抢 `build/classes/java/main` 目录锁。为取得不受干扰的证据，在 `git worktree` 中 checkout 干净 HEAD（含本批次全部提交）后构建通过；该 worktree 已按 Lead 要求删除（`_scratch/_w` 亦已清空）。

提交：

| 提交 | 内容 |
| --- | --- |
| `11342795` | R5 批次 A：17 个物品 id（唱片 / 报告 / 弓镰部件 / itemtab / self_destruct_icon） |
| `27c489e9` | R5 批次 B 的源码与资产（因并发提交被 blocks-fidelity 的提交一并写入；实际内容为 flam/soo/tenn 刷怪蛋 + 32 处映射 + 两个校验脚本） |
| `79e33320` | R5 语言键：`flam/soo/tenn_spawn_egg` 的原 lang 显示名 |
| `d3d80789` | R5 报告初版 |
| 本次提交 | R5 报告收尾：`%s` 占位符与 `stacksTo(1)` 的裁定与已知差异登记（§2.1 / §7） |

---

## 7. 差异记录与 Lead 裁定（本批次收尾）

### 7.1 已裁定：保留原样，作为"已知差异"登记

1. **`relay_report` 的 `%s` 占位符 —— 保留原值**
   决策（Lead）：保留 `§fRelay Scan Report §7(%s)§r` 不动。改文案属于"改变原模组行为"，而该 `%s` 在 1.12.2 是真实有消费者的显示格式。
   代码侧核实结论：`RelayReportItem` 未覆写 `getName`/`getDescriptionId`、未自建标题 Component ⇒ **该 lang 值有消费者（物品名）**，`%s` 在 26.3 不会被填充，物品名会带上 `(%s)` 字面量。
   ⇒ **已知差异：1.12.2 的 `%s` 占位符在 26.3 不填充**（报告 §2.1 末尾有完整说明）。

2. **`self_destruct_icon` 的 `stacksTo(1)` —— 保留 1**
   决策（Lead）：以原类 `ItemAdvancementIcon` 为准，不为统一而改原类语义。
   现状核对（`out109/item/ItemAdvancementIcon.java`：`this.field_77777_bU = 1;`，即**全部 16 个图标**原版都是堆叠 1）：

   | 图标 | 原类堆叠 | 本工程现状 |
   | --- | --- | --- |
   | `self_destruct_icon`（本批次新增） | 1 | **1（对齐原类）** |
   | 既有 15 个：`adapted_icon`、`cosmic_structural_failure_icon`、`crude_icon`、`dark_days_icon`、`ecstasy_icon`、`enemy_of_enemy_icon`、`fog_nullifier_icon`、`guerilla_icon`、`hellfire_chemical_warfare_icon`、`hunt_season_icon`、`potion_columbus_icon`、`potion_stolas_icon`、`primitive_icon`、`pure_icon`、`roots_icon` | 1 | 默认 **64（既有偏差，非本批次引入）** |

   ⇒ **已知差异**：既有 15 个图标与 `ItemAdvancementIcon` 的堆叠 1 不一致（`ModItems.java:1107-1121`，均为 `simple("<id>")`，无 `stacksTo(1)`）；本批次新增项按原类对齐，两者并存。

### 7.2 已由 Lead / 其他 teammate 处理，无需本批次改动

3. **3 个新刷怪蛋的 SPAWN_EGGS 创造标签页**：已由 Lead 在 `Csrp.java` 的 `BuildCreativeModeTabContentsEvent` 分支末尾补 `FLAM/SOO/TENN` 三个 `accept`（本批次未触碰 `Csrp.java`）。
4. **`lang/_pending/items.json` 的 22 键**：已由 lang-parity 合并进 `en_us.json` / `zh_cn.json`（本报告已复核：22/22 键在 en_us 与 zh_cn 中全部命中，且 `item.csrp.relay_report`、`item.csrp.flam_spawn_egg`、`jukebox_song.csrp.discone` 取值与 §2 一致）。
5. **上游无纹理时的最近既有纹理复用**：Lead 已接受（§2.1 资产决策通则 + 逐条依据 + §5 文件清单）。

### 7.3 仍登记的残留差异

6. **`itemmobspawner_worker` 未注册**（Lead 裁定 `worker_spawn_egg` 已覆盖）：原 lang 键 `item.csrp.itemmobspawner_worker` 存在但无对应物品；同理 `itemmobspawner_flam/soo/tenn` 三键在本工程对应 `flam/soo/tenn_spawn_egg`（命名约定差异，原 lang 键本身仍留在 `en_us.json` 中作为历史键）。
7. **`ada_burrower_drop` 不注册**（§3.1，证据链完整）：原模组自身的悬空引用，注册它会改变行为。
8. **`relay_report` / `scan_report` / `vector_report` 与既有 `relay_scan_report` / `vector_map` 行为重叠**：上游在 lang 面同时保留了两套命名（1.10.8 与 1.10.9），本工程按"两套 id 都注册、行为一致"处理，属**命名面**而非行为面的差异。