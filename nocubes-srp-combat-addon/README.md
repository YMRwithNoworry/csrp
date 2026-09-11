# SRP Combat Addon: Armory（NeoForge 1.21.1 移植版）

这是 `NoCubes_SRP_Combat_Addon_3.0.0.jar` 的独立 NeoForge 1.21.1 附属模组，模组 ID 保持为 `nocubessrparmory`，并在 `neoforge.mods.toml` 中声明必须安装 `csrp >= 1.10`。

## 构建

在 `D:\code\MC模组\csrp` 根目录执行：

```bat
rem 推荐：先构建 csrp，再构建附属模组
build-nocubes-addon.bat

rem 或在根目录手动执行
gradlew.bat -p nocubes-srp-combat-addon build
```

产物：`nocubes-srp-combat-addon/build/libs/nocubessrparmory-3.0.0-port.1.jar`。

将该 JAR 与 `csrp-1.10.8.jar` 一起放入实例的 `mods` 目录。不要把旧的 1.12.2/1.14.4 JAR 同时放入。

## 移植说明

* 保留了原模组的 84 个注册 ID、资源纹理、模型、配方、进度和英文名称，因此旧配方/资源引用可继续使用。
* 七套护甲、近战武器、弓和发射器已使用 1.21.1 NeoForge 注册 API 重写。
* 原 MCreator 版本的 13 个自定义投射物实体已按原 `entitybullet*` 注册 ID 完整重建为 `ArmoryArrowEntity`：
  * `entitybullettwistedbomb`、`entitybulletplasmatorch`、`entitybulletflamethrower`、`entitybulletincinerator`、`entitybulletpestilentmiasm`、`entitybulletpestilentshuriken`、`entitybulletheadbomb`、`entitybullethosttentacle`、`entitybullethostbomb`、`entitybulletevolutionbow`、`entitybulletoverlordblade`、`entitybullettwistedbow`、`entitybulletgorecombatbow`。
  * 使用原版箭物理（重力/阻力）、`setSilent(true)`、`PickupStatus.DISALLOWED`，并复刻 1.12 的 `ceil(速度 × 伤害)` 伤害公式、暴击随机加成和击退公式。
  * 客户端使用 `ThrownItemRenderer` 复刻原 `RenderSnowball` 图标：弓/喷火器显示隐形投射物，炸弹/手里剑显示自身图标，Overlord Blade 显示 Overlord Core。
  * 发射器保留原始发射间隔（例如 Twisted Bow 18 tick、Gore Bow 20 tick、Overlord Blade 200 tick、Pestilent Miasm 22 tick、Pestilent Shuriken 5 tick）。
  * 燃料类发射器（Flamethrower、Incinerator、Plasma Torch）消耗 `capsulefuel`；Twisted Bow、Gore Bow、Evolution Bow、Pestilent Miasm 使用箭矢；Infinity 附魔可绕过弹药消耗（与原 MCreator 代码一致）。
  * 投射物携带原始伤害、命中点燃/中毒/缓慢/发光效果，以及每 tick 的飞行粒子轨迹。
  * Head Bomb、Twisted Bomb 命中后按原概率召唤 `csrp:sim_humanhead`、`csrp:sim_villagerhead` 或 `csrp:rupter`；Host Bomb、Host Tentacle 产生 4.0 爆炸；Overlord Blade 命中后召唤 `csrp:anc_pod`。
  * 护甲套装、Mimic 系列、Twisted 系列、Gore Hatchet 等原始 procedure 行为已按原持续时间和倍率迁移。
  * 原版的 7 个发射器相关进度（`advgorebow`、`advheadbomb`、`advpestilentmiasm`、`advpestilentshuriken`、`advplasmatorch`、`advrupterbomb`、`advtwistedbow`）会在对应使用时授予。

配方中的旧 `srparasites:*` 材料已迁移到当前 `csrp:*` 注册表（包括 `ada_*_drop`、`living_core`、`lurecomponent1..6` 和 `bone`），不会再引用已经不存在的旧 SRP namespace。

资源遵循 1.21.1 的 `data/nocubessrparmory/recipe` 与 `data/nocubessrparmory/advancement`（单数）路径；配方结果使用 1.21 的 `result.id` 字段，并将旧染色玻璃/染料元数据配方改为现代白色染料和玻璃板。

当前已生成的可直接分发副本也位于 `D:\code\MC模组\csrp附属开发\移植产物\nocubessrparmory-3.0.0-port.1.jar`。
