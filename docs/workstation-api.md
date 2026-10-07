# 强能工作站数据包与扩展 API

闪电科技：模拟（Lightning Tech: Simulation）的强能工作站外尺寸固定为 7×7×7。控制器统一处理物品、流体、AE 网络与频率绑定；其它外壳没有资源 capability。施工共用 `overload_sim:laser_frame`、`overload_sim:laser_glass` 和 `overload_sim:laser_optical_port`，只保留 `overload_sim:high_energy_workstation` 为公开的工作站专用施工物品。

## 执行与继承

工作站读取游戏最终加载的过载处理工厂配方，保留原配方 ID、Ingredient 实例、材料数量、流体匹配条件和产物组件。数据包与 KubeJS 修改、替换、删除的结果均在这个最终目录中生效。不会复制静态配方，也不会补回被脚本删除的配方。

AE2LT 2.1.0 使用 `OVERLOAD_PROCESSING_TYPE` 的最终 `RecipeManager` 条目；2.1.1 新版存在 `OverloadProcessingRecipeCatalog` 时使用其执行目录，因此也继承工厂借用的 AdvancedAE 反应仓配方。压印机推导条目本来就在最终工厂类型中。新增配方与移除配方、相同管理器的配方 map 替换、标签更新会使目录缓存失效。

一次加工最多执行 1024 次原配方操作，两个连续有效 tick 完成。后壁光口需要收到**单束强度严格大于 1,000,000,000 FE**的激光；接收有效期为 22 tick，弱光束不会相加。激光替代原配方 FE 与闪电费用，工作站只消耗材料。断光或结构失效重置未完成进度，尚未完成的加工不扣材料。完成前重新检查当前配方和输出空间。

原配方的加工语义保持：最多九个带数量的无序物品输入，可分布于多个格，重叠 Ingredient 共享数量约束；材料用 `Ingredient.test` 判定，包括标签、替代项和自定义组件匹配。纯流体配方要求九个物品输入格为空。物品加工不额外生成容器/工具剩余物，只有配方声明的结果。

每个输入/输出格容量为 1,000,000；保存和同步使用数量为 1 的原型物品与独立数量字段。手动及 capability 抽取返回普通合法堆叠。输入/输出流体槽各容纳 1,024,000,000 mB，批量结果与数量运算在结算前检查上限。

流体界面同步真实流体身份、组件和数量，按材质与填充比例渲染，并提供精确余量提示。容器操作由当前服务端菜单验证并按 NeoForge 流体容器 API 结算：输入槽允许灌入和取出，输出槽只允许取出。每次只处理一个容器；剩余堆叠保留，容器产物没有可用去处时拒绝交易。手动操作独立于控制器侧面自动化模式。

## 专用配方类型

内置专属条目 `overload_sim:transcendent_crystal` 消耗 256 个 `ae2lt:overload_crystal`，产出 4 个 `overload_sim:transcendent_crystal`；不加入上游工厂类型。与其它专属条目一样可由数据包覆盖或移除。

Minecraft 1.21.1 的数据包路径为 `data/<namespace>/recipe/<path>.json`。专用配方类型为 `overload_sim:high_energy_workstation`，采用上游过载处理工厂的序列化格式。例如：

```json
{
  "type": "overload_sim:high_energy_workstation",
  "priority": 10,
  "inputs": [
    { "ingredient": { "tag": "c:ingots/iron" }, "count": 4 },
    { "ingredient": { "item": "minecraft:redstone" }, "count": 1 }
  ],
  "results": [ { "id": "minecraft:gold_ingot", "count": 2 } ],
  "totalEnergy": 5,
  "lightningCost": 1,
  "lightningTier": "high_voltage"
}
```

`totalEnergy`（至少 5）、正整数 `lightningCost` 和 `lightningTier` 保留上游格式，但强能工作站忽略这些费用。`inputs` 最多 9 项，`results` 最多 1 项；可选 `inputFluid` / `resultFluid` 使用 NeoForge `FluidStack` 格式。至少声明一种输入和一种输出。材料和输出的组件序列化跟随上游；物品结果数量不受普通最大堆叠限制，但必须为正整数。

选择首先比较可执行并行数，然后比较 `priority`、物品输入种类数和总需求数量（均降序），最后按配方 ID 确定顺序。专用配方与继承配方一起匹配。

## Java 扩展

公共入口位于 `dev.overloadsim.workstation`：

- `WorkstationRecipes.registerSource(ResourceLocation, Source)` 在 common setup 注册附加来源，`Source.recipes(RecipeManager)` 返回当前最终管理器中可执行的 `Entry(id, OverloadProcessingRecipe)`。保留原对象；来源 ID 和配方 ID 不允许静默覆盖。
- `WorkstationRecipes.invalidate()` 用于扩展自身的规则改变时清理目录。`recipes` / `find` 读取当前目录，`select` / `evaluate` 计算可行批次，不改变资源。
- `HighEnergyWorkstationRecipe` 包装上游 recipe，使用独立 type 与 serializer；不能继承 final 的上游类。
- `WorkstationBuffer` 提供 `prototype(slot)`、`count(slot)`、`insert(slot, item, amount, simulate)`、`extract(slot, amount, simulate)`、`debit(slot, amount)`、`save/load/copy`。原型是防御性复制。
- `HighEnergyWorkstationBlockEntity` 提供 `formed()`、`facing()`、`center()`、`receiver()`、`template()`、`currentPower()`、`progress()`、`lastParallel()`、`lastRecipe()`，以及 `inputs()` / `outputs()`、防御性流体快照。
- 控制器的任意面都支持管道：物品自动化只向输入格插入、只从输出格提取；流体自动化只注入输入槽、只抽取输出槽。旧的 `SideModes` 存档字段不再限制交互。为旧脚本保留 `sideMode(Direction)`（恒为 3）与 `setSideMode(Direction,int)`（不再修改状态），两个方法均已弃用。
- `eject/setEject` 控制相邻输出，`networkExport/setNetworkExport` 控制已连接的 AE 网络输出；两个开关默认关闭，只有产物可以导出，每 tick 有数量预算。
- `build(ServerPlayer)` 使用统一 `AutomaticConstruction` 事务从背包和在线 AE 网络搭建完整模板；不破坏内部占据，不强制加载区块。

## 结构坐标

局部坐标 x 向右、y 向上、z 朝内，各轴 0..6；控制器位于 `(3,0,0)`，面向外。激光光口位于 `(3,3,6)`。顶底两层完整封闭、不透光；四面墙的边框封闭，背面 5×5 内壁为激光框架（中心换光口），其它三面 5×5 内壁为激光通光玻璃。总计控制器 1、光口 1、框架 141、玻璃 75、内部空气 125，所有 343 单元参与归属。

旧 `high_energy_workstation_frame`、`high_energy_workstation_glass`、`high_energy_workstation_receiver` 注册 ID 保留兼容加载，隐藏于创造分类，并撤下获取它们的旧配方。完整有效的旧外壳在服务端形成时迁移到对应激光方块；单独旧方块的掉落和旧物品的一比一合成转换也使用对应激光物品。迁移不重置控制器 UUID、物品、流体或已配置模式。方块模型直接继承现有激光模型，控制器继承 `high_energy_laser_emitter`。

`LaserExtensions.TargetContext.incoming` 是激光**行进方向**。从背墙外部照入时，它等于控制器 `facing()`。透明的自有激光光照代理可占据内部空气；其它方块会使结构失效。`WorkstationStructureIndex` 提供跨结构占据检查与已成型成员的控制器查询。成员菜单以实际点击的框架/光口为距离锚点，并复核控制器 UUID 和归属。

正常激光接收携带发射源坐标与 UUID；各源独立保存最新脉冲，强度取仍有效的单束最大值，不会相加。路径停止、断开或发射源移除时，通过 `LaserTarget.deactivate` 立即释放对应来源，剩余强度不足则立即取消未完成进度。22 tick 租期只用于过期兜底，不能让已经断开的路径继续加工。无来源的五参数测试/扩展上下文保持匿名租期兼容；正常路径不会同时登记匿名副本。

拆除控制器的正常掉落把库存和流体存入 `CUSTOM_DATA`；替换有库存的控制器也保存为携带数据的控制器物品。若邻接容器或 AE 存储在输出转移回调中拆除控制器，掉落会延后到回调结束：先恢复拒收部分，再生成唯一一个包含剩余物品和流体的原控制器，不额外生成可用控制器。卸载清理结构归属与激光租期，不清空库存，不保存待结算配方或激光费用。
