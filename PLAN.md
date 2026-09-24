# PLAN — SRParasites (csrp) 移植到 Minecraft 26.3 / NeoForge 26.3

> 本文件是 26.3 移植工作的共享工件（subagent 链 A→B→C→D 共用）。
> 最后更新：wave 3 完成时（错误 3235 → 3062）。
>
> **工具链变更**：本机没有 python 解释器，`scripts/port263/census.py` 已用 Node 重写为
> `census.mjs`（同样参数、同样输出：`.ref263/censusN.txt` + `.json`，并修掉了 javac 诊断输出
> 用控制台字符集导致路径乱码的问题）。另外新增两个 Node 查证工具：
> `errors.mjs`（按符号/消息/路径查询 census JSON，带源码行）与
> `refgrep.mjs`（在 `.ref263/mc-src` 真实 26.3 源码树里检索 API）。

---

## 1. 目标

把本仓库（已从原版 SRParasites 1.10.9 / MC 1.12.2 Forge 移植到 MC 1.21.1 NeoForge 21.1.235）
继续移植到 **Minecraft 26.3 + NeoForge 26.3（当前只有 beta）**，并最终达到内容 100% 还原。

- 原版参考 jar：`[逃逸：寄生体] SRParasites-1.10.9.jar`（1.12.2 / Forge，`modid=srparasites`，
  174 个实体、`mcmod.info` + `FMLCorePlugin` + Mixin 0.8.7）
- 当前仓库规模：462 个 Java 文件、8008 个资源文件
- MC 26.3 发布时间：**2026-09-15**（两天前），生态尚在跟进

---

## 2. 目标版本（已实测钉选，不要凭记忆改）

| 项 | 值 | 说明 |
|---|---|---|
| MC | `26.3` | Mojang manifest 确认 `javaVersion.majorVersion=25` |
| NeoForge | `26.3.0.3-beta` | maven 上 26.3 线最新；26.3.0.0/.1/.2/.3 均为 beta |
| moddev (ModDevGradle) | **`2.0.147`** | **硬性要求，见第 3 节** |
| Gradle wrapper | `9.2.1` | 已从脚手架模板拷入 |
| JDK | GraalVM 25（`D:/MC/jdk/graalvm-25.2.4+7.1`） | 项目内 `org.gradle.java.home` 钉死 |
| Java toolchain | 25 | `java.toolchain.languageVersion` |

`gradle.properties` 里额外钉了三行（因为 `mc_gradle` 没有 env 参数，无法在调用处设 JAVA_HOME）：

```properties
org.gradle.java.home=D:/MC/jdk/graalvm-25.2.4+7.1
org.gradle.java.installations.paths=D:/MC/jdk/graalvm-25.2.4+7.1,D:/MC/jdk/graalvm-community-25.3.4.1+1.1
org.gradle.java.installations.auto-download=false
```

---

## 3. ⚠️ 关键坑：NeoForm 26.3 recompile 失败（已解决，勿回退）

**症状**：`createMinecraftArtifacts` 阶段 `recompile` 失败：

```
ERROR Line: 44, <..net.minecraft.core.HolderSet$1>..contents()....net.minecraft.core.HolderSet.Named..contents()
```

**根因**（已定位到文件级）：MC 26.3 源码里 `HolderSet.emptyNamed()` 有一个匿名子类，
而 NeoForge 26.3 自带的 access transformer 有两条相邻条目：

```
public net.minecraft.core.HolderSet$Named contents()Ljava/util/List;
public net.minecraft.core.HolderSet$1     contents()Ljava/util/List;
```

NeoForge 的 `jst` 变换工具（`net.neoforged.jst:jst-cli-bundle`）**无法为匿名内部类建立
binary representation**（`transformSources/problems.json` 里报
`Missing Target: The target net.minecraft.core.HolderSet$1 METHOD contents() does not exist`），
于是只把 `HolderSet$Named.contents()` 放宽成 `public`，匿名子类的 `protected contents()`
就成了「用更弱的访问权限覆盖」——javac 报错，整个 NeoForm recompile 挂掉。

**已排除的伪因**（不要再走这些弯路）：

- ❌ 不是 GraalVM 的问题：Oracle GraalVM `25.2.4+7.1`(javac 25.0.4) 和
  community `25.3.4.1+1.1`(javac 25.0.4.1) 都失败。
- ❌ 不是某个 beta 版本的问题：`26.3.0.0-beta` / `.1-beta` / `.3-beta` 全部失败。
- ❌ 不是 pandle 的 AT 需要手改。

**正解**：`jst-cli-bundle` 升到 **2.0.11** 即可（2.0.10 有该 bug）。
moddev `2.0.144` → NFRT 2.0.x → jst **2.0.10**（坏）；
moddev **`2.0.147`** → NFRT 2.0.31 → jst **2.0.11**（好）。

实测：moddev 2.0.147 + NeoForge 26.3.0.3-beta + GraalVM 25 → `BUILD SUCCESSFUL`。
**因此 `build.gradle` 里的 moddev 版本绝不能低于 2.0.147。**

---

## 4. 依赖处置

| 依赖 | 1.21.1 状态 | 26.3 可行性 | 决定 |
|---|---|---|---|
| `ldlib2`（lowdragmc） | 1 个文件用（`CreditsTitleScreenEvents`） | 无 26.3 构建 | **丢弃**，改用原版 Screen 重写 |
| `citadel`（alexthe666） | 13 处 import，约 28 个文件 | 最高只到 MC 1.21.11，**无 26.x** | **用自研实现替换**（见 5.4） |

涉及的原版/第三方 API 面很窄：

```
com.github.alexthe666.citadel.client.model.AdvancedModelBox            (6)
com.github.alexthe666.citadel.client.model.AdvancedEntityModel         (5)
com.github.alexthe666.citadel.client.model.basic.BasicModelPart        (2)
com.github.alexthe666.citadel.client.model.container.TabulaModelContainer (1)
com.github.alexthe666.citadel.client.model.container.TabulaCubeContainer  (1)
com.github.alexthe666.citadel.client.model.TabulaModelHandler          (1)
com.github.alexthe666.citadel.client.model.TabulaModel                 (1)
```

---

## 5. 错误普查（census）现状

工具：`scripts/port263/census.py`（用 Gradle 导出的真实 classpath 直接跑 javac，
`-Xmaxerrs 30000`，产出 `.ref263/census.txt` + `.json`）。

| 阶段 | 错误数 | 受影响文件 |
|---|---|---|
| 移植 build 配置后 | **4815** | 370 / 462 |
| wave 1（类型改名/包迁移） | **3860** | 339 |
| wave 2（访问器/常量改名） | **3235** | 322 |
| wave 3（简单缺失符号） | **3062** | 319 |

### 已完成的 wave

- **wave 1** `scripts/port263/wave1_renames.py`（195 文件，~2900 处）
  - `ResourceLocation` → `Identifier`（**639 处，全项目最大的单项**）
  - `MobSpawnType` → `EntitySpawnReason`（139）
  - `ItemInteractionResult` → `InteractionResult`（23）
  - `InteractionResultHolder` → `InteractionResult`（含 `.sidedSuccess/.pass/.consume/.success/.fail` 重写，19 文件）
  - 包迁移：`GameRules`→`world.level.gamerules`、`RenderType`→`client.renderer.rendertype`、
    `WaterAnimal`→`entity.animal.fish`、`Villager`→`entity.npc.villager`、
    `ArmorMaterial`→`item.equipment`、`Snowball`→`projectile.throwableitemprojectile`、
    `AbstractSkeleton`→`monster.skeleton`、`Slime`→`monster.cubemob`、
    `AbstractArrow`/`Arrow`→`projectile.arrow`、`CriteriaTriggers`→`advancements.triggers`、
    `GuiMessage`/`GuiMessageTag`→`client.multiplayer.chat`
- **wave 2** `scripts/port263/wave2_accessors.py`（193 文件，635 处）
  - `Level.isClientSide` 字段私有化 → `isClientSide()`（337）
  - `Level.random` 变 protected → `getRandom()`（46）
  - `Entity.moveTo(...)` → `snapTo(...)`（114；**`PathNavigation.moveTo` 仍存在，必须跳过**）
  - `MobEffects` 常量改名：MOVEMENT_SLOWDOWN→SLOWNESS、DIG_SLOWDOWN→MINING_FATIGUE、
    MOVEMENT_SPEED→SPEED、CONFUSION→NAUSEA、DAMAGE_BOOST→STRENGTH、DAMAGE_RESISTANCE→RESISTANCE
  - `GameRules.RULE_MOBGRIEFING` → `MOB_GRIEFING`（29）
  - `InteractionResult.sidedSuccess(bool)` → `InteractionResult.SUCCESS`（28）
- **wave 3** `scripts/port263/wave3_symbols.mjs`（60 文件，157 处 + 21 处手改；3235 → 3062）
  - `Entity.isInWaterOrBubble()` 消失 → `isInWater()`（53；26.3 的流体交互改由
    `FluidInteraction`/`isInFloatableFluid()` 承担）
  - `Entity.hurtMarked` / `Entity.hasImpulse` 统一成公开字段 `Entity.syncVelocity`
    （26.3 `Entity.java:284`；vanilla `Ravager` 就是 `defender.syncVelocity = true`）（28）
  - `BlockBehaviour.Properties.noCollission()` → `noCollision()`（19）
  - `Level.getMinBuildHeight()` → `getMinY()`（14）
  - `Level.getDayTime()` → `getOverworldClockTime()`（13）
  - **GameRules 重做**：`Level.getGameRules().getBoolean(rule)` 在 26.3 不存在，只有
    `ServerLevel.getGameRules().get(GameRule<T>)`；新增 `alku.csrp.world.SrpGameRules`
    收口（30）
  - `Player.displayClientMessage(msg, true)` → `ServerPlayer/LocalPlayer.sendOverlayMessage(msg)`（8）
  - `ServerPlayer.getServer()` → `player.level().getServer()`（8；26.3 的
    `ServerPlayer.level()` 返回 `ServerLevel`）
- **wave 4** `scripts/port263/wave4_optional_nbt.mjs` + `wave4b_nbt_containers.mjs`（598 + 41 处；
  3062 → 2496）
  - 26.3 的 `CompoundTag` getter 全部改为返回 `Optional<T>`：`getInt/getBoolean/getFloat/getDouble/
    getLong/getString/getByte/getShort` → 对应的 `...Or(key, 默认值)`，默认值就是 1.21.1 缺键时
    返回的值，因此语义等价（598）
  - 容器 getter 没有 `...Or`：`getCompound` → `getCompoundOrEmpty`、`getList(key, Tag.TAG_X)` →
    `getListOrEmpty(key)`、`getIntArray/getLongArray` → `.orElse(new int[0]/new long[0])`（41）
  - ⚠️ 该类统一改名会误伤同名非 NBT 方法（`Component.getString()`、`Boolean.getBoolean(String)`），
    已用 `fix_empty_or_args.mjs` 修复 11 处并给 wave 4 脚本加了「空参数列表即跳过」的保护

### 剩余错误聚类（wave 3+ 的输入）

| 数量 | 问题 | 性质 |
|---|---|---|
| 419 | `method does not override or implement a method from a supertype` | 签名变更，需逐类核对 |
| 189 + 47 + 34 + 25 + 24 | `Optional<...>` 不能转成裸值 | API 改为 Optional 返回 |
| 154 + 27 | `EntityType.create(ServerLevel/Level)` 无匹配 | 生成语义变化 |
| 98 | 无法推断 `T#1` | 泛型收紧 |
| 59 | 条件表达式类型不匹配 | 连带 |
| 48 + 16 + 22 | `ArmorItem` / `ArmorItem` 包不存在 / `ArmorMaterial` | **装备系统重做（组件化）** |
| 53 | `MultiBufferSource` 缺失 | **渲染管线重写（renderpearl）** |
| 53 | `isInWaterOrBubble()` 缺失 | 待查新名 |
| 49 + 8 | `AdvancedModelBox` / `AdvancedEntityModel` | **Citadel 替换** |
| 42 | `GuiGraphics` 缺失 | **GUI 渲染重写** |
| 37×3 | `putUUID` / `hasUUID` / `getUUID`（CompoundTag） | NBT API 变更 |
| 40 | `mulPose(Quaternionf)` | 矩阵/姿态 API |
| 40 | `registerEntityRenderer` 签名 | 客户端注册事件 |
| 31 | `UseAnim` 缺失 | 物品动画枚举 |
| 31 | `registerItem(String, Item.Properties, Properties)` | 注册器签名 |
| 34 | `String` 不能转 `ResourceKey<EntityType<?>>` | 注册表 key 化 |
| 24 | `CompoundTag` 不能转 `ValueOutput` | **NBT ↔ 新的 ValueOutput 序列化** |

### 26.3 重大结构性变化（必须在后续 wave 里当设计问题处理，不能只改名）

1. **MC 26.1 起不再混淆**，因此类名回归 Mojang 源码名（`ResourceLocation`→`Identifier` 等）。
2. **新渲染后端 `com.mojang.renderpearl.*`**：
   `GlStateManager`→`com.mojang.renderpearl.backend.opengl`、
   `VertexFormat`→`com.mojang.renderpearl.api.vertex`、
   `VertexBuffer`→`com.mojang.renderpearl.backend.api`；
   `MultiBufferSource` / `GuiGraphics` / `LightTexture` / `Tesselator` / `RenderType` 全部重排。
   → `client/` 的 73 个文件 + 所有 renderer 是本次移植的最大工作量。
3. **装备系统组件化**：`ArmorItem`/`SwordItem`/`AxeItem`/`Tier`/`DiggerItem` 等类消失，
   `ArmorMaterial` 移到 `world.item.equipment`。
4. **GameRules 重做**：`GameRules` 成为 holder 式 `GameRule<T>` 常量表。
5. **NBT → ValueOutput/ValueInput** 序列化接口。

---

## 6. 后续 wave 划分（建议顺序）

| wave | 内容 | 预估错误 | 说明 |
|---|---|---|---|
| 3 ✅ | 简单缺失符号：`isInWaterOrBubble` 新名、`getMinBuildHeight`、`getDayTime`、`hurtMarked`、`hasImpulse`、`displayClientMessage` 等 | 已做 | 完成，3235 → **3062** |
| 4 ✅ | `Optional` 包装类 API（`Optional<Integer>`/`Optional<Boolean>`） | 已做 | 完成，3062 → **2496**（NBT getter 全部 Optional 化） |
| 5 | 实体注册/生成：`create(...)`、`ResourceKey<EntityType<?>>`、`registerEntityRenderer` | ~250 | 注册器与 key 化 |
| 6 | NBT / `ValueOutput` 序列化 | ~150 | `putUUID`/`hasUUID`/`getUUID`、`CompoundTag`↔`ValueOutput/ValueInput` |
| 7 | 装备/物品组件化（`ModItems` 181 错、`ModArmorMaterials`、`ModTiers`） | ~300 | 设计问题 |
| 8 | **Citadel 替换**：自研 `AdvancedModelBox` / `AdvancedEntityModel` / `BasicModelPart` / Tabula 容器 | ~80 + 28 文件返工 | 见 5.4 |
| 9 | 渲染管线 renderpearl（`client/` 73 文件） | ~400 | **最大且最难** |
| 10 | `method does not override`（419）收尾 | ~400 | 逐类核对 |

> 注：各 wave 的错误数会互相重叠（修好一个上游符号会连带消掉下游错误），
> 因此总量**不是简单相加**。以 census 实测为准。

---

## 7. 每次改动的固定流程

```bash
# 1) 改代码（机械改名优先写成 scripts/port263/waveN_*.mjs，不要手改 300 个文件）
# 2) 用 Gradle 跑真实构建（铁律：mc_gradle，不要直接调 gradlew）
#    mc_gradle(projectDir="D:/code/MC模组/csrp", task="compileJava")
# 3) 需要完整错误清单时用 census（Gradle 输出会被 harness 截断，本身也不够用）
node scripts/port263/census.mjs --out .ref263/censusN.txt
node scripts/port263/errors.mjs --symbol isInWaterOrBubble --source   # 查某类错误
node scripts/port263/refgrep.mjs "syncVelocity" -p net/minecraft/world/entity/Entity.java
# 4) 提交
git commit -m "-（移植）..."
```

> census 与 `mc_gradle compileJava` 的错误数必须一致（wave 3 实测两边都是 3062），
> 不一致说明 classpath 过期：先删掉 `build/compile-cp.txt` 再跑 `dumpCompileClasspath`。
>
> ⚠️ **语法错误会吞掉全部类型错误**：只要有一个文件解析失败，javac 就不进入 attr/flow 阶段，
> census 会给出一个虚低的数字（实测 wave 4 中途出现过「只剩 11 个错误」，修掉 11 处
> `getStringOr(, "")` 语法错误后真实数字是 2537）。因此 census 后先看有没有
> `illegal start of expression` / `';' expected` 这类 parse error，有就先修语法再读数。

**工作方法要求**：任何 API 签名/新类名都必须先在
`.ref263/mc-src`（真实 26.3 + NeoForge 26.3 源码树，28795 个文件）
或 `.ref263/typeindex.json`（8910 个类型的全量索引）里查证，**禁止凭记忆写**。

---

## 8. 当前状态与下一步

- ✅ 26.3 工具链打通（build 配置已移植、jst 坑已解）
- ✅ 错误从 4815 降到 **2496**（-48%），195 + 193 + 60 + 约 120 个文件已过 wave 1/2/3/4
- ⬜ 剩余 2496 个错误，按第 6 节 wave 5~10 推进
- ⬜ 内容补全到 100%（原版 174 实体 vs 当前 96；73 类 AI vs 5）——**版本移植完成后**再做

**下一步（下一个 round）**：wave 5（`EntityType.create(Level, EntitySpawnReason)` 与
`ResourceKey<EntityType<?>>`、`RegisterRenderers.registerEntityRenderer` 签名），
随后 wave 6（NBT ↔ `ValueOutput/ValueInput`、`putUUID/hasUUID/getUUID`）。
