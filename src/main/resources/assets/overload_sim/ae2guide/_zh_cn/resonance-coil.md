---
navigation:
  title: 过载雷鸣线圈
  icon: overload_sim:resonance_coil
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:resonance_coil
  - overload_sim:extreme_voltage_module
  - overload_sim:mimic_tool_module
  - overload_sim:ultimate_destruction_module
  - overload_sim:efficiency_module
  - overload_sim:fortune_module
  - overload_sim:silk_touch_module
  - overload_sim:wrench_module
---

# 过载雷鸣线圈

<ItemImage id="overload_sim:resonance_coil" scale="3" />

把线圈放进接入在线 ME 网络的过载装备工作站，安装过载核心。工作站会把工具绑定到自己的网络；工作站保持在线且区块已加载时，取出的工具可远程使用网络闪电。

手持工具或把它留在在线工作站内，可从应用通量（Applied Flux）FE 存储单元充电，默认每 tick 最多 10,000 FE。FE 单元为空或未安装应用通量时停止这项补电；AE 能源单元仍供给网络运行能量。也可使用外部 FE 充电。

## 引雷

短按右键后松开，雷击视线内最近且未被阻挡的实体或方块，默认射程 32 格。蓄力 30 tick（1.5 秒）后松开则雷击自己。每次只产生一道雷，消耗 10 个高压闪电，冷却 10 tick（0.5 秒）。

安装极高压模块，在 G 设置中开启自然雷后，每次改用 10 个极高压闪电；收集器与仪式按自然雷识别。所选电压库存不足时不能使用，不会改扣另一种电压。雷击遵循正常伤害和点火规则。

左手拿空白模拟电鸣水晶，靠近生物后蓄力雷击自己，可尝试 33% 生物绑定。真实雷击命中即可，无需受到伤害，创造模式也可使用。已绑定水晶可继续受雷击培养，详见[模拟水晶](overload-simulation.md)。

## 模块与 FE

在工作站安装模块，七种线圈模块各最多一个。所有操作都需要过载核心。

|模块|用途|
|---|---|
|极高压|开放自然雷设置|
|拟态工具|下界合金等级采集、剑和剪刀能力；潜行右键去皮、耕地或铲路|
|终极破坏|需要拟态；忽略采集等级，可挖掘不可破坏方块|
|效率|需要拟态；可开关并调整 0～10 级，默认 10|
|时运|需要拟态；可调整 0～5 级，默认 5|
|精准采集|需要拟态；默认开启，优先于时运|
|扳手|开放扳手设置，不需要拟态|

卸下拟态前，必须先卸下其四种依赖模块。终极破坏仍遵循方块掉落与保护事件，基岩默认没有掉落。

挖掘和近战默认每次消耗 200 FE。基础容量为 10,000,000 FE；安装原生 T1、T2 或 T3 能量模块后，容量分别替换为 1,000,000,000、5,000,000,000 或 20,000,000,000 FE。能量模块合计最多一个，卸下后储能钳制到剩余容量。引雷使用闪电费用，不扣挖掘 FE。

## G 设置与扳手

G 打开闪电科技设备界面，右上角武器页使用当前手持线圈或电磁炮，主手优先。模块与设置区可滚动。

扳手模式默认关闭。安装核心和扳手模块后，在 G 中开启即可右键旋转 AE 方块、潜行右键拆卸，按原生规则返还内容。扳手模式关闭右键引雷和已蓄力释放，不额外消耗 FE 或闪电。

安装可选通用机械后，可选择通用扳手、物品、流体、化学品、能量、热量、清空或旋转。关闭 GUI，主手握持已启用的扳手线圈，按 Shift＋滚轮双向循环用途；配置模式潜行右键循环侧面设置。原生安全与管道规则仍生效，清空模式会掉落内部物品。未安装通用机械时仍可使用 AE 扳手。卸下核心或模块后扳手能力失效；关闭扳手模式后恢复引雷。

## 配置与 Java API

标准合成配方位于 `data/<namespace>/recipe/<path>.json`。在 `config/overload_sim-common.toml` 的 `[resonanceCoil]` 中设置：

```toml
[resonanceCoil]
lightningRange = 32
selfStrikeChargeTicks = 30
lightningCooldownTicks = 10
miningEnergyFe = 200
chargeFePerTick = 10000
```

五项范围依次为 1～128 格、1～200 蓄力 tick、1～200 冷却 tick、0～1,000,000 FE/次、0～1,000,000 FE/tick。

Java 扩展可用 `CoilModules.install/uninstall` 校验模块改动，用 `CoilConfiguration.apply(stack, action, value)` 修改设置。安装返回工具副本，不扣传入的模块，调用方需自行扣一件。`CoilEnergy.INSTANCE` 提供 FE 充电能力。完整参考见 `docs/api.md`。
