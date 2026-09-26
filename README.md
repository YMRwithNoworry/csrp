# CSRP — Scape and Run: Parasites for Minecraft 1.21.1 (NeoForge)

[English](#english) · [中文](#中文)

---

## English

**An unofficial, open-source port of *Scape and Run: Parasites* (SRP) to Minecraft 1.21.1 / NeoForge 21.1.**

### Why this repository exists

SRP — created by **dhanantry** and the SRP team — stopped at Minecraft 1.12.2 while the game moved
on. This repository was built for one single purpose:

> **We hope the official SRP development team will use this repository to migrate SRP to modern
> Minecraft versions.** That is the whole reason this repository was created, and the reason it is
> released under a permissive licence with every design decision written down.

Everything here was reconstructed by reading the official SRP 1.10.9 decompiled sources and
recording, clause by clause, how each behaviour maps to the original. The audit ledger lives in
[`docs/entity-parity/`](docs/entity-parity/) and every entry cites both the original source line and
the port's implementation, so a maintainer can verify — or overturn — any decision.

**If you are from the SRP team:** take it, fork it, rename it, relicense it, or ask us to transfer the
repository. No permission is needed, and none will be withheld.
**If you are a player:** this is an unofficial fan port, not an official SRP release.

### Licence

Code, resources and documentation in this repository are released under the **MIT Licence** — see
[`LICENSE`](LICENSE). You may use, modify, redistribute and sell it, as long as the copyright notice
is kept.

**Third-party notice:** *Scape and Run: Parasites*, its name, assets, sounds and game design belong to
**dhanantry** and the SRP team. They are **not** covered by the MIT Licence and this repository claims
no ownership over them. They are present only because this project reconstructs SRP for a newer
Minecraft version, in the hope that the original authors take the work over. The full text is in
[`NOTICE`](NOTICE).

### Building

Requires **JDK 21**.

```bash
./gradlew build        # Windows: gradlew.bat build
./gradlew runClient    # development client
./gradlew runServer    # headless development server
./gradlew runData      # regenerate src/generated/resources
```

The mod JAR is written to `build/libs/csrp-<version>.jar`. Mod metadata is expanded from
[`src/main/templates/META-INF/neoforge.mods.toml`](src/main/templates/META-INF/neoforge.mods.toml)
during the build.

### Repository layout

| Path | Contents |
| --- | --- |
| `src/main/java/alku/csrp` | mod source (entities, infection, world systems, client, network) |
| `src/main/resources` | assets, data, mixin config, mod icon |
| `src/main/templates` | `neoforge.mods.toml` template |
| `docs/entity-parity` | clause-level parity ledger against the original SRP sources |
| `scripts` | static verification suite (`node scripts/run-all-verifications.cjs`) |
| `tools` | small pure-JS helpers (PNG inspection/resize, texture and structure checks) |

### Contributing

1. Keep changes focused and explain the behaviour they restore.
2. Run `./gradlew.bat build` and `node scripts/run-all-verifications.cjs` before committing.
   The suite has a known baseline of failures; do not make it worse.
3. Cite evidence when restoring original behaviour — the ledger convention in
   `docs/entity-parity/` is what makes this port auditable.

### Reporting a problem (ESU)

Use the [**提交 ESU**](../../issues/new?template=esu.yml) issue form: pick the CSRP version you run
(1.20.1 / 1.21.1 / 26.3), describe the problem and the behaviour you expected, and — if you can —
attach your `crash-report` or `latest.log`.

---

## 中文

**《Scape and Run: Parasites》(SRP) 的非官方开源移植 —— Minecraft 1.21.1 / NeoForge 21.1。**

### 这个仓库为什么存在

SRP 由 **dhanantry** 与 SRP 官方开发组制作，官方版本停留在 Minecraft 1.12.2，而游戏版本一直在往前走。
本仓库的创建初心只有一条：

> **期待 SRP 官方开发组利用本仓库，把 SRP 迁移到更高版本的 Minecraft。**
> 这也是本仓库选择开源、选择 MIT 协议、并把每一个设计决策都记录下来的原因。

仓库里的每一处实现，都是对照官方 SRP 1.10.9 的反编译源码逐条还原的；每条行为的原版出处与本仓库落点都记在
[`docs/entity-parity/`](docs/entity-parity/) 台账里，任何人（尤其是原作者）都可以核对、推翻或接手。

**如果你是 SRP 官方开发组成员**：拿去、fork、改名、换协议，或者直接找我们要仓库转移权限都可以，
不需要任何许可，我们也不会拒绝。
**如果你是玩家**：这是爱好者移植，不是 SRP 官方发布。

### 协议

本仓库的代码、资源与文档以 **MIT 协议** 开源，详见 [`LICENSE`](LICENSE)：
可自由使用、修改、再分发与商用，只需保留版权声明。

**第三方声明**：*Scape and Run: Parasites* 的名称、素材、音效与玩法设计归 **dhanantry** 与 SRP 官方开发组所有，
**不在** MIT 协议覆盖范围内，本仓库不主张任何相关权利；它们出现在这里，只是因为本项目为更高版本的 Minecraft
重建 SRP，并期待原作者接手。完整声明见 [`NOTICE`](NOTICE)。

### 构建

需要 **JDK 21**。

```bash
./gradlew.bat build        # 编译并打包到 build/libs/csrp-<版本>.jar
./gradlew.bat runClient    # 开发客户端
./gradlew.bat runServer    # 开发服务端
./gradlew.bat runData      # 重新生成 src/generated/resources
```

### 目录结构

| 路径 | 内容 |
| --- | --- |
| `src/main/java/alku/csrp` | 模组源码（实体、感染、世界系统、客户端、网络） |
| `src/main/resources` | 资源、数据、mixin 配置、模组图标 |
| `src/main/templates` | `neoforge.mods.toml` 模板 |
| `docs/entity-parity` | 对照原版源码的条款级还原台账 |
| `scripts` | 静态校验套件（`node scripts/run-all-verifications.cjs`） |
| `tools` | 纯 JS 小工具（PNG 检查/缩放、贴图与结构校验） |

### 贡献

1. 改动尽量聚焦，并说明它还原了什么行为。
2. 提交前跑 `./gradlew.bat build` 与 `node scripts/run-all-verifications.cjs`；
   套件有已知失败基线，不要把基线弄得更差。
3. 还原原版行为时请附出处——`docs/entity-parity/` 的记账约定正是本仓库可被审计的原因。

### 反馈问题（提交 ESU）

请用 [**提交 ESU**](../../issues/new?template=esu.yml) 表单：先选你使用的 CSRP 版本（1.20.1 / 1.21.1 / 26.3），
再具体反馈问题并描述期待的效果，如果可以的话把 `crash-report` 或 `latest.log` 一并附上。
