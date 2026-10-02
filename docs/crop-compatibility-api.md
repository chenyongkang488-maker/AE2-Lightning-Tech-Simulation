# 耕地作物兼容接口

目标版本：Minecraft 1.21.1 / NeoForge 21.1.252，alpha.13。

以闪电收集器为中心，同一层放置24格耕地，每格上方有一株同类植株，中心留给收集器。放入空白水晶，成功捕获一次自然雷或人工雷后消耗植株、保留土地；再培养10次成为完美水晶。生长阶段可以不同；双格植株会一并移除其上半部分。

## 自动发现

- `CropBlock`、其它 `BushBlock` 植株、`minecraft:crops` / `overload_sim:simulation_crops` 内的方块，以及由 `c:seeds` 中的 `BlockItem` 种子种出的植株可自动发现。
- 原版 `FarmBlock` 和所有子类（包括神秘农业精华耕地）自动识别。其它实现可加入 `c:farmlands` 或 `overload_sim:simulation_farmlands`。NeoForge `canSustainPlant` 明确支持的种植床也接受，无需继承或标签；明确拒绝时不绑定，默认决策继续检查土地类别/标签及植株 `canSurvive`。
- 植株必须已种下；背包中的种子不算周围的植株。普通建筑方块不会仅因放在耕地上被视为作物。
- `CropBlock.getStateForAge(getMaxAge())` 提供成熟状态，其它作物将整数 `age` / `growth` / `stage` / `maturity` 调至最大值。没有生长属性的单阶段植株采收自身。成熟后换方块的作物归并到原幼苗，例如火把花；瓜类茎采收其果实。
- 保存ID为 `overload_sim:auto/crop/<命名空间>/<植株路径>`。例如 `overload_sim:auto/crop/mysticalagriculture/diamond_crop`；不会因注册顺序变化而换ID。
- 单方块基础费用为200tick、1000FE、1高压；多方块继续使用既有180tick基础策略和模块倍率。

## 产物与优先级

已有 `crystal_binding` 配方优先于自动绑定；显式的自然雷限制、条件拒绝、失效档案或同优先级冲突不会退回自动识别。已有 `overload_simulation` 配方也优先于自动采收。

默认以成熟 `BlockState.getDrops(LootParams.Builder)` 采收一次，不直接读取JSON掉落表。因此支持神秘农业这类重写Java采收方法的作物，也支持原版掉落表和NeoForge掉落修改器。保留作物正常掉落的种子及其它副产物，不额外附加时运附魔；多方块时运模块仅乘一次模拟产物。随机结果沿用机器原有的固定批次和保存机制。

采收上下文的位置是模拟室位置，使用空工具、幸运值0。绑定不复制来源耕地等级、原位置、生物群系、crux或其它生长加成；也不在世界中实际种植或催熟。依赖特殊环境的加成可通过显式产物配方或输出提供器控制。含方块实体/NBT的植株不自动绑定。

## 数据包覆盖

例如覆写钻石种子的模拟产物为2精华，不输出种子：

路径：`data/example/recipe/diamond_crop_simulation.json`

```json
{
  "type": "overload_sim:overload_simulation",
  "profile": "overload_sim:auto/crop/mysticalagriculture/diamond_crop",
  "priority": 100,
  "production": {
    "ticks": 200, "fe": 1000, "lightning": 1,
    "outputs": [{"item": "mysticalagriculture:diamond_essence", "count": 2}]
  }
}
```

也可以继续使用已有 `simulation_profile`、`crystal_binding` 和 `registerOutputV2` API，为自定义档案设定绑定条件、材料标签、费用、概率、辅助材料和Java动态产物。

禁止某作物自动发现：`data/overload_sim/tags/block/simulation_crop_blacklist.json`，例如：

```json
{"replace":false,"values":["example:restricted_crop"]}
```

将非标准植株和土地分别加入 `overload_sim:simulation_crops`、`overload_sim:simulation_farmlands` 即可参与发现。修改标签或配方后 `/reload` 生效；旧水晶仍按稳定ID解析。

## Java成熟状态适配

在common setup注册纯函数，只返回匹配作物的成熟采收状态；不修改世界、库存或费用。`source` 始终是归并后的来源方块默认状态，绑定和生产使用相同输入，不能依赖田里的当前生长阶段。若不同状态需要独立档案，请使用显式绑定配方的属性条件及对应的产物配方。

```java
SimulationExtensions.registerCropMaturityResolver(
    ResourceLocation.fromNamespaceAndPath("example", "special_crop"),
    (level, source) -> source.is(MyBlocks.CROP.get())
        ? Optional.of(MyBlocks.CROP.get().defaultBlockState().setValue(MyCrop.RIPE, true))
        : Optional.empty()
);
```

注册ID不能重复；多个适配器同时匹配一个来源状态时报告 `ambiguous_crop_maturity`，不消费绑定材料。返回空气或含方块实体的状态也拒绝。产物数量和额外环境逻辑使用既有输出提供器，配方的 `production.provider` 引用其注册ID。

## 诊断

- 手持水晶执行 `/overload_sim explain`：显示档案、产物提供器和成熟状态。
- `/overload_sim explain_block x y z`：坐标指向植株，显示识别结果和下面土地是否支持种植。
- `/overload_sim audit`：收录自动发现的作物诊断。

诊断不调用输出提供器、采收或抽随机掉落。输出中的 `context=machine_position` 明确表示不复制来源耕地的加成。

神秘农业采收方法参考：[官方1.21源码](https://github.com/BlakeBr0/MysticalAgriculture/blob/1.21/src/main/java/com/blakebr0/mysticalagriculture/block/MysticalCropBlock.java)。实际兼容验收使用用户已安装的8.0.28及Cucumber8.0.16。
