# PLAN.md — SRParasites 1.10.8 → 1.10.9 内容移植到 MC 1.20.1 Forge 47.4.23

> **API 结论的核实等级**（本文件严格遵守）：
> - **【核实】** = 已用 `javap` / Forge sources jar / ForgeFlower 反编译**本地真机核实**，或来自本工程**已编译通过的源码**，或来自已逐文件读过的 1.10.9 原始源码。
> - **`[待查证]`** = 本会话无法核实，必须由 B 阶段先落实再落码，**禁止凭记忆补全**。
>
> 核实素材与产物：官方映射 ground truth jar `forge-1.20.1-47.4.23_mapped_official_1.20.1.jar`（8614 类）；
> 详细核实报告：`C:\Users\P傲娇34\AppData\Local\Temp\verify1201\REPORT.md`；反编译产物：同目录 `dec/`。

---

## 0. 五行摘要（供主代理直接汇报）

1. **平台与版本**：Minecraft 1.20.1 + Forge 47.4.23（ForgeGradle 6.x / Gradle 8.8 / official 映射 / Java 17 字节码、GraalVM 21 编译）——现代 1.18.2–1.20.4 时代，metadata 在 `src/main/templates/META-INF/mods.toml`（构建期展开）。
2. **文件布局概览**：新增约 21 个 Java 类落在 `alku/csrp/{block,client/weather,client,world,network}`；新增 43 个资源落到 `assets/csrp/{blockstates,models/block,models/item,textures/block,sounds/misc}` + `data/csrp/structures/`；改动 14 个既有文件（`gradle.properties`、`ModBlocks`、`ModItems`、`ModSounds`、`CsrpNetwork`、`StarWorldClientState`、`SrpWorldData`、`SrpStarWorldEvents`、`StarBiomeGenerationEvents`、`MeteorStructureLoader`、`sounds.json`、`lang/*.json`、`SrpDifficultyScreenEvents`、可选 `csrp.mixins.json`）。
3. **B 阶段骨架范围**：5 个新方块 + 4 个 BlockItem + 1 个 SoundEvent 的注册骨架、`SrpWorldData` 新增两个持久化布尔字段、`gradle.properties` 版本号 1.10.8→1.10.9、43 个资产与语言键——此片必须能独立 `build` 通过。
4. **C 阶段功能分片**：①资源导入 ②冷星生态（死头树 NBT + 雪草 + 碎裂地形）③暴风雪客户端渲染（**零新增 mixin**，改用自定义 `DimensionSpecialEffects` + 2 个 Forge 事件）④网络同步 ⑤（可选）世界创建 UI 开关。
5. **最大风险（已大幅降低）**：原本预估的 3 条 mixin **全部被真机核实为「1.20.1 不存在对应 API」**——`EntityRenderer` / `RenderGlobal` / `ShaderGroup` 在 1.20.1 均已移除。新的落地路径已查实：**自定义 `DimensionSpecialEffects` 子类（经 `RegisterDimensionSpecialEffectsEvent` 注册，同一类里同时接管 `getSunriseColor()` 返回 `null` 与 `renderSnowAndRain()` 委托）** + `ViewportEvent.RenderFog`/`ComputeFogColor` 事件 + **可选兜底的一条 `LevelRenderer.renderSnowAndRain` mixin（描述符已核实为 `(Lnet/minecraft/client/renderer/LightTexture;FDDD)V`）**；Shader uniform 仍走工程既有的 `StarWorldShaderEvents` 反射（`PostChain.passes` 已核实为 private 无 getter）。

---

## 1. 目标与成功标准

### 1.1 目标
把 SRParasites **1.10.9 相对 1.10.8 的全部新增内容**移植进现有 `D:\code\MC模组\csrp-1.20.1-forge`（1.20.1 Forge 工程）：

- 新增 24 个 Java 类中，约 **21 个需新建/等价重写**、3 个由既有等价物承担、1 个（`SRPCoreMod`）因 1.20.1 无 `IFMLLoadingPlugin` 而不移植；
- 删除 1 个（1.10.9 移除 `SRParasites/Tags.java`）——本工程无对应物，**无需动作**；
- 新增 43 个资产（blockstates / models / textures / sounds / structures）；
- 删除 7 个资产（`textures/blocks/parasitebush_frostg*.png`）——**本工程 `textures/` 下无同名文件（已 grep 确认），无需动作**；
- 版本号 `mod_version` 1.10.8 → 1.10.9；
- 命名空间全部由 `srparasites:` 迁移到 `csrp:`，纹理目录由 `textures/blocks/` 迁到 `textures/block/`。

### 1.2 成功标准（可机械判定）
| # | 判据 |
|---|---|
| S1 | `gradlew.bat build` 全绿，产出 `build/libs/csrp-1.10.9.jar` |
| S2 | jar 内 `assets/csrp/blockstates/{deadhead_grass_short,deadhead_grass_tall,snow_covered_grass,snow_short_grass,snow_tall_grass}.json` 与 `data/csrp/structures/deadhead_tree_large_1..4.nbt` 存在 |
| S3 | jar 内 `sounds.json` 存在 `blizzard_reverse` 条目，`sounds/misc/snow_reversal.ogg` 存在 |
| S4 | jar 内 `csrp.mixins.json` 与改动前一致（**走推荐方案则零新增 mixin**）；若走兜底方案则 `client` 数组含 `client.LevelRendererBlizzardMixin` |
| S5 | `gradlew.bat runClient` 起得来，冷星世界不抛 `NoSuchMethodError` / `NoSuchFieldError` / `ClassNotFoundException`；日志出现自定义维度特效的注册确认 |
| S6 | 冷星（`SrpStarType.COLD`）世界：玩家上方出现与原版一致的斜向雪丝；日出日落时分天空无橙红渐变；切到冷星后重启客户端仍保持 |
| S7 | `grep -rn "srparasites:" src/main/resources` 在 `assets/csrp/**` 与 `data/csrp/**` 下为 **0 命中** |
| S8 | jar 内 `assets/csrp/lang/en_us.json` 含 4 个新方块名键 + 4 组世界设置键 |
| S9 | **回归**：非冷星（NORMAL/WARM）世界的天空、云、地形雾、雨雪渲染与改动前**视觉一致** |

---

## 2. 平台 / 版本 / 时代判定

| 项 | 值 | 依据 |
|---|---|---|
| MC | 1.20.1 | `gradle.properties: minecraft_version=1.20.1` |
| Forge | 47.4.23 | `gradle.properties: forge_version=47.4.23` |
| ForgeGradle | `net.minecraftforge.gradle` version `[6.0,6.2)` | `build.gradle:5` |
| Gradle wrapper | 8.8 | `gradle/wrapper/gradle-wrapper.properties:3` |
| 映射 | `official` / `1.20.1`（Mojang 官方映射） | `gradle.properties:18-19` |
| Java | toolchain 21 + `options.release = 17` | `build.gradle:20,24-26`（GraalVM 21 编译器出 Java 17 字节码） |
| 时代 | **现代 1.18.2–1.20.4** | ForgeGradle 6 + 官方映射 + `META-INF/mods.toml` |
| metadata | `src/main/templates/META-INF/mods.toml`（`mod_version` 经 `generateModMetadata` 展开） | `build.gradle:103-127` |
| 源集 | 单模块，无 fabric/neoforge 子项目 | `build.gradle` 只声明 `java-library` + `maven-publish` + `idea` |

**结论**：无需更换工具链、无需改 Gradle 版本、无需改映射通道；本阶段只做内容移植与版本号提升。

---

## 3. 差异全清单

### 3.1 新增 Java 类（24 个，逐个一行）

> 目标包一律 `alku/csrp/...`（`mod_group_id=alku`，`Csrp.MODID="csrp"`）。
> 「映射要点」列为 1.12.2(MCP) → 1.20.1(official) 的关键 API 替换；1.20.1 侧签名凡标 **【核实】** 者均已真机核实。

| # | 1.10.9 原始类 | 职责 | 1.12.2 → 1.20.1 映射要点 | 目标路径 |
|---|---|---|---|---|
| 1 | `SRPCoreMod` | 1.12.2 的 `IFMLLoadingPlugin`，`MixinBootstrap.init()` + `MixinExtrasBootstrap.init()` + `Mixins.addConfiguration("mixins.srparasites.json")` | **1.20.1 无 IFMLLoadingPlugin 概念**；mixin 配置改由 jar manifest 的 `MixinConfigs`（**工程已有**，`build.gradle:138`）+ `src/main/resources/csrp.mixins.json` 承担 → **本类不移植**；1.10.9 依赖的 `MixinExtras` 本工程**未引入**，故新 mixin 一律禁用 MixinExtras 注解（见 R2） | **不移植** |
| 2 | `block/BlockDeadheadGrassShort` | 死头短藤蔓，5 种纹理按坐标哈希选；只能长在 DEADHEAD 变体树干或死头树叶上；可剪切、无掉落 | `BlockBush`→`BushBlock`（`canSurvive(BlockState, LevelReader, BlockPos)`、`mayPlaceOn(BlockState, BlockGetter, BlockPos)` 均为 1.20.1 名 **【核实】**）；`PropertyInteger("texture",0,4)`→`IntegerProperty.create("texture",0,4)`（`BlockStateProperties` 无此项，自定义）；1.20.1 无 blockstate「动态属性」→ 改为**放置时按坐标哈希固化纹理**（`getStateForPlacement`）；`getBoundingBox`→`getShape(BlockState, BlockGetter, BlockPos, CollisionContext)` **【核实】**；`IShearable`→`net.minecraftforge.common.IForgeShearable`（**`TallGrassBlock` 在 1.20.1 继承了它，可直接照抄其方法签名【核实】**）；`BlockRenderLayer.CUTOUT`→模型 json 顶层 `"render_type": "cutout"` **【核实】**；`setHardness(0.0F)`→`Properties.instabreak()` **【核实】** | `alku/csrp/block/DeadheadGrassShortBlock.java` |
| 3 | `block/BlockDeadheadGrassTall` | 死头高藤蔓，PART=TOP/BOTTOM 双格；顶上放下半 | `BlockBush`→`BushBlock`；`PropertyEnum<EnumPart>`→**用 `BlockStateProperties.DOUBLE_BLOCK_HALF`（`EnumProperty<DoubleBlockHalf>`，取值 TOP/BOTTOM）【核实】**；放置上半时用 `setPlacedBy(Level, BlockPos, BlockState, LivingEntity, ItemStack)` **【核实】** 补下半；破坏联动直接照 `DoublePlantBlock` 的 `playerWillDestroy(Level, BlockPos, BlockState, Player)` + `protected static void preventCreativeDropFromBottomPart(Level, BlockPos, BlockState, Player)` **【核实】**；**不继承 `DoublePlantBlock`**（它会带入 `WATERLOGGED`，与原方块语义不符） | `alku/csrp/block/DeadheadGrassTallBlock.java` |
| 4 | `block/BlockSnowCoveredGrass` | 雪覆盖草方块：恒 `snowy=true`；上方无雪草时回退为草方块；挖掉落泥、pick block 给草方块 | `BlockGrass`→`net.minecraft.world.level.block.GrassBlock`；**注意 `GrassBlock extends SpreadingSnowyDirtBlock`，而 `SnowyDirtBlock.SNOWY` 是 `public static final BooleanProperty`【核实】→ `SNOWY` 可直接使用**；`getActualState` 恒 snowy → 覆写 `getStateForPlacement` 返回 `defaultBlockState().setValue(SnowyDirtBlock.SNOWY, true)`；`updateTick`→`randomTick(BlockState, ServerLevel, BlockPos, RandomSource)`；`neighborChanged`→`neighborChanged(BlockState, Level, BlockPos, Block, BlockPos, boolean)`；`getDrops`→可覆写 `playerDestroy`；`getPickBlock`→`getCloneItemStack`；**不注册 BlockItem**（与 1.10.9 一致）；其 blockstate 引用原版模型 `minecraft:grass_snowed`（1.12.2 名）→ 1.20.1 用 `minecraft:block/grass_block_snow`（`[待查证]` T22） | `alku/csrp/block/SnowCoveredGrassBlock.java` |
| 5 | `block/BlockSnowGrass` | 一个类带 `tallGrass` 开关，注册成 `snow_short_grass` / `snow_tall_grass` 两个方块：放置时把下方草方块换成雪草，破坏时还原；可剪切；1/8 概率掉草种子；`isPassable` = true | 拆为**基类 + 2 子类**：`SnowGrassBlock extends BushBlock` + `SnowShortGrassBlock`（`getShape` 返回 `(0.1,0,0.1,0.9,1,0.9)`）+ `SnowTallGrassBlock`（`(0.1,0,0.1,0.9,2,0.9)`）；`onBlockAdded`/`breakBlock`→`setPlacedBy`（下方草→雪草）+ `playerWillDestroy`/`onRemove`（还原）；`getBoundingBox`/`getCollisionBoundingBox`→`getShape()`/`getCollisionShape()` **【核实】**；`getDrops`→`level.random.nextInt(8)==0` 掉 `Items.WHEAT_SEEDS`（1.20.1 无 `ForgeHooks.getGrassSeed`，行为近似 → UNVERIFIED）；`isShearable`/`onSheared`→`IForgeShearable` **【核实：`TallGrassBlock` 就是这么做，签名可照抄】**；`canPlaceBlockAt`→`canSurvive` | `alku/csrp/block/SnowGrassBlock.java` + `SnowShortGrassBlock.java` + `SnowTallGrassBlock.java` |
| 6 | `client/weather/SRPBlizzardClient` | 客户端暴风雪强度：仅主世界 + 客户端星类型 COLD 时返回 `MathHelper.clamp(level.getRainLevel(partialTicks), 0, 1)` | `Minecraft.field_71441_e`→`Minecraft.getInstance().level`；`level.dimension()==Level.OVERWORLD`（**工程已有同款判定**【核实】`StarWorldShaderEvents.java:100`）；**`getRainLevel(float)` 在 `net.minecraft.world.level.Level` 上【核实】**（`ClientLevel` 未声明，继承可用）；星类型改读 `StarWorldClientState.starType()` | `alku/csrp/client/weather/BlizzardClient.java` |
| 7 | `client/weather/SRPBlizzardClientEvents` | ①世界加载/卸载清客户端星类型与方向状态；②HUD 最上层叠 205/215/224 雪雾（`alpha = clamp(intensity*0.24, 0, 0.24)`） | ①`WorldEvent.Load/Unload`→`LevelEvent.Load/Unload`（**`ChunkEvent extends LevelEvent`【核实】**，`Load/Unload` 同族）；②`RenderGameOverlayEvent.Pre(ElementType.ALL)`→`RenderGuiEvent.Post`（`getGuiGraphics()` / `getPartialTick()` **【核实】**）+ `GuiGraphics.fill(int,int,int,int,int)` **【核实】**（工程 `MeteorClientEvents.java:48-58` 同法） | `alku/csrp/client/weather/BlizzardClientEvents.java` |
| 8 | `client/weather/SRPBlizzardDirectionClient` | 方向状态机：`setReverseRequested` → 刹车（每 tick `|motion| -= 0.065`，反向时 `blackBlend += 0.075`）→ 归零后等 `blackBlend≥0.999` → 保持 8 tick → 换向 → 回白（`-0.125`）；非刹车时 `motion` 每 tick ±0.045 逼近 ±1、`blackBlend` 每 tick ±0.08；驱动 `motionPhase`；切向时播音效 | `TickEvent.ClientTickEvent` + `Phase.END`【核实】（工程 `MeteorClientEvents.java:35`）；`SoundEvent.REGISTRY.getObject(rl)`→**改读 `ModSounds.BLIZZARD_REVERSE.get()`**（1.20.1 无该静态注册表【核实】）；`MovingSound`→`net.minecraft.client.resources.sounds.AbstractTickableSoundInstance`（`[待查证]` T21）；`Minecraft.getSoundManager().play(...)` 保留；全部常数（0.065 / 0.075 / 0.125 / 8 / 0.08 / 0.045）**逐字保留** | `alku/csrp/client/weather/BlizzardDirectionClient.java` |
| 9 | `client/weather/SRPBlizzardFogRenderer` | 仅当 OptiFine 光影包激活时，画 8 层带正弦扰动的球壳雪雾（近半径 lerp(14→5.5)、远 lerp(48→20)、alpha `0.02+strength*0.14` 上限 0.22、颜色随 `blackBlend` 由 (0.78,0.82,0.86) 到 (0.055,0.06,0.07)，再乘 `0.78+daylight*0.22`） | **去掉 OptiFine 判定**（1.20.1 用 Forge 自带 shader，`Class.forName("net.optifine.shaders.Shaders")` 恒失败 → 原逻辑永不执行）：改为在 `RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS` 绘制（**Stage 枚举完整列表已核实，见 §4.2**）；`GlStateManager` 静态调用→`RenderSystem`；`Tessellator.func_178181_a()`→`Tesselator.getInstance()`；`DefaultVertexFormats.field_181706_f`(POSITION_COLOR)→`DefaultVertexFormat.POSITION_COLOR`；`buffer.func_181668_a(7, fmt)`→`buffer.begin(VertexFormat.Mode.QUADS, fmt)`；`func_181662_b`→`vertex`、`func_181666_a`→`color`、`func_181675_d`→`endVertex`、`tessellator.func_78381_a()`→`BufferUploader.drawWithShader(buffer.end())`【核实：`AuroraSkyRenderer.java:164-185`】；几何（14 纬度段 / 32 经度段 / 8 层 / `deformation = 1 + sin(lon*3+t*0.01+shell*1.73)*cos(lat*4-t*0.006+shell*0.91)*0.018`）**逐字保留** | `alku/csrp/client/weather/BlizzardFogRenderer.java` |
| 10 | `client/weather/SRPBlizzardRenderer` | 核心雪丝渲染：半径 `8 + floor(intensity*6)` 的方形天气格；每格用降水表面高度定位；按距离衰减；`laneCount = intensity>0.7?3:2`；`baseSpacing = intensity>0.55?1:2`；风角 `time*0.0012 + sin(time*0.00037)*0.45`；阵风 `0.72+sin(t*0.065)*0.18+sin(t*0.017)*0.1`；风强 `(0.8+intensity*2.2)*gust`；每格 1~3 条 lane、每条 lane 竖 1.8~3.9 高的条带、按 `hash01(x,z,salt)` 决定跳过/间距/相位；`blackBlend` 压黑 | API 替换同 #9；纹理改用自带 `assets/csrp/textures/environment/snow.png`（**原版路径 `textures/environment/snow.png` 在 1.20.1 仍然存在【核实：`LevelRenderer.renderSnowAndRain` 内 `SNOW_LOCATION = "textures/environment/snow.png"`】**，但自带一份可避免受其他 mod 影响）；`world.getPrecipitationHeight(pos)`→`level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos)`（**`getHeightmapPos` 是 `LevelReader` 的 default 方法【核实】**，`Heightmap.Types.MOTION_BLOCKING` 存在【核实】）；`world.getTotalWorldTime()`→`level.getGameTime()`；相机三插值坐标改用 `Minecraft.getInstance().gameRenderer.getMainCamera()` 或玩家 `xo/yo/zo`（`[待查证]` T12）；`DefaultVertexFormats.field_181709_i`(POSITION_TEX_COLOR)→`DefaultVertexFormat.POSITION_COLOR_TEX`，**1.20.1 顶点字段顺序为 POSITION→COLOR→TEX**（与 1.12.2 的 POSITION→TEX→COLOR 不同，须按 `.vertex().color().uv().endVertex()` 调用，`[待查证]` T13）；`buffer.func_187315_a(u,v)`→`uv(u,v)` | `alku/csrp/client/weather/BlizzardRenderer.java` |
| 11 | `client/weather/SoundBlizzardReverse` | 跟随玩家的 `WEATHER` 声道循环声，无衰减、音量/音调 1.0 | `MovingSound`→`AbstractTickableSoundInstance`（`super(SoundEvent, SoundSource.WEATHER, SoundSource.WEATHER.getRandom())`；`AttenuationType.NONE`→`SoundInstance.Attenuation.NONE`）；`tick()` 同步玩家眼位；`donePlaying` 标志→`stop()`（`[待查证]` T21） | `alku/csrp/client/weather/BlizzardReverseSound.java` |
| 12 | `client/world/SRPClientStarWorldState` | 客户端星类型 `-1 未知 / 0 普通 / 1 冷 / 2 暖`，含 `isKnown/isCold/isWarm/reset` | **与既有 `alku/csrp/celestial/client/StarWorldClientState` 合并，不新开一套**：`SrpStarType` 已有 `NORMAL(0)/COLD(1)/WARM(2)`，`byValue` 兜底 `NORMAL`（「未知==普通」，接受差异，见 R6）；给既有类**只加 2 个方法** `isCold()`/`isWarm()`（`clear()` 已有） | 扩展 `alku/csrp/celestial/client/StarWorldClientState.java`（**不新建**） |
| 13 | `mixins/MixinEntityRendererBlizzard` | 三条注入：`@Inject(RETURN, renderRainSnow(F)V)` 画暴风雪；`@Inject(TAIL, setupFog(IF)V)` 覆写线性雾 `72 → 72−56·i`、`near = far*0.08`（水下/岩浆下不改）；`@Redirect(renderRainSnow, Biome.canRain()/getEnableSnow())` 关原版雨雪 | **1.20.1 已无 `EntityRenderer`**。【核实】`LevelRenderer` 有 `private void renderSnowAndRain(LightTexture, float, double, double, double)`（描述符 `(Lnet/minecraft/client/renderer/LightTexture;FDDD)V`），体首 `getRainLevel(partialTick) <= 0` 直接 return，整个体被 `if (!level.effects().renderSnowAndRain(...))` 守卫。→ **推荐零 mixin**：自定义 `DimensionSpecialEffects` 覆写 `renderSnowAndRain` 直接接管（见 §4.1.1 方案 A）；**兜底**才用 1 条 mixin | `alku/csrp/client/BlizzardDimensionEffects.java`（**新形态**）；兜底 `alku/csrp/mixin/client/LevelRendererBlizzardMixin.java` |
| 14 | `mixins/MixinRenderGlobalBlizzardSky` | `@Redirect(RenderGlobal.renderSky, WorldProvider.calcSunriseSunsetColors)` → 暴风雪时返回 `null` 以隐藏日出霞光 | **1.20.1 已无 `RenderGlobal` / `WorldProvider.calcSunriseSunsetColors`**。【核实】等价物是 `net.minecraft.client.renderer.DimensionSpecialEffects#getSunriseColor(float timeOfDay, float partialTick)`（`@Nullable public float[]`），其返回 `null` 正是隐藏日出霞光的唯一途径（`FogRenderer.setupColor` 是唯一消费者）。**Forge 全库无任何 sunrise 钩子（已全文扫描 `(?i)sunrisecolor|sunset|getSunriseColor` = 0 命中）** → 改为**注册自定义 `DimensionSpecialEffects` 子类**覆写该方法（**零 mixin**，见 §4.1.2） | `alku/csrp/client/BlizzardDimensionEffects.java`（**新形态，非 mixin**） |
| 15 | `mixins/accessor/AccessorShaderGroup` | `@Accessor("listShaders") List<Shader> getShaders()` on `ShaderGroup` | 1.20.1 **`net.minecraft.client.shader.ShaderGroup` / `Shader` 类已不存在**。【核实】等价物 `PostChain.passes` 是 **`private final List<PostPass>`、且 `PostChain` 无 getter（也无 `getEffect()`，它在 `PostPass` 上）**；`PostPass#getEffect()` 是 **public**；`EffectInstance#safeGetUniform(String)` 返回 `com.mojang.blaze3d.shaders.AbstractUniform`；`Uniform#set(float)` 是 `public final void`。【核实】工程 `StarWorldShaderEvents.java:42-174` 已用反射（候选名 `{"passes","f_110009_","e"}` + `trySetAccessible()` + 降级）实现同一件事 | 复用 `alku/csrp/celestial/client/StarWorldShaderEvents.java`（**不新建**）；可选 `alku/csrp/mixin/client/PostChainAccessorMixin.java` |
| 16 | `network/MsgSyncBlizzardReverse` | S2C：同步「暴风雪是否反向」布尔 | `IMessage/IMessageHandler`→1.20.1 `SimpleChannel` + record【核实范式 `CsrpNetwork.java:32-58` + `StarWorldStatePayload.java:11-22`】；`ByteBuf`→`FriendlyByteBuf`；`addScheduledTask`→`ctx.get().enqueueWork(...)` + `setPacketHandled(true)` | `alku/csrp/network/BlizzardReversePayload.java` |
| 17 | `network/MsgSyncStarType` | S2C：同步星类型 int | **与既有 `StarWorldStatePayload` 完全重复** → 不新建（见 §5.4） | 扩展 `alku/csrp/celestial/network/StarWorldStatePayload.java`（**不新建**） |
| 18 | `util/handlers/SnowGrassHandler` | ①`BlockEvent.PlaceEvent`：玩家在草/双高草上放雪 → 换成雪草；②`WorldTickEvent`：下雨时每 10 tick 绕玩家 32 格随机 96 次，把可积雪处（上方可见天空 + 生物群系温度 ≤0.15 + 方块光照 <10 + 该处可雨雪）的草换成雪草 | ①`BlockEvent.PlaceEvent`→`BlockEvent.EntityPlaceEvent`；**【核实】它确实暴露被替换前状态**：`event.getBlockSnapshot().getReplacedBlock()` 是旧状态、`.getCurrentBlock()` 是新状态（**`BlockSnapshot.getState()` 在 Forge 47.4.23 不存在，不要用**）；构造 `EntityPlaceEvent(BlockSnapshot, BlockState placedAgainst, @Nullable Entity)`，`@Cancelable`；②`WorldTickEvent`→`TickEvent.LevelTickEvent`（`public final Level level;` + `Phase` START/END【核实】）；`world.rand`→`level.random`；`Level#isRaining()` **【核实，在 `Level` 上】**；`world.getBiome(pos).getTemperature(pos)`→`level.getBiome(pos).value().getBaseTemperature()`（1.20.1 无逐位置温度，`[待查证]` T6）；`world.getLightFor(EnumSkyBlock.BLOCK, pos)`→`level.getBrightness(LightLayer.BLOCK, pos)`；`world.canBlockSeeSky(pos)`→`level.canSeeSky(pos)`（`[待查证]` T15）；`world.provider.canDoRainSnowIce`→`level.getBiome(pos).value().getPrecipitationAt(pos) != Biome.Precipitation.NONE`【核实 `getPrecipitationAt` + `Biome.Precipitation`，工程 `AuroraSkyRenderer.java:67-69` 也在用】 | `alku/csrp/world/SnowGrassEvents.java` |
| 19 | `world/gen/feature/WorldGenDeadheadTreeStructure` | 从 `TemplateManager` 取 `srparasites:deadhead_tree_large_1..4`（锚点 `(4,0,5)/(5,0,5)/(4,0,5)/(6,0,6)`），随机旋转（`Mirror.NONE` + 4 种 `Rotation`），保留范围内冰/浮冰/蓝冰，蘑菇模式下若落点是死头树叶则「接树」，根模式在最低树干层向下长根（≤48 深，22% 横向偏一格），垂直深度 >5 时 28% 概率挂死头树叶 | `WorldGenerator`→普通工具类；`TemplateManager.getTemplate`→**复用 `MeteorStructureLoader` 的资源加载路径**【核实：`MeteorStructureLoader.java:52-81`，`StructureTemplate.load(HolderGetter<Block>, CompoundTag)`，`placeInWorld(ServerLevelAccessor, BlockPos, BlockPos, StructurePlaceSettings, RandomSource, int)`】；`PlacementSettings`→`StructurePlaceSettings`（`setMirror`/`setRotation`/`setIgnoreEntities`/`setKeepLiquids`/`setRandom`）；`Template.func_186266_a`（旋转锚点）→ 1.20.1 **无公开等价方法**，自行按 `Rotation` 旋转 `BlockPos`（`[待查证]` T16）；`world.isAreaLoaded`→`level.hasChunksAt(min,max)` | `alku/csrp/world/DeadheadTreePlacer.java` |
| 20 | `world/star/GenLayerSRPDynamicStar` | 1.12.2 的 `GenLayer` 包裹层，按当前星类型把生物群系层切到 cold/warm 变体 | **不要移植**：1.20.1 生物群系由 `MultiNoiseBiomeSource`/`NoiseBasedChunkGenerator` 生成，`GenLayer` 体系整体移除。工程已有等价替换 `StarBiomeGenerationEvents`【核实：`ChunkEvent.Load` + `isNewChunk` 逐 section 重映射生物群系，`StarBiomeGenerationEvents.java:42-104`】 | **不移植**（复用既有类） |
| 21 | `world/star/SRPBlizzardDerivedHandler` | Server：每 10 tick 检查玩家 100 格内是否有 Heblu 或 Kirin，状态变化时 S2C 发 `MsgSyncBlizzardReverse`；登出移除缓存 | `EntityHeblu`(1.10.9)→工程 `DraconiteEntity`【核实：`ModelTabula_draconite.java:9` 注释「Direct Citadel port of SRParasites 1.10.8's Tabula-exported ModelHeblu」】；`EntityKirin`→`KirinEntity`【核实存在，`KirinEntity.java:55`】；`getEntitiesWithinAABB(Class, AABB)`→`level.getEntitiesOfClass(Class, AABB)`；`getDistanceSq`→`distanceToSqr`；星类型判定→`SrpWorldData.get(level).starType() != SrpStarType.COLD`；`ticksExisted % 10`→`tickCount % 10`；`PlayerLoggedOutEvent`→`PlayerEvent.PlayerLoggedOutEvent`（`[待查证]` T17）；`TickEvent.PlayerTickEvent`（`[待查证]` T17 —— 若已移除，则改用 `LevelTickEvent` 遍历 `level.players()`） | `alku/csrp/world/BlizzardDerivedHandler.java` |
| 22 | `world/star/SRPColdStarTreeHandler` | Server：`DecorateBiomeEvent.Decorate(TREE)`（LOWEST）上 **DENY** 原版树，按生物群系树密度在冷星世界种死头树；`mushroomTrees` 关时密度 ×0.5；树下 7 格半径铺雪并替换草/双高草 | **1.20.1 无 `DecorateBiomeEvent`**（生物群系装饰走 `BiomeGenerationSettings`/`FeatureSorter`）。改在 `ChunkEvent.Load` 的 `isNewChunk` 分支生成（与 `ColdStarVillageGenerator` 同构【核实：`StarBiomeGenerationEvents.java:55-61`】）：`level.getServer().execute(() -> ColdStarTreeHandler.decorate(level, chunkX, chunkZ))`；树密度改查 `BiomeGenerationSettings`（成本高）→ **初版用等价常量**（基础 5 棵 + 8% 概率 +1），标 UNVERIFIED（R7）；`world.getTopSolidOrLiquidBlock`→`level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos)`【核实 `getHeightmapPos` 是 `LevelReader` default + `MOTION_BLOCKING_NO_LEAVES` 存在】；`world.isAirBlock`→`level.isEmptyBlock(pos)`；`state.getMaterial().isSolid()/isLiquid()/isReplaceable()`→`state.isSolid()`/`state.liquid()`/`state.canBeReplaced()`（`[待查证]` T18）；`Block.isLeaves(state,world,pos)`→`state.is(BlockTags.LEAVES)`【核实 `BlockTags.LEAVES` 存在】；`world.setBlockState(pos,state,2)`→`level.setBlock(pos,state,2)` | `alku/csrp/world/ColdStarTreeHandler.java` |
| 23 | `world/star/SRPFracturedTerrainHandler` | Server：`PopulateChunkEvent.Pre`（HIGHEST）时对主世界冷星世界做碎裂地形——96 格板块 + ±28 抖动、板块偏移 −20..24（16% 额外 ±6..14）、表面粗糙度（32 格 broad×1.25 + 14 格 detail×0.45）、间隙 ≤4.75 时 36% 生成「碰撞齿」（rise 7..19 插值）否则凿裂缝（宽 2.25..4.0、深 20..52、38% 概率留一层平台） | **1.20.1 无 `PopulateChunkEvent`**：改在 `ChunkEvent.Load` 的 `isNewChunk` 分支、**在 `replaceBiomes` 之前**执行；`chunk.getBlockState(pos)` / `chunk.setBlockState(pos,state,false)` 可直接用；**`chunk.func_76603_b()`（重算高度图）无直接等价** → 确认 `LevelChunk#setBlockState` 是否已自动维护 `Heightmap`（`[待查证]` T5）；`Material` 判定→`BlockTags` 白名单（`BASE_STONE_OVERWORLD` ∪ `DIRT` ∪ `SAND` ∪ `SNOW` ∪ `ICE` ∪ `GRAVEL`/`CLAY`/`MOSS_BLOCK`/`DEEPSLATE`；**`BlockTags.SNOWY` 在 1.20.1 不存在【核实，勿用】**），排除 `Blocks.BEDROCK` 与 `state.hasBlockEntity()`；`world.getSeed()`→`level.getSeed()`【核实】；高度边界 `4..238`/`1`/`255` → `level.getMinBuildHeight()+4` / `level.getMaxBuildHeight()-2`（主世界 −64..320）；`chunk.getHeightValue`→`chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)`（`[待查证]` T5）；哈希/噪声（`mix64`/`hashCell`/`latticeNoise`/`coherentNoise`/`value`/`smoothStep`/`lerp`）纯算术，**逐字保留** | `alku/csrp/world/FracturedTerrainHandler.java` |
| 24 | `world/star/SRPStarTypeSyncHandler` | Server：登录 / 重生 / 换维度时把主世界 `SRPStarWorldData.getStarType()` 发给玩家 | **与既有 `SrpStarWorldEvents` 完全重复**【核实：`SrpStarWorldEvents.java:16-33` 已处理登录 + 换维度】→ 退化为**只需补一个 `PlayerEvent.PlayerRespawnEvent`** | 扩展 `alku/csrp/world/SrpStarWorldEvents.java`（**不新建**） |

**净需新建的 Java 文件：约 21 个**（24 − `SRPCoreMod` − `GenLayerSRPDynamicStar` − `SRPStarTypeSyncHandler`；`AccessorShaderGroup` 可选）。

### 3.2 新增资产（43 个，全部需改名空间 + 改纹理目录）

**blockstates（5）** → `assets/csrp/blockstates/`
`deadhead_grass_short.json`、`deadhead_grass_tall.json`、`snow_covered_grass.json`、`snow_short_grass.json`、`snow_tall_grass.json`

**models/block（18）** → `assets/csrp/models/block/`
`deadhead_grass_short1..5.json`、`deadhead_grass_tall_bottom.json`、`deadhead_grass_tall_top.json`、`parasitetrunk_deadhead_rare.json`、`snow_short_grass.json`、`snow_short_grass_1..4.json`、`snow_tall_grass.json`、`snow_tall_grass_1..4.json`

**models/item（4）** → `assets/csrp/models/item/`
`deadhead_grass_short.json`、`deadhead_grass_tall.json`、`snow_short_grass.json`、`snow_tall_grass.json`

**structures（4）** → `data/csrp/structures/`
`deadhead_tree_large_1.nbt` … `deadhead_tree_large_4.nbt`

**sounds（1）** → `assets/csrp/sounds/misc/`
`snow_reversal.ogg`（**【核实】源文件存在**）

**textures（11）** → `assets/csrp/textures/block/`（**是 `block` 不是 `blocks`**）
`deadhead_grass_short1..5.png`、`deadhead_grass_tall_bottom.png`、`deadhead_grass_tall_top.png`、`parasitetrunk_deadhead_side_rare.png`、`snowy_double_plant_grass_bottom.png`、`snowy_double_plant_grass_top.png`、`snowy_grass_short.png`

> 计数：5 + 18 + 4 + 4 + 1 + 11 = 43 ✓
>
> **【核实】`blockstates/snow_covered_grass.json` 全文**：
> ```json
> { "variants": { "snowy=false": { "model": "minecraft:grass_snowed" },
>                 "snowy=true":  { "model": "minecraft:grass_snowed" } } }
> ```
> 两个变体都指向**原版**模型 `minecraft:grass_snowed`（1.12.2 名）。1.20.1 对应 `minecraft:block/grass_block_snow`（`[待查证]` T22）。两个变体键都保留（原方块永远 snowy）。

### 3.3 删除资产（7 个，无需动作）
`assets/srparasites/textures/blocks/parasitebush_frostg*.png`（7 个）—— 本工程 `assets/csrp/textures/` 下无同名文件（已 grep 确认），**无动作**。

### 3.4 删除 Java 类（1 个，无需动作）
`dhanantry/scapeandrunparasites/SRParasites/Tags.java` —— 本工程无对应文件，**无动作**。

### 3.5 资产命名空间迁移规则（可机械执行）

对 **§3.2 的每一个 json**（不含 .nbt / .png / .ogg）按顺序做下列替换，**无例外**：

| # | 查找 | 替换为 | 说明 |
|---|---|---|---|
| M1 | `srparasites:blocks/` | `csrp:block/` | 1.12.2 纹理目录 `blocks` → 1.20.1 `block`（**已核实**：`models/block/deadhead_grass_short1.json` 写 `"srparasites:blocks/deadhead_grass_short1"`；`models/block/snow_short_grass_1.json` 写 `"srparasites:blocks/snowy_grass_short"`） |
| M2 | `minecraft:blocks/` | `minecraft:block/` | 同上，原版纹理（**已核实** `snow_short_grass*.json` / `snow_tall_grass*.json` 的 `particle` 与 `snow` 写 `minecraft:blocks/snow`） |
| M3 | `srparasites:` | `csrp:` | 剩余命名空间引用（模型引用、`layer0`、`cross` 纹理） |
| M4 | `"parent": "block/` | `"parent": "minecraft:block/` | 工程既有 json 统一写全限定名（`models/block/deadhead_leaves_snow.json` 写 `minecraft:block/cube_bottom_top`） |
| M5 | `"parent": "item/` | `"parent": "minecraft:item/` | 同上（`models/item/*.json` 的 `item/generated`） |
| M6 | `"model": "block/` / `"models": "block/` | 改为 `csrp:block/` | 工程既有 blockstate 一律写 `csrp:block/...` 全限定（`blockstates/deadhead_leaves.json`） |
| M7 | （新增非替换项） | 给 `snow_short_grass*.json` / `snow_tall_grass*.json` / `deadhead_grass_*.json` 加顶层 `"render_type": "cutout"` | **【核实】1.20.1 允许模型 json 顶层写 `render_type`**（工程 `deadhead_leaves_snow.json:3` 写 `"cutout_mipped"`）。这是 1.20.1 声明模型渲染层的推荐方式（`ItemBlockRenderTypes.setRenderLayer` 也可用【核实】，但需 Java 代码 + `FMLClientSetupEvent`，不如 json 声明简洁） |

**必须人工修正（规则覆盖不到的 2 处）**：
1. `blockstates/snow_short_grass.json` 与 `snow_tall_grass.json` 的变体键从 `"normal"` 改为 `""`（1.20.1 无属性方块的空变体键是空串——**【核实】** `blockstates/gothshroom.json:1` = `{"variants":{"":{"model":"csrp:block/gothshroom"}}}`）。**【核实 1.10.9 原文】**：`snow_short_grass.json` 为 `{"variants":{"normal":[ 5 个模型 ]}}`（数组 = 随机变体，1.20.1 保留数组形式）。
2. `deadhead_grass_short.json` / `deadhead_grass_tall.json` 的变体键 `texture=0..4` / `part=top|bottom` **保持不变**，只按 M1/M3/M6 把模型引用前缀改为 `csrp:block/`。**【核实 1.10.9 原文】**：`deadhead_grass_short.json` 的 5 个变体分别指向 `deadhead_grass_short1..5`；`deadhead_grass_tall.json` 的 `part=top/bottom` 分别指向 `deadhead_grass_tall_top/bottom`。

**NBT 结构（4 个 .nbt）不改文件本身**：沿用 `MeteorStructureLoader` 的运行时改写（**【核实】** `MeteorStructureLoader.java:83-120` 把 `srparasites:<block>` 重写成 `csrp:<block>`，经 `BLOCK_RENAMES` 旧名映射）。`parasitetrunk` 无重命名条目 → `getOrDefault` 直达 → **无需新增映射**。

---

## 4. Mixin / Shader 决策（最大风险点，已被真机核实大幅降级）

### 4.0 现状核实：Mixin 基础设施**已存在且在工作**

证据（全部来自工程本体读取）：

1. `src/main/resources/csrp.mixins.json` 存在：
   ```json
   { "required": true, "minVersion": "0.8", "package": "alku.csrp.mixin",
     "compatibilityLevel": "JAVA_17", "mixins": [],
     "client": ["client.ChatComponentMixin","client.CreateWorldScreenMixin","client.FontMixin","client.GuiMixin","client.SignRendererMixin"],
     "injectors": { "defaultRequire": 1 }, "refmap": "csrp.refmap.json" }
   ```
2. `build.gradle:138` 已把 `MixinConfigs : "csrp.mixins.json"` 写进 jar manifest（Forge 1.20.1 的 `FMLLoader` 经此加载；`mods.toml` 内**没有** `[[mixins]]` 块，已核实 `src/main/templates/META-INF/mods.toml` 全文 43 行）。
3. `build.gradle:98-99` 注释「Mixin AP is intentionally disabled」；dependencies 里**没有任何 annotation processor** → **无 refmap 生成**。
4. 因此 `"refmap": "csrp.refmap.json"` 指向**不存在的文件**，mixin 只能按**官方名**解析；现有 5 个 mixin 全部用 `method = {"官方名","m_XXXXXX_"}` 双写 + `require = 0`（**【核实】** `GuiMixin.java:15`、`FontMixin.java:12-42`、`CreateWorldScreenMixin.java:12`、`SignRendererMixin.java:12,17`）。

**结论：Mixin 可用，但本方案把新增 mixin 从 2（或 3）条压到 0 条（推荐）或 1 条（兜底）。**

### 4.1 三个原 mixin 的落地方案（**已因 1.20.1 API 不存在而全部改用官方路线**）

#### 4.1.1 `MixinEntityRendererBlizzard` → 自定义 `DimensionSpecialEffects.renderSnowAndRain`（零 mixin）

| 原 1.12.2 注入 | 1.20.1 落点 | 依据 |
|---|---|---|
| `@Inject(RETURN, renderRainSnow(F)V)` 画暴风雪 | **方案 A（推荐，零 mixin）**：自定义 `DimensionSpecialEffects` 覆写 **`renderSnowAndRain(ClientLevel, int, float, LightTexture, double, double, double)`**。**【核实】** `IForgeDimensionSpecialEffects` 提供该 default 方法，而 `LevelRenderer.renderSnowAndRain` 的**整个方法体被 `if (!this.level.effects().renderSnowAndRain(...)) { …vanilla… }` 包住** → 覆写它**返回 `true` 即可完全接管雨雪渲染**并调用工程自己的 `BlizzardRenderer`；冷星返回 `true`，其他星/维度返回 `false` 走原版。这是「只加不换」的官方扩展点，**不需要任何 mixin**。 | 【核实】`IForgeDimensionSpecialEffects.renderSnowAndRain` 签名；`LevelRenderer.renderSnowAndRain` 体受其守卫 |
| 同上 | **方案 B（兜底，1 条 mixin，仅当 A 不可行时）**：`alku/csrp/mixin/client/LevelRendererBlizzardMixin.java`，`@Mixin(net.minecraft.client.renderer.LevelRenderer.class)`，`@Inject(method = {"renderSnowAndRain", "m_1097xx_"}, at = @At("HEAD"), cancellable = true, require = 0)`，HEAD 处 `if (BlizzardClient.getIntensity(partialTicks) > 0.001F) { BlizzardFogRenderer.render(...); BlizzardRenderer.render(...); ci.cancel(); }`。**【核实】目标签名**：`private void renderSnowAndRain(LightTexture, float, double, double, double)`，描述符 `(Lnet/minecraft/client/renderer/LightTexture;FDDD)V`；参数顺序 = `(LightTexture lightTexture, float partialTick, double camX, double camY, double camZ)` → `partialTick` 可直接喂给 `getIntensity`。**SRG 名 `[待查证]` T1′**（从 SRG 名 jar 反查；写错也不崩，因 `require = 0`）。 | 【核实】签名与描述符 |
| `@Inject(TAIL, setupFog(IF)V)` 覆写线性雾 | **事件**：`@SubscribeEvent ViewportEvent.RenderFog`。**【核实】方法表**：`getMode()` / `getType()` / `getFarPlaneDistance()` / `getNearPlaneDistance()` / `getFogShape()` / `setFarPlaneDistance(float)` / `setNearPlaneDistance(float)` / `setFogShape(FogShape)` / `scaleFarPlaneDistance(float)` / `scaleNearPlaneDistance(float)`；**必须 `event.setCanceled(true)` 距离修改才会生效**（`ForgeHooksClient.onFogRender` 在 cancel 时应用 `RenderSystem.setShaderFogStart/End/Shape`）。取值：`far = 72.0F - intensity*56.0F`、`near = far*0.08F`、`FogShape.SPHERE`；水下/岩浆下直接 `return`（不 cancel）。 | 【核实】`ViewportEvent.RenderFog` 全方法表 + `FogRenderer` 反编译 |
| 同上（雾色，可选） | `ViewportEvent.ComputeFogColor`（**【核实】构造 `(Camera, float partialTicks, float red, float green, float blue)` —— 5 参、无 `FogMode`/`boolean`**；`getRed/setRed/getGreen/setGreen/getBlue/setBlue` 为 `float`）把雾色改成雪雾色。 | 【核实】 |
| `@Redirect(Biome.canRain()/getEnableSnow())` 关原版雨雪 | **不需要**：方案 A/B 都直接「不画原版雨雪」，等价于 1.10.9 的效果。 | — |

> **决策**：**优先方案 A**（自定义 `DimensionSpecialEffects`）。它同时解决 #13 与 #14 两件事、零 mixin、且是 Forge 官方扩展点。
> **风险**：方案 A 会**完整替换**原版 `OverworldEffects`，必须忠实委托天空/云/地形雾等所有方法（见 §4.1.2 的委托实现要求），否则会出现「天空没了」这类严重回退。
> **兜底**：若 T2b 证明原版 `OverworldEffects` 不可实例化/委托，退到方案 B（1 条 mixin）。

#### 4.1.2 `MixinRenderGlobalBlizzardSky` → **零 mixin**：同一个 `DimensionSpecialEffects` 子类覆写 `getSunriseColor`

**【核实】核心事实链**：
1. `net.minecraft.client.renderer.DimensionSpecialEffects implements IForgeDimensionSpecialEffects`；它有 `private final float[] sunriseCol = new float[4];`（private，无 setter）与 **`@Nullable public float[] getSunriseColor(float timeOfDay, float partialTick)`**。
2. `getSunriseColor` 方法体：在 `Mth.cos(timeOfDay * 2π) ∈ [-0.4, 0.4]` 时返回 `sunriseCol`（经典橙红日出色），**否则返回 `null`**。
3. **`FogRenderer.setupColor` 是 `getSunriseColor` 的唯一消费者**：`float[] a = level.effects().getSunriseColor(level.getTimeOfDay(pt), pt); if (a != null) { t *= a[3]; fogRed = fogRed*(1-t)+a[0]*t; ... }`（仅在 `renderDistance >= 4` 且相机朝向太阳时）。
4. **返回 `null` 就等价于「隐藏日出霞光」**——这正是 1.10.9 那个 `@Redirect` 想做的事。
5. **Forge 全库没有任何 sunrise 钩子**（对 Forge sources jar 全部 `.java` 做 `(?i)sunrisecolor|sunset|getSunriseColor` 全文扫描，**0 命中**）→ 唯一官方路线就是**注册自己的 `DimensionSpecialEffects` 子类并覆写该方法**。
6. 注册方式：`net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent.register(?, DimensionSpecialEffects)`（**MOD 总线、仅客户端**）。
7. Forge 的 patch 把 `DimensionSpecialEffects.forType` 改到了 `DimensionSpecialEffectsManager.getForType(ResourceLocation)` → 说明注册键是**维度类型/维度 stem 的 `ResourceLocation`**。

> **【主代理已真机核实 → T2a / T2b 结案，本节及 §4.3 中标 `[待查证]` 的两处以此为准】**
> 核实命令与输出（jar = `forge-1.20.1-47.4.23_mapped_official_1.20.1.jar`）：
> ```
> javap -cp <jar> net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent
>   public void register(net.minecraft.resources.ResourceLocation, net.minecraft.client.renderer.DimensionSpecialEffects)
> javap -cp <jar> net.minecraft.client.renderer.DimensionSpecialEffects
>   public DimensionSpecialEffects(float, boolean, SkyType, boolean, boolean)
>   public static DimensionSpecialEffects forType(net.minecraft.world.level.dimension.DimensionType)
>   public float[] getSunriseColor(float, float)
>   public abstract Vec3 getBrightnessDependentFogColor(Vec3, float)
>   public abstract boolean isFoggyAt(int, int)
> javap -cp <jar> 'net.minecraft.client.renderer.DimensionSpecialEffects$OverworldEffects'
>   public class ...DimensionSpecialEffects$OverworldEffects extends DimensionSpecialEffects
>   public DimensionSpecialEffects$OverworldEffects()      <-- public 无参构造
> ```
> - **T2a 结案**：第一参数是 **`net.minecraft.resources.ResourceLocation`**（维度类型 id）。主世界注册键 = `new ResourceLocation("minecraft", "overworld")`。
> - **T2b 结案**：`OverworldEffects` 是 **public 且具备 public 无参构造** → 方案 A 可以直接 `new DimensionSpecialEffects.OverworldEffects()` 做**忠实委托**。§4.1.2 下面「逐行搬运反编译实现」的要求**降级为委托 `VANILLA` 字段**；风险表 R1 的应对 ① 同理降级。
> - **最终形态**：`BlizzardDimensionEffects` 持 `private static final DimensionSpecialEffects VANILLA = new DimensionSpecialEffects.OverworldEffects();`，**只**覆写 `getSunriseColor` 与 `renderSnowAndRain`，其余方法（`getCloudHeight`/`hasGround`/`skyType`/`forceBrightLightmap`/`constantAmbientLight`/`getBrightnessDependentFogColor`/`isFoggyAt` + `IForgeDimensionSpecialEffects` 的 `renderSky`/`renderClouds`/`tickRain`/`adjustLightmapColors`）全部转发给 `VANILLA`。注册键 `minecraft:overworld` 全局生效；非冷星时两个覆写方法都走 `VANILLA` 分支 → 满足回归判据 S9。

> **决策**：**用方案 A（自定义 `DimensionSpecialEffects` 覆写 `getSunriseColor`），删掉这条 mixin。**
> 与 `ViewportEvent.ComputeFogColor` 的取舍：纯事件改雾色**不能**隐藏日出霞光的 quad（霞光由 `RenderSystem.setShaderColor` 单独画，与雾色无关），所以必须动 `getSunriseColor`。

**`BlizzardDimensionEffects` 的委托实现要求（落码必读）**：
- 必须 `extends DimensionSpecialEffects`；构造参数 **【核实】为 `(float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLightmap, boolean constantAmbientLight)`** —— **1.20.1 没有「sunrise 数组」这个构造参数、也没有 `getFogColor()`**（那是 1.20.2+/1.21 的形态，网上很多示例是错的）。
- 必须覆写 `public abstract Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness)` 与 `public abstract boolean isFoggyAt(int x, int z)`（抽象方法）。
- 必须覆写（否则原版天空/云会消失）：`getCloudHeight()`、`hasGround()`、`skyType()`、`forceBrightLightmap()`、`constantAmbientLight()`、`getCloudColor(...)`、`renderClouds(...)`、`renderSky(...)`、`tickRain(...)`、`adjustLightmapColors(...)`。
- **最省事、最忠实的做法（已由 T2b 核实成立）**：不用反编译搬运，直接 `new DimensionSpecialEffects.OverworldEffects()` 作为委托目标：
  ```java
  private static final DimensionSpecialEffects VANILLA = new DimensionSpecialEffects.OverworldEffects();
  @Override public float[] getSunriseColor(float timeOfDay, float partialTick) {
      if (BlizzardClient.getIntensity(partialTick) > 0.001F) return null;      // 隐藏日出霞光
      return VANILLA.getSunriseColor(timeOfDay, partialTick);
  }
  @Override public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick,
          LightTexture lightTexture, double camX, double camY, double camZ) {
      if (BlizzardClient.getIntensity(partialTick) > 0.001F) {
          BlizzardFogRenderer.render(...);
          BlizzardRenderer.render(...);
          return true;   // 接管，原版雨雪不画
      }
      return VANILLA.renderSnowAndRain(level, ticks, partialTick, lightTexture, camX, camY, camZ);
  }
  ```
- **本类自身用 `super(...)` 还是 `VANILLA` 转发**：构造函数仍按 `(float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLightmap, boolean constantAmbientLight)` 传 `DimensionSpecialEffects.OverworldEffects.CLOUD_LEVEL`（**【核实】该常量存在，`public static final int`**）等原版值；其余所有方法**逐条转发给 `VANILLA`**，不要依赖 `super` 的默认实现（`super.getBrightnessDependentFogColor`/`super.isFoggyAt` 是抽象方法，无法调用）。
- 反编译产物备用：`C:\Users\P傲娇34\AppData\Local\Temp\verify1201\dec\`（ForgeFlower 2.0.629.0）——仅在需要核对 `OverworldEffects` 某方法默认行为时查阅。

#### 4.1.3 `AccessorShaderGroup` → **不新建 accessor mixin**

**【核实】1.20.1 目标类已不存在**：`net.minecraft.client.shader.ShaderGroup` / `Shader` 在 1.13+ 被 `PostChain`/`PostPass`/`EffectInstance` 取代。

| 原需求 | 1.20.1 等价（**【核实】全部真机核对**） | 本工程现状 |
|---|---|---|
| 通过 accessor 拿 `ShaderGroup.listShaders` 写 uniform | `PostChain` 字段 **`private final List<PostPass> passes;`**（**private、`PostChain` 无任何 getter，也无 `getEffect()`——`getEffect()` 在 `PostPass` 上**）；`PostPass` 字段 `private final EffectInstance effect;` + **`public EffectInstance getEffect();`**；`EffectInstance#safeGetUniform(String)` 返回 `com.mojang.blaze3d.shaders.AbstractUniform`；`com.mojang.blaze3d.shaders.Uniform#set(float)` 是 **`public final void`**（另有 `set(float,float)`、`set(int,float)`、`set(float,float,float)`、`set(Vector3f)`、`set(float,float,float,float)`、`set(Vector4f)`、`set(int…)`、`set(float[])`、`set(Matrix4f)`、`set(Matrix3f)`、`setSafe(...)`） | **已实现**：`StarWorldShaderEvents.java:42-158` 用反射（候选字段名 `{"passes","f_110009_","e"}` + `trySetAccessible()`）+ `pass.getEffect().safeGetUniform(name).set(value)`，带失败降级（`LOGGER.warn`）。`PostChain` 其他公开 API（**【核实】** `PostChain(TextureManager, ResourceManager, RenderTarget, ResourceLocation)`、`getTempTarget`、`addTempTarget`、`addPass`、`resize`、`process(float)`、`getName`、`close`）本工程用不到。加载侧 `GameRenderer.loadEffect(ResourceLocation)` / `currentEffect(): PostChain` / `shutdownEffect()` **【核实】全部存在**。 |

**决策**：**不新建 accessor mixin**，**复用 `StarWorldShaderEvents` 的反射 + `GameRenderer.loadEffect` 路线**，新增 `assets/csrp/shaders/post/blizzard_reverse.json` + `shaders/program/blizzard_reverse.{json,fsh}`（照抄工程既有 `kirin_vhs` 结构），uniform 名沿用 `SRP_*`。

理由：① accessor 能拿到的信息与反射完全相同（都要绕过 `passes` 的 private）；② 反射版已在本工程跑通并带降级；③ 少一个 mixin 就少一个 `required:true` 的启动崩溃面。
**取舍**：反射比 accessor 慢（`Field.get` 未缓存），但只在 `ClientTickEvent` 里每 tick 写一次 uniform，不在渲染热路径上。

> 可选优化（不进必做范围）：`alku/csrp/mixin/client/PostChainAccessorMixin.java`
> ```java
> @Mixin(PostChain.class)
> public interface PostChainAccessorMixin {
>     @Accessor("passes") List<PostPass> csrp$passes();
> }
> ```
> 在 `csrp.mixins.json` 的 `"client"` 数组注册 `"client.PostChainAccessorMixin"`。**【核实】`passes` 字段名与类型正确，此 accessor 一定可用。**

### 4.2 1.20.1 Forge 已核实的可编译 API 路线（可直接照写）

| API | 签名/用法 | 来源 |
|---|---|---|
| `RenderLevelStageEvent` | `getStage()`、`getLevelRenderer()`、`getPoseStack()`、`getProjectionMatrix()`、`getRenderTick()`、`getPartialTick()`、`getCamera()`、`getFrustum()` | 【核实】Forge sources + 工程 `AuroraSkyRenderer.java:50-51,123-126,144` |
| `RenderLevelStageEvent.Stage` | **完整列表（声明顺序）**：`AFTER_SKY`、`AFTER_SOLID_BLOCKS`、`AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS`、`AFTER_CUTOUT_BLOCKS`、`AFTER_ENTITIES`、`AFTER_BLOCK_ENTITIES`、`AFTER_TRANSLUCENT_BLOCKS`、`AFTER_TRIPWIRE_BLOCKS`、`AFTER_PARTICLES`、`AFTER_WEATHER`、`AFTER_LEVEL`；另有 `@Nullable static Stage fromRenderType(RenderType)` | 【核实】Forge sources |
| `ViewportEvent.RenderFog` | `getMode()`、`getType()`、`getFarPlaneDistance()`、`getNearPlaneDistance()`、`getFogShape()`、`setFarPlaneDistance(float)`、`setNearPlaneDistance(float)`、`setFogShape(FogShape)`、`scaleFarPlaneDistance(float)`、`scaleNearPlaneDistance(float)`；**必须 cancel 才生效** | 【核实】Forge sources + 工程 `CelestialSkyRenderer.java:191-200` |
| `ViewportEvent.ComputeFogColor` | 构造 `(Camera, float partialTicks, float red, float green, float blue)`（**5 参**）；`getRed/setRed/getGreen/setGreen/getBlue/setBlue`（float） | 【核实】Forge sources + 工程 `CelestialSkyRenderer.java:182-188` |
| `ViewportEvent.ComputeCameraAngles` | `(GameRenderer, Camera, double, float yaw, float pitch, float roll)` + `get/setYaw/Pitch/Roll` | 【核实】+ 工程 `MeteorClientEvents.java:62-72` |
| `RenderGuiEvent.Post` | `(Window, GuiGraphics, float)`；`getWindow()`/`getGuiGraphics()`/`getPartialTick()`；`GuiGraphics.fill(int,int,int,int,int)` | 【核实】Forge sources + 工程 `MeteorClientEvents.java:48-58` |
| `FogRenderer` | `public static void setupColor(Camera, float, ClientLevel, int, float)`（**5 参！1.20.1 无 trailing boolean**）；`public static void setupFog(Camera, FogMode, float, boolean, float)`（**5 参！**）；`setupNoFog()`；`levelFogColor()`；`FogMode` = `FOG_SKY`/`FOG_TERRAIN`；`FogShape` = `SPHERE`/`CYLINDER`；雾距离最终经 `RenderSystem.setShaderFogStart/End/Shape` 应用，`ForgeHooksClient.onFogRender(...)` 是其**最后一条语句** | 【核实】官方映射 jar + 反编译 |
| `GameRenderer` | `loadEffect(ResourceLocation)`、`currentEffect(): PostChain`、`shutdownEffect()`、`togglePostEffect()`、`cycleEffect()`、`getShader(String)`、一批 `getPositionShader()/getPositionColorShader()/getPositionTexShader()/getPositionColorTexShader()/...`；**没有雾字段、没有 `getFogColor()`**（网上 1.20.1 示例里的 `gameRenderer.getFogColor()` 是错的） | 【核实】javap + grep |
| `PostChain` / `PostPass` / `EffectInstance` | 见 §4.1.3 表 | 【核实】javap |
| `DimensionSpecialEffects` | `implements IForgeDimensionSpecialEffects`；构造 `(float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLightmap, boolean constantAmbientLight)`；`private final float[] sunriseCol`；**`@Nullable public float[] getSunriseColor(float timeOfDay, float partialTick)`**；`getCloudHeight()`、`hasGround()`、抽象 `getBrightnessDependentFogColor(Vec3,float)`、抽象 `isFoggyAt(int,int)`、`skyType()`、`forceBrightLightmap()`、`constantAmbientLight()`；`SkyType` = `NONE`/`NORMAL`/`END`；**没有 `getFogColor()`、没有 sunrise 数组构造参数** | 【核实】javap + 反编译 |
| `IForgeDimensionSpecialEffects` | `renderClouds(...)`、`renderSky(...)`、**`renderSnowAndRain(ClientLevel, int, float, LightTexture, double, double, double)`**、`tickRain(ClientLevel, int, Camera)`、`adjustLightmapColors(...)`（全为 default） | 【核实】Forge sources |
| `LevelRenderer.renderSnowAndRain` | **`private void renderSnowAndRain(LightTexture, float, double, double, double)`**，描述符 `(Lnet/minecraft/client/renderer/LightTexture;FDDD)V`；体首 `float rainLevel = minecraft.level.getRainLevel(partialTick); if (rainLevel <= 0) return;`；整个方法体被 `if (!this.level.effects().renderSnowAndRain(...)) { …vanilla… }` 包住；另有 `public void tickRain(Camera)`；`RAIN_RADIUS = 10`；雪纹理常量 `"textures/environment/snow.png"` | 【核实】javap + 反编译 |
| `BlockEvent.EntityPlaceEvent` | 构造 `(BlockSnapshot, BlockState placedAgainst, @Nullable Entity)`，`@Cancelable`；`getEntity()`、`getBlockSnapshot()`、`getPlacedBlock()`、`getPlacedAgainst()`；**旧状态 = `getBlockSnapshot().getReplacedBlock()`，新状态 = `.getCurrentBlock()`**；**`BlockSnapshot.getState()` 在 47.4.23 不存在** | 【核实】Forge sources |
| `BlockEvent.NeighborNotifyEvent` | 构造 `(Level, BlockPos, BlockState, EnumSet<Direction>, boolean)`；`getNotifiedSides()`、`getForceRedstoneUpdate()` | 【核实】Forge sources |
| `TickEvent.LevelTickEvent` | `public final Level level;` + `Phase`（START/END）+ `haveTime()`；`TickEvent.PlayerTickEvent` `[待查证]` T17 | 【核实】Forge sources + 工程 `MeteorEvents.java:30-35` |
| `ChunkEvent` | `extends LevelEvent`；`ChunkEvent(ChunkAccess)` / `(ChunkAccess, LevelAccessor)`；`getChunk()`；`ChunkEvent.Load`/`.Unload`；`LevelEvent.Load/Unload` 同族 | 【核实】Forge sources + 工程 `StarBiomeGenerationEvents.java:42-104` |
| `Level#getRainLevel(float)` / `getThunderLevel(float)` / `isRaining()` / `isRainingAt(BlockPos)` | 全在 **`net.minecraft.world.level.Level`** 上（`ClientLevel` 未声明，继承可用） | 【核实】javap |
| `LevelReader#getHeightmapPos(Heightmap.Types, BlockPos)` | **default 方法在 `LevelReader` 上**（不在 `Level` 声明，但可直接调）；`LevelReader#getHeight(Heightmap.Types,int,int)` 抽象；`Heightmap.Types` 含 `MOTION_BLOCKING`、`MOTION_BLOCKING_NO_LEAVES` | 【核实】javap + 工程 `ColdStarVillageGenerator.java:62,72` |
| `BushBlock` | `protected boolean mayPlaceOn(BlockState, BlockGetter, BlockPos)`（1.20.1 名）；`public boolean canSurvive(BlockState, LevelReader, BlockPos)`；`getPlant(BlockGetter, BlockPos)` | 【核实】javap |
| `TallGrassBlock` | `extends BushBlock implements BonemealableBlock, IForgeShearable`；`getShape`、`isValidBonemealTarget`、`isBonemealSuccess`、`performBonemeal`；**它自己没有声明 `canSurvive`/`getStateForPlacement`**（继承自 `BushBlock`/`Block`）——**照抄它的剪切实现即可拿到 `IForgeShearable` 的正确签名** | 【核实】javap |
| `SnowyDirtBlock` / `GrassBlock` | `public class SnowyDirtBlock extends Block { public static final BooleanProperty SNOWY; ... }`；**`public class GrassBlock extends SpreadingSnowyDirtBlock implements BonemealableBlock`（不是直接 extends `SnowyDirtBlock`）**，但 `SNOWY` 仍可访问 | 【核实】javap |
| `DoublePlantBlock` | `public static final EnumProperty<DoubleBlockHalf> HALF;`；`updateShape`、`getStateForPlacement(BlockPlaceContext)`、`setPlacedBy(Level,BlockPos,BlockState,LivingEntity,ItemStack)`、`canSurvive(BlockState,LevelReader,BlockPos)`、`public static void placeAt(LevelAccessor,BlockState,BlockPos,int)`、`public static BlockState copyWaterloggedFrom(LevelReader,BlockPos,BlockState)`、`playerWillDestroy(Level,BlockPos,BlockState,Player)`、`protected static void preventCreativeDropFromBottomPart(Level,BlockPos,BlockState,Player)`、`getSeed(...)` | 【核实】javap |
| 渲染层（cutout） | `RenderShape` = `INVISIBLE`/`ENTITYBLOCK_ANIMATED`/`MODEL`；`RenderType.cutout()/cutoutMipped()/solid()/translucent()/tripwire()`；`ItemBlockRenderTypes.setRenderLayer(Block, RenderType)` 可用（需客户端 + `FMLClientSetupEvent`）；**但模型 json 顶层 `"render_type"` 更简洁且工程已在用** | 【核实】javap + 工程 `models/block/deadhead_leaves_snow.json:3` |
| `BlockBehaviour` / `BlockBehaviour.Properties` | `getRenderShape(BlockState)`、`getShape(BlockState, BlockGetter, BlockPos, CollisionContext)`；`Properties.of()`、`copy(BlockBehaviour)`；`noOcclusion()`、`noCollission()`、`instabreak()`、`randomTicks()`、`strength(float,float)`、`sound(SoundType)`、`offsetType(...)`、`replaceable()`、`mapColor(...)`、`lightLevel(...)` | 【核实】javap + 工程 `ModBlocks.java:98-134,503-509` |
| `BlockTags` | 含 `LEAVES`、`LOGS`、`DIRT`、`SNOW`、`REPLACEABLE`、`MINEABLE_WITH_*` 等；**没有 `BlockTags.SNOWY`（勿用）** | 【核实】javap |
| `RegisterDimensionSpecialEffectsEvent` | `register(?, DimensionSpecialEffects)`（**MOD 总线、客户端**）——第一参数类型 `[待查证]` T2a | 【核实】事件存在，参数待定 |
| `RegisterClientReloadListenersEvent` | 存在（`IModBusEvent`）：`registerReloadListener(PreparableReloadListener)` | 【核实】Forge sources |
| `DeferredRegister` / `RegistryObject` | `create(IForgeRegistry, String)` / `create(ResourceKey<Registry>, String)` / `create(ResourceLocation, String)`；`register(String, Supplier)` → `RegistryObject`；`register(IEventBus)`；`RegistryObject#get/getId/getKey/isPresent/ifPresent/map/orElse/orElseGet/orElseThrow/getHolder` | 【核实】javap + 工程用法 |
| `SimpleChannel` 注册 | `NetworkRegistry.newSimpleChannel(ResourceLocation, ()->String, Predicate, Predicate)` + `registerMessage(id++, Class, encode, decode, handle, Optional<NetworkDirection>)` | 【核实】工程 `CsrpNetwork.java:20-59` |
| payload 写法 | `record XxxPayload(...) { void encode(FriendlyByteBuf); static XxxPayload decode(FriendlyByteBuf); static void handle(XxxPayload, Supplier<NetworkEvent.Context>) { enqueueWork; setPacketHandled(true); } }` | 【核实】工程 `StarWorldStatePayload.java:9-22` |
| `Tesselator` 立即模式 | `Tesselator.getInstance().getBuilder()` → `begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)` → `vertex(x,y,z).color(...).endVertex()` → `end()` → `new VertexBuffer(Usage.STATIC)` + `bind()`/`upload(...)` | 【核实】工程 `AuroraSkyRenderer.java:164-185` |
| `VertexBuffer` 绘制 | `bind()` → `drawWithShader(Matrix4f, Matrix4f, ShaderInstance)` → `VertexBuffer.unbind()` → `close()` | 【核实】工程 `AuroraSkyRenderer.java:143-145,180-183,206-208` |
| `ShaderInstance` | `new ShaderInstance(ResourceManager, ResourceLocation, VertexFormat)`、`setSampler(String, Texture)`、`getUniform(String).set(float)`、`close()` | 【核实】工程 `AuroraSkyRenderer.java:83-91,157-161,199` |
| `RenderSystem` 状态 | `depthMask`、`disableDepthTest`、`disableCull`、`enableBlend`、`blendFunc(GlStateManager.SourceFactor, GlStateManager.DestFactor)`、`defaultBlendFunc`、`setShaderColor`、**`setShaderFogStart/End/Shape/Color` + `getShaderFogStart/End/Shape/Color`** | 【核实】工程 `AuroraSkyRenderer.java:134-152` + `FogRenderer` 反编译 |
| `SoundEvent.createVariableRangeEvent(ResourceLocation)` | 音效注册（`ModSounds.register` 带去重） | 【核实】工程 `ModSounds.java:139-141,161-179` |
| `BlockItem` 注册 | `ITEMS.registerSimpleBlockItem("id", ModBlocks.XXX)` | 【核实】工程 `ModItems.java:365-651` |
| 无属性方块 blockstate 变体键 | 空串 `""` | 【核实】工程 `blockstates/gothshroom.json:1` |
| 自定义 NBT 结构加载 | `getServer().getResourceManager().getResource(ResourceLocation)` → `NbtIo.readCompressed` → `StructureTemplate#load(HolderGetter<Block>, CompoundTag)` → `placeInWorld(ServerLevelAccessor, BlockPos, BlockPos, StructurePlaceSettings, RandomSource, int)` | 【核实】工程 `MeteorStructureLoader.java:52-81` |
| `mod_version` 展开 | `generateModMetadata`（`ProcessResources` + `expand`）→ 加进 `sourceSets.main.resources.srcDir` | 【核实】工程 `build.gradle:103-127` + `mods.toml:10` |

### 4.3 待本地核实清单（B 阶段落实；**禁止凭记忆写**）

**已在本轮解决、从清单移除**：原 T1（雨雪方法签名）、原 T3（`PostChain.passes`）、原 T4（BlockItem 注册）、原 T7（`EntityPlaceEvent` 旧状态）、原 T8（`DoublePlantBlock`）、原 T9（`IForgeShearable`）、原 T10（`LevelEvent`）、原 T11（原版雪纹理路径）、原 T12 的 `getRainLevel` 部分、原 T14（`isRaining`）、原 T17 的 `LevelTickEvent`/`PlayerEvent` 家族部分、原 T20（`Stage` 完整列表）、原 T2 的②③（`getSunriseColor` 签名与消费者）。

| T# | 待核实项 | 核实手段 |
|---|---|---|
| T1′ | `LevelRenderer.renderSnowAndRain` 的 **SRG 名**（形如 `m_1097xx_`），仅在走兜底方案 B 时需要 | `javap -p -classpath <mcp_repo/.../joined/rename/output.jar>  net.minecraft.client.renderer.LevelRenderer`（**该 jar 是 SRG 名**，见 §10.4） |
| T2a | `RegisterDimensionSpecialEffectsEvent.register` 的**第一参数精确类型**（`ResourceLocation` / `ResourceKey<LevelStem>` / `ResourceKey<DimensionType>`），以及主世界应传什么 key | `javap -p -classpath <官方jar> net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent` |
| T2b | `DimensionSpecialEffects.OverworldEffects` 的**可见性与构造签名**（决定 §4.1.1/4.1.2 方案 A「委托原版实现」是否可行） | `javap -p -classpath <官方jar> 'net.minecraft.client.renderer.DimensionSpecialEffects$OverworldEffects'`；反编译产物已在 `%TEMP%\verify1201\dec\` |
| T5 | `LevelChunk#setBlockState` 是否已自动维护 `Heightmap`；`ChunkAccess#getHeight(Heightmap.Types,int,int)` 签名 | `javap -p net.minecraft.world.level.chunk.LevelChunk` / `ChunkAccess` |
| T6 | `Biome#getBaseTemperature()` 的精确取名；`Biome.Precipitation` 常量 | `javap -p net.minecraft.world.level.biome.Biome` |
| T12 | 相机三插值坐标的官方字段/方法名（玩家 `xo/yo/zo` 与 `getX/Y/Z()`，或 `Camera#getPosition()`）；`ClientLevel` 的日光亮度取名 | `javap -p net.minecraft.world.entity.Entity` / `net.minecraft.client.Camera` / `net.minecraft.client.multiplayer.ClientLevel` |
| T13 | `DefaultVertexFormat.POSITION_COLOR_TEX` 的**字段顺序**（1.20.1 应为 POSITION→COLOR→TEX） | `javap -p com.mojang.blaze3d.vertex.DefaultVertexFormat` + 读 `BufferBuilder.vertex/color/uv` 返回类型 |
| T15 | `Level#canSeeSky(BlockPos)` 在 1.20.1 的精确名 | `javap -p net.minecraft.world.level.Level` |
| T16 | `StructureTemplate` 是否有「按 `StructurePlaceSettings` 变换 BlockPos」的公开方法（对应 1.12.2 `Template.func_186266_a`） | `javap -p net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate` |
| T17 | `TickEvent.PlayerTickEvent` 是否仍存在；`PlayerEvent.PlayerRespawnEvent` / `PlayerLoggedOutEvent` 的精确嵌套名与 getter | `javap -p net.minecraftforge.event.TickEvent` / `net.minecraftforge.event.entity.player.PlayerEvent` |
| T18 | `BlockState#liquid()` / `isSolid()` / `canBeReplaced()` 的精确名 | `javap -p net.minecraft.world.level.block.state.BlockState` |
| T21 | `AbstractTickableSoundInstance` 的构造与必须实现的方法 | `javap -p net.minecraft.client.resources.sounds.AbstractTickableSoundInstance` |
| T22 | 1.20.1 原版雪草模型名（1.12.2 `minecraft:grass_snowed` 的对应物，预期 `minecraft:block/grass_block_snow`） | `python -c "import zipfile;z=zipfile.ZipFile(r'<client.jar>');print([n for n in z.namelist() if 'models/block/grass' in n])"` |
| **T23** | ~~`BlockStateProperties.DOUBLE_BLOCK_HALF` 的属性名与取值名~~ | **【B 已核实并结案】** 属性名 = **`half`**、取值为 **`DoubleBlockHalf.UPPER` / `LOWER`**，序列化名 **`upper` / `lower`**（不在 `BlockStateProperties` 里，在 `DoubleBlockHalf.getSerializedName()` 的字节码里）。⚠ **§3.5 与 §5.3 里「blockstate 变体键 `part=top|bottom`」全部作废**：`deadhead_grass_tall.json` 必须写 **`half=upper` / `half=lower`**，否则雪/死头高草全变 missing model。证据：`javap -c ...DoubleBlockHalf` 输出 `ldc "upper"` / `ldc "lower"`；`javap -p -c ...BlockStateProperties` 的常量池紧邻 `putstatic DOUBLE_BLOCK_HALF` 处为 `ldc_w "half"`。 |
| **T24** | ~~`IForgeShearable` 的精确方法签名~~ | **【B 已核实并结案】** `public default boolean isShearable(ItemStack, Level, BlockPos)`、`public default List<ItemStack> onSheared(Player, ItemStack, Level, BlockPos, int)` —— **与 1.12.2 的 `IShearable` 完全不同**（多了 `Player` 参数，且 `Level` 取代了 `IBlockAccess`，无 `fortune` 之外的差异）。`TallGrassBlock` 未覆写它们（`javap` 无输出）→ 直接用接口默认实现也安全。 |
| **T25** | ~~`SnowyDirtBlock.SNOWY` 是否已在 `GrassBlock` 的 state definition 内~~ | **【B 已核实并结案】** `GrassBlock extends SpreadingSnowyDirtBlock extends SnowyDirtBlock`，`SnowyDirtBlock` 自带 `createBlockStateDefinition` 注册 `SNOWY` → **`SnowCoveredGrassBlock` 绝不能再次 `builder.add(SNOWY)`**（会重复注册）。C 阶段只需覆写 `getStateForPlacement` 强制 `snowy=true`。 |
| **T26** | ~~自定义方块能否直接覆写 `codec()`~~ | **【B 已核实并结案】** 官方映射 jar 里 **`Block` 类没有任何 `codec()` 成员**（`javap -p net.minecraft.world.level.block.Block` 无命中）→ 覆写 `codec()` 会直接报「method does not override」。**新方块一律不要写 `codec()`/`MapCodec`**，除非查清 1.20.1 的确切位置。 |
| **T27** | ~~`DoublePlantBlock.preventCreativeDropFromBottomPart` 能否被非 `DoublePlantBlock` 子类调用~~ | **【B 已核实并结案】** 它是 **`protected static`** 且声明在 `DoublePlantBlock` 上 → **`DeadheadGrassTallBlock`（`extends BushBlock`）无法调用**，C 必须在自己的类里重写等价逻辑（照 `DoublePlantBlock.playerWillDestroy` 的判定：上/下半 + 是否处于创造模式）。 |
| **T12 / T13 / T21** | ~~相机插值坐标取名 / `POSITION_COLOR_TEX` 字段顺序 / `AbstractTickableSoundInstance` 构造~~ | **【C 片 4 已核实并结案】** 详见后文追加的 **§12.1 V10–V13**：相机用 `Camera.getPosition()`（已按 partialTick 插值）、风向取 `Camera.getYRot()`；`POSITION_COLOR_TEX` 顺序为 **POSITION→COLOR→TEX**；`AbstractTickableSoundInstance(SoundEvent, SoundSource, RandomSource)` + `isStopped()/stop()`。 |

---

## 5. 逐条技术难点落地方案

### 5.1 Mixin / Shader（最高风险，已降级）
见 §4。**结论**：
- **推荐新增 0 条 mixin**：日出霞光 + 雨雪渲染**统一由自定义 `DimensionSpecialEffects`（`BlizzardDimensionEffects`）承接**，用 `RegisterDimensionSpecialEffectsEvent` 注册。
- 雾由 `ViewportEvent.RenderFog`（必须 cancel）承担；雾色可选 `ViewportEvent.ComputeFogColor`。
- Shader uniform 走工程既有反射路线，**不加 accessor mixin**（`PostChain.passes` 已核实 private 无 getter）。
- 兜底方案：1 条 `LevelRendererBlizzardMixin`（仅当 T2b 证明 `OverworldEffects` 不可委托时）。

### 5.2 `AccessorShaderGroup` + 自定义 post shader
见 §4.1.3。**不新建 accessor；复用 `StarWorldShaderEvents` 反射；新增 `assets/csrp/shaders/post/blizzard_reverse.json` + `shaders/program/blizzard_reverse.{json,fsh}`。**

### 5.3 5 个新方块 + `SnowGrassHandler`
- **注册落点**：`alku/csrp/registry/ModBlocks.java`，在 `DEADHEAD_LEAVES`（第 503-509 行）之后、`LEGACY_BLOCKS`（第 517 行）之前追加 5 个 `RegistryObject`：
  ```
  DEADHEAD_GRASS_SHORT  -> "deadhead_grass_short"  (DeadheadGrassShortBlock)
  DEADHEAD_GRASS_TALL   -> "deadhead_grass_tall"   (DeadheadGrassTallBlock)
  SNOW_SHORT_GRASS      -> "snow_short_grass"      (SnowShortGrassBlock)
  SNOW_TALL_GRASS       -> "snow_tall_grass"       (SnowTallGrassBlock)
  SNOW_COVERED_GRASS    -> "snow_covered_grass"    (SnowCoveredGrassBlock，无 BlockItem)
  ```
- **BlockItem**（4 个）：`ModItems.java` 追加 `ITEMS.registerSimpleBlockItem("deadhead_grass_short", ModBlocks.DEADHEAD_GRASS_SHORT);` 等 4 行（**【核实】这是工程既有写法**）。
- **`BlockBehaviour.Properties`**：`BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)`；雪覆盖草用 `.strength(0.6F).randomTicks()`（照 1.10.9 `setHardness(0.6F)`）。
- **渲染层**：模型 json 顶层 `"render_type": "cutout"`（**【核实】键可用**）；**不需要** `ItemBlockRenderTypes`。
- **事件签名**：
  - `BlockEvent.EntityPlaceEvent`：`getLevel()`（`LevelAccessor`）、`getPos()`、`getPlacedBlock()`、`getEntity()`、**`getBlockSnapshot().getReplacedBlock()`（旧状态）**；普通 `@EventBusSubscriber(modid = Csrp.MODID)`。
  - `TickEvent.LevelTickEvent`：`event.phase == TickEvent.Phase.END`、`event.level`。
  - `ChunkEvent.Load`：照 `StarBiomeGenerationEvents.java:42-47`。
- **双高草**：`extends BushBlock`，`EnumProperty<DoubleBlockHalf> PART = BlockStateProperties.DOUBLE_BLOCK_HALF`；`setPlacedBy` 放下半；破坏联动照 `DoublePlantBlock`（`playerWillDestroy` + `preventCreativeDropFromBottomPart`）。**不继承 `DoublePlantBlock`**。
- **雪覆盖草**：`extends GrassBlock`（`SNOWY` 可用）；`getStateForPlacement` 强制 `SNOWY=true`；`randomTick`/`neighborChanged` 检查上方不是 `SNOW_TALL_GRASS`/`SNOW_SHORT_GRASS` 时 `setBlock(..., Blocks.GRASS_BLOCK..., 3)`；`playerDestroy` 给 `Items.DIRT`；`getCloneItemStack` 给 `Items.GRASS_BLOCK`。
- **`SnowGrassHandler` 落点**：新建 `alku/csrp/world/SnowGrassEvents.java`。

### 5.4 星世界代码的合并（扩展而非另起）
| 1.10.9 类/机制 | 决策 | 落点 |
|---|---|---|
| `SRPClientStarWorldState` | **扩展** `alku/csrp/celestial/client/StarWorldClientState.java`：新增 `isCold()` / `isWarm()` | 改 1 个文件（+2 方法） |
| `MsgSyncStarType` | **不需要**（既有 `StarWorldStatePayload` 已同步 `SrpStarType`） | 无动作 |
| `SRPStarTypeSyncHandler` | **并入** `SrpStarWorldEvents`（补 `PlayerEvent.PlayerRespawnEvent`） | 改 `SrpStarWorldEvents.java`（+1 方法） |
| `GenLayerSRPDynamicStar` | **不移植**；既有 `StarBiomeGenerationEvents` 已是等价替换 | 无动作 |
| `SRPStarWorldData` 的两个新开关 | **扩展** `SrpWorldData`：+2 布尔字段 + load/save + 4 方法；`DATA_VERSION` 5 → 6；`setStarType` 里「非 COLD 时清 `fracturedTerrain`」 | 改 `SrpWorldData.java` |
| 世界创建选择传递 | 新开 `alku/csrp/world/SrpStarWorldSelection.java`（**不改既有 `SrpStarTypeSelection`**） | 新 1 个文件 |
| 网络包注册落点 | `CsrpNetwork.register()` 末尾追加（当前最大 id = 8，新包用 **id 9**） | 改 `CsrpNetwork.java` |
| 星类型同步触发点 | 既有 login + changedDimension 已有，补 respawn 即齐 | 改 `SrpStarWorldEvents.java` |

### 5.5 `SRPFracturedTerrainHandler` 落点
- 类：`alku/csrp/world/FracturedTerrainHandler.java`（包私有 `final`）。
- 触发：`StarBiomeGenerationEvents.convertNewChunk` 里，**`replaceBiomes` 之前**：
  ```java
  SrpWorldData data = SrpWorldData.get(level);
  if (starType == SrpStarType.COLD && data.fracturedTerrainEnabled()) {
      FracturedTerrainHandler.fractureChunk(level, chunk, level.getSeed());
  }
  replaceBiomes(level, chunk, starType);
  ```
- 常数逐字照抄（`PLATE_SIZE=96`、`PLATE_JITTER=28`、`CRACK_WIDTH=2.25`、`COLLISION_WIDTH=4.75`、`MIN_RAVINE_DEPTH=20`、`MAX_RAVINE_DEPTH=52`、`MIN_COLLISION_RISE=7`、`MAX_COLLISION_RISE=19`、`MIN_PLATE_OFFSET=-12`、`MAX_PLATE_OFFSET=16`）；哈希/噪声（`mix64`/`hashCell`/`latticeNoise`/`coherentNoise`/`value`/`smoothStep`/`lerp`）纯算术、逐字搬。
- 高度限幅：`level.getMinBuildHeight()+4` 与 `level.getMaxBuildHeight()-2`。
- 方块白名单：`BlockTags.BASE_STONE_OVERWORLD` ∪ `DIRT` ∪ `SAND` ∪ `SNOW` ∪ `ICE` ∪ (`Blocks.GRAVEL`/`CLAY`/`MOSS_BLOCK`/`DEEPSLATE`)，排除 `Blocks.BEDROCK` 与任何 `state.hasBlockEntity()`。**不要用 `BlockTags.SNOWY`（1.20.1 不存在）。**
- 高度图重算：见 T5。

### 5.6 版本号
- `gradle.properties:31`：`mod_version=1.10.8` → `mod_version=1.10.9`。
- **已确认 `mods.toml` 会跟着走**：`build.gradle:103-127` 的 `generateModMetadata` 展开 `mod_version`（`build.gradle:113`），`mods.toml:10` 是 `version = "${mod_version}"`，输出目录又加进 `sourceSets.main.resources.srcDir` → jar 内 `META-INF/mods.toml` 的 `version` 为 `1.10.9`。**无额外改动。**
- `build.gradle:135` 的 `Implementation-Version` 取 `project.jar.archiveVersion`（= `mod_version`），自动跟到 1.10.9。

### 5.7 assets 命名空间迁移
见 §3.5（7 条机械规则 + 2 处人工修正）。

### 5.8 音效与语言键清单

**音效（1 个新事件 + 1 个新 .ogg）**
- `assets/csrp/sounds/misc/snow_reversal.ogg` ← `assets/srparasites/sounds/misc/snow_reversal.ogg`（**【核实】源文件存在**）
- `assets/csrp/sounds.json` 追加（**【核实】1.10.9 原文**，仅改命名空间）：
  ```json
  "blizzard_reverse": {
    "category": "ambient",
    "sounds": [ { "name": "csrp:misc/snow_reversal", "stream": true } ]
  }
  ```
- `ModSounds.java`：`public static final RegistryObject<SoundEvent> BLIZZARD_REVERSE = register("blizzard_reverse");`
- **关键坑**：`SoundEventCatalog` 由 `scripts/import-original-sounds.cjs` 生成（`SoundEventCatalog.java:5`），**不含 `blizzard_reverse`**（已 grep 确认）→ 必须像 `DARK_DAYS_START` 那样在 `ModSounds` 里**显式注册**（catalog 只在 `static {}` 里批量注册，见 `ModSounds.java:139-141`）。

**语言键**（`en_us.json` 必补，`zh_cn.json` 强烈建议；`hr_hr.json`/`ko_kr.json` 可选）
1. `"block.csrp.snow_short_grass": "Snowy Tall Grass"`（1.10.9 `lang/en_us.lang:698`）
2. `"block.csrp.snow_tall_grass": "Snowy Double Tall Grass"`（`:697`）
3. `"block.csrp.deadhead_grass_short": "Short Deadhead Vines"`（`:739`）
4. `"block.csrp.deadhead_grass_tall": "Long Deadhead Vines"`（`:740`）
5. `"options.csrp.fractured_terrain": "Fractured Terrain"` + `.enabled` / `.disabled` / `.description`（1.10.9 `:2767-2774`）
6. `"options.csrp.mushroom_trees": "Enable Mushroom Trees"` + `.enabled` / `.disabled` / `.description`（1.10.9 `:2776-2782`）
7. `snow_covered_grass` **无 lang 键**（1.10.9 未注册其 item）→ 无需新增。

> 「18 个音效/语言键」核对：**音效键只有 1 个**（`blizzard_reverse`）；**语言键**约 21 行（4 方块名 + 6×2 世界设置 + ≥3 star tooltip）。按上表逐键补齐即不遗漏。

### 5.9 `WorldGenDeadheadTreeStructure` + 4 个 `deadhead_tree_large_*.nbt`
- 4 个 `.nbt` → `src/main/resources/data/csrp/structures/`（与既有 `meteor*.nbt`、`dh_village_*.nbt` 同目录）。
- 新建 `alku/csrp/world/DeadheadTreePlacer.java`，**必须放 `alku/csrp/world/` 包**（`MeteorStructureLoader` 是包私有 `final class`，`MeteorStructureLoader.java:27`）。
- 两种做法（推荐前者）：①把 `MeteorStructureLoader.load(ServerLevel, String)` 从 `private static` 放宽为 `static`（1 个词），复用它（含 `rewriteLegacyIds`）；②在 `DeadheadTreePlacer` 里复制其 `load` 实现（约 20 行）。
- 锚点：`_1` = `(4,0,5)`、`_2` = `(5,0,5)`、`_3` = `(4,0,5)`、`_4` = `(6,0,6)`（**【核实】1.10.9 `TREES` 常量**）。**锚点偏移须手动加到 origin**（按 `Rotation` 旋转后 `origin.offset(...)`；T16）。
- 触发点：`ColdStarTreeHandler.decorate(level, cx, cz)`，挂在 `StarBiomeGenerationEvents.convertNewChunk` 的 COLD 分支里、用 `level.getServer().execute(...)`（**【核实】`ChunkPos` 用 `.x`/`.z`**）。
- `mushroomTrees == false` 时树密度 ×0.5（1.10.9 `reduceNormalTreeDensity`）。
- `addSnowUnderTree`：`expectedGroundY = treeSurfacePos.y - 1`；`dx,dz ∈ [-7,7]` 且 `dx²+dz² ≤ 49`；`findGroundForSnow` 搜索 `[max(1, y-8), min(getMaxBuildHeight()-2, y+8)]`；禁用表面 = 冰/浮冰/蓝冰/液体。
- 植被替换：`SHORT_GRASS`(GRASS)/`FERN` → `SNOW_SHORT_GRASS`；`TALL_GRASS`(双高 GRASS) → 破坏上半 + 下半换成 `SNOW_TALL_GRASS`；`GRASS_BLOCK` → `SNOW_COVERED_GRASS`；flag 2 不变。
- `isTreeOrLeaves`：`state.is(BlockTags.LOGS)` / `state.is(BlockTags.LEAVES)` 或 `ModBlocks` 的树干。
- **既有缺口提示**：工程里 8 个 `dh_village_*.nbt` **无任何 Java 引用**（grep `dh_village` 零命中）——孤儿资源，不在本次范围（见 R16）。

---

## 6. 既有文件改动清单（每条写清改什么）

| # | 文件 | 改什么 |
|---|---|---|
| 1 | `gradle.properties` | 第 31 行 `mod_version=1.10.8` → `1.10.9`。**其余不动。** |
| 2 | `src/main/resources/csrp.mixins.json` | **推荐方案下不改**。仅当走 §4.1.1 兜底方案 B 时，`"client"` 数组追加 `"client.LevelRendererBlizzardMixin"`；若采纳 §4.1.3 可选优化再加 `"client.PostChainAccessorMixin"`。**片 1–3 不改。** |
| 3 | `src/main/java/alku/csrp/registry/ModBlocks.java` | 在 `DEADHEAD_LEAVES`（第 503-509 行）之后追加 5 个 `RegistryObject`（见 §5.3）。 |
| 4 | `src/main/java/alku/csrp/registry/ModItems.java` | 追加 4 个 `ITEMS.registerSimpleBlockItem("id", ModBlocks.XXX)`（第 627-628 行附近最自然）。 |
| 5 | `src/main/java/alku/csrp/registry/ModSounds.java` | 第 126-128 行 `DARK_DAYS_*` 之后追加 `BLIZZARD_REVERSE = register("blizzard_reverse");` |
| 6 | `src/main/resources/assets/csrp/sounds.json` | 追加 `"blizzard_reverse"` 条目（见 §5.8），建议紧跟 `"celestial.dark_days.rumble"`（第 52-60 行）之后。⚠ 文件已 6503 行，改完必须用 `python -c "import json;json.load(open('...',encoding='utf-8'))"` 校验语法（R12）。 |
| 7 | `src/main/java/alku/csrp/network/CsrpNetwork.java` | `register()` 末尾（第 53-58 行 `MeteorShakePayload` 之后）追加 `BlizzardReversePayload` 的 `registerMessage(id++, ...)`，新 id = 9。 |
| 8 | `src/main/java/alku/csrp/celestial/network/StarWorldStatePayload.java` | **不改**（推荐：用独立 `BlizzardReversePayload`）。若选「扩 record」方案则加第二分量 `boolean blizzardReversed` 并同步改 `SrpStarWorldEvents.sync`。**二选一。** |
| 9 | `src/main/java/alku/csrp/celestial/client/StarWorldClientState.java` | 追加 `public static boolean isCold()` / `isWarm()`（基于第 6 行 `starType`）。 |
| 10 | `src/main/java/alku/csrp/world/SrpWorldData.java` | +2 字段 `mushroomTrees`/`fracturedTerrain`；`load` 读 `"mushroom_trees"`/`"fractured_terrain"`；`save` 写同键；+4 方法 `mushroomTreesEnabled()`/`setMushroomTreesEnabled(boolean)`/`fracturedTerrainEnabled()`/`setFracturedTerrainEnabled(boolean)`；`setStarType`（第 176 行）加「非 COLD 清 `fracturedTerrain`」；`DATA_VERSION`（第 21 行）5 → 6；`initialize`（第 650 行）从 `SrpStarWorldSelection.consume()` 读取；`reset`（第 626 行）清字段。 |
| 11 | `src/main/java/alku/csrp/world/SrpStarWorldEvents.java` | 追加 `@SubscribeEvent public static void playerRespawn(PlayerEvent.PlayerRespawnEvent event)` → `sync(player)`。 |
| 12 | `src/main/java/alku/csrp/world/StarBiomeGenerationEvents.java` | `convertNewChunk`（第 42-63 行）内：①`replaceBiomes` 之前调 `FracturedTerrainHandler.fractureChunk(...)`；②COLD 分支里 `level.getServer().execute(() -> ColdStarTreeHandler.decorate(level, cx, cz))`，再按 `ColdStarVillageGenerator.isVillageChunk` 决定是否生成村子。 |
| 13 | `src/main/java/alku/csrp/world/SrpStarWorldSelection.java`（新） | 仿 `SrpStarTypeSelection`/`SrpMeteorSelection` 的 `AtomicReference` 模式，stage/consume 两个布尔。 |
| 14 | `src/main/java/alku/csrp/client/SrpDifficultyScreenEvents.java` | 追加两个 `CycleButton<Boolean>`（仅 `SrpStarType.COLD` 时 `visible`），`WeakHashMap` 记录，`onCreate` 前（第 100-102 行）stage。**可选片 6。** |
| 15 | `assets/csrp/lang/{en_us,zh_cn}.json`（+ 可选 `hr_hr`/`ko_kr`） | 追加 §5.8 第 1–6 条键。 |
| 16 | `src/main/java/alku/csrp/world/MeteorStructureLoader.java` | **1 个词**：第 64 行 `private static StructureTemplate load(...)` → `static StructureTemplate load(...)`（可选）。 |
| 17 | `src/main/java/alku/csrp/Csrp.java` | **不必须改**。若要加创造标签展示项，在第 138-185 行 `displayItems` 与第 221-351 行 `addCreativeItems` 各追加 4 个 `output.accept(...)`。 |
| 18 | `src/main/java/alku/csrp/client/ClientModEvents.java` | **推荐不改**（`BlizzardDimensionEffects` 用 `RegisterDimensionSpecialEffectsEvent` 注册，放独立类 `BlizzardDimensionEffectsEvents`）。 |
| 19 | `src/main/java/alku/csrp/celestial/client/StarWorldShaderEvents.java` | **不改**（复用它作为写 uniform 的公共入口；若新增 `blizzard_reverse` shader，则在该类里并列处理或新建 `BlizzardShaderEvents` 复用同一反射工具）。 |

---

## 7. 实施分片（5 片必做 + 1 片可选，每片结束必须 `gradlew.bat build` 通过）

### 片 1：骨架与注册（B 阶段核心，~14 个文件）
**目标**：编译通过；jar 内出现新方块/物品/音效/版本号。

- M `gradle.properties`（版本号）
- N `src/main/java/alku/csrp/block/DeadheadGrassShortBlock.java`（骨架：`BushBlock` + 5 纹理 `IntegerProperty` + `getShape` + `canSurvive`）
- N `src/main/java/alku/csrp/block/DeadheadGrassTallBlock.java`（骨架）
- N `src/main/java/alku/csrp/block/SnowGrassBlock.java`（基类骨架）
- N `src/main/java/alku/csrp/block/SnowShortGrassBlock.java`
- N `src/main/java/alku/csrp/block/SnowTallGrassBlock.java`
- N `src/main/java/alku/csrp/block/SnowCoveredGrassBlock.java`
- M `src/main/java/alku/csrp/registry/ModBlocks.java`（5 注册）
- M `src/main/java/alku/csrp/registry/ModItems.java`（4 个 `registerSimpleBlockItem`）
- M `src/main/java/alku/csrp/registry/ModSounds.java`（`BLIZZARD_REVERSE`）
- N `src/main/java/alku/csrp/world/SrpStarWorldSelection.java`
- M `src/main/java/alku/csrp/world/SrpWorldData.java`（2 字段 + `DATA_VERSION` 6 + 4 方法）

**验证**：`build` 通过；`tar -tf build/libs/csrp-1.10.9.jar | grep mods.toml` 后确认展开内容 `version` 为 `1.10.9`。

### 片 2：资源导入（43 个资产 + 语言键，零 Java）
- N `assets/csrp/blockstates/` 5 个；`models/block/` 18 个；`models/item/` 4 个；`textures/block/` 11 个 png；`sounds/misc/snow_reversal.ogg`；`data/csrp/structures/deadhead_tree_large_1..4.nbt`
- M `assets/csrp/sounds.json`、`assets/csrp/lang/en_us.json`、`assets/csrp/lang/zh_cn.json`

**验证**：`build` 通过；`grep -rn "srparasites:" src/main/resources` = 0；`runClient` + `F3+T` 不报 `Unable to load model`；`/setblock` 放 5 个方块可见（双高草需放两格）。

### 片 3：冷星生态（服务端逻辑，无客户端）
- N `src/main/java/alku/csrp/world/DeadheadTreePlacer.java`
- N `src/main/java/alku/csrp/world/ColdStarTreeHandler.java`
- N `src/main/java/alku/csrp/world/FracturedTerrainHandler.java`
- N `src/main/java/alku/csrp/world/SnowGrassEvents.java`
- M `src/main/java/alku/csrp/world/StarBiomeGenerationEvents.java`
- M `src/main/java/alku/csrp/world/MeteorStructureLoader.java`（可选）

**验证**：`build`；`runServer` 新建 COLD 星世界 + `/tp` 到未加载区块，确认出现 DEADHEAD 树干、树下雪草/雪覆盖草、板块裂缝；无 `ClassCastException`；日志无 `Missing meteor structure structures/deadhead_tree_large_*`。

### 片 4：暴风雪客户端渲染（**最高风险，进片前先做 T2a/T2b**）
- N `src/main/java/alku/csrp/client/weather/BlizzardClient.java`
- N `src/main/java/alku/csrp/client/weather/BlizzardDirectionClient.java`
- N `src/main/java/alku/csrp/client/weather/BlizzardRenderer.java`
- N `src/main/java/alku/csrp/client/weather/BlizzardFogRenderer.java`
- N `src/main/java/alku/csrp/client/weather/BlizzardReverseSound.java`
- N `src/main/java/alku/csrp/client/weather/BlizzardClientEvents.java`
- N **`src/main/java/alku/csrp/client/BlizzardDimensionEffects.java`**（覆写 `getSunriseColor` + `renderSnowAndRain`，逐行搬运 `OverworldEffects` 实现后加暴风雪分支）
- N `src/main/java/alku/csrp/client/BlizzardDimensionEffectsEvents.java`（`@EventBusSubscriber(modid = Csrp.MODID, bus = MOD, value = Dist.CLIENT)`，`RegisterDimensionSpecialEffectsEvent` 注册）
- N（兜底，仅当 T2b 失败）`src/main/java/alku/csrp/mixin/client/LevelRendererBlizzardMixin.java` + M `csrp.mixins.json`
- N `assets/csrp/textures/environment/snow.png`（自带雪纹理）
- N（可选）`assets/csrp/shaders/post/blizzard_reverse.json` + `shaders/program/blizzard_reverse.{json,fsh}`

**验证**：`build`；`runClient` 进 COLD 星 + `/weather rain`：① 斜向雪丝；② 雾近远变化（`far = 72 - intensity*56`，且事件 `setCanceled(true)`）；③ `/time set 23000` 无橙红霞光；④ 日志无异常、注册确认可见；⑤ **S9 回归**：切到 NORMAL 星世界，天空/云/地形雾/雨雪与改动前视觉一致。

### 片 5：网络同步（暴风雪反向 + 星类型补漏）
- N `src/main/java/alku/csrp/network/BlizzardReversePayload.java`
- N `src/main/java/alku/csrp/world/BlizzardDerivedHandler.java`
- M `src/main/java/alku/csrp/network/CsrpNetwork.java`
- M `src/main/java/alku/csrp/celestial/client/StarWorldClientState.java`
- M `src/main/java/alku/csrp/world/SrpStarWorldEvents.java`

**验证**：`build`；双端 `runClient`+`runServer`，`/summon csrp:draconite`（或 `csrp:kirin`）靠近玩家，确认切换音 `blizzard_reverse` 播一次 + 「卡→黑→保持 8tick→反→回白」状态机（对照常数 `0.065/0.075/0.125/8/0.08/0.045`）；登出重登状态归位。

### 片 6（可选）：世界创建 UI 开关
- M `src/main/java/alku/csrp/client/SrpDifficultyScreenEvents.java`、M `assets/csrp/lang/*.json`

**验证**：`runClient` 新建世界 → 选 K-Type Cold Star 后两个按钮出现；创建后 `SrpWorldData` 两布尔正确落盘。

---

## 8. 验证方式与 D 阶段构建步骤

### 8.1 D 阶段构建步骤（项目自带 wrapper + GraalVM 21）
```bash
export JAVA_HOME='D:\MC\jdk\graalvm-community-openjdk-21.0.2+13.1'
export PATH="$JAVA_HOME/bin:$PATH"
cd /d/code/MC模组/csrp-1.20.1-forge
./gradlew.bat build --console=plain --no-daemon
```
- **首次构建**：先 `./gradlew.bat --version` 确认 wrapper 8.8，再 `build`（**建议超时 25 分钟**；缓存命中约 6 分钟）。
- **仅编译**：`./gradlew.bat compileJava --console=plain`（超时 8 分钟）。`build.gradle:188` 已设 `-Xmaxerrs 3000`。
- **不要跑** `runData`（本方案不引入 datagen；新结构走 `src/main/resources`）。
- **`runClient` 需图形环境**，超时 10 分钟并在 `Sound engine started` 后手动关闭。
- `generateSrgForMixinAp()`（`build.gradle:186-194`）返回 null 属正常降级。
- **环境要求**：`JAVA_HOME` 必须是 GraalVM 21；不要用 Gradle 自动下载的 toolchain JDK。

### 8.2 每片验证判据
| 片 | 验证命令 | 通过判据 | 与 1.10.9 的对照点 |
|---|---|---|---|
| 1 | `./gradlew.bat build` | BUILD SUCCESSFUL；`build/libs/csrp-1.10.9.jar` 存在 | `mcmod.info` 版本 1.10.9 ↔ `mods.toml` 的 `version` |
| 2 | `build` + `tar -tf build/libs/csrp-1.10.9.jar` + grep 关键路径 | 5 + 18 + 4 + 1 全在 jar 内；`grep -rn "srparasites:" src/main/resources` = 0 | 1.10.9 jar 内同名资产计数 43 |
| 3 | `runServer` + 生成 COLD 世界 | 日志无异常；新区块含 DEADHEAD 树干、雪草、板块裂缝 | `NORMAL_TREE_DENSITY=0.5`、`TREE_SNOW_RADIUS=7`、`PLATE_SIZE=96`、`CRACK_WIDTH=2.25`、`MIN/MAX_RAVINE_DEPTH=20/52` |
| 4 | `runClient` | 雪丝可见、雾变化、日出无霞光、**天空/云无回归**、无崩溃 | `fogEnd = 72 - intensity*56`、`fogStart = fogEnd*0.08`、`radius = 8 + floor(intensity*6)`、`laneCount = intensity>0.7?3:2`、`baseSpacing = intensity>0.55?1:2`、`alpha = intensity*(0.34+distanceFade*0.58)*(0.74+randomA*0.26)` ≤ 0.96 |
| 5 | 双端 | 靠近 Draconite/Kirin 时音效 + 变向；登出重登状态正确 | `RANGE=100`、`RANGE_SQ=10000`、每 10 tick、`LAST_STATE` 去重 |
| 6 | `runClient` 新建世界 | 两个按钮出现且落盘 | `gui.srparasites.worldsettings.{fractured,mushroom_trees}`（`lang/en_us.lang:2767-2782`） |

### 8.3 建议的 verify 脚本（D 阶段生成）
`scripts/verify-plan-1.10.9.sh`（Niubash bash）：
1. `./gradlew.bat build`；
2. `tar -tf build/libs/csrp-1.10.9.jar > /tmp/jar-listing.txt`；
3. 43 个新资产路径逐个 `grep -q`（列表硬编码）；
4. `grep -rn "srparasites:" src/main/resources/assets/csrp src/main/resources/data/csrp` 必须 0 行；
5. 若走兜底 mixin，用 `python -c` 读 `csrp.mixins.json` 断言 `"client.LevelRendererBlizzardMixin" in data["client"]`；
6. `python -c "import json;json.load(open(p,encoding='utf-8'))"` 逐个校验 `sounds.json` 与 `lang/*.json` 语法。

---

## 9. 风险清单

| R# | 风险 | 等级 | 现状 | 缓解 |
|---|---|---|---|---|
| R1 | 自定义 `DimensionSpecialEffects` **完整替换原版 `OverworldEffects`**，若委托实现不完整 → 原版天空/云/地形雾丢失 | **中（已降级）** | **T2b 已结案**：`OverworldEffects` 是 public + public 无参构造 → 可整体委托 | ①持有 `VANILLA = new DimensionSpecialEffects.OverworldEffects()`，除 `getSunriseColor`/`renderSnowAndRain` 外**全部转发**给 `VANILLA`（不再需要逐行搬运）；②片 4 验证第 ⑤ 项专门做天空/云回归检查（S9）；③兜底仍是 1 条 `LevelRenderer` mixin（方案 B），但 T2b 结案后已无触发理由 |
| R2 | `MixinExtras` 不可用（1.10.9 的 `SRPCoreMod` 显式 `MixinExtrasBootstrap.init()`，本工程未引入） | 中 | 推荐方案 **0 条 mixin**；兜底方案也只 1 条标准 `@Inject`，不含 `@Redirect`/`@ModifyExpressionValue`/`@WrapOperation` | 明确禁止使用 MixinExtras 注解 |
| R3 | `PostChain.passes` 是 private 且无 getter（**已核实**），反射字段名在 dev/prod 可能不同 | 低 | 现有 `StarWorldShaderEvents` 已有 3 候选名 + `trySetAccessible` + 降级 | 复用同款；**已核实 `passes` 是正确字段名** |
| R4 | 1.20.1 **无 `GenLayer`**、无 `SRPWorldEntitySpawner.starType` 全局静态 | 中 | 已由 `StarBiomeGenerationEvents` + `SrpWorldData` 替代 | 明确标「不移植」 |
| R5 | 1.20.1 **无 `DecorateBiomeEvent` / `PopulateChunkEvent`**，触发时机改挂 `ChunkEvent.Load`，与 1.10.9 时序不同 | 高 | 已给落点（照 `ColdStarVillageGenerator` 的成熟做法） | `hasChunk` 保护 + `server.execute`；加日志确认成功率；若 `setBlock` 在 chunk 未 `LevelChunk` 化时静默失败，改为首次被玩家加载时补做 |
| R6 | `SrpStarType` 无「未知态」（1.10.9 用 `-1`） | 低 | `StarWorldStatePayload.decode` 已用 `byValue` 兜底 `NORMAL` | 接受差异（未知态仅用于「登录前不渲染暴风雪」，默认即 `NORMAL`，行为等价） |
| R7 | `SRPColdStarTreeHandler` 依赖 1.12.2 `Biome.spawnList` 的树数量，1.20.1 生物群系生成设置已重构，**无逐生物群系「树数量」可读** | 中 | 用等价常量替代 | 标 UNVERIFIED（行为近似）；若需精确改查 `BiomeGenerationSettings#getFeatures()` 统计 `TreeFeature`（成本高，暂不做） |
| R8 | `JAVA_HOME` 必须是 GraalVM 21 | 中 | `build.gradle` toolchain 21 + `release=17` 正确 | D 阶段每条命令显式 `export JAVA_HOME` |
| R9 | 工程无 vendored jar / 无 spigot 依赖 → **无 vendored jar 风险** | 低 | 已核实 | — |
| R10 | 资产计数口径（`parasitebush_frostg*.png` 后缀未列全） | 低 | 本工程无同名文件，**无动作** | — |
| R11 | `SNOW_COVERED_GRASS` 不注册 BlockItem（与 1.10.9 一致）；3 个草方块注册 | 低 | 已对齐 1.10.9 | — |
| R12 | `sounds.json` 已 6503 行，手改 JSON 有语法风险（解析失败 → **全部**音效消失） | 中 | — | 改完用 `python -c "import json;json.load(...)"` 校验；`runClient` 确认既有音效仍在 |
| R13 | `snow_short_grass.json` / `snow_tall_grass.json` 的变体键 `"normal"` 是 1.12.2 写法，1.20.1 必须 `""` | 中 | 已在 §3.5 列为必须人工修正 | 片 2 逐个检查 5 个 blockstate 的变体键 |
| R14 | 1.10.9 硬编码高度 `4..238`/`1`/`255`，而 1.20.1 主世界是 −64..320 | 中 | 已在 §5.5 要求改为 `getMinBuildHeight()`/`getMaxBuildHeight()` | 片 3 观察边界处是否出现未处理地形 |
| R15 | `web_fetch` 全域被拒（§10.3），但本地真机核实路径已完全打通（§10.4） | 低 | 已解决：`javap` + Forge sources jar + ForgeFlower 反编译全部可用 | B 阶段一律走本地 jar |
| R16 | 工程 8 个 `dh_village_*.nbt` 是孤儿资源（无 Java 引用） | 低 | 已记录 | 本次只保证 `deadhead_tree_large_*.nbt` 被加载；`dh_village_*` 单独立项 |
| R17 | `blockstates/snow_covered_grass.json` 引用原版模型 `minecraft:grass_snowed`，1.20.1 名称待确认（T22） | 低 | 已给两种写法 | 最稳：复制到 `assets/csrp/models/block/snow_covered_grass.json` 写 `{"parent":"minecraft:block/grass_block_snow"}` |
| R18 | **原版雪草模型名 `grass_block_snow` 是推测**（T22）；若错，雪覆盖草方块显示 missing model（不崩） | 低 | 待核实 | 片 2 用 `build` + `runClient` 目视确认；备选：给 `SnowCoveredGrassBlock` 写一个用 `csrp:block/` 纹理的 `cube_bottom_top` 模型 |
| R19 | **网上 1.20.1 示例大量混入 1.20.2+/1.21 API**（本轮已实测踩中 3 处：`FogRenderer.setupColor/setupFog` 尾参、`DimensionSpecialEffects` 的 sunrise 构造参数与 `getFogColor()`、`gameRenderer.getFogColor()`） | 中 | 已全部纠正 | 铁律：**任何 1.20.1 签名必须对本地官方映射 jar 做 `javap` 或读 Forge sources，不得引用网上示例** |
| R20 | T2a（`RegisterDimensionSpecialEffectsEvent` 第一参数类型）错会导致注册表键不匹配 → 冷星走原版雨雪（不崩但功能不生效） | **已消除** | **T2a 已结案**：第一参数 = `net.minecraft.resources.ResourceLocation`，注册键 = `new ResourceLocation("minecraft","overworld")` | 落码时仍加一条 `LOGGER.info` 确认注册发生；退路（方案 B mixin）已无必要 |

---

## 10. 查证记录

### 10.1 已核实（本轮真机核实，证据可复现）

**核实工具与方法**：`javap -p -classpath <官方映射 jar>`、Forge sources jar 全文读 + 全文正则扫描、**ForgeFlower 2.0.629.0 反编译**（在 Gradle 缓存内）。

**关键点（务必遵守）**：
- ✅ 官方映射 ground truth = `...\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1.jar`（8614 类，官方名，MC + patch 后的 Forge 在同一 jar）
- ❌ **`mcp_repo\...\joined\rename\output.jar` 只有 SRG 名**（`m_109018_`、`f_109010_`），**不能**当官方名来源（只能用来查 SRG 名 → 用于 T1′）

| 结论 | 证据 |
|---|---|
| `LevelRenderer`：**`private void renderSnowAndRain(LightTexture, float, double, double, double)`**，描述符 `(Lnet/minecraft/client/renderer/LightTexture;FDDD)V`；体首 `getRainLevel(partialTick) <= 0` 直接 return；整个体被 `if (!level.effects().renderSnowAndRain(...))` 守卫；另有 `public void tickRain(Camera)`；雪纹理常量 `"textures/environment/snow.png"` | 【核实】javap + 反编译 |
| `FogRenderer.setupColor(Camera, float, ClientLevel, int, float)`（**5 参**）、`setupFog(Camera, FogMode, float, boolean, float)`（**5 参**）；`FogMode` = `FOG_SKY`/`FOG_TERRAIN`；`FogShape` = `SPHERE`/`CYLINDER`；距离最终经 `RenderSystem.setShaderFogStart/End/Shape`；`ForgeHooksClient.onFogRender(...)` 是最后一条语句（cancel 时才应用） | 【核实】javap + 反编译 |
| `GameRenderer`：`loadEffect(ResourceLocation)`、`currentEffect(): PostChain`、`shutdownEffect()` 等；**无雾字段、无 `getFogColor()`** | 【核实】javap + grep |
| `PostChain`：**`private final List<PostPass> passes;`（private，无 getter，也无 `getEffect()`）**；公开 API `PostChain(TextureManager,ResourceManager,RenderTarget,ResourceLocation)`、`getTempTarget`、`addTempTarget`、`addPass`、`resize`、`process(float)`、`getName`、`close` | 【核实】javap + 反编译 |
| `PostPass`：`private final EffectInstance effect;` + **`public EffectInstance getEffect();`**；`public final RenderTarget inTarget/outTarget`；`PostPass(ResourceManager,String,RenderTarget,RenderTarget)`、`addAuxAsset`、`setOrthoMatrix`、`process(float)`、`getName`、`close` | 【核实】javap |
| `EffectInstance#safeGetUniform(String)` → `com.mojang.blaze3d.shaders.AbstractUniform`；`Uniform#set(float)` 是 `public final void`（另有 (float,float)/(int,float)/(float,float,float)/(Vector3f)/(float,float,float,float)/(Vector4f)/(int…)/(float[])/(Matrix4f)/(Matrix3f)/setSafe） | 【核实】javap |
| `DimensionSpecialEffects`：`implements IForgeDimensionSpecialEffects`；构造 `(float cloudLevel, boolean hasGround, SkyType, boolean forceBrightLightmap, boolean constantAmbientLight)`（**无 sunrise 数组参数、无 `getFogColor()`**）；`private final float[] sunriseCol`；**`@Nullable public float[] getSunriseColor(float timeOfDay, float partialTick)`**；`getCloudHeight()`、`hasGround()`、抽象 `getBrightnessDependentFogColor(Vec3,float)`、抽象 `isFoggyAt(int,int)`、`skyType()`、`forceBrightLightmap()`、`constantAmbientLight()`；`SkyType` = `NONE`/`NORMAL`/`END` | 【核实】javap + 反编译 |
| **`getSunriseColor` 返回 `null` 就是隐藏日出霞光的唯一途径**：`FogRenderer.setupColor` 是唯一消费者（`float[] a = level.effects().getSunriseColor(...); if (a != null) { t *= a[3]; fogRed = … }`）；方法体在 `Mth.cos(timeOfDay*2π) ∈ [-0.4,0.4]` 时返回 `sunriseCol`，否则返回 `null` | 【核实】反编译 |
| **Forge 1.20.1 全库无任何 sunrise/sunset 钩子**（对 sources jar 全部 `.java` 做 `(?i)sunrisecolor\|sunset\|getSunriseColor` 全文扫描 = **0 命中**）；`DimensionSpecialEffects` 的 Forge patch 只加了 `implements IForgeDimensionSpecialEffects` 并把 `forType` 改到 `DimensionSpecialEffectsManager` | 【核实】Forge sources 全文扫描 |
| `IForgeDimensionSpecialEffects`：`renderClouds`、`renderSky`、**`renderSnowAndRain(ClientLevel, int, float, LightTexture, double, double, double)`**、`tickRain(ClientLevel, int, Camera)`、`adjustLightmapColors(...)`，全为 default | 【核实】Forge sources |
| `RenderLevelStageEvent` getters：`getStage/getLevelRenderer/getPoseStack/getProjectionMatrix/getRenderTick/getPartialTick/getCamera/getFrustum`；**`Stage` 完整列表**：AFTER_SKY、AFTER_SOLID_BLOCKS、AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS、AFTER_CUTOUT_BLOCKS、AFTER_ENTITIES、AFTER_BLOCK_ENTITIES、AFTER_TRANSLUCENT_BLOCKS、AFTER_TRIPWIRE_BLOCKS、AFTER_PARTICLES、AFTER_WEATHER、AFTER_LEVEL | 【核实】Forge sources |
| `ViewportEvent`：`RenderFog` 有 `getMode/getType/getFarPlaneDistance/getNearPlaneDistance/getFogShape/setFarPlaneDistance/setNearPlaneDistance/setFogShape/scaleFarPlaneDistance/scaleNearPlaneDistance`，**必须 cancel 才生效**；`ComputeFogColor(Camera, float, float, float, float)`（**5 参，无 FogMode/boolean**）；`ComputeCameraAngles(...)`；`ComputeFov(...)` 有 `getFOV/setFOV/usedConfiguredFov` | 【核实】Forge sources |
| `RenderGuiEvent`：抽象 `getWindow/getGuiGraphics/getPartialTick`；`Pre/Post(Window, GuiGraphics, float)`；另有 `RenderGuiOverlayEvent.Pre/Post(Window, GuiGraphics, float, NamedGuiOverlay)` + `getOverlay()` | 【核实】Forge sources |
| `RegisterClientReloadListenersEvent` **存在**（`IModBusEvent`）：`registerReloadListener(PreparableReloadListener)` | 【核实】Forge sources |
| `BlockEvent.EntityPlaceEvent(BlockSnapshot, BlockState placedAgainst, @Nullable Entity)`，`@Cancelable`；**旧状态 = `getBlockSnapshot().getReplacedBlock()`，新状态 = `.getCurrentBlock()`；`BlockSnapshot.getState()` 在 47.4.23 不存在**；`NeighborNotifyEvent(Level, BlockPos, BlockState, EnumSet<Direction>, boolean)` | 【核实】Forge sources |
| `ChunkEvent extends LevelEvent`（`ChunkEvent(ChunkAccess)`/`(ChunkAccess, LevelAccessor)` + `getChunk()`）；`TickEvent.LevelTickEvent` 有 `public final Level level;` + `Phase` START/END + `haveTime()` | 【核实】Forge sources |
| `Level#getRainLevel(float)` / `getThunderLevel(float)` / `isRaining()` / `isRainingAt(BlockPos)` 在 `Level` 上（`ClientLevel` 未声明，继承可用） | 【核实】javap |
| `getHeightmapPos(Heightmap.Types, BlockPos)` 是 **`LevelReader` 的 default 方法**；`LevelReader#getHeight(Heightmap.Types,int,int)` 抽象；`Heightmap.Types` 含 `MOTION_BLOCKING`、`MOTION_BLOCKING_NO_LEAVES` | 【核实】javap |
| `BushBlock#mayPlaceOn(BlockState, BlockGetter, BlockPos)` 是 1.20.1 名（protected）；`canSurvive(BlockState, LevelReader, BlockPos)` public | 【核实】javap |
| `SnowyDirtBlock` 有 `public static final BooleanProperty SNOWY`；**`GrassBlock extends SpreadingSnowyDirtBlock implements BonemealableBlock`**（不是直接 extends `SnowyDirtBlock`），但 `SNOWY` 仍可用 | 【核实】javap |
| `DoublePlantBlock`：`public static final EnumProperty<DoubleBlockHalf> HALF` + `placeAt(LevelAccessor,BlockState,BlockPos,int)` + `copyWaterloggedFrom` + `playerWillDestroy(Level,BlockPos,BlockState,Player)` + `protected static preventCreativeDropFromBottomPart(...)` | 【核实】javap |
| `TallGrassBlock extends BushBlock implements BonemealableBlock, IForgeShearable`；**未声明 `canSurvive`/`getStateForPlacement`**（继承）→ **`IForgeShearable` 的正确签名可直接从它照抄** | 【核实】javap |
| `BlockBehaviour`：`getRenderShape(BlockState)`、`getShape(BlockState, BlockGetter, BlockPos, CollisionContext)`；`Properties` 静态 `of()`/`copy(...)` + `noOcclusion/noCollission/instabreak/randomTicks/strength/sound/offsetType/replaceable/mapColor/lightLevel` | 【核实】javap |
| `RenderShape` = `INVISIBLE`/`ENTITYBLOCK_ANIMATED`/`MODEL`；`RenderType.cutout()/cutoutMipped()/solid()/translucent()/tripwire()`；`ItemBlockRenderTypes.setRenderLayer(Block, RenderType)` 可用 | 【核实】javap |
| `BlockTags` 有 `LEAVES`/`LOGS`/`DIRT`/`SNOW`/…；**没有 `BlockTags.SNOWY`** | 【核实】javap |
| `DeferredRegister`：`create(IForgeRegistry, String)` / `create(ResourceKey<Registry>, String)` / `create(ResourceLocation, String)`；`register(String, Supplier)`；`register(IEventBus)`；`RegistryObject` 有 `get/getId/getKey/isPresent/ifPresent/map/orElse/orElseGet/orElseThrow/getHolder` | 【核实】javap |
| `RegisterDimensionSpecialEffectsEvent` **存在**（MOD 总线、客户端），`register(?, DimensionSpecialEffects)` —— 第一参数类型待 T2a | 【核实】Forge sources |
| **Mixin 基础设施已在工程内工作**：`csrp.mixins.json`（`required:true`、`package alku.csrp.mixin`、5 个 client mixin、`refmap: csrp.refmap.json`、`compatibilityLevel JAVA_17`）；jar manifest 有 `MixinConfigs`（`build.gradle:138`）；Mixin AP 被禁用（`build.gradle:98-99`）→ 无 refmap，按官方名解析；既有 mixin 全用 `method = {"官方名","m_XXXXXX_"}` + `require = 0` | 【核实】工程文件实读 |
| `mod_version` 经 `generateModMetadata` 展开进 `mods.toml`（`build.gradle:103-127` + `mods.toml:10`） | 【核实】工程文件实读 |
| `BlockItem` 注册 = `ITEMS.registerSimpleBlockItem("id", ModBlocks.XXX)` | 【核实】工程 `ModItems.java:365-651` |
| 模型 json 顶层 `"render_type"` 键在 1.20.1 可用 | 【核实】工程 `models/block/deadhead_leaves_snow.json:3` |
| 无属性方块 blockstate 变体键为空串 `""` | 【核实】工程 `blockstates/gothshroom.json:1` |
| 自定义 NBT 结构加载路径 + `srparasites:` 运行时改写 + `BLOCK_RENAMES`（`parasitetrunk` 无条目 → 直达） | 【核实】工程 `MeteorStructureLoader.java:1-121` |
| 工程既有 `StarWorldShaderEvents` 已跑通 post-shader 反射 + `loadEffect` 路线 | 【核实】工程 `StarWorldShaderEvents.java:42-174` |
| 1.10.9 的 24 个新类语义与全部常数 | 【核实】逐文件读 `_srp-orig/decomp-1.10.9/dhanantry/scapeandrunparasites/**` |
| 1.10.9 资产内部结构（含 `blockstates/snow_covered_grass.json` 全文、`snow_short_grass.json` 的 `normal` 数组变体、`models/block/*.json` 的 `parent`/纹理路径、`models/item/*.json`） | 【核实】读 `_srp-orig/jar/assets/srparasites/**` |
| 1.10.9 `sounds.json` 的 `blizzard_reverse` 原文（`:37-45`）与 `sounds/misc/snow_reversal.ogg` 存在 | 【核实】读文件 |
| 1.10.9 `lang/en_us.lang` 新增键精确行号：`:697-698`、`:739-740`、`:2763-2782` | 【核实】读文件 |
| 1.10.9 `SRPStarWorldData` 两个新开关语义（`fracturedTerrain` 只在 COLD 有效） | 【核实】`SRPStarWorldData.java:38-92` |
| `EntityHeblu`(1.10.9) ↔ 工程 `DraconiteEntity`；`EntityKirin` ↔ `KirinEntity` | 【核实】`ModelTabula_draconite.java:9` 注释 + `KirinEntity.java:55`/`DraconiteEntity.java:35` |

### 10.1′ B 阶段（片 1）新增核实结论（同见 §4.3 T23–T27）

| 结论 | 证据 |
|---|---|
| `BlockStateProperties.DOUBLE_BLOCK_HALF`：属性名 `half`，取值 `DoubleBlockHalf.UPPER`/`LOWER`，序列化名 `upper`/`lower` | 【核实】`javap -c` 官方映射 jar：`DoubleBlockHalf.getSerializedName()` 字节码 `ldc "upper"`/`ldc "lower"`；`BlockStateProperties` 静态块 `ldc_w "half"` + `EnumProperty.create` |
| `IForgeShearable` = `isShearable(ItemStack, Level, BlockPos)` + `onSheared(Player, ItemStack, Level, BlockPos, int)` | 【核实】`javap` 官方映射 jar |
| `GrassBlock extends SpreadingSnowyDirtBlock extends SnowyDirtBlock`；`SNOWY` 已由 `SnowyDirtBlock.createBlockStateDefinition` 注册 | 【核实】`javap` |
| `Block` 无 `codec()` 成员（在 1.20.1 官方映射 jar 中） | 【核实】`javap -p net.minecraft.world.level.block.Block` |
| `DoublePlantBlock.preventCreativeDropFromBottomPart` 是 `protected static`（外部子类不可见） | 【核实】`javap -p` |
| `Block.box(double×6)`、`CollisionContext` 为接口、`BushBlock.mayPlaceOn(BlockState, BlockGetter, BlockPos)` protected | 【核实】`javap` |
| **片 1 构建结果：`./gradlew.bat build` BUILD SUCCESSFUL（GraalVM 21.0.2，Gradle 8.8），产出 `build/libs/csrp-1.10.9.jar`，jar 内 `META-INF/mods.toml` 的 `version = "1.10.9"`** | 【核实】片 1 实测 |

### 10.2 未能核实（**已标 `[待查证]`，禁止凭空补全**）
见 §4.3 的 T1′、T2a、T2b、T5、T6、T12、T13、T15、T16、T17、T18、T21、T22。

### 10.3 本轮尝试过但失败/受限的途径（记录以免重复踩坑）
- `web_fetch` 全域被拒：`raw.githubusercontent.com` / `github.com` / `mcstreetguy.github.io` / `lexxie.dev` / `nekoyue.github.io` 一律返回 `Error: URL hostname "..." resolves to a non-public IP address`。
- `web_search` 可用但**只返回来源 URL 列表、无正文** → 只能做「某页存在」的存在性佐证，不能作签名证据。
- 主代理会话**无 shell 工具**（`bash` → `Error: unknown tool "bash"`）→ 已委派具备 shell 的子代理完成核实（成功）。
- 最初凭记忆/网上示例写的 3 条 1.20.1 签名假设中有 **2 处实际是 1.20.2+/1.21 形态**（`FogRenderer.setupColor`/`setupFog` 的尾参；`DimensionSpecialEffects` 的 sunrise 构造参数与 `getFogColor()`）——**已全部纠正**。教训见 R19。
- 1.12.2 反编译源码目录 `D:\code\MC模组\模组反编译器\decompiled\[逃逸：寄生体] SRParasites-1.10.8` glob 报 `os error 3`（路径含方括号/中文名）→ **未能做 1.10.8/1.10.9 逐行差异对比**；本次差异以「用户给的文件级 diff 清单 + 1.10.9 源码实读」为依据。

### 10.4 本地核实素材（B/D 阶段无需联网）

**官方映射 ground truth（首选，官方名）**
```
C:\Users\P傲娇34\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1.jar
```
**Forge 源码（读 `.java` 正文）**
```
C:\Users\P傲娇34\.gradle\caches\forge_gradle\maven_downloader\net\minecraftforge\forge\1.20.1-47.4.23\forge-1.20.1-47.4.23-sources.jar
C:\Users\P傲娇34\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23\forge-1.20.1-47.4.23-inject_src.jar
```
**SRG 名（仅查 T1′ 用；⚠ 只有 SRG 名，无官方名）**
```
C:\Users\P傲娇34\.gradle\caches\forge_gradle\mcp_repo\de\oceanlabs\mcp\mcp_config\1.20.1-20230612.114412\joined\rename\output.jar
C:\Users\P傲娇34\.gradle\caches\forge_gradle\mcp_repo\de\oceanlabs\mcp\mcp_config\1.20.1-20230612.114412\joined\downloadClient\client.jar
```
**已完成的核实产物**
```
C:\Users\P傲娇34\AppData\Local\Temp\verify1201\REPORT.md      # 完整核实报告
C:\Users\P傲娇34\AppData\Local\Temp\verify1201\dec\           # ForgeFlower 反编译的 .java
C:\Users\P傲娇34\AppData\Local\Temp\verify1201\official\      # 抽出的官方映射类
```
`javap`：`D:\MC\jdk\graalvm-community-openjdk-21.0.2+13.1\bin\javap.exe`
ForgeFlower：在 Gradle 缓存内（版本 2.0.629.0）

### 10.5 C 阶段（片 3 冷星生态）新增核实结论

全部用 `javap -p -classpath <官方映射 jar>` 真机核实（jar 路径同 §10.4）。

| 结案项 | 结论 | 对片 3 代码的影响 |
|---|---|---|
| **T5（高度图）** | `ChunkAccess` 有 `public abstract BlockState setBlockState(BlockPos, BlockState, boolean)`、`public int getHeight(Heightmap.Types, int, int)`、`protected final Map<Heightmap.Types, Heightmap> heightmaps`、`getOrCreateHeightmapUnprimed`/`setHeightmap`；`LevelChunk` 公开覆写 `getBlockState(BlockPos)` 与 `setBlockState(...)` | 1.10.9 `chunk.func_76603_b()`（重算高度图）**无等价调用是安全的**：`LevelChunk.setBlockState` 自带 `heightmaps` 维护。`FracturedTerrainHandler` 用 `chunk.setBlockState(pos, state, false)` + `chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)`（对应 `func_76611_b`） |
| **T16（旋转锚点）** | `public static BlockPos StructureTemplate.calculateRelativePosition(StructurePlaceSettings, BlockPos)`；`public static BlockPos transform(BlockPos, Mirror, Rotation, BlockPos)`；`public Vec3i getSize()`；`public BoundingBox getBoundingBox(StructurePlaceSettings, BlockPos)` | `DeadheadTreePlacer` 用它替代 1.12.2 `Template.func_186266_a`（锚点 `(4,0,5)` 等经 rotation 后加到 origin） |
| **T18（BlockState 判定）** | `BlockState` 有 `public boolean isSolid()`、`liquid()`、`blocksMotion()`、`canBeReplaced()`、`isAir()`、`hasBlockEntity()`、`isFaceSturdy(BlockGetter, BlockPos, Direction)` | `ColdStarTreeHandler`/`FracturedTerrainHandler` 的 `Material` 判定按此改写 |
| **getDrops 覆盖点（新）** | 覆写点是 **`BlockBehaviour.getDrops(BlockState, LootParams.Builder) : List<ItemStack>`**（`BlockBehaviour$BlockStateBase.getDrops(LootParams$Builder)` 无 BlockState 参数）。`Block` 上只有 **`public static`** 的同名便捷方法，**不可覆写** | 无掉落的 `DeadheadGrass*Block`（返回 `List.of()`）与 1/8 掉种子的 `SnowGrassBlock`、掉泥土的 `SnowCoveredGrassBlock` 均按此签名覆写；`LootParams.getLevel()` 返回 `ServerLevel`（可 `getRandom()`） |
| **`MultiPlayerGameMode` 之外** | `LevelReader` 有 `public default boolean hasChunksAt(BlockPos, BlockPos)`；`LevelAccessor` 有 `public default boolean hasChunk(int, int)`；`Level` **不**直接声明 `hasChunksAt`/`canSeeSky`（后者在 `LevelHeightAccessor` 上） | `DeadheadTreePlacer` 用 `level.hasChunksAt(min, max)`；`SnowGrassEvents` 用 `level.hasChunk(x >> 4, z >> 4)` + `level.canSeeSky(pos)` |
| **`Block.asItem()`** | `public Item asItem()` 存在于 `net.minecraft.world.level.block.Block` | 剪切/`getCloneItemStack` 直接 `new ItemStack(asItem())`，无需 `Item.BY_BLOCK` |
| **`Biome`** | `public float getBaseTemperature()`（无参）、`public Precipitation getPrecipitationAt(BlockPos)`、`Precipitation` = `NONE/RAIN/SNOW` | `SnowGrassEvents.canSnowHere` 的 T6 落地 |
| **`Level` 药丸** | `isRaining()`、`getGameTime()`、`getRainLevel(float)`、`isRainingAt(BlockPos)`、`setBlock(BlockPos,BlockState,int)`、`getBlockState`、`isClientSide()` 全在 `Level` 上；`ServerLevel.players()` 返回 `List<ServerPlayer>` | `SnowGrassEvents.onLevelTick` 的 T17 落地 |
| **`BlockEvent.EntityPlaceEvent`** | 构造 `(BlockSnapshot, BlockState, Entity)`；`getPlacedBlock()`、`getBlockSnapshot()`、`getEntity()`、`getPlacedAgainst()`；`BlockSnapshot.getReplacedBlock()`（旧状态）与 `getCurrentBlock()` **均存在**，`getState()` 不存在 | `SnowGrassEvents.onSnowPlaced` 读旧状态 |
| **1.10.9 结构 NBT 实读（新增证据）** | `deadhead_tree_large_1..4.nbt` 的 palette 只含 `csrp:parasitetrunk`（仅 `axis` 属性）、`csrp:deadhead_leaves`、`csrp:deadhead_grass_short`、`csrp:deadhead_grass_tall`（`half`）、`minecraft:snow`、`minecraft:air`。**原 1.12.2 的 `parasitetrunk` 带 `variant=deadhead`，转换时丢失** | 片 3 不能按 `variant` 属性识别死头树干；`ColdStarTreeHandler.isDeadheadTrunk` 退化为「方块就是 `csrp:parasitetrunk`」+ 注册名含 `deadhead` 的兜底（UNVERIFIED 行为近似，已在代码注释标注） |
| **片 3 构建结果** | `./gradlew.bat build`（GraalVM 21.0.2 + Gradle 8.8，`--no-daemon`）**BUILD SUCCESSFUL**；jar 内新增 `alku/csrp/world/{ColdStarTreeHandler,DeadheadTreePlacer,FracturedTerrainHandler,SnowGrassEvents}.class` 与 4 个 `data/csrp/structures/deadhead_tree_large_*.nbt` | 若 `build/libs/csrp-1.10.9.jar` 被运行中的 JVM 占用，`:jar` 会以 `Could not add file ... to ZIP` 失败（**与源码无关**）→ 先关闭占用进程或删除该 jar 再构建 |

示例命令：
```bash
export JAVA_HOME='D:\MC\jdk\graalvm-community-openjdk-21.0.2+13.1'
MC_JAR='C:\Users\P傲娇34\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_official_1.20.1\forge-1.20.1-47.4.23_mapped_official_1.20.1.jar'
SRG_JAR='C:\Users\P傲娇34\.gradle\caches\forge_gradle\mcp_repo\de\oceanlabs\mcp\mcp_config\1.20.1-20230612.114412\joined\rename\output.jar'
"$JAVA_HOME/bin/javap" -p -classpath "$MC_JAR" 'net.minecraft.client.renderer.DimensionSpecialEffects$OverworldEffects'   # T2b
"$JAVA_HOME/bin/javap" -p -classpath "$MC_JAR" net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent       # T2a
"$JAVA_HOME/bin/javap" -p -classpath "$MC_JAR" net.minecraft.world.level.chunk.LevelChunk | grep -i heightmap             # T5
"$JAVA_HOME/bin/javap" -p -classpath "$MC_JAR" net.minecraft.world.level.biome.Biome | grep -i -E "temperature|precipitation"  # T6
"$JAVA_HOME/bin/javap" -p -classpath "$MC_JAR" com.mojang.blaze3d.vertex.DefaultVertexFormat                              # T13
"$JAVA_HOME/bin/javap" -p -classpath "$MC_JAR" net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate  # T16
"$JAVA_HOME/bin/javap" -p -classpath "$MC_JAR" net.minecraftforge.event.TickEvent                                          # T17
"$JAVA_HOME/bin/javap" -p -classpath "$MC_JAR" net.minecraft.world.level.block.state.BlockState                           # T18
"$JAVA_HOME/bin/javap" -p -classpath "$MC_JAR" net.minecraft.client.resources.sounds.AbstractTickableSoundInstance         # T21
"$JAVA_HOME/bin/javap" -p -classpath "$SRG_JAR" net.minecraft.client.renderer.LevelRenderer | grep -i snow                  # T1'（SRG 名）
# 原版模型名单（T22）
python -c "import zipfile;z=zipfile.ZipFile(r'C:/Users/P傲娇34/.gradle/caches/forge_gradle/mcp_repo/de/oceanlabs/mcp/mcp_config/1.20.1-20230612.114412/joined/downloadClient/client.jar');print([n for n in z.namelist() if 'models/block/grass' in n])"
```
（路径含中文与空格，Niubash 里整体加引号；传给 python 的路径用 `r'...'` 或双引号。）

---

## 11. 文件归属表（B / C / D）

| 文件（相对 `D:\code\MC模组\csrp-1.20.1-forge`） | 归属 | 动作 |
|---|---|---|
| `PLAN.md` | A | 本文档（本阶段唯一写入） |
| `gradle.properties` | B | 改（`mod_version`） |
| `src/main/java/alku/csrp/registry/ModBlocks.java` | B | 改（+5 注册） |
| `src/main/java/alku/csrp/registry/ModItems.java` | B | 改（+4 BlockItem） |
| `src/main/java/alku/csrp/registry/ModSounds.java` | B | 改（+1 音效） |
| `src/main/java/alku/csrp/world/SrpWorldData.java` | B | 改（+2 字段、+4 方法、DATA_VERSION） |
| `src/main/java/alku/csrp/world/SrpStarWorldSelection.java` | B | 新 |
| `src/main/java/alku/csrp/block/DeadheadGrassShortBlock.java` | B（骨架）+ C（逻辑） | 新 |
| `src/main/java/alku/csrp/block/DeadheadGrassTallBlock.java` | B + C | 新 |
| `src/main/java/alku/csrp/block/SnowGrassBlock.java` | B + C | 新 |
| `src/main/java/alku/csrp/block/SnowShortGrassBlock.java` | B + C | 新 |
| `src/main/java/alku/csrp/block/SnowTallGrassBlock.java` | B + C | 新 |
| `src/main/java/alku/csrp/block/SnowCoveredGrassBlock.java` | B + C | 新 |
| `src/main/resources/assets/csrp/blockstates/*.json`（5） | B | 新 |
| `src/main/resources/assets/csrp/models/block/*.json`（18） | B | 新 |
| `src/main/resources/assets/csrp/models/item/*.json`（4） | B | 新 |
| `src/main/resources/assets/csrp/textures/block/*.png`（11） | B | 新 |
| `src/main/resources/assets/csrp/sounds/misc/snow_reversal.ogg` | B | 新 |
| `src/main/resources/assets/csrp/sounds.json` | B | 改（+1 条目） |
| `src/main/resources/assets/csrp/lang/{en_us,zh_cn}.json` | B | 改（+语言键） |
| `src/main/resources/data/csrp/structures/deadhead_tree_large_1..4.nbt` | B | 新 |
| `src/main/java/alku/csrp/world/DeadheadTreePlacer.java` | C | 新 |
| `src/main/java/alku/csrp/world/ColdStarTreeHandler.java` | C | 新 |
| `src/main/java/alku/csrp/world/FracturedTerrainHandler.java` | C | 新 |
| `src/main/java/alku/csrp/world/SnowGrassEvents.java` | C | 新 |
| `src/main/java/alku/csrp/world/StarBiomeGenerationEvents.java` | C | 改（插入 2 处调用） |
| `src/main/java/alku/csrp/world/MeteorStructureLoader.java` | C（可选） | 改（可见性 +1 词） |
| `src/main/java/alku/csrp/client/weather/BlizzardClient.java` | C | 新 |
| `src/main/java/alku/csrp/client/weather/BlizzardDirectionClient.java` | C | 新 |
| `src/main/java/alku/csrp/client/weather/BlizzardRenderer.java` | C | 新 |
| `src/main/java/alku/csrp/client/weather/BlizzardFogRenderer.java` | C | 新 |
| `src/main/java/alku/csrp/client/weather/BlizzardReverseSound.java` | C | 新 |
| `src/main/java/alku/csrp/client/weather/BlizzardClientEvents.java` | C | 新 |
| `src/main/java/alku/csrp/client/BlizzardDimensionEffects.java` | C | 新（**替代原 2 条 mixin**） |
| `src/main/java/alku/csrp/client/BlizzardDimensionEffectsEvents.java` | C | 新 |
| `src/main/resources/assets/csrp/textures/environment/snow.png` | C | 新 |
| `src/main/java/alku/csrp/mixin/client/LevelRendererBlizzardMixin.java` | C（兜底，可选） | 新 |
| `src/main/resources/csrp.mixins.json` | C（仅在走兜底 mixin 时） | 改（+1 条） |
| `src/main/resources/assets/csrp/shaders/post/blizzard_reverse.json` + `shaders/program/blizzard_reverse.{json,fsh}` | C（可选） | 新 |
| `src/main/java/alku/csrp/network/BlizzardReversePayload.java` | C | 新 |
| `src/main/java/alku/csrp/world/BlizzardDerivedHandler.java` | C | 新 |
| `src/main/java/alku/csrp/network/CsrpNetwork.java` | C | 改（+1 注册） |
| `src/main/java/alku/csrp/celestial/client/StarWorldClientState.java` | C | 改（+2 方法） |
| `src/main/java/alku/csrp/world/SrpStarWorldEvents.java` | C | 改（+1 方法） |
| `src/main/java/alku/csrp/client/SrpDifficultyScreenEvents.java` | C（片 6 可选） | 改（+2 按钮） |
| `src/main/java/alku/csrp/Csrp.java` | C（可选） | 改（+4 个 `output.accept`） |
| `build/libs/csrp-1.10.9.jar` 及全部构建产物 | D | 构建 |
| 全仓只读复核 + 跑 §8 验证 | D | 审阅/验证 |

---

## 12. 追加核实记录 — C 阶段（片 4 暴风雪客户端渲染）

> 本节为**纯追加**，不修改上文任何既有结论。全部结论用
> `javap -p -classpath <官方映射 jar>`（jar 路径同 §10.4）或读**反编译产物**真机核实。
> 另：Forge 的 `forge-1.20.1-47.4.23-sources.jar` **只含 `net/minecraftforge/**` 与 `patches/`，
> 不含任何 `net/minecraft/**` 源码**；`FogRenderer`/`LevelRenderer`/`DimensionSpecialEffects` 的
> 方法体改用 Vineflower 1.10.1 反编译官方映射 jar 得到（**反编译产物，非 Mojang 原文**）。

### 12.1 片 4 落地细节结论（§4.1 未覆盖的部分）

| # | 结论 | 证据 | 对片 4 代码的影响 |
|---|---|---|---|
| V1 | `net.minecraftforge.client.IForgeDimensionSpecialEffects` 的**真实包**是 `net.minecraftforge.client.extensions.IForgeDimensionSpecialEffects`（`net.minecraftforge.client.` 下不存在该类） | 【核实】`javap` 前者报「找不到类」，换 `...client.extensions.` 成功 | `DimensionSpecialEffects` 的 `implements` 由 Forge patch 加上；**自有子类无需 import 该接口**，覆写方法直接写 `@Override` |
| V2 | `DimensionSpecialEffects.OverworldEffects.renderSnowAndRain` **未在类中声明**（`javap -p` 只有 `getBrightnessDependentFogColor`/`isFoggyAt`/`CLOUD_LEVEL`/构造）→ 它是**接口 default 方法**，沿 `VANILLA` 调用返回 `false` | 【核实】`javap -p 'DimensionSpecialEffects$OverworldEffects'` | `return VANILLA.renderSnowAndRain(...)` 在非暴风雪时**必须**返回 `false`，原版雨雪才会继续渲染（S9 回归关键） |
| V3 | `LevelRenderer.renderSnowAndRain` 的**全部内容**被 `if (!level.effects().renderSnowAndRain(level, ticks, partialTick, lightTexture, camX, camY, camZ)) { ...vanilla... }` 包住（Forge patch 在方法体首行） | 【核实】反编译 `LevelRenderer.java:285-436` + `LevelRenderer.java.patch` | 返回 `true` 即**完全接管**；无需 mixin |
| V4 | `ForgeHooksClient.dispatchRenderStage(AFTER_WEATHER, ...)` 的**唯一两处调用点**就在 `renderSnowAndRain` 之后（`renderLevel` 的 1503/1512 行），且此时 `posestack` 已 `mulPoseMatrix(相机矩阵)` 且 `applyModelViewMatrix()` | 【核实】反编译 `LevelRenderer.java:1482-1515` | 暴风雪渲染挂 **`RenderLevelStageEvent.Stage.AFTER_WEATHER`** 即可落在原版天气槽位；该 stage 下**模型视图已是相机相对空间**，顶点须减相机坐标，**不可再 translate 相机位置**（会二次平移） |
| V5 | `FogType` 常量 = `LAVA`/`WATER`/`POWDER_SNOW`/`NONE` | 【核实】`javap -p net.minecraft.world.level.material.FogType` | 片 4 的「水下/岩浆下不改雾」按 WATER/LAVA 判定，并额外排除 POWDER_SNOW |
| V6 | `ViewportEvent.getPartialTick()` 是 **`double`**；`RenderGuiEvent.getPartialTick()` / `RenderLevelStageEvent.getPartialTick()` 是 **`float`** | 【核实】`javap -p` 三个事件类 | `RenderFog` 里必须 `(float) event.getPartialTick()`（否则编译报「有损转换」） |
| V7 | `LevelAccessor` **没有** `isClientSide()`（它在 `LevelReader` 上，1.20.1 该接口不是 `LevelAccessor` 的父接口）；`LevelEvent.getLevel()` 返回 `LevelAccessor` | 【核实】`javap -p net.minecraft.world.level.LevelAccessor` 与 `net.minecraftforge.event.level.LevelEvent` | 片 4 的 `LevelEvent.Load/Unload` **不再做客户端判定**，直接调客户端状态机 `reset()`（服务端调用只写几个静态字段，无副作用） |
| V8 | `AbstractTickableSoundInstance`：`protected AbstractTickableSoundInstance(SoundEvent, SoundSource, RandomSource)`、`public boolean isStopped()`、`protected final void stop()`；字段 `volume/pitch/x/y/z/looping/delay/attenuation/relative` 全在 `AbstractSoundInstance` 上；`SoundInstance.Attenuation` = `NONE`/`LINEAR` | 【核实】`javap -p` | `BlizzardReverseSound` 继承它、写 `this.attenuation = SoundInstance.Attenuation.NONE` 并实现 `tick()`；**结案 T21** |
| V9 | `AbstractTickableSoundInstance.tick()` 自 1.17 起**必须自己 `stop()`**（无 `donePlaying` 字段语义） | 【核实】javap 无 `donePlaying` 字段 | `tick()` 中玩家失效时调 `stop()`（≈ 1.10.9 的 `field_147668_j = true`） |
| V10 | `Level` 上：`getRainLevel(float)`、`getGameTime()`、`getDayTime()`、`isRaining()`；**`getSkyDarken()` 在 `Level` 上返回 `int`**，`ClientLevel` 另有 `getSkyDarken(float) -> float` | 【核实】`javap -p net.minecraft.world.level.Level` / `ClientLevel` | 渲染器取日光用 `(float) level.getSkyDarken()`（1.10.9 的 `world.func_72971_b(pt)` 带插值版本在客户端；`ClientLevel.getSkyDarken(float)` 也可用，但为兼容 `Minecraft.level` 的静态类型 `ClientLevel` 与 `Level` 两处，统一改用 `(float) getSkyDarken()`——**UNVERIFIED 近似**，见 §12.3） |
| V11 | `Entity`：`public double xo/yo/zo`、`yRotO`、`public final double getX()/getY()/getZ()`、`public double getEyeY()`、`public boolean isAlive()` | 【核实】`javap -p` | 相机三插值坐标改用 `Camera.getPosition()`（返回**已按 partialTick 插值**的 `Vec3`）——**结案 T12** |
| V12 | `Camera`：`getPosition(): Vec3`、`getXRot()`、`getYRot()`、`getEntity()` | 【核实】`javap -p` | 片 4 的风向基准直接取 `event.getCamera().getYRot()`，**无需**手写 `yRotO + wrapDegrees(yRot - yRotO) * pt`（更稳） |
| V13 | 顶点元素顺序：`DefaultVertexFormat.POSITION_COLOR_TEX` 的写入顺序是 **POSITION → COLOR → TEX**（`BufferBuilder.vertex(...).color(...).uv(...).endVertex()`），与 `POSITION_TEX_COLOR` 相反 | 【核实】反编译 `LevelRenderer.renderSnowAndRain` 的 `PARTICLE` 写法为 `vertex(...).uv(...).color(...).uv2(...).endVertex()`；`PARTICLE` = POSITION_TEX_COLOR_LIGHTMAP | 斜向雪丝用 `POSITION_COLOR_TEX` 并**按 `.vertex().color().uv().endVertex()` 调用**——**结案 T13** |
| V14 | `Tesselator(int bufferSize)`（第二构造）存在，`Tesselator.end()` 会 `BufferUploader.drawWithShader(buffer.end())` | 【核实】`javap -p com.mojang.blaze3d.vertex.Tesselator` / `LevelRenderer:341,380,428` | 雾壳 8×448×4 = 14336 个顶点，须自建 `new Tesselator(2 * 1024 * 1024)`，不能用默认 2MB 以下容量的单例 |
| V15 | `RenderSystem`：`blendFunc(SourceFactor, DestFactor)`（**2 参**，非 4 参）、`polygonOffset(float, float)`、`setShaderFogStart/End/Shape/Color`、`depthMask/disableDepthTest/enableDepthTest/disableCull/enableBlend/disableBlend/setShaderTexture(int, ResourceLocation)/setShaderColor` 全部存在 | 【核实】`javap -p com.mojang.blaze3d.systems.RenderSystem` | 1.12.2 的 `GlStateManager.func_187428_a(...)`（4 参）→ `blendFunc(SRC_ALPHA, ONE_MINUS_SRC_ALPHA)` |
| V16 | 原版雪纹理在客户端 jar 内路径 = `assets/minecraft/textures/environment/snow.png`（354 字节，CRC32 `f204a1f5`） | 【核实】`python zipfile` 读 `client.jar` 并**逐字节校验** | 已按字节复制到 `assets/csrp/textures/environment/snow.png`，渲染器引用 `csrp:textures/environment/snow.png`，**不依赖原版路径被其它 mod 覆盖** |
| V17 | `Mth`：`clamp(float,float,float)`、`clamp(int,int,int)`、`floor(double)`、`floor(float)` 均存在 | 【核实】`javap -p net.minecraft.util.Mth` | 1.12.2 的 `MathHelper.func_76131_a/func_76125_a/func_76141_d/func_76128_c` 映射为上面这组 |

### 12.2 片 4 构建结果

| 项 | 结果 |
|---|---|
| 命令 | `./gradlew.bat build --console=plain --no-daemon --rerun-tasks`（GraalVM 21.0.2，Gradle 8.8） |
| 结果 | **BUILD SUCCESSFUL in 1m 30s**，退出码 0，**0 个编译错误**（100 条警告全部为既有的 `ResourceLocation(String,String)` `[removal]` 弃用） |
| jar 内新增 | `alku/csrp/client/weather/{BlizzardClient,BlizzardClientEvents,BlizzardDimensionEffects,BlizzardDimensionEffectsEvents,BlizzardDirectionClient,BlizzardFogRenderer,BlizzardRenderer,BlizzardReverseSound,BlizzardShaderEvents}.class`、`assets/csrp/shaders/post/blizzard_reverse.json`、`assets/csrp/shaders/program/blizzard_reverse.{json,fsh}` |
| **mixin 数** | **csrp.mixins.json 未改动**（S4 满足：零新增 mixin） |

### 12.3 片 4 的 UNVERIFIED 近似实现清单（需 `runClient` 目视验证）

| # | 近似点 | 原实现 | 片 4 实现 | 风险 |
|---|---|---|---|---|
| U1 | 雾壳渲染的**触发条件** | 仅当 OptiFine shader pack 激活（`net.optifine.shaders.Shaders.currentShaderName` 非空且非 `OFF`） | 1.20.1 无 OptiFine 类，判定恒失败 → **只要 `intensity > 0.001` 就渲染** | 低（几何/常数逐字保留；仅"是否显示"的门槛变了，且这正是原作者的意图） |
| U2 | 日光亮度取值 | `world.func_72971_b(partialTicks)`（带插值的 daylight） | `(float) minecraft.level.getSkyDarken()`（无插值） | 低（每 tick 阶跃，肉眼几乎不可见）；`ClientLevel.getSkyDarken(float)` 可作为 D 阶段的可选改进 |
| U3 | 雾壳/雪丝的**渲染槽位** | `EntityRenderer.renderRainSnow(RETURN)` 注入 | `RenderLevelStageEvent.Stage.AFTER_WEATHER`（与 `renderSnowAndRain` 同一槽位，已由 V4 核实） | 低 |
| U4 | 原版雨雪纹理 **V 坐标**超出 1.0 | `buffer.func_187315_a(0, textureOffset)`，`textureOffset = time*0.09 + randomC*8 + lane*0.37` 可达几十 | 逐字保留同一公式 | 低（1.12.2 依赖 GL_REPEAT；1.20.1 默认 `GL_REPEAT`，行为一致。若出现雪花被拉伸，需改 `% 1.0`） |
| U5 | 斜向雪丝的**象限判断** `if (!(distance > radius))` 与 `randomF > 0.84F` 跳过逻辑 | 逐字照抄 | 逐字照抄（已核实与原文件一致） | 无 |
| U6 | `blizzard_reverse` post effect 的**着色值** | 1.10.9 **没有**这个 post shader（`blackBlend` 只作用于雪丝与雾壳顶点色） | 新增 fsh：`dim = 1 - blackBlend*0.94` + 轻微偏暖压暗 | 低（纯视觉近似，不影响任何逻辑）；**需目视确认"变黑"观感与 1.10.9 一致** |
| U7 | 该 post effect 与 `StarWorldShaderEvents` 的 **post chain 互斥** | 无此概念 | `gameRenderer.currentEffect() != null` 时**不抢占**（与 `KirinVhsEffectEvents` 同款礼让） | 低（冷星 shader 开启时 `blackBlend` 只作用于雪丝/雾壳顶点色，绕过 post） |
| U8 | `BlizzardDirectionClient.setReverseRequested` 的**调用方** | `SRPBlizzardDerivedHandler`（服务端）+ `MsgSyncBlizzardReverse` 包 | **本片不接线**（片 5 负责网络）；方法已按 1.10.9 签名 `public static void setReverseRequested(boolean)` 备好 | 无（片 5 直接调用即可） |

---

## 13. 追加核实记录 — C 阶段（片 5 暴风雪网络同步）

> 本节为**纯追加**，不修改上文任何既有结论。全部结论用
> `javap -p -classpath <官方映射 jar>`（jar 路径同 §10.4）真机核实；片 5 代码已 `build` 通过。

### 13.1 片 5 落地结论（§3.1 #16/#21/#24 与 §5.4 的细化）

| # | 结论 | 证据 | 对片 5 代码的影响 |
|---|---|---|---|
| W1 | **T17 结案**：`TickEvent.PlayerTickEvent` **存在**，`public final Player player;` + 构造 `(Phase, Player)`；`TickEvent.Phase` 只有 `START`/`END`（`phase` 字段是 `public final` 且在父类 `TickEvent` 上） | 【核实】`javap` `TickEvent$PlayerTickEvent` / `TickEvent$Phase` / `TickEvent` | 1.10.9 的 `PlayerTickEvent` 判定 **可逐字保留**，**不需要**退化为 `LevelTickEvent` 遍历 `players()` |
| W2 | **T17 结案**：`PlayerEvent.PlayerRespawnEvent` 与 `PlayerEvent.PlayerLoggedOutEvent` 均存在且嵌套名与 1.10.9 一致；`PlayerEvent` **自己声明** `public Player getEntity()`（另两个 `getEntity()` 是 `ACC_BRIDGE, ACC_SYNTHETIC` 桥） | 【核实】`javap -p` 三处 + `javap -v` 看 flags | `PlayerEvent` 家族全部直接可用；`instanceof ServerPlayer` 三元判断不变 |
| W3 | `net.minecraft.world.entity.player.Player` **既不声明** `tickCount` **也不声明** `getUUID()`；二者都在 `Entity` 上（`public int tickCount;` / `public java.util.UUID getUUID();`，经 `LivingEntity` 继承） | 【核实】`javap -p Player` 全量无匹配，`javap -p Entity` 命中 | `player.tickCount % 10` 与 `player.getUUID()` 合法（只是继承而来） |
| W4 | `Entity.isAlive()` 在 `LivingEntity` 上（`public boolean isAlive();`）；`Entity.distanceToSqr(Entity)` 存在（另有 `(double,double,double)` / `(Vec3)` 重载） | 【核实】`javap -p LivingEntity` / `Entity` | 1.10.9 的 `!entity.field_70128_L` → `candidate.isAlive()`；`getDistanceSq(ent)` → `distanceToSqr(ent)` |
| W5 | `Level.getEntitiesOfClass(Class, AABB)` **不在 `Level`/`ServerLevel` 上声明**，它是 `net.minecraft.world.level.EntityGetter` 的 **default 方法**（`Level` 经 `LevelAccessor` 继承）；`Level.dimension()` 返回 `ResourceKey<Level>` | 【核实】`javap -p Level` 零匹配 → `javap -p EntityGetter` 命中，`Level` 只声明三个 `getEntities(...)` 重载 | `level.getEntitiesOfClass(DraconiteEntity.class, aabb)` 可编译；返回 `List<DraconiteEntity>`。**泛型不变**：形参须写 `List<? extends LivingEntity>`，不能写 `Iterable<? extends LivingEntity>` |
| W6 | `AABB.inflate(double)`（单参）与 `inflate(double,double,double)` 都存在且**返回新 `AABB`** | 【核实】`javap -p AABB` | 1.10.9 的 `func_186662_g(100.0)` → `player.getBoundingBox().inflate(100.0)` |
| W7 | `net.minecraftforge.fml.loading.FMLEnvironment.dist` 是 **`public static final Dist`**，位于 **`fmlloader-1.20.1-47.4.23.jar`**（**不在**官方映射 jar 里）；`LogicalSide` 有 `isServer()` / `isClient()`；`NetworkDirection.getReceptionSide()` 返回 `LogicalSide` | 【核实】`javap` 加 fmlloader/fmlcore/javafmllanguage 到 classpath 后命中 | 见 13.2 的隔离写法 |
| W8 | `net.minecraftforge.api.distmarker.Dist` / `OnlyIn` **既不在官方映射 jar，也不在 fmlloader/fmlcore/javafmllanguage**，只在 `net.minecraftforge:mergetool:1.1.7:api` 中 | 【核实】扫描 `~/.gradle/caches` 全部 1376 个 jar，`net/minecraftforge/api/distmarker/` 仅命中 4 个 mergetool jar | 用 javap 单独核实 `Dist`/`OnlyIn` 时**必须额外加 mergetool-api jar**，否则会出现「找不到类」的**假阴性** |
| W9 | `SimpleChannel` 只有**两个** `registerMessage` 重载：5 参（无方向）与 **6 参**（末参 `Optional<NetworkDirection>`）；`NetworkDirection` 恰好 4 个常量（`PLAY_TO_SERVER`/`PLAY_TO_CLIENT`/`LOGIN_TO_SERVER`/`LOGIN_TO_CLIENT`） | 【核实】`javap -p SimpleChannel` / `NetworkDirection` | 片 5 的注册沿用既有 6 参重载 + `Optional.of(NetworkDirection.PLAY_TO_CLIENT)` |
| W10 | 工程既有 **`@OnlyIn(Dist.CLIENT)` 客户端类被直接引用在 payload 的 `handle` 里**（`ParasiteDeathFxPayload:24` → `ParasiteDeathFxClient`，该类带 `@OnlyIn(Dist.CLIENT)`），且工程 `build` 通过 | 【核实】工程源码 `ParasiteDeathFxPayload.java:3,24` + `ParasiteDeathFxClient.java:13` | 说明**既有隔离手段 = 把引用放进 `enqueueWork` 的 lambda 体内**（惰性解析，类验证期不解析客户端类）。片 5 沿用该手段，**并额外加一层 dist 判定**（见 13.2） |

### 13.2 片 5 对「客户端类在服务端加载隔离」的处理

`BlizzardDirectionClient` 是**纯客户端类**（读 `Minecraft.getInstance()`、`getSoundManager()`），但**未**标 `@OnlyIn`——与既有的
`StarWorldClientState` / `CompendiumClient` / `CelestialClientState` 同一形态。片 5 采用**三重保险**，逐条说明：

1. **引用只存在于 `enqueueWork` 的 lambda 体内**——这是工程既有手段（W10 证据）。lambda 体在类验证期**不被解析**（`invokestatic` 位于合成方法里，只在首次执行时才链接目标类），所以专用服务器上加载 `BlizzardReversePayload` 本身不会触发 `BlizzardDirectionClient` 的加载。
2. **`if (FMLEnvironment.dist == Dist.CLIENT)` 显式判定**（W7）——在专用服务器上该分支不可达，调用点永不执行，客户端类**永不被链接**。
3. **注册方向为 `PLAY_TO_CLIENT`**（W9）——服务端根本不存在该包的处理路径。

> **未采用 `DistExecutor.runWhenOn`**：它在工程里**零使用**（已 grep 全量 `DistExecutor|safeRunWhenOn|PhysicalSide` = **0 命中**），引入会新增一套本工程不存在的范式；而 `FMLEnvironment.dist` 是同一信息的直接读法。若 D 阶段希望完全消除
> `BlizzardReversePayload` 常量池里对客户端类的引用（当前**未被证实**为必要的加固），可退到二类模型：`handle` 里改为调
> `alku.csrp.client.weather.BlizzardReverseClient.apply(boolean)`，把 `setReverseRequested` 的调用封进那个客户端类内部。

### 13.3 片 5 构建结果

| 项 | 结果 |
|---|---|
| 命令 | `./gradlew.bat build --console=plain`（GraalVM 21.0.2+13.1，Gradle 8.8，daemon pid 8052） |
| 结果 | **BUILD SUCCESSFUL in 32s**，`8 actionable tasks: 2 executed, 6 up-to-date`，**0 个编译错误** |
| 证据 | `~/.gradle/daemon/8.8/daemon-8052.out.log`：`Daemon is dispatching the build result: Success`；日志尾部无 `error:`/`What went wrong` |
| 产物 | `build/libs/csrp-1.10.9.jar`（133,901,591 字节，mtime 21:49:39）+ `build/reobfJar/output.jar` 同尺寸 |
| 新类已编译 | `build/classes/java/main/alku/csrp/network/BlizzardReversePayload.class`、`build/classes/java/main/alku/csrp/world/SrpBlizzardDerivedHandler.class` |

### 13.4 片 5 与 §7 的文件名差异（**以下文为准**）

§7「片 5」列的是 `src/main/java/alku/csrp/world/BlizzardDerivedHandler.java`，而 §3.1 #21 的目标路径是
`SrpBlizzardDerivedHandler` 且与 1.10.9 原类 **同名同义**。实际落地采用 **`src/main/java/alku/csrp/world/SrpBlizzardDerivedHandler.java`**
（与 `SrpStarWorldEvents` / `SrpWorldData` / `SrpCoreSystems` 的既有 `Srp` 前缀一致）。

### 13.5 片 5 的 UNVERIFIED 近似实现清单

| # | 近似点 | 原实现（1.10.9） | 片 5 实现 | 风险 |
|---|---|---|---|---|
| U9 | 「Heblu」实体 | `EntityHeblu` | **`DraconiteEntity`**（`alku/csrp/entity/DraconiteEntity.java`，`extends DerivedParasiteEntity`）。工程 **无任何文件名含 `heblu` 的类**（`find src/main/java -iname '*heblu*'` = 0 命中）；§3.1 #21 已核实 Draconite 是 Heblu 的直接 Citadel 移植（`ModelTabula_draconite.java:9` 注释） | 低，但**实体 ≠ 模型**：Draconite 的**行为**是否等价于 1.10.9 的 Heblu 未在本轮核实；若二者在 1.10.9 中并非同一生物，本条的触发条件需 D 阶段复核 |
| U10 | 「存活」判定 | `!entity.field_70128_L`（1.12.2 的 `isDead` 标志） | `candidate.isAlive()`（W4） | 无（`isAlive()` 即 `!isRemoved() && health > 0`，与 1.10.9 语义一致） |
| U11 | `LAST_STATE` 缓存的线程模型 | 普通 `HashMap` | **逐字保留普通 `HashMap`**（`PlayerTickEvent.END` 与 `PlayerLoggedOutEvent` 都在服务端主线程） | 低 |
| U12 | 发包通道 | `SRPNetwork.CHANNEL.sendTo(msg, player)` | `CsrpNetwork.sendToPlayer(player, msg)`（= `PacketDistributor.PLAYER`），与工程其余 9 条包一致 | 无 |
| U13 | 重生同步的触发次数 | `SRPStarTypeSyncHandler` 的 respawn 分支 | 新增 `PlayerEvent.PlayerRespawnEvent`；**`PlayerLoggedInEvent` 与 respawn 不重叠**（登录才发登录事件），故不会重复发包 | 无 |
| U14 | 片 6（世界创建 UI 开关） | — | **本片未做**：`SrpDifficultyScreenEvents.java` 正被并行会话修改（属片 3/片 6 的 `world/` + 客户端 UI 重叠区），为避免覆盖未提交改动而按任务许可跳过。落地入口已就绪：`SrpStarWorldSelection.stage(boolean, boolean)` 与 `SrpWorldData.initialize()` 的两个 `consume*OrDefault()` 已存在且已接线 | 无（语言键 4×2 组已在 `en_us.json`/`zh_cn.json` 备好，见 §12 之外的 `options.csrp.{fractured_terrain,mushroom_trees}.*`） |

---

## 14. 追加核实记录 — D 阶段（独立编译与审查验证）

> 本节为**纯追加**，不修改上文任何既有结论。全部结论来自 D 阶段独立真机复现。
> **勘误一条（对上文，见 §4.1.2 委托实现要求）**：`getCloudColor(...)` **不是** `DimensionSpecialEffects`
> / `IForgeDimensionSpecialEffects` 的方法（javap 全量扫描：该方法只存在于
> `ClientLevel.getCloudColor(float)`，1.20.1 无 DimensionSpecialEffects 级云色钩子，本工程 0 引用）。
> 上文的「必须覆写 `getCloudColor(...)`」应作废；`BlizzardDimensionEffects` 实际覆写了全部
> **13 个**可覆写实例方法（无遗漏），故该错误未造成实际缺口。

### 14.1 编译与构建

| 项 | 结果 |
|---|---|
| 工具链 | GraalVM Community 21.0.2+13.1，Gradle 8.8，ForgeGradle 6.0.54（`gradlew.bat --version` 实测） |
| 第一轮 | `clean` + `build` → SUCCESSFUL 1m30s，但 **`:compileJava FROM-CACHE`** → 不构成 S1 证据 |
| **强制真编译** | `build --rerun-tasks --no-build-cache` → **BUILD SUCCESSFUL in 2m2s，`8 actionable tasks: 8 executed`，`:compileJava` 实执行，0 error**（100 条警告全部为既有 `ResourceLocation(String,String)` `[removal]` 弃用） |
| 收尾 | 修完 D 阶段 3 处缺陷后 `build` → **BUILD SUCCESSFUL in 40s** |
| 产物 | `build/libs/csrp-1.10.9.jar`，133,902,546 字节，sha256 `a868bc6b…4923f` |

### 14.2 S1–S9 判定

| # | 判定 | 证据 |
|---|---|---|
| S1 | **通过** | 强制真编译 0 error；`build/libs/csrp-1.10.9.jar` 存在；`mods.toml` `version = "1.10.9"` |
| S2 | **通过** | 6 个 blockstate（含新增 `parasitetrunk_deadhead.json`）+ 4 个 `.nbt` 全在 jar 内 |
| S3 | **通过** | `sounds.json` 的 `blizzard_reverse` = `{"category":"ambient","sounds":[{"name":"csrp:misc/snow_reversal","stream":true}]}`；`.ogg` 存在 |
| S4 | **通过** | `csrp.mixins.json` `client` 数组与 HEAD **内容逐字节等价**（仅 CRLF/LF 差异，`git diff` 为空）；**零新增 mixin** |
| S5 | **无法验证** | 需 `runClient`（无 GUI/耗时长）→ 归入 §14.7 人工清单 |
| S6 | **无法验证** | 同上（雪丝/无霞光为目视项） |
| S7 | **通过（按判据原义）** | `grep -rn "srparasites:" src/main/resources/data/csrp` = **0**；`assets/csrp/**` 下唯一的命中全在**既有的** `compendium/{entries/*.json,lang/*.lang}`（245 个 tracked 文件，`git diff`/`git status` 均**为空**，非本片引入）。新增资产 0 命中 |
| S8 | **通过** | 4 个方块名键全在；两开关各 4 键齐全（注意真实键形为 `options.csrp.<base>.enabled.description` / `.disabled.description`，由 `SrpDifficultyScreenEvents.updateToggleTooltip` 拼接，1:1 匹配） |
| S9 | **代码级通过 / 目视待验** | 见 §14.5；需 `runClient` 目视的部分归入 §14.7 |

### 14.3 资源交叉引用审计（自写 python，1.20.1 精确解析规则）

扫描规模：blockstates 363、模型 1735、`.png` 纹理 2236（+91 `.mcmeta`）、blockstate→模型引用 3569、模型 parent 引用 1671、纹理引用 2196（`#var` 8）。**JSON 语法 3368/3368 全部可解析。**

| 类别 | 修复前 | 修复后 |
|---|---|---|
| **FAIL 合计** | **2**（均为本片新增文件） | **0** |
| B2 模型 `parent` 悬空（非 minecraft 命名空间） | **2** | **0** |
| B1 blockstate→模型 悬空 | 0 | 0 |
| B3 纹理悬空 | 0 | 0 |
| B4 `#var` 未解析 | 0（6 个来自从未被烘焙的孤儿模型，无害） | 0 |
| B5 1.12.2 时代变体键 | 246 命中 / 18 文件——**全部为既有遗留**，本片新增文件 0 命中 | 不变 |
| FATAL | 0 | 0 |

- 修复前 2 条 FAIL 明细（D 阶段已修，见 §14.6）：
  `assets/csrp/models/item/snow_short_grass.json` → `csrp:item/block/snow_short_grass` 解析到
  `assets/csrp/models/item/block/…json`（1.16 起无目录回退，该目录不存在）；
  `snow_tall_grass.json` 同型。
- 说明：`csrp:blocks/xxx`（复数）与 `csrp:block/xxx`（单数）在本工程**都合法**——11 个本片纹理实际落在
  `textures/blocks/`，模型也照该路径写，属对 §3.5 规则 M1 的有意偏离（结果等价，纹理全部解析成功）。
- B5 的 246 条命中与 31 个孤儿 blockstate **均无危害**：`ModelBakery` 只加载**已注册**方块的 blockstate
  （`javap -c` 实证其构造器遍历 `BuiltInRegistries.BLOCK`），且这 6 个 id 是 `legacyBlock()` 造出的
  无属性 `Block`。属既有历史债，不在本片范围。

### 14.4 NBT 可加载性（自写 gzip+struct 解析器，逐文件逐属性）

4 个文件全部：gzip 魔数 `1f 8b` 正确、根为 COMPOUND、尾部无残留字节。

| 文件 | size | DataVersion | palette | blocks | state 下标越界 | `pos` 标签 | `state` 标签 |
|---|---|---|---|---|---|---|---|
| `_1` | [10,16,11] | **3465** | 7 | 329 | **0** | LIST<INT>×3 ✓ | INT ✓ |
| `_2` | [11,16,11] | **3465** | 9 | 352 | **0** | LIST<INT>×3 ✓ | INT ✓ |
| `_3` | [11,17,11] | **3465** | 9 | 349 | **0** | LIST<INT>×3 ✓ | INT ✓ |
| `_4` | [13,18,13] | **3465** | 9 | 506 | **0** | LIST<INT>×3 ✓ | INT ✓ |

palette 属性 vs Java 实声明（逐个比对）：

| 方块 | NBT 属性 | Java 实声明属性 | 判定 |
|---|---|---|---|
| `csrp:deadhead_leaves` | `snowy` | `distance,persistent,snowy,waterlogged`（继承 `LeavesBlock`） | ✓ 无未声明属性 |
| `csrp:parasitetrunk_deadhead` | `axis` (x/y/z) | `axis` | ✓ 精确匹配 |
| `csrp:deadhead_grass_short` | `texture` (0) | `texture` | ✓ 精确匹配 |
| `csrp:deadhead_grass_tall` | `half` (upper/lower) | `half` | ✓ 精确匹配 |
| `minecraft:snow` | `layers` (1) | `layers` | ✓ 精确匹配 |

- **1.12.2 残留属性名（`check_decay`/`decayable`/`variant`/`part`/`snow_layer`）= 0 命中** → 不构成致命缺陷。
- palette 内全部 `csrp:` id 均已注册：`deadhead_leaves`(513)、`deadhead_grass_short`(523)、
  `deadhead_grass_tall`(532)、`parasitetrunk_deadhead`(637)。**无未注册 id。**
- **判定：4 个结构均可被 1.20.1 原版 `StructureTemplate.load` 正常加载。**

### 14.5 S9 回归逐条核对（`BlizzardDimensionEffects` + 事件）

| 核对项 | 结果 |
|---|---|
| 仅 `getSunriseColor` / `renderSnowAndRain` 会因冷星走非 VANILLA 分支 | **是**（两者都以 `BlizzardClient.getIntensity(pt) > 0.001F` 为唯一分流条件） |
| 其余方法无条件转发 VANILLA | **是，无遗漏**：javap 实测可覆写实例方法共 **13 个**，`BlizzardDimensionEffects` **全部覆写**并全部 `return VANILLA.…`，不依赖 `super` 默认实现；`OverworldEffects` 为 public 且有 public 无参构造，`new OverworldEffects()` 合法 |
| 3 个事件监听均有 `intensity <= 阈值` 提前返回 | **是**（`renderBlizzard` 先查 stage 再查 intensity；`renderFog` 查 intensity + WATER/LAVA/POWDER_SNOW 排除；`renderOverlay` 查 intensity）；另有 `MIN_INTENSITY = 0.001F` |
| 注册键为 `minecraft:overworld` | **是**（`new ResourceLocation("minecraft","overworld")`，`RegisterDimensionSpecialEffectsEvent.register(ResourceLocation, DimensionSpecialEffects)` 签名已 javap 核实；注册在 MOD 总线 + `Dist.CLIENT`，注解正确带 `bus = Bus.MOD`） |
| 非冷星是否可能退化 | 静态代码路径上**否**：`BlizzardClient.isColdWorld()` 在非冷星/非主世界一律返回 `false`，`getIntensity` 恒 0，两处分流恒走 VANILLA，其余 11 个方法本就恒 VANILLA |

### 14.6 D 阶段修掉的问题（3 处，全部已重跑 build 通过）

| # | 文件 | 原因 | 验证 |
|---|---|---|---|
| 1 | `src/main/java/alku/csrp/registry/SoundEventCatalog.java` | `sounds.json` 有 485 条而 catalog 只有 484 → **既有验证脚本 `scripts/verify-original-sounds.cjs` 实红**（`sound event is absent from Java catalog: blizzard_reverse`）。`ModSounds` 虽已显式注册该事件，但 catalog 与 sounds.json 失同步 | `verify-original-sounds.cjs` exit **0**（`Verified 431 original events and 485 total registered events`）；catalog 485 条、去重后 485、0 重复 |
| 2 | `src/main/resources/assets/csrp/blockstates/snow_covered_grass.json` | 两个变体都指向 `minecraft:block/grass_block`（**非雪**草模型）→ 冷星雪覆盖草会渲染成普通草。1.20.1 原版 `snowy=true` 用的是 `minecraft:block/grass_block_snow`（读 vanilla `client.jar` 的 `blockstates/grass_block.json` 实证，且在两个独立 client jar 中一致） | 已改用 `grass_block_snow`；该模型在 vanilla jar 中存在 |
| 3 | `assets/csrp/models/item/{snow_short_grass,snow_tall_grass}.json` | `"parent": "csrp:item/block/<name>"` 解析到不存在的 `models/item/block/`（1.16 起无目录回退）→ 两个方块**物品形态 fallback 为 missing model**（客户端会报 `Unable to load model`） | 改为 `csrp:block/<name>`；审计 **FAIL 2 → 0**，两个 parent 均 `exists=True` |

> 未采纳的「缺陷」1 条（**误报，证据反驳**）：审计初轮报 `options.csrp.{fractured_terrain,mushroom_trees}.description` 缺失。
> 实读 `SrpDifficultyScreenEvents.java:159-162` 后确认真实键形为 `base + ".enabled.description"` /
> `".disabled.description"`，两条键在 `en_us.json:1408-1411` **存在**，无缺陷。
>
> 另：一次 `:reobfJar` 失败（`FileSystemException … 正被另一进程使用`）经查为 D 阶段自身的 python 审计脚本
> 从 `build/d-audit/` 打开了 `build/libs/csrp-1.10.9.jar`（python 不以 `FILE_SHARE_DELETE` 打开）所致，
> **不是源码缺陷**；释放句柄后 `:reobfJar` 立即成功。后续审计一律改用 `build/d-audit/snapshot*.jar` 快照。

### 14.7 必须人工 `runClient` 目视确认（按风险排序）

1. **【最高】非冷星世界的天空/云/地形雾/雨雪 = 与改动前逐像素一致（S9）**。看：NORMAL 星世界正午云层、
   日落霞光、雨天雨丝、水下雾。期望：与未装本片前**完全一致**（`minecraft:overworld` 的
   `DimensionSpecialEffects` 已被本 mod 完全接管，是全片唯一的全局性改动）。
2. **【高】冷星暴风雪整体观感**。看：`/weather rain` 后是否有斜向雪丝 + 8 层雾壳 + HUD 雪雾；
   雾近远是否符合 `far = 72 - intensity*56`、`near = far*0.08`。期望：雪丝斜向、随强度变密。
3. **【高】日出霞光在冷星被隐藏、非冷星正常**。看：冷星 `/time set 23000` 期望**无**橙红渐变；
   切回 NORMAL 星期望霞光**恢复**。
4. **【中】5 个新方块的模型/纹理**。看：`/setblock` 各放一次（双高草需两格），期望贴图正确、无
   missing model、雪草为「雪覆盖」外观而非普通草。**新增**：两个雪草方块的**物品栏图标**（验证 §14.6-3）。
5. **【中】雪覆盖草的视觉**。看：放雪短草/雪高草后下方草方块是否变雪草；挖掉草后是否还原为普通草。
6. **【中】死头树在冷星世界的生成**。看：冷星世界新区块的树干/树冠/树下雪草与雪层；日志无
   `Missing deadhead structure structures/deadhead_tree_large_*`。
7. **【中】碎裂地形**。看：冷星世界是否出现板块/裂缝；边界 `−64..320` 处无异常。
8. **【低】切换音效与状态机**。看：靠近 `csrp:draconite` / `csrp:kirin` 时播一次
   `blizzard_reverse`，并呈「卡→黑→保持 8 tick→反→回白」。
9. **【低】片 6 未做**：世界创建界面的两个开关按钮**预期不存在**（§13.5 U14），`SrpStarWorldSelection`
   与 `SrpWorldData.initialize()` 入口已就绪但未接线。

### 14.8 D 阶段结论

**编译与静态验证层面：达到可发布状态。** 强制真编译 0 error；jar 内容、资源交叉引用（FAIL 0）、
NBT 可加载性、S9 代码级回归、既有验证套件 **98/98 全绿** 均已独立复现。

**遗留阻塞项：无编译/静态级阻塞。** 但下列两项在发布前应处理：

1. **S5/S6/S9 目视项未验证**（本阶段无 GUI 能力）→ 必须按 §14.7 人工过一遍，其中第 1 条为全局回归风险。
2. **工作区有未提交改动**（`git status`：`SoundEventCatalog.java` + 3 个资源文件，以及未跟踪的
   `shaders/post|program/blizzard_reverse.*`、`textures/environment/snow.png`）→ 需 commit + push，
   否则 shader 与雪纹理不在版本库内。另：仓库根有 5 个游离产物
   （`compile.err`/`compile.out`/`compile_en.err`/`compile_en.out`/`thinking-effort-loaded.json`），
   建议加入 `.gitignore` 而非提交。
3. 片 6（世界创建 UI 开关）未做——已由 §13.5 U14 声明为可选项，非阻塞。


