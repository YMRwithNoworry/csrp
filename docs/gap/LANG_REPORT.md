# 语言文件迁移报告：SRParasites 1.10.9 → csrp / Minecraft 26.3

本报告由 `scripts/convert-lang-109.cjs` 生成。所有数字都是原始 `.lang` 与源码的函数，不含时间戳、不含「本次运行新增了多少」这类过程量，因此重复执行本脚本会得到逐字节相同的文件。

## 1. 事实来源与规模

- 原始事实来源（只读）：`D:/code/MC模组/_scratch/vf/out109/assets/srparasites/lang/*.lang`
- 原始 `.lang` 套数：**33**。任务书写的「37 套」= 这 33 个 `.lang` 再加上参考镜像 `_scratch/ref1201/src/main/resources/assets/csrp/lang` 里另外 4 个 `.json`（`en_us` / `zh_cn` / `hr_hr` / `ko_kr`）；`out109` 目录下实际只有 33 个 `.lang`。
- `en_us.lang` 原始键：**2332**（唯一 2332，无重复定义、无格式错误行）。
- 转换目标：`src/main/resources/assets/csrp/lang/<locale>.json`。26.3 只加载 JSON，`.lang` 自 1.13 起已完全失效。
- 迁移后语言文件数：**33** 套 JSON（每套语言一个文件）。
- 任务开始时本工程 `en_us.json` 为 1456 键、`zh_cn.json` 为 1470 键（任务书写明的基线；该数字只作为记录，不参与幂等计算）。迁移后见第 3 节。

## 2. 键映射规则

| 规则 | 说明 |
| --- | --- |
| `tile.name` | tile.<ns>.<id>.name → block.csrp.<id> |
| `tile.legacy-no-namespace` | tile.<id>.name (无命名空间) → block.csrp.<id> |
| `item.name` | item.<ns>.<id>.name → item.csrp.<id> |
| `item.other` | item.<ns>.<id>.<suffix> → item.csrp.<id>.<suffix> |
| `entity.name` | entity.<ns>.<id>.name → entity.csrp.<id> |
| `entity.other` | entity.<ns>.<id>.<suffix> → entity.csrp.<id>.<suffix> |
| `mob_effect.colon` | mob_effect.<ns>:<id> → effect.csrp.<id>（冒号写法） |
| `effect.dot` | effect.<ns>.<id> → effect.csrp.<id> |
| `advancements` | advancements.<ns>.<id>.<suffix> → advancements.csrp.<id>.<suffix> |
| `advancement.title` | advancement.<ns>.<id>.title → advancements.csrp.<id>.title |
| `advancement.desc` | advancement.<ns>.<id>.desc → advancements.csrp.<id>.description |
| `block.other` | block.<ns>.<id>.<suffix> → block.csrp.<id>.<suffix> |
| `potion.effect` | potion|splash_potion|lingering_potion|tipped_arrow.effect.<ns>:<id> → item.minecraft.<kind>.effect.<id> |
| `top-level-namespace` | <ns>.<id>… → csrp.<id>… |
| `namespace-segment-swap` | 其余任何 `srparasites` 命名空间段 → `csrp`（保留前导前缀） |
| `identity` | 键里不含命名空间：原样保留（1.12.2 里本来就没有命名空间） |

规则按上表顺序匹配，先命中者生效。

## 3. 转换结果（逐语言）

列含义：`原键数` = 该语言 `.lang` 的唯一键数；`映射后唯一键` = 按规则去重后的 26.3 键数（含冲突消解）；`转换后键数` = 最终 JSON 的键数；`非原模组键` = 转换后键数 − 映射后唯一键（本工程既有的 26.3 专用键，按「既有键优先」原则保留）；`兼容别名` = 为源码里遗留的旧命名空间额外补出的键。

| locale | 原键数 | 映射后唯一键 | 转换后键数 | 非原模组键 | 兼容别名 | 冲突 | 自身原键覆盖率 | 相对 en_us.lang 覆盖率 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `en_us` | 2332 | 2324 | 2978 | 654 | 6 | 8 | 100% | 100% |
| `de_at` | 311 | 311 | 317 | 6 | 6 | 0 | 100% | 12.78% |
| `de_ch` | 311 | 311 | 317 | 6 | 6 | 0 | 100% | 12.78% |
| `de_de` | 582 | 582 | 588 | 6 | 6 | 0 | 100% | 24.14% |
| `en_pt` | 252 | 252 | 252 | 0 | 0 | 0 | 100% | 10.41% |
| `en_ws` | 252 | 252 | 252 | 0 | 0 | 0 | 100% | 10.41% |
| `es_ar` | 402 | 402 | 408 | 6 | 6 | 0 | 100% | 16.74% |
| `es_cl` | 597 | 597 | 603 | 6 | 6 | 0 | 100% | 24.87% |
| `es_ec` | 402 | 402 | 408 | 6 | 6 | 0 | 100% | 16.74% |
| `es_es` | 402 | 402 | 408 | 6 | 6 | 0 | 100% | 16.74% |
| `es_mx` | 402 | 402 | 408 | 6 | 6 | 0 | 100% | 16.74% |
| `es_uy` | 402 | 402 | 408 | 6 | 6 | 0 | 100% | 16.74% |
| `es_ve` | 402 | 402 | 408 | 6 | 6 | 0 | 100% | 16.74% |
| `fr_ca` | 402 | 402 | 408 | 6 | 6 | 0 | 100% | 16.74% |
| `fr_fr` | 1844 | 1837 | 1843 | 6 | 6 | 7 | 100% | 78.31% |
| `hr_hr` | 2072 | 2065 | 2725 | 660 | 6 | 7 | 100% | 90.83% |
| `it_it` | 327 | 327 | 333 | 6 | 6 | 0 | 100% | 13.47% |
| `ja_jp` | 1965 | 1958 | 1964 | 6 | 6 | 7 | 100% | 83.61% |
| `ja_jp21` | 892 | 889 | 895 | 6 | 6 | 3 | 100% | 36.49% |
| `ko_kr` | 2133 | 2125 | 2761 | 636 | 6 | 8 | 100% | 90.23% |
| `lol_us` | 399 | 399 | 405 | 6 | 6 | 0 | 100% | 16.44% |
| `lv_lv`（原 `lv_LV`） | 252 | 252 | 252 | 0 | 0 | 0 | 100% | 10.41% |
| `nl_nl` | 252 | 252 | 252 | 0 | 0 | 0 | 100% | 10.41% |
| `pl_pl` | 582 | 582 | 588 | 6 | 6 | 0 | 100% | 24.01% |
| `pt_br` | 582 | 582 | 588 | 6 | 6 | 0 | 100% | 24.14% |
| `ro_ro` | 392 | 392 | 398 | 6 | 6 | 0 | 100% | 16.09% |
| `ru_ru` | 2106 | 2098 | 2104 | 6 | 6 | 8 | 100% | 89.5% |
| `sv_se` | 333 | 333 | 339 | 6 | 6 | 0 | 100% | 13.73% |
| `tr_tr` | 583 | 583 | 589 | 6 | 6 | 0 | 100% | 24.23% |
| `uk_ua` | 402 | 402 | 408 | 6 | 6 | 0 | 100% | 16.74% |
| `zh_cn` | 2140 | 2132 | 2994 | 862 | 6 | 8 | 100% | 100% |
| `zh_tw` | 893 | 890 | 896 | 6 | 6 | 3 | 100% | 36.53% |
| `zh_tw21` | 893 | 890 | 896 | 6 | 6 | 3 | 100% | 36.53% |

- `en_us.json`：2978 键，自身原键覆盖率 **100%**。
- `zh_cn.json`：2994 键，自身原键覆盖率 **100%**，相对 `en_us.lang` 覆盖率 **100%**。
- 规则命中统计（以 `en_us` 为准）：`identity`=719，`namespace-segment-swap`=461，`tile.name`=386，`item.name`=285，`potion.effect`=148，`entity.name`=143，`advancements`=74，`mob_effect.colon`=38，`top-level-namespace`=31，`item.other`=27，`tile.legacy-no-namespace`=6，`advancement.title`=4，`advancement.desc`=4，`effect.dot`=4，`entity.other`=1，`block.other`=1
- 原模组 lang 里有 78 个键的值**本来就是空的**（例如 `tootip.srparasites.item.7=`、`item.srparasites.itemthrow.name=`），按原样保留：26.3 里空值渲染为空字符串，与原模组 1.12.2 行为一致。`scripts/verify-lang-format.cjs` 会断言「JSON 里为空的键在原始 `.lang` 里也是空的」。

### 3.1 覆盖率说明

- `en_us` 与 `zh_cn` 对**原模组对应的那份 `.lang`** 都是 100% 覆盖：`en_us.lang` 的 2332 个键、`zh_cn.lang` 的 2140 个键全部有对应键。
- `zh_cn` 额外补齐了 854 个「`en_us` 有、`zh_cn.lang` 没有」的键（沿用英文原文），使 `zh_cn` 相对 `en_us.lang` 也是 100% 覆盖。原 `zh_cn.lang` 独有的 8+ 个键保持中文不变。
- 其余 31 套语言只转换自己那份 `.lang`。26.3 的 `LanguageManager` 先加载 `en_us` 再叠加所选语言，缺键自动回退英文，因此不需要把英文文案灌进每一套语言文件（那样只会让「哪些还没翻译」变得不可见）。
- `fr_fr` / `ja_jp` / `hr_hr` / `ko_kr` / `ru_ru` 等套的 `相对 en_us.lang 覆盖率` 就是原模组自身翻译的完成度，没有被本次迁移人为拉高。

## 4. 未映射 / 无消费者键

所有 2332 个 `en_us.lang` 键都有确定的 26.3 对应键，**不存在无法映射的键**（映射是纯函数，最差落到 `identity` 规则）。

下面统计「已按规则转换、但当前源码/数据/资源里没有任何消费者」的键，共 **1359** 个。这些键保留了 1.12.2 的原始前缀，在 26.3 里是死键（不影响加载，只占体积）。保留它们是为了满足「原键 100% 有归宿、可追溯」的要求，同时不丢失原模组文案（将来若把这些系统接回来，键已经在位）。

| 规则 | 键数 |
| --- | --- |
| `identity` | 719 |
| `namespace-segment-swap` | 461 |
| `potion.effect` | 148 |
| `top-level-namespace` | 31 |

按前缀分组（只列前 40 组，完整清单可由 `node scripts/convert-lang-109.cjs` 重新生成）：

| 前缀分组 | 键数 | 示例 |
| --- | --- | --- |
| `item.minecraft.*` | 148 | `item.minecraft.lingering_potion.effect.adapted`<br>`item.minecraft.lingering_potion.effect.antimall`<br>`item.minecraft.lingering_potion.effect.bleed` |
| `bestiary.progress.*` | 122 | `bestiary.progress.bullet`<br>`bestiary.progress.loading`<br>`bestiary.progress.phase_header` |
| `lore.csrp.*` | 120 | `lore.csrp.abo_bodies`<br>`lore.csrp.abo_head`<br>`lore.csrp.ada_arachnida` |
| `gui.csrp.*` | 104 | `gui.csrp.config.apply`<br>`gui.csrp.config.edit.key`<br>`gui.csrp.config.edit.title` |
| `tootip.csrp.*` | 73 | `tootip.csrp.item.0`<br>`tootip.csrp.item.1`<br>`tootip.csrp.item.10` |
| `command.srpevolution.*` | 48 | `command.srpevolution.addpoints.error.cannot_gain`<br>`command.srpevolution.addpoints.error.cannot_lose`<br>`command.srpevolution.addpoints.error.cooldown` |
| `bestiary.celestial.*` | 45 | `bestiary.celestial.arrow.desc`<br>`bestiary.celestial.arrow.name`<br>`bestiary.celestial.blip.desc` |
| `bestiary.effect.*` | 36 | `bestiary.effect.adapted.desc`<br>`bestiary.effect.antimall.desc`<br>`bestiary.effect.bleed.desc` |
| `bestiary.system.*` | 34 | `bestiary.system.collective_consciousness.desc`<br>`bestiary.system.collective_consciousness.name`<br>`bestiary.system.colonies.desc` |
| `tooltip.csrp.*` | 32 | `tooltip.csrp.alveoligrowth`<br>`tooltip.csrp.bough.line1`<br>`tooltip.csrp.bough.line2` |
| `csrp.dislodgement.*` | 31 | `csrp.dislodgement.code`<br>`csrp.dislodgement.code.value`<br>`csrp.dislodgement.decoded` |
| `bestiary.block.*` | 24 | `bestiary.block.csrp.alveoli.desc`<br>`bestiary.block.csrp.assimilated_pumpkin.desc`<br>`bestiary.block.csrp.assimilated_reed.desc` |
| `commands.csrp.*` | 22 | `commands.csrp.bestiary_stats.cap_missing`<br>`commands.csrp.bestiary_stats.cleared_other`<br>`commands.csrp.bestiary_stats.cleared_self` |
| `message.csrp.*` | 22 | `message.csrp.armortooltips1`<br>`message.csrp.dark_days.optifine_visuals_disabled`<br>`message.csrp.diffuser.already_running` |
| `srphelp.srpevolution.*` | 20 | `srphelp.srpevolution.addcooldown.desc`<br>`srphelp.srpevolution.addcooldown.usage`<br>`srphelp.srpevolution.addpoints.desc` |
| `profile.csrp.*` | 17 | `profile.csrp.adapted`<br>`profile.csrp.ancient`<br>`profile.csrp.assimilated` |
| `bestiary.tier.*` | 16 | `bestiary.tier.abomination`<br>`bestiary.tier.adapted`<br>`bestiary.tier.ancient` |
| `command.srpguide.*` | 15 | `command.srpguide.cap_missing`<br>`command.srpguide.clear_no_snapshot`<br>`command.srpguide.clear_restore_ok` |
| `chat.csrp.*` | 14 | `chat.csrp.relay.insert_module`<br>`chat.csrp.relay.no_profile`<br>`chat.csrp.relay.not_formed` |
| `srphelp.csrp.*` | 14 | `srphelp.csrp.getgeneration.desc`<br>`srphelp.csrp.getgeneration.usage`<br>`srphelp.csrp.parasites.desc` |
| `tier.csrp.*` | 14 | `tier.csrp.adapted`<br>`tier.csrp.ancient`<br>`tier.csrp.assimara` |
| `bestiary.stats.*` | 12 | `bestiary.stats`<br>`bestiary.stats.all_parasites`<br>`bestiary.stats.chart_title` |
| `srphelp.srpcolonies.*` | 12 | `srphelp.srpcolonies.clearworld.desc`<br>`srphelp.srpcolonies.clearworld.usage`<br>`srphelp.srpcolonies.removecolony.desc` |
| `bestiary.controls.*` | 8 | `bestiary.controls.greenscreen.blue`<br>`bestiary.controls.greenscreen.green`<br>`bestiary.controls.greenscreen.off` |
| `bestiary.pose.*` | 8 | `bestiary.pose.apply`<br>`bestiary.pose.panx`<br>`bestiary.pose.pany` |
| `srphelp.srpnodes.*` | 8 | `srphelp.srpnodes.clearworld.desc`<br>`srphelp.srpnodes.clearworld.usage`<br>`srphelp.srpnodes.removenode.desc` |
| `srphelp.srpvectors.*` | 8 | `srphelp.srpvectors.clearworld.desc`<br>`srphelp.srpvectors.clearworld.usage`<br>`srphelp.srpvectors.removevector.desc` |
| `bestiary.tab.*` | 7 | `bestiary.tab.blocks`<br>`bestiary.tab.celestial`<br>`bestiary.tab.current_progress` |
| `subtitles.draconite.*` | 7 | `subtitles.draconite.bell_chargeup`<br>`subtitles.draconite.death`<br>`subtitles.draconite.hurt` |
| `subtitles.kirin.*` | 7 | `subtitles.kirin.blackhole`<br>`subtitles.kirin.death`<br>`subtitles.kirin.hurt` |
| `bestiary.effects.*` | 6 | `bestiary.effects.home`<br>`bestiary.effects.id`<br>`bestiary.effects.missing_desc` |
| `srphelp.srpdislodgment.*` | 6 | `srphelp.srpdislodgment.codes_reset.desc`<br>`srphelp.srpdislodgment.codes_reset.usage`<br>`srphelp.srpdislodgment.random_code.desc` |
| `srphelp.srpguide.*` | 6 | `srphelp.srpguide.clearall.desc`<br>`srphelp.srpguide.clearall.usage`<br>`srphelp.srpguide.restore.desc` |
| `srphelp.srpudevelopment.*` | 6 | `srphelp.srpudevelopment.getlevel.desc`<br>`srphelp.srpudevelopment.getlevel.usage`<br>`srphelp.srpudevelopment.setlevel.desc` |
| `bestiary.systems.*` | 5 | `bestiary.systems.home`<br>`bestiary.systems.id`<br>`bestiary.systems.missing_desc` |
| `bestiary.blocks.*` | 4 | `bestiary.blocks.empty`<br>`bestiary.blocks.home`<br>`bestiary.blocks.subtitle` |
| `bestiary.nav.*` | 4 | `bestiary.nav.back`<br>`bestiary.nav.home`<br>`bestiary.nav.mob_list` |
| `death.attack.*` | 4 | `death.attack.csrp.ricardo`<br>`death.attack.sepeku`<br>`death.attack.srp_dod_block` |
| `srphelp.srpgeneration.*` | 4 | `srphelp.srpgeneration.getgeneration.desc`<br>`srphelp.srpgeneration.getgeneration.usage`<br>`srphelp.srpgeneration.setgeneration.desc` |
| `subtitles.hi_blaze.*` | 4 | `subtitles.hi_blaze.death`<br>`subtitles.hi_blaze.hurt`<br>`subtitles.hi_blaze.idle` |

典型情况：
- `bestiary.*`（0 键）：1.12.2 的图鉴文案，本工程改用自带的 `assets/csrp/compendium/lang/*.lang` 渲染图鉴，因此这批键在 26.3 侧没有消费者。
- `subtitles.*`（扁平原名，如 `subtitles.buglinliving`）：本工程的 `sounds.json` 用的是 26.3 风格 `subtitles.csrp.<mob>.<action>`，扁平原名不再被引用。
- `tootip.*`（`tootip` 是原模组的拼写错误）：按「保留前缀、只换命名空间」转换；本工程源码用的是 `tooltip.csrp.*`。
- `potion.effect.*` / `splash_potion.*` / `lingering_potion.*` / `tipped_arrow.*`：本工程未注册对应的药水物品，因此没有消费者。

## 5. 源码实际使用但原 lang 缺失的键（补齐清单）

- 静态扫描到的翻译键字面量总数（去重）：**486**。来源：`src/main/java/alku/csrp/**/*.java` 的 `Component.translatable(...)`、`src/main/resources/data/**/*.json` 的 `"translate"`、`assets/csrp/sounds.json` 的 `subtitle`。vanilla 自带的键（`subtitles.block.generic.*`、`subtitles.entity.generic.*` 等 6 个）已剔除：本模组不应重复定义它们，否则会覆盖原版文案。
- 三类归属：
  1. **281** 个能由 1.10.9 lang 直译得到（`en_us.lang` 有对应键）。
  2. **21** 个原模组 lang 覆盖不到、需要「补齐」的键 —— 下表全部列出。
  3. **184** 个是本工程移植时自建的 26.3 专用键（如 `screen.csrp.*`、`message.csrp.*`、`options.csrp.*`）：它们在任务开始时的 `en_us.json`（1456 键）里就已经存在，不属于本次补齐范围。
- 另有 6 个运行时拼接的前缀字面量，静态无法解析，见 5.2。

| 26.3 键 | 取值来源 | 引用它的文件 |
| --- | --- | --- |
| `advancement.csrp.cosmic_structural_failure.title` | `alias` → `advancements.csrp.cosmic_structural_failure.title` | `src/main/resources/data/csrp/advancement/cosmic_structural_failure.json` |
| `advancement.csrp.hellfire_chemical_warfare.title` | `alias` → `advancements.csrp.hellfire_chemical_warfare.title` | `src/main/resources/data/csrp/advancement/hellfire_chemical_warfare.json` |
| `advancements.csrp.columbus.desc` | `alias` → `advancements.csrp.columbus.description` | `src/main/resources/data/csrp/advancement/csrp/columbus.json` |
| `advancements.csrp.controversial.desc` | `alias` → `advancements.csrp.controversial.description` | `src/main/resources/data/csrp/advancement/csrp/controversial.json` |
| `advancements.csrp.cosmic_structural_failure.desc` | `alias` → `advancements.csrp.cosmic_structural_failure.description` | `src/main/resources/data/csrp/advancement/csrp/cosmic_structural_failure.json` |
| `advancements.csrp.cut_roots.desc` | `alias` → `advancements.csrp.cut_roots.description` | `src/main/resources/data/csrp/advancement/csrp/cut_roots.json` |
| `advancements.csrp.dark_days.desc` | `alias` → `advancements.csrp.dark_days.description` | `src/main/resources/data/csrp/advancement/csrp/dark_days.json` |
| `advancements.csrp.fermented_arrogance.desc` | `alias` → `advancements.csrp.fermented_arrogance.description` | `src/main/resources/data/csrp/advancement/csrp/fermented_arrogance.json` |
| `advancements.csrp.fog_nullifier.desc` | `alias` → `advancements.csrp.fog_nullifier.description` | `src/main/resources/data/csrp/advancement/csrp/fog_nullifier.json` |
| `advancements.csrp.guerilla.desc` | `alias` → `advancements.csrp.guerilla.description` | `src/main/resources/data/csrp/advancement/csrp/guerilla.json` |
| `advancements.csrp.hellfire_chemical_warfare.desc` | `alias` → `advancements.csrp.hellfire_chemical_warfare.description` | `src/main/resources/data/csrp/advancement/csrp/hellfire_chemical_warfare.json` |
| `advancements.csrp.hunt_season.desc` | `alias` → `advancements.csrp.hunt_season.description` | `src/main/resources/data/csrp/advancement/csrp/hunt_season.json` |
| `advancements.csrp.peacekeeper.desc` | `alias` → `advancements.csrp.peacekeeper.description` | `src/main/resources/data/csrp/advancement/csrp/peacekeeper.json` |
| `advancements.csrp.qliphoth_overload.desc` | `alias` → `advancements.csrp.qliphoth_overload.description` | `src/main/resources/data/csrp/advancement/csrp/qliphoth_overload.json` |
| `advancements.csrp.root.desc` | `alias` → `advancements.csrp.root.description` | `src/main/resources/data/csrp/advancement/csrp/root.json` |
| `advancements.csrp.sepeku.desc` | `alias` → `advancements.csrp.sepeku.description` | `src/main/resources/data/csrp/advancement/csrp/sepeku.json` |
| `advancements.csrp.stolas.desc` | `alias` → `advancements.csrp.stolas.description` | `src/main/resources/data/csrp/advancement/csrp/stolas.json` |
| `item.csrp.itemmobspawner` | `synthesized`（原模组 lang 无此键，按用途合成） | `src/main/java/alku/csrp/item/LegacyMobSpawnerItem.java` |
| `subtitles.assimsquiddeath` | `synthesized`（原模组 lang 无此键，按用途合成） | `assets/csrp/sounds.json` |
| `subtitles.assimsquidhurt` | `synthesized`（原模组 lang 无此键，按用途合成） | `assets/csrp/sounds.json` |
| `subtitles.rof.emerge` | `synthesized`（原模组 lang 无此键，按用途合成） | `assets/csrp/sounds.json` |

说明：第 2 类里绝大多数是**数据文件写错了后缀**——`src/main/resources/data/csrp/advancement/*.json` 用了 `advancements.csrp.<id>.desc`，而 26.3 的进度描述键是 `.description`。本脚本按 `.desc → .description` 别名补出正确的键，同时保留数据文件里写的 `.desc`（不动数据文件，只补语言键）。

### 5.1 兼容别名（源码仍写旧命名空间）

| 别名键 | 原因 |
| --- | --- |
| `tootip.srparasites.lurecomp.1` | LureComponentItem.java 仍使用 1.12.2 命名空间 |
| `tootip.srparasites.lurecomp.2` | LureComponentItem.java 仍使用 1.12.2 命名空间 |
| `tootip.srparasites.lurecomp.3` | LureComponentItem.java 仍使用 1.12.2 命名空间 |
| `tootip.srparasites.lurecomp.4` | LureComponentItem.java 仍使用 1.12.2 命名空间 |
| `tootip.srparasites.lurecomp.5` | LureComponentItem.java 仍使用 1.12.2 命名空间 |
| `tootip.srparasites.lurecomp.6` | LureComponentItem.java 仍使用 1.12.2 命名空间 |

### 5.2 运行时拼接前缀

| 前缀字面量 | `en_us.json` 中可命中的具体键数 | 位置 |
| --- | --- | --- |
| `tootip.srparasites.lurecomp.` | 6 | `src/main/java/alku/csrp/item/LureComponentItem.java` |
| `tooltip.csrp.` | 88 | `src/main/java/alku/csrp/item/RelayModuleItem.java` |
| `report.csrp.` | 74 | `src/main/java/alku/csrp/item/RelayReportItem.java`<br>`src/main/java/alku/csrp/relay/client/RelayReportScreen.java` |
| `report.csrp.tier.` | 14 | `src/main/java/alku/csrp/item/RelayReportItem.java` |
| `report.csrp.dislodgement.effect.` | 24 | `src/main/java/alku/csrp/item/RelayReportItem.java` |
| `report.csrp.field.` | 23 | `src/main/java/alku/csrp/item/RelayReportItem.java` |

## 6. 冲突与决策

同一目标键被多个原始键映射到时，按第 2 节的规则优先级保留，其余丢弃（值相同的丢弃无影响，值不同的以高优先级为准）。

| 目标键 | 保留 | 丢弃 | 规则（保留 / 丢弃） | 值是否不同 |
| --- | --- | --- | --- | --- |
| `block.csrp.relaycontroller` | `tile.srparasites.relaycontroller.name` | `tile.relaycontroller.name` | `tile.name` / `tile.legacy-no-namespace` | 是 |
| `block.csrp.relay_base` | `tile.srparasites.relay_base.name` | `tile.relay_base.name` | `tile.name` / `tile.legacy-no-namespace` | 是 |
| `block.csrp.relay_middle` | `tile.srparasites.relay_middle.name` | `tile.relay_middle.name` | `tile.name` / `tile.legacy-no-namespace` | 是 |
| `block.csrp.relay_roof` | `tile.srparasites.relay_roof.name` | `tile.relay_roof.name` | `tile.name` / `tile.legacy-no-namespace` | 是 |
| `effect.csrp.dod_smoke_trail` | `effect.srparasites.dod_smoke_trail` | `mob_effect.srparasites:dod_smoke_trail` | `effect.dot` / `mob_effect.colon` | 否 |
| `effect.csrp.thornshade_thorns` | `effect.srparasites.thornshade_thorns` | `mob_effect.srparasites:thornshade_thorns` | `effect.dot` / `mob_effect.colon` | 是 |
| `effect.csrp.distorted_enlightenment` | `mob_effect.srparasites:distorted_enlightenment` | `effect.srparasites.distorted_enlightenment` | `mob_effect.colon` / `effect.dot` | 否 |
| `effect.csrp.the_sign` | `mob_effect.srparasites:the_sign` | `effect.srparasites.the_sign` | `mob_effect.colon` / `effect.dot` | 否 |

关键决策：
1. **既有键值优先。** `en_us.json` / `zh_cn.json` 里已有的键一律不覆盖，只补缺失键（`repair` 仅针对空值或「值等于键名」的占位，本次迁移未发现此类占位）。
2. **同键冲突按规则优先级。** `mob_effect.srparasites:<id>`（1.12.2 的效果名键）优先于 `effect.srparasites.<id>`；`tile.srparasites.<id>.name`（带命名空间的正式键）优先于 `tile.<id>.name`（遗留键）。
3. **过期 `.lang` 已移除。** 目标目录里原有的 33 个 `.lang` 是早先只换命名空间、没换键格式的半成品，26.3 不会加载它们，且会与 `.json` 互相矛盾；已全部转换成 `.json` 后删除。`scripts/verify-lang-format.cjs` 会断言目录里不再有 `.lang`。
4. **`lv_LV` → `lv_lv`。** 26.3 的语言代码是小写，`lv_LV` 从来就加载不到。
5. **`ja_jp21` / `zh_tw21` 不是合法的 26.3 语言代码**，但按「完整迁移」要求仍照原样转换保留，内容与 `ja_jp` / `zh_tw` 的 21 版原文一致。

迁移掉的过期文件（33 个）：`assets/csrp/lang/de_at.lang`，`assets/csrp/lang/de_ch.lang`，`assets/csrp/lang/de_de.lang`，`assets/csrp/lang/en_pt.lang`，`assets/csrp/lang/en_us.lang`，`assets/csrp/lang/en_ws.lang`，`assets/csrp/lang/es_ar.lang`，`assets/csrp/lang/es_cl.lang`，`assets/csrp/lang/es_ec.lang`，`assets/csrp/lang/es_es.lang`，`assets/csrp/lang/es_mx.lang`，`assets/csrp/lang/es_uy.lang`，`assets/csrp/lang/es_ve.lang`，`assets/csrp/lang/fr_ca.lang`，`assets/csrp/lang/fr_fr.lang`，`assets/csrp/lang/hr_hr.lang`，`assets/csrp/lang/it_it.lang`，`assets/csrp/lang/ja_jp.lang`，`assets/csrp/lang/ja_jp21.lang`，`assets/csrp/lang/ko_kr.lang`，`assets/csrp/lang/lol_us.lang`，`assets/csrp/lang/lv_LV.lang`，`assets/csrp/lang/nl_nl.lang`，`assets/csrp/lang/pl_pl.lang`，`assets/csrp/lang/pt_br.lang`，`assets/csrp/lang/ro_ro.lang`，`assets/csrp/lang/ru_ru.lang`，`assets/csrp/lang/sv_se.lang`，`assets/csrp/lang/tr_tr.lang`，`assets/csrp/lang/uk_ua.lang`，`assets/csrp/lang/zh_cn.lang`，`assets/csrp/lang/zh_tw.lang`，`assets/csrp/lang/zh_tw21.lang`

## 7. `_pending` 合并

| 来源文件 | 文件内键数 | 已存在于 en_us.json | 示例 |
| --- | --- | --- | --- |
| `src/main/resources/assets/csrp/lang/_pending/items.json` | 22 | 22 | `item.csrp.discone`，`item.csrp.disctwo`，`item.csrp.relay_report`，`item.csrp.scan_report`，`item.csrp.vector_report` |

合并策略：`_pending` 的键写入 `en_us.json`；既有键仍优先。合并是幂等的，`_pending` 文件保留不删（它们属于 items teammate 的写入范围）。

## 8. 已知问题（按任务要求只记录，未修改源码）

| 位置 | 问题 | 本次处理 |
| --- | --- | --- |
| `src/main/java/alku/csrp/item/LureComponentItem.java:41` | 硬编码 `"tootip.srparasites.lurecomp." + version`：旧命名空间 + `tootip` 拼写错误 | 已在 lang 里补出 `tootip.srparasites.lurecomp.1..6` 别名使其可用；建议源码改为 `tooltip.csrp.lurecomp.` |
| `src/main/resources/data/csrp/advancement/*.json` | 4 处使用单数 `advancement.csrp.<id>.title/.desc`（26.3 应为 `advancements.` / `.description`） | 已补出对应别名键；建议数据文件改为 `advancements.csrp.<id>.title|description` |
| `src/main/java/alku/csrp/item/LegacyMobSpawnerItem.java:229` | `Component.translatable("item.csrp.itemmobspawner", legacyName)` 的键在原模组 lang 中不存在 | 已合成 `item.csrp.itemmobspawner` = `Spawn %s` |
| `assets/csrp/sounds.json` | 引用了 6 个 vanilla 字幕键（`subtitles.block.generic.*`、`subtitles.entity.generic.explode`） | 由原版客户端语言包提供，本模组不重复定义（重复定义反而会覆盖原版） |
| `src/main/resources/assets/csrp/lang/_pending/items.json` | `_pending/` 是构建期交接目录，但位于 resources 下，因此会被打进 jar | 对运行时无影响（MC 只按语言代码精确查找 `lang/<code>.json`）。建议 items teammate 收尾时把它移出 resources，或在 `build.gradle` 里排除 |

## 9. 复现与校验

```bash
node scripts/convert-lang-109.cjs            # 幂等转换（可重复执行，第二次起零改动）
node scripts/convert-lang-109.cjs --check     # 只检查是否需要写入；已同步时 exit 0
node scripts/verify-lang-parity.cjs          # 键覆盖 / 映射抽样 / 源码键断言
node scripts/verify-lang-format.cjs          # JSON 合法性 / § 完整性 / 过期 .lang 断言
node scripts/run-all-verifications.cjs
```

注意：本脚本会扫描 `src/main/java/**` 与 `src/main/resources/data/**` 里实际使用的键。其他 teammate 每新增一处 `Component.translatable(...)` / 数据文件 `translate` / `sounds.json` `subtitle`，都可能带来新键；**在所有 teammate 收尾后请再跑一次 `node scripts/convert-lang-109.cjs`**，把新键补齐并提交。`scripts/verify-lang-parity.cjs` 会在有键缺失时失败，可用来判断是否需要重跑。

