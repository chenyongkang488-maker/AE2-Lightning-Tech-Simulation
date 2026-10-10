# 过载输出接口：配置与 Java API

方块 ID 为 `overload_sim:overload_output_interface`。公共接口位于 `dev.overloadsim.output`；源槽使用 **0～35**，游戏提示使用 1～36。API 在逻辑服务端调用，不会为目标加载区块。

```java
if (level.getBlockEntity(sourcePos) instanceof OutputInterfaceBlockEntity source) {
    var link = new OutputConnection(0, Level.OVERWORLD, targetPos, Direction.NORTH);
    boolean added = source.addConnection(link);
    var snapshot = source.getConnections(); // immutable snapshot
    boolean removed = source.removeConnection(link);
    int count = source.clearConnections(0); // only source slot 0
}
```

`addConnection` 拒绝重复、非法槽号、超出范围、不允许的跨维度、已加载但不具备资源或 FE 接收能力的目标，以及第 1025 个连接。目标卸载时保留已有连接并暂停。模式通过 `setInterfaceMode(NORMAL/WIRELESS)`、`setExportMode(OFF/AUTO)`、`setIOSpeedMode(NORMAL/FAST)` 控制；资源配置使用 `getOutputLogic().getConfig()` 和 `GenericStack`，`setUnlimited(slot, boolean)` 切换每轮无限预算。

数量是每槽每轮的搬运预算，不是目标保有量。物品单位为个，原生流体单位为 mB；其它资源使用各自 AE 原生单位。同槽的多个目标共享预算并轮询；定向输出使用精确资源键。模糊卡只影响网络展示和手动提取。

在 NeoForge 游戏事件总线上监听 `OutputInterfaceExportEvent`，可取消输出，或通过 `setAmount` 降低本次输出上限。`getSourceSlot()`、`getTarget()`、`getKey()`、`getAmount()` 和 `isSimulation()` 描述当前尝试。事件会用于模拟与实际插入；监听器不得在模拟阶段扣取资源。FE 同样使用事件，感应卡向所有去重后的连接供能：无线 FE 使用当前有效连接中的一个代表槽号，普通模式 FE 的槽号为 **-1**，不会占用配置槽。普通资源配置不接受 FE。

未被目标实际接收的资源先返还原网络。原网络拒收或已离线时，`getPendingRefunds()` 返回私有持久返还缓冲的只读快照；它不是可输入的库存。破坏或打包接口保留该缓冲的所有权，不会把外来资源存入网络。

返还记录保存原频率或已加载的原物理节点／线缆部件身份。频率返还可通过前置公开 API 在重启后恢复。AE2 1.21.1 没有公开的持久网络或存储单元 UUID；有线锚点经过区块卸载重建或冷重启后，若无法确认它仍是原网络，缓冲会继续持久保留，不猜测返还到新网络。原锚点仍已加载时，输出接口自己的保存／读档可正常恢复返还。此限制只涉及已经发送失败且原网络当时也拒绝返还的余量，正常供货不受影响。

主菜单 → 模组 → 闪电科技：模拟 → 配置中的 `overload_sim-output.toml` 提供 `wirelessRange`（默认 128，0 为无限）及 `allowCrossDimension`（默认 true）。跨维度不比较坐标，目标区块始终按已加载状态处理。配置修改实时生效，越界的已有连接保留并暂停；配置重新允许后恢复。

连接工具操作：右键无线模式的源接口，Shift＋滚轮循环选择全部 36 个槽，再右键目标面。Ctrl 批量绑定相邻同种方块。重复连接只提示已连接；在目标管理页删除单条或清空某槽。右键空气清除工具选择。

新接口的数据、工具选择和注册 ID 与原接口独立，不迁移或改写原接口存档。AE2LT 原接口参考代码及相关署名、许可见项目 `LICENSES.md` 与 `THIRD_PARTY_NOTICES.md`。
