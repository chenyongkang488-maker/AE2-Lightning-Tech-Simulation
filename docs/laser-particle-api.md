# 激光与粒子原型 API · 0.2.0-alpha.3

目标为 Minecraft 1.21.1 / NeoForge 21.1.252。已有模拟室、水晶、线圈及配方 ID 保持原样。新增设备先在创造模式验收；尚未设定生存制造配方。

## 游戏内查看

在创造模式、启用命令的超平坦测试世界，面向已加载的空地执行 `/overload_sim laser demo`。需要长约 80 格、宽约 7 格的空中区域。它生成并预充有限资源的完整光路：强化、16 个爆发和中途约束、聚焦、核心、位于核心后面的约束器。预充极高压约支持 100 秒，耗尽后须真实供能。

`/overload_sim laser build <kind>` 只生成一台设备；Tab 查看 `emitter`、`constraint`、`enhancement`、`burst`、`deflector`、`focus`、`core`、`receiver`。指令仅限拥有权限的创造模式玩家，不会覆盖已有方块或加载远处区块。右键控制器打开界面；在物品上按 G 查搭建指南。

激光从控制器朝向的反方向传播。环形矩阵控制器位于底部，光路穿过上方三格的中心；相邻结构间留至少一格空隙。强化与爆发保持剩余射程，约束重新提供六格。聚焦矩阵前方第七格放核心；粒子继续向前，在七格内找接收口朝向核心的粒子约束器。

控制器界面左侧的自动搭建按钮使用同一份 `LaserStructureGeometry.template`，补齐八种设备的缺少部件，保留上游半面已经合法放置的单个过载接口。材料优先取玩家背包与副手，再从控制器连接并处于活动状态的 AE 网络补齐；创造模式不扣材料。它不会寻找附近的任意网络，也不会替换障碍、流体、方块实体或其它结构的保留空间。坍缩核心本身已是单方块，无需额外部件。

`LaserAutoConstruction.plan(host)` 与 `build(player, host)` 可用于扩展界面；通用事务和失败退款约定见 [自动搭建 API](construction-api.md)。成型约束器的连续玻璃球面与圆环仅替换客户端显示，原有方块、判定及储存数据保持不变，解体和区块卸载时恢复普通方块显示。

## 精确资源与配置

强度按每 20 tick 一批计量；实体伤害每两 tick 一次。库存与费用使用 `long`，GUI 简写只影响显示。固定原型边界：光束 1P FE，核心阈值 1G FE，粒子费用 512G FE，库存 125,000 粒子，约束器 FE 容量 1024P。强化 100k–9000k FE、每批爆发 64 极高压。生存常规 FE 接口每次仍是 `int`，对接高容量储能建议使用下面的 long 接口。

`config/overload_sim-laser.toml` 提供发射器/约束 FE 费用、爆发极高压费用、伤害倍率、爆炸高度及每 tick 破坏预算。默认爆炸为中心的 3×3 个区块、上下各 12 格，仅处理已加载区块，保留不可破坏方块，遵循 NeoForge 破坏事件。大量破坏分 tick 执行；当前原型的剩余爆炸任务不会在服务器重启后继续。

AE 连线或结构上游半面的一处闪电科技过载接口可连接网络。安装应用通量 2.1.6 时使用其真实 FE 键进行 long 存储交易；未安装时按每 tick 最多 10,000 FE 桥接普通 AE 能量，保存分数余量。只有对端实际接收的能量才扣缓存。AE 网络没有足够储能空间时保留产能，不能因超高容量缓冲而令网络凭空获得无限容量。

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
