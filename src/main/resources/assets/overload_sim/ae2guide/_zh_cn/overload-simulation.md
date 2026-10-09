---
navigation:
  title: 闪电科技：模拟
  icon: overload_sim:overload_simulation_chamber
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
item_ids:
  - overload_sim:blank_simulation_crystal
  - overload_sim:simulation_crystal
  - overload_sim:perfect_simulation_crystal
  - overload_sim:overload_simulation_chamber
---

# 闪电科技：模拟

<ItemImage id="overload_sim:blank_simulation_crystal" scale="3" />

本附属模组需要 AE2、AE2 Lightning Tech、Thunderbolt 与 GuideME。应用通量（Applied Flux）是可选依赖，提供网络 FE 储存；未安装时可用外部 FE 供电。

空白模拟电鸣水晶在闪电科技闪电模拟室中制造：1 个电鸣水晶、16 个过载水晶、16 个紫水晶碎片，消耗 200,000 FE 和 32 个高压闪电，每次产出一个。

## 绑定与培养

把空白水晶放入闪电收集器。在收集器同一高度的 5×5 平面，除中心外填满 24 个同材料粗矿储存块；钻石、红石、青金石、煤、绿宝石、石英和下界合金使用对应储存块。成功收集雷击后消耗这些方块，记录矿物。

作物使用同一高度的 24 格受支持土地，每格上方种下同类植株，生长阶段可以不同。绑定消耗植株，双格植株连上半部分一起消耗，保留土地，不产生采收掉落。树木使用泥土类土地上的同类树苗。公共标签支持额外矿物和作物；材料存在歧义或矿石掉落表不同，需要显式规则。

左手拿空白水晶受到真实雷击时，有 33% 概率记录五格球形半径内最近的存活且符合资格的生物。无需受到伤害，创造模式也可用。排除玩家，只记录实体类型，不复制装备或个体数据。

已绑定水晶再受十次有效雷击成为完美水晶。可放在收集器内培养，也可拿在左手受雷击。自然雷、指令雷和人工雷均有效；匹配规则的 `allow_artificial=false` 可禁止人工雷。首次绑定的雷击不计入培养。

## 单方块模拟室

将模拟室接入在线 ME 网络，取得频道并准备高压库存。第一个槽放完美模拟电鸣水晶，可重复使用。默认每次操作需要 1000 FE、1 个高压和 200 tick；下界合金档案每次需要 16,000 FE，产出一个远古残骸。

FE 缓冲容量为 2,000,000。在线时每 tick 最多从应用通量 FE 单元补入 10,000 FE，待机时也会补电。FE 单元为空或未安装应用通量时停止网络 FE 补电；AE 能源单元供给网络运行能量，机器也接受外部 FE。

第二槽放闪电坍缩矩阵。没有矩阵时为一并行，每个矩阵提供四并行，最多 32 个、128 并行。升级栏有四个独立 AE2 加速卡槽，每张把耗时减半，向上取整且至少一 tick，不改变单次成本。手持卡右键机器或 Shift 点击卡可插入；加工中插入的卡下一批生效。

辅助槽提供自定义配方要求的材料。批次开始时按实际并行一次支付 FE、高压和辅助材料。每次操作独立抽取战利品，结果固定并保存。输出满时等待交付空间，网络离线时暂停工作；拆除机器会丢弃未完成任务。

自动弹出开关和六个方向开关可把产物送入相邻库存。标准物品自动化只能提取输出。频率按钮打开共享过载频率界面。

## 产物规则

默认生物模拟使用正常掉落，以中性玩家击杀、无抢夺、幸运值零为上下文。史莱姆大小为 1，岩浆怪大小为 2。凋零产出一个下界之星；末影龙独立抽取 25% 龙蛋和 25% 龙首，必出一个龙息；坚守者产出幽匿催发体、回响碎片和幽匿块各一个。

自动作物在两种模拟室中均按成熟植株正常采收，包含种子和副产物。不复制来源土地等级、crux 或环境加成。特殊代码生物掉落、装备及带方块实体数据的植株需要显式产物规则或 Java 提供器。

## 数据包与 API

Minecraft 1.21.1 配方路径为 `data/<namespace>/recipe/<path>.json`。建议覆盖原 ID，或为新规则设定唯一更高的 `priority`；最高优先级匹配存在冲突时停止处理。修改后执行 `/reload`。

|目录或配方类型|配置内容|
|---|---|
|`simulation_profile/<id>.json`|稳定档案的 `kind`、`name`、`icon`；kind 为 mineral、crop、tree 或 mob|
|`overload_sim:crystal_binding`|档案、优先级、人工雷许可，以及 `world` 中的材料和土地选择器|
|`overload_sim:mob_crystal_binding`|实体 ID、标签或 `*`，半径、概率、禁用标志和可选条件|
|`overload_sim:crystal_cultivation`|`cultivation.required` 与 `increment`，各为 1～1,000,000|
|`overload_sim:overload_simulation`|生产耗时、FE、高压、辅助材料，以及固定产物、实体掉落或命名提供器|

例如覆盖 `data/overload_sim/recipe/production/iron.json`：

```json
{
  "type": "overload_sim:overload_simulation",
  "profile": "overload_sim:iron",
  "priority": 100,
  "production": {
    "ticks": 200, "fe": 1000, "lightning": 1,
    "outputs": [{"item": "minecraft:raw_iron", "count": 2, "chance": 1}]
  }
}
```

额外矿物和生物映射使用 `simulation_mineral/<id>.json` 与 `simulation_mob/<id>.json`。作物和土地别名使用 `overload_sim:simulation_crops`、`simulation_farmlands` 方块标签；矿物、作物与实体类型各有黑名单标签。

`config/overload_sim-common.toml` 提供 `matrixLimit`（1～32）、`accelerationCardLimit`（0～4）、`autoEjectIntervalTicks`（1～400）和 `networkFeChargePerTick`（0～4,000,000；零表示停止新网络 FE 抽取）。

Java 在 common setup 通过 `SimulationExtensions` 注册命名条件和产物提供器。`registerOutputV2` 提供明确 RNG 与机器上下文，`registerCropMaturityResolver` 提供特殊作物的成熟状态。重复 ID 拒绝注册。`CrystalDataAccess.read/bound/perfect` 用于读取或创建水晶数据。

主手拿水晶执行 `/overload_sim explain`；对已加载方块执行 `/overload_sim explain_block x y z`。管理员可运行 `/overload_sim audit`，在世界目录生成 `overload_sim-audit.json`。详细字段见 `docs/api.md`、`docs/compatibility-api.md` 与 `docs/crop-compatibility-api.md`。[多方块模拟室](multiblock-simulation.md)使用独立生产策略。
