# 闪电科技前置与 API 兼容

本轮目标版本为闪电科技：模拟 0.2.2。窗口与自动回归使用已锁定的 AE2 Lightning Tech 2.1.2-beta4 及旧前置组合。

## 前置组合

| 用途 | AE2 | AE2 Lightning Tech | Thunderbolt Core | LDLib2 | JEI（可选） |
| --- | --- | --- | --- | --- | --- |
| 最新默认运行 | 19.2.18 | 2.1.2-beta4 | 2.0.3-beta3 | 2.2.42 all | 19.64.0.455 |
| 先前 API 运行 | 19.2.18 | 2.1.2-beta2 | 2.0.3-beta2 | 不加载 | 19.44.0.403 |
| 旧兼容运行 | 19.2.17 | 2.1.0 | 2.0.0 | 不加载 | 19.44.0.403 |

Minecraft 1.21.1、Java 21、NeoForge 21.1.252+、GuideME 21.1.19；其他整合包必须同时满足上游自己的前置范围。LDLib2 是新版 AE2LT 的必需依赖，本附属不把它打入 JAR，也不强迫旧版环境安装它。不能把 Forge 1.20.1 的同名 JAR 放入此环境。

## 互斥接入

- 启动早期 Mixin 插件仅检查字节码，不提前加载目标类。CollectorCrystalApi、DeviceWorkbenchApi、LightningApi 全部存在时使用现代路径；缺少任一类时共同保留旧路径。损坏/不匹配的完整 API 桥初始化失败会明确报错，不静默回退到双重处理。
- 现代路径在 common setup enqueueWork 中注册三种模拟晶体和共振线圈。CollectorInventoryMixin、CollectorMixin、CoilWorkbenchMixin 不加载；不再替换上游工作台 ticker，其他附属注册的设备仍能运行。
- 现代 API 类仅位于 src/modernApi/java，单独以 beta4 编译并打入同一 JAR；主体继续按 2.1.0 编译。旧环境不链接现代实现或其 Mixin。源码 JAR 和完整源码包均包括现代目录。
- 工作台公共 API 只提供能量显示和 tick，没有完整 DeviceEnergyBuffer 操作。modern.CoilApiEnergyMixin 仅对本附属注册的 CoilDevice 返回原 CoilEnergy 缓冲，不更改其他设备的缓冲、适配器注册或 ticker。
- 中枢、状态构造适配、Minecraft/AE2 挖掘/扳手/滚轮以及其余既有 Mixin 保留，不能批量删除。

## 资源结算

集雷器只在正数实际入网后的 onCaptured 内执行一次绑定/培养，原生事件不再次培养。返回晶体由上游安装；上游成功观察事件只在最终安装结果匹配时发布本附属 Completed 事件。上游库存复制 ItemStack，因此以物品及组件/数量匹配最终结果，而非 Java 引用相等。回调换栈、取消和满仓不覆盖新栈、不多培养。

普通主机扣费使用 LightningApi.forHost，并保留 IActionSource；任意已解析网络使用 keyOf 转换。simulate 只是预检，不是预留。退款仍返回原事务的存储路径，记录实际接受量和剩余欠款；不能改成要求主机在线的普通 handler.insert。原有两种电压不互换，失败生成闪电、加工取消和重启恢复使用原结算规则。

## 构建与测试

JEI 19.64 的布局入口使用 `ImmutableSize2i`。可选的 `jeiCompat` 源码集按真实字节码签名选择补丁，缩小类别高度并重建布局缓存。较小窗口的详情允许滚动，LDLib2 相机按视口比例取景，方块选中读取独立预览渲染器的射线命中结果。旧 JEI 沿用上游布局入口，滚轮仍经 JEI 原有分派。上游 beta4 的旧可选注入仍可能记录签名警告。

硫矿实测需同时传入 `-PmekTests -PsulfurTests`。将 `alltheores-3.2.0_neoforge_1.21.1.jar` 放入 `libs/`（SHA-256 `7de4018f8475874347bd8dc5b7551cad80c31446de8daae7c86aee5480f9c80a`）。AllTheOres 仅为此测试的硫矿提供方。

最新环境：`gradlew.bat test runGameTestServer -PfluxTests -PmekTests -PcropTests -PjeiTests`。

beta2：追加 `-Pae2ltRuntimeVersion=2.1.2-beta2 -PthunderboltRuntimeVersion=2.0.3-beta2 -PjeiRuntimeVersion=19.44.0.403`。

旧环境：追加 `-Pae2RuntimeVersion=19.2.17 -Pae2ltRuntimeVersion=2.1.0 -PthunderboltRuntimeVersion=2.0.0 -PjeiRuntimeVersion=19.44.0.403`。

实际验证结果另见本轮 release verification；版本范围接纳不代表验证过所有未来版本。窗口验收须另获电脑使用许可。此轮不替换用户实例、不远程发布。

## English

Version 0.2.2 targets the published AE2LT 2.1.2-beta4 binary with Thunderbolt 2.0.3-beta3 and LDLib2 2.2.42. Main sources compile against AE2LT 2.1.0. The modernApi source set supplies the public extension implementations and the optional jeiCompat source set fits the upstream preview to the window. One collector/workbench path is active at a time. Modern hosts use LightningApi. Inactive-grid refunds retain their AE storage and durable debt handling. A narrowly scoped FE-buffer Mixin preserves coil operations missing from WorkbenchDevice.

Sources: [AE2LT beta4 release](https://github.com/AE2-Lightning-Tech-Reborn/AE2-Lightning-Tech-Reborn/releases/tag/2.1.2-beta4), [Thunderbolt beta3 release](https://github.com/AE2-Lightning-Tech-Reborn/Thunderbolt-Core-Reborn/releases/tag/2.0.3-beta3), [addon Issue #1](https://github.com/chenyongkang488-maker/AE2-Lightning-Tech-Simulation/issues/1).
