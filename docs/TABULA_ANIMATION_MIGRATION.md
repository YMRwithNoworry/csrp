# Citadel Tabula 动画迁移

## 资源来源

`D:\code\模组反编译器\decompiled\[逃逸：寄生体] SRParasites-1.10.8` 中保存的是
Tabula 导出的 1.12.2 Java 源码（`ModelRenderer`、`func_78087_a`），并不包含
`.tbl` 项目文件。Citadel 不能把这些 Java 源码当作资源直接读取，因此本项目在
构建前将已经从这些源码提取的几何和动画转录打包为 Citadel 资源：

```
src/main/resources/assets/csrp/tabula/<model>.tbl
  ├── model.json       # Citadel 原生 Tabula 容器
  └── animations.json  # 原版 func_78087_a 的无损 Molang 转录
```

`.tbl` 中的 `model.json` 使用 Tabula 的 `cubes`、`children`、`position`、`offset`、
`rotation`、UV、纹理尺寸和隐藏标记。`animations.json` 保留原始动画表达式，避免把
依赖 parasite status / still animation 的动态公式粗略烘焙成低精度关键帧。

## 运行时

`LegacyTabulaModel` 通过 `Minecraft` 的 `ResourceManager` 打开 `.tbl`，定位其中的
`model.json`，然后调用 `TabulaModelHandler.INSTANCE.loadTabulaModel`。模型树由
Citadel 的 `TabulaModel` 创建，实体模型类只负责 SRP 状态选择和特殊动画公式。

`LegacyAnimationLibrary` 从同一个 `.tbl` 读取 `animations.json`，再把表达式求值结果写
入 Citadel 的 `AdvancedModelBox`。因此运行时不再读取 `geo/*.geo.json` 作为模型几何，
几何和动画也不会因为两个独立资源的版本漂移。对没有打包归档的旧开发资源仍保留
`animations/*.animation.json` 回退路径。

## 重新生成

在项目根目录执行：

```powershell
$env:JAVA_HOME = 'D:\MC\jdk\jdk-21.0.2'
node scripts/convert-geo-to-tabula.cjs
```

脚本从 `src/main/resources/assets/csrp/geo` 的提取结果生成 Tabula 几何，并将同名
`src/main/resources/assets/csrp/animations/*.animation.json` 嵌入归档；Java `jar` 工具用于创建 `.tbl`
ZIP 容器。归档不包含纹理，实体渲染器继续使用 `textures/entity` 中的正式资源。

## 验证

```powershell
./gradlew.bat compileJava
./gradlew.bat processResources
```

当前资源目录包含 136 个几何模型归档和 2 个共享动画别名，共 138 个 Citadel `.tbl` 归档；所有已注册的 Citadel 模型 ID 均有
对应归档。
