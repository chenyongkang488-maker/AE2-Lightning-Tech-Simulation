# 激光与粒子原型 API · 0.2.0-alpha.9

目标为 Minecraft 1.21.1 / NeoForge 21.1.252。已有模拟室、水晶、线圈及配方 ID 保持原样。新增设备先在创造模式验收；尚未设定生存制造配方。

## 游戏内查看

在创造模式、启用命令的超平坦测试世界，面向已加载的空地执行 `/overload_sim laser demo`。需要长约 80 格、宽约 7 格的空中区域。它生成并预充有限资源的完整光路：强化、16 个爆发和中途约束、聚焦、核心、位于核心后面的约束器。预充极高压约支持 100 秒，耗尽后须真实供能。

`/overload_sim laser build <kind>` 只生成一台设备；Tab 查看 `emitter`、`constraint`、`enhancement`、`burst`、`deflector`、`focus`、`core`、`receiver`。指令仅限拥有权限的创造模式玩家，不会覆盖已有方块或加载远处区块。右键控制器或成型结构的所属框架打开界面；潜行及扳手操作保留原行为。在物品上按 G 查搭建指南。

激光从控制器朝向的反方向传播。环形矩阵控制器位于底部，光路穿过上方三格的中心；相邻结构间留至少一格空隙。强化与爆发保持剩余射程，约束重新提供六格。聚焦矩阵前方第七格放核心；粒子继续向前，在七格内找接收口朝向核心的粒子约束器。

控制器界面左侧的自动搭建按钮使用宿主的 `structureTemplate()`，补齐八种设备的缺少部件，保留上游半面已经合法放置的单个过载接口。材料优先取玩家背包与副手，再从控制器连接并处于活动状态的 AE 网络补齐；创造模式不扣材料。它不会寻找附近的任意网络，也不会替换障碍、流体、方块实体或其它结构的保留空间。坍缩核心本身已是单方块，无需额外部件。

`LaserAutoConstruction.plan(host)` 与 `build(player, host)` 可用于扩展界面；通用事务和失败退款约定见 [自动搭建 API](construction-api.md)。成型设备的装甲与玻璃网格仅替换客户端显示，服务器仍使用真实建材、碰撞、判定和库存；解体与区块卸载时恢复普通方块显示。

## 四面偏折水晶

新偏折矩阵保持世界竖直的 7³ 球壳，控制器在底部，顶部使用框架，只有东、南、西、北四个光口。成型显示的光口直径约三格，中央为带双尖端的六棱粉色水晶。入射光段从输入口到球心，输出段从球心到对应输出口，再向外延伸六格。

`LaserMachineBlockEntity.sideMode(Direction)` 与 `setSideMode(Direction, int)` 使用 `SIDE_OFF=0`、`SIDE_INPUT=1`、`SIDE_OUTPUT=2`。新设备四面默认关闭；界面依次切换关闭、输入、输出，可设置多个输入与输出，无固定输入面。输入、输出掩码采用 `Direction.get3DDataValue()` 对应位，两掩码不重叠；上下方向始终关闭。每批只认领一次入射并守恒分流，全部输出关闭时没有下游输出；已运行的上游发射器与矩阵仍按其原费用规则收费。

NBT 保存 `OpticalInputMask`、`OpticalOutputMask`、`DeflectorShapeVersion=1` 及 `DeflectorLegacyLayout`。旧存档保留原结构、中心锚点和资源，将原水平固定输入及选中的水平输出迁移为明确配置；原上下路由停用。旧竖直结构的侧面控制器占位仍不能通光，可用水平物理端口为三个；需要四个光口时可按新投影重建。原顶部、底部端口不删除，成型时显示为封闭壳体。

光束使用独立的 `laser_energy_flow` 64 帧首尾连续粉色纹理，帧间插值只平滑时间变化，空间像素仍按最近邻显示。生产批次不重置纹理时钟；原模拟室光球、坍缩光球和外环符文保持独立材质。

## 带底座约束器与布局兼容

新控制器默认使用两层 7×7 底座，总外尺寸为 7×9×7。以控制器为原点，
`forward=FACING.opposite`、`up=basisUp(forward)`、`right=up×forward`。
底座占 right=-3..3、forward=0..6、up=-1..0，控制器替代 (0,0,0)。
球心位于 `forward*3+up*4`，正面光口位于球心-forward*3；球壳仍按半径3的体素规则。
需要 138 框架、56 玻璃、1 光口与1控制器；自动搭建在已放控制器外最多补195块，保留441格结构空间。

`host.receiverPedestalLayout()` 与 `host.structureTemplate()` 给扩展作者读取当前布局。
旧签名 `LaserStructureGeometry.template(kind,facing)` 保持原球壳语义；新增带布尔参数的重载
可显式选择新底座。持久化 `ReceiverPedestalLayout` 与客户端快照保存布局标记。
没有此标记的已保存球形约束器沿用旧结构，不要求拆除重建；粒子、FE、燃料及已支付时钟保留。
更早的方形约束器仍沿用原安全暂停迁移规则。

内部光点只读取已储存粒子。空容器无光点，正库存至少1个，随占比增加至最多96个。
轨道与尾迹是有界客户端几何，不创建生物/粒子实体、不耗费真实库存，暂停生产时仍可见。
可重载服务端粒子转换规则与能量扩展接口保持原样。

## 精确资源与配置

强度按每 20 tick 一批计量；实体伤害每两 tick 一次。库存与费用使用 `long`，GUI 简写只影响显示。固定原型边界：光束 1P FE，核心阈值 1G FE，粒子费用 512G FE，库存 125,000 粒子，约束器 FE 容量 1024P。强化 100k–9000k FE、每批爆发 64 极高压。生存常规 FE 接口每次仍是 `int`，对接高容量储能建议使用下面的 long 接口。

`config/overload_sim-laser.toml` 提供发射器/约束 FE 费用、爆发极高压费用、伤害倍率、爆炸高度及每 tick 破坏预算。默认爆炸为中心的 3×3 个区块、上下各 12 格，仅处理已加载区块，保留不可破坏方块，遵循 NeoForge 破坏事件。大量破坏分 tick 执行；当前原型的剩余爆炸任务不会在服务器重启后继续。

八种控制器均实现 AE2LT 的 `FrequencyBindingHost`。可使用原生过载频率卡绑定或清除控制器自身的频率，并通过共享频率界面管理；AE 记忆卡导出/导入同一绑定。绑定保存在控制器方块实体的外层 NBT，普通保存、重载和区块卸载保留频率。区块卸载释放实际节点和无线连接，设备仍列在频率的离线设备列表中；永久移除控制器才注销设备。无线连接遵循前置的频道容量、权限与安全规则；未成型控制器也可先连网用于自动搭建，生产仍要求结构有效。

AE 连线或结构上游半面的一处闪电科技过载接口仍可连接网络。结构桥接只销毁自己建立且仍由两端节点登记的连接；接口已经拆除或卸载时不会再次销毁 AE2 已释放的旧连接，也不清理现存的外部连线。安装应用通量 2.1.6 时使用其真实 FE 键进行 long 存储交易；未安装时按每 tick 最多 10,000 FE 桥接普通 AE 能量，保存分数余量。粒子约束器向当前活动 AE 网络输出 FE，无线绑定不要求另装结构过载接口。只有对端实际接收的能量才扣缓存。AE 网络没有足够储能空间时保留产能，不能因超高容量缓冲而令网络凭空获得无限容量。

## 耗材数据

文件位于 `data/<namespace>/particle_conversion/<name>.json`，支持 `/reload`：

```json
{
  "type": "overload_sim:particle_conversion",
  "ingredient": {"item": "minecraft:nether_star"},
  "consumable_count": 1,
  "consumable_duration_ticks": 100,
  "particle_interval_ticks": 100,
  "particles_per_operation": 1,
  "energy_per_particle": "512GFE"
}
```

`ingredient` 指定一个 `item` 或 `tag`。能量接受精确整数或字符串 `kFE`、`GFE`、`PFE`；必须能转换为完整 long FE。持续时间 1–1,000,000 tick，粒子间隔不大于持续时间，且单次固定用一个粒子/每个窗口一个耗材。文件按稳定 ID 排序匹配，修改原配方建议直接覆盖同一 ID。

第一有效 tick 即支付耗材，已投入耗材不可从界面、Shift 或自动化取出。没有粒子或 FE 缓存不足时暂停两个时钟。已支付窗口保存配方 ID；配方被移除或改得与剩余时钟不兼容时暂停，不免费刷新窗口。可选模组不存在时跳过其材料规则，不影响其它配方；`ParticleFuelData.diagnostics()` 提供诊断。

## Java 扩展与事件

`dev.overloadsim.api.LaserExtensions` 在 common setup 注册，不允许同名静默覆盖：

- `registerModifier(blockId, LaserModifier)`：普通光路遇该方块时传入世界、位置、传播方向、本批能量、剩余射程与 tick；返回 `ModifierResult`。自定义付费由实现负责，仍受 1P FE / 64 格 / 光路步数预算约束。
- `registerTarget(blockId, LaserTarget)`：返回 true 表示已处理并终止这束激光；负责自己工作方块的实际资源事务。
- `registerFuelProvider(providerId, ParticleConversionProvider)`：实现 `match(ItemStack)` 和 `resolve(recipeId)`，以稳定 ID 支持已支付窗口重载；优先采用 JSON 规则。
- `registerEnergySink(sinkId, EnergySink)`：精确 long 输出，先于内置 AE 桥；返回 `[0, amount]` 中实际接收的 FE，模拟调用不能修改任何库存。每次最多访问 32 个按 ID 排序的后端。

`LaserEvents` 中 `BeforeModifier`、`BeforeCoreEmission`、`BeforeParticleConversion`、`BeforeExplosion` 均可取消。修改数值必须经 setter，受上限校验；核心/转换事件不能增加请求粒子或产能。结算会重新检查事件后的宿主与库存。光路包含已访问节点和有界预算，闭环不能反复增幅或复制本批能量。

客户端仅按有界快照绘制光束、核心与少量演出粒子；不会根据渲染帧扣费或为每个库存粒子建立实体。新增 Blockbench 占位工程在 `art/blockbench/laser-prototype.bbmodel`；沿用已有美术许可范围，代码仍按项目 MIT 许可。

alpha.7 视觉接口：`LaserMachineBlockEntity.currentPower()` 返回已成型、启用且脉冲未过期的当前激光 FE／次，停机返回 0；不同于控制器本地 FE 缓冲。`collapseStartTick()` 为服务器同步的动画起始世界 tick，未启动为 -1。`CoreCollapseCycle` 独立于每次脉冲重建的光路，间隔超过 20 ticks 或明确停机会开启下一次 40-tick 动画。上游停机和光路失效也会重置下游核心。`BeforeCoreEmission` 取消或宿主校验失败时不会启动动画或音效。

约束和聚焦外环几何分别在 `GateMatrixMesh.outer(false/true)`；UV 使用原生图集 46～49 区域。核心流纹由 `collapse_energy_orb.png` 的 16 个 32×32 帧组成；资源包可替换纹理与 `overload_sim:collapse_transition` 音效。`art/blockbench/laser-constraint-gate-formed.bbmodel`、`laser-focus-gate-formed.bbmodel` 和 `collapse-energy-orb.bbmodel` 可编辑；新外环、球体流纹及音效属于原创 MIT 素材。视觉不改变 `LaserSegment` 的宽度、碰撞或服务器费用。
