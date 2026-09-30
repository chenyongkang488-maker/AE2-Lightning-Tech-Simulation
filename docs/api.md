# 数据包与 Java API（0.1.0-alpha.1）

面向 Minecraft 1.21.1，数据目录使用单数 `recipe`、`loot_table`、`tags/block`。建议覆盖本模组现有配方 ID；新增匹配配方应给明确的更高 priority，避免最高优先级相同造成冲突。

## 档案

`data/<命名空间>/simulation_profile/<名称>.json`：

```json
{"kind":"mineral","name":"profile.my_pack.silver","icon":"my_mod:raw_silver"}
```

档案 ID 为 `my_pack:silver`。kind 支持 mineral、crop、tree、mob。档案只保存稳定 ID，不将每种矿物做成独立注册物品。档案被移除时机器暂停，水晶数据保留。

## 结构绑定

`data/my_pack/recipe/silver_binding.json`：

```json
{
  "type":"overload_sim:crystal_binding",
  "profile":"my_pack:silver",
  "priority":100,
  "allow_artificial":false,
  "world":{
    "mode":"mineral",
    "material":{"id":"#c:storage_blocks/raw_silver"}
  }
}
```

固定检查水平 5×5 除中心 24 个位置。mode 为 mineral 时 material 在中心同一 Y；crop 和 tree 时 material 在上方一格，且需 soil 选择器。选择器 id 支持块 ID 或以 # 开头的块标签，properties 可精确匹配状态，例如 `{"id":"minecraft:wheat","properties":{"age":"0"}}`。默认作物配方允许任意生长阶段。

```json
{"world":{"mode":"crop","soil":{"id":"minecraft:farmland"},"material":{"id":"minecraft:wheat"},"condition":"my_pack:dimension_allowed"}}
```

condition 是 Java 注册的结构条件 ID。未知条件拒绝匹配。含方块实体的材料位置不会被消耗，避免丢失库存。

## 生物绑定

```json
{
  "type":"overload_sim:mob_crystal_binding",
  "profile":"overload_sim:mob",
  "priority":200,
  "allow_artificial":false,
  "mob":{"entity":"#my_pack:simulatable","radius":5,"probability":0.1,"disabled":false,"condition":"my_pack:mob_allowed"}
}
```

entity 为实体 ID、#实体类型标签或 *。只考虑存活 Mob，半径为三维球形，先挑选每个候选生物的最高优先级规则，再挑选允许记录的最近生物，最后抽取一次概率。disabled=true 可以配置黑名单。每位玩家每道雷只处理一次。成功只记录 EntityType ID，不记录个体 NBT。

## 培养

```json
{"type":"overload_sim:crystal_cultivation","profile":"my_pack:silver","priority":100,"allow_artificial":false,"cultivation":{"required":10,"increment":1}}
```

profile `overload_sim:any` 是默认兜底。首次绑定的雷击不算培养次数。完美水晶统一将 strikes 归零，使相同模板具有相同组件。required/increment 范围 1..1000000。

## 生产

```json
{
  "type":"overload_sim:overload_simulation",
  "profile":"my_pack:silver",
  "priority":100,
  "production":{
    "ticks":200,"fe":1000,"lightning":10,
    "outputs":[{"item":"my_mod:raw_silver","count":1,"chance":1}],
    "input":{"item":"minecraft:amethyst_shard","count":1}
  }
}
```

ticks/fe/lightning 为单次操作成本，lightning 指 EHV 单位。能量不足时降低实际并行；每个并行单独抽取 outputs 中各项概率。批次启动一次扣费，模板不消耗，input 按实际并行消耗。input 中 chance 不参与消耗逻辑，建议省略。

默认无矩阵 1 并行，N 个矩阵为 4N 并行，上限 32 个/128 并行。加速卡范围 0..4，耗时为 `ceil(ticks / 2^cards)`，最低 1 tick，单次成本不变。COMMON 配置可降低矩阵和加速卡数量上限。

fe/lightning 0..1000000000，ticks 1..1000000，outputs 最多 64 项，每项 count 1..4096，chance 0..1。自定义 Java 输出提供器同样限制单次最多 64 项、每项最多 4096 件。实际产物按物品堆叠上限拆分后保存，单次总量最多 4096 件、最多 256 个物品栈，超过限制会拒绝启动并返还已预留的 EHV。

显式 outputs 配方按所有非零概率产物都掉落的保守上限检查输出槽，考虑物品与组件兼容性，空间不足时降低实际并行。前置事件结束后重新检查容量。实体战利品和 Java 提供器无法预知产物，本 alpha 版要求至少一个空输出槽；抽取结果仅固定一次，超出剩余容量时保存并等待腾出空间，不重抽也不重复扣费。

生物默认生产改为 `"entity_loot":true`。可用 `"provider":"my_pack:custom_loot"` 注册自定义提供器；provider 优先于 entity_loot，后者优先于显式 outputs。未知 provider 会停止启动并记录错误。

任务开始后使用当时的水晶、并行、加速和随机输出快照。配方 /reload 不重抽已有任务；移除档案会暂停。机器拆除时未完成任务被丢弃。此版本没有 KubeJS 专用绑定与原生 AE2 crafting provider。

## Java 扩展

源码及公共类型位于 `dev.overloadsim.api`。在 common setup 的 enqueueWork 中注册，之后数据包引用 ID：

```java
SimulationExtensions.registerOutput(ResourceLocation.parse("my_pack:custom_loot"), context ->
    List.of(new ItemStack(Items.IRON_INGOT, 2)));
SimulationExtensions.registerBindingCondition(ResourceLocation.parse("my_pack:dimension_allowed"),
    (level, center) -> level.dimension() == Level.OVERWORLD);
SimulationExtensions.registerMobPredicate(ResourceLocation.parse("my_pack:mob_allowed"),
    mob -> !mob.isBaby());
```

重复 ID 拒绝注册。提供器在服务器主线程运行，应快速返回新的 ItemStack，避免修改世界、库存或网络；不要执行异步世界读写，也不要生成/击杀真实实体。

`CrystalDataAccess.read(stack)` 返回 Optional，`bound(data)`/`perfect(data)` 建立物品；CrystalData 为不可变记录，字段 profile、entityType、strikes、format。当前 format 固定 1。

NeoForge.EVENT_BUS 的事件：

- `SimulationEvents.BeforeBinding`：可取消；结构消耗/玩家换晶之前。
- `BeforeCultivation`：可取消；培养换晶之前。
- `BeforeSimulation`：可取消；EHV/FE 和辅助材料支付之前，包含实际 parallel。
- `Completed`：提交后通知，operation 为 binding、mob_binding、cultivation、production。产物已输出完毕才视为 production 完成。

事件包含服务器世界、不可变 BlockPos、水晶数据。数据字段不可变；前置事件后会重新检查相关库存和结构。条件/提供器应使用命名 API，避免直接依赖 compat 中的版本固定桥接类。

## 自动化接口与指南

机器注册 NeoForge `Capabilities.ItemHandler.BLOCK` 和 `Capabilities.EnergyStorage.BLOCK`。槽 0 为完美水晶，1 矩阵，2 加速卡，3 辅助材料，4..12 输出。外部只可提取输出，批次进行中拒绝修改输入。GUI 允许空闲时取回输入。

自动弹出只移动输出，最多每次 64 件，默认每 10 tick 尝试；六面可分别开关，按接收方真实余量扣除，未加载区块不访问。频率接口实现闪电科技公开的 FrequencyBindingHost，并使用其共享界面和记忆卡处理。

GuideME 页面位于 `assets/overload_sim/ae2guide/overload-simulation.md`，中文覆盖位于 `_zh_cn`，item_ids 绑定三种水晶与模拟室。替换资源包可以进一步扩写或翻译。
