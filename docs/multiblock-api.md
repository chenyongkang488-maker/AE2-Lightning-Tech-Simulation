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

默认直接映射原铁、原铜、原金为各两锭，远古残骸为两个下界合金碎片；额外原矿可以声明 smelting 映射。缺少映射但在 c:raw_materials（含子标签）中时，寻找所有匹配熔炉配方，只有结果一致、无组件、原版配方数量一、产物属于 c:ingots 时默认转换为两锭。冲突或不标准配方保留原物品；作者可用显式矿物/机器熔炼映射覆盖。详见 [兼容 API](compatibility-api.md)。

## Java API

继续使用 CrystalDataAccess 和 SimulationExtensions.registerOutput 注册水晶数据与产物提供器。新的事件位于 dev.overloadsim.api.MultiblockSimulationEvents，投递到 NeoForge.EVENT_BUS：

- BeforeStructureForm：可取消；提供 controller 与 immutable structure。事件后重新校验结构。
- Formed / Invalidated：结构成型/解体通知。
- BeforeBatchStart：可取消；batch 包含固定产物、参与水晶槽、费用与升级快照。事件后校验输入、结构、输出容量，开始计时，此时不收费。
- BeforeBatchCommit：可取消；完成计时后、最终费用支付之前。回调及网络操作之后再次检查批次身份、输入修订、结构/频道和容量，避免修改输入后提交过期产物。
- BatchAborted：取消通知，包含 reason。实际水晶变化取消整批；模拟或失败的操作不取消。
- Completed：已付款产物进入缓冲后通知；输出由合法 ItemStack prototype 与 long count 表达。

处理事件时不要自行扣费、直接修改 paid/started/remaining 或保存并重复插入 Completed 的产物。事件对象用于读取与取消；需要替换产物时使用数据包或产物提供器。busy() 指已经启动的批次；paid 仅代表旧预付任务或完成结算日志，不能用作加工中的判断。

BulkOutputBuffer.insert(prototype, long amount, simulate) 返回接受数量。extract(slot, int amount, simulate) 只返回物品自身允许的正常堆叠；读取数量使用 count(slot)。不应把 1024 写到 ItemStack.count，也不应把该缓冲同时挂成 AE 存储服务：机器已主动导出到当前网络，同时挂载会重复计数。

## 异常与持久化

HV/EHV 分开模拟并实际提取；实际提取不足时按电压退回已提取部分。网络容纳不了的退费记录为持久化债务，债务未解决前不启动生产。

新任务先保存固定产物、剩余时间、输入及成本，完成时一次支付 FE/HV/EHV，再提交缓冲。计时过程中不扣生产费用（网络补充本地 FE 仍正常进行）。缺电、结构损坏、相关区块未加载时暂停，恢复后不重新抽取战利品。完成后空间不足或费用不足则等候，不重扣费。

GUI 水晶随时可放入/取出，Shift 快捷搬运也允许；成功改变任意模板槽时取消整批，新任务重新计时，不收费。外部接口 0..48 只插入模板、49..176 只提取产物，槽号保持不变。缩小后的额外水晶通过回收视图取出，同样可在工作时操作。

SettlementVersion=2、Started 和输入修订号随任务保存。已启动批次和旧 Paid=true 任务保留快照；未启动抽取缓存带进程/资源代次，重载后重建。旧版本无 SettlementVersion 视为版本1，旧预付任务正常完成不再扣费；取消时 FE 转为持久化 FeCredit，缓冲已满也不损失，HV/EHV 记为相应电压退款。退款未清偿时不启动新批次。普通存档/重启受支持；不承诺进程崩溃时跨区块存盘具备数据库级原子性。

成型并有有效容量内完美模板时球体与光源存在，与加工、离线、堵塞无关。隐形光源由控制器坐标和 UUID 持有，亮度6在无遮挡方向约照亮五格；更换块、解体及孤儿恢复仅清理自有光源，不强制加载远处控制器。隐藏回收槽里的模板不产生球体。

生存拆解控制器会把自身机器数据附在控制器物品上，避免大库存落成大量物品实体。玻璃和框架自身不拥有生产库存。第三方通过指令强制清除控制器或自行改变 NBT 时，应自行保留这些数据。
