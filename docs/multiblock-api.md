# 多方块模拟室：整合包与 Java 扩展

目标为 Minecraft 1.21.1 / NeoForge 21.1.252。单方块模拟室的旧配方时间/费用保持原语义；多方块只复用水晶档案与产物提供器，使用独立机器策略。

## 数据包

覆盖 data/overload_sim/multiblock_simulation/default.json 可调整机器数值：

```json
{
  "policy": {
    "ticks": 180, "reduction_cap": 104, "fortune_cap": 10,
    "t1": 2, "t2": 4, "t3": 8,
    "fe": 1000, "hv": 1, "overload_ehv": 1, "smelting_hv": 2
  },
  "smelting": [
    {"input": "yourmod:raw_metal", "result": "yourmod:metal_ingot", "count": 2}
  ],
  "modules": [
    {"block": "yourmod:fast_floor", "kind": "T3"}
  ]
}
```

策略应在一个覆盖文件中声明，避免不同数据包各定义一份策略造成竞争。产物倍率上限最多 10 次翻倍；数值边界会在资源重载时检查。模块 kind 为 FRAME、T1、T2、T3、FORTUNE、OVERLOAD、SMELTING。模块别名只改变底面识别，外观由别名方块自身决定。

模拟档案/产物配方仍使用现有 simulation_profile 和 overload_sim:overload_simulation 类型。多方块固定以每个水晶执行一次产物抽取，忽略旧单方块 production 中的 ticks/fe/lightning/input 字段。生物战利品抽取一次后才乘时运倍率。

结构材料标签为 overload_sim:simulation_frames 和 overload_sim:simulation_glass（路径 tags/block）。新增玻璃材料成型后同样保存原 BlockState，解体恢复。带库存或复杂方块实体的材料不适合作为玻璃别名；转换只保存方块状态，默认玻璃无独立库存。

默认直接映射原铁、原铜、原金为各两锭；额外原矿可以声明 smelting 映射。缺少映射但在 c:raw_materials 标签中时，寻找熔炉配方，只有锭标签 c:ingots 中的产物才转换，数量默认为两锭。没有适用规则则保留原物品。

## Java API

继续使用 CrystalDataAccess 和 SimulationExtensions.registerOutput 注册水晶数据与产物提供器。新的事件位于 dev.overloadsim.api.MultiblockSimulationEvents，投递到 NeoForge.EVENT_BUS：

- BeforeStructureForm：可取消；提供 controller 与 immutable structure。事件后重新校验结构。
- Formed / Invalidated：结构成型/解体通知。
- BeforeBatchStart：可取消；batch 包含固定产物、参与水晶槽、费用与升级快照。事件后校验输入、结构、输出容量和能量，再付款。
- Completed：已付款产物进入缓冲后通知；输出由合法 ItemStack prototype 与 long count 表达。

处理事件时不要自行扣费、直接修改 paid/remaining 或保存并重复插入 Completed 的产物。事件对象用于读取与取消；需要替换产物时使用数据包或产物提供器。

BulkOutputBuffer.insert(prototype, long amount, simulate) 返回接受数量。extract(slot, int amount, simulate) 只返回物品自身允许的正常堆叠；读取数量使用 count(slot)。不应把 1024 写到 ItemStack.count，也不应把该缓冲同时挂成 AE 存储服务：机器已主动导出到当前网络，同时挂载会重复计数。

## 异常与持久化

HV/EHV 分开模拟并实际提取；实际提取不足时按电压退回已提取部分。网络容纳不了的退费记录为持久化债务，债务未解决前不启动生产。

付款后的产物、剩余时间、参与输入和费用随控制器存档。缺电、结构损坏、相关区块未加载时暂停，恢复后不重扣费用、不重新抽取战利品。普通游戏停机与存档重载受支持；不承诺进程崩溃时跨区块存盘具备数据库级原子性。

尚未付款的任务在结构容量、模块或数据包重载变化后重新校验。缓存带有本次服务器进程及资源重载标识，服务器重启后不沿用未付款的旧抽取结果，避免离线修改配方后仍输出旧产物。已付款任务独立保存固定产物，不受此标识变化影响。

生存拆解控制器会把自身机器数据附在控制器物品上，避免大库存落成大量物品实体。玻璃和框架自身不拥有生产库存。第三方通过指令强制清除控制器或自行改变 NBT 时，应自行保留这些数据。
