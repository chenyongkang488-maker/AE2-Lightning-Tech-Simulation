# 多方块模拟室：整合包与 Java 扩展

目标为 Minecraft 1.21.1 / NeoForge 21.1.252。单方块模拟室的旧配方时间/费用保持原语义；多方块只复用水晶档案与产物提供器，使用独立机器策略。

## 数据包

覆盖 data/overload_sim/multiblock_simulation/default.json 可调整机器数值：

```json
{
  "policy": {
    "ticks": 180, "reduction_cap": 104, "fortune_cap": 10,
    "t1": 2, "t2": 4, "t3": 8,
    "fe": 1000, "hv": 1, "overload_ehv": 1, "smelting_hv": 2, "primordial_hv": 2
  },
  "smelting": [
    {"input": "yourmod:raw_metal", "result": "yourmod:metal_ingot", "count": 2}
  ],
  "modules": [
    {"block": "yourmod:fast_floor", "kind": "T3"}
  ]
}
```

策略应在一个覆盖文件中声明，避免不同数据包各定义一份策略造成竞争。产物倍率上限最多 10 次翻倍；数值边界会在资源重载时检查。模块 kind 为 FRAME、T1、T2、T3、FORTUNE、OVERLOAD、SMELTING、PRIMORDIAL。模块别名只改变底面识别，外观由别名方块自身决定。

模拟档案/产物配方仍使用现有 simulation_profile 和 overload_sim:overload_simulation 类型。多方块固定以每个水晶执行一次产物抽取，忽略旧单方块 production 中的 ticks/fe/lightning/input 字段。生物战利品抽取一次后才乘时运倍率。

结构材料标签为 overload_sim:simulation_frames 和 overload_sim:simulation_glass（路径 tags/block）。新增玻璃材料成型后同样保存原 BlockState，解体恢复。带库存或复杂方块实体的材料不适合作为玻璃别名；转换只保存方块状态，默认玻璃无独立库存。

默认直接映射原铁、原铜、原金为各两锭，远古残骸为两个下界合金碎片；额外原矿可以声明 smelting 映射。缺少映射但在 c:raw_materials（含子标签）中时，寻找所有匹配熔炉配方，只有结果一致、无组件、原版配方数量一、产物属于 c:ingots 时默认转换为两锭。冲突或不标准配方保留原物品；作者可用显式矿物/机器熔炼映射覆盖。详见 [兼容 API](compatibility-api.md)。

## 原初模块（0.2.2-alpha.4）

`overload_sim:simulation_primordial_module` 为底座实体升级，每座最多一个，与熔炼模块互斥；数据包 `PRIMORDIAL` 别名同样受约束。冲突返回 `primordial_smelting_conflict`，重复返回 `primordial_duplicates`；自动搭建保留已有方块并拒绝冲突。

顶层可选映射：

```json
{"primordial": [{"profile": "overload_sim:iron", "block": "minecraft:deepslate_iron_ore"}]}
```

明确 profile 映射优先于原版十种默认映射，随后才按有效矿物 material 的 `c:ores/<material path>` 方块标签寻找唯一候选。候选必须非空气且有物品形式；缺失、无效或多候选时回退原产物，不加原初费用。实体水晶、作物和树木不会因映射而转换。矿物身份由 profile kind 识别，不依赖所选生产配方的 mineral 字段；`block_loot` 仍只决定普通矿物掉落。

成功转换使用基础一个方块再乘模拟室时运倍率，忽略普通掉落数量和概率。默认映射为铁、铜、金、煤、钻石、绿宝石、红石、青金石的普通石质矿石，石英为下界石英矿石，下界合金为远古残骸。金水晶不记录来源维度，默认不会选择下界金矿石。不同目标可通过数据包指定。

`policy.primordial_hv` 默认 2；每颗成功转换水晶以基础 HV 加附加 HV 后应用闪电费用百分比并向上取整，普通水晶独立取整，再累加批次总费用。费用不随时运倍率增加。原初不额外修改 FE、EHV 或时间。多个配置资源按资源 ID 稳定排序，同一个资源路径仍由数据包优先级决定；推荐覆盖同一个配置文件。

`SimulationStructure` 增加 `primordial()`，`MultiblockData.Policy` 增加 `primordialHv()` 与 `cost(crystals, converted, structure)`；原公共构造和 `cost(crystals, structure)` 保留，后者为基础费用，不含需要实际矿物计数才能确定的原初附加费。`SimulationBatch` 的新构造保存原初标志、转换数、回退数及首个回退原因；旧构造与旧 NBT 默认均为 false/0/空字符串。`ResolvedMineral`、水晶组件和生产配方 codec 不变。

已开始批次保持保存的输出、费用和模块状态，即使更换模块、重载映射或重启；无效结构暂停。未开始批次在策略签名变化后重规划。沿用结算版本 2，于完成时扣费，不提前收费。控制器以批次快照显示实际费用和模式，下一批配置单独提示。控制器拆除资源规则保持原行为。

## Java API

控制器界面提供外部尺寸 3..7 的选择和自动搭建。控制器放在底面前边的中间位置，结构向朝向的反方向延伸；偶数尺寸使用 `(size-1)/2` 的整数下取整位置。已有合法框架、玻璃、底面升级和单个过载接口会保留；已成型结构不能用此按钮调整尺寸。`SimulationAutoConstruction.plan(host, size)` 和 `build(player, host, size)` 使用 [通用自动搭建事务](construction-api.md)，材料优先背包，不足从宿主连接的活动 AE 网络补齐。

单方块模拟室和多方块控制器均实现 AE2LT 的 `FrequencyBindingHost`，使用原生过载频率卡、共享频率界面和 AE 记忆卡。控制器自身的频率与结构接口的频率分别保存；控制器界面优先显示自身绑定，未绑定时保留显示结构接口频率的行为。绑定遵循前置的频道容量、权限与安全规则，连网不免除有效结构、模板和生产费用检查。区块卸载保留绑定及频率离线设备列表，同时释放真实节点和无线连接；永久移除控制器才注销设备。

多方块的 HV/EHV 供能和自动搭建材料可来自控制器当前的活动有线或无线网络。产物自动导出仍要求结构有效、相关区块已加载，并且结构上的过载接口与控制器保有有效桥接连接；仅为控制器无线绑定频率不会启用这条结构接口的物品输出。接口断开时缓冲保留产物，可手动或通过既有物品能力提取。桥接只释放自己创建且仍存在的连接，不会再次销毁已被 AE2 移除的边或删除外部连接。

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

已付款或已启动任务的产物物品暂时缺失（例如移除产物所属模组）时，控制器仍加载，原始 Outputs NBT 原样保存，停止计时、扣费及部分产物提交。`batch.hasUnresolvedOutputs()` 报告此状态；fits/flush 均返回 false。重新安装该模组并重启后重新解析原始任务；主动换晶则中断并退还旧预付费用。未知产物不能转换为空 ItemStack 后假装已完成任务。

生存拆解控制器会把自身机器数据附在控制器物品上，避免大库存落成大量物品实体。玻璃和框架自身不拥有生产库存。第三方通过指令强制清除控制器或自行改变 NBT 时，应自行保留这些数据。
