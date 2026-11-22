---
navigation:
  title: 多方块模拟室
  icon: overload_sim:simulation_controller
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:simulation_controller
  - overload_sim:simulation_frame
  - overload_sim:simulation_efficiency_t1
  - overload_sim:simulation_efficiency_t2
  - overload_sim:simulation_efficiency_t3
  - overload_sim:simulation_fortune_module
  - overload_sim:simulation_overload_module
  - overload_sim:simulation_smelting_module
  - overload_sim:simulation_primordial_module
---

# 多方块模拟室

外部宽、高、深相等，边长可选 3～7 格。控制器存放可重复使用的完美模拟电鸣水晶。

|外尺寸|水晶容量|底部升级位|
|---|---|---|
|3×3×3|9|1|
|4×4×4|16|4|
|5×5×5|25|9|
|6×6×6|36|16|
|7×7×7|49|25|

十二条边框和整面顶盖使用模拟室框架。底部非角边框放一个控制器，正面朝外；另一处底部非角边框可放一个过载 ME 接口。四侧中间填 AE 聚能石英玻璃，底面中间填框架或升级模块，内部留空。

控制器自动检测并成型。右键打开，放入完美水晶，接入在线且有高压库存的 ME 网络。悬停进度条查看状态和单批费用。

## 生产与升级

安装通用机械时，使用 24 个氟石块绑定氟石水晶。使用 24 个带硫矿石标签的硫原矿绑定硫水晶，可混用普通和深层等变体。普通模拟每颗水晶基础产出 2～4 个氟石或 1 个硫粉。原初模块尽可能改产自然矿石，优先普通石质矿石。通用机械自身没有硫矿，可由 AllTheOres 等模组提供。

每批每个参与水晶抽取一次模拟产物。默认周期为 180 tick，完成时每个水晶扣 1000 FE 和 1 个高压。改变任意水晶槽会取消整批未完成任务，不收生产费用；加工中也可 Shift 快捷取放。

控制器接受外部 FE，在线时每 tick 最多从应用通量 FE 单元补入 10,000 FE。FE 单元为空或未安装应用通量时停止网络 FE 补电；网络本身仍需要正常 AE 供电。

|底部模块|效果|
|---|---|
|效率 T1 / T2 / T3|每个减少 2 / 4 / 8 tick，总减时上限为 104 tick|
|时运|每个把本次抽取产物翻倍，十个达到 ×1024|
|过载，最多一个|最终耗时减半，每个水晶额外消耗 1 个极高压|
|熔炼，最多一个|每个水晶额外消耗 2 个高压；原铁、原铜、原金变为两锭，远古残骸变为两个下界合金碎片；额外原矿使用标签或显式映射|

7×7×7 底面可放十三个 T3、十个时运、一个过载和一个熔炼，达到 38 tick 周期。49 个铁水晶每批产 100,352 个铁锭，费用为 49,000 FE、147 个高压、49 个极高压。

## 原初模块

在底座升级位置安装一个原初模块，可使矿物模拟直接产出自然矿石方块；与熔炼模块不能共存，重复安装也会阻止成型，自动搭建会提示并保留冲突方块。效率、时运和过载仍可搭配。单方块模拟室不接受此底座模块。

每颗成功转换的矿物水晶每轮基础产出 **1 个矿石方块，再应用模拟室时运倍率**；不会沿用红石、青金石等矿石的一次多粒掉落。每颗成功转换水晶额外消耗 **2 个高压**，不随时运倍率增加。FE 和耗时维持原规则；生物、作物和树木不改变产物、不加费。产出的矿石再次挖掘仍可享受工具时运，此叠加收益是有意保留的规则。

铁、铜、金、煤、钻石、绿宝石、红石和青金石默认产出普通石质矿石；石英产出下界石英矿石，下界合金产出远古残骸。金水晶没有维度来源信息，因此默认产出主世界金矿石。模组矿物优先读取数据包指定，否则只接受同材质标签内唯一的有效矿石；没有候选、多候选或目标失效时，回退普通产物且不收原初附加费。悬停进度条查看模式、实际批次费用和回退原因。

已经开始加工的一批保持原定产物和费用。拆装导致结构失效时暂停，重新成型后继续；新模块效果从下一批开始，重启也不会重新抽取。未开始的批次按新配置重规划。

制造：闪电装配室中投入模拟室框架×1、精准采集附魔书×1，消耗 100,000 FE 和 32 极高压，得到原初模块×1，附魔书消耗。允许精准采集书带有其他附魔或改名；普通书和附魔工具不能代替。

数据包可在 `multiblock_simulation` 配置中添加 `"primordial": [{"profile":"overload_sim:iron","block":"minecraft:deepslate_iron_ore"}]` 来指定变体；`policy.primordial_hv` 调整附加高压，`/reload` 后新批次生效。

## 输出与任务保存

输出缓冲有 128 格、四页，每格最多 1024 个同类同组件物品。左键取普通堆叠，右键取一个，Shift 按正常堆叠填入背包。管道同样只提取正常堆叠，不能抽走模板水晶。

装有过载 ME 接口时，接口绑定的频率把结构接入网络并启用自动产物导出，拒收部分留在缓冲。控制器有线或无线连接可取得网络资源，但单独连接控制器不会启用接口导出；仍可使用管道和手动取出。

断电、结构区块未加载或外壳失效时，已启动任务暂停；恢复后沿用保存的战利品结果。改动水晶会重新开始整批。生存模式拆掉控制器，会把库存、FE 和未完成任务保存在控制器物品里，放回后恢复。缩小结构后，通过回收视图取出超出容量的水晶。产物所属模组暂时缺失时，保存的任务暂停，等待物品恢复。

## 自动搭建

放置并确定控制器朝向，在左侧工具栏选择 3～7 阶后点击自动搭建。结构向控制器后方延伸，控制器位于底层正面从左角起的 `floor((size-1)/2)` 坐标。

搭建保留合法升级、框架和玻璃别名，以及一个合法接口。先取背包，再从控制器当前活动 AE 网络补齐；离线背包和创造模式也可搭建。材料不足、障碍、未加载位置或归属冲突会阻止施工。放置被取消时退还材料，重复搭建完整结构不耗材。已成型结构不能通过此按钮改尺寸。

## 数据包与 Java API

覆盖 `data/overload_sim/multiblock_simulation/default.json` 可修改机器策略：

```json
{
  "policy": {
    "ticks": 180, "reduction_cap": 104, "fortune_cap": 10,
    "t1": 2, "t2": 4, "t3": 8,
    "fe": 1000, "hv": 1, "overload_ehv": 1, "smelting_hv": 2
  },
  "smelting": [
    {"input": "example:raw_metal", "result": "example:metal_ingot", "count": 2}
  ],
  "modules": [
    {"block": "example:fast_floor", "kind": "T3"}
  ]
}
```

用一份覆盖文件声明策略。模块 kind 为 `FRAME`、`T1`、`T2`、`T3`、`FORTUNE`、`OVERLOAD`、`SMELTING`。框架和玻璃别名使用 `overload_sim:simulation_frames`、`simulation_glass` 方块标签。修改后 `/reload`。

多方块与单方块共用模拟档案和产物配方，但耗时和费用使用多方块策略，忽略单方块配方的 `production.ticks/fe/lightning/input` 字段。时运只把每个水晶的一次抽取结果乘倍数。

Java 提供器使用 `SimulationExtensions.registerOutputV2`，通过 `context.machine()` 判断 `multiblock`。`MultiblockSimulationEvents.BeforeBatchStart` 与 `BeforeBatchCommit` 可取消批次，`Completed` 在付款提交后通知产物。`BulkOutputBuffer.count(slot)` 读取数量。不要在事件中再次扣费或重复插入 Completed 产物。完整参考见 `docs/multiblock-api.md`。

## 分层搭建

F=模拟室框架，C=面向北的控制器，G=AE 聚能石英玻璃，U=框架或底部模块，.=空气。一个底部非角 F 可以替换为过载接口。

### 3×3×3 (9 水晶, 1 升级位)

底层 y=0

```
FCF
FUF
FFF
```

中间层 y=1～1

```
FGF
G.G
FGF
```

顶层 y=2

```
FFF
FFF
FFF
```

### 4×4×4 (16 水晶, 4 升级位)

底层 y=0

```
FCFF
FUUF
FUUF
FFFF
```

中间层 y=1～2

```
FGGF
G..G
G..G
FGGF
```

顶层 y=3

```
FFFF
FFFF
FFFF
FFFF
```

### 5×5×5 (25 水晶, 9 升级位)

底层 y=0

```
FFCFF
FUUUF
FUUUF
FUUUF
FFFFF
```

中间层 y=1～3

```
FGGGF
G...G
G...G
G...G
FGGGF
```

顶层 y=4

```
FFFFF
FFFFF
FFFFF
FFFFF
FFFFF
```

### 6×6×6 (36 水晶, 16 升级位)

底层 y=0

```
FFCFFF
FUUUUF
FUUUUF
FUUUUF
FUUUUF
FFFFFF
```

中间层 y=1～4

```
FGGGGF
G....G
G....G
G....G
G....G
FGGGGF
```

顶层 y=5

```
FFFFFF
FFFFFF
FFFFFF
FFFFFF
FFFFFF
FFFFFF
```

### 7×7×7 (49 水晶, 25 升级位)

底层 y=0

```
FFFCFFF
FUUUUUF
FUUUUUF
FUUUUUF
FUUUUUF
FUUUUUF
FFFFFFF
```

中间层 y=1～5

```
FGGGGGF
G.....G
G.....G
G.....G
G.....G
G.....G
FGGGGGF
```

顶层 y=6

```
FFFFFFF
FFFFFFF
FFFFFFF
FFFFFFF
FFFFFFF
FFFFFFF
FFFFFFF
```
