# 自动搭建 API

目标为 Minecraft 1.21.1 / NeoForge 21.1.252，调用只允许在服务器线程进行。

`dev.overloadsim.construction.AutomaticConstruction.Plan` 接受 1..1024 个绝对方块坐标和所需状态，以及每次放置前都会重新求值的 `BooleanSupplier available`。AIR 表示必须保留为空的空间。已有可接受的升级、接口或别名材料应使用其当前状态加入计划；同方块视为已经满足，不强行调整朝向或成型属性。计划不可变。

`build(ServerPlayer, BlockEntity host, Plan, IGrid)` 要求玩家存活、允许建造、距离宿主不超过八格，宿主实例仍在当前世界原位置。网格参数只在它属于该宿主的活动 `IActionHost` 节点时使用。材料优先主背包和副手，不使用装备栏；不足部分按真实玩家的 AE 操作来源提取。无网格时仍支持只用背包。创造模式免材料。

事务先检查已加载区块、边界、所有保留空间、世界建造权限和材料库存，再实际提取 AE 物品并复查背包、宿主与世界；每次放置投递正常 NeoForge 放置事件。材料不足时不扣任何材料。放置被取消或 AE 实际提取不足时恢复本次已经放置的方块并退还已提取材料及其组件。AE 拒绝退还的部分返还背包，背包装不下则掉落给玩家。不会强制加载区块、替换流体或覆盖已有方块实体。

`Result` 提供 `successful`、实际 `placed` 数量、稳定原因键 `reason`、缺少的物品 `missing` 和可翻译 `message()`。界面负责把结果发给玩家。结构计划的最终成型由具体适配器执行；第三方取消成型事件时，已经支付和放置的物理部件保留，结果为 `formation_cancelled`，允许调整后重试。

该事务用于服务器线程内的正常同步操作；不是跨世界存盘的数据库事务，第三方 AE 存储实现仍须遵守模拟提取无副作用和实际返回数量准确的接口约定。

alpha.8 的 `SimulationAutoConstruction.plan(host, size)` 默认将底部内部升级位保留为 AIR，不消耗框架填充。已经放置的合法升级模块和框架原样保留；底边、立柱和顶面仍必须完整。`SimulationStructureValidator` 同样允许这些底部内部空位，第三方结构适配器可以按需填入升级，不必先拆默认填充方块。空位仍参加事务中的空间检查，不能覆盖已有非法方块。

## 客户端投影与框架入口（alpha.9）

`SimulationAutoConstruction.previewMin(controller, facing, size)` 和 `previewTemplate(...)` 返回 3～7 外尺寸模拟室的规范起点及不可变绝对坐标模板，不读取世界，也不强制加载区块。模板包含控制器、完整顶面与边框、四面玻璃和必须留空的内部；已有合法升级与接口的保留仍由服务器实际搭建计划判断。

控制器的 `selectedBuildSize()` 与服务器 `setSelectedBuildSize(size)` 保存当前选择，使用 NBT `SelectedBuildSize`；合法范围为 3～7，旧数据默认 3，整数越界值在加载时夹到范围内。菜单打开及同步读取该值，自动搭建使用宿主的权威选择，因此关闭页面查看七格投影后重新打开仍保持七格。

客户端 `ConstructionProjection.toggleLaser(host)` / `toggleChamber(host, size)` 开关单个本地预览；`laserActive` / `chamberActive` 查询状态。缺少的框架为粉色、玻璃为青色、光源为金色、光口为紫色；已有正确部件为绿色，障碍为红色。AIR 和已放控制器不绘制，最多 512 格；单方块坍缩核心没有额外施工部件。成型、控制器移除、布局变化、换维度、离开 64 格或预览区块卸载时清除。不发送搭建请求、不扣库存、不生成实体、不加载区块。仅从客户端代码调用这些 API。

`FrameMenuAccess.openingAnchor(host)` 在同步打开菜单时给出实际点击框架坐标。菜单需保留它，并在服务器 `stillValid` 调用 `validAnchor(player, host, identity, anchor)`。该校验要求玩家距离点击框架不超过八格、当前宿主实例与 UUID 一致、结构有效且框架只有一个归属；每次操作复查，拆除或替换后会关闭会话。不要把客户端投影视为结构有效或材料支付的证据。
