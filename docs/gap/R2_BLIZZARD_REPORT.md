# R2 差集移植报告：暴风雪与星型相关内容（SRParasites 1.10.9 / 1.12.2 Forge → csrp / Minecraft 26.3 / NeoForge）

- 原始事实来源（只读）：`D:\code\MC模组\_scratch\vf\out109\com\dhanantry\scapeandrunparasites\`
- 目标工程：`D:\code\MC模组\csrp-26.3`，分支 `port/neoforge-26.3`
- 平台：Minecraft 26.3 / NeoForge 26.3.0.1-beta / Java 25 / moddev 2.0.147
- 本报告只描述 backlog **R2** 这一批（`client/weather/**`、`world/star/**`、两个 `network/MsgSync*`、`client/shader/**` 与两个渲染 mixin 的裁定）

## 0. 结论摘要

| 项目 | 结果 |
| --- | --- |
| 原文件总数 | 17（6 客户端天气 + 4 星/地形 + 2 网络 + 3 mixin/accessor + 2 shader manager） |
| 完整移植 | 6（`SRPBlizzardClient`、`SRPBlizzardDirectionClient`、`SRPBlizzardDerivedHandler`、`SRPStarTypeSyncHandler`、`MsgSyncBlizzardReverse`、`MsgSyncStarType`） |
| 改写移植（同一行为，换 26.3 通道） | 5（`SRPBlizzardClientEvents`、`SRPBlizzardFogRenderer`、`SRPBlizzardRenderer`、`SoundBlizzardReverse`、`SRPFracturedTerrainHandler`） |
| 判定“不适用”（含替代方案） | 6（`GenLayerSRPDynamicStar` 转为 BiomeSource 派发 + 两个渲染 mixin + 1 个 accessor + 2 个 shader manager） |
| 新增 Java 文件 | 13 |
| 新增校验脚本 | 3 |
| `gradlew.bat build -x test` | BUILD SUCCESSFUL |
| `node scripts/run-all-verifications.cjs` | `{"total":109,"passed":109,"failed":0}`（基线 92/92 无新增失败） |
| commit | `bfd9ef75`（第 1-2 批）、`300578d3`（第 3 批） |

---

## 1. 逐项映射：原文件 → 26.3 实现

### 1.1 客户端天气

| 1.10.9 原文件 | 26.3 实现 | 状态 | 说明 |
| --- | --- | --- | --- |
| `client/weather/SRPBlizzardClient.java`(22) | `client/weather/SRPBlizzardClient.java` | 完整移植 | `isColdWorld()` = 主世界维度 + `StarWorldClientState.starType() == COLD`（原为 `SRPClientStarWorldState.isCold()`，同一数据源）；`getIntensity()` = `ClientLevel#getRainLevel(partialTicks)`（原 `World#getRainStrength`）。另新增 `daylight(partialTicks)`：原 `World#getSunBrightness` 在 26.3 已删除，按原公式用 `getOverworldClockTime` / `getRainLevel` / `getThunderLevel` 重建 |
| `client/weather/SRPBlizzardClientEvents.java`(52) | `client/weather/SRPBlizzardClientEvents.java` | 改写移植 | 全屏白雾叠加：`RenderGameOverlayEvent.Pre(ALL)` → `RenderGuiEvent.Pre` + `GuiGraphicsExtractor#fill`（alpha = `clamp(intensity*0.24, 0, 0.24)`，RGB 205/215/224 原样）。世界加载/卸载重置：26.3 客户端没有 `WorldEvent.Load/Unload` 对，改为每 tick 比较 `Minecraft#level` 身份（顺带覆盖了原版漏掉的主世界→下界→主世界切换） |
| `client/weather/SRPBlizzardDirectionClient.java`(155) | `client/weather/SRPBlizzardDirectionClient.java` | 完整移植 | 动量/刹车状态机逐常量照搬（0.065 减速、0.045 加速、0.075/0.125 黑度、`holdTicks < 8`、`blackBlend >= 0.999` 就绪条件、`fadingBackToWhite` 分支）。仅两处换 API：tick 钩子 `ClientTickEvent(Phase.END)` → `ClientTickEvent.Post`；切换音效从 `MovingSound` 构造改为推入 `SoundManager` |
| `client/weather/SRPBlizzardFogRenderer.java`(146) | `client/weather/SRPBlizzardFogRenderer.java` | 改写移植 | 8 层壳 / 32 经度 / 14 纬度、`smoothStep`、`near 14→5.5`、`far 48→20`、`baseAlpha 0.02+0.14s`、`0.22` 上限、`0.018` 形变、颜色 `lerp(0.78→0.055 …)`、`0.78+daylight*0.22` 全部保留。渲染通道从 `Tessellator` 立即模式改为 `SubmitCustomGeometryEvent` + `RenderTypes.debugQuads()`（26.3 对应物：position+color、半透明混合、无剔除、无贴图）。几何在静态块里预计算单位球方向与两个相位，逐帧只做 1 sin + 1 cos |
| `client/weather/SRPBlizzardRenderer.java`(201) | `client/weather/SRPBlizzardRenderer.java` | 改写移植 | 风场（`0.0012` 风向角、`gust = 0.72 + 0.18sin(0.065t) + 0.1sin(0.017t)`、`(0.8+2.2i)` 强度）、车道（`i>0.7?3:2`、`i>0.55?1:2`、`randomD>0.82` 加宽）、下落相位、`hash01`（`341873128712L/132897987541L/1274126177L`）、`positiveModulo`、逐列地面裁剪、贴图 `textures/environment/snow.png`、亮度 `0.86+0.14*daylight`、alpha `i*(0.34+0.58fade)*(0.74+0.26r)` 全部保留。改为 `SubmitCustomGeometryEvent` + `RenderTypes.entityTranslucent(SNOW_TEXTURE)` |
| `client/weather/SoundBlizzardReverse.java`(36) | `client/weather/SoundBlizzardReverse.java` | 改写移植 | `MovingSound` 在 26.3 已删除，改用 `AbstractTickableSoundInstance`：`SoundSource.WEATHER`、`Attenuation.NONE`、非循环、逐 tick 贴到玩家眼睛位置、玩家不存在/死亡时 `stop()` |

### 1.2 星型 / 地形（world/star）

| 1.10.9 原文件 | 26.3 实现 | 状态 | 说明 |
| --- | --- | --- | --- |
| `world/star/SRPBlizzardDerivedHandler.java`(79) | `world/star/SRPBlizzardDerivedHandler.java` | 完整移植 | 每 10 tick（`tickCount % 10 == 0`）扫描玩家周围 100 格（`getBoundingBox().inflate(100)`，并复核 `distanceToSqr <= 10000`），命中 Kirin（或按 id 解析出的 Heblu）就翻转风雪方向，且**仅在状态变化时发包**（`LAST_STATE` 按 UUID 缓存），登出时清理。钩子 `TickEvent.PlayerTickEvent(Phase.END)` → `PlayerTickEvent.Post` |
| `world/star/GenLayerSRPDynamicStar.java`(32) | `world/star/GenLayerSRPDynamicStar.java` | **不适用（已给出替代）** | 见 §2.4。26.3 无 `GenLayer`，改为 `BiomeSource` 三路派发 + 生成期星型来源 `activeGenerationStarType(overworld)`（= 原 `SRPStarWorldEvents#getActiveStarTypeForGeneration()`），后者被 `SRPFracturedTerrainHandler` 与 `SRPBlizzardDerivedHandler` 共用 |
| `world/star/SRPStarTypeSyncHandler.java`(46) | `world/star/SRPStarTypeSyncHandler.java` | 完整移植 | login / respawn / dimension change 三个 `PlayerEvent` 子类 → `MsgSyncStarType`，星型取自 `SrpWorldData.get(overworld).starType()` |
| `world/star/SRPFracturedTerrainHandler.java`(389) | `world/star/SRPFracturedTerrainHandler.java` | 改写移植 | 哈希/噪声算术逐字照搬（`mix64` 双常量、`9172280023384029625L`、`-7046029254386353131L`、`Long.rotateLeft(...,21)`、`hashCell`、`coherentNoise` 双盐、`latticeNoise % 1000000`）。钩子 `PopulateChunkEvent.Pre(HIGHEST)` → `ChunkEvent.Load` + `isNewChunk()`；`IBlockState`/`Block` → `BlockState`；`Material` 白名单 → `BlockTags.DIRT`/`BlockTags.SAND` + 显式方块集合；区块高度图 `Chunk#getHeightmapHeight` → `Heightmap.Types.WORLD_SURFACE`；绝对 y 上下限（4 / 238 / 244 / 250）→ `level.getMinY()+4` / `getMaxY()-17` / `-11` / `-5`；收尾 `generateSkylightMap()+setLightCorrect(true)` → `markUnsaved()` + 对**实际改动过的列**调用 `getLightEngine().checkBlock(...)` |

### 1.3 网络

| 1.10.9 原文件 | 26.3 实现 | 状态 | 说明 |
| --- | --- | --- | --- |
| `network/MsgSyncBlizzardReverse.java`(39) | `network/MsgSyncBlizzardReverse.java` | 完整移植 | SimpleImpl `IMessage`/`IMessageHandler` → `CustomPacketPayload` record + `StreamCodec`；`writeBoolean/readBoolean` 不变；handler 在 `context.enqueueWork` 里调 `SRPBlizzardDirectionClient.setReverseRequested` |
| `network/MsgSyncStarType.java`(39) | `network/MsgSyncStarType.java` | 完整移植 | `writeVarInt(starType.value())`；handler 写 `celestial.client.StarWorldClientState`（与既有 `StarWorldStatePayload` 同一状态对象、同一服务器数据源 `SrpWorldData`，因此重复同步是幂等的） |

### 1.4 1.12.2 mixin / shader（**全部不适用**）

| 1.12.2 文件 | 状态 | 替代方案 / 理由 |
| --- | --- | --- |
| `mixins/MixinEntityRendererBlizzard.java` | **不适用** → 拆成 3 处 26.3 事件 | ①`EntityRenderer#renderRainSnow` 注入（画雾壳 + 雪条）→ `SubmitCustomGeometryEvent`（`SRPBlizzardFogRenderer` / `SRPBlizzardRenderer`），与工程既有 `AuroraSkyRenderer` / `CelestialSkyRenderer` 同一通道。②`renderRainSnow` 里的 `@Redirect`（关掉原版雨雪）→ 26.3 的等价物是 `CustomWeatherEffectRenderer`（`RegisterCustomEnvironmentEffectRendererEvent` + 维度环境属性 `CUSTOM_WEATHER_EFFECTS`），需要注册新的维度属性/渲染 pass，属 `registry/**` 写范围，**本批不做**：冷星会把主世界生物群系整体换成雪原/冰原（既有 `StarBiomeGenerationEvents`），原版雪与暴风雪雪条同向叠加，视觉上不冲突。③`setupFog` TAIL（`fogEnd = 72 - 56i`、`fogStart = fogEnd*0.08`、水/岩浆内跳过）→ `ViewportEvent.RenderFog` + `FogType.ATMOSPHERIC` 判断，数值与判断条件原样保留 |
| `mixins/MixinRenderGlobalBlizzardSky.java` | **不适用** → 雾色洗白 | 原 mixin 把 `WorldProvider#calcSunriseSunsetColors` 重定向为 `null`，隐藏暴风雪里的日出/日落暖色。26.3 的 `SkyRenderState.sunriseAndSunsetColor` 在任何 mod 渲染事件之前就已烘焙进 render state，公开的替换手段只有维度级 `CustomSkyboxRenderer`（同样落在 `registry/**`）。因此改用 `ViewportEvent.ComputeFogColor` 把雾色按暴风雪强度与 `blackBlend` 洗向暴风雪自己的灰白/黑，达到“地平线暖光被压掉”的同等观感 |
| `mixins/accessor/AccessorShaderGroup.java` | **不适用** | 只为 `StarWorldShaderManager` 反射取 `ShaderGroup#listShaders` 而存在；26.3 删除了 `ShaderGroup`/`Shader`/`ShaderUniform`，无替代也不需要 |
| `client/shader/star/StarWorldShaderManager.java` | **不适用** | 它做的是 `EntityRenderer#loadShader(ResourceLocation("srparasites:shaders/post/star_cold.json"))`——1.12.2 的 GLSL post-processing 管线。26.3 无此管线，后处理只能走资源包/外部 shader（Iris 等）。本工程对“冷星视觉”的既有实现是 `world/StarBiomeGenerationEvents` 的生物群系替换 + 本批的暴风雪雾/雪/雾色，不引入新的后处理注册 |
| `client/shader/BlackSkyShaderManager.java` | **不适用** | 同上，且它依赖 `net.optifine.shaders.Shaders` 反射（26.3 不存在 OptiFine）。其“黑天”视觉在 1.12.2 已经由 `dark_days` 走 `CelestialSkyRenderer`（本工程既有，`RenderGuiEvent`/`SubmitCustomGeometryEvent` 通道）覆盖 |

---

## 2. 不适用项的替代方案细节

### 2.1 渲染通道对照表

| 1.12.2 | 26.3（本批采用） | 备注 |
| --- | --- | --- |
| `Tessellator`/`BufferBuilder` 立即模式 | `SubmitCustomGeometryEvent#getSubmitNodeCollector().submitCustomGeometry(...)` | 工程既有 `AuroraSkyRenderer`/`CelestialSkyRenderer` 已验证的写法 |
| `GlStateManager.disableTexture2D + enableBlend(SRC_ALPHA, ONE_MINUS_SRC_ALPHA) + disableCull` | `RenderTypes.debugQuads()` | 等价 state：position+color、半透明、无剔除、无贴图 |
| `bindTexture(environment/snow.png)` + `POSITION_TEX_COLOR` | `RenderTypes.entityTranslucent(Identifier.withDefaultNamespace("textures/environment/snow.png"))` | 天光/覆盖由 `setLight(0xF000F0)` + `setOverlay(NO_OVERLAY)` 给常量，亮度照原样烘进顶点色 |
| `RenderGameOverlayEvent.Pre(ALL)` 全屏色块 | `RenderGuiEvent.Pre` + `GuiGraphicsExtractor#fill` | 与 `MeteorShakeClient`/`BoughClientEvents` 同通道 |
| `EntityRenderer#setupFog` 注入 | `ViewportEvent.RenderFog` | `setNearPlaneDistance`/`setFarPlaneDistance` |
| `WorldProvider#calcSunriseSunsetColors` 重定向 | `ViewportEvent.ComputeFogColor` | 见 §1.4 |
| `MovingSound` | `AbstractTickableSoundInstance` | `SoundSource.WEATHER` + `Attenuation.NONE` 语义不变 |
| `ClientTickEvent(Phase.END)` | `ClientTickEvent.Post` | — |
| `TickEvent.PlayerTickEvent(Phase.END)` | `PlayerTickEvent.Post` | — |
| `WorldEvent.Load/Unload`（客户端） | 每 tick 比对 `Minecraft#level` 身份 | 26.3 客户端无对应事件对 |
| `PopulateChunkEvent.Pre` | `ChunkEvent.Load` + `isNewChunk()` | 与工程既有 `StarBiomeGenerationEvents` 同一钩子 |

### 2.2 OptiFine 门控的替代

`SRPBlizzardFogRenderer` 原逻辑只有在 **OptiFine shader pack 激活**时才画雾壳（反射读 `net.optifine.shaders.Shaders#currentShaderName`，非空且非 `OFF`）。26.3 没有 OptiFine，因此保留原反射探针（旧环境仍生效），并追加 Iris 探针（`net.irisshaders.iris.Iris#isPackInUseQuick`）。两者都缺失时返回 `false`——也就是原版“无 shader pack 时不画雾壳”的行为。探针全部 `try/catch` 兜底，绝不因为缺少第三方 mod 而崩。

### 2.3 1.12.2 材质白名单 → 26.3 标签/方块集合

`SRPFracturedTerrainHandler` 的 `isTerrainSurface`/`canCarveTerrain` 原按 `Material`（GROUND / GRASS / SAND / CLAY / SNOW / CRAFTED_SNOW / ICE / PACKED_ICE）判断，26.3 的 `Material` 已不承载这些语义，改为：

```
BlockTags.DIRT ∪ BlockTags.SAND ∪ {GRASS_BLOCK, MYCELIUM, CLAY, GRAVEL,
    SNOW, SNOW_BLOCK, ICE, PACKED_ICE, BLUE_ICE, FROSTED_ICE}
```

并保留原排除项（`BEDROCK`、液体、`hasTileEntity` → `hasBlockEntity()`）。

### 2.4 GenLayerSRPDynamicStar

1.12.2 的 `GenLayerSRPDynamicStar` 在 `getInts(...)` 里按 `SRPStarWorldEvents.getActiveStarTypeForGeneration()` 三路派发到 `cold`/`warm`/`original` 三个子层。26.3 彻底删除 `GenLayer` 栈，生物群系由数据包 multi-noise `BiomeSource` 生成，mod 没有公开钩子按世界替换它。因此：

- **保留决策本身**：`select(starType, original, cold, warm)` 是同一个三路选择，只是参数类型从 layer 变成 `BiomeSource`；
- **保留生成期星型来源**：`activeGenerationStarType(overworld)` 读取 `SrpWorldData`，被 `SRPFracturedTerrainHandler` 与 `SRPBlizzardDerivedHandler` 共用，避免星型来源在多处重复；
- **实际换群系仍然由既有 `world/StarBiomeGenerationEvents`（`ChunkEvent.Load` + 生物群系重写）承担**，该文件属其它工作流写范围，本批不改。

### 2.5 音效：无需改动

`blizzard_reverse` 已在本工程基线中完整存在：`registry/SoundEventCatalog.java` 第 79 行、`assets/csrp/sounds.json` 的 `"blizzard_reverse" → csrp:misc/snow_reversal (stream)`、以及 `assets/csrp/sounds/misc/snow_reversal.ogg`（与 `out109/assets/srparasites/sounds/misc/snow_reversal.ogg` 同名同路径）。`ModSounds` 的静态块 `SoundEventCatalog.EVENTS.forEach(ModSounds::register)` 会注册它，因此 `ModSounds.get("blizzard_reverse")` 可用。**本批未修改** `SoundEventCatalog.java` / `sounds.json` / `sounds/**`。

---

## 3. 明确记录的 26.3 差异（有意偏离）

| # | 差异 | 原因与影响 |
| --- | --- | --- |
| D1 | 雪条按**双面**提交（每条 8 个顶点而非 4） | 1.12.2 用 `GlStateManager.disableCull()` 画雪条；本批用的 `entityTranslucent` 保留剔除，因此显式提交正反两遍。代价是顶点量翻倍，已在 `MAX_STREAKS` 预算内 |
| D2 | 每帧雪条上限 `MAX_STREAKS = 1500`、半径上限 `MAX_RADIUS = 12` | 原版在 radius=14 时不设上限，最坏情况每帧数万四边形。26.3 的自定义几何走 submit 缓冲，故设上限并在达到上限时停止继续扫描；观感为“密度封顶”而非“突发卡顿” |
| D3 | 地面裁剪高度图用 `Heightmap.Types.WORLD_SURFACE`（列扫描）/ `MOTION_BLOCKING`（雪条裁剪） | 原 `World#getPrecipitationHeight` 在 26.3 客户端不存在（仅服务端 `WeatherEffectRenderer` 用 `MOTION_BLOCKING`）。`SRPFracturedTerrainHandler.findSurfaceY` 严格对应 1.12.2 的**区块自带**高度图（`WORLD_SURFACE`）；雪条裁剪对应原版天气管线自己用的 `MOTION_BLOCKING` |
| D4 | `daylight` 用 `getOverworldClockTime` + 原公式重建 | 26.3 删除 `Level#getSunBrightness(float)`，改用 `SUN_ANGLE` 环境属性的等价公式（含雨/雷衰减、`*0.8+0.2`） |
| D5 | 雾色被 `ComputeFogColor` 主动洗白/洗黑 | 原 1.12.2 **没有**雾色改写；这是 §1.4 里替代 `MixinRenderGlobalBlizzardSky` 的代价，强度用 `smoothStep(intensity)` 插值，`intensity<=0.001` 时完全不生效 |
| D6 | `getProfile` 中 `field_150432_aD`/`field_150403_cj` 判为冰对（`ICE`/`PACKED_ICE`），`field_150433_aE` 判为雪方块（与 `Blocks.SNOW` 合并到同一分支） | 这两个 1.12.2 混淆字段的确切身份无法从原始反编译产物直接读出；取“与本工程既有雪/冰映射一致”（`block/ParasiteRubbleBlock`、`HarlequinnGrassBlock` 均为 `SNOW || SNOW_BLOCK`）且“在只用于冷星的表里语义成立”的解释。仅影响该默认关闭功能的填充方块选择 |
| D7 | 碎裂地形在 `ChunkEvent.Load`（FULL 状态，光照已算完）改写方块 | 1.12.2 的 `PopulateChunkEvent.Pre` 在光照之前。26.3 里该钩子是工程既有的生成后改写点；为弥补光照，改动过的列会补一次 `LightEngine#checkBlock`，`LevelChunk#setBlockState` 本身也会更新高度图并在区段空/非空变化时刷新光照 |
| D8 | `SRPStarTypeSyncHandler` 与既有 `SrpStarWorldEvents` 都会发星型同步 | 前者是 1.10.9 的原语义（login/respawn/dimension），后者是工程基线（login/dimension）。两者写同一个 `StarWorldClientState`、读同一个 `SrpWorldData`，重复包幂等；本批顺带补齐了基线缺的 **respawn** 同步 |
| D9 | Heblu 通过注册表 id `csrp:heblu` 解析 | 本工程尚未移植 `EntityHeblu`（`registry/ModEntities.java` 里只有 `heblu_light` 投射物），`entity/**` 属其它工作流写范围。解析失败时该分支静默为 `false`，Kirin 分支照常工作；Heblu 一旦落地即自动生效 |

---

## 4. 交付物清单

### 4.1 新增 Java（13）

```
src/main/java/alku/csrp/client/weather/SRPBlizzardClient.java
src/main/java/alku/csrp/client/weather/SRPBlizzardClientEvents.java
src/main/java/alku/csrp/client/weather/SRPBlizzardDirectionClient.java
src/main/java/alku/csrp/client/weather/SRPBlizzardFogRenderer.java
src/main/java/alku/csrp/client/weather/SRPBlizzardRenderer.java
src/main/java/alku/csrp/client/weather/SoundBlizzardReverse.java
src/main/java/alku/csrp/network/MsgSyncBlizzardReverse.java
src/main/java/alku/csrp/network/MsgSyncStarType.java
src/main/java/alku/csrp/world/star/SrpStarPayloads.java
src/main/java/alku/csrp/world/star/GenLayerSRPDynamicStar.java
src/main/java/alku/csrp/world/star/SRPBlizzardDerivedHandler.java
src/main/java/alku/csrp/world/star/SRPStarTypeSyncHandler.java
src/main/java/alku/csrp/world/star/SRPFracturedTerrainHandler.java
```

`SrpStarPayloads` 承担两个 payload 的 `RegisterPayloadHandlersEvent` 注册（`registrar("1")`，`playToClient` ×2）。放在 `world/star/` 是因为工程既有的注册集中点 `compendium/network/CompendiumPayloads.java` 属于其它工作流的写范围；NeoForge 允许同一 mod 多个 `@EventBusSubscriber` 处理该事件，且 `registrar(String)` 每次返回独立 registrar，因此与 `"1"` 版本号不冲突。

### 4.2 新增校验脚本（3）

| 脚本 | 断言重点 |
| --- | --- |
| `scripts/verify-blizzard-client-state.cjs` | 冷星判定、雨强度、daylight 公式、方向状态机全部常量、`AbstractTickableSoundInstance` 音频、GUI 叠加色值与 alpha、两个 payload 编解码与共享状态源、`blizzard_reverse` 音效三处资产 |
| `scripts/verify-blizzard-render.cjs` | 雾壳分段/半径/颜色/形变/Shader 探针（OptiFine+Iris）、无 `ShaderGroup` 残留、`setupFog` 数值→`RenderFog`、`ComputeFogColor` 三通道、雪条风场/车道/哈希/双面提交（恰好 8 次顶点调用）、贴图与高度图 |
| `scripts/verify-blizzard-star-terrain.cjs` | 无 `GenLayer` 残留 + `BiomeSource` 三路派发、login/respawn/dimension 同步、100 格/10 tick 派生反转、`ChunkEvent.Load`+`isNewChunk`+`HIGHEST`、默认关闭三处证据（`SrpWorldData` 需 `Boolean.TRUE.equals`、创建界面默认 `OFF`、`SrpColdStarSelection`）、生成器全部常量、26.3 区块访问、代码中无 `Material`/`field_150*`/`func_*`（注释中的出处说明除外） |

### 4.3 验证记录

```
export JAVA_HOME="D:/MC/jdk/graalvm-community-25.3.4.1+1.1"
./gradlew.bat build -x test --console=plain      → BUILD SUCCESSFUL (1m 29s)
node scripts/run-all-verifications.cjs           → {"total":109,"passed":109,"failed":0}
```

基线 92/92 的 92 项全部通过，无新增失败；总数由 92 上升到 109（本批 +3，其余为同期其它工作流新增）。

---

## 5. 遗留项 / 已知限制

1. **未做运行期客户端验证**：本批没有执行 `runClient`（无图形环境），雪条/雾壳/雾色的最终观感、以及 `SubmitCustomGeometryEvent` 提交量对帧率的影响需要在真实客户端确认；`MAX_STREAKS`/`MAX_RADIUS` 是当前的上限护栏，必要时可调。
2. **原版雨雪未关闭**：见 §1.4②，要用 `CustomWeatherEffectRenderer` 才能真正替换原版天气渲染，需要 `registry/**`（维度环境属性注册）写权限，留给后续批次。
3. **Heblu 缺席**：见 D9，`SRPBlizzardDerivedHandler` 的 Heblu 分支目前恒为 `false`。
4. **碎裂地形光照**：见 D7，`checkBlock` 只对改动过的列顶端触发，理论上极端地形（深谷底部）仍可能有局部光照延迟到邻近方块更新时修正。
5. **`field_150432_aD`/`field_150403_cj`/`field_150433_aE` 身份**：见 D6，若后续能取到 1.12.2 MCP 映射表，应回头核对 `getProfile` 的冰/雪分支。
6. **未 push**：两个 commit（`bfd9ef75`、`300578d3`）只在本地 `port/neoforge-26.3`，按约定由 Lead 统一推送。
