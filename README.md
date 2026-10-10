# VSIE Preview Lag Fix (`vsiefix`)

**作者 / Author:** [yuxizhimeng](https://github.com/yuxizhimeng)
**许可 / License:** MIT（jar 内已附 `META-INF/LICENSE`）
**适用 / Tested with:** Minecraft 1.21.1 · NeoForge 21.1.248 · Create 6.0.10 · Aeronautics: Interstellar Expansion (`vsie`) 1.0.1 (`AeroIE-1.0.1.jar`)

---

## 它解决什么问题 / What it fixes

**中文**

装上「Aeronautics: Interstellar Expansion」（modId `vsie`）后，**只要把含有该模组方块的机械动力蓝图拿在手上**（进入 Create 的蓝图预览状态），客户端就会严重掉帧，实测从 ~86 FPS 掉到 **~1 FPS**。

原因（用 spark 采样定位）：

- Create 的蓝图预览通过 `Create → BlockEntityRenderHelper.renderBlockEntities` **每帧直接调用** VSIE 的 `com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer.render(...)`（这个类是 VSIE **所有**动画方块渲染器的公共基类：控制座、屏幕、电池、油箱、推进器、矢量推进器、炮塔、武器、导弹发射器、电磁轨道……）；
- 同时 VSIE 控制座的「链接外设」读写逻辑（`read()/write()`、`addLinkedPeripheral(...)`、`SeatRegistry.registerOrUpdateControlSeat(...)`）在**预览虚拟世界**（`net.createmod.catnip.levelWrappers.SchematicLevel`）里照样执行。

这些活在幽灵预览世界里毫无意义，却每帧都在做，于是帧率归零。

**本补丁做的事情**：在**预览/虚拟世界**里跳过上述渲染与链接逻辑。**正常世界（包括航空学子世界中的座椅、炮塔、翘曲等）行为完全不变。**

---

## 效果与代价 / Effect & trade-off

| 项目 | 说明 |
|---|---|
| 蓝图预览里的 VSIE 方块 | **不再显示**（等于自动帮你把它们从预览中剔除；实体世界里一切照常） |
| 正常世界（含 Sable 子世界） | 完全不受影响，行为与未装补丁一致 |
| 兼容性 | Mixin 配置为 `required=false` + `defaultRequire=0`：目标类不存在或版本变动时**只会静默失效，不会崩溃** |

**强烈建议同时禁用 `ByePregen`**：它的 `replaceMissingSections` 钩子运行在 Create 预览的区块构建路径里，会进一步放大卡顿（spark profile 已证实）。只装本补丁而不禁用 ByePregen 时，卡顿可能只是减轻、并未消失。

另外，Create 预览本身的区块构建开销（`SchematicChunkSource.getChunk` → `LevelChunk.<init>` → `PalettedContainer.count`）是**固有成本**、随蓝图体积增长 —— 超大蓝图建议**拆小分开放**，或用**蓝图加农炮**慢速放置。

---

## 安装 / 卸载 / Install & Uninstall

1. 把 `vsiefix-1.2.1.jar` 放进 `mods/`；
2. 建议同时把 `byepregen-*.jar` 改名为 `byepregen-*.jar.disabled`；
3. 完全重启游戏（不是切换世界）。

> jar 内已附带 `META-INF/LICENSE` 与 `META-INF/README.md`，因此**单独转发这一个 jar** 也包含许可声明与使用说明。

**卸载**：删掉 `mods/vsiefix-1.2.1.jar`（可选：把 ByePregen 的 `.disabled` 去掉）。

**确认生效**：日志（`logs/latest.log`）里应出现

```
[vsiefix] skipped VSIE link logic in preview level net.createmod.catnip.levelWrappers.SchematicLevel
[vsiefix] skipped VSIE preview render #1 (...)
[vsiefix] skipped VSIE preview render #2 (...)
```

没有这些行说明目标类没被加载（或 VSIE 版本已变），补丁此时不会产生任何影响。

---

## 已拦截的目标 / Patched targets

| 目标 | 方式 |
|---|---|
| `com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer#render(...)` | 预览世界里取消（覆盖全部 VSIE 动画方块渲染器） |
| `com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity#read/write(...)` | 预览世界里取消 |
| `...AbstractControlSeatBlockEntity#addLinkedPeripheral / removeLinkedPeripheral / removeLinkedPeripheralImmediately` | 预览世界里取消 |
| `com.kodu16.vsie.content.controlseat.server.SeatRegistry#registerOrUpdateControlSeat(...)` | 预览世界里返回 `false` |
| `net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher#render(...)` | 兜底：预览世界里跳过 VSIE 方块实体（防止其它渲染路径漏网） |

「预览世界」判定：`Level` 类名包含 `Schematic` 或 `VirtualRenderWorld`（即 Create / Catnip 的假世界）。

---

## 反馈给 VSIE 作者 / Please also report upstream

这是绕过 VSIE 性能问题的**临时补丁**，不是官方修复。建议同时向 VSIE 作者反馈（附上 spark profile 与上面那几行日志）：

> While holding a Create blueprint that contains vsie blocks, the client drops from ~86 FPS to ~1 FPS.
> spark shows the hot chain: `Create SchematicHandler.render → SchematicRenderer.render → BlockEntityRenderHelper.renderBlockEntities → com.kodu16.vsie.foundation.AlwaysRenderGeoBlockRenderer.render` (called **every frame**), plus the control seat's `read()/write()` / `SeatRegistry` link bookkeeping running inside `net.createmod.catnip.levelWrappers.SchematicLevel`.
> Suggestion: skip block-entity rendering and link logic when the level is a schematic preview / virtual world.

---

## 从源码构建 / Build from source

```
javac -encoding UTF-8 --release 21 -proc:none \
      -cp "<sponge-mixin.jar>;<fml-loader.jar>;<client-1.21.1-*-srg.jar>;<neoforge-21.1.248-universal.jar>" \
      -d build/classes $(find src -name '*.java')
jar --create --file vsiefix-1.2.1.jar -C build/classes . -C resources .
```

---

## English summary

Holding a Create blueprint that contains **Aeronautics: Interstellar Expansion** (`vsie`) blocks drops the client to ~1 FPS, because Create's blueprint preview renders every `vsie` animated block **every frame** (`AlwaysRenderGeoBlockRenderer`, the shared base of all vsie block renderers) and vsie's control-seat link bookkeeping keeps running inside the preview's fake level.

This mod skips that rendering and those link operations **only inside preview/virtual levels**; real worlds (including Sable sub-levels) are untouched. **Also disable `ByePregen`** — its `replaceMissingSections` hook runs inside Create's preview chunk construction and makes the stutter worse.

Unofficial workaround; please report the underlying issue to the vsie author. Licensed MIT; a copy of the license is bundled at `META-INF/LICENSE` inside the jar.

---

## 变更记录 / Changelog

- **1.2.1** — 打包变更：把 `LICENSE` 与 `README` 打包进 jar（`META-INF/`），单独转发 jar 也自带许可与说明；**功能与 1.2.0 完全相同**。
- **1.2.0** — 拦截 `AlwaysRenderGeoBlockRenderer#render`（覆盖全部 VSIE 动画方块渲染器）；依据 spark profile 热点。
- **1.1.0** — 预览世界里跳过 VSIE 方块实体渲染（`BlockEntityRenderDispatcher` 兜底）。
- **1.0.0** — 预览世界里跳过 VSIE 链接读写（`read/write`、`*LinkedPeripheral`、`SeatRegistry`）。
